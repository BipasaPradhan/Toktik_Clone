package io.muzoo.ssc.project.backend.auth;
import io.muzoo.ssc.project.backend.SimpleResponseDTO;
import io.muzoo.ssc.project.backend.UserRepository;
import io.muzoo.ssc.project.backend.user.RegisterRequestDTO;
import io.muzoo.ssc.project.backend.user.RegisterService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthenticationController {

    @Autowired
    private LoginService loginService;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private UserRepository userRepository;

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
