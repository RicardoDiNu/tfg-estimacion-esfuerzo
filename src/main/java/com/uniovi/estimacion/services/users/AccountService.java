package com.uniovi.estimacion.services.users;

import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.web.forms.users.AccountForm;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findCurrentUserDetailed() {
        return currentUserService.getCurrentUser()
                .map(user -> {
                    Hibernate.initialize(user.getProjectManager());
                    return user;
                });
    }

    public boolean existsEmailUsedByAnotherUser(String email) {
        Optional<User> optionalCurrentUser = currentUserService.getCurrentUser();

        if (optionalCurrentUser.isEmpty()) {
            return false;
        }

        String normalizedEmail = normalize(email);

        return userRepository.existsByEmailAndIdNot(
                normalizedEmail,
                optionalCurrentUser.get().getId()
        );
    }

    @Transactional
    public boolean updateCurrentUser(AccountForm form) {
        Optional<User> optionalCurrentUser = currentUserService.getCurrentUser();

        if (optionalCurrentUser.isEmpty()) {
            return false;
        }

        User currentUser = optionalCurrentUser.get();

        currentUser.setEmail(normalize(form.getEmail()));

        if (StringUtils.hasText(form.getPassword())) {
            currentUser.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        userRepository.save(currentUser);

        return true;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}