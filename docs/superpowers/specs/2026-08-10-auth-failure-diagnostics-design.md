# Diagnosable Authentication Failures

**Date:** 2026-08-10
**Status:** Approved, ready for implementation planning
**Branch:** `feat/long-running-service-tokens`

## Problem

The Trust Center integration could not authenticate against OSCAL Hub and spent
a day suspecting the service-account token lifetime. The token was never the
problem. Every 401 the API returns for a pre-authentication failure carries the
same fixed body from `SecurityConfig.authenticationEntryPoint()`:

```json
{"error": "Unauthorized", "message": "Authentication required or token invalid"}
```

That one string covers a missing header, a wrong auth scheme, a malformed token,
an expired token, and an invalid signature. `JwtAuthenticationFilter` already
distinguishes all of them and writes each to the server log — then discards the
distinction before answering the caller. The diagnosis exists on the server and
never reaches the client.

### Evidence

Cloud Run logs for `oscal-tools-prod`, full 30-day retention window, queried
2026-08-10 (note: application logs land in `jsonPayload.message`, not
`textPayload`):

- **Zero** `"Rejected a ..."` events. The service-token gate — legacy `jti`,
  unknown `jti`, and revoked paths — never fired. No service-account token has
  been turned away on its merits.
- One `Malformed JWT token for request to /api/validate: ... Found: 0` at
  `11:56:14.701Z`. Zero period characters means the `Authorization` value was
  not a JWT at all, consistent with ciphertext sent un-decrypted or a truncated
  paste. A token had been minted successfully at `11:52:46` minutes earlier.
- Four 401s from `curl/8.7.1` at `24.214.27.233` (three at `11:56:14`, one at
  `12:14:06`) and several from `python-requests/2.34.2` at `108.236.237.176` on
  2026-08-09. All but the one above logged **nothing**, which is the signature of
  an absent `Authorization` header or one not beginning with `Bearer ` —
  `JwtAuthenticationFilter` only parses the `Bearer ` prefix and otherwise falls
  through silently.

Long-lived tokens already work: `app.service-tokens.max-expiration-days`
defaults to 3650, and `exp` is minted from the persisted `expires_at`, so a
requested expiry is honored exactly.

## Goals

Make every authentication failure explain itself to the caller, so an
integrator can tell "you sent no credential" from "your token expired" from
"your token was signed by another environment" without server log access.

## Non-goals

Deferred deliberately; each is real but independent of this change:

- Replacing `ResponseEntity<?>` with typed DTOs so OpenAPI renders real auth
  schemas, and declaring the 3650-day `maximum` on `expirationDays`.
- The `expiresAt` timezone bug in `AuthController` — a literal `'Z'` stamped
  onto a system-default-zone timestamp.
- Restricting or documenting `/api/auth/refresh`, which currently mints a
  session token from the username and silently downgrades a service token.
- Any change to the Trust Center client, which lives in another repository.

## Approach

The filter classifies the failure and records it; the entry point renders it.

`JwtAuthenticationFilter` stores an `AuthFailure` as a request attribute instead
of only logging. The existing `AuthenticationEntryPoint` reads that attribute
and renders one consistent body, falling back to today's generic message when
no attribute is present.

Two alternatives were considered and rejected:

- **Filter writes responses directly** for every failure branch, extending the
  pattern the service-token gate uses. Smaller diff, but it leaves two divergent
  renderers, puts response formatting in the filter, and cannot answer a request
  with no `Authorization` header at all — Spring Security owns that path.
- **Throw a typed `AuthenticationException`** and let Spring translate it. More
  idiomatic on paper, but whether the entry point receives the exception depends
  on filter ordering relative to `ExceptionTranslationFilter`, which makes it
  fragile for no gain.

### Correctness constraint

For a missing or bad token the filter **records and continues** — it never
responds. Public endpoints (`/api/health`, `/api/auth/login`) arrive with no
`Authorization` header and must keep working, so only Spring Security may
conclude a request was unauthorized, and the entry point renders at that moment.

