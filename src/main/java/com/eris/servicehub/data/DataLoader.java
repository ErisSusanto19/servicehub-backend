package com.eris.servicehub.data;

import com.eris.servicehub.entities.Profile;
import com.eris.servicehub.entities.Role;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.repositories.RoleRepository;
import com.eris.servicehub.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@org.springframework.context.annotation.Profile("!test")
public class DataLoader implements CommandLineRunner {

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception{

        createRoleIfNotFound("CUSTOMER");
        createRoleIfNotFound("PROVIDER");
        Role adminRole = createRoleIfNotFound("ADMIN");

        createAdminUserIfNotFound(adminRole);

    }

    private Role createRoleIfNotFound(String name) {
        return roleRepository.findByName(name)
                .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
    }

    private void createAdminUserIfNotFound(Role adminRole) {
        String adminEmail = "admin@servicehub.com";
        String adminPassword = "password123";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User adminUser = User.builder()
                    .name("Administrator")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(Set.of(adminRole))
                    .enabled(true)
                    .build();

            Profile adminProfile = new Profile();
            adminProfile.setUser(adminUser);
            adminUser.setProfile(adminProfile);

            userRepository.save(adminUser);
            System.out.println("Created ADMIN user: " + adminEmail);
        }
    }
}
