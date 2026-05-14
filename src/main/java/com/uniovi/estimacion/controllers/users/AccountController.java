package com.uniovi.estimacion.controllers.users;

import com.uniovi.estimacion.entities.users.User;
import com.uniovi.estimacion.services.users.AccountService;
import com.uniovi.estimacion.validators.users.AccountFormValidator;
import com.uniovi.estimacion.web.forms.users.AccountForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountController {

    private final AccountService accountService;
    private final AccountFormValidator accountFormValidator;

    @GetMapping
    public String getProfile(Model model) {
        Optional<User> optionalCurrentUser = accountService.findCurrentUserDetailed();

        if (optionalCurrentUser.isEmpty()) {
            return redirectToLogin();
        }

        addProfileAttributes(model, optionalCurrentUser.get());

        return "auth/profile";
    }

    @GetMapping("/edit")
    public String getEditProfile(Model model) {
        Optional<User> optionalCurrentUser = accountService.findCurrentUserDetailed();

        if (optionalCurrentUser.isEmpty()) {
            return redirectToLogin();
        }

        User currentUser = optionalCurrentUser.get();

        AccountForm form = new AccountForm();
        form.setEmail(currentUser.getEmail());

        addEditAttributes(model, currentUser, form);

        return "auth/edit-profile";
    }

    @PostMapping("/edit")
    public String editProfile(@ModelAttribute("accountForm") AccountForm form,
                              BindingResult result,
                              Model model) {
        Optional<User> optionalCurrentUser = accountService.findCurrentUserDetailed();

        if (optionalCurrentUser.isEmpty()) {
            return redirectToLogin();
        }

        User currentUser = optionalCurrentUser.get();

        accountFormValidator.validate(form, result);

        if (result.hasErrors()) {
            addEditAttributes(model, currentUser, form);
            return "auth/edit-profile";
        }

        boolean updated = accountService.updateCurrentUser(form);

        if (!updated) {
            result.reject("account.validation.update.invalid");
            addEditAttributes(model, currentUser, form);
            return "auth/edit-profile";
        }

        return "redirect:/account";
    }

    private void addProfileAttributes(Model model, User currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("projectManagerUsername", getProjectManagerUsername(currentUser));
    }

    private void addEditAttributes(Model model, User currentUser, AccountForm form) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("accountForm", form);
        model.addAttribute("projectManagerUsername", getProjectManagerUsername(currentUser));
    }

    private String getProjectManagerUsername(User user) {
        if (user.getProjectManager() == null) {
            return null;
        }

        return user.getProjectManager().getUsername();
    }

    private String redirectToLogin() {
        return "redirect:/login";
    }
}