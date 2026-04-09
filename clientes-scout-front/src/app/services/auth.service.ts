import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/enviroments';
import { LoginRequest, RegisterRequest, AuthResponse } from '../models/auth/auth.interface';

/**
 * Servicio de autenticación.
 *
 * SEGURIDAD — Almacenamiento del JWT:
 * El token JWT ya NO se almacena en localStorage ni sessionStorage.
 * El backend lo emite como cookie httpOnly+Secure+SameSite=Strict, lo que significa:
 *   - No es accesible desde JavaScript (protegido contra XSS).
 *   - Solo viaja por HTTPS.
 *   - No se envía en peticiones cross-site (protegido contra CSRF).
 *
 * El frontend solo guarda en localStorage el email del usuario (dato no sensible)
 * con el único propósito de mostrar quién está logueado en la UI y controlar
 * la navegación de rutas. La autenticación real siempre la verifica el backend
 * mediante la cookie en cada petición.
 *
 * El AuthInterceptor añade `withCredentials: true` a todas las peticiones para
 * que el navegador adjunte automáticamente la cookie httpOnly.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => {
        // El JWT está en la cookie httpOnly — solo guardamos el email para la UI
        localStorage.setItem('user', response.email);
      })
    );
  }

  register(data: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data);
  }

  /**
   * Cierra la sesión:
   * 1. Llama al backend para que invalide/limpie la cookie httpOnly.
   * 2. Elimina el email de localStorage.
   * 3. Redirige al login.
   *
   * IMPORTANTE: no basta con borrar localStorage — el JWT seguiría vivo en la cookie
   * hasta que el backend la limpie con maxAge=0. Por eso la llamada al backend es obligatoria.
   */
  logout(): void {
    this.http.post(`${this.apiUrl}/logout`, {}).subscribe({
      complete: () => {
        localStorage.removeItem('user');
        this.router.navigate(['/login']);
      },
      error: () => {
        // Aunque falle la petición, limpiamos localStorage y redirigimos
        localStorage.removeItem('user');
        this.router.navigate(['/login']);
      }
    });
  }

  /** Devuelve el email del usuario actual, o null si no hay sesión activa. */
  getCurrentUser(): string | null {
    return localStorage.getItem('user');
  }

  /**
   * Comprueba si hay una sesión activa basándose en el email guardado en localStorage.
   *
   * LIMITACIÓN: esto solo indica que el usuario inició sesión en algún momento.
   * Si la cookie expiró o fue eliminada manualmente del navegador, la siguiente
   * petición a la API devolverá 401 y el interceptor deberá redirigir al login.
   */
  isLoggedIn(): boolean {
    return !!localStorage.getItem('user');
  }
}
