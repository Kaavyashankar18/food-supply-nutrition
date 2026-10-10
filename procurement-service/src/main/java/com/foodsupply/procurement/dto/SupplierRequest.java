package com.foodsupply.procurement.dto;

import com.foodsupply.procurement.entity.SupplierStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequest {

    @NotBlank(message = "Supplier name is required")
    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @Size(max = 100, message = "Contact person must be at most 100 characters")
    private String contactPerson;

    @Pattern(regexp = "^[0-9+()\\-\\s]{7,30}$", message = "Phone must be 7-30 characters (digits, +, -, spaces, brackets)")
    private String phone;

    @Email(message = "Email must be a valid address")
    @Size(max = 120, message = "Email must be at most 120 characters")
    private String email;

    @Size(max = 300, message = "Address must be at most 300 characters")
    private String address;

    private SupplierStatus status;

    @DecimalMin(value = "0.0", message = "Rating must be between 0 and 5")
    @DecimalMax(value = "5.0", message = "Rating must be between 0 and 5")
    @Digits(integer = 1, fraction = 1, message = "Rating allows one decimal place")
    private BigDecimal rating;
}
