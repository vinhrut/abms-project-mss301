package com.abms.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitMaintenanceRequest {

    private UUID senderId;

    @NotBlank(message = "category is required")
    @Size(max = 50)
    private String category;

    @NotBlank(message = "priority is required")
    @Size(max = 30)
    private String priority;

    @NotBlank(message = "title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;
}
