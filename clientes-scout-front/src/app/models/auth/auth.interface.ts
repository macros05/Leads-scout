export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
}

/**
 * Respuesta del endpoint POST /api/auth/login.
 *
 * NOTA: el JWT ya NO se devuelve en el cuerpo de la respuesta.
 * El backend lo emite como cookie httpOnly (inaccesible desde JS).
 * El body solo contiene el email para uso en la UI (mostrar usuario, etc.).
 */
export interface AuthResponse {
  email: string;
}
