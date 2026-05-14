package com.uniovi.estimacion.entities.users;

public enum UserRole {

    ROLE_ADMIN,
    ROLE_PROJECT_MANAGER,
    ROLE_PROJECT_WORKER,

    /**
     * Rol antiguo mantenido temporalmente por compatibilidad.
     * Los usuarios antiguos con ROLE_USER se tratan como jefes de proyecto.
     */
    ROLE_USER;

    public String getAuthority() {
        return name();
    }
}