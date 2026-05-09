package com.uniovi.estimacion.web.forms.users;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountForm {

    private String email;

    private String password;

    private String passwordConfirm;
}