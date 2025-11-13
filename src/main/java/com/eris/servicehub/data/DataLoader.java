package com.eris.servicehub.data;

import com.eris.servicehub.entities.Role;
import com.eris.servicehub.repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception{

        if(roleRepository.findByName("CUSTOMER").isEmpty()){
            roleRepository.save(Role.builder().name("CUSTOMER").build());
        }

        if(roleRepository.findByName("PROVIDER").isEmpty()){
            roleRepository.save(Role.builder().name("PROVIDER").build());
        }

        if(roleRepository.findByName("ADMIN").isEmpty()){
            roleRepository.save(Role.builder().name("ADMIN").build());
        }

    }
}
