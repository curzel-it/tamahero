#!/usr/bin/env bash
set -euo pipefail

# Demo mode: wipe DB, launch desktop app, wait for login, auto-play via CLI
# Requires .env with SSH_USER, HOST, SSH_PASSWORD

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Load .env (trim whitespace around values)
if [ -f .env ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        line="${line%%#*}"
        [[ -z "$line" ]] && continue
        key="${line%%=*}"
        val="${line#*=}"
        key="$(echo "$key" | xargs)"
        val="$(echo "$val" | xargs)"
        export "$key=$val"
    done < .env
else
    echo "Error: .env file not found"
    exit 1
fi

if [ -z "${SSH_USER:-}" ] || [ -z "${HOST:-}" ] || [ -z "${SSH_PASSWORD:-}" ]; then
    echo "Error: SSH_USER, HOST, and SSH_PASSWORD must be set in .env"
    exit 1
fi

SSH_HOST="${SSH_USER}@${HOST}"
LOG_FILE="/tmp/tamahero-demo.log"
TOKEN_FILE="$HOME/.tamahero/token.json"
BASE_URL="https://tama.curzel.it"

cleanup() {
    echo ""
    echo "Shutting down..."
    if [ -n "${DESKTOP_PID:-}" ]; then
        kill "$DESKTOP_PID" 2>/dev/null || true
    fi
    rm -f "$LOG_FILE"
}
trap cleanup EXIT

# Step 0: Kill any running desktop app and clear cached credentials
echo "=== Step 0: Cleaning up previous runs ==="
pkill -f "composeApp" 2>/dev/null || true
pkill -f "it.curzel.tamahero" 2>/dev/null || true
sleep 1
# Clear Java Preferences (desktop app token cache)
defaults delete com.apple.java.util.prefs 2>/dev/null || true
rm -f ~/Library/Preferences/com.apple.java.util.prefs.plist 2>/dev/null || true
rm -f "$HOME/.tamahero/token.json" 2>/dev/null || true
echo "Cleaned up."

# Step 1: Wipe the database
echo ""
echo "=== Step 1: Wiping database on $HOST ==="
sshpass -p "$SSH_PASSWORD" ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$SSH_HOST" \
    "systemctl stop tamahero-server && rm -f /opt/tamahero-server/tamahero.db && rm -rf /opt/tamahero-server/data && systemctl start tamahero-server && echo clean-ok"
echo "Database wiped, server restarted."
sleep 3

# Step 2: Launch desktop app
echo ""
echo "=== Step 2: Launching desktop app ==="
> "$LOG_FILE"
./gradlew :composeApp:run 2>&1 | tee "$LOG_FILE" &
DESKTOP_PID=$!
echo "Desktop app launching (PID: $DESKTOP_PID)..."

# Step 3: Wait for login token in logs
echo ""
echo "=== Step 3: Waiting for you to login in the desktop app ==="
echo "(Register a new account or login — the token will be captured from logs)"
echo ""

TOKEN=""
while [ -z "$TOKEN" ]; do
    sleep 2
    # Look for: [AUTH] Token for username (id=123): abcdef...
    LINE=$(grep -m1 '\[AUTH\] Token for' "$LOG_FILE" 2>/dev/null || true)
    if [ -n "$LINE" ]; then
        USERNAME=$(echo "$LINE" | sed -E 's/.*Token for ([^ ]+) .*/\1/')
        USER_ID=$(echo "$LINE" | sed -E 's/.*\(id=([0-9]+)\).*/\1/')
        TOKEN=$(echo "$LINE" | sed -E 's/.*: ([a-zA-Z0-9]+)$/\1/')
        echo "Captured token for $USERNAME (id=$USER_ID)"
    fi
done

# Step 4: Write token file for CLI
echo ""
echo "=== Step 4: Writing CLI token file ==="
mkdir -p "$HOME/.tamahero"
cat > "$TOKEN_FILE" <<EOF
{"baseUrl":"$BASE_URL","token":"$TOKEN","username":"$USERNAME","userId":$USER_ID}
EOF
echo "Token saved to $TOKEN_FILE"

# Step 5: Launch CLI in auto-play mode
echo ""
echo "=== Step 5: Starting CLI auto-play ==="
echo "(Press Ctrl+C to stop)"
echo ""
./gradlew :cli:run --console=plain --args="$BASE_URL --autoplay"
