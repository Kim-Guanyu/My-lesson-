import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const proxyTarget = env.VITE_API_PROXY || "";

  return {
    plugins: [vue()],
    server: proxyTarget
      ? {
          proxy: {
            "/user-server": {
              target: proxyTarget,
              changeOrigin: true
            },
            "/course-server": {
              target: proxyTarget,
              changeOrigin: true
            },
            "/sale-server": {
              target: proxyTarget,
              changeOrigin: true
            },
            "/order-server": {
              target: proxyTarget,
              changeOrigin: true
            }
          }
        }
      : undefined
  };
});

