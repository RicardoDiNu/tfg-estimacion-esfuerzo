package com.uniovi.estimacion.services.users;

import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.repositories.users.UserRepository;
import com.uniovi.estimacion.web.forms.auth.SignUpForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(normalize(username));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalize(email));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(normalize(username));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(normalize(email));
    }

    @Transactional
    public User createUser(String username, String email, String rawPassword, UserRole role) {
        User user = new User();
        user.setUsername(normalize(username));
        user.setEmail(normalize(email));
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role != null ? role : UserRole.ROLE_USER);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    @Transactional
    public User registerUser(SignUpForm form) {
        User user = new User();
        user.setUsername(normalize(form.getUsername()));
        user.setEmail(normalize(form.getEmail()));
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(UserRole.ROLE_USER);
        user.setEnabled(true);

        return userRepository.save(user);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}