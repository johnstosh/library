# Standing orders

- Don't git commit nor git push to `main` without explicit permission.
- Push to `dev` after each change looks complete.
- When shutting down the library docker-test stack, stop only the `application` container. Leave `local-db` running so the next start reuses the same database.
- You are running in Google Cloud Shell. You can `cat /etc/os*` (this host is Ubuntu 24.04).
