package com.uniovi.estimacion.services.users;

import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserService userService;

    public Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        String username = authentication.getName();
        return userService.findByUsername(username);
    }

    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public Optional<String> getCurrentUsername() {
        return getCurrentUser().map(User::getUsername);
    }


    public boolean isAdmin() {
        return hasAuthority(UserRole.ROLE_ADMIN);
    }

    public boolean isProjectManager() {
        return hasAnyAuthority(
                UserRole.ROLE_PROJECT_MANAGER,
                UserRole.ROLE_USER
        );
    }

    public boolean isProjectWorker() {
        return hasAuthority(UserRole.ROLE_PROJECT_WORKER);
    }

    public boolean isAdminOrProjectManager() {
        return isAdmin() || isProjectManager();
    }

    private boolean hasAuthority(UserRole role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role.getAuthority()::equals);
    }

    private boolean hasAnyAuthority(UserRole... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        for (UserRole role : roles) {
            boolean hasRole = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(role.getAuthority()::equals);

            if (hasRole) {
                return true;
            }
        }

        return false;
    }
}