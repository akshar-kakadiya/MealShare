package com.mealshare.notification;

import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.notification.dto.DeviceTokenRequest;
import com.mealshare.notification.dto.MarkAllReadResponse;
import com.mealshare.notification.dto.NotificationResponse;
import com.mealshare.notification.dto.UnreadCountResponse;
import com.mealshare.security.FirebasePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Endpoints for retrieving and managing in-app notifications and push device tokens")
@SecurityRequirement(name = "firebase-auth")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List my notifications", description = "Retrieves a paginated list of notifications for the authenticated user, sorted newest first.")
    public ResponseEntity<PaginatedResponse<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponse> page = notificationService.getMyNotifications(principal.uid(), pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count", description = "Returns the total number of unread notifications for the authenticated user.")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal FirebasePrincipal principal) {
        UnreadCountResponse response = notificationService.getUnreadCount(principal.uid());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read. Enforces IDOR protection.")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @PathVariable UUID id) {
        NotificationResponse response = notificationService.markAsRead(principal.uid(), id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications for the authenticated user as read.")
    public ResponseEntity<MarkAllReadResponse> markAllAsRead(
            @AuthenticationPrincipal FirebasePrincipal principal) {
        MarkAllReadResponse response = notificationService.markAllAsRead(principal.uid());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/device-token")
    @Operation(summary = "Register FCM device token", description = "Registers or updates a Firebase Cloud Messaging device token for push notifications.")
    public ResponseEntity<Void> registerDeviceToken(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody DeviceTokenRequest request) {
        notificationService.registerDeviceToken(principal.uid(), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/device-token")
    @Operation(summary = "Unregister FCM device token", description = "Removes a registered device token for the authenticated user.")
    public ResponseEntity<Void> unregisterDeviceToken(
            @AuthenticationPrincipal FirebasePrincipal principal,
            @Valid @RequestBody DeviceTokenRequest request) {
        notificationService.unregisterDeviceToken(principal.uid(), request.token());
        return ResponseEntity.noContent().build();
    }
}
