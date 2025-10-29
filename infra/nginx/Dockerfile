FROM nginx:latest

COPY nginx-http-only.conf /etc/nginx/nginx.conf

# 임시 index.html
RUN echo '<!DOCTYPE html><html><head><title>Topoom</title></head><body><h1>🚀 Topoom - Coming Soon</h1></body></html>' > /usr/share/nginx/html/index.html

# COPY frontend/ /usr/share/nginx/html/