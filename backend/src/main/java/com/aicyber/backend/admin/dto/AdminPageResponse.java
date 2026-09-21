package com.aicyber.backend.admin.dto;

import java.util.List;

public record AdminPageResponse<T>(List<T> items, long total, int page, int size) {
}
