import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
  },
  build: {
    target: 'esnext',
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/md-editor-v3') || id.includes('node_modules/codemirror') || id.includes('node_modules/highlight.js')) {
            return 'editor'
          }
          if (id.includes('node_modules/echarts') || id.includes('node_modules/vue-echarts')) {
            return 'charts'
          }
          if (id.includes('node_modules/element-plus')) {
            return 'element-plus'
          }
          if (id.includes('node_modules/lucide-vue-next')) {
            return 'icons'
          }
        }
      }
    }
  },
  optimizeDeps: {
    esbuildOptions: {
      target: 'esnext'
    }
  }
})
