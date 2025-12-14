# ============================================================================#
# Cloud Storage Module
# ============================================================================#

# Build storage bucket for OSCAL component definitions
resource "google_storage_bucket" "build_bucket" {
  name          = "${var.bucket_prefix}-build-${var.environment}-${var.region}"
  location      = var.region
  project       = var.project_id
  storage_class = "STANDARD"

  # Versioning for data protection
  versioning {
    enabled = true
  }

  # Lifecycle rules to manage storage costs
  lifecycle_rule {
    condition {
      num_newer_versions = 5
      with_state         = "ARCHIVED"
    }
    action {
      type = "Delete"
    }
  }

  lifecycle_rule {
    condition {
      age            = 90
      with_state     = "ANY"
      matches_prefix = ["temp/", "cache/"]
    }
    action {
      type = "Delete"
    }
  }

  # Uniform bucket-level access (recommended)
  uniform_bucket_level_access = true

  # Encryption (Google-managed by default, can use CMEK)
  # encryption {
  #   default_kms_key_name = var.kms_key_name
  # }

  # CORS configuration for web access
  cors {
    origin          = ["*"] # Restrict this in production
    method          = ["GET", "HEAD", "PUT", "POST", "DELETE"]
    response_header = ["*"]
    max_age_seconds = 3600
  }

  # Labels for organization
  labels = {
    environment = var.environment
    managed-by  = "terraform"
    purpose     = "oscal-build-storage"
  }

  # Prevent accidental deletion in production
  force_destroy = var.environment != "prod"
}

# Library storage bucket for shared OSCAL content (optional)
resource "google_storage_bucket" "library_bucket" {
  name          = "${var.bucket_prefix}-library-${var.environment}-${var.region}"
  location      = var.region
  project       = var.project_id
  storage_class = "STANDARD"

  versioning {
    enabled = true
  }

  uniform_bucket_level_access = true

  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD"]
    response_header = ["*"]
    max_age_seconds = 3600
  }

  labels = {
    environment = var.environment
    managed-by  = "terraform"
    purpose     = "oscal-library-storage"
  }

  force_destroy = var.environment != "prod"
}
