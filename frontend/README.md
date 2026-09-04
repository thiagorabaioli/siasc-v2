# siac-frontend

SPA React + TypeScript + Vite, servida por nginx em produção. Único ponto de
entrada HTTP da aplicação — faz reverse-proxy de `/api/<serviço>/` para os
backends. Ver `../CLAUDE.md` e `../docs/arquitetura.md` §7.

```bash
npm install
npm run dev      # desenvolvimento
npm run build    # build de produção (dist/)
```
