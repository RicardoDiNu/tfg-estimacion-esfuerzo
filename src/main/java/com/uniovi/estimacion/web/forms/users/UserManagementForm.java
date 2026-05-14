package com.uniovi.estimacion.web.forms.users;

import com.uniovi.estimacion.entities.users.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserManagementForm {

    private Long id;

    private String username;

    private String email;

    private String password;

    private String passwordConfirm;

    private UserRole role;

    private Long projectManagerId;

    private Boolean enabled = true;
}