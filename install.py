#!/usr/bin/env python3
"""
Production Installation & Update Script for TamaHero Server
Target: Ubuntu 24.04 VPS (shared with galdria.com)

PREREQUISITES - Run these commands BEFORE first installation:
1. Update system packages:
   sudo apt update && sudo apt upgrade -y

2. Install git:
   sudo apt install -y git

3. Clone both repositories:
   git clone <your-repo-url> /root/tamahero
   git clone git@github_curzel:curzel-it/tamahero-deploy.git /root/tamahero-deploy

The script will automatically install: Java (OpenJDK 21), Nginx, Certbot

WHAT THIS SCRIPT DOES:
- Installs the pre-built server distribution (from tamahero-deploy repo)
- Creates systemd service (if first install)
- Sets up log rotation (if first install)
- Configures Nginx server block for tama.curzel.it
- Obtains Let's Encrypt SSL certificate (if first install)
- Restarts the service

USAGE:
  cd /root/tamahero
  sudo python3 install.py
"""

import os
import sys
import subprocess
import shutil
from pathlib import Path
from datetime import datetime, timezone
import time

# Configuration
SERVICE_NAME = "tamahero-server"
SERVICE_USER = os.environ.get("SUDO_USER", os.environ.get("USER", "root"))
PROJECT_ROOT = Path("/root/tamahero")
DEPLOY_REPO = Path("/root/tamahero-deploy")
SERVER_DIST = DEPLOY_REPO / "server-dist"
INSTALL_DIR = Path(f"/opt/{SERVICE_NAME}")
SYSTEMD_SERVICE_PATH = Path(f"/etc/systemd/system/{SERVICE_NAME}.service")
LOG_DIR = Path(f"/var/log/{SERVICE_NAME}")
ENV_FILE = Path(f"/etc/{SERVICE_NAME}/env")
DOMAIN = "tama.curzel.it"
SERVER_PORT = 8081
CERT_DIR = Path("/etc/letsencrypt")


def run_command(cmd, check=True, shell=False, cwd=None, env=None):
    """Execute a shell command and return the result."""
    print(f">>> Running: {cmd if isinstance(cmd, str) else ' '.join(cmd)}")
    try:
        result = subprocess.run(
            cmd, check=check, shell=shell, cwd=cwd,
            capture_output=True, text=True, env=env
        )
        if result.stdout:
            print(result.stdout)
        return result
    except subprocess.CalledProcessError as e:
        print(f"Command failed with exit code {e.returncode}")
        if e.stderr:
            print(f"Error: {e.stderr}")
        if check:
            sys.exit(1)
        return e


def check_root():
    """Ensure script is run with sudo privileges."""
    if os.geteuid() != 0:
        print("This script must be run with sudo privileges")
        print("  Usage: sudo python3 install.py")
        sys.exit(1)
    print("Running with sudo privileges")


def install_java():
    """Install Java if not already installed."""
    print("\n=== Checking Java Installation ===")
    result = run_command(["which", "java"], check=False)
    if result.returncode == 0:
        run_command(["java", "-version"], check=False)
        print("Java already installed")
        return

    print("Java not found. Installing OpenJDK 21...")
    run_command(["apt", "update"])
    run_command(["apt", "install", "-y", "openjdk-21-jdk"])
    result = run_command(["java", "-version"], check=False)
    if result.returncode != 0:
        print("Failed to install Java")
        sys.exit(1)
    print("Java installed successfully")


def validate_server_dist():
    """Validate that the pre-built server distribution exists."""
    print("\n=== Validating Server Distribution ===")
    if not SERVER_DIST.exists():
        print(f"Error: Pre-built server distribution not found at {SERVER_DIST}")
        sys.exit(1)

    bin_script = SERVER_DIST / "bin" / "server"
    if not bin_script.exists():
        print(f"Error: Server startup script not found at {bin_script}")
        sys.exit(1)

    print(f"Server distribution validated: {SERVER_DIST}")


