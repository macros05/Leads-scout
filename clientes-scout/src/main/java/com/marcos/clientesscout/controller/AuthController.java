package com.marcos.clientesscout.controller;

import com.marcos.clientesscout.model.User;
import com.marcos.clientesscout.repository.UserRepository;
import com.marcos.clientesscout.service.JwtService;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already registered"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        return ResponseEntity.status(201)
                .body(Map.of("message", "User created successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            body.get("email"),
                            body.get("password")
                    )
            );
            String token = jwtService.generateToken(body.get("email"));
            return ResponseEntity.ok(Map.of("token", token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }
}