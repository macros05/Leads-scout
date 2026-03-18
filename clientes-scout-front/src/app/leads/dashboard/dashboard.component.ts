import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LeadService } from '../../services/lead.service';

/**
 * Componente de dashboard.
 * Muestra estadísticas agregadas de todos los leads cargados desde el backend.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {

  stats = signal({ total: 0, critical: 0, optimization: 0, offline: 0 });
  isLoading = signal(true);

  constructor(private leadService: LeadService) {}

  ngOnInit(): void {
    this.leadService.getLeads().subscribe({
      next: (data) => {
        // performanceScore es opcional en la interfaz actualizada; se usa ?? 0 como fallback
        this.stats.set({
          total:       data.length,
          critical:    data.filter(l => (l.performanceScore ?? 0) < 40).length,
          optimization:data.filter(l => {
            const s = l.performanceScore ?? 0;
            return s >= 40 && s < 70;
          }).length,
          offline:     data.filter(l => (l.performanceScore ?? 0) === 0).length
        });
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load leads for dashboard:', err);
        this.isLoading.set(false);
      }
    });
  }
}
