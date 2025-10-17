terraform {
  required_version = ">= 1.5.0"

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 3.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "~> 2.0"
    }
  }

  # Backend configuration for storing Terraform state
  # Uncomment and configure after creating storage account
  # backend "azurerm" {
  #   resource_group_name  = "oscal-tfstate-rg"
  #   storage_account_name = "oscaltfstate"
  #   container_name       = "tfstate"
  #   key                  = "oscal-cli.terraform.tfstate"
  # }
}

provider "azurerm" {
  features {
    resource_group {
      prevent_deletion_if_contains_resources = false
    }

    key_vault {
      purge_soft_delete_on_destroy    = true
      recover_soft_deleted_key_vaults = true
    }
  }
}

provider "azuread" {}
