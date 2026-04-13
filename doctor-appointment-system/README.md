# Care Connect - Doctor Appointment System

A premium "End-to-End" Doctor Appointment System with a **separated frontend and backend architecture**. The application features rich aesthetics, dynamic UI, Role-Based Access Control, AI Chatbot capabilities, and Emergency tracking.

## Repository Structure
The project has been separated into two independent folders for true decoupled development:

1. **Backend (`doctor-appointment-system`)**: A Java Spring Boot REST API.
2. **Frontend (`doctor-appointment-frontend`)**: Vanilla HTML/CSS/JS files that communicate with the backend.

---

## 🚀 Deployment Instructions

### 1. Database Setup
Ensure you have MySQL installed and running on default port `3306`. Create the database:
```sql
CREATE DATABASE IF NOT EXISTS doctor_appointment;
```
*The Spring Boot application will automatically create all tables (`users`, `doctors`, `appointments`, `chat_logs`) on startup.*

### 2. Deploy the Backend (Spring Boot)
Open a terminal in the `doctor-appointment-system` directory:

1. **Build the JAR file:**
   ```bash
   mvn clean package
   ```
2. **Run the Application:**
   ```bash
   java -jar target/doctor-appointment-system-1.0.0-SNAPSHOT.jar
   ```
*The REST API server will start on `http://localhost:8080`. It acts strictly as an API, with CORS enabled.*

### 3. Deploy the Frontend
Open a **new** terminal in the `doctor-appointment-frontend` directory. Since the frontend is composed of purely static HTML/CSS/JS files, any local web server works. 

If you have **Python** installed, you can serve the frontend on port 3000:
```bash
python -m http.server 3000
```
*(Or use VSCode's "Live Server" extension, or `npx serve` if you use Node.js).*

Access the application in your browser at:
**[http://localhost:3000](http://localhost:3000)**

### 4. Admin Account Initialization
Upon initial launch of both servers:
Navigate to the `Register` page (`http://localhost:3000/register.html`). Create your first account continuously selecting **Admin** as the account type.

---
## 🔄 Ensuring 24/7 Operation

### 1. Local Environment (Your Laptop)
To keep the server running continuously and auto-restart on crashes:
1. Double-click the `keep_alive.bat` file in the root directory.
2. This will start the maven process and restart it instantly if it ever stops.

### 2. Cloud Environment (Render.com)
Cloud providers sleep after 15 mins of inactivity. To prevent this:
1. Sign up for a free account at [Cron-job.org](https://cron-job.org/).
2. Create a new "Cronjob" with these settings:
   - **URL**: `https://doctor-appointment-system-yhsg.onrender.com/api/auth/ping`
   - **Schedule**: Every 5 or 10 minutes.
3. This will "tickle" the server regularly, keeping it awake 24/7.

---
**Tech Stack Reminder:**
* **Backend:** Java 17, Spring Boot 3.2.x, Security, Data JPA, JWT, MySQL.
* **Frontend:** HTML5, Vanilla CSS, JS (Fetch API).
