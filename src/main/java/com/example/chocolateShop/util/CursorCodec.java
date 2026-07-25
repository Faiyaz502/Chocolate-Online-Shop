package com.example.chocolateShop.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Encodes/decodes opaque keyset-pagination cursors.
 * <p>
 * The cursor carries the sort key(s) of the last item on the previous page,
 * so the next query can resume with "WHERE (sortKey, id) > (lastSortKey, lastId)"
 * instead of an OFFSET, which stays fast and stable even as the underlying
 * data changes between page requests.
 * <p>
 * Format (before base64): "id=<id>" or "id=<id>&price=<price>"
 */
public final class CursorCodec {

    private CursorCodec() {}

    public record CursorPayload(Long id, BigDecimal price) {}

    public static String encode(Long id) {
        return encode(id, null);
    }

    public static String encode(Long id, BigDecimal price) {
        String raw = (price == null)
                ? "id=" + id
                : "id=" + id + "&price=" + price.toPlainString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static CursorPayload decode(String cursor) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            Long id = null;
            BigDecimal price = null;

            for (String part : raw.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2) continue;
                switch (kv[0]) {
                    case "id" -> id = Long.valueOf(kv[1]);
                    case "price" -> price = new BigDecimal(kv[1]);
                }
            }

            if (id == null) {
                throw new IllegalArgumentException("Cursor is missing required 'id' field");
            }
            return new CursorPayload(id, price);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid pagination cursor", e);
        }
    }
}
