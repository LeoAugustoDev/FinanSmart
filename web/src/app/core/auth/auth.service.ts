import { Injectable, signal } from '@angular/core';
import Keycloak from 'keycloak-js';

import { environment } from '../../../environments/environment';

const TOKEN_MIN_VALIDITY_SECONDS = 30;

/** Sessão do usuário no Keycloak. Os tokens ficam apenas em memória, dentro da keycloak-js. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = new Keycloak(environment.keycloak);
  private readonly authenticated = signal(false);
  private readonly available = signal(true);

  readonly isAuthenticated = this.authenticated.asReadonly();
  readonly isAvailable = this.available.asReadonly();

  async init(): Promise<void> {
    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
        pkceMethod: 'S256',
        checkLoginIframe: false,
      });
      this.authenticated.set(authenticated);
    } catch (error) {
      console.error('Não foi possível conectar ao Keycloak', error);
      this.available.set(false);
    }
  }

  login(returnPath = '/painel'): Promise<void> {
    return this.keycloak.login({ redirectUri: this.absoluteUrl(returnPath) });
  }

  register(returnPath = '/painel'): Promise<void> {
    return this.keycloak.register({ redirectUri: this.absoluteUrl(returnPath) });
  }

  logout(): Promise<void> {
    return this.keycloak.logout({ redirectUri: window.location.origin });
  }

  /** Retorna um token válido, renovando-o se necessário. Sem sessão, leva o usuário ao login. */
  async accessToken(): Promise<string | undefined> {
    if (!this.authenticated()) {
      return undefined;
    }

    try {
      await this.keycloak.updateToken(TOKEN_MIN_VALIDITY_SECONDS);
      return this.keycloak.token;
    } catch {
      this.authenticated.set(false);
      await this.login(window.location.pathname + window.location.search);
      return undefined;
    }
  }

  private absoluteUrl(path: string): string {
    return new URL(path, window.location.origin).href;
  }
}
