# GitHub Secrets Setup for GCP Deployment

**Quick Reference Guide**

Since you already have your GCP service account set up, you just need to configure GitHub secrets to enable automatic deployment.

## Quick Copy Commands

Run these from your project root to get the values you need:

```bash
# 1. Copy service account key to clipboard (macOS)
cat terraform/gcp/terraform-key.json | pbcopy

# 2. Get your project ID
echo "oscal-hub"
```

Then add these to GitHub:
- Secret `GCP_SA_KEY`: Paste from clipboard (Step 1)
- Secret `GCP_PROJECT_ID`: `oscal-hub`

---

## Step 1: Get Your Service Account Key

Your service account key is already on your machine. Here's where to find it:

**Location**: `terraform/gcp/terraform-key.json` (in your project root)

**To view the key**:

```bash
# From your project root
cat terraform/gcp/terraform-key.json
```

**Your Configuration**:
- **Project ID**: `oscal-hub`
- **Service Account**: `terraform-sa@oscal-hub.iam.gserviceaccount.com`
- **Key File**: `terraform/gcp/terraform-key.json`

## Step 2: Add Secrets to GitHub

### Navigate to GitHub Secrets

1. Go to your GitHub repository: `https://github.com/your-org/oscal-cli`
2. Click **Settings** (top right)
3. In left sidebar, click **Secrets and variables** → **Actions**
4. You'll see two tabs: **Secrets** and **Variables**

### Add Required Secrets

Click **New repository secret** and add each of these:

#### Secret 1: `GCP_SA_KEY`

- **Name**: `GCP_SA_KEY`
- **Value**: Paste the **entire contents** of `terraform/gcp/terraform-key.json`
  - It should start with `{` and end with `}`
  - Should be about 2,400 characters
  - Include all the JSON structure

**To copy the key** (run from project root):

```bash
# Copy to clipboard (macOS)
cat terraform/gcp/terraform-key.json | pbcopy

# Or display it to copy manually
cat terraform/gcp/terraform-key.json
```

#### Secret 2: `GCP_PROJECT_ID`

- **Name**: `GCP_PROJECT_ID`
- **Value**: `oscal-hub`

This is your GCP project ID that's already configured in gcloud.

### Optional: Add Variables (Recommended)

Click the **Variables** tab, then **New repository variable**:

#### Variable 1: `GCP_REGION` (optional)

- **Name**: `GCP_REGION`
- **Value**: `us-central1` (or your preferred region)

> **Note**: If you don't set this, it defaults to `us-central1`

## Step 3: Verify Setup

After adding the secrets, verify them:

1. Go back to **Secrets and variables** → **Actions** → **Secrets** tab
2. You should see:
   - ✅ `GCP_SA_KEY` - Updated X seconds/minutes ago
   - ✅ `GCP_PROJECT_ID` - Updated X seconds/minutes ago

**Important**: GitHub never shows secret values after they're saved. This is normal security behavior.

## Step 4: Test the Deployment

### Option A: Manual Test (Recommended First)

1. Go to **Actions** tab in GitHub
2. Click **Deploy to Google Cloud Run** workflow
3. Click **Run workflow** button
4. Choose environment: `dev` (test environment)
5. Click **Run workflow**

Watch the workflow run. It should:
- ✅ Build and test (3-5 minutes)
- ✅ Build and push images (5-10 minutes)
- ✅ Deploy to Cloud Run (2-3 minutes)
- ✅ Run health checks (1 minute)

### Option B: Automatic Test

Merge any PR to `main` branch. The workflow will automatically trigger and deploy to `prod` environment.

## Troubleshooting

### Error: "Unable to find the current active account"

**Cause**: `GCP_SA_KEY` secret is incorrect or not set

**Solution**:
- Verify you pasted the entire JSON content
- Check for extra spaces or line breaks
- Re-create the secret with fresh JSON

### Error: "Permission denied"

**Cause**: Service account doesn't have required permissions

**Solution**:
Verify your service account has these roles:
```bash
gcloud projects get-iam-policy oscal-hub \
  --flatten="bindings[].members" \
  --filter="bindings.members:serviceAccount:terraform-sa@oscal-hub.iam.gserviceaccount.com"
```

Should show:
- `roles/run.admin`
- `roles/iam.serviceAccountUser`
- `roles/artifactregistry.admin`
- `roles/storage.admin`

**To add missing roles** (if needed):
```bash
# Example: Add Cloud Run Admin role
gcloud projects add-iam-policy-binding oscal-hub \
  --member="serviceAccount:terraform-sa@oscal-hub.iam.gserviceaccount.com" \
  --role="roles/run.admin"
```

### Error: "API not enabled"

**Cause**: Required GCP APIs aren't enabled

**Solution**:
```bash
gcloud services enable run.googleapis.com
gcloud services enable artifactregistry.googleapis.com
gcloud services enable cloudbuild.googleapis.com
```

## Security Notes

- ✅ **Never commit** `terraform/gcp/terraform-key.json` to Git
  - This file should already be in `.gitignore`
  - Verify with: `git check-ignore terraform/gcp/terraform-key.json`
- ✅ **Keep the key file secure** on your local machine
  - It's needed for local Terraform deployments
  - Only share via GitHub secrets for CI/CD
- ✅ **Rotate keys regularly** (every 90 days recommended)
- ✅ **Use least-privilege** roles for service account

## Next Steps

After successful deployment:

1. **Access your deployed application**:
   - Frontend: Check the GitHub Actions summary for the URL
   - Backend: Check the GitHub Actions summary for the URL

2. **Monitor deployments**:
   ```bash
   # View deployed services
   gcloud run services list --platform managed

   # View logs
   gcloud logging read "resource.type=cloud_run_revision" --limit 20
   ```

3. **Set up custom domain** (optional):
   See `docs/GCP-DEPLOYMENT-SETUP.md` for custom domain instructions

## Quick Reference

| Secret/Variable | Type | Required | Default | Description |
|----------------|------|----------|---------|-------------|
| `GCP_SA_KEY` | Secret | ✅ Yes | - | Service account JSON key |
| `GCP_PROJECT_ID` | Secret/Variable | ✅ Yes | - | Your GCP project ID |
| `GCP_REGION` | Variable | ⚪ No | `us-central1` | Deployment region |

---

**You're all set!** Once these secrets are configured, every PR merge to `main` will automatically deploy to GCP Cloud Run.
