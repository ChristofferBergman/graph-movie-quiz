# GraphRAG Movie Quiz frontend

React and TypeScript frontend built with Vite.

## Local development

Install dependencies and start the development server:

```sh
npm install
npm run dev
```

Copy `.env.example` to `.env.local` when local configuration is needed. The
default backend URL is `http://localhost:8080`.

## Verification

```sh
npm run lint
npm test
npm run build
```

## Assets

- Put card art and other imported images in `src/assets/images/`.
- Put Syne Neo webfont files in `src/assets/fonts/`.
- Use `public/` only for assets that require a fixed, unhashed URL.
