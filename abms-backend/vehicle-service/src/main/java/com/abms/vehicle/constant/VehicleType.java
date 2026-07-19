package com.abms.vehicle.constant;

import java.util.Set;

public final class VehicleType {

    public static final String CAR = "CAR";
    public static final String MOTORBIKE = "MOTORBIKE";

    private static final Set<String> ALLOWED_TYPES = Set.of(CAR, MOTORBIKE);

    private VehicleType() {
    }

    public static boolean isAllowed(String type) {
        return type != null && ALLOWED_TYPES.contains(type.toUpperCase());
    }
}