import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [tailwindcss(), react()],
  // Dev-only: forward API paths to the backend services so the SPA can use
  // same-origin relative URLs (/catalog, /user-progress) in `pnpm dev` too —
  // mirroring how nginx proxies them in the built image. Keeps CORS out of dev.
  server: {
    proxy: {
      '/catalog': 'http://localhost:8082',
      '/user-progress': 'http://localhost:8081',
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    setupFiles: ['./src/__tests__/setup.ts'],
  },
})
