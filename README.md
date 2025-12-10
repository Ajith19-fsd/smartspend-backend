# 💰 SmartSpend – Backend (Spring Boot)

SmartSpend is a secure Expense Tracker backend built with **Spring Boot + PostgreSQL**, providing authentication, budget monitoring, report generation, and real-time notifications. This backend connects with a React frontend deployed separately.

---

## 🚀 Features

### 🔐 Authentication
- User Registration with **Email OTP Verification**
- Secure Login using **BCrypt + JWT**

### 💸 Expense & Budget Management
- CRUD for Expenses
- Set Monthly Budgets Per Category
- Auto Alerts when Spending Limit Exceeds

### 📑 Reports
- **PDF Export (iText Library)**
- **Excel Export (Apache POI)**

### 🔔 Notifications
- Real-time alerts using **WebSocket STOMP**

---

## 🌍 Deployment Details
| Service | Platform |
|--------|---------|
| Backend | Render (Java Spring Boot) |
| Database | PostgreSQL on Render |
| Email | SendGrid SMTP |

> ✔ **Production Mode (Render)** → OTP Email Required  
> ❗ **OTP Email may land in Gmail Spam Folder**

---

## 🛠 Run Locally (Development Mode)

### 📌 Prerequisites
- Java 17+
- Maven
- PostgreSQL (optional for dev)

### ▶ Start Server
```bash
mvn spring-boot:run

📌 Note on Backend URL Usage

🔸 The backend deployed URL (Render) is only for API validation and integration, not for direct use in a browser.
🔸 All features must be accessed via the frontend (Netlify), which communicates with the backend securely through REST APIs.
