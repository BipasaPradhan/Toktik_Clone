package com.example.securingweb.whoami;

import com.example.securingweb.User;
import com.example.securingweb.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoamiController {
    private final UserRepository userRepository;

    public WhoamiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/whoami")
    public WhoamiDTO whoami() {
        // Put try around the statement because we use nested dot notation which could raise a NullPointException.
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal != null && principal instanceof org.springframework.security.core.userdetails.User) {
                // user is logged in.
                org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) principal;
                User u = userRepository.findFirstByUsername(user.getUsername());

                return WhoamiDTO.builder()
                        .loggedIn(true)
                        .name(u.getUsername()) // we don't have a name field so i use username but you can add it yourself.
                        .role(u.getRole())
                        .username(u.getUsername())
                        .build();
            }
        } catch (Exception e) {
        }
        // user is not logged in.
        return WhoamiDTO.builder()
                .loggedIn(false)
                .build();
    }
}
