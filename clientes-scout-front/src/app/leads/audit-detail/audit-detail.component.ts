import { Component, OnInit, signal } from '@angular/core';
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import { CommonModule } from '@angular/common';
import { LeadService } from '../../services/lead.service';
import { Lead } from '../../models/lead/lead.interface';

@Component({
  selector: 'app-audit-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './audit-detail.component.html'
})
export class AuditDetailComponent implements OnInit {
  // Signal para el cliente seleccionado
  lead = signal<Lead | null>(null);
  isLoading = signal(true);

  constructor(
    private route: ActivatedRoute,
    private leadService: LeadService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.leadService.getLeadById(id).subscribe({
        next: (data) => {
          this.lead.set(data);
          this.isLoading.set(false);
        },
        error: () => this.isLoading.set(false)
      });
    }
  }

  confirmDeletion() {
    const leadData = this.lead();
    if (leadData && confirm(`Are you sure you want to delete ${leadData.name}?`)) {
      this.leadService.deleteLead(leadData.id).subscribe({
        next: () => {
          this.router.navigate(['/leads']);
        },
        error: (err) => console.error('Delete failed', err)
      });
    }
  }

  generateMailto(): string {
    const data = this.lead();
    if (!data) return '';

    const subject = encodeURIComponent(`Performance Audit for ${data.name} - Opportunity for Growth`);
    const body = encodeURIComponent(
      `Hi ${data.name} Team,\n\n` +
      `I've recently conducted a performance audit on your website (${data.website}) and your current score is ${data.performanceScore}/100.\n\n` +
      `I found some critical issues that are likely affecting your customer conversion rate. I'd love to share the full report and discuss how we can fix this.\n\n` +
      `Best regards,\nLead Scout Team`
    );

    return `mailto:?subject=${subject}&body=${body}`;
  }
}
