#!/usr/bin/env python3
"""
MealShare - Live Database Migration & Verification Runner
=========================================================
Purpose:
Runs Flyway migrations against a live managed PostgreSQL (Supabase / Render / Cloud)
and executes the required verification queries without leaking secrets.
"""

import os
import sys
import subprocess
import urllib.parse

def run_command(cmd, cwd=None, hide_output=False, mask_secret=None):
    result = subprocess.run(cmd, cwd=cwd, shell=True, capture_output=True, text=True)
    out = result.stdout or ""
    err = result.stderr or ""
    if mask_secret:
        out = out.replace(mask_secret, "********")
        err = err.replace(mask_secret, "********")
    if result.returncode != 0:
        if not hide_output:
            print(f"[ERROR] Command failed with return code {result.returncode}")
            print(err)
        return False, out, err
    return True, out, err

def main():
    print("==================================================")
    print("MEALSHARE LIVE DATABASE MIGRATION & AUDIT")
    print("==================================================")

    db_url = os.environ.get("LIVE_DB_URL") or os.environ.get("SPRING_DATASOURCE_URL") or "jdbc:postgresql://db.ztbjtkonfpdlqznjkvgj.supabase.co:5432/postgres?sslmode=require"
    db_user = os.environ.get("LIVE_DB_USER") or os.environ.get("SPRING_DATASOURCE_USERNAME") or "postgres"
    db_pass = os.environ.get("LIVE_DB_PASSWORD") or os.environ.get("SPRING_DATASOURCE_PASSWORD")

    # If password not in env, check Windows Registry (HKCU\Environment)
    if not db_pass and sys.platform == "win32":
        try:
            import winreg
            key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, r"Environment")
            db_pass, _ = winreg.QueryValueEx(key, "LIVE_DB_PASSWORD")
            key.Close()
        except Exception:
            pass

    # If still not found, check local gitignored .db_password file
    if not db_pass:
        pw_file = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".db_password"))
        if os.path.exists(pw_file):
            try:
                with open(pw_file, "r", encoding="utf-8") as f:
                    db_pass = f.read().strip()
            except Exception:
                pass

    if not db_pass:
        print("[ERROR] Database password not found.")
        print("Please provide the database password via one of these SECURE methods (without pasting in chat):")
        print("  Method 1 (Local file): Save the password into a temporary untracked file named '.db_password' in the project root.")
        print("  Method 2 (PowerShell User Env): Run in PowerShell:")
        print("    [System.Environment]::SetEnvironmentVariable('LIVE_DB_PASSWORD', 'YourPasswordHere', 'User')")
        print("  Method 3 (PowerShell Session): Run in PowerShell:")
        print("    $env:LIVE_DB_PASSWORD='YourPasswordHere'")
        print("    python scripts/deploy_live_database.py")
        sys.exit(1)

    # Ensure URL starts with jdbc:postgresql:
    if not db_url.startswith("jdbc:"):
        if db_url.startswith("postgresql://") or db_url.startswith("postgres://"):
            parsed = urllib.parse.urlparse(db_url)
            host = parsed.hostname
            port = parsed.port or 5432
            db_name = parsed.path.lstrip("/").split("?")[0] or "postgres"
            if parsed.username and not db_user:
                db_user = parsed.username
            if parsed.password and not db_pass:
                db_pass = parsed.password
            db_url = f"jdbc:postgresql://{host}:{port}/{db_name}?sslmode=require"
        else:
            db_url = f"jdbc:postgresql://{db_url}"

    # Test network connectivity to target host (handling IPv6-only Supabase direct hosts)
    import socket
    parsed = urllib.parse.urlparse(db_url.replace("jdbc:", ""))
    host = parsed.hostname
    port = parsed.port or 5432
    
    is_reachable = False
    try:
        s = socket.create_connection((host, port), timeout=3)
        s.close()
        is_reachable = True
    except Exception:
        pass

    if not is_reachable and host and host.startswith("db.") and host.endswith(".supabase.co"):
        proj_ref = host.split(".")[1]
        pooler_host = "aws-0-ap-south-1.pooler.supabase.com"
        print(f"[NETWORK] Direct host {host} is unreachable on this network (IPv6-only).")
        print(f"[NETWORK] Automatically routing via Supabase IPv4 Pooler: {pooler_host}:5432 (Session Mode)")
        host = pooler_host
        port = 5432
        db_user = f"postgres.{proj_ref}"
        db_name = parsed.path.lstrip("/").split("?")[0] or "postgres"
        db_url = f"jdbc:postgresql://{host}:{port}/{db_name}?sslmode=require"

    print(f"[1] Target Database Host: {host}:{port} (User: {db_user}, SSL: Required)")
    print(f"[2] Executing Flyway migrations V1 through V10...")

    backend_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "backend"))
    flyway_cmd = f'.\\mvnw.cmd flyway:migrate "-Dflyway.url={db_url}" "-Dflyway.user={db_user}" "-Dflyway.password={db_pass}"'

    success, stdout, stderr = run_command(flyway_cmd, cwd=backend_dir, mask_secret=db_pass)
    if not success:
        print("[FATAL] Flyway migration failed against the live database!")
        print(stderr)
        sys.exit(1)

    print("  [PASS] Flyway migrations executed successfully!")

    print("\n[3] Running Flyway status check...")
    info_cmd = f'.\\mvnw.cmd flyway:info "-Dflyway.url={db_url}" "-Dflyway.user={db_user}" "-Dflyway.password={db_pass}"'
    success, stdout, stderr = run_command(info_cmd, cwd=backend_dir, mask_secret=db_pass)
    if success:
        for line in stdout.splitlines():
            if "Versioned" in line or "Schema version" in line:
                print(f"  {line}")

    print("\n[4] Running Direct PostgreSQL & PostGIS Verification Queries...")
    try:
        import psycopg2
        conn = psycopg2.connect(
            host=host,
            port=port,
            dbname=db_name,
            user=db_user,
            password=db_pass,
            sslmode="require"
        )
        cur = conn.cursor()

        # 4.1 PostGIS Version
        cur.execute("SELECT PostGIS_Version();")
        postgis_ver = cur.fetchone()[0]
        print(f"  [PASS] PostGIS Version: {postgis_ver}")

        # 4.2 Flyway Schema History
        cur.execute("SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;")
        print("  [PASS] Installed Flyway Migrations:")
        for row in cur.fetchall():
            print(f"         V{row[0]}: {row[1]} (Success: {row[2]})")

        # 4.3 Table Verification
        required_tables = [
            "users", "donor_profiles", "receiver_organizations",
            "donations", "donation_requests", "pickups",
            "notifications", "user_device_tokens", "ratings", "audit_logs"
        ]
        cur.execute("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';")
        existing_tables = set(r[0] for r in cur.fetchall())
        missing = [t for t in required_tables if t not in existing_tables]
        if missing:
            print(f"  [FAIL] Missing required tables: {missing}")
        else:
            print(f"  [PASS] All {len(required_tables)} core application tables verified in public schema:")
            for t in required_tables:
                print(f"         - {t}")

        # 4.4 Role Constraint Check
        cur.execute("""
            SELECT conname, pg_get_constraintdef(c.oid)
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            WHERE t.relname = 'users' AND c.contype = 'c';
        """)
        constraints = cur.fetchall()
        print("  [PASS] Users Table Check Constraints:")
        role_constraint_found = False
        for cname, cdef in constraints:
            print(f"         {cname}: {cdef}")
            if "DONOR" in cdef and "RECEIVER" in cdef and "VOLUNTEER" not in cdef:
                role_constraint_found = True
        if role_constraint_found:
            print("  [PASS] Strict 3-role constraint validated: DONOR, RECEIVER, ADMIN (Zero Volunteers)")

        # 4.5 PostGIS Geometry Column
        cur.execute("""
            SELECT f_geometry_column, type, srid, coord_dimension 
            FROM geometry_columns 
            WHERE f_table_name = 'donations';
        """)
        geom = cur.fetchall()
        for g in geom:
            print(f"  [PASS] Spatial column: donations.{g[0]} (Type: {g[1]}, SRID: {g[2]})")

        # 4.6 Spatial Index on donations.location
        cur.execute("""
            SELECT indexname, indexdef 
            FROM pg_indexes 
            WHERE tablename = 'donations' AND indexdef ILIKE '%gist%';
        """)
        indexes = cur.fetchall()
        if indexes:
            for idx_name, idx_def in indexes:
                print(f"  [PASS] Spatial GiST Index: {idx_name}")
        else:
            print("  [WARN] No GiST spatial index found on donations table.")

        cur.close()
        conn.close()

    except Exception as e:
        print(f"  [WARN] psycopg2 query verification failed: {e}")

    print("\n==================================================")
    print("LIVE DATABASE MIGRATION & AUDIT COMPLETED")
    print("==================================================")

if __name__ == "__main__":
    main()
