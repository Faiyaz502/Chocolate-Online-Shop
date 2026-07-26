package com.example.chocolateShop.service;

import com.example.chocolateShop.dtos.admin.AdminCustomerResponse;
import com.example.chocolateShop.dtos.common.CursorPageResponse;
import com.example.chocolateShop.entity.User;
import com.example.chocolateShop.enums.Role;
import com.example.chocolateShop.exceptions.ResourceNotFoundException;
import com.example.chocolateShop.repository.UserRepository;
import com.example.chocolateShop.util.CursorCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCustomerService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 50;

    private final UserRepository userRepository;

    public CursorPageResponse<AdminCustomerResponse> list(String cursor, Integer limit) {
        int effectiveLimit = clampLimit(limit);
        Long cursorId = (cursor != null && !cursor.isBlank()) ? CursorCodec.decode(cursor).id() : null;

        List<User> fetched = userRepository.findByRoleKeyset(
                Role.CUSTOMER, cursorId, PageRequest.of(0, effectiveLimit + 1));

        boolean hasNext = fetched.size() > effectiveLimit;
        List<User> page = hasNext ? fetched.subList(0, effectiveLimit) : fetched;

        String nextCursor = (hasNext && !page.isEmpty())
                ? CursorCodec.encode(page.get(page.size() - 1).getId())
                : null;

        return CursorPageResponse.<AdminCustomerResponse>builder()
                .items(page.stream().map(this::toResponse).toList())
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    public AdminCustomerResponse getById(Long id) {
        User user = findCustomer(id);
        return toResponse(user);
    }

    @Transactional
    public AdminCustomerResponse setEnabled(Long id, boolean enabled) {
        User user = findCustomer(id);
        user.setEnabled(enabled);
        return toResponse(user);
    }

    private User findCustomer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        if (user.getRole() != Role.CUSTOMER) {
            throw new ResourceNotFoundException("Customer not found with id: " + id);
        }
        return user;
    }

    private int clampLimit(Integer limit) {
        if (limit == null) return DEFAULT_LIMIT;
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private AdminCustomerResponse toResponse(User user) {
        return AdminCustomerResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
