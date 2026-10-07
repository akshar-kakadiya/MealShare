#!/usr/bin/env python3
"""
MealShare - Live Registration and Profile Provisioning Test
============================================================
Tests Acceptance Criteria #4 and #5:
- Firebase user creation
- Firebase ID token acquisition
- POST /api/v1/users/register
- Verification of PostgreSQL users & profile tables
- Role onboarding (DONOR, RECEIVER_ORG, RECEIVER_VOL)
- Security guards: ADMIN public registration blocked, email spoofing blocked, duplicate blocked
"""

import urllib.request
import urllib.error
import json
import time
import os
import subprocess
import firebase_admin
from firebase_admin import credentials, auth

BASE_URL = "http://localhost:8081/api/v1"
API_KEY = "AIzaSyB6iV9fG3vO2rT0-3BG76_zhmo488fWBiw"

cred_path = os.path.abspath("backend/mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json")
try:
    firebase_admin.get_app()
except ValueError:
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred)

def get_id_token_for_uid(uid: str) -> str:
    custom_token = auth.create_custom_token(uid)
    if isinstance(custom_token, bytes):
        custom_token = custom_token.decode("utf-8")
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key={API_KEY}"
    payload = json.dumps({"token": custom_token, "returnSecureToken": True}).encode("utf-8")
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as resp:
        res = json.loads(resp.read().decode("utf-8"))
        return res["idToken"]

