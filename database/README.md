# Database Setup & Configuration - Cricket App

This directory manages database configuration guidelines and migration artifacts for the **Cricket App** platform.

## Stage 2 Database Architecture & Tables

In **Stage 2: User Authentication & Account Foundation**, the following core database tables are introduced in MySQL schema `cricket_app`:

### 1. `users` Table
Stores registered, verified user accounts.

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
```

### 2. `otp_verifications` Table
Stores hashed OTP verification codes, attempt counts, and expiration timestamps.

```sql
CREATE TABLE otp_verifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL, -- 'REGISTRATION' or 'PASSWORD_RESET'
    expires_at DATETIME NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL
);
```

### 3. `pending_registrations` Table
Temporary storage for pending registration data prior to OTP verification.

```sql
CREATE TABLE pending_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL
);
```

---

## Key Constraints
- `users.email`: UNIQUE (normalized lowercase)
- `users.user_id`: UNIQUE (format `CRK` + 6 digits, e.g. `CRK102847`). Permanent and immutable.
- `pending_registrations.email`: UNIQUE

---

## Environment Variables
- `DB_URL`: JDBC Connection string
- `DB_USERNAME`: MySQL Username
- `DB_PASSWORD`: MySQL Password
