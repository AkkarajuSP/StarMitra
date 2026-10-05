# StarMitra Mobile — React Native + Expo (ADR-005)

Creator + Audience app. Admin/Judge stay web-only.

## Run

```powershell
# 1. Backend (UAT profile)
cd backend
$env:SPRING_PROFILES_ACTIVE = "uat"; $env:SERVER_PORT = "9090"
mvn spring-boot:run

# 2. Mobile dev server
cd mobile
npx expo start        # Metro on :8081
```

## Open the app

- **Expo Go (recommended)**: install *Expo Go* on Android → app opens the QR/
  enter `exp://192.168.1.10:8081` (Metro LAN URL — printed in terminal).
- **Android emulator**: `npx expo start --android` — the app then reaches the
  backend at `http://10.0.2.2:9090` automatically (see `app.json → extra.apiBase`,
  currently `http://192.168.1.10:9090` for devices on the same Wi-Fi).

## UAT accounts

| Account | Role |
|---|---|
| uat-user@starmitra.dev | audience |
| uat-creator@starmitra.dev | creator |
| uat-judge@starmitra.dev | + JUDGE |
| uat-admin@starmitra.dev | + ADMIN |

OTP codes print to the **backend console** under `UatOtpSender` —
`channel=EMAIL destination=... otp=NNNNNN`.

## Structure

- `src/app/` — Expo Router screens (login, tabs, competition, conversation,
  rooms, notifications, leaderboard)
- `src/lib/` — API client (`api.ts`, Bearer + CSRF bootstrap), endpoints,
  session storage, `useApi` hook
- `src/components/ui.tsx` — Mark/Lockup/Btn/Field/OtpRow/Badge/Card/states
- `assets/brand/` — official PO-supplied PNGs (mark, lockup, favicon)

## Auth / CSRF

`POST /api/v1/auth/otp/*` is a cookie-carried mutation → the client first
calls `GET /api/v1/public/csrf`, then sends `X-XSRF-TOKEN` + `Cookie` on the
OTP calls. Authenticated calls use `Authorization: Bearer …` (CSRF-exempt
per `SecurityConfig`).

## Verify

```
npx tsc --noEmit    # typecheck — clean
npx expo-doctor     # dependency/config check
```
