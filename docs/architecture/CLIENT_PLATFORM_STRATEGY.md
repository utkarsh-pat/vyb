# Vyb Client Platform Strategy

Owner: Client Platform
Last Updated: 2026-07-28
Status: Active for web/PWA and native Android

## Purpose

Vyb ships two real client surfaces:

- `apps/web`: Next.js responsive web and PWA;
- `apps/mobile`: native Android using Kotlin and Jetpack Compose.

iOS is deferred. Backend contracts must remain portable if an iOS client is added later.

## Shared Product Contract

Both clients:

- authenticate with Firebase;
- call the same versioned Cloud Run `/v1` API;
- obey the same tenant, feature-flag, rate-limit, and moderation rules;
- use the same request/response contracts and analytics event names;
- upload media through the same signed upload-intent protocol;
- support Marketplace, feed, profile, resources, chat, notifications, and reports;
- must tolerate additive response fields and feature disable/read-only states.

## Surface-Specific Ownership

Shared:

- API contracts and validation rules;
- feature flag names;
- domain state machines;
- error taxonomy;
- analytics event names;
- design tokens where they map cleanly.

Not forced to share:

- React and Compose components;
- navigation and lifecycle code;
- CSS, DOM, native gestures, notifications, or storage implementations;
- web session-cookie and Android token-storage plumbing;
- media selection/compression UI.

## Web/PWA

- Vercel-hosted Next.js App Router;
- mobile-first and desktop-complete;
- SSR for public/session-sensitive entry routes;
- service worker and installable manifest where supported;
- cookie/session proxy routes may normalize web behavior but may not own business truth;
- CSRF protection for cookie-authenticated mutations;
- same-origin helper routes are non-critical and cannot become an undocumented second backend.

## Android

- Kotlin, Jetpack Compose, JDK 17, Android SDK 35;
- Firebase Auth, FCM, Crashlytics, and Performance Monitoring;
- encrypted platform storage for session and chat key material;
- Play App Signing, internal/closed/production tracks, and staged percentage rollout;
- deep links for notification targets;
- backend update manifest and Remote Config/feature manifest kill switches;
- current and previous release supported during backend rollout.

## Launch Compatibility

- contract tests run against both client models;
- backend changes are additive until the previous Android release is below the supported threshold;
- Android force-update is reserved for security/compatibility emergencies;
- Stories are hidden/disabled in both clients for the initial public launch even if implementation remains in the repository;
- video/events/games follow tenant feature flags;
- Marketplace safety copy and report/block entry points must match across surfaces.

## UX and Accessibility

- all primary web flows support keyboard, focus visibility, semantic controls, and responsive layouts;
- Android supports TalkBack labels, scalable text, safe touch targets, and back navigation;
- both clients provide empty, loading, offline/retry, permission-denied, rate-limited, and read-only states;
- slow/offline networks never show fake success;
- user-generated media has placeholders, retry, and bounded memory behavior.

## Definition of Ready

Before client implementation:

- API contract and error states are approved;
- mobile, tablet/desktop, and offline behavior are described;
- feature flag and rollback behavior are defined;
- analytics and privacy impact are defined;
- current and previous Android compatibility is understood;
- accessibility acceptance criteria exist.
