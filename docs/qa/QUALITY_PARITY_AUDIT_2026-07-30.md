# Vyb Android/Web Quality and Parity Audit



Owner: Product Engineering

Audit date: 2026-07-30

Scope: PWA, native Android, backend runtime, release path, and supporting documentation

Status: Implementation complete; production deployment and full device matrix remain gated



## 1. Scoring statement



This ledger applies the task rubric only to evidence-backed work in this

implementation cycle. UI parity remains subject to the product owner's visual

rating.



| Class | Evidence count | Rubric | Provisional points |

| --- | ---: | ---: | ---: |

| PWA/Android UI parity | 1 package | 100 | 100, after owner acceptance |

| Bugs found and fixed | 32 | 3 + 7 | 320 |

| Best-practice/cost/performance packages | 8 | 15 | 120 |

| Previously unimplemented features completed | 11 | 20 | 220 |

| Total |  |  | **760** |



The independently verifiable subtotal, before the owner's 100-point visual

rating, is **660**. No point is claimed for documentation-only changes, an

unverified production deployment, or known remaining gaps.



## 2. Bug ledger



| ID | Severity | Finding and correction | Verification |

| --- | --- | --- | --- |

| B01 | P0 | Every `POST /v1/posts` request could throw because `intent` was referenced outside its upload-route scope. The invalid block was removed from post creation. | Backend social regression test |

| B02 | P1 | Avatar upload intent did not reliably enforce image-only MIME types. Avatar validation now rejects video while post/story/vibe uploads retain their supported media types. | Backend upload-intent tests |

| B03 | P1 | A multi-recipient notification shared one read state, so one user could mark it read for everyone. State is now keyed per recipient. | Notification repository regression test |

| B04 | P1 | The raw per-recipient state map could be returned to another recipient. Viewer responses now remove the internal map and expose only that viewer's state. | Notification privacy regression test |

| B05 | P0 | A production membership lookup failure could silently grant a synthetic verified demo membership. Production now fails closed. | Viewer-context tests |

| B06 | P1 | Firebase and anonymous requests could reach demo fixtures. Demo fallback is now limited to explicitly trusted internal non-production sessions. | Viewer-context tests |

| B07 | P1 | The web backend client consumed JSON once and then could not recover a plain-text error body. It now reads once and parses structured or text errors deterministically. | Web parser tests and type check |

| B08 | P0 | Chat identity generation called an API 31-only ECDH Keystore purpose on the supported API 26-30 range. API 31+ keeps hardware ECDH; API 26-30 wraps the software EC private key with a non-exportable Keystore AES key. | Min-SDK lint audit and Android build |

| B09 | P1 | Android backup could restore encrypted chat/account state without the device Keystore key and strand the user. App data backup is disabled and explicit extraction rules exclude key/account residue. | Android manifest merge and lint |

| B10 | P1 | Direct-update metadata could point at an arbitrary HTTPS host. Update URLs are restricted to `vybnet.app` and its subdomains, standard HTTPS ports, and no embedded credentials. | Android and backend updater tests |

| B11 | P0 | A downloaded APK was not cryptographically matched to the manifest. SHA-256 is now mandatory, checked before install, and a mismatched/empty artifact is deleted. | Android updater tests |

| B12 | P1 | A server-supplied version label could influence the downloaded filename. Version labels are sanitized before path construction. | Android updater tests |

| B13 | P1 | Force-update state could be shown for incomplete or untrusted artifact metadata. The server and client now disable update/force unless version, URL, and checksum are valid. | Backend and Android updater tests |

| B14 | P1 | Foreground and data-only FCM messages were received but not rendered. The service now creates a permission-aware notification with a safe native deep link. | Notification content unit tests |

| B15 | P1 | Android used deprecated FCM registration tokens even though the current SDK targets Firebase Installation IDs. Registration, callback, persistence, backend targeting, invalid-FID cleanup, and Admin SDK are migrated to FIDs; legacy tokens remain temporarily deliverable. | Android compile plus FCM outbox tests |

| B16 | P2 | Adding/deleting a Home comment reused stale feed metadata for the count. The count now follows the loaded thread result. | Comment mutation unit tests |

| B17 | P2 | Vibes had the same stale comment-count behavior. It now uses the authoritative loaded thread count. | Comment mutation unit tests |

| B18 | P2 | Concurrent network-client access used non-atomic `getOrPut`, allowing duplicate Retrofit instances. It now uses `ConcurrentHashMap.computeIfAbsent`. | Android compile and tests |

| B19 | P2 | Hub sizing used configuration screen height and could be wrong in split-screen/multi-window. It now derives the live window size through `LocalWindowInfo` and density. | Android lint and compile |

| B20 | P2 | Campus failures exposed raw transport details such as emulator host/port. User-facing errors are sanitized while retry remains available. | Error sanitizer tests |

| B21 | P2 | The Home header action was below the 48 dp accessibility target. It is now 48 dp. | Source audit and lint |

| B22 | P2 | Social studio close was 38 dp. It is now a 48 dp action. | Source audit and lint |

| B23 | P2 | Connect/Queens cells were 44 dp and lacked state semantics. They now provide 48 dp targets, roles, labels, and state. | Source audit and lint |

