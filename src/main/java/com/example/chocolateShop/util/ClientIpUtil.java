package com.example.chocolateShop.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtil {

    private ClientIpUtil() {}

    public static String resolve(HttpServletRequest request) {
        // Forwarded headers are attacker controlled unless a trusted proxy
        // strips and rewrites them. Use the socket peer address here.
        return request.getRemoteAddr();
    }
}
