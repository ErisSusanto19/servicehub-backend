package com.eris.servicehub.dtos.ordernote;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderNoteRequest(
        @NotBlank(message = "Content cannot be blank")
        @Size(max = 2000, message = "Note content cannot exceed 2000 characters")
        String content
) {}