def install_distribution():
    """Copy the distribution to the installation directory."""
    print("\n=== Installing Server Distribution ===")

    run_command(["systemctl", "stop", SERVICE_NAME], check=False)

    # Preserve the data directory (contains SQLite database) across deploys
    data_dir = INSTALL_DIR / "data"
    tmp_data_backup = Path("/tmp/tamahero-data-backup")
    preserved_data = False
    if data_dir.exists():
        if tmp_data_backup.exists():
            shutil.rmtree(tmp_data_backup)
        shutil.copytree(data_dir, tmp_data_backup)
        preserved_data = True
        print(f"Backed up data directory: {data_dir}")

    if INSTALL_DIR.exists():
        shutil.rmtree(INSTALL_DIR)
        print(f"Removed old installation: {INSTALL_DIR}")

    shutil.copytree(SERVER_DIST, INSTALL_DIR)

    if preserved_data:
        if (INSTALL_DIR / "data").exists():
            shutil.rmtree(INSTALL_DIR / "data")
        shutil.copytree(tmp_data_backup, INSTALL_DIR / "data")
        shutil.rmtree(tmp_data_backup)
        print(f"Restored data directory: {data_dir}")

    bin_dir = INSTALL_DIR / "bin"
    for script in bin_dir.iterdir():
        os.chmod(script, 0o755)

    print(f"Distribution installed: {INSTALL_DIR}")


def create_directories():
    """Create necessary directories for logs."""
    print("\n=== Creating Directories ===")
    directories = [
        (LOG_DIR, "logs"),
        (ENV_FILE.parent, "environment configuration"),
    ]
    for dir_path, description in directories:
        dir_path.mkdir(parents=True, exist_ok=True)
        print(f"Created {dir_path} ({description})")


def create_env_file():
    """Create or update environment file."""
    print("\n=== Creating Environment File ===")
    if not ENV_FILE.exists():
        env_content = f"""# TamaHero Server Production Environment Configuration
SERVER_PORT={SERVER_PORT}
JAVA_OPTS=-Xms256m -Xmx512m
LOG_LEVEL=INFO
"""
        ENV_FILE.write_text(env_content)
        os.chmod(ENV_FILE, 0o640)
        print(f"Environment file created: {ENV_FILE}")
    else:
        print(f"Environment file exists (preserved): {ENV_FILE}")


def create_systemd_service():
    """Create systemd service file for the server (first install only)."""
    print("\n=== Checking Systemd Service ===")
    if SYSTEMD_SERVICE_PATH.exists():
        print(f"Service file already exists: {SYSTEMD_SERVICE_PATH}")
        return

    service_content = f"""[Unit]
Description=TamaHero Game Server
After=network.target

[Service]
Type=simple
User={SERVICE_USER}
Group={SERVICE_USER}
WorkingDirectory={INSTALL_DIR}
EnvironmentFile={ENV_FILE}
ExecStart={INSTALL_DIR}/bin/server
Restart=always
RestartSec=10
StartLimitInterval=0
NoNewPrivileges=true
PrivateTmp=true
StandardOutput=append:{LOG_DIR}/server.log
StandardError=append:{LOG_DIR}/error.log
SyslogIdentifier={SERVICE_NAME}
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
"""
    SYSTEMD_SERVICE_PATH.write_text(service_content)
    os.chmod(SYSTEMD_SERVICE_PATH, 0o644)
    print(f"Systemd service created: {SYSTEMD_SERVICE_PATH}")


def setup_logrotate():
    """Configure log rotation (first install only)."""
    print("\n=== Checking Log Rotation ===")
    logrotate_config = Path(f"/etc/logrotate.d/{SERVICE_NAME}")
    if logrotate_config.exists():
        print(f"Logrotate already configured: {logrotate_config}")
        return

    logrotate_content = f"""{LOG_DIR}/*.log {{
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0640 {SERVICE_USER} {SERVICE_USER}
    sharedscripts
    postrotate
        systemctl reload {SERVICE_NAME} >/dev/null 2>&1 || true
    endscript
}}
"""
    logrotate_config.write_text(logrotate_content)
    os.chmod(logrotate_config, 0o644)
    print(f"Log rotation configured: {logrotate_config}")


