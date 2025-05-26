package com.example.securingweb.auth;

import com.example.securingweb.SimpleResponseDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private RegisterService registerService;

    @GetMapping("/api/test")
    public String test() {
        return "If this message is shown, it means login is successful because we didn't set to permit this path.";
    }

    @PostMapping("/api/login")
    public SimpleResponseDTO login(@RequestParam String username, @RequestParam String password, HttpServletRequest request, HttpServletResponse response) {

        try {
            loginService.login(username, password, request, response);
            return SimpleResponseDTO
                    .builder()
                    .success(true)
                    .message("You are logged in successfully.")
                    .build();
        } catch (AuthenticationException e) {
            return SimpleResponseDTO
                    .builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }

    @GetMapping("/api/logout")
    public SimpleResponseDTO logout(HttpServletRequest request) {
        try {
            request.logout();
            return SimpleResponseDTO
                    .builder()
                    .success(true)
                    .message("You are successfully logged out")
                    .build();
        } catch (ServletException e) {
            return SimpleResponseDTO
                    .builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }

    @PostMapping("/api/register")
    public SimpleResponseDTO register(@RequestParam String username, @RequestParam String password, @RequestParam(required = false) String role) {
        try {
            registerService.register(username, password, role);
            return SimpleResponseDTO.builder()
                    .success(true)
                    .message("User registered successfully")
                    .build();
        } catch (IllegalArgumentException e) {
            return SimpleResponseDTO.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }

}