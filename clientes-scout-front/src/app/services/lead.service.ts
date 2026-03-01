import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {Lead} from '../models/lead/lead.interface';
import {environment} from '../environments/enviroments';
@Injectable({
  providedIn: 'root'
})
export class LeadService {
  private readonly URL = `${environment.apiUrl}/clients`;

  constructor(private http: HttpClient) { }

  getLeads(): Observable<Lead[]> {
    return this.http.get<Lead[]>(this.URL);
  }

  getLeadById(id: string): Observable<Lead> {
    return this.http.get<Lead>(`${this.URL}/${id}`);
  }

  scoutLeads(sector: string): Observable<string> {
    return this.http.post(`${this.URL}/scout?sector=${sector}`, {}, { responseType: 'text' });
  }

  deleteLead(id: number): Observable<void> {
    return this.http.delete<void>(`${this.URL}/${id}`);
  }
}
