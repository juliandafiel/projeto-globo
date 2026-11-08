package br.com.checkout.bff.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PreCheckoutRequest(
        @NotEmpty @Size(max = 10) List<@Valid ProductRef> products
) {
    public record ProductRef(@NotBlank @Size(max = 60) String slug) {
    }
}
