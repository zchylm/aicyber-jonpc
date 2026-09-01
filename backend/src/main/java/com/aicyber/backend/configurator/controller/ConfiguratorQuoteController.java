package com.aicyber.backend.configurator.controller;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;
import com.aicyber.backend.configurator.service.ConfiguratorQuoteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/configurator")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5174", "http://127.0.0.1:5174"})
public class ConfiguratorQuoteController {

    private final ConfiguratorQuoteService quoteService;

    public ConfiguratorQuoteController(ConfiguratorQuoteService quoteService) {
        this.quoteService = quoteService;
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
}