def configure_firewall():
    """Configure firewall to allow HTTP/HTTPS traffic (first install only)."""
    print("\n=== Checking Firewall ===")
    result = run_command(["which", "ufw"], check=False)
    if result.returncode != 0:
        print("ufw not found, skipping firewall configuration")
        return

    status = run_command(["ufw", "status"], check=False)
    if status.returncode == 0 and "Status: active" in (status.stdout or ""):
        print("Firewall already active")
        return

    run_command(["ufw", "allow", "22/tcp"])
    run_command(["ufw", "allow", "80/tcp"])
    run_command(["ufw", "allow", "443/tcp"])
    run_command(["ufw", "--force", "enable"])
    print("Firewall enabled")


def install_nginx():
    """Install Nginx if not already installed."""
    print("\n=== Checking Nginx Installation ===")
    result = run_command(["which", "nginx"], check=False)
    if result.returncode == 0:
        print("Nginx already installed")
        return

    print("Nginx not found. Installing...")
    run_command(["apt", "update"])
    run_command(["apt", "install", "-y", "nginx"])
    run_command(["systemctl", "enable", "nginx"])
    run_command(["systemctl", "start", "nginx"])
    print("Nginx installed and started")


def setup_nginx(force=False):
    """Configure Nginx with a dedicated server block for tama.curzel.it."""
    print("\n=== Checking Nginx Configuration ===")

    site_available = Path(f"/etc/nginx/sites-available/{DOMAIN}")
    site_enabled = Path(f"/etc/nginx/sites-enabled/{DOMAIN}")

    if site_available.exists() and not force:
        print(f"Nginx already configured: {site_available}")
        return

    cert_path = CERT_DIR / "live" / DOMAIN / "fullchain.pem"
    has_ssl = cert_path.exists()

    if has_ssl:
        config = f"""# {DOMAIN} - TamaHero Server
# Generated by tamahero install.py

server {{
    listen 80;
    listen [::]:80;
    server_name {DOMAIN};

    location / {{
        return 301 https://$server_name$request_uri;
    }}

    location /.well-known/acme-challenge/ {{
        root /var/www/html;
    }}
}}

server {{
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name {DOMAIN};

    ssl_certificate /etc/letsencrypt/live/{DOMAIN}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/{DOMAIN}/privkey.pem;
    ssl_session_timeout 1d;
    ssl_session_cache shared:TamaSSL:10m;
    ssl_session_tickets off;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;

    add_header Strict-Transport-Security "max-age=63072000" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    location / {{
        proxy_pass http://127.0.0.1:{SERVER_PORT};
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }}
}}
"""
    else:
        config = f"""# {DOMAIN} - TamaHero Server (Initial, no SSL)
# Generated by tamahero install.py

server {{
    listen 80;
    listen [::]:80;
    server_name {DOMAIN};

    location / {{
        proxy_pass http://127.0.0.1:{SERVER_PORT};
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }}

    location /.well-known/acme-challenge/ {{
        root /var/www/html;
    }}
}}
"""

    site_available.write_text(config)
    print(f"Nginx config written: {site_available}")

    if site_enabled.exists() or site_enabled.is_symlink():
        site_enabled.unlink()
    site_enabled.symlink_to(site_available)

    # Remove default site if exists
    default_site = Path("/etc/nginx/sites-enabled/default")
    if default_site.exists() or default_site.is_symlink():
        default_site.unlink()

    run_command(["nginx", "-t"])
    run_command(["systemctl", "reload", "nginx"])
    print("Nginx reloaded")


def install_certbot():
    """Install Certbot if not already installed."""
    print("\n=== Installing Certbot ===")
    result = run_command(["which", "certbot"], check=False)
    if result.returncode == 0:
        print("Certbot already installed")
        return

    print("Installing Certbot...")
    run_command(["apt", "update"])
    run_command(["apt", "install", "-y", "certbot", "python3-certbot-nginx"])
    print("Certbot installed")


