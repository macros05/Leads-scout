import { Component, OnInit, signal } from '@angular/core'; // Importa signal
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LeadService } from '../../services/lead.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css',
})
export class DashboardComponent implements OnInit {
  // 1. Definimos las variables como Signals
  stats = signal({ total: 0, critical: 0, optimization: 0, offline: 0 });
  isLoading = signal(true);

  constructor(private leadService: LeadService) {}

  ngOnInit(): void {
    this.leadService.getLeads().subscribe({
      next: (data) => {
        // 2. Actualizamos los valores usando .set()
        this.stats.set({
          total: data.length,
          critical: data.filter(l => l.performanceScore < 40).length,
          optimization: data.filter(l => l.performanceScore >= 40 && l.performanceScore < 70).length,
          offline: data.filter(l => l.performanceScore === 0).length
        });

        // 3. Notificamos que la carga ha terminado
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Fetch error:', err);
        this.isLoading.set(false);
      }
    });
  }
}
