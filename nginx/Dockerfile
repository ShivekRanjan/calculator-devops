# Reverse proxy in front of the calculator replicas.
# The config is baked into the image (instead of a bind mount) so the stack
# also works when Jenkins itself runs inside a Docker container.
FROM nginx:1.30-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
