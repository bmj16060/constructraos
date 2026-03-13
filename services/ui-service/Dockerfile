FROM nginx:1.27-alpine

RUN apk add --no-cache gettext \
    && rm /etc/nginx/conf.d/default.conf \
    && mkdir -p /etc/constructraos-ui /var/run/constructraos-ui-config /tmp/client_temp /tmp/proxy_temp /tmp/fastcgi_temp /tmp/uwsgi_temp /tmp/scgi_temp \
    && chown -R nginx:nginx /usr/share/nginx/html /etc/nginx /etc/constructraos-ui /var/cache/nginx /var/run/constructraos-ui-config /tmp

COPY build/frontend-static/ /usr/share/nginx/html/
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY nginx/local-proxy.conf.template /etc/constructraos-ui/local-proxy.conf.template
COPY nginx/runtime-config.json.template /etc/constructraos-ui/runtime-config.json.template
COPY nginx/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

USER nginx

EXPOSE 8090

ENTRYPOINT ["/entrypoint.sh"]
