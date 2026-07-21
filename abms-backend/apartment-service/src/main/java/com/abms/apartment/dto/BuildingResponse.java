package com.abms.apartment.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingResponse {

    private UUID buildingId;
    private String name;
    private String code;
    private String address;
    private Integer floors;
}