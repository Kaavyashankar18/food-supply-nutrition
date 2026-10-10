package com.foodsupply.distribution.dto;

import com.foodsupply.distribution.entity.CentreStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CentreRequest {

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be at most 150 characters")
    private String name;

    @Size(max = 300, message = "address must be at most 300 characters")
    private String address;

    @Size(max = 100, message = "contactPerson must be at most 100 characters")
    private String contactPerson;

    @Size(max = 20, message = "phone must be at most 20 characters")
    private String phone;

    private CentreStatus status;
}
