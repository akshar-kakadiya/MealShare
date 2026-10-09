package com.mealshare.pickup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;

public record FoodInspectionDto(
        @NotNull(message = "Field 'accepted' is required") Boolean accepted,
        @JsonAlias({"inspectionNotes", "inspection_notes"}) String notes,
        String rejectionReason,
        String photoUrl
) {
    public FoodInspectionDto(Boolean accepted, String notes, String inspectionNotes, String rejectionReason, String photoUrl) {
        this(
                accepted,
                inspectionNotes != null && !inspectionNotes.isBlank() ? inspectionNotes : notes,
                rejectionReason,
                photoUrl
        );
    }

    public String effectiveNotes() {
        if (notes != null && !notes.isBlank()) return notes.trim();
        return null;
    }

    public String effectiveRejectionReason() {
        if (rejectionReason != null && !rejectionReason.isBlank()) return rejectionReason.trim();
        return effectiveNotes();
    }
}
