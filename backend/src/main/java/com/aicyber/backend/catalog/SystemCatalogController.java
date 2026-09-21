package com.aicyber.backend.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/systems")
public class SystemCatalogController {
    private final SystemCatalogService service;

    public SystemCatalogController(SystemCatalogService service) {
        this.service = service;
    }

    @GetMapping("/preview")
    public List<PublicSystemPreview> previews() {
        return service.previews().stream().map(PublicSystemPreview::from).toList();
    }
}
