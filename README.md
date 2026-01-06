# 🚀 Career Agent AI

**Live URL:**  
https://agent-backend-gttz.onrender.com

Career Agent AI is an intelligent full-stack platform designed to help job seekers analyze resumes, manage job applications, and get AI-powered career guidance through an interactive chatbot.

---

## 🖼️ Application UI Preview

### 🔐 Login Page
![Login Page](images/login-page.png)

### 📊 Dashboard
![Dashboard](images/dashboard.png)

### 🤖 Chat Assistant
![Chatbot](images/chatbot.png)

> **Note:** Add real screenshots from your deployed app inside the `images/` folder to replace these placeholders.

---

## 🧩 Features

- AI-based Resume & Job Description Matching  
- Secure Authentication using JWT  
- Google OAuth 2.0 Sign-In  
- Save and Manage Job Applications  
- PDF Resume Parsing  
- Real-Time Career Guidance Chatbot  
- Responsive Web Interface  

---

## 🧱 Tech Stack

### Backend
- Java 17  
- Spring Boot 3.4.1  
- Spring Data JPA & Hibernate  
- Spring Web & WebFlux  
- Thymeleaf Templates  
- JWT Authentication  
- Google OAuth Integration  
- Apache PDFBox  
- Reactor Core  

### Database
- MySQL  
- H2 (Testing)  

### Frontend
- HTML / CSS / JavaScript  
- Bootstrap 5  
- Font Awesome  
- Google Sign-In  
- Responsive Thymeleaf UI  

### DevOps
- Docker  
- Kubernetes  
- GitHub  
- Postman  

---

## 🌐 REST APIs

### Authentication
- `POST /api/auth/google` – Google token verification and JWT generation  

### Job Management
- `GET /api/jobs` – Retrieve saved jobs  
- `POST /api/jobs/save` – Save a job  
- `DELETE /api/jobs/{id}` – Delete a job  
- `GET /api/jobs/{id}` – View job details  

### AI Services
- `POST /api/ai/analyze` – Resume scoring  
- `POST /api/ai/chat` – Chat assistant  

---

## 📂 Important Files & Purpose

| File | Purpose |
|------|--------|
| `AgentApplication.java` | Main Spring Boot entry point |
| `JobMatch.java` | Job entity model |
| `JobMatchRepository.java` | Database access layer |
| `JwtUtil.java` | JWT utility functions |
| `JwtFilter.java` | Request interceptor for JWT validation |
| `SecurityConfig.java` | Spring Security configuration |
| `CorsConfig.java` | CORS setup |
| `AuthController.java` | Google OAuth & JWT APIs |
| `JobController.java` | Job CRUD APIs |
| `HomeController.java` | Dashboard & AI prompts |
| `CustomErrorController.java` | Error handling |
| `application.properties` | App configuration |
| `Dockerfile` | Containerization setup |
| `k8s/deployment.yaml` | Kubernetes deployment |

---

## 📦 Docker Deployment

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/*.jar"]