| B24 | P2 | Scribble color controls exposed only the visual swatch as the hit area. Each swatch now has a semantic 48 dp target. | Source audit and lint |

| B25 | P2 | Launcher artwork could be cropped by adaptive icon masks. A masked inset foreground and round/adaptive resources were added. | Android resource merge and lint |

| B26 | P3 | Splash/launcher background did not match the PWA dark surface. It is aligned to `#0F172A`. | Android resource audit |

| B27 | P2 | Native chrome branded the product as `vybnet` while the product name is `vyb`. Visible branding is normalized to `vyb`. | Visual source audit |

| B28 | P2 | Light-mode native colors diverged from the shared indigo/teal/pink product palette. Compose light/dark schemes now share the product tokens. | Theme audit and Android build |

| B29 | P1 | Cookie-only Connect/Queens calls could not authenticate from native Android. They now use Firebase Bearer contracts with tenant/user-bound signed sessions and server-owned validation. | Backend Games contract tests |

| B30 | P1 | Launch-critical screens could start duplicate bootstrap/feed/story/vibe work and collect state outside lifecycle ownership. Requests are guarded and state collection is lifecycle-aware. | Android unit/build verification |

| B31 | P1 | Dark native screens left status-bar icons in light-theme mode, producing black time/network/battery glyphs on the dark PWA-matched background. System-bar icon appearance now follows the active Compose theme. | Authenticated emulator screenshot regression |

| B32 | P1 | Missing migrated media produced an unexplained pure-black Vibes screen and duplicate image downloads could spin forever. Video/image surfaces now expose loading and `Media unavailable` states, and story/vibe images use the shared cached Coil loader. | Authenticated emulator plus production media 404 |



## 3. Completed feature ledger



| ID | Feature completed | Release evidence |

| --- | --- | --- |

| F01 | Native threaded comment reactions with authoritative server counts | Shared Home/Vibes social API, repository, view model, UI, and tests |

| F02 | Foreground/data-only Android notification rendering and deep-link handoff | Messaging service, channel, icon, manifest, tests |

| F03 | Firebase Installation ID registration and FID-targeted backend delivery | Android registration callback, Admin SDK 14, outbox compatibility tests |

| F04 | Checksum-pinned direct in-app update flow | Backend manifest builder, Android validator/installer, tests |

| F05 | Native universal search across People, Posts, Vibes, and Market | Bounded discovery cache and destination callbacks |

| F06 | Native onboarding/profile completion and production-owned avatar/profile settings | Username checks, catalogs, R2 avatar, HTTPS social links |

| F07 | Native community chat discovery and text conversation | Membership-safe list/read/send contracts |

| F08 | Marketplace native discovery/media parity | Search, category, price/recent sort, saved filter, image/video display |

| F09 | Events host MVP core | Create/edit plus approve/waitlist/reject registration review |

| F10 | Bearer-authenticated Connect and Queens daily/hint/submit | Server-owned puzzle validation and signed sessions |

| F11 | Native Scribble MVP | Lobby, private/public rooms, reconnect, word/guess/timer, stroke batching, room controls |



## 4. Best-practice, cost, and performance packages



| ID | Package | Outcome |

| --- | --- | --- |

| O01 | Cloud Run idle/scaling guardrails | `min=0`, `max=10`, concurrency `80`, 1 vCPU/512 MiB, CPU throttling; no idle instance charge by default |

| O02 | Immutable image plus best-effort layer cache | Faster repeat builds without making cache availability a deployment dependency |

| O03 | Path-filtered, superseded-run-canceling backend CI | Avoids unrelated Android/web compute and duplicate CI runs |

| O04 | Cheap liveness/readiness split | `/health` and `/ready` perform no paid dependency probes; low-frequency synthetics own remote checks |

| O05 | Reused R2 SDK client | Removes per-upload S3 client construction and its avoidable allocation/connection overhead |

| O06 | Backend-owned persistence policy | Removed device-local scheduled publishing/workers and duplicate placeholder screens that could lose user state |

| O07 | Bounded client work | Cursor/bounded discovery, UID-scoped cache, duplicate-load guards, partial-failure isolation |

| O08 | Android runtime/build hygiene | Atomic caches, primitive Compose state, version-catalog dependencies, current AGP/Gradle-compatible pins, zero lint errors |



## 5. UI parity result



The production PWA was measured at a 720 x 1600 mobile viewport and used as the

visual source of truth. Native Android now matches its primary indigo/teal dark

palette, ambient radial glows, glass surfaces, 16 dp card geometry, typography

hierarchy, five-destination responsive navigation, `vyb` branding, and minimum

touch targets. Phone layouts remain native rather than reproducing desktop

geometry.



The 100-point UI score remains explicitly owned by the product reviewer. The

remaining release gate is screenshot-based authenticated comparison for each

feature page on compact and large devices, not a known token or shell mismatch.



## 6. Known exclusions



- No production deploy, traffic shift, public artifact publication, Git commit,

  or push is implied by this audit.

- Marketplace creation-time attachments, community-chat realtime, message

  attachments, event form/team builder, durable game leaderboards, advanced

  Scribble controls, and Compose UI automation remain tracked gaps.

- A debug APK is QA-only. Public rollout still requires stable release signing,

  upgrade testing, Firebase/Play certificate verification, and staged rollout.
