import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs'; // 1. Importa Observable
import { LeadService } from '../../services/lead.service';
import { Lead } from '../../models/lead/lead.interface';
import {RouterLink} from '@angular/router'; // Mantén tu ruta actual

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
