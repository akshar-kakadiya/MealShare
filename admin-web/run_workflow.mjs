import { initializeApp } from 'firebase/app';
import { getAuth, signInWithEmailAndPassword } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyB6iV9fG3vO2rT0-3BG76_zhmo488fWBiw",
  authDomain: "mealshare-100.firebaseapp.com",
  projectId: "mealshare-100",
  storageBucket: "mealshare-100.firebasestorage.app",
  messagingSenderId: "435165674165",
  appId: "1:435165674165:web:4f7b649e3ff8ec55a89cfc"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const BASE_URL = "http://localhost:8081/api/v1";

async function getToken(email, password) {
  const cred = await signInWithEmailAndPassword(auth, email, password);
  return await cred.user.getIdToken();
}

async function api(path, method = 'GET', body = null, token = null) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : null
  });

  const text = await res.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch {
    json = text;
  }
  return { status: res.status, ok: res.ok, data: json };
}

async function run() {
  console.log("==================================================");
  console.log("MEALSHARE FRESH END-TO-END WORKFLOW EXECUTION");
  console.log("==================================================\n");

  console.log("--> Authenticating all actors with Firebase Auth...");
  const donorToken = await getToken("donor@mealshare.org", process.env.DONOR_PASSWORD || "<DONOR_PASSWORD>");
  const receiverToken = await getToken("receiver@mealshare.org", process.env.RECEIVER_PASSWORD || "<RECEIVER_PASSWORD>");
  const adminToken = await getToken("admin@mealshare.org", process.env.ADMIN_PASSWORD || "<ADMIN_PASSWORD>");
  const volunteerToken = await getToken("volunteer@mealshare.org", process.env.VOLUNTEER_PASSWORD || "<VOLUNTEER_PASSWORD>");
  const otherVolToken = await getToken("other.vol@mealshare.org", process.env.OTHER_VOL_PASSWORD || "<OTHER_VOL_PASSWORD>");
  console.log("[OK] All 5 Firebase ID tokens successfully acquired.\n");

  // ----------------------------------------------------
  // PART 2: CREATE FRESH DONATION
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 2: CREATE FRESH DONATION (Donor)");
  console.log("--------------------------------------------------");
  const now = new Date();
  const prepTime = new Date(now.getTime() - 60 * 60 * 1000).toISOString(); // 1h ago
  const pickupFrom = new Date(now.getTime() - 15 * 60 * 1000).toISOString(); // 15m ago
  const pickupUntil = new Date(now.getTime() + 6 * 60 * 60 * 1000).toISOString(); // 6h ahead
  const consumeBefore = new Date(now.getTime() + 8 * 60 * 60 * 1000).toISOString(); // 8h ahead

  const donationPayload = {
    title: "Fresh Vegetable Biryani",
    description: "Authentic dum vegetable biryani prepared for banquet event, safely packed in thermo-insulated containers",
    foodCategory: "PREPARED_MEALS",
    quantity: 40.0,
    quantityUnit: "meals",
    preparationDateTime: prepTime,
    consumeBefore: consumeBefore,
    pickupAvailableFrom: pickupFrom,
    pickupAvailableUntil: pickupUntil,
    pickupAddress: "Grand Gourmet Catering, MG Road, Bengaluru, Karnataka 560001",
    latitude: 12.9716,
    longitude: 77.5946,
    foodSafetyDeclared: true,
    foodSafetyDeclarationText: "Hygiene standards verified; temperature held above 65C until insulated packing"
  };

  const createDonationRes = await api("/donor/donations", "POST", donationPayload, donorToken);
  console.log("Create Donation HTTP Status:", createDonationRes.status);
  if (!createDonationRes.ok) {
    console.error("Failed to create donation:", createDonationRes.data);
    process.exit(1);
  }
  const donation = createDonationRes.data;
  const donationId = donation.id;
  console.log("Fresh Donation Created Successfully!");
  console.log("Donation ID:", donationId);
  console.log("Title:", donation.title);
  console.log("Quantity:", donation.quantity, donation.quantityUnit);
  console.log("Allocated Quantity:", donation.allocatedQuantity);
  console.log("Status:", donation.status);
  console.log("");

  // ----------------------------------------------------
  // PART 3: RECEIVER DISCOVERY
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 3: RECEIVER DISCOVERY (Receiver Organization)");
  console.log("--------------------------------------------------");
  const nearbyRes = await api("/receiver/donations/nearby?latitude=12.9716&longitude=77.5946&radiusKm=15", "GET", null, receiverToken);
  console.log("Nearby Discovery HTTP Status:", nearbyRes.status);
  if (!nearbyRes.ok) {
    console.error("Failed to query nearby donations:", nearbyRes.data);
    process.exit(1);
  }

  const items = nearbyRes.data.content || [];
  const foundDonation = items.find(d => d.id === donationId);
  if (!foundDonation) {
    console.error("Fresh donation not found in nearby search results! Items:", items);
    process.exit(1);
  }
  console.log("[OK] Receiver successfully discovered newly published donation via PostGIS spatial query!");
  console.log("Discovered Donation ID:", foundDonation.id);
  console.log("Title:", foundDonation.title);
  console.log("Quantity:", foundDonation.quantity);
  console.log("Distance (km):", foundDonation.distanceKm);
  console.log("Pickup Available Until:", foundDonation.pickupAvailableUntil);
  console.log("Consume Before:", foundDonation.consumeBefore);
  console.log("");

  // ----------------------------------------------------
  // PART 4: CREATE REQUEST
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 4: CREATE REQUEST (Receiver Organization)");
  console.log("--------------------------------------------------");
  const requestPayload = {
    requestedQuantity: 15.0,
    requestReason: "Evening hot meal distribution for shelter residents"
  };

  const createReqRes = await api(`/receiver/donations/${donationId}/requests`, "POST", requestPayload, receiverToken);
  console.log("Create Request HTTP Status:", createReqRes.status);
  if (!createReqRes.ok) {
    console.error("Failed to create request:", createReqRes.data);
    process.exit(1);
  }
  const request = createReqRes.data;
  const requestId = request.id;
  console.log("[OK] Donation Request Created Successfully!");
  console.log("Request ID:", requestId);
  console.log("Requested Quantity:", request.requestedQuantity);
  console.log("Allocated Quantity:", request.allocatedQuantity);
  console.log("Status:", request.status);
  console.log("");

  // ----------------------------------------------------
  // PART 5: DONOR REQUEST REVIEW & APPROVAL
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 5: DONOR REQUEST REVIEW & APPROVAL (Donor)");
  console.log("--------------------------------------------------");
  const donorViewReqsRes = await api(`/donor/donations/${donationId}/requests`, "GET", null, donorToken);
  console.log("Donor List Requests HTTP Status:", donorViewReqsRes.status);
  const donorReqList = donorViewReqsRes.data.content || [];
  const incomingReq = donorReqList.find(r => r.requestId === requestId);
  if (!incomingReq) {
    console.error("Request not visible to donor! Found:", donorReqList);
    process.exit(1);
  }
  console.log("[OK] Incoming request verified by Donor:");
  console.log("Request ID:", incomingReq.requestId);
  console.log("Requested Qty:", incomingReq.requestedQuantity);
  console.log("Allocated Qty:", incomingReq.allocatedQuantity);
  console.log("Status:", incomingReq.status);

  console.log("Approving request...");
  const approveRes = await api(`/donor/requests/${requestId}/approve`, "POST", null, donorToken);
  console.log("Approve Request HTTP Status:", approveRes.status);
  if (!approveRes.ok) {
    console.error("Failed to approve request:", approveRes.data);
    process.exit(1);
  }
  const approveData = approveRes.data;
  console.log("[OK] Request Approved Successfully!");
  console.log("Allocated Quantity for this Request:", approveData.allocatedQuantity);
  console.log("Total Allocated Quantity on Donation:", approveData.totalAllocatedQuantity);
  console.log("Remaining Unallocated Quantity:", approveData.remainingQuantity);
  console.log("Donation Status:", approveData.donationStatus);
  console.log("Request Status:", approveData.requestStatus);
  console.log("");

  // ----------------------------------------------------
  // PART 6: VERIFY INVARIANTS
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 6: INVARIANTS VERIFICATION");
  console.log("--------------------------------------------------");
  console.log(`Invariant 1: request.allocatedQuantity (${approveData.allocatedQuantity}) <= request.requestedQuantity (${incomingReq.requestedQuantity}) ->`, approveData.allocatedQuantity <= incomingReq.requestedQuantity ? "[PASS]" : "[FAIL]");
  console.log(`Invariant 2: donation.allocatedQuantity (${approveData.totalAllocatedQuantity}) <= donation.quantity (40.0) ->`, approveData.totalAllocatedQuantity <= 40.0 ? "[PASS]" : "[FAIL]");
  console.log(`Invariant 3: remainingQuantity = 40 - 15 = 25.0 ->`, approveData.remainingQuantity === 25.0 ? "[PASS]" : "[FAIL]");
  console.log(`Invariant 4: donation remains AVAILABLE due to 25.0 remaining ->`, approveData.donationStatus === "AVAILABLE" ? "[PASS]" : "[FAIL]");
  console.log("");

  // ----------------------------------------------------
  // PART 7: ADMIN ASSIGN VOLUNTEER
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 7: ADMIN ASSIGN VOLUNTEER (Admin)");
  console.log("--------------------------------------------------");
  const volunteerId = "33333333-3333-3333-3333-333333333332"; // Alex Kumar
  const scheduledTime = new Date(now.getTime() + 2 * 60 * 60 * 1000).toISOString(); // 2h ahead, within pickup window

  const assignPayload = {
    donationRequestId: requestId,
    volunteerId: volunteerId,
    scheduledAt: scheduledTime
  };

  const assignRes = await api("/admin/pickups", "POST", assignPayload, adminToken);
  console.log("Assign Pickup HTTP Status:", assignRes.status);
  if (!assignRes.ok) {
    console.error("Failed to assign volunteer:", assignRes.data);
    process.exit(1);
  }
  const pickup = assignRes.data;
  const pickupId = pickup.pickupId || pickup.id;
  const generatedOtp = pickup.devOtpCode;
  console.log("[OK] Volunteer Assigned Successfully!");
  console.log("Pickup ID:", pickupId);
  console.log("Volunteer ID:", pickup.volunteerId);
  console.log("Scheduled At:", pickup.scheduledAt);
  console.log("Status:", pickup.status);
  console.log("Generated Secure OTP (from assign response):", generatedOtp);
  console.log("");

  // ----------------------------------------------------
  // PART 8: VOLUNTEER ACCESS & IDOR SECURITY CHECK
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 8: VOLUNTEER ASSIGNED PICKUPS & IDOR CHECK");
  console.log("--------------------------------------------------");
  const volPickupsRes = await api("/volunteer/pickups", "GET", null, volunteerToken);
  console.log("Volunteer Get Pickups HTTP Status:", volPickupsRes.status);
  const myPickups = volPickupsRes.data.content || [];
  const assignedPickup = myPickups.find(p => (p.pickupId || p.id) === pickupId);
  if (!assignedPickup) {
    console.error("Assigned pickup not in volunteer list! Found:", myPickups);
    process.exit(1);
  }
  console.log("[OK] Assigned pickup verified in volunteer dashboard!");
  console.log("Pickup ID:", assignedPickup.pickupId || assignedPickup.id);
  console.log("Scheduled At:", assignedPickup.scheduledAt);
  console.log("Status:", assignedPickup.status);

  console.log("\n--> Testing IDOR Protection: other volunteer attempts to access this pickup...");
  const idorRes = await api(`/volunteer/pickups/${pickupId}`, "GET", null, otherVolToken);
  console.log("Other Volunteer Access HTTP Status:", idorRes.status);
  if (idorRes.status === 403) {
    console.log("[PASS] IDOR Protection Confirmed: HTTP 403 Forbidden returned when accessing another volunteer's pickup.");
  } else {
    console.error("SECURITY VULNERABILITY: Other volunteer was able to access pickup! Status:", idorRes.status);
    process.exit(1);
  }
  console.log("");

  // ----------------------------------------------------
  // PART 9: OTP VERIFICATION
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 9: OTP VERIFICATION WORKFLOW");
  console.log("--------------------------------------------------");
  console.log("--> Test 9.1: Verifying INCORRECT OTP ('999999')...");
  const wrongOtpRes = await api(`/volunteer/pickups/${pickupId}/verify-otp`, "POST", { otp: "999999" }, volunteerToken);
  console.log("Wrong OTP HTTP Status:", wrongOtpRes.status);
  console.log("Wrong OTP Response Message:", wrongOtpRes.data?.message || wrongOtpRes.data);
  if (wrongOtpRes.status === 400 || wrongOtpRes.status === 409) {
    console.log("[PASS] Incorrect OTP properly rejected with HTTP", wrongOtpRes.status);
  } else {
    console.error("FAILED: Incorrect OTP was not rejected with 400 or 409! Received:", wrongOtpRes.status);
    process.exit(1);
  }

  console.log("\n--> Test 9.2: Verifying CORRECT OTP...");
  const correctOtpRes = await api(`/volunteer/pickups/${pickupId}/verify-otp`, "POST", { otp: generatedOtp }, volunteerToken);
  console.log("Correct OTP HTTP Status:", correctOtpRes.status);
  if (!correctOtpRes.ok) {
    console.error("Failed to verify correct OTP:", correctOtpRes.data);
    process.exit(1);
  }
  const verifiedPickup = correctOtpRes.data;
  console.log("[PASS] Correct OTP Verified Successfully!");
  console.log("Pickup Status:", verifiedPickup.status);
  console.log("Picked Up At:", verifiedPickup.pickedUpAt);

  console.log("\n--> Test 9.3: Attempting to REUSE the same OTP...");
  const reuseOtpRes = await api(`/volunteer/pickups/${pickupId}/verify-otp`, "POST", { otp: generatedOtp }, volunteerToken);
  console.log("Reused OTP HTTP Status:", reuseOtpRes.status);
  if (reuseOtpRes.status === 400 || reuseOtpRes.status === 409 || !reuseOtpRes.ok) {
    console.log("[PASS] Replay Protection Verified: Reused OTP rejected! (Status:", reuseOtpRes.status, ")");
  } else {
    console.error("SECURITY VULNERABILITY: OTP was reused successfully!");
    process.exit(1);
  }

  console.log("\n--> Test 9.4: Verifying raw OTP is NOT exposed on GET endpoint...");
  const detailRes = await api(`/volunteer/pickups/${pickupId}`, "GET", null, volunteerToken);
  const detailObj = detailRes.data;
  const rawOtpExposed = JSON.stringify(detailObj).includes(generatedOtp);
  console.log("Raw OTP in GET response:", rawOtpExposed ? "[EXPOSED - FAIL]" : "[NEVER EXPOSED - PASS]");
  console.log("");

  // ----------------------------------------------------
  // PART 10: COMPLETE PICKUP
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 10: COMPLETE PICKUP (Volunteer)");
  console.log("--------------------------------------------------");
  const completeRes = await api(`/volunteer/pickups/${pickupId}/complete`, "POST", null, volunteerToken);
  console.log("Complete Pickup HTTP Status:", completeRes.status);
  if (!completeRes.ok) {
    console.error("Failed to complete pickup:", completeRes.data);
    process.exit(1);
  }
  const completedPickup = completeRes.data;
  console.log("[OK] Pickup Marked Completed by Volunteer!");
  console.log("Pickup ID:", completedPickup.pickupId || completedPickup.id);
  console.log("Pickup Status:", completedPickup.status);
  console.log("Completed At:", completedPickup.completedAt);
  console.log("");

  // ----------------------------------------------------
  // PART 14: ADDITIONAL SECURITY CHECKS
  // ----------------------------------------------------
  console.log("--------------------------------------------------");
  console.log("PART 14: SECURITY AUDIT CHECKS");
  console.log("--------------------------------------------------");
  console.log("--> Check 1: Unauthenticated request to protected endpoint...");
  const unauthRes = await api("/donor/donations", "GET", null, null);
  console.log("Unauthenticated HTTP Status:", unauthRes.status, unauthRes.status === 401 ? "[PASS 401]" : "[FAIL]");

  console.log("--> Check 2: Non-admin accessing admin endpoint (Donor -> /admin/pickups)...");
  const donorAdminRes = await api("/admin/pickups", "GET", null, donorToken);
  console.log("Donor accessing Admin HTTP Status:", donorAdminRes.status, donorAdminRes.status === 403 ? "[PASS 403]" : "[FAIL]");

  console.log("--> Check 3: Receiver accessing donor-only endpoint (Receiver -> /donor/donations)...");
  const receiverDonorRes = await api("/donor/donations", "POST", donationPayload, receiverToken);
  console.log("Receiver accessing Donor HTTP Status:", receiverDonorRes.status, receiverDonorRes.status === 403 ? "[PASS 403]" : "[FAIL]");

  console.log("\n==================================================");
  console.log("E2E WORKFLOW COMPLETED SUCCESSFULLY!");
  console.log("Fresh Donation ID:", donationId);
  console.log("Fresh Request ID :", requestId);
  console.log("Fresh Pickup ID  :", pickupId);
  console.log("==================================================");
}

run().catch(err => {
  console.error("FATAL ERROR IN E2E WORKFLOW:", err);
  process.exit(1);
});
