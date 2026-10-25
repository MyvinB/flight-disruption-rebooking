# syntax=docker/dockerfile:1

# --- Build: type-check and bundle the Vue app ---
FROM node:20-alpine AS build
WORKDIR /app

# Dependencies first, so they are cached until the lock file changes
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- Runtime: nginx serves the static files and proxies /api to the backend ---
FROM nginx:1.27-alpine

# Where nginx forwards /api; the default matches the service name in docker-compose.yml.
# PORT is the port nginx listens on; hosting platforms such as Render override it.
ENV BACKEND_URL=http://backend:8080 \
    PORT=80

# The nginx image runs envsubst on /etc/nginx/templates/*.template at startup
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=5 \
  CMD wget -qO- "http://127.0.0.1:${PORT}/" > /dev/null || exit 1
