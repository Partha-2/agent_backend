Career Agent AI

Live URL: https://agent-backend-gttz.onrender.com

Career Agent AI is a full-stack career assistance platform built with Java Spring Boot, Thymeleaf, MySQL, and AI-powered resume/job matching. The application uses JWT authentication, Google OAuth sign-in, and provides a real-time chat assistant for career guidance. Fully containerized with Docker and deployable via Kubernetes.

🧱 Tech Stack & Tools

Backend:

Java 17

Spring Boot 3.4.1

Spring Web & WebFlux

Spring Data JPA & Hibernate

Thymeleaf (Server-side rendering)

JWT Authentication (io.jsonwebtoken)

Google OAuth 2.0 Sign-in

PDF Resume Parsing (Apache PDFBox)

Reactive Chat via Reactor Core

Database:

MySQL (Cloud / Local)

H2 (Runtime DB for testing)

Frontend:

HTML / CSS / JS

Bootstrap 5

Google Sign-In

Font Awesome

Responsive Thymeleaf templates

DevOps & Deployment:

Docker & Multi-stage build

Render Web Service Deployment

Kubernetes (Optional production-grade deployment)

GitHub (CI/CD)

Postman (API Testing)

📂 Important Files & Purpose
File	Purpose
AgentApplication.java	Entry point for Spring Boot app
JobMatch.java	Entity representing saved jobs in DB
JobMatchRepository.java	Data access layer using JpaRepository
JwtUtil.java	Utility for generating and validating JWT tokens
JwtFilter.java	Intercepts requests to validate JWT authentication
SecurityConfig.java	Spring Security configuration (routes & session)
CorsConfig.java	Cross-Origin configuration for API access
AuthController.java	Handles Google OAuth login & JWT generation
JobController.java	CRUD APIs for saving and retrieving jobs
HomeController.java	Dashboard, AI prompts, and resume/job scoring
CustomErrorController.java	Handles error pages and messages
application.properties	Database, JWT secret, Google Client ID, server port
Dockerfile	Containerizes the backend app
k8s/deployment.yaml	Kubernetes deployment with secrets and service
login.html / dashboard.html	Frontend pages with Thymeleaf, Google Sign-In, and chat UI
🌐 REST APIs

All endpoints are JWT-protected except login.

Auth APIs:

POST /api/auth/google → Receives Google ID token and returns JWT

Job APIs:

GET /api/jobs → List all jobs saved by the user

POST /api/jobs/save → Save a job (requires user email from JWT)

DELETE /api/jobs/{id} → Delete a saved job

GET /api/jobs/{id} → Get job details

Chat/AI APIs:

POST /api/ai/analyze → Send resume + job description, get score & reasons

POST /api/ai/chat → Interactive chat bot for career guidance

🖥️ Frontend Features

Login Page

Google Sign-In integration

JWT storage in localStorage

Clean, modern design with Bootstrap & CSS effects

Dashboard

Job search & save features

AI-based resume scoring

Chatbot assistant

Responsive cards with glassmorphism effect

Real-time spinner loader for API requests

🔐 Security

JWT Authentication: Stateless, scalable, no session storage on server

Google OAuth 2.0: Secure sign-in, tokens verified using Google API

Spring Security: Protect routes and define access control

Secrets Management: JWT secret, DB credentials, Google client ID stored in environment variables / Kubernetes secrets

📦 Docker Deployment

Dockerfile:

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/*.jar"]


Multi-stage build can be used to reduce image size

Port 8080 exposed for web access

☁️ Kubernetes Deployment Example
apiVersion: v1
kind: Secret
metadata:
  name: career-agent-secrets
type: Opaque
data:
  DB_URL: <base64-db-url>
  DB_USERNAME: <base64-username>
  DB_PASSWORD: <base64-password>
  JWT_SECRET: <base64-jwt-secret>
  GOOGLE_CLIENT_ID: <base64-google-client-id>

---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: career-agent-backend
spec:
  replicas: 1
  selector:
    matchLabels:
      app: career-agent
  template:
    metadata:
      labels:
        app: career-agent
    spec:
      containers:
        - name: career-agent-container
          image: <dockerhub-username>/career-agent:latest
          ports:
            - containerPort: 8080
          env:
            - name: DB_URL
              valueFrom:
                secretKeyRef:
                  name: career-agent-secrets
                  key: DB_URL
            - name: DB_USERNAME
              valueFrom:
                secretKeyRef:
                  name: career-agent-secrets
                  key: DB_USERNAME
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: career-agent-secrets
                  key: DB_PASSWORD
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: career-agent-secrets
                  key: JWT_SECRET
            - name: GOOGLE_CLIENT_ID
              valueFrom:
                secretKeyRef:
                  name: career-agent-secrets
                  key: GOOGLE_CLIENT_ID
            - name: PORT
              value: "8080"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10

---
apiVersion: v1
kind: Service
metadata:
  name: career-agent-service
spec:
  type: LoadBalancer
  selector:
    app: career-agent
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080

⚡ Deployment Flow

Code written locally → GitHub repository

Docker build → Push to Docker Hub

Render deployment → Environment variables for DB, JWT, Google Client ID injected

Kubernetes deployment optional for production scale

MySQL hosted on cloud (Aiven or other provider)

✅ Achievements

Full-stack Spring Boot application with AI/Chat integration

JWT authentication + Google OAuth

PDF resume parsing

Dockerized backend for cloud deployment

Kubernetes-ready for production-grade scaling

Clean, responsive, Thymeleaf-based frontend

Live deployment: https://agent-backend-gttz.onrender.com
