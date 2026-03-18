import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../environments/enviroments';
import { LoginRequest, RegisterRequest, AuthResponse } from '../models/auth/auth.interface';

/**
 * Servicio de autenticación.
 *
 * Gestiona el flujo de login/registro y el almacenamiento del token JWT.
 *
 * LIMITACIÓN DE SEGURIDAD: el token se almacena en localStorage, que es accesible
 * desde JavaScript en la misma página. Si hubiera una vulnerabilidad XSS, el token
 * podría ser robado. Para mayor seguridad en producción considerar usar cookies
 * httpOnly+Secure+SameSite=Strict en su lugar.
 *
 * LIMITACIÓN: isLoggedIn() solo comprueba que el token existe en localStorage,
 * no que sea válido o que no haya expirado. Una petición con token expirado
 * recibirá un 401 del backend y deberá manejarse en el interceptor o en el guard.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private apiUrl = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
      tap(response => localStorage.setItem('token', response.token))
    );
  }

  register(data: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data);
  }

  logout(): void {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  /** Comprueba si hay un token almacenado (no valida expiración ni firma). */
  isLoggedIn(): boolean {
    return !!this.getToken();
  }
}
