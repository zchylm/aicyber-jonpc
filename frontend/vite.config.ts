import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { storefrontDevPages } from './scripts/storefront-dev.mjs'

// https://vite.dev/config/
export default defineConfig(({ isSsrBuild }) => ({
  base: process.env.VITE_BASE_PATH ?? '/',
  plugins: [react(), storefrontDevPages()],
  build: isSsrBuild ? {} : {
    rollupOptions: {
      input: { main: 'index.html', storefront: 'storefront.html' },
    },
  },
  server: {
    port: 5174,
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
}))
