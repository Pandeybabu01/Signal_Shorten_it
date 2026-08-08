package com.urlshortener.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/legal")
@Tag(name = "Legal", description = "Privacy policy and terms of service")
public class LegalController {

    @GetMapping(value = "/privacy-policy", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(summary = "Get the current privacy policy (Markdown)")
    public ResponseEntity<String> privacyPolicy() throws IOException {
        ClassPathResource resource = new ClassPathResource("legal/PRIVACY_POLICY.md");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(content);
    }

    @GetMapping(value = "/terms", produces = MediaType.TEXT_MARKDOWN_VALUE)
    @Operation(summary = "Get the current terms of service (Markdown)")
    public ResponseEntity<String> terms() throws IOException {
        ClassPathResource resource = new ClassPathResource("legal/TERMS_OF_SERVICE.md");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        return ResponseEntity.ok(content);
    }
}
