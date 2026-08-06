#!/usr/bin/env bash
#
# Quick reachability check for all 7 TechShop Armenia services.
#
# None of the services expose Spring Boot Actuator, so this doesn't check
# for a "healthy" 200 response - it just checks that each service accepts a
# connection and returns *some* HTTP response within a few seconds. A 401/403
# from an endpoint that requires auth still counts as "up" (the JVM is
# running and Spring's DispatcherServlet is handling requests); only a
# connection failure or timeout counts as "down". That's the right signal
# for "did the container crash / is the port unreachable", which is what an
# outage actually looks like here.
#
# Usage:
#   ./scripts/health-check.sh                  # checks localhost
#   HOST=13.53.171.79 ./scripts/health-check.sh # checks a remote host
#
# Exit code is 0 if every service responded, 1 if any did not - so this can
# be wired into cron with a mailer, e.g.:
#   */5 * * * * /path/to/TechShopArmenia/scripts/health-check.sh >> /var/log/techshop-health.log 2>&1 || echo "TechShop outage detected" | mail -s "TechShop health check failed" you@example.com

set -uo pipefail

HOST="${HOST:-localhost}"
TIMEOUT_SECONDS=5

# name:port:path - path is whatever's cheapest to hit on each service, public
# where one exists, otherwise any endpoint (an auth rejection still proves
# the service is up).
SERVICES=(
  "techshop-user:8081:/api/users/count"
  "techshop-cart:8082:/api/cart/1"
  "techshop-order:8083:/api/orders"
  "techshop-product:8084:/api/categories"
  "techshop-notification:8085:/api/notifications/1"
  "techshop-wishlist:8086:/wishlist"
  "techshop-chat:8087:/api/chat/conversations"
)

failures=0

printf "%-22s %-6s %s\n" "SERVICE" "PORT" "STATUS"
printf "%-22s %-6s %s\n" "-------" "----" "------"

for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  rest="${entry#*:}"
  port="${rest%%:*}"
  path="${rest#*:}"

  http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT_SECONDS" "http://${HOST}:${port}${path}" || true)

  if [[ "$http_code" =~ ^[0-9]{3}$ && "$http_code" != "000" ]]; then
    printf "%-22s %-6s UP (HTTP %s)\n" "$name" "$port" "$http_code"
  else
    printf "%-22s %-6s DOWN (no response within %ss)\n" "$name" "$port" "$TIMEOUT_SECONDS"
    failures=$((failures + 1))
  fi
done

echo
if [[ "$failures" -eq 0 ]]; then
  echo "All 7 services responded."
  exit 0
else
  echo "$failures service(s) did not respond - see DOWN entries above."
  exit 1
fi
