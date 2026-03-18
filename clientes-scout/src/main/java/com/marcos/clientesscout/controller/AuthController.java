package com.marcos.clientesscout.controller;

import com.marcos.clientesscout.model.User;
import com.marcos.clientesscout.repository.UserRepository;
import com.marcos.clientesscout.service.JwtService;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de autenticación: registro e inicio de sesión con JWT.
 *
 * Endpoints públicos (no requieren token):
 * - POST /api/auth/register
 * - POST /api/auth/login
 *
 * MEJORA DE SEGURIDAD PENDIENTE:
 * - Añadir rate limiting (p.ej. Bucket4j) para prevenir ataques de fuerza bruta.
 * - Añadir verificación de email al registrar para evitar cuentas falsas.
 * - Considerar bloqueo temporal tras N intentos fallidos de login.
 */
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

    /**
     * Registra un nuevo usuario.
     *
     * Valida que email y contraseña no sean nulos/vacíos antes de procesar,
     * ya que el body se recibe como Map sin anotaciones @Valid.
     *
     * @param body JSON con campos "email" y "password"
     * @return 201 Created si el registro fue exitoso, 400 si los datos son inválidos
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        // Validación de campos obligatorios (el Map no tiene @Valid automático)
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }
        if (password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
        }
        if (password.length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters"));
        }

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

    /**
     * Autentica un usuario y devuelve un JWT.
     *
     * Spring Security's AuthenticationManager se encarga de verificar credenciales
     * contra la base de datos (via UserDetailsServiceImpl + BCrypt).
     *
     * @param body JSON con campos "email" y "password"
     * @return 200 con token JWT, o 401 si las credenciales son incorrectas
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            String token = jwtService.generateToken(email);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }
}
