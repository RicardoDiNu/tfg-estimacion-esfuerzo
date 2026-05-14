package com.uniovi.estimacion.validators.users;

import com.uniovi.estimacion.services.users.AccountService;
import com.uniovi.estimacion.web.forms.users.AccountForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;

@Component
@RequiredArgsConstructor
public class AccountFormValidator {

    private final AccountService accountService;

    public void validate(AccountForm form, Errors errors) {
        if (!StringUtils.hasText(form.getEmail())) {
            errors.rejectValue("email", "account.validation.email.empty");
        } else if (accountService.existsEmailUsedByAnotherUser(form.getEmail())) {
            errors.rejectValue("email", "account.validation.email.duplicate");
        }

        boolean passwordHasText = StringUtils.hasText(form.getPassword());
        boolean passwordConfirmHasText = StringUtils.hasText(form.getPasswordConfirm());

        if ((passwordHasText || passwordConfirmHasText)
                && !String.valueOf(form.getPassword()).equals(String.valueOf(form.getPasswordConfirm()))) {
            errors.rejectValue("passwordConfirm", "account.validation.passwordConfirm.mismatch");
        }
    }
}