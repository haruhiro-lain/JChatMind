import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";
import { resolve } from "path";

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 从项目根目录 .env 读取环境变量
  const env = loadEnv(mode, resolve(__dirname, ".."), "");
  const frontendPort = parseInt(env.FRONTEND_PORT || "15173");
  const backendPort = parseInt(env.BACKEND_PORT || "8080");

  return {
    plugins: [vue(), tailwindcss()],
    server: {
      port: frontendPort,
      host: "127.0.0.1",
      proxy: {
        "/api": `http://127.0.0.1:${backendPort}`,
        "/sse": `http://127.0.0.1:${backendPort}`,
        "/avatars": `http://127.0.0.1:${backendPort}`,
      },
    },
  };
});
