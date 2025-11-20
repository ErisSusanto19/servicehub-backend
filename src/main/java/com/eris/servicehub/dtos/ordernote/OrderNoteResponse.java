package com.eris.servicehub.dtos.ordernote;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OrderNoteResponse {
    private UUID id;
    private String content;
    private AuthorSummary author;
    private Instant createdAt;

    @Data
    @Builder
    public static class AuthorSummary {
        private UUID id;
        private String name;
    }
}