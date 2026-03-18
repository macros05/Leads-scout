import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Lead } from '../models/lead/lead.interface';
import { environment } from '../environments/enviroments';

/**
 * Servicio de acceso a la API de leads.
 *
 * Todos los endpoints requieren autenticación JWT (inyectada por AuthInterceptor).
 */
@Injectable({
  providedIn: 'root'
})
export class LeadService {

  private readonly URL = `${environment.apiUrl}/clients`;

  constructor(private http: HttpClient) {}

  getLeads(): Observable<Lead[]> {
    return this.http.get<Lead[]>(this.URL);
  }

  getLeadById(id: string): Observable<Lead> {
    return this.http.get<Lead>(`${this.URL}/${id}`);
  }

  /**
   * Inicia el proceso de scouting para un sector.
   *
   * SEGURIDAD: el parámetro sector se pasa via HttpParams para que Angular
   * lo codifique correctamente (encodeURIComponent). Antes se concatenaba
   * directamente en la URL: ?sector=${sector}, lo que permitía inyectar
   * caracteres especiales como & para añadir parámetros adicionales no deseados.
   */
  scoutLeads(sector: string): Observable<string> {
    const params = new HttpParams().set('sector', sector);
    return this.http.post(`${this.URL}/scout`, {}, { params, responseType: 'text' });
  }

  deleteLead(id: number): Observable<void> {
    return this.http.delete<void>(`${this.URL}/${id}`);
  }
}
