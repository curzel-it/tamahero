#!/usr/bin/env python3
"""
Deployment script for TamaHero server.
Cross-platform: works on Windows (using paramiko) and macOS/Linux (using sshpass).

The server is built locally because the VPS doesn't have enough RAM.
Build artifacts are committed to a separate deploy repo (tamahero-deploy)
which the server pulls from.

Usage:
    python deploy.py                      # Uses default message "Deployment"
    python deploy.py -m "Your message"    # Uses custom commit message
"""

import subprocess
import sys
import os
import platform
import shutil
import json
import time
import argparse
import urllib.request
from pathlib import Path
from datetime import datetime, timezone

# Load environment variables from .env file
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    print("Warning: python-dotenv not installed. Install with: pip install python-dotenv")
    print("Falling back to system environment variables only")

IS_WINDOWS = platform.system() == "Windows"

DEPLOY_REPO = Path.home() / "dev" / "tamahero-deploy"
DOMAIN = "tama.curzel.it"


def run_command(cmd, description, cwd=None):
    """Run a command and handle errors."""
    print(f"\n{'='*60}")
    print(f"{description}")
    print(f"{'='*60}")
    print(f"Running: {cmd}")

    result = subprocess.run(cmd, shell=True, capture_output=True, text=True, cwd=cwd)

    if result.stdout:
        print(result.stdout)
    if result.stderr:
        print(result.stderr, file=sys.stderr)

    if result.returncode != 0:
        print(f"Error: {description} failed with exit code {result.returncode}")
        sys.exit(1)

    print(f"{description} completed successfully")
    return result


def get_current_branch():
    """Get the current git branch name."""
    result = subprocess.run(
        ["git", "rev-parse", "--abbrev-ref", "HEAD"],
        capture_output=True, text=True, check=True
    )
    return result.stdout.strip()


def ssh_exec(host, password, remote_cmd):
    """Run a short SSH command and return (exit_code, stdout)."""
    if IS_WINDOWS:
        return _ssh_exec_windows(host, password, remote_cmd)
    else:
        return _ssh_exec_unix(host, password, remote_cmd)


def _ssh_exec_windows(host, password, remote_cmd):
    """Run SSH command on Windows using paramiko."""
    try:
        import paramiko
    except ImportError:
        print("Error: paramiko not installed. Install with: pip install paramiko")
        sys.exit(1)

    username, hostname = host.split("@", 1)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(hostname, username=username, password=password, timeout=30)
        stdin, stdout, stderr = client.exec_command(remote_cmd)
        output = stdout.read().decode('utf-8', errors='replace')
        exit_code = stdout.channel.recv_exit_status()
        return exit_code, output.strip()
    except Exception as e:
        return -1, str(e)
    finally:
        client.close()


def _ssh_exec_unix(host, password, remote_cmd, timeout=60):
    """Run SSH command on macOS/Linux using sshpass."""
    escaped_cmd = remote_cmd.replace("'", "'\"'\"'")
    ssh_cmd = f"sshpass -p \"{password}\" ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 {host} '{escaped_cmd}'"
    result = subprocess.run(ssh_cmd, shell=True, capture_output=True, text=True, timeout=timeout)
    return result.returncode, result.stdout.strip()


def get_server_version(domain):
    """Fetch the current /version JSON from the server. Returns dict or None."""
    url = f"https://{domain}/version"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "deploy.py"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode())
    except Exception:
        return None


def build_server():
    """Build the server distribution locally."""
    print(f"\n{'='*60}")
    print("Building Server Distribution")
    print(f"{'='*60}")

    project_root = Path(__file__).parent.resolve()
    gradlew = project_root / ("gradlew.bat" if IS_WINDOWS else "gradlew")

    # Write build timestamp to version.txt resource BEFORE building
    build_time = datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')
    version_file = project_root / "server" / "src" / "main" / "resources" / "version.txt"
    version_file.write_text(build_time)
    print(f"Set build timestamp: {build_time}")

    run_command(
        f'"{gradlew}" :server:installDist --no-daemon',
        "Building server (this may take a few minutes)",
        cwd=str(project_root)
    )

    server_dist = project_root / "server" / "build" / "install" / "server"
    if not server_dist.exists():
        print(f"Error: Server build output not found at {server_dist}")
        sys.exit(1)

    print(f"Server built: {server_dist}")
    return server_dist


