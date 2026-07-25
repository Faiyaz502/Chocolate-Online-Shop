package com.example.chocolateShop.Dtos.common;

import lombok.Builder;

import java.util.List;

@Builder
public record CursorPageResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasNext
) {}
