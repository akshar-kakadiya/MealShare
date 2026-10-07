# MealShare API Inventory

> **Document Version**: 1.0  
> **Date**: September 26, 2026  
> **Milestone**: CAPSTONE-01 / CAPSTONE-02  
> **Total Endpoints**: 47 (across 18 REST controllers)  
> **Base URL**: `http://localhost:8081/api/v1`  
> **Security Protocol**: `Authorization: Bearer <Firebase_ID_Token>`  

---

## 1. Authentication & User Management

| HTTP Method | Route | Authorized Role | Purpose | Request Body | Response Purpose | Mutation / Read-Only |
|:------------|:------|:----------------|:--------|:-------------|:-----------------|:---------------------|
| `GET` | `/api/v1/health` | Public | System liveness and health probe | None | Returns service status (`UP`), service name, and server timestamp | Read-Only |
| `POST` | `/api/v1/users/register` | Authenticated Firebase User (`ROLE_UNREGISTERED`) | Creates a MealShare user profile for a verified Firebase user | `RegistrationRequest`<br>`{ "email": string, "name": string, "phone": string, "role": string }` | Returns created `UserResponse` with generated UUID and assigned status | **Mutation** |
| `GET` | `/api/v1/users/me` | Registered User (`ROLE_USER`) | Resolves current authenticated user from verified Firebase UID | None | Returns `UserResponse` (`id`, `firebaseUid`, `email`, `role`, `status`) | Read-Only |

---

## 2. Donor Endpoints

| HTTP Method | Route | Authorized Role | Purpose | Request Body | Response Purpose | Mutation / Read-Only |
|:------------|:------|:----------------|:--------|:-------------|:-----------------|:---------------------|
| `GET` | `/api/v1/donor/profile` | `ROLE_DONOR` | Retrieves donor organization/individual profile | None | Returns `DonorProfileResponse` | Read-Only |
| `PUT` | `/api/v1/donor/profile` | `ROLE_DONOR` | Updates donor profile details and default coordinates | `DonorProfileRequest`<br>`{ "entityName": string, "donorType": string, "addressText": string, "latitude": number, "longitude": number }` | Returns updated `DonorProfileResponse` | **Mutation** |
| `POST` | `/api/v1/donor/donations` | `ROLE_DONOR` | Publishes a surplus food donation listing with mandatory food safety declaration | `CreateDonationRequest`<br>`{ "title": string, "description": string, "foodCategory": enum, "quantity": number, "quantityUnit": string, "preparationDateTime": ISO8601, "consumeBefore": ISO8601, "pickupAvailableFrom": ISO8601, "pickupAvailableUntil": ISO8601, "pickupAddress": string, "latitude": number, "longitude": number, "foodSafetyDeclared": boolean, "foodSafetyDeclarationText": string }` | Returns created `DonationResponse` (`status` = `AVAILABLE`) | **Mutation** |
| `GET` | `/api/v1/donor/donations` | `ROLE_DONOR` | Paginated listing of donations created by the authenticated donor | Query params: `page`, `size`, `sort` | Returns `PaginatedResponse<DonationResponse>` | Read-Only |
| `GET` | `/api/v1/donor/donations/{id}` | `ROLE_DONOR` | Retrieves full details of a specific donation owned by the donor | None | Returns `DonationResponse` | Read-Only |
| `PATCH` | `/api/v1/donor/donations/{id}` | `ROLE_DONOR` | Updates editable fields of an active `AVAILABLE` donation | `UpdateDonationRequest` (editable title, description, quantity, timestamps, location) | Returns updated `DonationResponse` | **Mutation** |
| `POST` | `/api/v1/donor/donations/{id}/cancel` | `ROLE_DONOR` | Cancels an active `AVAILABLE` donation and notifies affected receivers | None | Returns updated `DonationResponse` (`status` = `CANCELLED`) | **Mutation** |
| `GET` | `/api/v1/donor/donations/{donationId}/requests` | `ROLE_DONOR` | Lists incoming receiver claims for a donation in strict FCFS order | Query params: `page`, `size` | Returns `PaginatedResponse<DonorDonationRequestResponse>` ordered by `requestedAt ASC` | Read-Only |
| `POST` | `/api/v1/donor/requests/{requestId}/approve` | `ROLE_DONOR` | Approves receiver request and atomically locks and increments allocated quantity | None | Returns `ApproveDonationRequestResponse` (`remainingDonationQuantity`, `allocatedQuantity`, `requestStatus`) | **Mutation** |
| `POST` | `/api/v1/donor/requests/{requestId}/reject` | `ROLE_DONOR` | Rejects a pending receiver claim | `RejectDonationRequestDto`<br>`{ "reason": string }` | Returns `DonorDonationRequestResponse` (`status` = `REJECTED`) | **Mutation** |

---

## 3. Receiver Endpoints