def obtain_ssl_certificate():
    """Obtain SSL certificate from Let's Encrypt."""
    print("\n=== Obtaining SSL Certificate ===")
    cert_path = CERT_DIR / "live" / DOMAIN / "fullchain.pem"
    if cert_path.exists():
        print(f"Certificate already exists for {DOMAIN}")
        return True

    print(f"Obtaining certificate for {DOMAIN}...")
    result = run_command([
        "certbot", "--nginx",
        "--non-interactive",
        "--agree-tos",
        "--email", "admin@curzel.it",
        "--domains", DOMAIN
    ], check=False)

    if result.returncode == 0:
        print(f"SSL certificate obtained for {DOMAIN}")
        return True
    else:
        print("Failed to obtain SSL certificate")
        print(f"  You can manually run: sudo certbot --nginx -d {DOMAIN}")
        return False


def renew_cert_if_needed():
    """Renew SSL certificate if it expires within 30 days."""
    print("\n=== Checking SSL Certificate Expiry ===")
    cert_path = CERT_DIR / "live" / DOMAIN / "fullchain.pem"
    if not cert_path.exists():
        print("No certificate found, skipping renewal check")
        return

    result = run_command(
        ["openssl", "x509", "-enddate", "-noout", "-in", str(cert_path)],
        check=False
    )
    if result.returncode != 0:
        print("Could not read certificate expiry, skipping")
        return

    end_str = result.stdout.strip().split("=", 1)[1]
    end_date = datetime.strptime(end_str, "%b %d %H:%M:%S %Y %Z").replace(tzinfo=timezone.utc)
    days_left = (end_date - datetime.now(timezone.utc)).days
    print(f"SSL certificate expires in {days_left} days")

    if days_left < 30:
        print("Certificate expiring soon, renewing...")
        run_command(["certbot", "renew", "--non-interactive"], check=False)
        run_command(["systemctl", "reload", "nginx"], check=False)
    else:
        print("Certificate OK, no renewal needed")


def setup_certbot_renewal():
    """Enable certbot auto-renewal timer (first install only)."""
    print("\n=== Checking Certbot Renewal Timer ===")
    result = run_command(["systemctl", "is-enabled", "certbot.timer"], check=False)
    if result.returncode == 0:
        print("Certbot renewal timer already enabled")
        return
    run_command(["systemctl", "enable", "certbot.timer"], check=False)
    run_command(["systemctl", "start", "certbot.timer"], check=False)
    print("Certbot renewal timer enabled")


def setup_database_backup_cron():
    """Set up daily database backup cron job (first install only)."""
    print("\n=== Checking Database Backup Cron ===")
    backup_script = Path("/usr/local/bin/tamahero-backup-db.sh")
    backup_dir = INSTALL_DIR / "data" / "backups"

    if backup_script.exists():
        print(f"Backup cron already configured: {backup_script}")
        return

    backup_dir.mkdir(parents=True, exist_ok=True)

    script_content = f"""#!/bin/bash
# TamaHero daily database backup
# Keeps the last 14 daily backups

DB_FILE="/opt/tamahero-server/data/tamahero.db"
BACKUP_DIR="/opt/tamahero-server/data/backups"
TIMESTAMP=$(date +%Y-%m-%d)

if [ ! -f "$DB_FILE" ]; then
    echo "Database file not found: $DB_FILE"
    exit 1
fi

mkdir -p "$BACKUP_DIR"
cp "$DB_FILE" "$BACKUP_DIR/tamahero-$TIMESTAMP.db"
echo "Backup created: $BACKUP_DIR/tamahero-$TIMESTAMP.db"

# Remove backups older than 14 days
find "$BACKUP_DIR" -name "tamahero-*.db" -mtime +14 -delete
echo "Old backups cleaned up"
"""
    backup_script.write_text(script_content)
    os.chmod(backup_script, 0o755)
    print(f"Backup script created: {backup_script}")

    cron_line = f"0 4 * * * {backup_script} >> /var/log/{SERVICE_NAME}/backup.log 2>&1\n"

    result = run_command(["crontab", "-l"], check=False)
    existing_cron = result.stdout if result.returncode == 0 else ""

    if str(backup_script) not in existing_cron:
        new_cron = existing_cron.rstrip("\n") + "\n" + cron_line if existing_cron.strip() else cron_line
        run_command(f"echo '{new_cron}' | crontab -", shell=True)
        print("Cron job added: daily backup at 4:00 AM UTC")
    else:
        print("Cron job already exists")


