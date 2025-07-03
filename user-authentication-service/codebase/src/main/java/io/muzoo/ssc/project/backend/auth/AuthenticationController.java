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
            @RequestParam String password,
            HttpServletResponse response
    ) {
        try {
            // Get the user object
            User user = loginService.login(username, password);

            // Generate the token
            String authToken = jwtUtil.generateToken(user.getUsername());

            // Generate short web socket token (5 minutes)
            String wsToken = jwtUtil.generateShortLivedToken(user.getUsername(), 5 * 60 * 1000);

            System.out.println("User " + username + " logged in successfully");

            return ResponseEntity.ok(
                    SimpleResponseDTO.builder()
                            .success(true)
                            .message("Login successful")
                            .data(Map.of(
                                    "token", authToken,
                                    "wsToken", wsToken,
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

    @GetMapping("/api/ws-token")
    public ResponseEntity<SimpleResponseDTO> refreshWsToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(SimpleResponseDTO.builder()
                                .success(false)
                                .message("Authorization header missing or invalid")
                                .build());
            }
            String jwt = authHeader.substring(7);
            if (jwtUtil.validateJwtToken(jwt)) {
                String username = jwtUtil.getUsernameFromToken(jwt);
                String newWsToken = jwtUtil.generateShortLivedToken(username, 5 * 60 * 1000);
                System.out.println("Generated new wsToken for user: " + username);
                return ResponseEntity.ok(
                        SimpleResponseDTO.builder()
                                .success(true)
                                .message("WebSocket token generated")
                                .data(Map.of("wsToken", newWsToken))
                                .build()
                );
            } else {
                System.out.println("Invalid or expired JWT for wsToken refresh");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(SimpleResponseDTO.builder()
                                .success(false)
                                .message("Invalid or expired JWT")
                                .build());
            }
        } catch (Exception e) {
            System.out.println("Error generating wsToken: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(SimpleResponseDTO.builder()
                            .success(false)
                            .message("Error generating WebSocket token: " + e.getMessage())
                            .build());
        }
    }

}
