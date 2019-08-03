# Proxied WebUntis

To enhance your privacy you can proxy webuntis.com through a server. This will reduce the leakage of your IP and useragent to webuntis.com


### nginx

Please note that the security (SSL) headers may need to be changed depending on your OS and nginx version.  
You can also completely remove them, they are optional and do not change functionality in general.

Change `demo` to the server you want to proxy.

```
server {
  listen <IPv4>:80;
  listen <IPv6>:80;
  server_name demo.openuntis.example.com;

  location /.well-known/acme-challenge/ { allow all; }
  location / { return 301 https://$host$request_uri; }

  access_log /dev/null;
  error_log /dev/null;
}

server {
  listen <IPv4>:443 ssl http2;
  listen <IPv6>:443 ssl http2;

  server_name demo.openuntis.example.com;

  access_log /dev/null;
  error_log /dev/null;

  # optional security headers (may need to be changed!)
  #ssl_protocols TLSv1.2;
  #ssl_prefer_server_ciphers on;
  #ssl_dhparam /etc/nginx/dhparam.pem;
  #ssl_ciphers EECDH+AESGCM:EDH+AESGCM;
  #ssl_ecdh_curve secp384r1;
  #ssl_session_timeout  10m;
  #ssl_session_cache shared:SSL:10m;
  #ssl_session_tickets off;
  #add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";
  #add_header X-Frame-Options DENY;
  #add_header X-Content-Type-Options nosniff;
  #add_header X-XSS-Protection "1; mode=block";
  #add_header Referrer-Policy "no-referrer";
  #add_header Expect-CT "enforce, max-age=21600";

  # you can also use a wildcard certificate here if you want to proxy multiple webuntis server
  ssl_certificate     /etc/letsencrypt/live/demo.openuntis.example.com/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/demo.openuntis.example.com/privkey.pem;
  ssl_trusted_certificate /etc/letsencrypt/live/demo.openuntis.example.com/chain.pem;

client_max_body_size 0;
location / {
           proxy_pass https://demo.webuntis.com;
           proxy_redirect https://demo.webuntis.com/ /;
           proxy_set_header Host demo.webuntis.com;
           # we do not want to leak any IP, if you want uncomment below
           # proxy_set_header X-Real-IP $remote_addr;
           # proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           # proxy_set_header X-Forwarded-Host $server_name;
           sub_filter_types text/html, application/json;
           sub_filter_once off;
           sub_filter "demo.webuntis.com" "demo.openuntis.example.com";
   }
}
```
