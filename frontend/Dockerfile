# ===================================================================
# Multi-Stage Dockerfile for React Frontend
# ===================================================================

# Stage 1: Build the React Application
FROM node:20-alpine AS builder
WORKDIR /app

# Cache package dependencies
COPY package*.json ./
RUN npm ci

# Copy source code and build
COPY . .
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# Stage 2: Serve with Nginx Alpine
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Clean default html and copy built distribution
RUN rm -rf ./*
COPY --from=builder /app/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
