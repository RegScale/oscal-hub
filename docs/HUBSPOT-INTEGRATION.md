# HubSpot Marketing CRM Integration

**Date**: 2026-07-26
**Status**: Implemented; requires a HubSpot Service Key to activate

> Authentication uses a HubSpot **Service Key** — HubSpot's supported
> credential for single-account, system-to-system API access. (Private apps
> are legacy; Service Keys need no app management and authenticate with the
> same `Authorization: Bearer` header. Note Service Keys don't support
> webhooks — not needed here, this integration only pushes.)

## What it does

Every new OSCAL Hub account and every self-serve organization signup is
registered in HubSpot for OSCAL-related product/service marketing:

| App event | HubSpot action |
|---|---|
| Self-serve registration | Contact upserted by email (`lifecyclestage=lead`) |
| New user via invitation accept | Contact upserted by email |
| New user via access-request approval | Contact upserted by email |
| Self-serve organization signup | Company created (deduped by name) + associated with the owner contact |

Notes:
- Contacts are **upserted by email**, so retries and multi-org users never
  create duplicates. Only standard HubSpot properties are used (email,
  firstname, lastname, lifecyclestage; company name/description) — no
  custom-property setup needed.
- Existing accounts accepting an invitation are **not** re-registered — only
  new account creation syncs.
- The acquisition path (self_serve_registration / invitation /
  access_request_approval) is logged app-side per sync.

## Reliability model

Sync runs AFTER the signup transaction commits, on an async executor, with one
retry (`CrmSyncListener`, same pattern as transactional email). A HubSpot
outage can never slow or fail onboarding — at worst a lead is logged as lost:

```
CRM sync FAILED after 2 attempts (contact x@y.com): ...
```

With no token configured the integration is a logged no-op
(`[crm-noop] would have synced contact ...`).

## User-facing consent

All three account-creation surfaces (signup form, invitation accept,
request-access) display:

> OSCAL Hub is free to use. By creating an account, you agree to receive
> occasional emails about new OSCAL-related products and services. You can
> unsubscribe at any time.

Unsubscribes are handled by HubSpot's standard email subscription management.

## Setup (one-time, HubSpot side)

1. In HubSpot (requires Super Admin or developer-tools access):
   **Settings → Integrations → Service Keys → Create a service key**.
2. Name it (e.g. "oscal-hub-signup-sync") and grant only these CRM scopes
   (least privilege):
   - `crm.objects.contacts.read` + `crm.objects.contacts.write`
   - `crm.objects.companies.read` + `crm.objects.companies.write`
3. Copy the key immediately after creation (it is shown once).
4. Provide it to the app:
   - **Prod (pipeline)**: add a GitHub Actions **secret** named
     `HUBSPOT_SERVICE_KEY` on the repo — the deploy workflow passes it to
     Terraform, which sets it on Cloud Run. Next deploy activates the sync.
   - **Local/dev**: set env var `HUBSPOT_SERVICE_KEY`.
5. Optional knobs: `HUBSPOT_ENABLED=false` disables the sync without removing
   the key; `HUBSPOT_BASE_URL` overrides the API host (tests/proxies).

Service Keys can be rotated in HubSpot without code changes — update the
GitHub secret and redeploy. HubSpot audit-logs key usage, and the key is
account-owned, so it keeps working if the person who created it leaves.

## Verifying

- Startup log line: `HubSpot CRM sync enabled (https://api.hubapi.com)`.
- Register a test user → contact appears in HubSpot within seconds; with an
  organization name → company appears, associated to the contact.
- Failures surface in Cloud Logging as `CRM sync FAILED` (WARN/ERROR).

## Code map

- `crm/CrmService.java` — integration boundary (HubSpot + NoOp impls)
- `crm/HubSpotCrmService.java` — CRM v3 batch-upsert contacts, v3 company
  search/create, v4 default association
- `crm/CrmEvents.java` / `crm/CrmSyncListener.java` — after-commit async sync
- Publishers: `AuthService.register`, `OrganizationService.createOrganizationForUser`,
  `InvitationService.acceptInvitation` (new-user branch),
  `UserAccessRequestService.createUserFromRequest`
- Config: `application.properties` (`hubspot.*`), `terraform/gcp/variables.tf`
  (`hubspot_service_key`), `gcp-deploy.yml` (secret pass-through)
