{{/*
Expand the name of the chart.
*/}}
{{- define "oscal-hub.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "oscal-hub.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Chart name + version label.
*/}}
{{- define "oscal-hub.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels.
*/}}
{{- define "oscal-hub.labels" -}}
helm.sh/chart: {{ include "oscal-hub.chart" . }}
{{ include "oscal-hub.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels.
*/}}
{{- define "oscal-hub.selectorLabels" -}}
app.kubernetes.io/name: {{ include "oscal-hub.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
ServiceAccount name.
*/}}
{{- define "oscal-hub.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "oscal-hub.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{/*
Name of the application secret (the one this chart creates, when no
existingSecret is provided).
*/}}
{{- define "oscal-hub.secretName" -}}
{{- if .Values.secrets.existingSecret -}}
{{- .Values.secrets.existingSecret -}}
{{- else -}}
{{- printf "%s-secrets" (include "oscal-hub.fullname" .) -}}
{{- end -}}
{{- end -}}

{{/*
Resolve the public origin (used for CORS). Falls back to https://<ingress.host>.
*/}}
{{- define "oscal-hub.publicOrigin" -}}
{{- if .Values.config.publicOrigin -}}
{{- .Values.config.publicOrigin -}}
{{- else if .Values.ingress.host -}}
{{- if .Values.ingress.tls.enabled -}}https://{{ .Values.ingress.host }}{{- else -}}http://{{ .Values.ingress.host }}{{- end -}}
{{- else -}}
{{- fail "Either .Values.config.publicOrigin or .Values.ingress.host must be set" -}}
{{- end -}}
{{- end -}}

{{/*
JDBC URL for the database — internal Postgres if bundled, otherwise external.
*/}}
{{- define "oscal-hub.dbUrl" -}}
{{- if .Values.postgresql.enabled -}}
{{- $svc := printf "%s-postgresql" .Release.Name -}}
jdbc:postgresql://{{ $svc }}:5432/{{ .Values.postgresql.auth.database }}
{{- else -}}
jdbc:postgresql://{{ required ".Values.externalDatabase.host is required when postgresql.enabled=false" .Values.externalDatabase.host }}:{{ .Values.externalDatabase.port }}/{{ .Values.externalDatabase.database }}
{{- end -}}
{{- end -}}

{{/*
DB username.
*/}}
{{- define "oscal-hub.dbUsername" -}}
{{- if .Values.postgresql.enabled -}}
{{- .Values.postgresql.auth.username -}}
{{- else -}}
{{- .Values.externalDatabase.username -}}
{{- end -}}
{{- end -}}
