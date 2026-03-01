import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {LeadService} from '../../services/lead.service';
@Component({
  selector: 'app-scout-search',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './scout-search.component.html'
})
export class ScoutSearchComponent {
  isScouting = signal(false);
  statusMessage = signal('');

  constructor(private leadService: LeadService) {}

  startScouting(sector: string) {
    if (!sector) return;

    this.isScouting.set(true);
    this.leadService.scoutLeads(sector).subscribe({
      next: (response) => {
        this.statusMessage.set(response);
        this.isScouting.set(false);
        setTimeout(() => this.statusMessage.set(''), 5000);
      },
      error: () => this.isScouting.set(false)
    });
  }
}
