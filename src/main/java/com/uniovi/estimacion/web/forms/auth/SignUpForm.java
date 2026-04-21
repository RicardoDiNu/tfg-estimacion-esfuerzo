package com.uniovi.estimacion.web.forms.auth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SignUpForm {

    private String username;
    private String email;
    private String password;
    private String passwordConfirm;
}