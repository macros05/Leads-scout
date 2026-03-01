import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {AuthService} from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {
  email = '';
  password = '';
  errorMessage = '';
  loading = false;
  isRegisterMode = false;

  constructor(private authService: AuthService, private router: Router) {}

  submit(): void {
    this.errorMessage = '';
    this.loading = true;

    if (this.isRegisterMode) {
      this.authService.register({ email: this.email, password: this.password }).subscribe({
        next: () => {
          this.isRegisterMode = false;
          this.loading = false;
        },
        error: (err) => {
          this.errorMessage = err.error?.error || 'Registration failed';
          this.loading = false;
        }
      });
    } else {
      this.authService.login({ email: this.email, password: this.password }).subscribe({
        next: () => this.router.navigate(['/dashboard']),
        error: () => {
          this.errorMessage = 'Invalid email or password';
          this.loading = false;
        }
      });
    }
  }

  toggleMode(): void {
    this.isRegisterMode = !this.isRegisterMode;
    this.errorMessage = '';
  }
}
