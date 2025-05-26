package io.muzoo.ssc.project.backend.user;

import io.muzoo.ssc.project.backend.User;
import io.muzoo.ssc.project.backend.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(String username, String password, String role) throws IllegalArgumentException {
        // Check if username already exists
        if (userRepository.findFirstByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // Hash the password
        user.setRole(role != null ? role : "USER"); // Default role if not provided

        // Save user to database
        userRepository.save(user);
    }
}