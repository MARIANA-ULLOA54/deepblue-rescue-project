# DeepBlue Rescue — Frontend (React + Vite + TanStack Query v5)

Carpeta `deepblue-ui/` dentro del proyecto, rama `feature/frontend-react`. El backend (`src/main/java`) no se modificó.

## Requisitos
- Node.js >= 20.19 (recomendado 22 LTS) y npm
- Backend DeepBlue en http://localhost:8080 (Java 21, Maven Wrapper, PostgreSQL con datos)

## Comandos
```bash
npm install        # instala dependencias
npm test           # pruebas del cliente HTTP (node:test, sin dependencias extra)
npm run build      # compila para producción (detecta errores de import/JSX)
npm run dev        # http://localhost:5173 (proxy /api -> :8080)
npm run preview    # sirve el build de producción
```