def api_call(path: str, method="GET", body=None, token=None):
    headers = {"Content-Type": "application/json", "Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(f"{BASE_URL}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            resp_data = resp.read().decode("utf-8")
            try:
                return resp.status, json.loads(resp_data)
            except Exception:
                return resp.status, resp_data
    except urllib.error.HTTPError as e:
        err_data = e.read().decode("utf-8")
        try:
            return e.code, json.loads(err_data)
        except Exception:
            return e.code, err_data

def query_db(sql: str) -> str:
    cmd = ["docker", "exec", "mealshare-postgres", "psql", "-U", "postgres", "-d", "mealshare_app", "-t", "-A", "-c", sql]
    res = subprocess.run(cmd, capture_output=True, text=True, check=True)
    return res.stdout.strip()

def main():
    print("=" * 60)
    print("MEALSHARE LIVE REGISTRATION & ONBOARDING VERIFICATION")
    print("=" * 60)

    ts = int(time.time())
    created_uids = []

    try:
        # TEST 1: Register New Donor
        print("\n[TEST 1] Registering New Surplus Donor...")
        donor_email = f"live.donor.{ts}@mealshare.org"
        donor_fb_user = auth.create_user(email=donor_email, password="DonorPassword123!", display_name="Green Garden Bistro")
        created_uids.append(donor_fb_user.uid)
        donor_token = get_id_token_for_uid(donor_fb_user.uid)
        print(f"  [PASS] Firebase user created: {donor_email} (UID: {donor_fb_user.uid})")

        status, res = api_call("/users/register", "POST", {
            "name": "Green Garden Bistro",
            "email": donor_email,
            "phone": "+919876500001",
            "role": "DONOR"
        }, token=donor_token)
        assert status == 201, f"Expected 201 Created, got {status}: {res}"
        print(f"  [PASS] POST /users/register returned 201 Created. User ID: {res['id']}")

        # Verify DB users table
        db_role = query_db(f"SELECT role FROM users WHERE email = '{donor_email}';")
        assert db_role == "DONOR", f"Expected DONOR in DB, got {db_role}"
        print(f"  [PASS] PostgreSQL 'users' table confirmed role = {db_role}")

        # Verify DB donor_profiles table
        dp_count = query_db(f"SELECT count(*) FROM donor_profiles WHERE user_id = '{res['id']}';")
        assert dp_count == "1", f"Expected 1 donor_profile, got {dp_count}"
        print("  [PASS] PostgreSQL 'donor_profiles' auto-provisioned successfully!")

        # Verify GET /users/me
        status, me = api_call("/users/me", "GET", None, token=donor_token)
        assert status == 200, f"Expected 200, got {status}: {me}"
        assert me["email"] == donor_email
        print(f"  [PASS] GET /users/me verified: {me['name']} ({me['role']})")

        # TEST 2: Register New Receiver Organization
        print("\n[TEST 2] Registering New Receiver Organization...")
        recv_email = f"live.recv.{ts}@mealshare.org"
        recv_fb_user = auth.create_user(email=recv_email, password="ReceiverPassword123!", display_name="Sunrise Care Shelter")
        created_uids.append(recv_fb_user.uid)
        recv_token = get_id_token_for_uid(recv_fb_user.uid)

        status, res = api_call("/users/register", "POST", {
            "name": "Sunrise Care Shelter",
            "email": recv_email,
            "phone": "+919876500002",
            "role": "RECEIVER_ORG"
        }, token=recv_token)
        assert status == 201, f"Expected 201, got {status}: {res}"
        ro_count = query_db(f"SELECT count(*) FROM receiver_organizations WHERE user_id = '{res['id']}';")
        assert ro_count == "1", f"Expected 1 receiver_org, got {ro_count}"
        print("  [PASS] Receiver Org registered and auto-provisioned in 'receiver_organizations'!")

        # TEST 3: Register New Logistics Volunteer
        print("\n[TEST 3] Registering New Logistics Volunteer...")
        vol_email = f"live.vol.{ts}@mealshare.org"
        vol_fb_user = auth.create_user(email=vol_email, password="VolunteerPassword123!", display_name="Pooja Volunteer")
        created_uids.append(vol_fb_user.uid)
        vol_token = get_id_token_for_uid(vol_fb_user.uid)

        status, res = api_call("/users/register", "POST", {
            "name": "Pooja Volunteer",
            "email": vol_email,
            "phone": "+919876500003",
            "role": "RECEIVER_VOL"
        }, token=vol_token)
        assert status == 201, f"Expected 201, got {status}: {res}"
        vp_count = query_db(f"SELECT count(*) FROM volunteer_profiles WHERE user_id = '{res['id']}';")
        assert vp_count == "1", f"Expected 1 volunteer_profile, got {vp_count}"
        print("  [PASS] Logistics Volunteer registered and auto-provisioned in 'volunteer_profiles'!")

        # TEST 4: Security - Block Public Registration as ADMIN
        print("\n[TEST 4] Security: Attempting public registration as ADMIN...")
        hack_email = f"hacker.{ts}@mealshare.org"
        hack_fb_user = auth.create_user(email=hack_email, password="HackPassword123!", display_name="Malicious Actor")
        created_uids.append(hack_fb_user.uid)
        hack_token = get_id_token_for_uid(hack_fb_user.uid)

        status, res = api_call("/users/register", "POST", {
            "name": "Malicious Actor",
            "email": hack_email,
            "role": "ADMIN"
        }, token=hack_token)
        assert status == 403, f"Expected 403 Forbidden for ADMIN registration, got {status}"
        print(f"  [PASS] Public ADMIN registration correctly rejected with HTTP 403: {res}")

        # TEST 5: Security - Block Email Spoofing
        print("\n[TEST 5] Security: Attempting email spoofing (token email != body email)...")
        status, res = api_call("/users/register", "POST", {
            "name": "Spoofed User",
            "email": "victim@mealshare.org",
            "role": "DONOR"
        }, token=hack_token)
        assert status == 403, f"Expected 403 Forbidden for spoofed email, got {status}"
        print(f"  [PASS] Email spoofing correctly rejected with HTTP 403: {res}")

        # TEST 6: Security - Block Duplicate Registration
        print("\n[TEST 6] Security: Attempting duplicate registration on same account...")
        status, res = api_call("/users/register", "POST", {
            "name": "Green Garden Bistro Duplicate",
            "email": donor_email,
            "role": "DONOR"
        }, token=donor_token)
        assert status == 409, f"Expected 409 Conflict for duplicate registration, got {status}"
        print(f"  [PASS] Duplicate registration correctly rejected with HTTP 409: {res}")

        print("\n" + "=" * 60)
        print("ALL REGISTRATION & ONBOARDING ACCEPTANCE CRITERIA VERIFIED!")
        print("=" * 60)

    finally:
        # Cleanup test Firebase accounts and DB test rows
        for uid in created_uids:
            try:
                auth.delete_user(uid)
            except Exception:
                pass
        query_db(f"DELETE FROM users WHERE email LIKE 'live.%.{ts}@mealshare.org' OR email LIKE 'hacker.{ts}@mealshare.org';")
        print("  [CLEANUP] Ephemeral test accounts successfully purged.")

if __name__ == "__main__":
    main()
