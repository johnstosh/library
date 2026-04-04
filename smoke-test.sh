#!/bin/bash
# (c) Copyright 2025 by Muczynski
# Smoke test for the native binary running on localhost:8080.
# Requires: curl, sha256sum
# Usage: ./smoke-test.sh

set -e
BASE=http://localhost:8080
PASS=0
FAIL=0

ok()   { echo "  PASS: $1"; ((PASS++)); }
fail() { echo "  FAIL: $1"; ((FAIL++)); }

# Helper: sha256 of a password (matches frontend hashing)
sha256() { echo -n "$1" | sha256sum | awk '{print $1}'; }

echo "=== Native binary smoke test against $BASE ==="

# 1. Health / static frontend
echo ""
echo "--- 1. Frontend served ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/")
[ "$STATUS" = "200" ] && ok "GET / → $STATUS" || fail "GET / → $STATUS (expected 200)"

# 2. Login
echo ""
echo "--- 2. Login ---"
COOKIE_JAR=$(mktemp)
HASHED=$(sha256 "divinemercy")
LOGIN_RESP=$(curl -s -o /dev/null -w "%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"librarian\",\"password\":\"$HASHED\"}")
[ "$LOGIN_RESP" = "200" ] && ok "POST /api/auth/login → $LOGIN_RESP" || fail "POST /api/auth/login → $LOGIN_RESP (expected 200)"

# 3. Authenticated session: /api/auth/me
echo ""
echo "--- 3. Session check ---"
ME_RESP=$(curl -s -w "\n%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE/api/auth/me")
ME_STATUS=$(echo "$ME_RESP" | tail -1)
ME_BODY=$(echo "$ME_RESP" | head -1)
[ "$ME_STATUS" = "200" ] && ok "GET /api/auth/me → $ME_STATUS" || fail "GET /api/auth/me → $ME_STATUS (expected 200)"
echo "$ME_BODY" | grep -q '"username"' && ok "/api/auth/me body contains username" || fail "/api/auth/me body missing username: $ME_BODY"

# 4. Books list
echo ""
echo "--- 4. Books list ---"
BOOKS_RESP=$(curl -s -o /dev/null -w "%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  "$BASE/api/books?page=0&size=5")
[ "$BOOKS_RESP" = "200" ] && ok "GET /api/books → $BOOKS_RESP" || fail "GET /api/books → $BOOKS_RESP (expected 200)"

# 5. Authors list
echo ""
echo "--- 5. Authors list ---"
AUTHORS_RESP=$(curl -s -o /dev/null -w "%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  "$BASE/api/authors?page=0&size=5")
[ "$AUTHORS_RESP" = "200" ] && ok "GET /api/authors → $AUTHORS_RESP" || fail "GET /api/authors → $AUTHORS_RESP (expected 200)"

# 6. Library card PDF — uses iText 8 renderer
echo ""
echo "--- 6. Library card PDF (iText 8) ---"
PDF_RESP=$(curl -s -o /dev/null -w "%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  "$BASE/api/library-cards/pdf")
[ "$PDF_RESP" = "200" ] && ok "GET /api/library-cards/pdf → $PDF_RESP" || fail "GET /api/library-cards/pdf → $PDF_RESP (expected 200)"

# 7. Branches (libraries) list
echo ""
echo "--- 7. Branches list ---"
LIBS_RESP=$(curl -s -o /dev/null -w "%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  "$BASE/api/libraries")
[ "$LIBS_RESP" = "200" ] && ok "GET /api/libraries → $LIBS_RESP" || fail "GET /api/libraries → $LIBS_RESP (expected 200)"

# 8. Users list (librarian only)
echo ""
echo "--- 8. Users list (librarian) ---"
USERS_RESP=$(curl -s -o /dev/null -w "%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  "$BASE/api/users")
[ "$USERS_RESP" = "200" ] && ok "GET /api/users → $USERS_RESP" || fail "GET /api/users → $USERS_RESP (expected 200)"

# 9. Logout
echo ""
echo "--- 9. Logout ---"
LOGOUT_RESP=$(curl -s -o /dev/null -w "%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -X POST "$BASE/api/auth/logout")
[ "$LOGOUT_RESP" = "200" ] && ok "POST /api/auth/logout → $LOGOUT_RESP" || fail "POST /api/auth/logout → $LOGOUT_RESP (expected 200)"

# 10. Unauthenticated access should be rejected after logout
echo ""
echo "--- 10. Auth enforcement after logout ---"
ME_AFTER=$(curl -s -o /dev/null -w "%{http_code}" -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE/api/auth/me")
[ "$ME_AFTER" = "401" ] || [ "$ME_AFTER" = "403" ] && ok "GET /api/auth/me after logout → $ME_AFTER (rejected)" || fail "GET /api/auth/me after logout → $ME_AFTER (expected 401/403)"

rm -f "$COOKIE_JAR"
echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
[ "$FAIL" -eq 0 ] && exit 0 || exit 1
