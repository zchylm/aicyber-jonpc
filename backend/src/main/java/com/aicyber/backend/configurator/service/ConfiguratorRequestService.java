package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorBuildResponse;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;
import com.aicyber.backend.email.service.CustomerEmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class ConfiguratorRequestService {

    private final ConfiguratorQuoteService quoteService;
    private final BuildRequestStore requestStore;
    private final CustomerEmailService customerEmails;

    public ConfiguratorRequestService(ConfiguratorQuoteService quoteService, BuildRequestStore requestStore,
                                      CustomerEmailService customerEmails) {
        this.quoteService = quoteService;
        this.requestStore = requestStore;
        this.customerEmails = customerEmails;
    }

    public ConfiguratorBuildResponse submit(ConfiguratorBuildRequest request) {
        return submit(request, null);
    }

    @Transactional
    public ConfiguratorBuildResponse submit(ConfiguratorBuildRequest request, UUID userId) {
        validateContactDetails(request);
        ConfiguratorQuoteResponse quote = quoteService.quote(request.configuration());
        if (!quote.compatible()) {
            throw new IllegalArgumentException("Build request contains incompatible component selections");
        }

        String directionCode = request.configuration().direction().substring(0,
                Math.min(3, request.configuration().direction().length())).toUpperCase(Locale.ROOT);
        String reference = "JON-" + directionCode
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        UUID requestId = requestStore.create(userId, request, quote, reference);
        customerEmails.buildReceived(requestId, request, reference);
        return new ConfiguratorBuildResponse(reference, "RECEIVED", quote,
                "Your configuration has been received. A JON. PC specialist will confirm availability and the final quote.");
    }

    private void validateContactDetails(ConfiguratorBuildRequest request) {
        if (request == null || request.configuration() == null) {
            throw new IllegalArgumentException("Build request must include a configuration");
        }
        if (isBlank(request.name()) || isBlank(request.email()) || isBlank(request.location())) {
            throw new IllegalArgumentException("Name, email and location are required");
        }
        if (!request.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email address is invalid");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
