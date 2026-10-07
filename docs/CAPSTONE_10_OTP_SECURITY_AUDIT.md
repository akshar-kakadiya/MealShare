# CAPSTONE-10: Live OTP Secrecy & Pickup Security Audit Report

**Project**: MealShare  
**Milestone**: CAPSTONE-10  
**Date**: October 6, 2026  
**Status**: Verified & Hardened  

---

## 1. Executive Summary

This audit evaluated the secrecy, distribution, and lifecycle of the 6-digit pickup handover One-Time Password (OTP) across the MealShare platform.

### Core Objective
Ensure that the food handover OTP is strictly confidential, held exclusively by the **authorized Donor**, and verified by the **Receiver** upon physical food collection, with **zero leakage** to Admin web interfaces, API logs, database storage, or unauthorized clients.

---

## 2. Handover Architecture & Role Workflow

MealShare operates under a 3-role operational model: **DONOR**, **RECEIVER**, and **ADMIN**.

```
+--------------------------------------------------------------------------------------------------+
|                                    HANDOVER OTP LIFECYCLE                                        |
+--------------------------------------------------------------------------------------------------+
                                                                                                    
 [1] Admin schedules pickup                                                                         
     POST /api/v1/admin/pickups                                                                    
            |                                                                                      
            v                                                                                      
     Backend generates 6-digit OTP (SecureRandom)                                                  
            |                                                                                      
            +---> BCrypt hash (strength 12) ------> Persisted in PostgreSQL (pickups.otp_hash)      
            |                                                                                      
            +---> Ephemeral in-memory cache ------> ConcurrentHashMap with 10-minute TTL           
            |                                       (NEVER stored in plaintext in PostgreSQL)      
            v                                                                                      
     Admin response returns metadata ONLY (pickupId, scheduledAt, receiverId)                     
     *** devOtpCode REMOVED — Zero secrets exposed to Admin ***                                     
                                                                                                    
 [2] Donor views Handover OTP                                                                       
     GET /api/v1/donor/pickups/{id}/otp (ROLE_DONOR + Ownership check)                             
            |                                                                                      
            +---> Validates: requester is the authentic donation owner                              
            +---> Validates: pickup status is ASSIGNED (not yet picked up)                         
            +---> Validates: OTP has not expired                                                   
            +---> Audit log recorded: OTP_VIEWED (no OTP in log)                                   
            v                                                                                      
     Donor displays 6-digit code in Flutter Mobile UI                                              
                                                                                                    
 [3] Physical Handover & Receiver Verification                                                     
     Receiver physically arrives at Donor facility                                                  
     Donor communicates OTP to Receiver                                                            
     Receiver submits: POST /api/v1/receiver/pickups/{id}/verify-otp                               
            |                                                                                      
            +---> BCrypt comparison against PostgreSQL otp_hash                                    
            +---> Cache evicted: pickupOtpService.invalidateOtp(pickupId)                          
            +---> Status transition: ASSIGNED -> PICKED_UP                                         
            v                                                                                      
     Subsequent OTP requests return HTTP 409 / 400 (Single-use enforcement)                        
                                                                                                    
 [4] Completion                                                                                     
     Receiver confirms delivery: POST /api/v1/receiver/pickups/{id}/complete                       
     Status transition: PICKED_UP -> COMPLETED                                                     
```

---

## 3. Security Hardening & Implementation Changes

### 3.1 Removal of Plaintext OTP from Admin Responses
* **File**: `backend/src/main/java/com/mealshare/admin/dto/AdminAssignPickupResponse.java`
* **Change**: Completely removed `devOtpCode` and any plaintext OTP or hash fields. The response record now contains only non-sensitive scheduling metadata:
  ```java
  public record AdminAssignPickupResponse(
          UUID pickupId,
          UUID donationRequestId,
          UUID donationId,
          UUID receiverId,
          String receiverName,
          PickupStatus status,
          Instant scheduledAt,
          Instant assignedAt,
          Instant otpExpiresAt
  )
  ```
