variable "project_id"  { type = string }
variable "environment" { type = string }
variable "region"      { type = string }
variable "kms_key_id"  {
  type    = string
  default = null
}
variable "events_partition_expiration_days" {
  type    = number
  default = 400
}
