package com.aicyber.backend.email.model;

public record EmailAttachment(String filename, String contentType, byte[] content) {
    public EmailAttachment {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}