def copy_artifacts_to_deploy_repo(server_dist):
    """Copy built server artifacts into the deploy repo."""
    print(f"\n{'='*60}")
    print("Copying Artifacts to Deploy Repo")
    print(f"{'='*60}")

    deploy_server = DEPLOY_REPO / "server-dist"
    if deploy_server.exists():
        shutil.rmtree(deploy_server)
    shutil.copytree(server_dist, deploy_server)
    print(f"Copied server-dist to {deploy_server}")


def push_deploy_repo(message):
    """Commit and force-push deploy repo artifacts with a fresh root commit (no history)."""
    print(f"\n{'='*60}")
    print("Pushing Deploy Repo")
    print(f"{'='*60}")

    git_prefix = f'git -C "{DEPLOY_REPO}"'
    run_command(f'{git_prefix} add -A', "Staging deploy artifacts")

    # Write current index as a tree object
    tree_result = subprocess.run(
        f'{git_prefix} write-tree',
        shell=True, capture_output=True, text=True
    )
    if tree_result.returncode != 0:
        print(f"Error: write-tree failed: {tree_result.stderr}")
        sys.exit(1)
    tree_sha = tree_result.stdout.strip()

    # Create a root commit (no parent) — keeps the repo history tiny
    escaped_message = message.replace('"', '\\"')
    commit_result = subprocess.run(
        f'{git_prefix} commit-tree {tree_sha} -m "{escaped_message}"',
        shell=True, capture_output=True, text=True
    )
    if commit_result.returncode != 0:
        print(f"Error: commit-tree failed: {commit_result.stderr}")
        sys.exit(1)
    commit_sha = commit_result.stdout.strip()

    # Point the main branch at the new root commit
    run_command(
        f'{git_prefix} update-ref refs/heads/main {commit_sha}',
        "Resetting main to fresh root commit"
    )

    # Force push — overwrites all history on the remote
    run_command(
        f'{git_prefix} push origin main --force',
        "Force pushing deploy repo (history overwritten)"
    )


def trigger_remote_deploy(host, password):
    """Trigger the deploy script on the server detached, then poll for completion."""
    print(f"\n{'='*60}")
    print(f"Remote deploy on {host}")
    print(f"{'='*60}")

    old_version = get_server_version(DOMAIN)
    old_build_time = old_version.get("buildTime") if old_version else None
    if old_version:
        print(f"Current server version: buildTime={old_build_time}")
    else:
        print("Could not fetch current server version (server may be down)")

    # Pull the source repo first so remote_deploy.sh is up to date
    print("Pulling latest source on server...")
    exit_code, output = ssh_exec(
        host, password,
        "cd /root/tamahero && git pull origin main && echo ok"
    )
    if exit_code != 0 or "ok" not in output:
        print(f"Error: Failed to pull source repo: {output}")
        sys.exit(1)

    # Trigger the deploy script detached
    print("Triggering deploy (runs detached on server)...")
    trigger_cmd = (
        "rm -f /tmp/tamahero-deploy-status && "
        "setsid bash /root/tamahero/remote_deploy.sh "
        "</dev/null >/dev/null 2>&1 & echo triggered"
    )
    try:
        exit_code, output = ssh_exec(host, password, trigger_cmd)
    except subprocess.TimeoutExpired:
        print("  SSH timed out, but deploy was likely triggered. Proceeding to poll...")
        exit_code, output = 0, "triggered"

    if exit_code != 0 or "triggered" not in output:
        print(f"Error: Failed to trigger deploy: {output}")
        sys.exit(1)

    # Poll for completion via /version endpoint
    print("Deploy triggered, polling for completion...")
    max_wait = 300  # 5 minutes
    poll_interval = 10
    elapsed = 0

    while elapsed < max_wait:
        time.sleep(poll_interval)
        elapsed += poll_interval

        new_version = get_server_version(DOMAIN)
        if new_version:
            new_build_time = new_version.get("buildTime")
            if old_build_time and new_build_time and new_build_time != old_build_time:
                print(f"  [{elapsed}s] Deploy completed successfully!")
                print(f"  New version: buildTime={new_build_time}")
                return
            if not old_build_time and new_build_time and new_build_time != "unknown":
                print(f"  [{elapsed}s] Deploy completed successfully!")
                print(f"  New version: buildTime={new_build_time}")
                return
            print(f"  [{elapsed}s] Server is up, waiting for new version... (buildTime={new_build_time})")
        else:
            print(f"  [{elapsed}s] Server not responding (restarting?)...")

    print(f"\nTimed out after {max_wait}s. The deploy may still be running on the server.")
    print(f"Check manually: curl https://{DOMAIN}/version")
    sys.exit(1)


