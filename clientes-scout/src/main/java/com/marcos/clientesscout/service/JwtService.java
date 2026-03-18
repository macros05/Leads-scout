package com.marcos.clientesscout.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

/**
 * Servicio de generación y validación de tokens JWT.
 *
 * Algoritmo: HMAC-SHA256 (HS256).
 * El subject del token es el email del usuario.
 *
 * REQUISITOS DE SEGURIDAD:
 * - jwt.secret debe tener al menos 32 caracteres (256 bits) para HS256.
 *   Si es menor, Keys.hmacShaKeyFor lanzará WeakKeyException al arrancar.
 * - jwt.expiration en milisegundos (86400000 = 24 horas por defecto).
 * - El secret nunca debe estar en el código fuente ni en git; usar variables de entorno.
 *
 * LIMITACIÓN: este servicio no implementa revocación de tokens (blacklist).
 * Un token válido seguirá funcionando hasta su expiración aunque el usuario
 * haga logout. Para uso en producción considerar implementar un blacklist en Redis.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Construye la clave de firma a partir del secret configurado.
     * Se recrea en cada llamada para simplificar; en contextos de alta carga
     * podría cachearse como campo final inicializado en @PostConstruct.
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Genera un token JWT firmado para el email dado.
     *
     * @param email Email del usuario autenticado (se usará como subject)
     * @return Token JWT compacto (formato header.payload.signature)
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extrae el email (subject) de un token JWT.
     * Lanza JwtException si el token es inválido o ha expirado.
     *
     * @param token Token JWT
     * @return Email del usuario
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token)
                .getBody().getSubject();
    }

    /**
     * Valida la firma y la expiración del token.
     *
     * @param token Token JWT
     * @return true si el token es válido y no ha expirado, false en caso contrario
     */
    public boolean isValid(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey()).build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
