package com.example.securingweb.auth;

import com.example.securingweb.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class LoginService {

    private final AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final SecurityContextRepository securityContextRepository;

    public LoginService(PasswordEncoder passwordEncoder, UserRepository userRepository, SecurityContextRepository securityContextRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.securityContextRepository = securityContextRepository;
    }

    public void login(String username, String credential, HttpServletRequest request, HttpServletResponse response) {
        AbstractAuthenticationToken authRequest = new PreAuthenticatedAuthenticationToken(username, credential);
        String hashedPassword = userRepository.findFirstByUsername(username).getPassword();
        if (!passwordEncoder.matches(credential, hashedPassword)) {
            throw new BadCredentialsException("Username or password is incorrect.");
        }
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
        authRequest.setAuthenticated(true);
        Authentication authResult = loadUserDetails(authRequest, username);
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authResult);
        securityContextRepository.saveContext(context, request, response);
    }


    private UsernamePasswordAuthenticationToken loadUserDetails(Authentication authentication, String username) {
        List<GrantedAuthority> grantedAuths = new ArrayList<>();
        grantedAuths.add(new SimpleGrantedAuthority("ROLE_USER"));

        User user = new User(username, "", Collections.unmodifiableCollection(grantedAuths));

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                user,
                authentication.getCredentials(),
                grantedAuths);
        token.setDetails(authentication.getDetails());
        return token;
    }

}
