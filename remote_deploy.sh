#!/bin/bash
# Remote deploy script — runs on the server, survives SSH disconnects.
# Triggered by deploy.py via: nohup bash remote_deploy.sh &

STATUS_FILE="/tmp/tamahero-deploy-status"
LOG_FILE="/tmp/tamahero-deploy.log"

echo "running" > "$STATUS_FILE"

{
    echo "=== Starting remote deploy at $(date) ==="

    cd /root || { echo "failed" > "$STATUS_FILE"; exit 1; }

    # Pull deploy repo (artifacts)
    # Use fetch + reset --hard because deploy pushes are force-pushed (no history)
    echo "--- Pulling tamahero-deploy ---"
    if test -d tamahero-deploy; then
        cd tamahero-deploy && git fetch origin && git reset --hard origin/main
    else
        git clone git@github.com:curzel-it/tamahero-deploy.git
    fi

    if [ $? -ne 0 ]; then
        echo "FAILED: deploy repo pull"
        echo "failed" > "$STATUS_FILE"
        exit 1
    fi

    # Pull source repo
    echo "--- Pulling tamahero ---"
    cd /root/tamahero && git pull origin main

    if [ $? -ne 0 ]; then
        echo "FAILED: source repo pull"
        echo "failed" > "$STATUS_FILE"
        exit 1
    fi

    # Run install
    echo "--- Running install.py ---"
    python3 install.py

    if [ $? -ne 0 ]; then
        echo "FAILED: install.py"
        echo "failed" > "$STATUS_FILE"
        exit 1
    fi

    echo "=== Deploy completed successfully at $(date) ==="
    echo "success" > "$STATUS_FILE"

} > "$LOG_FILE" 2>&1
