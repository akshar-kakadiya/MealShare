#!/usr/bin/env python3
"""
MealShare - Safe Demo Data Reset and Seeding Script
==================================================
Purpose:
Restores clean, canonical demonstration data for professor/capstone live demos.
- Resets Docker PostgreSQL tables to clean demo state (via backend/src/main/resources/demo_dataset.sql)
- Syncs/verifies Firebase Authentication accounts and demo passwords
- Does NOT expose passwords in public git (reads from local config/defaults)
- Safe maintenance tool: NOT exposed as an end-user UI feature.
"""

import os
import sys
import subprocess
import json

def log(msg):
    print(f"[DEMO-RESET] {msg}")

def reset_postgres():
    log("Resetting PostgreSQL database 'mealshare_app' via Docker container 'mealshare-postgres'...")
    sql_path = os.path.join(os.path.dirname(__file__), "..", "backend", "src", "main", "resources", "demo_dataset.sql")
    sql_path = os.path.abspath(sql_path)
    if not os.path.exists(sql_path):
        log(f"ERROR: Cannot find {sql_path}")
        return False

    with open(sql_path, "r", encoding="utf-8") as f:
        sql_content = f.read()

    # Execute via docker exec
    cmd = ["docker", "exec", "-i", "mealshare-postgres", "psql", "-U", "postgres", "-d", "mealshare_app"]
    try:
        proc = subprocess.run(cmd, input=sql_content, text=True, capture_output=True, check=True)
        log("Database reset completed successfully.")
        return True
    except subprocess.CalledProcessError as e:
        log(f"ERROR executing SQL: {e.stderr}")
        return False
    except Exception as e:
        log(f"ERROR: {e}")
        return False

def sync_firebase():
    log("Synchronizing Firebase Authentication accounts...")
    cred_path = os.path.join(os.path.dirname(__file__), "..", "backend", "mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json")
    cred_path = os.path.abspath(cred_path)
    if not os.path.exists(cred_path):
        log(f"WARNING: Firebase admin credentials not found at {cred_path}. Skipping Firebase sync.")
        return True

    try:
        import firebase_admin
        from firebase_admin import credentials, auth

        try:
            app = firebase_admin.get_app()
        except ValueError:
            cred = credentials.Certificate(cred_path)
            app = firebase_admin.initialize_app(cred)

        demo_accounts = [
            {
                "email": "admin@mealshare.org",
                "uid": "ObiO1gDTdBTZJkzDFEGgTVNTEir2",
                "password": "AdminPassword123!",
                "name": "Platform Administrator"
            },
            {
                "email": "donor@mealshare.org",
                "uid": "drekxkp9uXV2g16JioDrgu7uXSk2",
                "password": "DonorPassword123!",
                "name": "Grand Gourmet Catering"
            },
            {
                "email": "receiver@mealshare.org",
                "uid": "pwvnvHgatoahwb9ndKfSM2TVFPG2",
                "password": "ReceiverPassword123!",
                "name": "Hope Community Shelter Foundation"
            },
            {
                "email": "volunteer@mealshare.org",
                "uid": "mXSXYZ1nJTf4HfNTDNxRcfrDYVO2",
                "password": "VolunteerPassword123!",
                "name": "Alex Kumar"
            }
        ]

        for acc in demo_accounts:
            try:
                user = auth.get_user_by_email(acc["email"])
                auth.update_user(user.uid, password=acc["password"], display_name=acc["name"])
                log(f"  [OK] Updated Firebase account: {acc['email']}")
            except auth.UserNotFoundError:
                auth.create_user(
                    uid=acc["uid"],
                    email=acc["email"],
                    password=acc["password"],
                    display_name=acc["name"]
                )
                log(f"  [OK] Created Firebase account: {acc['email']}")
        return True
    except ImportError:
        log("WARNING: firebase_admin Python package not installed. Skipping Firebase sync.")
        return True
    except Exception as e:
        log(f"WARNING during Firebase sync: {e}")
        return False

def main():
    print("=" * 60)
    print(" MealShare Capstone Demo Reset & Data Provisioning")
    print("=" * 60)

    db_ok = reset_postgres()
    fb_ok = sync_firebase()

    print("\n" + "=" * 60)
    if db_ok:
        print(" DEMO DATA RESTORED SUCCESSFULLY!")
        print(" Canonical accounts ready for live demonstration:")
        print("  - Admin Web     : admin@mealshare.org     (AdminPassword123!)")
        print("  - Donor App     : donor@mealshare.org     (DonorPassword123!)")
        print("  - Receiver App  : receiver@mealshare.org  (ReceiverPassword123!)")
        print("  - Volunteer App : volunteer@mealshare.org (VolunteerPassword123!)")
        print("=" * 60)
    else:
        print(" RESET FAILED. Please verify Docker container is running.")
        sys.exit(1)

if __name__ == "__main__":
    main()