| HTTP Method | Route | Authorized Role | Purpose | Request Body | Response Purpose | Mutation / Read-Only |
|:------------|:------|:----------------|:--------|:-------------|:-----------------|:---------------------|
| `GET` | `/api/v1/receiver/profile` | `ROLE_RECEIVER_ORG`, `ROLE_RECEIVER_VOL` | Retrieves receiver organization profile | None | Returns `ReceiverOrganizationResponse` | Read-Only |
| `PUT` | `/api/v1/receiver/profile` | `ROLE_RECEIVER_ORG`, `ROLE_RECEIVER_VOL` | Updates organization address, coordinates, and beneficiary capacity | `ReceiverOrganizationRequest`<br>`{ "orgType": string, "orgName": string, "registrationNumber": string, "addressText": string, "serviceLatitude": number, "serviceLongitude": number, "serviceRadiusMeters": number, "beneficiaryCapacity": number }` | Returns updated `ReceiverOrganizationResponse` | **Mutation** |
| `GET` | `/api/v1/receiver/donations/nearby` | `ROLE_RECEIVER_ORG`, `ROLE_RECEIVER_VOL` | PostGIS spatial discovery of active available non-expired donations | Query params: `latitude` (req), `longitude` (req), `radiusKm` (req), `foodCategory` (opt), `page`, `size` | Returns `PaginatedResponse<NearbyDonationResponse>` including computed geodesic `distanceKm` | Read-Only |
| `GET` | `/api/v1/receiver/donations/{id}` | `ROLE_RECEIVER_ORG`, `ROLE_RECEIVER_VOL` | Inspects donation details, donor organization, and food safety metadata | None | Returns `DonationResponse` | Read-Only |
| `POST` | `/api/v1/receiver/donations/{donationId}/requests` | `ROLE_RECEIVER_ORG` | Submits a formal claim request for surplus food | `CreateDonationRequestDto`<br>`{ "requestedQuantity": number, "requestReason": string }` | Returns created `DonationRequestResponse` (`status` = `PENDING`) | **Mutation** |
| `GET` | `/api/v1/receiver/requests` | `ROLE_RECEIVER_ORG` | Lists all donation requests submitted by the authenticated organization | Query params: `page`, `size` | Returns `PaginatedResponse<DonationRequestResponse>` | Read-Only |
| `GET` | `/api/v1/receiver/requests/{id}` | `ROLE_RECEIVER_ORG` | Retrieves details and current status of an organization request | None | Returns `DonationRequestResponse` | Read-Only |
| `POST` | `/api/v1/receiver/requests/{id}/withdraw` | `ROLE_RECEIVER_ORG` | Withdraws an active `PENDING` request before donor adjudication | None | Returns `DonationRequestResponse` (`status` = `WITHDRAWN`) | **Mutation** |

---

## 4. Volunteer Endpoints

| HTTP Method | Route | Authorized Role | Purpose | Request Body | Response Purpose | Mutation / Read-Only |
|:------------|:------|:----------------|:--------|:-------------|:-----------------|:---------------------|
| `GET` | `/api/v1/volunteer/pickups` | `ROLE_RECEIVER_VOL` | Lists assigned pickups for authenticated volunteer | Query params: `page`, `size` | Returns `PaginatedResponse<PickupResponse>` | Read-Only |
| `GET` | `/api/v1/volunteer/pickups/{pickupId}` | `ROLE_RECEIVER_VOL` | Retrieves full pickup and address details for assigned task (enforces IDOR) | None | Returns `PickupResponse` | Read-Only |
| `POST` | `/api/v1/volunteer/pickups/{pickupId}/verify-otp` | `ROLE_RECEIVER_VOL` | Verifies donor handover 6-digit numeric OTP against BCrypt hash | `VerifyOtpRequest`<br>`{ "otp": string }` | Returns updated `PickupResponse` (`status` = `PICKED_UP`, `otpVerifiedAt`) | **Mutation** |
| `POST` | `/api/v1/volunteer/pickups/{pickupId}/complete` | `ROLE_RECEIVER_VOL` | Marks delivery complete upon handing food to receiver organization | None | Returns updated `PickupResponse` (`status` = `COMPLETED`, `completedAt`) | **Mutation** |

---

## 5. Admin Endpoints

