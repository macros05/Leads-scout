package com.marcos.clientesscout.security;

import com.marcos.clientesscout.service.JwtService;
import com.marcos.clientesscout.service.UserDetailsServiceImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Filtro JWT que se ejecuta una única vez por petición HTTP.
 *
 * ESTRATEGIA DE EXTRACCIÓN DEL TOKEN (en orden de prioridad):
 * 1. Cookie httpOnly "jwt" — método principal y más seguro.
 *    El navegador la envía automáticamente si la petición incluye withCredentials: true.
 * 2. Header "Authorization: Bearer ..." — fallback para clientes sin cookie
 *    (Postman, herramientas CLI, integraciones externas).
 *
 * Si ninguna fuente proporciona un token válido, la petición continúa sin autenticar
 * y Spring Security denegará el acceso a rutas protegidas.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String JWT_COOKIE_NAME = "jwt";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Prioridad 1: cookie httpOnly (inaccesible desde JS, resistente a XSS)
        String token = extractTokenFromCookie(request);

        // Prioridad 2: header Authorization (fallback para herramientas de prueba)
        if (token == null) {
            token = extractTokenFromHeader(request);
        }

        if (token != null && jwtService.isValid(token)) {
            String email = jwtService.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            // credentials=null porque la autenticación ya fue verificada por JWT
            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el JWT de la cookie httpOnly "jwt".
     * Devuelve null si la cookie no existe o la petición no tiene cookies.
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        return Arrays.stream(cookies)
                .filter(c -> JWT_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Extrae el JWT del header "Authorization: Bearer <token>".
     * Devuelve null si el header no está presente o no tiene el formato Bearer.
     */
    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
