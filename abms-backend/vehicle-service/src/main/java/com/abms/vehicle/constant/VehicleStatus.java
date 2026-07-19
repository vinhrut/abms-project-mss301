package com.abms.vehicle.constant;

public final class VehicleStatus {

    public static final String PENDING = "PENDING";
    public static final String PENDING_CANCEL = "PENDING_CANCEL";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String INACTIVE = "INACTIVE";

    private VehicleStatus() {
    }

    public static boolean isFinalStatus(String status) {
        return APPROVED.equalsIgnoreCase(status)
                || REJECTED.equalsIgnoreCase(status)
                || CANCELLED.equalsIgnoreCase(status)
                || INACTIVE.equalsIgnoreCase(status);
    }
}