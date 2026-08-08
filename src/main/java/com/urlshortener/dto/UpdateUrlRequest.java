package com.urlshortener.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateUrlRequest {

    @Size(max = 255)
    private String title;

    private Boolean active;

    private LocalDateTime expiresAt;

    @Min(1)
    private Long maxClicks;

    /** Send an empty string to remove password protection, null to leave unchanged. */
    private String password;
}
