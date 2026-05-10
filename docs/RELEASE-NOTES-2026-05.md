# What's New in OSCAL Hub — May 2026

A major release lands today. The headline is that **OSCAL Hub now generates compliance documentation from natural-language inputs**, plus a full visual builder for every OSCAL model type, multi-tenant authorization packages, continuous-monitoring ingestion for FedRAMP POA&Ms, and a public catalog of shareable OSCAL data products. There's also a built-in user guide and an in-app ticketing system so questions and requests no longer scatter across email and Slack.

Below: each new capability, the feature that implements it, and the benefit you'll get.

---

## 1. AI assistance for every OSCAL document type

**Use case:** Authoring an OSCAL document from scratch is slow — you start from a blank page, look up control language, write narratives, and reformat into the OSCAL JSON/XML schema by hand. Most teams give up and stay in spreadsheets.

**Feature:** The new **AI Wizards** generate a working OSCAL draft from a PDF, a URL, or pasted text:

- **Catalog Wizard** — paste a control catalog (NIST 800-53, FedRAMP baseline, internal policy doc), get a valid OSCAL Catalog with controls and parameters wired up.
- **Component Definition Wizard** — feed it a STIG, SCAP benchmark, or CIS guide; it produces an OSCAL Component Definition with each rule mapped to controls.
- **SSP Wizard** — pick a profile, drop in your existing security plan PDF, get an SSP draft with control implementations populated and ready to refine.
- **POA&M Wizard** — describe the gap or upload a finding; it produces a structured POA&M item.
- **AI Rule Generator** — describe a custom validation rule in plain English ("flag any control that lacks an implementation status"), and it generates Metapath constraint code that you can edit, test, and save.

**How it works:** Bring your own Anthropic API key — each organization configures its own at `Org Admin → AI Settings`. The key is encrypted at rest and never leaves your tenant. Token usage and cost are tracked per session in an Org-Admin analytics dashboard.

**Benefit:** Time-to-first-draft drops from hours to minutes. The output is real OSCAL — schema-valid, reviewable, editable, not a "summary." You stay in control of the AI provider and spend.

---

## 2. Visual builders for every OSCAL document type

**Use case:** Teams need to *edit* OSCAL documents — fix a control implementation, update a metadata field, regenerate a UUID — without hand-editing JSON or XML.

**Feature:** A **structured builder** for every OSCAL type:

- Catalog
- Profile
- Component Definition
- System Security Plan (SSP)
- Assessment Plan
- Assessment Results
- Plan of Action & Milestones (POA&M)

Each builder is step-based with a Validate panel on every step, a structured controls editor with autocomplete, dark-mode Monaco code editor for power users, and one-click UUID regeneration. AI-generated drafts hand off directly into the builder for refinement.

**Benefit:** Anyone on the compliance team can edit OSCAL documents — not just engineers comfortable in JSON. Validation errors surface inline as you type, so you catch issues before export.

---

## 3. OSCAL Data Products — share and discover catalogs publicly

**Use case:** Many organizations have OSCAL artifacts they want to publish (control overlays, baseline mappings, component libraries) but no good place to host them. Customers/partners need to discover and download those artifacts without creating accounts.

**Feature:** **Public Catalog** at `/catalog` — anonymous browsing of any OSCAL artifact you mark as public, with a polished detail page per item, search, and downloads.

Pair it with **Save to Library** (a button on every builder) and the **three-tier visibility model** — Private, Organization, Public — to control who sees what:

- Private: only you
- Organization: anyone in your org
- Public: world-readable, indexed by the public catalog

Publishing is one click; unpublishing is one click; downloads of public items are gated to authenticated users so you keep attribution data.

**Benefit:** Turn your internal compliance work into a public knowledge resource (or browse what others have published). Customers and partners can find your published OSCAL content without needing accounts; your internal teams keep all private documents fully isolated.

---

## 4. Multi-tenant authorization packages with role-based access

**Use case:** A single OSCAL Hub instance now hosts authorization packages for many organizations, with strict isolation between them and granular permissions inside each.

**Feature:** **Authorizations** are now first-class, multi-tenant objects:

- **Org isolation** — every authorization belongs to exactly one organization; users from other orgs can't see, list, or access it.
- **ACL grants** — share an individual authorization with specific users at specific roles (Owner, Editor, Reviewer, Viewer). Or set a default share-with-org role so anyone in your organization gets a baseline level of access.
- **Tabbed detail page** — Overview (Sharing & Access), Continuous Monitoring, and Documents tabs.
- **Role-help dialog** — explains what each role can do, inline.

**Benefit:** A single tenancy-aware hub for ATO packages: you can host federal/agency authorizations alongside customer or internal ones, with auditable role-based access on each. RBAC is enforced end-to-end; integration tests cover every role × action combination.

---

## 5. Continuous Monitoring (ConMon) for FedRAMP POA&Ms

**Use case:** ConMon teams ingest a fresh FedRAMP POA&M Excel each month and need to reconcile it against the prior snapshot — what's new, what closed, what aged out, what changed severity.

