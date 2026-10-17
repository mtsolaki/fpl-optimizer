# ==========================================
# 1. Build Stage
# ==========================================
FROM node:22-alpine AS builder
WORKDIR /app

# Αντιγραφή αρχείων package για dependency caching
COPY package*.json ./
RUN npm ci

# Αντιγραφή πηγαίου κώδικα και build
COPY . .
RUN npm run build

# ==========================================
# 2. Production Stage με Nginx
# ==========================================
FROM nginx:alpine
WORKDIR /usr/share/nginx/html

# Καθαρισμός προεπιλεγμένων αρχείων nginx
RUN rm -rf ./*

# Αντιγραφή των compiled assets (dist για Vite, build για CRA)
COPY --from=builder /app/dist .

# Αντιγραφή του custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]