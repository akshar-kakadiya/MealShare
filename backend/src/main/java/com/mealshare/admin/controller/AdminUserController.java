package com.mealshare.admin.controller;

import com.mealshare.admin.dto.AdminUserDetailResponse;
import com.mealshare.admin.dto.AdminUserResponse;
import com.mealshare.admin.dto.UpdateUserStatusRequest;
import com.mealshare.admin.service.AdminUserService;
import com.mealshare.common.dto.PaginatedResponse;
import com.mealshare.security.FirebasePrincipal;
import com.mealshare.user.UserRole;
import com.mealshare.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<AdminUserResponse>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<AdminUserResponse> page = adminUserService.getUsers(role, status, search, pageable);
        return ResponseEntity.ok(PaginatedResponse.from(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUserDetail(@PathVariable UUID id) {
        AdminUserDetailResponse response = adminUserService.getUserDetail(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal FirebasePrincipal principal,
            HttpServletRequest httpRequest
    ) {
        AdminUserResponse response = adminUserService.updateUserStatus(id, request, principal.uid(), httpRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal FirebasePrincipal principal,
            HttpServletRequest httpRequest
    ) {
        adminUserService.removeUser(id, principal.uid(), httpRequest);
        return ResponseEntity.noContent().build();
    }
}
