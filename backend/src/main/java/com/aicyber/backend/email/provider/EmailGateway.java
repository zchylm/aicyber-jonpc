package com.aicyber.backend.email.provider;

import com.aicyber.backend.email.model.QueuedEmail;

public interface EmailGateway {
    String send(QueuedEmail email);
}

