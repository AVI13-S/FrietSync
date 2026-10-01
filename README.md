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

All API routes use the `/api/v1` prefix.

| Method | URL | Body |
|---|---|---|
| POST | /api/v1/auth/signup | name, email, password |
| POST | /api/v1/auth/login | email, password |
| POST | /api/v1/auth/verify-otp | email, code |
| POST | /api/v1/auth/forgot-password | email |
| POST | /api/v1/auth/reset-password | email, code, newPassword |
| POST | /api/v1/auth/refresh | refreshToken |
| POST | /api/v1/auth/logout | refreshToken |

`GET /api/v1/check` returns `{"status": "ok"}` to indicate that the server is running.

## Invites

Create invites with `POST /api/v1/admin/invites`.

When creating an invite, the admin must provide a role (`PROJECT_MANAGER`, `TEAM_LEAD`, `CONTRIBUTOR`, or `REPORTER`) and `expiresAt` as an ISO-8601 UTC timestamp (for example, `2030-05-01T12:00:00Z`). The expiration must be in the future. `ADMIN` is not an assignable invite role. The admin-selected expiration is used for new invites; the 7-day duration only applies to legacy invites that have no expiration value.

Example create request body:

```json
{
  "email": "collaborator@example.com",
  "role": "PROJECT_MANAGER",
  "expiresAt": "2030-05-01T12:00:00Z"
}
```

The seven-day period below applies only to legacy invites; new invites use the admin-selected expiration date.

Admin-created contributor invites expire 7 days after creation. Authenticated users can view their pending invites with `GET /api/v1/invites/me`, accept with `POST /api/v1/invites/accept`, or reject with `POST /api/v1/invites/reject`. Accept, reject, and revoke use `{"inviteId": "<invite UUID>"}`. An admin can revoke a pending invite with `POST /api/v1/admin/invites/revoke`, using an admin access token as a Bearer token. Routes under `/api/v1/admin/**` require the `ADMIN` role. Invite responses include `createdAt`, `acceptedAt` (null until accepted), and `expiresAt`; expired invites are marked `EXPIRED` when the recipient loads their pending invites.

### Signup

```
POST /api/v1/auth/signup

{
  "name": "Test User",
  "email": "test@example.com",
  "password": "test123"
}
```

The password should be between 6 and 20 characters. If the email is already used, it gives an error.

### Login

```
POST /api/v1/auth/login

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

- `/api/v1/auth/**` and `/api/v1/check` can be opened by anyone, because the user has no token when they sign up or log in
- `/api/v1/admin/**` is only for users with the ADMIN role
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
