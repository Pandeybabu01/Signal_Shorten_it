package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Used to submit a password for password-protected short links. */
@Data
public class RedirectAccessRequest {
    @NotBlank
    private String password;
}
