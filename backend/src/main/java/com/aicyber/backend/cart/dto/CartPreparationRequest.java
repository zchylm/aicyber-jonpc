package com.aicyber.backend.cart.dto;

import java.util.List;
import java.util.UUID;

public record CartPreparationRequest(List<Item> items) {
    public record Item(UUID systemId, int quantity) {}
}