**Feature:** **ConMon tab** on every authorization with:

- **Upload dialog** — drop in a FedRAMP Rev 5 POA&M XLSX or an OSCAL POA&M JSON.
- **Reconciliation banner** — six-category diff against the prior snapshot (new, closed, modified, unchanged, removed, re-opened).
- **Analytics dashboard** — KPI tiles (Open/Overdue/Closed/SLA), current-severity pie, four time-series charts (aging, trend, severity distribution, status mix).
- **Items drawer** — paginated tables for Analytics / Open / Overdue / Closed sub-tabs, server-side filtered.
- **OSCAL POA&M parsing** — rolls up linked-risk statuses for accurate item-level state.

**Benefit:** Replace ad-hoc Excel diffing and manual chart-making with one click per snapshot. Auditors and program managers see exactly what changed since the last reporting cycle, with the full lineage preserved.

---

## 6. Custom validation rules — write them, or have AI write them

**Use case:** Out-of-the-box OSCAL validation catches schema and metaschema issues, but every organization has policy-specific rules ("control X must reference parameter Y", "all SSPs must have an implementation status") that aren't in the standard.

**Feature:** **Custom Rules engine** — author Metapath constraints, save them, and they run on every OSCAL validation against documents in your organization. The **AI Rule Generator** writes the constraint code from a plain-English description, with a synthetic test runner that generates positive and negative examples to verify the rule before you save it.

**Benefit:** Encode your organization's compliance policy as code. New documents are validated against both the OSCAL standard *and* your internal rules, automatically. The AI generator means non-engineers can author rules.

---

## 7. In-app Ticketing — bug reports and feature requests without leaving the app

**Use case:** Users report bugs and request features; today that scatters across email, Slack, and "shoot me a message". Tracking, routing, and closing the loop is manual.

**Feature:** **Tickets** built into the app:

- "Open Ticket" in the avatar menu opens a bug or feature-request form with optional file attachments.
- "My Tickets" shows everything you've filed, with search and filters.
- **Per-ticket thread** — reply, attach files, see system messages (status changes, auto-close).
- **Admin panel** at `/admin/tickets` — search, filter, paginate, change status, and a 5-metric analytics panel showing ticket volume, age, and resolution time.
- **Email notifications** at every state transition (created, replied, status-changed, resolved, reopened).
- **Auto-close** — tickets in "Resolved" state for 7 days close automatically, with a system comment explaining why.

**Benefit:** A single audited channel for support. No ticket lost to inbox sprawl; both reporters and admins always know the state and history.

---

## 8. Built-in user guide

**Use case:** "Where do I configure X?" / "How do I do Y?" — questions that are answered the same way every time deserve a permanent home, not a Slack thread.

**Feature:** A full **user guide** at `/guide`, ~50 pages organized into 10 sections (Getting Started, Core Tools, Build, AI, Library & Sharing, Authorizations, Account, Org Admin, Super Admin, Reference). Sticky sidebar, mobile drawer, deep-linkable headings, MDX components for callouts and step-by-step walk-throughs. Plus a **HelpButton** on every screen that takes you straight to the relevant guide page.

**Benefit:** Self-service onboarding. New users can answer their own "how do I" questions; the support team isn't the documentation.

---

## 9. Quality and reliability

Behind the scenes, this release also includes:

- **Production-grade infrastructure sizing** — Cloud SQL, Cloud Run services, and the dimsync background worker all have explicit, codified resource baselines that prevent OOM-restart and cold-start outages. Capped at 3 max instances each for cost predictability; documented in `docs/PRODUCTION-SIZING.md`.
- **Schema management hardening** — Flyway is now the schema authority. Migrations are required for entity changes; silent schema drift is no longer possible. Documented in `CLAUDE.md`.
- **CI/CD reliability** — GitHub Actions deploy pipeline now correctly fails on terraform plan errors (was silently passing); IAM and OTel/analytics infrastructure documented for fresh deploys.
- **2,482 backend tests + 212 frontend tests** — full RBAC matrices, role-progression flows, reconciliation scenarios, and end-to-end smoke tests for the AI wizards.
- **Security baseline updates** — Bouncy Castle 1.84 (timing-channel CVE fix), Ubuntu Noble base image (libnghttp2 / libpng16 CVE fixes), Next.js 16.2.6 (vite path-traversal CVE fix).

---

## How to try it

- **AI Wizards:** Org Admin grabs an Anthropic API key, sets it at `/org-admin/ai-settings`, then anyone in the org sees `/ai/wizard` in the nav.
- **Visual Builder:** click "Build" in the top nav.
- **Public Catalog:** browse `/catalog` (no login required); save anything from your library to the public tier via the visibility menu.
- **Authorizations:** `/authorizations` — create one, share with team members, attach docs and POA&Ms.
- **Tickets:** click your avatar → "Open Ticket" or "My Tickets".
- **User Guide:** `/guide` from the top nav, or click the **?** HelpButton anywhere in the app.

Questions, feedback, or feature requests — file them via the new ticketing system. Or just reply to this email.
