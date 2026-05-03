package com.uniovi.estimacion.config;

import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.services.users.UserService;
import com.uniovi.estimacion.common.codes.SizeTechniqueCodes;
import com.uniovi.estimacion.common.codes.SizeUnitCodes;
import com.uniovi.estimacion.services.effortconversions.TransformationFunctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final TransformationFunctionService transformationFunctionService;

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

        transformationFunctionService.createPredefinedFunction(
                "Matson, Barrett y Mellichamp (1994)",
                "Función lineal empírica para Puntos Función basada en el modelo E = 585.7 + 15.12 × FP.",
                SizeTechniqueCodes.FUNCTION_POINTS,
                SizeUnitCodes.FP,
                585.7,
                15.12
        );
    }
}