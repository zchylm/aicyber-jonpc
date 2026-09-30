package com.aicyber.backend.configurator.controller;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;
import com.aicyber.backend.configurator.dto.ConfiguratorRecommendationRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorRecommendationResponse;
import com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityResponse;
import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorBuildResponse;
import com.aicyber.backend.configurator.service.ConfiguratorCompatibilityService;
import com.aicyber.backend.configurator.service.ConfiguratorRecommendationService;
import com.aicyber.backend.configurator.service.ConfiguratorRequestService;
import com.aicyber.backend.configurator.service.ConfiguratorQuoteService;
import com.aicyber.backend.auth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/configurator")
@CrossOrigin(origins = {
        "http://localhost:5174",
        "http://127.0.0.1:5174",
        "https://jonpc.com.au",
        "https://www.jonpc.com.au"
})
public class ConfiguratorQuoteController {

    private final ConfiguratorQuoteService quoteService;
    private final ConfiguratorRecommendationService recommendationService;
    private final ConfiguratorCompatibilityService compatibilityService;
    private final ConfiguratorRequestService requestService;
    private final UserRepository users;

    public ConfiguratorQuoteController(ConfiguratorQuoteService quoteService, ConfiguratorRecommendationService recommendationService, ConfiguratorCompatibilityService compatibilityService, ConfiguratorRequestService requestService, UserRepository users) {
        this.quoteService = quoteService;
        this.recommendationService = recommendationService;
        this.compatibilityService = compatibilityService;
        this.requestService = requestService;
        this.users = users;
    }

    @PostMapping("/quote")
    @ResponseStatus(HttpStatus.OK)
    public ConfiguratorQuoteResponse quote(@RequestBody ConfiguratorQuoteRequest request) {
        try {
            return quoteService.quote(request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PostMapping("/recommendation")
    @ResponseStatus(HttpStatus.OK)
    public ConfiguratorRecommendationResponse recommendation(@RequestBody ConfiguratorRecommendationRequest request) {
        try {
            return recommendationService.recommend(request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PostMapping("/compatibility")
    @ResponseStatus(HttpStatus.OK)
    public ConfiguratorCompatibilityResponse compatibility(@RequestBody ConfiguratorCompatibilityRequest request) {
        try {
            return compatibilityService.compatibleOptions(request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/catalog")
    @ResponseStatus(HttpStatus.OK)
    public com.aicyber.backend.configurator.dto.ConfiguratorCatalogResponse catalog() {
        return com.aicyber.backend.configurator.service.ConfiguratorCatalog.response();
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.OK)
    public ConfiguratorBuildResponse submitRequest(Authentication authentication, @RequestBody ConfiguratorBuildRequest request) {
        try {
            if (request.configuration() != null && request.configuration().answers() != null
                    && request.configuration().answers().containsKey("systemSku"))
                throw new IllegalArgumentException("A custom build cannot be submitted as a preconfigured system");
            UUID userId = UUID.fromString(authentication.getName());
            var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Account not found"));
            return requestService.submit(new ConfiguratorBuildRequest(user.displayName(), user.email(),
                    request.phone(), "Online", request.notes(), request.contact(), request.configuration()), userId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

}
