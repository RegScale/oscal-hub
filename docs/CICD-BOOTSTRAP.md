# CI/CD Bootstrap

One-time setup to wire GitHub Actions to GCP for the Terraform-based deploy
flow. After this is done, `main`-branch merges automatically build, push, and
`terraform apply` against `terraform/gcp/`.

## Architecture

- **PR opened/updated** → `ci.yml` runs backend + frontend tests; `gcp-deploy.yml`
  runs `terraform fmt -check`, `terraform validate`, `terraform plan` and posts
  the plan summary as a PR comment. No infra changes.
- **PR merged to `main`** (gated by branch protection: required reviews +
  required CI checks) → `gcp-deploy.yml` builds the combined Docker image,
  pushes it to Artifact Registry, then runs `terraform apply` with the new
  `image_tag`. Health-check job follows.

Auth uses **Workload Identity Federation** — no long-lived service-account
keys in GitHub.

## Prerequisites

- `gcloud` CLI authenticated as a project owner (or someone with
  `roles/iam.workloadIdentityPoolAdmin`, `roles/iam.serviceAccountAdmin`,
  `roles/storage.admin`, and `roles/resourcemanager.projectIamAdmin`).
- `gh` CLI authenticated, or repo admin access in the GitHub UI to set
  secrets/variables.
- `terraform` ≥ 1.5 installed locally (one-time state migration).

Set shell vars used throughout:

```bash
export PROJECT_ID=oscal-hub
export PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')
export REGION=us-central1
export GH_REPO=<github-org>/oscal-cli   # <-- fill in
export STATE_BUCKET=${PROJECT_ID}-tfstate
```

## Step 1 — Create the Terraform state bucket

```bash
gcloud storage buckets create "gs://${STATE_BUCKET}" \
  --project="$PROJECT_ID" \
  --location="$REGION" \
  --uniform-bucket-level-access \
  --public-access-prevention

gcloud storage buckets update "gs://${STATE_BUCKET}" --versioning
```

## Step 2 — Migrate existing local state to GCS

The repo currently has `terraform/gcp/terraform.tfstate` on disk tracking the
live infra (Cloud Run service `oscal-tools-prod`, Cloud SQL
`oscal-db-prod-9dc1847d`, etc.). Push it to GCS *before* CI runs, otherwise
the first `terraform apply` from CI will try to create resources that already
exist.

The `backend "gcs" {}` block in `terraform/gcp/main.tf` is already enabled
(see Step 3 of this doc — done as part of the CI/CD setup commit).

```bash
cd terraform/gcp
terraform init -migrate-state \
  -backend-config="bucket=${STATE_BUCKET}" \
  -backend-config="prefix=terraform/state"
# Answer "yes" when prompted to copy local state to the new backend.
```

After confirming the migration:

```bash
gcloud storage ls "gs://${STATE_BUCKET}/terraform/state/"
# Should show default.tfstate
```

You can now delete the local state files (they're gitignored anyway):

```bash
rm -f terraform.tfstate terraform.tfstate.*backup
cd ../..
```

## Step 3 — Create the deploy service account

```bash
gcloud iam service-accounts create gh-deploy \
  --project="$PROJECT_ID" \
  --display-name="GitHub Actions deploy"

export DEPLOY_SA="gh-deploy@${PROJECT_ID}.iam.gserviceaccount.com"
```

Grant the roles Terraform + the image push need:

```bash
for ROLE in \
  roles/run.admin \
  roles/cloudsql.admin \
  roles/storage.admin \
  roles/secretmanager.admin \
  roles/artifactregistry.writer \
  roles/iam.serviceAccountUser \
  roles/serviceusage.serviceUsageAdmin \
  roles/compute.networkAdmin \
  roles/vpcaccess.admin
do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${DEPLOY_SA}" \
    --role="$ROLE" \
    --condition=None
done
```

> If you don't use VPC connectors or Secret Manager today (the current
> `main.tf` has them commented out), drop those two roles. Add them back if
> you uncomment those modules.

## Step 4 — Create the Workload Identity Pool + Provider

