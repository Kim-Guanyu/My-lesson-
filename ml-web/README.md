# ml-web

Admin frontend for MyLesson, built with Vite + Vue 3 + Element Plus.

## Quick start

1) Install dependencies

```powershell
cd D:\java\my-lesson\ml-web
npm install
```

2) Run in dev mode

```powershell
npm run dev
```

## Configuration

Set API base URL (for Axios) or proxy target (for Vite dev server):

- `VITE_API_BASE_URL` (e.g. `http://localhost:24101`)
- `VITE_API_PROXY` (e.g. `http://localhost:24101`)

Example `.env.local`:

```
VITE_API_BASE_URL=http://localhost:24101
VITE_API_PROXY=http://localhost:24101
```

## Build

```powershell
npm run build
npm run preview
```

