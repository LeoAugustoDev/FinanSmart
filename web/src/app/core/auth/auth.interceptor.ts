import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

/** Anexa o token apenas às chamadas da nossa API, para nunca enviá-lo a outros domínios. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url)) {
    return next(request);
  }

  return from(inject(AuthService).accessToken()).pipe(
    switchMap((token) =>
      next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request),
    ),
  );
};

function isApiRequest(url: string): boolean {
  return url === environment.apiUrl || url.startsWith(`${environment.apiUrl}/`);
}
