import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { LeadService } from '../../services/lead.service';
import { Lead } from '../../models/lead/lead.interface';
import { RouterLink } from '@angular/router';

/**
 * Componente de listado de leads.
 * Muestra todos los prospectos en una tabla con su puntuación de rendimiento y estado.
 */
@Component({
  selector: 'app-leads-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './leads-list.component.html'
})
export class LeadsListComponent implements OnInit {

  leads$!: Observable<Lead[]>;

  constructor(private leadService: LeadService) {}

  ngOnInit(): void {
    this.leads$ = this.leadService.getLeads();
  }
}
