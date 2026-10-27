#!/usr/bin/env python3
"""
MealShare - Safe Clean Local Development Reset Script
=====================================================
Purpose:
Safely resets the local PostgreSQL database to a clean, empty operational state:
- Cleans operational records (donations, requests, pickups, ratings, notifications).
- Removes prototype and temporary test users.
- PRESERVES the real platform administrator account (admin@mealshare.org).
- Backs up the database before execution.
- NEVER modifies production data or deletes Firebase users blindly.
"""

import os
import sys
import subprocess
import datetime

def log(msg):
    print(f"[CLEAN-RESET] {msg}")

def run_psql_query(query):
    cmd = ["docker", "exec", "-i", "mealshare-postgres", "psql", "-U", "postgres", "-d", "mealshare_app", "-t", "-c", query]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return res.stdout.strip()

def backup_db():
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    backup_file = f"backup_local_mealshare_app_{timestamp}.sql"
    log(f"Backing up database to {backup_file}...")
    cmd = ["docker", "exec", "-i", "mealshare-postgres", "pg_dump", "-U", "postgres", "mealshare_app"]
    with open(backup_file, "w", encoding="utf-8") as f:
        subprocess.run(cmd, stdout=f, check=True)
    log(f"Backup saved successfully ({os.path.getsize(backup_file)} bytes).")
    return backup_file

def print_table_counts(label):
    query = """
    SELECT 'users: ' || count(*) FROM users
    UNION ALL SELECT 'donations: ' || count(*) FROM donations
    UNION ALL SELECT 'donation_requests: ' || count(*) FROM donation_requests
    UNION ALL SELECT 'pickups: ' || count(*) FROM pickups
    UNION ALL SELECT 'ratings: ' || count(*) FROM ratings
    UNION ALL SELECT 'notifications: ' || count(*) FROM notifications
    UNION ALL SELECT 'donor_profiles: ' || count(*) FROM donor_profiles
    UNION ALL SELECT 'receiver_organizations: ' || count(*) FROM receiver_organizations;
    """
    counts = run_psql_query(query)
    log(f"--- Record Counts ({label}) ---")
    for line in counts.splitlines():
        if line.strip():
            log(f"  {line.strip()}")

def execute_clean_reset():
    sql_path = os.path.join(os.path.dirname(__file__), "clean_dev_reset.sql")
    if not os.path.exists(sql_path):
        log(f"ERROR: Cannot find {sql_path}")
        return False

    with open(sql_path, "r", encoding="utf-8") as f:
        sql_content = f.read()

    cmd = ["docker", "exec", "-i", "mealshare-postgres", "psql", "-U", "postgres", "-d", "mealshare_app"]
    try:
        subprocess.run(cmd, input=sql_content, text=True, capture_output=True, check=True)
        log("Clean reset SQL script executed successfully.")
        return True
    except subprocess.CalledProcessError as e:
        log(f"ERROR executing SQL: {e.stderr}")
        return False

def verify_admin_account():
    query = "SELECT id, email, role, status FROM users WHERE email = 'admin@mealshare.org';"
    out = run_psql_query(query)
    log(f"Preserved administrator account: {out}")
    if "admin@mealshare.org" in out and "ADMIN" in out and "ACTIVE" in out:
        log("[OK] Admin account verified intact and ACTIVE.")
        return True
    else:
        log("[ERROR] Administrator account verification failed!")
        return False

def main():
    print("=" * 60)
    print(" MealShare Clean Local Development Reset")
    print(" Removes prototype data | Preserves Platform Administrator")
    print("=" * 60)

    try:
        print_table_counts("BEFORE RESET")
        backup_db()
        if not execute_clean_reset():
            sys.exit(1)
        print_table_counts("AFTER RESET")
        if not verify_admin_account():
            sys.exit(1)
        print("=" * 60)
        print(" CLEAN RESET COMPLETE: Clean database ready for real testing.")
        print(" Platform Administrator is available at admin@mealshare.org.")
        print("=" * 60)
    except Exception as e:
        log(f"FATAL ERROR: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
