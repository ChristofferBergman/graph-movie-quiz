import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { NeedleThemeProvider } from '@neo4j-ndl/react'
import '@neo4j-ndl/base/lib/neo4j-ds-styles.css'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <NeedleThemeProvider theme="light">
      <App />
    </NeedleThemeProvider>
  </StrictMode>,
)
