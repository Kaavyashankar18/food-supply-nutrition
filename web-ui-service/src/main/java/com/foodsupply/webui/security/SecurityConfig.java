package com.foodsupply.webui.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/error", "/error/**", "/css/**", "/js/**", "/images/**").permitAll()
                // only admin deletes beneficiaries
                .requestMatchers(HttpMethod.POST, "/beneficiary/*/delete").hasRole("ADMIN")
                .requestMatchers("/beneficiary/**").hasAnyRole("ADMIN", "FIELD_OFFICER")
                // everybody can look at food items, only admin / store manager change them
                .requestMatchers(HttpMethod.POST, "/food/**").hasAnyRole("ADMIN", "STORE_MANAGER")
                .requestMatchers("/food/new", "/food/*/edit", "/food/categories/**").hasAnyRole("ADMIN", "STORE_MANAGER")
                .requestMatchers("/programme/**").hasAnyRole("ADMIN", "FIELD_OFFICER", "STORE_MANAGER")
                .requestMatchers("/procurement/**").hasAnyRole("ADMIN", "PROCUREMENT_OFFICER", "STORE_MANAGER")
                .requestMatchers("/inventory/**").hasAnyRole("ADMIN", "STORE_MANAGER", "PROCUREMENT_OFFICER")
                .requestMatchers("/distribution/**").hasAnyRole("ADMIN", "STORE_MANAGER", "FIELD_OFFICER")
                .requestMatchers("/quality/**").hasAnyRole("ADMIN", "STORE_MANAGER")
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "STORE_MANAGER", "PROCUREMENT_OFFICER")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll());
        return http.build();
    }
}