```bash
gcloud iam workload-identity-pools create github \
  --project="$PROJECT_ID" \
  --location=global \
  --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers create-oidc github-provider \
  --project="$PROJECT_ID" \
  --location=global \
  --workload-identity-pool=github \
  --display-name="GitHub OIDC" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping='google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner,attribute.ref=assertion.ref' \
  --attribute-condition="assertion.repository=='${GH_REPO}'"
```

Allow the GitHub repo to impersonate the deploy SA:

```bash
gcloud iam service-accounts add-iam-policy-binding "$DEPLOY_SA" \
  --project="$PROJECT_ID" \
  --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/attribute.repository/${GH_REPO}"
```

Capture the provider resource name — you'll paste it into a GitHub variable
in the next step:

```bash
echo "projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/providers/github-provider"
```

## Step 5 — Configure GitHub repo secrets and variables

Variables (non-sensitive, visible in logs):

| Name | Value |
|---|---|
| `GCP_PROJECT_ID` | `$PROJECT_ID` (e.g. `oscal-hub`) |
| `GCP_REGION` | `$REGION` (e.g. `us-central1`) |
| `GCP_WIF_PROVIDER` | output from Step 4 |
| `GCP_DEPLOY_SA` | `$DEPLOY_SA` |
| `TF_STATE_BUCKET` | `$STATE_BUCKET` |

Set them via `gh`:

```bash
gh variable set GCP_PROJECT_ID --body "$PROJECT_ID" --repo "$GH_REPO"
gh variable set GCP_REGION --body "$REGION" --repo "$GH_REPO"
gh variable set GCP_WIF_PROVIDER --body "projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github/providers/github-provider" --repo "$GH_REPO"
gh variable set GCP_DEPLOY_SA --body "$DEPLOY_SA" --repo "$GH_REPO"
gh variable set TF_STATE_BUCKET --body "$STATE_BUCKET" --repo "$GH_REPO"
```

No secrets are required for the deploy itself — WIF replaces `GCP_SA_KEY`.
You can delete the old `GCP_SA_KEY` secret if it's still set:

```bash
gh secret delete GCP_SA_KEY --repo "$GH_REPO" 2>/dev/null || true
```

## Step 6 — Branch protection on `main`

CI/CD assumes only reviewed PRs land on `main`. Confirm in
**Settings → Branches → Branch protection rules** for `main`:

- ✅ Require a pull request before merging (1+ approvals)
- ✅ Require status checks to pass before merging — select:
  - `Build and Test` (from `ci.yml`)
  - `Terraform Plan` (from `gcp-deploy.yml`)
- ✅ Require branches to be up to date before merging
- ✅ Do not allow bypassing the above settings

## Step 7 — First run

Push a no-op PR to confirm:

1. `Build and Test` runs.
2. `Terraform Plan` runs and posts a comment on the PR. The plan should show
   **"No changes"** if Step 2 (state migration) was done correctly. If it
   shows resources being created, **stop** — your state didn't migrate, and
   merging will try to recreate live infra.
3. Merge to `main` → `Build and Push` → `Terraform Apply` → `Health Check`.

## Troubleshooting

**"Permission denied" on `gcloud iam workload-identity-pools` calls**
You need `roles/iam.workloadIdentityPoolAdmin` on the project.

**Plan shows resources being recreated**
Local state didn't migrate. Re-run Step 2 with `-migrate-state` from the
machine that has the original `terraform.tfstate`.

**Apply fails with `Error 409: ... already exists`**
The bucket/SQL/Cloud Run resource exists in GCP but isn't in Terraform state.
Import it manually, e.g.:

```bash
cd terraform/gcp
terraform init  # picks up the GCS backend
terraform import 'module.oscal_app.google_cloud_run_v2_service.app' \
  "projects/${PROJECT_ID}/locations/${REGION}/services/oscal-tools-prod"
```

(Resource address depends on the module — check `terraform state list`.)

**WIF auth fails: `Permission 'iam.serviceAccounts.getAccessToken' denied`**
The repo isn't bound to the SA. Re-run the
`add-iam-policy-binding ... workloadIdentityUser` command from Step 4 and
verify `$GH_REPO` matches the actual repo (case-sensitive).
