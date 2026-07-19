package com.abms.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignStaffRequest {

    @NotNull(message = "technicianId is required")
    private UUID technicianId;
}
