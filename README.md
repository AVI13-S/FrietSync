# FrietSync - Authentication

This is the authentication part of our FrietSync backend. It has signup, login, JWT token checking and forgot password with OTP.

## What is in this part

- Signup and login
- Passwords are saved as hashes using BCrypt, not as plain text
- A JWT token is given to the user when they log in
- A filter checks the token on every request
- Some URLs are only for admins
- Forgot password using an OTP that is sent to the user's email

## Folders

- `common/security` has JwtUtil, JwtAuthFilter and SecurityConfig
- `common/exception` has the custom exceptions, ErrorResponse and GlobalExceptionHandler
- `common/mail` has the classes that send the email
- `otp` has the Otp entity, OtpPurpose, OtpRepository and OtpService
- `user` has AuthController, the DTOs, the User entity, the Role enum, UserRepository and AuthService

## What each main class does

- **AuthController** takes the requests for signup, login and forgot password
- **AuthServiceImpl** has the logic for signup, login and reset password
- **JwtUtil** creates a token and checks a token
- **JwtAuthFilter** reads the token from the request and finds out who the user is
- **SecurityConfig** decides which URLs are open and which need a token
- **OtpService** makes the OTP, sends it on email and checks it later
- **GlobalExceptionHandler** changes exceptions into a proper error response

## APIs

All of these start with `/api/auth`.

| Method | URL | Body |
|---|---|---|
| POST | /api/auth/signup | name, email, password |
| POST | /api/auth/login | email, password |
| POST | /api/auth/forgot-password | email |
| POST | /api/auth/verify-reset-otp | email, code |
| POST | /api/auth/reset-password | email, code, newPassword |

There is also `GET /api/check`. It only returns `{"status": "ok"}` so we can see that the server is running.

### Signup

```
POST /api/auth/signup

{
  "name": "Test User",
  "email": "test@example.com",
  "password": "test123"
}
```

The password should be between 6 and 20 characters. If the email is already used, it gives an error.

### Login

```
POST /api/auth/login

{
  "email": "test@example.com",
  "password": "test123"
}
```

If the email and password are right, we get the user details and a token:

```
{
  "user": { "id": "...", "name": "Test User", "email": "test@example.com", "role": "ADMIN", ... },
  "token": "eyJ..."
}
```

## How the token works

1. The user logs in and gets a token.
2. For the next requests, the frontend sends the token in the header like this:

```
Authorization: Bearer <token>
```

3. JwtAuthFilter takes the token from the header and checks it using JwtUtil. The token has the user id and the role inside it.
4. SecurityConfig then decides if the request is allowed.

Who can open what:

- `/api/auth/**` and `/api/check` can be opened by anyone, because the user has no token when they sign up or log in
- `/api/admin/**` is only for users with the ADMIN role
- every other URL needs a valid token

The token is valid for 24 hours. After that the user has to log in again.

## Forgot password

This has 3 steps.

1. The user enters the email. We check that the account exists, then make a 6 digit OTP and send it on email (`/forgot-password`).
2. The user enters the OTP and we check if it is correct (`/verify-reset-otp`).
3. The user enters the new password. We check the OTP again and then save the new password (`/reset-password`).

About the OTP:

- it is valid for 10 minutes
- it is saved in the database with a purpose, SIGNUP or RESET_PASSWORD, so an OTP for one purpose cannot be used for the other
- if a new OTP is sent, the old one for that email is deleted
- after the password is changed, the OTP is marked as used