def clean_database(host, password):
    """Stop the server, delete the database, and restart."""
    print(f"\n{'='*60}")
    print("Cleaning database (fresh start)")
    print(f"{'='*60}")

    cmd = (
        "systemctl stop tamahero-server && "
        "rm -f /opt/tamahero-server/tamahero.db && "
        "rm -rf /opt/tamahero-server/data && "
        "systemctl start tamahero-server && "
        "echo clean-ok"
    )
    exit_code, output = ssh_exec(host, password, cmd)
    if exit_code != 0 or "clean-ok" not in output:
        print(f"Error: Failed to clean database: {output}")
        sys.exit(1)

    print("Database deleted, server restarted with clean state")


def main():
    parser = argparse.ArgumentParser(description="Deploy TamaHero server via SSH")
    parser.add_argument(
        "-m", "--message",
        type=str,
        default="Deployment",
        help="Commit message (default: 'Deployment')"
    )
    parser.add_argument(
        "--clean-db",
        action="store_true",
        default=False,
        help="Drop the database on the server after deploying (fresh start)"
    )
    args = parser.parse_args()

    username = os.getenv("SSH_USER")
    host = os.getenv("HOST")
    ssh_password = os.getenv("SSH_PASSWORD")
    skip_push = os.getenv("DEPLOY_SKIP_PUSH", "false").lower() == "true"
    branch_override = os.getenv("DEPLOY_BRANCH")

    if not username or not host:
        print("Error: SSH_USER and HOST not set in .env file")
        sys.exit(1)

    ssh_host = f"{username}@{host}"

    if not ssh_password:
        print("Error: SSH_PASSWORD not set in .env file")
        sys.exit(1)

    branch = branch_override if branch_override else get_current_branch()
    print(f"Deploying branch: {branch}")
    print(f"Target host: {ssh_host}")

    # Verify deploy repo exists
    if not (DEPLOY_REPO / ".git").exists():
        print(f"Error: Deploy repo not found at {DEPLOY_REPO}")
        print("Clone it first: git clone git@github_curzel:curzel-it/tamahero-deploy.git ~/dev/tamahero-deploy")
        sys.exit(1)

    repo_root = Path(__file__).parent.resolve()
    while not (repo_root / ".git").exists() and repo_root != repo_root.parent:
        repo_root = repo_root.parent

    # Step 1: Push source changes
    if not skip_push:
        git_prefix = f'git -C "{repo_root}"'
        run_command(f'{git_prefix} add -A', "Adding all changes to index")

        result = subprocess.run(
            f'{git_prefix} diff --cached --quiet',
            shell=True, capture_output=True
        )
        if result.returncode != 0:
            escaped_message = args.message.replace('"', '\\"')
            run_command(
                f'{git_prefix} commit -m "{escaped_message}"',
                "Committing changes"
            )
            run_command(
                f'{git_prefix} push origin {branch}',
                f"Pushing to origin/{branch}"
            )
        else:
            print("\nNo source changes to commit, skipping push")
    else:
        print("\nSkipping git push (DEPLOY_SKIP_PUSH set)")

    # Step 2: Build server locally
    server_dist = build_server()

    # Step 3: Copy artifacts to deploy repo
    copy_artifacts_to_deploy_repo(server_dist)

    # Step 4: Commit and push deploy repo
    if not skip_push:
        push_deploy_repo(args.message)
    else:
        print("\nSkipping deploy repo push (DEPLOY_SKIP_PUSH set)")

    # Step 5: SSH to server — trigger deploy detached
    trigger_remote_deploy(ssh_host, ssh_password)

    # Step 6: Optionally drop the database for a clean start
    if args.clean_db:
        clean_database(ssh_host, ssh_password)

    print(f"\n{'='*60}")
    print("Deployment completed successfully!")
    print(f"{'='*60}")


if __name__ == "__main__":
    main()