def enable_and_start_service():
    """Enable and start (or restart) the systemd service."""
    print("\n=== Starting Service ===")
    run_command(["systemctl", "daemon-reload"])

    result = run_command(["systemctl", "is-enabled", SERVICE_NAME], check=False)
    if result.returncode != 0:
        run_command(["systemctl", "enable", SERVICE_NAME])
        print("Service enabled (will start on boot)")

    run_command(["systemctl", "restart", SERVICE_NAME])
    print("Service restarted")

    time.sleep(3)
    result = run_command(["systemctl", "status", SERVICE_NAME], check=False)

    if result.returncode == 0:
        print("Service is running")
        return True
    else:
        print("Service may have issues. Check logs with:")
        print(f"  sudo journalctl -u {SERVICE_NAME} -f")
        return False


def print_summary():
    """Print installation/update summary."""
    print("\n" + "="*60)
    print("TamaHero Server Installation Complete!")
    print("="*60)
    print(f"\nService Management:")
    print(f"  Start:   sudo systemctl start {SERVICE_NAME}")
    print(f"  Stop:    sudo systemctl stop {SERVICE_NAME}")
    print(f"  Restart: sudo systemctl restart {SERVICE_NAME}")
    print(f"  Status:  sudo systemctl status {SERVICE_NAME}")
    print(f"\nLog Files:")
    print(f"  Application: {LOG_DIR}/server.log")
    print(f"  Errors:      {LOG_DIR}/error.log")
    print(f"  Live logs:   sudo journalctl -u {SERVICE_NAME} -f")
    print(f"\nConfiguration:")
    print(f"  Environment: {ENV_FILE}")
    print(f"  Service:     {SYSTEMD_SERVICE_PATH}")
    print(f"  Nginx:       /etc/nginx/sites-available/{DOMAIN}")
    print(f"\nServer Details:")
    print(f"  Domain:      {DOMAIN}")
    print(f"  Internal:    127.0.0.1:{SERVER_PORT}")
    print(f"  Server dir:  {INSTALL_DIR}")
    print(f"\nEndpoints:")
    print(f"  API:     https://{DOMAIN}/")
    print(f"  Health:  https://{DOMAIN}/health")
    print(f"  Version: https://{DOMAIN}/version")
    print("\n")


def main():
    """Main installation flow."""
    print("="*60)
    print("TamaHero Server - Production Installation")
    print("="*60)
    print(f"Project root: {PROJECT_ROOT}")
    print(f"Target user:  {SERVICE_USER}")
    print(f"Domain:       {DOMAIN}")
    print()

    try:
        check_root()
        install_java()
        validate_server_dist()
        install_distribution()
        create_directories()
        create_env_file()
        create_systemd_service()
        setup_logrotate()
        configure_firewall()
        install_nginx()
        setup_nginx()
        install_certbot()
        ssl_ok = obtain_ssl_certificate()
        if ssl_ok:
            setup_nginx(force=True)
            setup_certbot_renewal()
            renew_cert_if_needed()
        setup_database_backup_cron()
        is_running = enable_and_start_service()
        print_summary()

        if not is_running:
            print(f"Service is not running. Check logs:")
            print(f"  sudo journalctl -u {SERVICE_NAME} -f")

    except KeyboardInterrupt:
        print("\n\nInstallation cancelled by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\nInstallation failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()
