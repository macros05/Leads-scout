export interface Lead {
  id: number;
  name: string;
  website: string;
  performanceScore: number;
  status: 'Critical' | 'Warning' | 'Good' | 'Offline';
  tech: string[];
}
