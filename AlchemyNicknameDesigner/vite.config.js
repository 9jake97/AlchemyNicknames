import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: './',
  server: {
    proxy: {
      '/api/nickname': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/nickname/, '')
      },
      '/api/link-code': {
        target: 'http://localhost:8085',
        changeOrigin: true
      },
      '/api/accounts': {
        target: 'http://localhost:8085',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/accounts/, '/accounts')
      },
      '/auth': {
        target: 'http://localhost:8085',
        changeOrigin: true
      }
    }
  }
})
