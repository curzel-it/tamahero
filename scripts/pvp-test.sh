#!/bin/bash
# Launch two desktop clients for PvP testing.
#
# Usage:
#   ./scripts/pvp-test.sh [server_url]
#
# Default server: http://localhost:8081
#
# This script:
#   1. Registers two test accounts (pvp_player1 / pvp_player2) if they don't exist
#   2. Launches two desktop windows, each logged into a different account
#
# Prerequisites:
#   - Server must be running (./gradlew :server:run)

set -e

SERVER="${1:-http://localhost:8081}"

echo "=== PvP Test Setup ==="
echo "Server: $SERVER"
echo ""

# Register test accounts (ignore errors if they already exist)
echo "Registering test accounts..."
curl -s -X POST "$SERVER/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"pvp_player1","password":"testpass123"}' 2>/dev/null | python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    print(f'  pvp_player1: {d.get(\"error\", \"registered\")}')
except: print('  pvp_player1: done')
" 2>/dev/null

curl -s -X POST "$SERVER/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"pvp_player2","password":"testpass123"}' 2>/dev/null | python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    print(f'  pvp_player2: {d.get(\"error\", \"registered\")}')
except: print('  pvp_player2: done')
" 2>/dev/null

echo ""
echo "Launching Player 1 (pvp_player1)..."
./gradlew :composeApp:run -PappArgs="--user pvp_player1 --pass testpass123 --server $SERVER" &
PID1=$!

sleep 3

echo "Launching Player 2 (pvp_player2)..."
./gradlew :composeApp:run -PappArgs="--user pvp_player2 --pass testpass123 --server $SERVER" &
PID2=$!

echo ""
echo "Both clients launched."
echo "  Player 1 PID: $PID1"
echo "  Player 2 PID: $PID2"
echo ""
echo "Press Ctrl+C to stop both."

trap "kill $PID1 $PID2 2>/dev/null" EXIT
wait $PID1 $PID2