| HTTP Method | Route | Authorized Role | Purpose | Request Body | Response Purpose | Mutation / Read-Only |
|:------------|:------|:----------------|:--------|:-------------|:-----------------|:---------------------|
| `GET` | `/api/v1/admin/dashboard` | `ROLE_ADMIN` | Overall platform KPIs, active counter totals, and 7-day activity feed | None | Returns `AdminDashboardResponse` | Read-Only |
| `GET` | `/api/v1/admin/users` | `ROLE_ADMIN` | Paginated listing of users with search and role/status filtering | Query params: `role`, `status`, `search`, `page`, `size` | Returns `PaginatedResponse<AdminUserResponse>` | Read-Only |
| `GET` | `/api/v1/admin/users/{id}` | `ROLE_ADMIN` | Deep inspection of user account with role-specific profile details | None | Returns `AdminUserDetailResponse` | Read-Only |
| `PATCH` | `/api/v1/admin/users/{id}/status` | `ROLE_ADMIN` | Adjudicates user account status (`ACTIVE`, `INACTIVE`, `RESTRICTED`, `SUSPENDED`) | `UpdateUserStatusRequest`<br>`{ "status": enum, "reason": string }` | Returns updated `AdminUserResponse` | **Mutation** |
| `GET` | `/api/v1/admin/verification` | `ROLE_ADMIN` | Review queue of receiver organizations seeking verification | Query params: `status`, `search`, `page`, `size` | Returns `PaginatedResponse<AdminVerificationResponse>` | Read-Only |
| `GET` | `/api/v1/admin/verification/{id}` | `ROLE_ADMIN` | Inspection of organization registration documents and profile | None | Returns `AdminVerificationResponse` | Read-Only |
| `PATCH` | `/api/v1/admin/verification/{id}/status` | `ROLE_ADMIN` | Approves or rejects organization institutional verification | `UpdateVerificationStatusRequest`<br>`{ "status": string, "notes": string }` | Returns updated `AdminVerificationResponse` | **Mutation** |
| `GET` | `/api/v1/admin/donations` | `ROLE_ADMIN` | Comprehensive operational listing of all platform donations | Query params: `status`, `category`, `donorId`, `search`, `page`, `size` | Returns `PaginatedResponse<AdminDonationResponse>` | Read-Only |
| `GET` | `/api/v1/admin/donations/{id}` | `ROLE_ADMIN` | Complete detail of donation, remaining balance, and requests | None | Returns `AdminDonationDetailResponse` | Read-Only |
| `GET` | `/api/v1/admin/requests` | `ROLE_ADMIN` | Operational oversight of all donation claim requests | Query params: `status`, `donationId`, `receiverOrgId`, `donorId`, `search`, `page`, `size` | Returns `PaginatedResponse<AdminDonationRequestResponse>` | Read-Only |
| `GET` | `/api/v1/admin/requests/{id}` | `ROLE_ADMIN` | Complete detail of donation request, receiver org, and donor | None | Returns `AdminDonationRequestDetailResponse` | Read-Only |
| `POST` | `/api/v1/admin/pickups` | `ROLE_ADMIN` | Assigns an available volunteer to an approved donation claim; generates OTP | `AssignPickupRequest`<br>`{ "donationRequestId": UUID, "volunteerId": UUID, "scheduledAt": ISO8601 }` | Returns `AdminAssignPickupResponse` (includes generated `otpCode` shown once to admin) | **Mutation** |
| `GET` | `/api/v1/admin/pickups` | `ROLE_ADMIN` | Operational list of pickups across all volunteers | Query params: `status`, `page`, `size` | Returns `PaginatedResponse<PickupResponse>` | Read-Only |
| `GET` | `/api/v1/admin/pickups/{pickupId}` | `ROLE_ADMIN` | Complete pickup delivery detail, timestamps, and attempt counts | None | Returns `PickupResponse` | Read-Only |
| `GET` | `/api/v1/admin/reports/summary` | `ROLE_ADMIN` | Aggregated delivery volume, category distribution, and impact trends | Query param: `timeRange` (`7d`, `30d`, `90d`, `1y`) | Returns `AdminReportSummaryResponse` | Read-Only |
| `GET` | `/api/v1/admin/audit-logs` | `ROLE_ADMIN` | Filterable immutable system audit trail | Query params: `actionType`, `actorUserId`, `entityName`, `page`, `size` | Returns `PaginatedResponse<AdminAuditLogResponse>` | Read-Only |

---

## 6. Notifications Endpoints

| HTTP Method | Route | Authorized Role | Purpose | Request Body | Response Purpose | Mutation / Read-Only |
|:------------|:------|:----------------|:--------|:-------------|:-----------------|:---------------------|
| `GET` | `/api/v1/notifications` | Authenticated User | Retrieves user's in-app notifications sorted newest first | Query params: `page`, `size`, `sort` | Returns `PaginatedResponse<NotificationResponse>` | Read-Only |
| `GET` | `/api/v1/notifications/unread-count` | Authenticated User | Returns count of unread notifications | None | Returns `UnreadCountResponse` (`{ "count": number }`) | Read-Only |
| `PATCH` | `/api/v1/notifications/{id}/read` | Authenticated User | Marks a single notification as read (enforces IDOR ownership) | None | Returns `NotificationResponse` (`isRead` = `true`) | **Mutation** |
| `POST` | `/api/v1/notifications/read-all` | Authenticated User | Marks all notifications for authenticated user as read | None | Returns `MarkAllReadResponse` (`{ "markedCount": number }`) | **Mutation** |
| `POST` | `/api/v1/notifications/device-token` | Authenticated User | Registers or updates a client FCM device token for push notifications | `DeviceTokenRequest`<br>`{ "token": string, "platform": string }` | Returns `HTTP 200 OK` (token persisted to database) | **Mutation** |
| `DELETE` | `/api/v1/notifications/device-token` | Authenticated User | Unregisters an FCM device token upon client logout | `DeviceTokenRequest`<br>`{ "token": string, "platform": string }` | Returns `HTTP 204 No Content` | **Mutation** |