* **Frontend**: `admin-web/src/pages/PickupsPage.tsx`
  * Removed `devOtpCode` extraction from the scheduling handler.
  * Table column relabeled to `Handover OTP` displaying a `Confidential` badge.
  * Details modal displays: `Handover OTP: Held exclusively by Donor`.

### 3.2 Ephemeral In-Memory Storage for Plaintext OTP
* **File**: `backend/src/main/java/com/mealshare/pickup/otp/PickupOtpService.java`
* **Design**: Plaintext OTP is **never** written to PostgreSQL tables. It is stored exclusively in a thread-safe `ConcurrentHashMap<UUID, CachedOtp>` with TTL verification.
* **Eviction Policies**:
  * Single-use eviction immediately upon successful OTP verification (`invalidateOtp`).
  * Explicit eviction upon completion (`completePickup`).
  * Automatic expiration when current time exceeds `otpExpiresAt`.

### 3.3 Authenticated Donor Retrieval Endpoint
* **File**: `backend/src/main/java/com/mealshare/donor/DonorPickupController.java`
* **Base Path**: `/api/v1/donor/pickups` guarded by `@PreAuthorize("hasRole('DONOR')")`
* **Endpoints**:
  * `GET /api/v1/donor/pickups`: Paginated list of pickups for the authenticated donor.
  * `GET /api/v1/donor/pickups/by-donation/{donationId}`: List pickups associated with a specific donation owned by the donor.
  * `GET /api/v1/donor/pickups/{pickupId}`: Pickup detail view.
  * `GET /api/v1/donor/pickups/{pickupId}/otp`: Secure endpoint to retrieve the active handover OTP.
* **Authorization & Validation Controls**:
  1. **Authentication**: Must present valid Firebase ID token mapped to an active user.
  2. **Role Check**: Must possess `ROLE_DONOR`. Receivers and Admins are rejected with `HTTP 403 Forbidden`.
  3. **IDOR / Ownership Check**: Verifies `pickup.getDonationRequest().getDonation().getDonor().getId().equals(donorProfile.getId())`. Unrelated donors are rejected with `HTTP 403 Forbidden`.
  4. **Status Guard**: Pickups in `PICKED_UP`, `COMPLETED`, or `CANCELLED` status cannot return an OTP (returns `HTTP 400 Bad Request`).
  5. **Expiry Guard**: Returns `HTTP 400 Bad Request` if OTP TTL has lapsed.
  6. **Audit Trail**: Writes `OTP_VIEWED` event with `actor_role = DONOR`, `entity_id = pickupId`, without logging the plaintext code.

### 3.4 Mobile Donor UI Integration
* **File**: `mobile/lib/donor/donation_details_screen.dart`
* **UI Features**:
  * Added "Scheduled Pickups & Handover OTP" card section for active donations.
  * Displays scheduled time and assigned receiver organization name.
  * "View Handover OTP" action launches a confirmation dialog displaying the 6-digit code with security instructions: *"Provide this code to the receiver representative upon physical food collection."*
  * Once the status becomes `PICKED_UP` or `COMPLETED`, the OTP button is disabled and replaced with status confirmation.

---

## 4. Verification and Audit Matrix

