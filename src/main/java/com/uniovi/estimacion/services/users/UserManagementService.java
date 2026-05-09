package com.uniovi.estimacion.services.users;

import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.repositories.projects.EstimationProjectRepository;
import com.uniovi.estimacion.repositories.projects.ProjectMembershipRepository;
import com.uniovi.estimacion.repositories.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserManagementService {

    private final UserRepository userRepository;
    private final EstimationProjectRepository estimationProjectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public boolean canManageUsers() {
        return currentUserService.isAdminOrProjectManager();
    }

    public Page<User> findPageVisibleForCurrentUser(Pageable pageable) {
        Page<User> usersPage;

        if (currentUserService.isAdmin()) {
            usersPage = userRepository.findAllByOrderByUsernameAsc(pageable);
            usersPage.getContent().forEach(this::initializeUserReferences);
            return usersPage;
        }

        if (currentUserService.isProjectManager()) {
            usersPage = currentUserService.getCurrentUser()
                    .map(manager -> userRepository.findByProjectManagerIdOrderByUsernameAsc(manager.getId(), pageable))
                    .orElse(Page.empty(pageable));

            usersPage.getContent().forEach(this::initializeUserReferences);
            return usersPage;
        }

        return Page.empty(pageable);
    }

    public Optional<User> findManageableUserByIdForCurrentUser(Long userId) {
        if (currentUserService.isAdmin()) {
            return userRepository.findById(userId)
                    .map(user -> {
                        initializeUserReferences(user);
                        return user;
                    });
        }

        if (currentUserService.isProjectManager()) {
            return currentUserService.getCurrentUser()
                    .flatMap(manager -> userRepository.findByIdAndProjectManagerId(userId, manager.getId()))
                    .map(user -> {
                        initializeUserReferences(user);
                        return user;
                    });
        }

        return Optional.empty();
    }

    public List<UserRole> findCreatableRolesForCurrentUser() {
        List<UserRole> roles = new ArrayList<>();

        if (currentUserService.isAdmin()) {
            roles.add(UserRole.ROLE_PROJECT_MANAGER);
            roles.add(UserRole.ROLE_PROJECT_WORKER);
            return roles;
        }

        if (currentUserService.isProjectManager()) {
            roles.add(UserRole.ROLE_PROJECT_WORKER);
            return roles;
        }

        return roles;
    }

    public List<User> findAvailableProjectManagersForAdmin() {
        if (!currentUserService.isAdmin()) {
            return List.of();
        }

        List<User> managers = new ArrayList<>();
        managers.addAll(userRepository.findByRoleOrderByUsernameAsc(UserRole.ROLE_PROJECT_MANAGER));
        managers.addAll(userRepository.findByRoleOrderByUsernameAsc(UserRole.ROLE_USER));

        return managers;
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(normalize(username));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(normalize(email));
    }

    public boolean existsByUsernameExcludingId(String username, Long userId) {
        return userRepository.existsByUsernameAndIdNot(normalize(username), userId);
    }

    public boolean existsByEmailExcludingId(String email, Long userId) {
        return userRepository.existsByEmailAndIdNot(normalize(email), userId);
    }

    @Transactional
    public Optional<User> createUserForCurrentUser(String username,
                                                   String email,
                                                   String rawPassword,
                                                   UserRole requestedRole,
                                                   Long projectManagerId) {
        if (!canManageUsers()) {
            return Optional.empty();
        }

        User user = new User();
        user.setUsername(normalize(username));
        user.setEmail(normalize(email));
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);

        if (currentUserService.isAdmin()) {
            return createUserAsAdmin(user, requestedRole, projectManagerId);
        }

        if (currentUserService.isProjectManager()) {
            return createWorkerAsCurrentManager(user);
        }

        return Optional.empty();
    }

    @Transactional
    public boolean updateUserForCurrentUser(Long userId,
                                            String username,
                                            String email,
                                            String rawPassword) {
        Optional<User> optionalUser = findManageableUserByIdForCurrentUser(userId);

        if (optionalUser.isEmpty()) {
            return false;
        }

        User existingUser = optionalUser.get();

        existingUser.setUsername(normalize(username));
        existingUser.setEmail(normalize(email));

        if (StringUtils.hasText(rawPassword)) {
            existingUser.setPassword(passwordEncoder.encode(rawPassword));
        }

        userRepository.save(existingUser);
        return true;
    }

    @Transactional
    public boolean deleteUserForCurrentUser(Long userId) {
        Optional<User> optionalUser = findManageableUserByIdForCurrentUser(userId);

        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();

        if (isCurrentUser(user)) {
            return false;
        }

        if (user.getRole() == UserRole.ROLE_ADMIN) {
            return false;
        }

        if (isProjectManagerRole(user.getRole())) {
            boolean hasProjects = estimationProjectRepository.existsByOwnerId(user.getId());
            boolean hasWorkers = userRepository.existsByProjectManagerId(user.getId());

            if (hasProjects || hasWorkers) {
                return false;
            }
        }

        if (user.getRole() == UserRole.ROLE_PROJECT_WORKER) {
            projectMembershipRepository.deleteByWorkerId(user.getId());
        }

        userRepository.delete(user);
        return true;
    }

    private Optional<User> createUserAsAdmin(User user,
                                             UserRole requestedRole,
                                             Long projectManagerId) {
        UserRole finalRole = normalizeAdminCreatableRole(requestedRole);

        user.setRole(finalRole);

        if (finalRole == UserRole.ROLE_PROJECT_MANAGER) {
            user.setProjectManager(null);
            return Optional.of(userRepository.save(user));
        }

        if (finalRole == UserRole.ROLE_PROJECT_WORKER) {
            Optional<User> optionalManager = findValidProjectManager(projectManagerId);

            if (optionalManager.isEmpty()) {
                return Optional.empty();
            }

            user.setProjectManager(optionalManager.get());
            return Optional.of(userRepository.save(user));
        }

        return Optional.empty();
    }

    private Optional<User> createWorkerAsCurrentManager(User user) {
        Optional<User> optionalCurrentUser = currentUserService.getCurrentUser();

        if (optionalCurrentUser.isEmpty()) {
            return Optional.empty();
        }

        user.setRole(UserRole.ROLE_PROJECT_WORKER);
        user.setProjectManager(optionalCurrentUser.get());

        return Optional.of(userRepository.save(user));
    }

    private void applyAdminRoleChanges(User existingUser,
                                       UserRole requestedRole,
                                       Long projectManagerId) {
        if (existingUser.getRole() == UserRole.ROLE_ADMIN) {
            existingUser.setProjectManager(null);
            return;
        }

        UserRole finalRole = normalizeAdminCreatableRole(requestedRole);

        existingUser.setRole(finalRole);

        if (finalRole == UserRole.ROLE_PROJECT_MANAGER) {
            existingUser.setProjectManager(null);
        } else if (finalRole == UserRole.ROLE_PROJECT_WORKER) {
            User manager = findValidProjectManager(projectManagerId)
                    .orElse(existingUser.getProjectManager());

            existingUser.setProjectManager(manager);
        }
    }

    private Optional<User> findValidProjectManager(Long projectManagerId) {
        if (projectManagerId == null) {
            return Optional.empty();
        }

        return userRepository.findById(projectManagerId)
                .filter(user -> isProjectManagerRole(user.getRole()));
    }

    private UserRole normalizeAdminCreatableRole(UserRole requestedRole) {
        if (requestedRole == UserRole.ROLE_PROJECT_WORKER) {
            return UserRole.ROLE_PROJECT_WORKER;
        }

        return UserRole.ROLE_PROJECT_MANAGER;
    }

    private boolean isProjectManagerRole(UserRole role) {
        return role == UserRole.ROLE_PROJECT_MANAGER || role == UserRole.ROLE_USER;
    }

    private boolean isCurrentUser(User user) {
        return currentUserService.getCurrentUser()
                .map(currentUser -> currentUser.getId().equals(user.getId()))
                .orElse(false);
    }

    private void initializeUserReferences(User user) {
        Hibernate.initialize(user.getProjectManager());
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}