The service-token gate is the sole exception: it fires on an already-validated
token, so responding immediately is correct and preserves existing behavior.

## Error contract

Every 401 from the auth layer:

```json
{
  "error": "Unauthorized",
  "message": "Token was signed with a different key. It may have been issued by another environment.",
  "code": "invalid_signature"
}
```

`error` remains the literal `"Unauthorized"`, so any client keying off it is
unaffected. `message` carries actionable prose. `code` is the stable string an
integration branches on. The frontend already prefers `message` over `error`
(`front-end/src/lib/api-client.ts:135`), so its user-facing messages improve
with no frontend change.

This also normalizes the service-token gate, which today puts prose in `error`
and carries no code.

| `code` | Cause |
|---|---|
| `missing_credentials` | No `Authorization` header |
| `unsupported_auth_scheme` | Header present, not `Bearer ` |
| `malformed_token` | Not a parseable JWT |
| `token_expired` | Past `exp`; includes `expiredAt` |
| `invalid_signature` | Signed with a different key |
| `service_token_revoked` | Gate: `revoked_at` set |
| `service_token_unknown` | Gate: no row for `jti` |
| `service_token_legacy` | Gate: token predates `jti` support |
| `invalid_token` | Catch-all |

Two additions:

- **`WWW-Authenticate`**, per RFC 6750: `Bearer error="invalid_token",
  error_description="..."`, and a bare `Bearer` for `missing_credentials`. One
  line, and standard HTTP clients surface the reason without parsing our body.
- **`expiredAt` on `token_expired`.** The filter already holds the exact expiry
  in the `ExpiredJwtException` it catches and discards. Returning it lets a
  client report "your token expired on 2026-08-08" instead of "could not
  validate."

  Format is ISO-8601 instant in **UTC** (`2026-08-08T21:36:19Z`), produced via
  `DateTimeFormatter.ISO_INSTANT` from the exception's expiry `Date`. It must not
  be built by stamping a literal `Z` onto a system-default-zone value — that is
  precisely the existing `AuthController` bug listed under non-goals, and
  repeating it here would put a wrong timestamp in a new place. The field is
  omitted from the JSON entirely when null, so no other code carries it.

### Disclosure

Every code goes only to a caller who already holds the credential and already
knows it was rejected, so nothing leaks that the 401 did not. This extends the
judgment already documented for the service-token gate in
`JwtAuthenticationFilter`, rather than making a new one.

## Structure

| File | Change |
|---|---|
| `security/AuthFailure.java` | New. Record `(String code, String message, String expiredAt)` with a static factory per code, so the contract is readable in one file. Holds the request-attribute key. |
| `security/AuthFailureRenderer.java` | New. Writes status, `WWW-Authenticate`, and a Jackson-serialized body. Shared by the filter and the entry point; removes the hand-built JSON. |
| `security/JwtAuthenticationFilter.java` | Each catch block and the two silent branches record an `AuthFailure`. The service-token gate returns `AuthFailure` instead of `String` and renders through the shared renderer. Existing `warn` logs keep their wording with `code` appended for correlation. |
| `config/SecurityConfig.java` | Entry point reads the attribute and renders it; falls back to `missingCredentials()`. |

## Testing

- `AuthFailureTest` — code/message pairing and `WWW-Authenticate` formatting.
- `JwtAuthenticationFilterDiagnosticsTest` — one case per branch asserting the
  recorded attribute, plus a regression guard that a no-header request to a
  public path writes no response and sets no authentication. Follows the
  existing `ReflectionTestUtils` field-injection pattern in
  `JwtAuthenticationFilterServiceTokenTest`.
- Entry-point test — renders the attribute when present, generic when absent,
  and emits valid JSON when a message contains a quote character.
- MockMvc integration test against a protected endpoint asserting `code` for a
  missing header, a malformed token, and an expired token — the three failures
  actually observed in production.

## Verification

Implementation is complete when the three observed production failures each
return a distinct `code`, the full backend test suite passes, and a request to
`/api/health` with no `Authorization` header still returns 200.
