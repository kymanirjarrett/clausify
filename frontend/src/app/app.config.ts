import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';
import { AuthService } from './core/auth.service';
import { ServerStatus } from './core/server-status';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'top' })),
    // Not withFetch(): upload progress events need the XHR backend.
    provideHttpClient(withInterceptors([authInterceptor])),
    // Neither blocks startup: the API may be asleep for up to two minutes on Render's free plan.
    provideAppInitializer(() => {
      inject(ServerStatus).warmUp();
      void inject(AuthService).restore();
    }),
  ],
};
