package com.marcos.clientesscout.controller;

import com.marcos.clientesscout.model.User;
import com.marcos.clientesscout.repository.UserRepository;
import com.marcos.clientesscout.service.JwtService;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * Controlador de autenticación.
 *
 * SEGURIDAD — Almacenamiento del JWT:
 * El token ya NO se devuelve en el cuerpo de la respuesta ni se espera que el
 * cliente lo guarde en localStorage/sessionStorage (vulnerable a XSS).
 *
 * En su lugar, el backend emite el JWT como cookie con los atributos:
 *   - HttpOnly: el token no es accesible desde JavaScript.
 *   - Secure:   solo se envía por HTTPS (desactivar solo en desarrollo local).
 *   - SameSite=Strict: el navegador no envía la cookie en peticiones cross-site,
 *                      lo que elimina la necesidad de tokens CSRF adicionales.
 *   - Path=/api: la cookie solo se adjunta a peticiones a la API.
 *
 * El frontend no necesita inyectar ninguna cabecera Authorization; basta con
 * que las peticiones HTTP usen `withCredentials: true`.
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

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

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
            return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        return ResponseEntity.status(201).body(Map.of("message", "User created successfully"));
    }

    /**
     * Autentica al usuario y emite el JWT como cookie httpOnly.
     *
     * El cuerpo de la respuesta solo devuelve el email (dato no sensible) para
     * que el frontend pueda identificar al usuario en la UI. El token en sí
     * nunca abandona el contexto httpOnly.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                                   jakarta.servlet.http.HttpServletResponse response) {
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

            // Emitir el JWT como cookie httpOnly — no se expone en el body
            ResponseCookie jwtCookie = ResponseCookie.from("jwt", token)
                    .httpOnly(true)           // inaccesible desde JavaScript
                    .secure(true)             // solo HTTPS; usar false en desarrollo HTTP local
                    .sameSite("Strict")       // no se envía en peticiones cross-site (anti-CSRF)
                    .path("/api")             // solo se adjunta a rutas de la API
                    .maxAge(Duration.ofMillis(jwtService.getExpiration()))
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

            // Devolver el email (no el token) para uso en la UI
            return ResponseEntity.ok(Map.of("email", email));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    /**
     * Cierra la sesión del usuario eliminando la cookie JWT.
     *
     * La cookie se invalida enviando una nueva cookie con el mismo nombre,
     * maxAge=0 y value vacío, lo que hace que el navegador la elimine.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletResponse response) {
        ResponseCookie clearCookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api")
                .maxAge(0)   // maxAge=0 instruye al navegador a eliminar la cookie
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
