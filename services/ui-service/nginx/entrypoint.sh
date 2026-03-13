#!/bin/sh
set -eu

mkdir -p /tmp/client_temp /tmp/proxy_temp /tmp/fastcgi_temp /tmp/uwsgi_temp /tmp/scgi_temp /var/run/constructraos-ui-config

: "${UI_API_PROXY_BASE_URL:=}"
: "${UI_OTLP_PROXY_BASE_URL:=}"
: "${UI_PUBLIC_API_URL:=http://localhost:18080/api/healthz}"
: "${UI_PUBLIC_ORCHESTRATION_URL:=http://localhost:18081/healthz}"
: "${UI_PUBLIC_POLICY_URL:=http://localhost:18082/healthz}"
: "${UI_PUBLIC_OPA_URL:=http://localhost:18181}"
: "${UI_PUBLIC_TEMPORAL_UI_URL:=http://localhost:18233}"
: "${UI_PUBLIC_JAEGER_URL:=http://localhost:18686}"

ui_root_target="/usr/share/nginx/html-baked"
if [ -f /var/run/constructraos-ui-overlay/index.html ]; then
  ui_root_target="/var/run/constructraos-ui-overlay"
fi
ln -sfn "${ui_root_target}" /var/run/constructraos-ui-config/web-root

if [ -n "${UI_API_PROXY_BASE_URL}" ]; then
  envsubst '${UI_API_PROXY_BASE_URL} ${UI_OTLP_PROXY_BASE_URL}' \
    < /etc/constructraos-ui/local-proxy.conf.template \
    > /var/run/constructraos-ui-config/local-proxy.conf
else
  : > /var/run/constructraos-ui-config/local-proxy.conf
fi

envsubst '${UI_PUBLIC_API_URL} ${UI_PUBLIC_ORCHESTRATION_URL} ${UI_PUBLIC_POLICY_URL} ${UI_PUBLIC_OPA_URL} ${UI_PUBLIC_TEMPORAL_UI_URL} ${UI_PUBLIC_JAEGER_URL}' \
  < /etc/constructraos-ui/runtime-config.json.template \
  > /var/run/constructraos-ui-config/config.json

exec nginx -g 'daemon off;' -c /etc/nginx/nginx.conf
