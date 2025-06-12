package io.muzoo.ssc.project.backend.auth;
import io.muzoo.ssc.project.backend.SimpleResponseDTO;
import io.muzoo.ssc.project.backend.User;
import io.muzoo.ssc.project.backend.UserRepository;
import io.muzoo.ssc.project.backend.user.RegisterRequestDTO;
import io.muzoo.ssc.project.backend.user.RegisterService;
import io.muzoo.ssc.project.backend.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthenticationController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/api/test")
    public String test() {
        return "If this message is shown, it means login is successful because we didn't set to permit this path.";
    }

    @PostMapping("/api/login")
    public ResponseEntity<SimpleResponseDTO> login(
            @RequestParam String username,
            @RequestParam String password
    ) {
        try {
            // Get the user object
            User user = loginService.login(username, password);

            // Generate the token
            String token = jwtUtil.generateToken(user.getUsername());

            System.out.println("User " + username + " logged in successfully");

            return ResponseEntity.ok(
                    SimpleResponseDTO.builder()
                            .success(true)
                            .message("Login successful")
                            .data(Map.of(
                                    "token", token,
                                    "username", user.getUsername(),
                                    "role", user.getRole()
                            ))
                            .build()
            );
        } catch (BadCredentialsException e) {
            System.out.println("Login failed for user " + username + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(SimpleResponseDTO.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/api/logout")
    public ResponseEntity<SimpleResponseDTO> logout() {
        // For JWT, logout is handled client-side by discarding the token
        return ResponseEntity.ok(
                SimpleResponseDTO.builder()
                        .success(true)
                        .message("Logout successful. Please remove the JWT token on the client side.")
                        .build()
        );
    }

    @PostMapping("/api/register")
    public SimpleResponseDTO register(@RequestBody RegisterRequestDTO dto) {
        try {
            registerService.register(dto.getUsername(), dto.getPassword(), "USER");
            return SimpleResponseDTO.builder()
                    .success(true)
                    .message("Registration successful")
                    .build();
        } catch (IllegalArgumentException e) {
            return SimpleResponseDTO.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
        }
    }

    @GetMapping("/api/username-check")
    public ResponseEntity<Boolean> checkUsername(@RequestParam String username) {
        boolean exists = userRepository.findFirstByUsername(username) != null;
        return ResponseEntity.ok(!exists);
    }



}
