package com.uniovi.estimacion.config;

import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.services.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) {
        if (!userService.existsByUsername("admin")) {
            userService.createUser(
                    "admin",
                    "admin@estimacion.local",
                    "admin123",
                    UserRole.ROLE_ADMIN
            );
        }
    }
}