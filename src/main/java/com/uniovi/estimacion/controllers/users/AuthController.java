package com.uniovi.estimacion.controllers.users;

import com.uniovi.estimacion.services.security.SecurityService;
import com.uniovi.estimacion.services.users.UserService;
import com.uniovi.estimacion.validators.users.SignUpFormValidator;
import com.uniovi.estimacion.web.forms.users.SignUpForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final SignUpFormValidator signUpFormValidator;
    private final SecurityService securityService;

    @GetMapping("/login")
    public String getLogin() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String getSignup(Model model) {
        model.addAttribute("signUpForm", new SignUpForm());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@ModelAttribute("signUpForm") SignUpForm signUpForm,
                         BindingResult result,
                         Model model,
                         jakarta.servlet.http.HttpServletRequest request) {
        signUpFormValidator.validate(signUpForm, result);

        if (result.hasErrors()) {
            model.addAttribute("signUpForm", signUpForm);
            return "auth/signup";
        }

        var createdUser = userService.registerUser(signUpForm);
        securityService.autoLogin(createdUser.getUsername(), signUpForm.getPassword(), request);

        return "redirect:/";
    }
}