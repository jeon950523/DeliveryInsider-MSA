FROM node:24.16.0-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .

ARG VITE_EXTERNAL_API_BASE_URL
ARG VITE_APP_BASE_PATH=/simulator/
ENV VITE_EXTERNAL_API_BASE_URL=${VITE_EXTERNAL_API_BASE_URL}
ENV VITE_APP_BASE_PATH=${VITE_APP_BASE_PATH}
RUN test -n "${VITE_EXTERNAL_API_BASE_URL}" && npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html/simulator

EXPOSE 8080
