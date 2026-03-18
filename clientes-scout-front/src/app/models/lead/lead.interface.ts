/**
 * Interfaz que representa un lead (cliente potencial) tal como lo devuelve el backend.
 *
 * IMPORTANTE: los nombres de campos deben coincidir exactamente con los del JSON
 * serializado por Spring (Client.java). Si se cambia el modelo backend, actualizar aquí.
 *
 * Campos opcionales (pueden ser null si el negocio no tiene web o el análisis falló):
 * - websiteUrl, performanceScore, techStack, detectedIssues, email, phone
 */
export interface Lead {
  id: number;
  name: string;
  email?: string;
  phone?: string;
  notes?: string;
  status: string;
  /** URL del sitio web, o "NO WEBSITE" si no tiene presencia digital */
  websiteUrl?: string;
  /** Puntuación de rendimiento PageSpeed de 0 a 100 */
  performanceScore?: number;
  /** Tecnologías detectadas en el sitio, separadas por coma */
  techStack?: string;
  /** Lista de problemas técnicos generados por Gemini AI */
  detectedIssues?: string[];
  contactDate?: string;
  budget?: number;
}
