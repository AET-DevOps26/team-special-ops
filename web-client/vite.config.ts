import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [tailwindcss(), react()],
  // Dev-only: forward /api/* paths to the backend services, stripping the /api
  // prefix before forwarding — mirroring what the nginx ingress does in k8s and
  // what Traefik does in docker-compose. Keeps CORS out of dev.
  server: {
    proxy: {
      '/api/user-progress': {
        target: 'http://localhost:8081',
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/api/catalog': {
        target: 'http://localhost:8082',
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/api/chat': {
        target: 'http://localhost:8083',
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    setupFiles: ['./src/__tests__/setup.ts'],
  },
})
