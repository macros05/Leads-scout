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

/**
 * Filtro JWT que se ejecuta una única vez por petición HTTP.
 *
 * Flujo:
 * 1. Lee el header "Authorization: Bearer <token>".
 * 2. Si el token existe y es válido, extrae el email del subject.
 * 3. Carga el UserDetails del usuario y establece la autenticación en el SecurityContext.
 * 4. Si no hay token o es inválido, continúa sin autenticar (Spring Security denegará
 *    el acceso a rutas protegidas en la siguiente etapa del pipeline).
 *
 * OncePerRequestFilter garantiza que el filtro no se ejecute dos veces en la misma petición
 * (puede ocurrir en redirecciones internas con forward/include en ciertos servidores).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

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

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtService.isValid(token)) {
                String email = jwtService.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Creamos el objeto de autenticación con los roles del usuario.
                // credentials se pasa como null porque ya validamos con JWT (no necesitamos la contraseña aquí).
                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // Si el token es inválido/expirado, simplemente no se autentica al usuario.
            // La petición continuará y Spring Security la rechazará si la ruta requiere autenticación.
        }

        filterChain.doFilter(request, response);
    }
}
