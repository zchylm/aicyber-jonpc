package com.aicyber.backend.auth.email;

public interface AccountEmailSender {
    void sendVerification(String email, String displayName, String verificationUrl);

    void sendPasswordReset(String email, String displayName, String resetUrl);
}