| Security Check | Expected Behavior | Verification Mechanism | Result |
|---|---|---|---|
| Admin API Response Confidentiality | No `devOtpCode`, `otp`, or `otpHash` returned in `POST /api/v1/admin/pickups` | `PickupSecurityAndApiTest`, `execute_live_workflow.py` | PASS |
| Database Secrecy | Only `otp_hash` (BCrypt string) stored in PostgreSQL `pickups` table | SQL inspection & unit tests | PASS |
| Admin Access Control to Donor OTP | Admin calling `GET /api/v1/donor/pickups/{id}/otp` receives `HTTP 403 Forbidden` | `PickupSecurityAndApiTest`, `execute_live_workflow.py` | PASS |
| Receiver Access Control to Donor OTP | Receiver calling `GET /api/v1/donor/pickups/{id}/otp` receives `HTTP 403 Forbidden` | `PickupSecurityAndApiTest`, `execute_live_workflow.py` | PASS |
| IDOR Protection (Unrelated Donor) | Different donor calling `GET /api/v1/donor/pickups/{id}/otp` receives `HTTP 403 Forbidden` | `PickupSecurityAndApiTest` | PASS |
| Unauthenticated Access Control | Unauthenticated request receives `HTTP 401 Unauthorized` | `PickupSecurityAndApiTest`, `execute_live_workflow.py` | PASS |
| Receiver Pickup Summary Confidentiality | `GET /api/v1/receiver/pickups` contains zero OTP fields | `execute_live_workflow.py` | PASS |
| Wrong OTP Rejection | Entering invalid code returns `HTTP 409 Conflict` | `execute_live_workflow.py`, `DockerPostgresE2ETest` | PASS |
| Correct OTP Acceptance | Valid code sets status to `PICKED_UP` with timestamp | `execute_live_workflow.py`, `DockerPostgresE2ETest` | PASS |
| Single-Use Cache Eviction | OTP endpoint returns error once pickup is `PICKED_UP` | `execute_live_workflow.py` | PASS |
| Replay Prevention | Re-verifying OTP after pickup returns `HTTP 409 Conflict` | `execute_live_workflow.py`, `DockerPostgresE2ETest` | PASS |

---

## 5. Test Suite Execution Summary

### 5.1 Backend Test Suite (Maven)
* **Command**: `.\mvnw.cmd test`
* **Result**: **223 tests run, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESS in 55.4s)
* **Key Test Classes**:
  * `PickupSecurityAndApiTest`: 16/16 passed
  * `PickupServiceTest`: 30/30 passed
  * `PickupOtpServiceTest`: 4/4 passed
  * `DockerPostgresE2ETest`: 10/10 passed
  * `MealShareE2ETest`: 19/19 passed

### 5.2 Live End-to-End Workflow Execution
* **Script**: `execute_live_workflow.py`
* **Status**: **14 out of 14 workflow phases passed with zero failures**
  1. Genuine Firebase token acquisition (Donor, Receiver, Admin, Other Receiver)
  2. Fresh surplus food donation creation (40 meals)
  3. PostGIS spatial discovery within 15 km
  4. Receiver request submission (15 meals)
  5. Donor approval with atomic inventory deduction
  6. Admin scheduling without secret leakage
  7. Handover OTP access control verification (403 for Receiver, 403 for Admin, 401 for Unauth, 200 for Donor)
  8. Receiver scheduled pickups check without OTP exposure
  9. IDOR protection check for unassigned receiver (403)
  10. Invalid OTP rejection (409)
  11. Valid OTP verification (`PICKED_UP`)
  12. Replay prevention and single-use cache eviction check
  13. Food delivery completion (`COMPLETED`)
  14. Negative authorization checks (401, 403)

### 5.3 Web Admin Frontend Build
* **Command**: `npm run build` in `admin-web/`
* **Result**: `tsc -b && vite build` completed in 1.30s with zero errors.

### 5.4 Mobile Client Analysis & Tests
* **Commands**: `flutter analyze` and `flutter test` in `mobile/`
* **Result**:
  * `flutter analyze`: **No issues found!**
  * `flutter test`: **6/6 tests passed!**

---

## 6. Operational Scope and Architectural Notes

1. **Ephemeral Cache Scope**: Plaintext OTP is held in JVM memory using `ConcurrentHashMap`. If the backend application restarts while a pickup is scheduled but before physical verification occurs, the in-memory cache is cleared, although the BCrypt hash in PostgreSQL remains intact. For a clustered multi-node deployment, an encrypted Redis cache with TTL would serve as the distributed cache layer.
2. **Notification Channel**: The OTP is currently retrieved on-demand via the authenticated Donor mobile interface. In a production environment with active SMS or push infrastructure, the OTP could additionally be dispatched to the verified phone number or device token of the primary donor contact.
