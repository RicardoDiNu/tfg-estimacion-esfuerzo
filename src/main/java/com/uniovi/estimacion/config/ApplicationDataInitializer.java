package com.uniovi.estimacion.config;

import com.uniovi.estimacion.common.codes.SizeTechniqueCodes;
import com.uniovi.estimacion.common.codes.SizeUnitCodes;
import com.uniovi.estimacion.entities.users.UserRole;
import com.uniovi.estimacion.services.effortconversions.transformationfunctions.TransformationFunctionService;
import com.uniovi.estimacion.services.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ApplicationDataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final TransformationFunctionService transformationFunctionService;

    @Value("${app.initial-admin.enabled:true}")
    private boolean initialAdminEnabled;

    @Value("${app.initial-admin.username:admin}")
    private String initialAdminUsername;

    @Value("${app.initial-admin.email:admin@estimacion.local}")
    private String initialAdminEmail;

    @Value("${app.initial-admin.password:admin123}")
    private String initialAdminPassword;

    @Override
    public void run(String... args) {
        createInitialAdminIfConfigured();
        createPredefinedTransformationFunctions();
    }

    private void createInitialAdminIfConfigured() {
        if (!initialAdminEnabled) {
            return;
        }

        if (!StringUtils.hasText(initialAdminUsername)
                || !StringUtils.hasText(initialAdminEmail)
                || !StringUtils.hasText(initialAdminPassword)) {
            throw new IllegalStateException("La configuración del administrador inicial está incompleta.");
        }

        if (!userService.existsByUsername(initialAdminUsername)) {
            userService.createUser(
                    initialAdminUsername,
                    initialAdminEmail,
                    initialAdminPassword,
                    UserRole.ROLE_ADMIN
            );
        }
    }

    private void createPredefinedTransformationFunctions() {
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