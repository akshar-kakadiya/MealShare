package com.mealshare.user;

public enum UserCategory {
    // Donor categories
    HOTEL_RESTAURANT("Hotel / Restaurant"),
    HOSTEL_MESS("Hostel / Mess"),
    EVENT_ORGANIZER("Event Organizer"),
    HOUSEHOLD("Household"),
    OTHER_BUSINESS("Other Business"),

    // Receiver categories
    NGO("NGO"),
    COMMUNITY_SHELTER("Community Shelter"),
    COMMUNITY_GROUP("Community Group"),
    INDIVIDUAL("Individual");

    private final String displayName;

    UserCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDonorCategory() {
        return this == HOTEL_RESTAURANT ||
               this == HOSTEL_MESS ||
               this == EVENT_ORGANIZER ||
               this == HOUSEHOLD ||
               this == OTHER_BUSINESS;
    }

    public boolean isReceiverCategory() {
        return this == NGO ||
               this == COMMUNITY_SHELTER ||
               this == COMMUNITY_GROUP ||
               this == INDIVIDUAL;
    }
}
