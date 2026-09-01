# GraphRAG Movie Quiz frontend

React and TypeScript frontend built with Vite.

The application uses Neo4j Needle (`@neo4j-ndl/react`) for standard UI
components, design tokens, themes, and accessibility behavior. Game-specific
visuals such as clue cards and token animations remain custom components.

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

## Instructions content

Edit `src/content/instructions.html` to change the text shown in the
Instructions dialog. It is an HTML fragment, so it should contain only the
content inside the dialog, not `<html>` or `<body>` tags. Unstyled elements
inherit the application's font.

## Production build

Copy `.env.production.example` to `.env.production`, set the deployed backend
URL, and run `npm run build`. Hosting and smoke-test guidance is documented in
[`../docs/deployment.md`](../docs/deployment.md).
