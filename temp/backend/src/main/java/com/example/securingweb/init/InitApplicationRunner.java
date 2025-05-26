package com.example.securingweb.init;

import com.example.securingweb.User;
import com.example.securingweb.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitApplicationRunner implements ApplicationRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public InitApplicationRunner(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // add default admin user and set its password if missing
        try {
            User admin = userRepository.findFirstByUsername("admin");
            if (admin == null) {
                admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("123456"));
                admin.setRole("USER");
                userRepository.save(admin);
            }
        } catch (DataIntegrityViolationException e) {
            System.out.println("Admin already exists");
        }
    }

}