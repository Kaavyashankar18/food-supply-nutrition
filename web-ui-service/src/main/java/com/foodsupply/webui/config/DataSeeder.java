package com.foodsupply.webui.config;

import com.foodsupply.webui.entity.AppUser;
import com.foodsupply.webui.entity.Role;
import com.foodsupply.webui.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        save("admin", "admin123", "Anitha Raman (Administrator)", Role.ADMIN);
        save("store", "store123", "Suresh Kumar (Store Manager)", Role.STORE_MANAGER);
        save("procure", "procure123", "Priya Natarajan (Procurement Officer)", Role.PROCUREMENT_OFFICER);
        save("field", "field123", "Meena Krishnan (Field Officer)", Role.FIELD_OFFICER);
        log.info("Seeded 4 users (admin, store, procure, field)");
    }

    private void save(String username, String rawPassword, String fullName, Role role) {
        users.save(AppUser.builder()
                .username(username)
                .password(encoder.encode(rawPassword))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build());
    }
}
