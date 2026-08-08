package com.example.chocolateShop.util;

/**
 * Identifies who a cart/order belongs to: either a registered user, or an
 * anonymous guest tracked by IP address. Exactly one of the two is set.
 * <p>
 * Note on the IP-as-identity approach: it's a reasonable low-friction way
 * to let guests use a cart without an account, but it's not a strong
 * identity -- IPs get reused (NAT, shared networks, mobile carriers) and
 * change (dynamic IPs, switching networks), so a guest can lose their cart
 * by changing networks, and in rare cases could see another guest's cart
 * on the same shared IP. Treat it as a convenience, not a security
 * boundary; anything sensitive should require the user to log in.
 */
public record OwnerRef(Long userId, String ipAddress) {

    public OwnerRef {
        if ((userId == null) == (ipAddress == null)) {
            throw new IllegalStateException("OwnerRef requires exactly one of userId or ipAddress to be set");
        }
    }

    public static OwnerRef ofUser(Long userId) {
        return new OwnerRef(userId, null);
    }

    public static OwnerRef ofGuest(String ipAddress) {
        return new OwnerRef(null, ipAddress);
    }

    public boolean isGuest() {
        return userId == null;
    }
}
