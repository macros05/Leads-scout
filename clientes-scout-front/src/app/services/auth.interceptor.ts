import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Interceptor de autenticación basado en cookies.
 *
 * CAMBIO RESPECTO AL DISEÑO ANTERIOR:
 * Antes: leía el JWT de localStorage e inyectaba "Authorization: Bearer <token>"
 *        en cada petición. Esto exponía el token a cualquier script en la página (XSS).
 *
 * Ahora: simplemente añade `withCredentials: true` a todas las peticiones.
 *        Esto indica al navegador que adjunte automáticamente las cookies httpOnly
 *        al origen del backend, sin que el código JavaScript tenga acceso al token.
 *
 * REQUISITO: el backend debe tener CORS configurado con `allowCredentials(true)`
 * y un origen explícito (no wildcard "*"), que ya está configurado en SecurityConfig.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const secureReq = req.clone({
    withCredentials: true
  });
  return next(secureReq);
};
