import os
import firebase_admin
from firebase_admin import credentials, auth
import urllib.request
import urllib.error
import json
import sys
import datetime
from datetime import timezone

BASE_URL = os.environ.get("LIVE_BASE_URL", os.environ.get("BASE_URL", "http://localhost:8081/api/v1")).rstrip("/")
API_KEY = os.environ.get("FIREBASE_API_KEY", "AIzaSyB6iV9fG3vO2rT0-3BG76_zhmo488fWBiw")

# Initialize Firebase Admin
cred_path = os.environ.get("FIREBASE_CREDENTIALS_PATH", "backend/mealshare-100-firebase-adminsdk-fbsvc-8964cd362d.json")
cred = credentials.Certificate(cred_path)
try:
    firebase_admin.get_app()
except ValueError:
    firebase_admin.initialize_app(cred)

def get_id_token(uid: str) -> str:
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

def main():
    print("==================================================")
    print("MEALSHARE 3-ROLE LIVE END-TO-END DEMO WORKFLOW")
    print("  Roles: DONOR, RECEIVER, ADMIN (Zero Volunteers)")
    print("==================================================")

    # UIDs
    DONOR_UID = "drekxkp9uXV2g16JioDrgu7uXSk2"
    RECEIVER_UID = "pwvnvHgatoahwb9ndKfSM2TVFPG2"
    ADMIN_UID = "ObiO1gDTdBTZJkzDFEGgTVNTEir2"
    OTHER_RECEIVER_UID = "X7cbD4mXYCOoC0TZkRjT3cbDSHv2"

    print("\n[1] Acquiring genuine Firebase ID tokens for 4 actors...")
    donor_token = get_id_token(DONOR_UID)
    receiver_token = get_id_token(RECEIVER_UID)
    admin_token = get_id_token(ADMIN_UID)
    other_receiver_token = get_id_token(OTHER_RECEIVER_UID)
    print("  -> Donor token acquired (len:", len(donor_token), ")")
    print("  -> Receiver token acquired (len:", len(receiver_token), ")")
    print("  -> Admin token acquired (len:", len(admin_token), ")")
    print("  -> Other Receiver token acquired (len:", len(other_receiver_token), ")")

    # Step 1: Donor creates donation
    print("\n[2] DONOR: Creating fresh surplus food donation (40 meals)...")
    now = datetime.datetime.now(timezone.utc)
    prep_time = (now - datetime.timedelta(hours=1)).isoformat()
    pickup_from = (now - datetime.timedelta(minutes=10)).isoformat()
    pickup_until = (now + datetime.timedelta(hours=6)).isoformat()
    consume_before = (now + datetime.timedelta(hours=8)).isoformat()

    donation_payload = {
        "title": "Fresh Steamed Meals & Dal (3-Role Live Demo)",
        "description": "Hot nutritious meals prepared by Grand Gourmet Catering, packed in insulated food-grade containers.",
        "foodCategory": "PREPARED_MEALS",
        "quantity": 40.0,
        "quantityUnit": "meals",
        "preparationDateTime": prep_time,
        "consumeBefore": consume_before,
        "pickupAvailableFrom": pickup_from,
        "pickupAvailableUntil": pickup_until,
        "pickupAddress": "100 MG Road, Bengaluru, Karnataka 560001",
        "latitude": 12.9716,
        "longitude": 77.5946,
        "foodSafetyDeclared": True,
        "foodSafetyDeclarationText": "Hygiene standards verified; temperature monitored and packed in thermo containers."
    }

    status, res = api_call("/donor/donations", "POST", donation_payload, donor_token)
    assert status == 201, f"Expected 201 Created, got {status}: {res}"
    donation_id = res["id"]
    print(f"  [PASS] Donation created: ID = {donation_id}, Status = {res['status']}")

    # Step 2: Receiver discovers donation
    print("\n[3] RECEIVER: Spatial search within 15 km of Bangalore center (12.9700, 77.6000)...")
    status, res = api_call("/receiver/donations/nearby?latitude=12.9700&longitude=77.6000&radiusKm=15", "GET", None, receiver_token)
    assert status == 200, f"Expected 200 OK, got {status}: {res}"
    items = res.get("content", [])
    found = next((d for d in items if d["id"] == donation_id), None)
    assert found is not None, f"Donation {donation_id} not found in nearby results: {items}"
    print(f"  [PASS] Found newly published donation via PostGIS! Title = '{found['title']}', Distance = {found.get('distanceKm')} km")

    # Step 3: Receiver requests 15 meals
    print("\n[4] RECEIVER: Requesting 15 meals for community shelter distribution...")
    request_payload = {
        "requestedQuantity": 15.0,
        "requestReason": "Evening community distribution at Hope Foundation shelter"
    }
    status, res = api_call(f"/receiver/donations/{donation_id}/requests", "POST", request_payload, receiver_token)
    assert status == 201, f"Expected 201 Created, got {status}: {res}"
    request_id = res["id"]
    print(f"  [PASS] Request submitted: ID = {request_id}, Status = {res['status']}, Requested Qty = {res['requestedQuantity']}")

    # Step 4: Donor reviews and approves request
    print("\n[5] DONOR: Reviewing incoming requests and approving claim...")
    status, res = api_call(f"/donor/donations/{donation_id}/requests", "GET", None, donor_token)
    assert status == 200, f"Expected 200 OK, got {status}: {res}"
    req_items = res.get("content", [])
    donor_req = next((r for r in req_items if (r.get("requestId") or r.get("id")) == request_id), None)
    assert donor_req is not None, f"Request {request_id} not visible to donor!"
    org_name = donor_req.get("receiverOrganizationName") or donor_req.get("receiverName") or "Receiver"
    print(f"  [PASS] Donor sees request from {org_name} in FCFS order.")

    status, res = api_call(f"/donor/requests/{request_id}/approve", "POST", None, donor_token)
    assert status == 200, f"Expected 200 OK, got {status}: {res}"
    print(f"  [PASS] Request approved! Request Status = {res['requestStatus']}")
    print(f"         Allocated Quantity = {res['allocatedQuantity']}, Remaining Surplus = {res['remainingQuantity']}")
    print(f"         Donation Status = {res['donationStatus']} (remains AVAILABLE because surplus > 0)")
    assert res["allocatedQuantity"] == 15.0
    assert res["remainingQuantity"] == 25.0
    assert res["donationStatus"] == "AVAILABLE"

    # Step 5: Admin schedules food pickup directly for receiver
    print("\n[6] ADMIN: Scheduling food pickup directly for claiming receiver...")
    scheduled_at = (now + datetime.timedelta(hours=2)).isoformat()
    assign_payload = {
        "donationRequestId": request_id,
        "scheduledAt": scheduled_at
    }
    status, res = api_call("/admin/pickups", "POST", assign_payload, admin_token)
    assert status == 201, f"Expected 201 Created, got {status}: {res}"
    pickup_id = res.get("pickupId") or res.get("id")
    # Verify that NO plaintext OTP or secret is returned to the Admin
    assert "devOtpCode" not in res or res.get("devOtpCode") is None, f"Admin response must NOT contain devOtpCode: {res}"
    assert "otp" not in res or res.get("otp") is None, f"Admin response must NOT contain otp: {res}"
    assert "otpHash" not in res or res.get("otpHash") is None, f"Admin response must NOT contain otpHash: {res}"
    print(f"  [PASS] Pickup scheduled: Pickup ID = {pickup_id}, Status = {res['status']}")
    print(f"         Receiver ID = {res.get('receiverId')}")
    print(f"         [CONFIRMED] Admin response is confidential: zero OTP/secret data exposed.")

    # Step 6b: OTP Access Control & Legitimate Donor Handover Retrieval
    print("\n[7] SECURITY / OTP SECRECY: Verifying handover OTP access controls...")
    # Receiver blocked from Donor OTP endpoint
    rec_otp_status, rec_otp_res = api_call(f"/donor/pickups/{pickup_id}/otp", "GET", None, receiver_token)
    assert rec_otp_status == 403, f"Expected 403 for Receiver accessing donor OTP, got {rec_otp_status}: {rec_otp_res}"
    print("  [PASS] Receiver blocked from viewing handover OTP (HTTP 403 Forbidden)")

    # Admin blocked from Donor OTP endpoint
    adm_otp_status, adm_otp_res = api_call(f"/donor/pickups/{pickup_id}/otp", "GET", None, admin_token)
    assert adm_otp_status == 403, f"Expected 403 for Admin accessing donor OTP, got {adm_otp_status}: {adm_otp_res}"
    print("  [PASS] Admin blocked from viewing handover OTP (HTTP 403 Forbidden)")

    # Unauthenticated blocked from Donor OTP endpoint
    unauth_otp_status, _ = api_call(f"/donor/pickups/{pickup_id}/otp", "GET", None, None)
    assert unauth_otp_status == 401, f"Expected 401 for unauthenticated request, got {unauth_otp_status}"
    print("  [PASS] Unauthenticated request blocked from viewing handover OTP (HTTP 401 Unauthorized)")

    # Legitimate Authorized Donor retrieves handover OTP
    donor_otp_status, donor_otp_res = api_call(f"/donor/pickups/{pickup_id}/otp", "GET", None, donor_token)
    assert donor_otp_status == 200, f"Expected 200 OK for Donor viewing handover OTP, got {donor_otp_status}: {donor_otp_res}"
    generated_otp = donor_otp_res.get("otp")
    assert generated_otp is not None and len(generated_otp) == 6 and generated_otp.isdigit(), f"Invalid OTP format: {generated_otp}"
    print(f"  [PASS] Authorized Donor successfully retrieved 6-digit Handover OTP: {generated_otp}")
    print(f"         Donor Pickup Info: Donation = '{donor_otp_res.get('donationTitle')}', Receiver = '{donor_otp_res.get('receiverOrganizationName')}'")

    # Step 7: Receiver sees scheduled pickup in their pickups dashboard (without OTP)
    print("\n[8] RECEIVER: Checking scheduled pickups dashboard (/api/v1/receiver/pickups)...")
    status, res = api_call("/receiver/pickups", "GET", None, receiver_token)
    assert status == 200, f"Expected 200 OK, got {status}: {res}"
    pickups = res.get("content", [])
    my_pickup = next((p for p in pickups if (p.get("pickupId") or p.get("id")) == pickup_id), None)
    assert my_pickup is not None, f"Pickup {pickup_id} not found in receiver pickups!"
    assert "otp" not in my_pickup or my_pickup.get("otp") is None, "Receiver must NOT receive plaintext OTP in summary"
    assert "devOtpCode" not in my_pickup or my_pickup.get("devOtpCode") is None, "Receiver must NOT receive devOtpCode in summary"
    print(f"  [PASS] Receiver sees scheduled pickup for '{my_pickup['donationTitle']}', Status = {my_pickup['status']}")
    print(f"         [CONFIRMED] Receiver view contains zero OTP secrets.")

    # Step 8: IDOR Protection Check
    print("\n[9] SECURITY / IDOR: Another receiver attempts to inspect this pickup...")
    status, res = api_call(f"/receiver/pickups/{pickup_id}", "GET", None, other_receiver_token)
    assert status == 403, f"Expected 403 Forbidden, got {status}: {res}"
    print(f"  [PASS] IDOR Protection confirmed! HTTP 403 Forbidden returned for unassigned receiver.")

    # Step 9: Receiver enters wrong OTP
    print("\n[10] RECEIVER: Submitting WRONG OTP ('000000')...")
    status, res = api_call(f"/receiver/pickups/{pickup_id}/verify-otp", "POST", {"otp": "000000"}, receiver_token)
    assert status in (400, 409), f"Expected 400 or 409, got {status}: {res}"
    print(f"  [PASS] Wrong OTP correctly rejected with HTTP {status}: {res.get('message', res)}")

    # Step 10: Receiver enters correct OTP upon food collection from donor
    print(f"\n[11] RECEIVER: Submitting CORRECT OTP ('{generated_otp}')...")
    status, res = api_call(f"/receiver/pickups/{pickup_id}/verify-otp", "POST", {"otp": generated_otp}, receiver_token)
    assert status == 200, f"Expected 200 OK, got {status}: {res}"
    assert res["status"] == "PICKED_UP", f"Expected PICKED_UP, got {res['status']}"
    assert res.get("pickedUpAt") is not None, "Missing pickedUpAt timestamp"
    print(f"  [PASS] Correct OTP verified! Pickup status = {res['status']}, Picked Up At = {res['pickedUpAt']}")

    # Step 11: Replay Prevention & OTP Invalidation
    print("\n[12] SECURITY / REPLAY: Attempting to re-verify OTP after food pickup...")
    status, res = api_call(f"/receiver/pickups/{pickup_id}/verify-otp", "POST", {"otp": generated_otp}, receiver_token)
    assert status in (400, 409), f"Expected 400 or 409 for replay, got {status}: {res}"
    print(f"  [PASS] Replay prevention verified! Re-verification rejected with HTTP {status}.")

    # Verify donor OTP is now invalidated/rejected since pickup is PICKED_UP
    post_pickup_otp_status, _ = api_call(f"/donor/pickups/{pickup_id}/otp", "GET", None, donor_token)
    assert post_pickup_otp_status in (400, 404, 409), f"Expected 400/404/409 after pickup is PICKED_UP, got {post_pickup_otp_status}"
    print("  [PASS] Single-use cache eviction confirmed! Donor OTP endpoint rejects request once food is collected.")

    # Step 12: Receiver marks pickup / delivery complete
    print("\n[13] RECEIVER: Completing food collection / delivery...")
    status, res = api_call(f"/receiver/pickups/{pickup_id}/complete", "POST", None, receiver_token)
    assert status == 200, f"Expected 200 OK, got {status}: {res}"
    assert res["status"] == "COMPLETED", f"Expected COMPLETED, got {res['status']}"
    assert res.get("completedAt") is not None, "Missing completedAt timestamp"
    print(f"  [PASS] Delivery confirmed! Status = {res['status']}, Completed At = {res['completedAt']}")

    # Step 13: Receiver rates Donor
    print("\n[14] RATINGS / RECEIVER: Rating the Donor (5 stars + feedback)...")
    receiver_rating_payload = {
        "pickupId": pickup_id,
        "rating": 5,
        "feedback": "Outstanding food condition, warm insulated packing, and prompt coordination by donor!"
    }
    status, res = api_call("/ratings", "POST", receiver_rating_payload, receiver_token)
    assert status == 201, f"Expected 201 Created for Receiver rating, got {status}: {res}"
    receiver_rating_id = res.get("id")
    assert res.get("rating") == 5, f"Expected rating 5, got {res.get('rating')}"
    assert res.get("raterRole") == "RECEIVER", f"Expected raterRole RECEIVER, got {res.get('raterRole')}"
    assert res.get("ratedRole") == "DONOR", f"Expected ratedRole DONOR, got {res.get('ratedRole')}"
    print(f"  [PASS] Receiver rating submitted: ID = {receiver_rating_id}, Rating = {res['rating']} Stars")
    print(f"         Rater: {res['raterName']} ({res['raterRole']}) -> Rated: {res['ratedName']} ({res['ratedRole']})")

    # Step 14: Donor rates Receiver
    print("\n[15] RATINGS / DONOR: Rating the Receiver (5 stars + feedback)...")
    donor_rating_payload = {
        "pickupId": pickup_id,
        "rating": 5,
        "feedback": "Receiver arrived on time with proper food carriers and completed pickup courteously."
    }
    status, res = api_call("/ratings", "POST", donor_rating_payload, donor_token)
    assert status == 201, f"Expected 201 Created for Donor rating, got {status}: {res}"
    donor_rating_id = res.get("id")
    assert res.get("rating") == 5, f"Expected rating 5, got {res.get('rating')}"
    assert res.get("raterRole") == "DONOR", f"Expected raterRole DONOR, got {res.get('raterRole')}"
    assert res.get("ratedRole") == "RECEIVER", f"Expected ratedRole RECEIVER, got {res.get('ratedRole')}"
    print(f"  [PASS] Donor rating submitted: ID = {donor_rating_id}, Rating = {res['rating']} Stars")
    print(f"         Rater: {res['raterName']} ({res['raterRole']}) -> Rated: {res['ratedName']} ({res['ratedRole']})")

    # Step 15: Duplicate Rating Prevention
    print("\n[16] SECURITY / DUPLICATE RATINGS: Attempting duplicate submissions...")
    status, res = api_call("/ratings", "POST", receiver_rating_payload, receiver_token)
    assert status in (400, 409), f"Expected 409 or 400 for duplicate receiver rating, got {status}: {res}"
    print(f"  [PASS] Duplicate Receiver rating correctly rejected with HTTP {status}: {res.get('message', res)}")

    status, res = api_call("/ratings", "POST", donor_rating_payload, donor_token)
    assert status in (400, 409), f"Expected 409 or 400 for duplicate donor rating, got {status}: {res}"
    print(f"  [PASS] Duplicate Donor rating correctly rejected with HTTP {status}: {res.get('message', res)}")

    # Step 16: Ratings Security & Authorization Checks
    print("\n[17] SECURITY / RATINGS: Authorization, IDOR & input validation checks...")
    # Admin blocked from rating
    status, res = api_call("/ratings", "POST", {"pickupId": pickup_id, "rating": 5}, admin_token)
    assert status == 403, f"Expected 403 for Admin rating, got {status}: {res}"
    print("  [PASS] Admin blocked from submitting transaction ratings (HTTP 403 Forbidden)")

    # Unrelated receiver blocked from rating
    status, res = api_call("/ratings", "POST", {"pickupId": pickup_id, "rating": 5}, other_receiver_token)
    assert status == 403, f"Expected 403 for unrelated receiver rating, got {status}: {res}"
    print("  [PASS] Unrelated Receiver blocked from rating this pickup (HTTP 403 Forbidden)")

    # Unauthenticated blocked
    status, _ = api_call("/ratings", "POST", {"pickupId": pickup_id, "rating": 5}, None)
    assert status == 401, f"Expected 401 for unauthenticated rating, got {status}"
    print("  [PASS] Unauthenticated request blocked (HTTP 401 Unauthorized)")

    # Invalid rating values (< 1 or > 5)
    status, _ = api_call("/ratings", "POST", {"pickupId": pickup_id, "rating": 0}, donor_token)
    assert status == 400, f"Expected 400 for rating 0, got {status}"
    status, _ = api_call("/ratings", "POST", {"pickupId": pickup_id, "rating": 6}, donor_token)
    assert status == 400, f"Expected 400 for rating 6, got {status}"
    print("  [PASS] Invalid rating ranges (0 and 6) rejected with HTTP 400 Bad Request")

    # Step 17: Pickup Rating Status Verification
    print("\n[18] RATINGS: Verifying pickup rating status endpoint (/api/v1/ratings/pickup/{id})...")
    status, res = api_call(f"/ratings/pickup/{pickup_id}", "GET", None, donor_token)
    assert status == 200, f"Expected 200 OK for donor rating status, got {status}: {res}"
    assert res.get("alreadyRated") is True, "Donor must be marked as already rated"
    assert res.get("myRating") is not None, "Donor must see their own submitted rating"
    assert res.get("counterpartRating") is not None, "Donor must see counterpart receiver rating"
    print(f"  [PASS] Donor rating status verified: alreadyRated = {res['alreadyRated']}, myRating = {res['myRating']['rating']} Stars")

    status, res = api_call(f"/ratings/pickup/{pickup_id}", "GET", None, receiver_token)
    assert status == 200, f"Expected 200 OK for receiver rating status, got {status}: {res}"
    assert res.get("alreadyRated") is True, "Receiver must be marked as already rated"
    print(f"  [PASS] Receiver rating status verified: alreadyRated = {res['alreadyRated']}, myRating = {res['myRating']['rating']} Stars")

    # Step 18: Admin Ratings View Verification
    print("\n[19] ADMIN: Verifying moderation ratings dashboard (/api/v1/admin/ratings)...")
    status, res = api_call("/admin/ratings", "GET", None, admin_token)
    assert status == 200, f"Expected 200 OK for admin ratings, got {status}: {res}"
    admin_ratings = res.get("content", [])
    assert len(admin_ratings) >= 2, f"Admin must see at least 2 ratings, found: {len(admin_ratings)}"
    pickup_ratings = [r for r in admin_ratings if r.get("pickupId") == pickup_id]
    assert len(pickup_ratings) == 2, f"Expected exactly 2 ratings for pickup {pickup_id}, found: {len(pickup_ratings)}"
    print(f"  [PASS] Admin successfully retrieved ratings table: found {len(pickup_ratings)} ratings for this pickup.")

    # Step 19: Negative Authorization Checks
    print("\n[20] SECURITY: General negative authorization checks...")
    unauth_status, _ = api_call("/donor/donations", "GET", None, None)
    assert unauth_status == 401, f"Expected 401 for unauthenticated request, got {unauth_status}"
    print(f"  [PASS] Unauthenticated request -> HTTP 401 Unauthorized")

    donor_admin_status, _ = api_call("/admin/users", "GET", None, donor_token)
    assert donor_admin_status == 403, f"Expected 403 for donor accessing admin, got {donor_admin_status}"
    print(f"  [PASS] Donor accessing Admin endpoint -> HTTP 403 Forbidden")

    rec_donor_status, _ = api_call("/donor/donations", "POST", donation_payload, receiver_token)
    assert rec_donor_status == 403, f"Expected 403 for receiver creating donation, got {rec_donor_status}"
    print(f"  [PASS] Receiver accessing Donor endpoint -> HTTP 403 Forbidden")

    print("\n==================================================")
    print("ALL 20 WORKFLOW PHASES PASSED WITH ZERO FAILURES!")
    print(f"  Donation ID: {donation_id}")
    print(f"  Request ID : {request_id}")
    print(f"  Pickup ID  : {pickup_id}")
    print(f"  Receiver Rating ID: {receiver_rating_id}")
    print(f"  Donor Rating ID   : {donor_rating_id}")
    print("==================================================")

if __name__ == "__main__":
    main()
