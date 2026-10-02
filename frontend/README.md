# Clausify web app

Angular 22 (standalone components, zoneless change detection, signals) with Tailwind CSS v4.

```bash
npm install
npm start                       # http://localhost:4200; /api is proxied to the API on :8080 (proxy.conf.json)
npx ng test --watch=false       # unit tests (Vitest)
npx ng build                    # production build into dist/
```

The API must be running for anything past the landing page (see the root README).

## Layout

- `src/app/core/`: API types, auth state and interceptor, route guards, API services, error mapping
- `src/app/shared/`: icon set, wordmark, status stamp, formatting helpers
- `src/app/features/`: landing (with the WebGL hero), auth, shell, dashboard, contract detail, account

## Design

- [`PRODUCT.md`](PRODUCT.md): who the product is for and what the UI must stay honest about
- [`DESIGN.md`](DESIGN.md): the visual system ("Specification Sheet"): tokens, type, components, rules
- `src/styles.css`: the tokens and component classes in code
