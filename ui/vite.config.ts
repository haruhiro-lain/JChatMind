import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  server: {
    port: 15173,
    host: '127.0.0.1',
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/avatars': 'http://127.0.0.1:8080',
    },
  },
});
