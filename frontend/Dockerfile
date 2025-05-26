# Stage 1: Build the application
FROM node:22 AS builder 

WORKDIR /app

# Copy package.json and package-lock.json to the container
COPY codebase/package*.json ./

# Install dependencies
RUN npm install

# Copy the rest 
COPY codebase/ .

# Build app
RUN npm run build

# Stage 2: Serve application
FROM node:22-slim
WORKDIR /app
COPY --from=builder /app/dist ./dist

# Install http-server to serve the static files
RUN npm install -g http-server

EXPOSE 8080
CMD ["http-server", "dist"]