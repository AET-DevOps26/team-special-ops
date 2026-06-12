{{/*
Common naming + label helpers.
*/}}

{{- define "tso.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{/*
Standard labels applied to every object. helm.sh/chart + managed-by give Helm
ownership; app.kubernetes.io/part-of groups the whole stack in dashboards.
*/}}
{{- define "tso.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
app.kubernetes.io/part-of: tso
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/*
Per-service selector labels. Pass a dict: {root: $, name: <service name>}.
Selector labels must be stable, so keep this minimal.
*/}}
{{- define "tso.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
{{- end -}}

{{/*
The image tag actually deployed: explicit image.tag, else the chart appVersion.
*/}}
{{- define "tso.imageTag" -}}
{{- .Values.image.tag | default .Chart.AppVersion -}}
{{- end -}}

{{/*
The Postgres Service hostname other pods connect to.
*/}}
{{- define "tso.postgresHost" -}}
{{- printf "%s-postgres" .Release.Name -}}
{{- end -}}

{{/*
JDBC URL for the shared application database.
*/}}
{{- define "tso.jdbcUrl" -}}
{{- $host := .Values.database.host | default (include "tso.postgresHost" .) -}}
{{- printf "jdbc:postgresql://%s:5432/%s" $host .Values.postgres.db -}}
{{- end -}}
