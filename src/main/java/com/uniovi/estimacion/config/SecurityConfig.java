package com.uniovi.estimacion.config;

import com.uniovi.estimacion.services.users.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 1. Recursos públicos y autenticación
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/logout",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // 2. Flujo público de estimación temporal / anónima
                        .requestMatchers(
                                "/estimate/**"
                        ).permitAll()

                        // 3. Rutas solo para admin
                        .requestMatchers(
                                "/admin/**",
                                "/users/**"
                        ).hasAuthority("ROLE_ADMIN")

                        // 4. Zona autenticada normal
                        .requestMatchers(
                                "/home",
                                "/projects/**"
                        ).authenticated()

                        // 5. Lo demás, autenticado por defecto
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            HttpSecurity http,
            PasswordEncoder passwordEncoder
    ) throws Exception {
        AuthenticationManagerBuilder authBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authBuilder
                .userDetailsService(customUserDetailsService)
                .passwordEncoder(passwordEncoder);

        return authBuilder.build();
    }

}