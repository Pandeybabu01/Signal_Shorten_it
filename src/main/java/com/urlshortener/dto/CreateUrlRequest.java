package com.urlshortener.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateUrlRequest {

    @NotBlank(message = "originalUrl must not be blank")
    @Size(max = 2048, message = "originalUrl must be at most 2048 characters")
    @Pattern(
        regexp = "^(https?)://[^\\s]+$",
        message = "originalUrl must be a valid http(s) URL"
    )
    private String originalUrl;

    @Pattern(regexp = "^[a-zA-Z0-9_-]{3,20}$", message = "customAlias must be 3-20 alphanumeric characters, - or _")
    private String customAlias;

    @Size(max = 255)
    private String title;

    /** Optional - if set, visitors must supply this password before redirect. */
    @Size(min = 4, max = 72, message = "password must be between 4 and 72 characters")
    private String password;

    /** Optional expiry timestamp. Null = never expires. */
    private LocalDateTime expiresAt;

    /** Optional cap on number of redirects before the link auto-deactivates. */
    @Min(value = 1, message = "maxClicks must be at least 1")
    private Long maxClicks;
}
