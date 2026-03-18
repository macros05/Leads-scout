import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { LeadService } from '../../services/lead.service';
import { Lead } from '../../models/lead/lead.interface';

/**
 * Componente de detalle de auditoría de un lead.
 *
 * Muestra el informe completo: puntuación PageSpeed, tecnologías detectadas,
 * problemas encontrados por IA y acciones disponibles (enviar email, eliminar).
 */
@Component({
  selector: 'app-audit-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './audit-detail.component.html'
})
export class AuditDetailComponent implements OnInit {

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

  /**
   * Solicita confirmación al usuario y elimina el lead si acepta.
   * Redirige a /leads tras la eliminación exitosa.
   */
  confirmDeletion(): void {
    const leadData = this.lead();
    if (leadData && confirm(`Are you sure you want to delete ${leadData.name}?`)) {
      this.leadService.deleteLead(leadData.id).subscribe({
        next: () => this.router.navigate(['/leads']),
        error: (err) => console.error('Delete failed', err)
      });
    }
  }

  /**
   * Genera un enlace mailto con asunto y cuerpo predefinidos para enviar
   * una propuesta comercial al lead.
   *
   * BUG CORREGIDO: antes el destinatario (to:) estaba vacío (mailto:?subject=...)
   * porque no se incluía el email del lead. Ahora se usa lead.email si está disponible.
   *
   * Si el lead no tiene email (no fue detectado durante el scraping), se genera
   * un mailto sin destinatario para que el usuario lo rellene manualmente.
   */
  generateMailto(): string {
    const data = this.lead();
    if (!data) return '';

    const recipient = data.email ?? '';
    const subject = encodeURIComponent(
      `Performance Audit for ${data.name} - Opportunity for Growth`
    );
    const body = encodeURIComponent(
      `Hi ${data.name} Team,\n\n` +
      `I've recently conducted a performance audit on your website (${data.websiteUrl ?? 'your site'}) ` +
      `and your current score is ${data.performanceScore ?? 'N/A'}/100.\n\n` +
      `I found some critical issues that are likely affecting your customer conversion rate. ` +
      `I'd love to share the full report and discuss how we can fix this.\n\n` +
      `Best regards,\nLead Scout Team`
    );

    return `mailto:${recipient}?subject=${subject}&body=${body}`;
  }
}
