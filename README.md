# Cricket App

## Project Overview
**Cricket App** is a cricket match management and live scoring platform built for real-time score tracking, team management, player statistics, and match streaming.

This repository implements **Stage 2: User Authentication & Account Foundation** featuring **Real Email OTP Delivery**, registration, email verification, permanent Cricket User ID generation (`CRKXXXXXX`), login, JWT token security, protected endpoints, and password recovery.

---

## Current Stage
**Stage 2 — User Authentication & Real Email OTP Delivery**

---

## Real Email SMTP Setup Guide (Gmail)

To receive real OTP emails in your inbox during registration and password reset, configure your Gmail App Password:

1. Enable **2-Step Verification** on your Google Account:  
   👉 [Google 2-Step Verification](https://myaccount.google.com/signinoptions/two-step-verification)
2. Generate an **App Password**:  
   👉 [Google App Passwords](https://myaccount.google.com/apppasswords)
   - Select App: *Mail*
   - Select Device: *Windows Computer* or *Other*
   - Copy the generated **16-character App Password**.
3. Set your environment variables before launching the backend:

### In PowerShell:
```powershell
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-16-digit-app-password"
```

### In Command Prompt (CMD):
```cmd
set MAIL_USERNAME=your-email@gmail.com
set MAIL_PASSWORD=your-16-digit-app-password
```

---

## Environment Variables Reference

| Variable | Description | Default / Example |
|----------|-------------|-------------------|
| `MAIL_HOST` | SMTP server host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP server port | `587` |
| `MAIL_USERNAME` | Your Gmail address | `your-email@gmail.com` |
| `MAIL_PASSWORD` | 16-character Gmail App Password | `abcd efgh ijkl mnop` |
| `APP_EMAIL_ENABLED` | Enables real SMTP email sending | `true` |
| `OTP_DEV_LOG_ENABLED` | Disables console OTP printing in production | `false` |
| `DB_URL` | JDBC Connection URL | `jdbc:mysql://localhost:3306/cricket_app?...` |
| `DB_USERNAME` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | `YOUR_DATABASE_PASSWORD` |
| `JWT_SECRET` | Secret key for signing JWT tokens | Configurable random secret |

---

## Key Authentication REST API Endpoints

### Public Endpoints
- `GET /api/health` - Backend health status
- `POST /api/auth/register/request-otp` - Validates registration & sends **REAL OTP email via SMTP**
- `POST /api/auth/register/verify-otp` - Verifies real OTP & generates permanent Unique User ID (`CRKXXXXXX`)
- `POST /api/auth/register/resend-otp` - Resends new OTP email with 60s cooldown
- `POST /api/auth/login` - Login using email & password -> returns JWT token
- `POST /api/auth/logout` - Sign out acknowledgment
- `POST /api/auth/forgot-password/request-otp` - Sends password reset OTP via SMTP
- `POST /api/auth/forgot-password/verify-otp` - Verifies password reset OTP
- `POST /api/auth/reset-password` - Resets password (User ID remains unchanged!)

### Protected Endpoints (Requires `Authorization: Bearer <JWT>`)
- `GET /api/auth/me` - Get current user account profile

---

## How to Run Backend

From the `backend` directory:

```bash
# Run unit & integration tests
mvn clean test

# Build executable JAR
mvn clean package

# Run Spring Boot server with real SMTP credentials
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-app-password"
java -jar target/cricket-app-backend-0.0.1-SNAPSHOT.jar
```

---

## How to Run Frontend

Open `frontend/index.html` via any static server on port `5500` (e.g. `npx serve -l 5500 frontend` or VS Code Live Server).
