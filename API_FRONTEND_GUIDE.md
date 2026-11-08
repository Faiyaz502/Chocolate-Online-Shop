# Chocolate Shop API: complete frontend contract

This document describes every HTTP endpoint currently implemented by the backend and the JSON DTOs exchanged with the frontend. It was checked against the Spring controllers and DTO records on 2026-11-03. Paths below are relative to the base URL.

## 1. Environments, headers, and authentication

| Setting | Value |
|---|---|
| API prefix | `/api/v1` |
| Local backend default | `http://localhost:8080` |
| JSON request headers | `Accept: application/json`, `Content-Type: application/json` (on requests with a body) |
| Auth header | `Authorization: Bearer <accessToken>` |
| Access token lifetime | 15 minutes |
| Refresh token lifetime | 7 days; rotated at each successful refresh |
| Local CORS origin default | `http://localhost:4200` |

Protected endpoints require a valid access token. `/admin/**` endpoints additionally require role `ADMIN`. Public endpoints are explicitly marked below. Do not send a token in query parameters. The API returns tokens in JSON and does not use auth cookies.

### Token lifecycle

1. Register or login and retain the returned access and refresh tokens for the session.
2. Attach the access token as a bearer token to protected calls.
3. On an expired/invalid access token (401), call `POST /api/v1/auth/refresh` with the current refresh token. Replace **both** tokens with the response; refresh tokens rotate and the old one cannot be reused.
4. Logout by posting the current refresh token to `/api/v1/auth/logout`. Discard both client-side tokens after success.
5. If refresh fails (401), clear the session and require login again. Disabled accounts cannot use an existing access token or refresh.

Auth responses carry `Cache-Control: no-store`. Frontends should avoid logging tokens, avoid storing them in URLs, and choose token storage with XSS exposure in mind. If auth credentials are ever moved to cookies, reassess CSRF and enable appropriate CSRF protection.

## 2. Shared response conventions

- JSON timestamps are ISO-8601 date/time strings. Entity timestamps are generally `LocalDateTime` and may look like `2026-11-03T12:30:00`; auth/error timestamps are instants and include `Z` or an offset.
- Money is represented as JSON numbers backed by decimal values. Keep decimal precision; do not use binary floating point for calculations.
- IDs are numeric `Long` values. Slugs and cursor strings are opaque strings.
- Empty success responses have no JSON body: 204.
- Cursor page shape is `{ "items": [], "nextCursor": null, "hasNext": false }`. When `hasNext` is true, pass `nextCursor` unchanged as the next `cursor` parameter.
- Normal error shape: `{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "path": "/api/v1/..." }`.
- Validation error shape: `{ "timestamp": "...", "status": 400, "error": "Validation Failed", "fieldErrors": { "field": "reason" }, "path": "/api/v1/..." }`.
- Rate-limit error is the normal error shape with status 429 and a `Retry-After: 60` response header.

Common outcomes: `400` Jakarta bean-validation failures; `401` missing/invalid/expired auth or invalid credentials/refresh; `403` authenticated caller lacks role; `404` missing resource (also used to conceal another user's address/order); `409` duplicate email or data-integrity conflict; `429` auth rate limited; `500` unexpected server failure. Not every endpoint produces every status. **Current API limitation:** several service-level business errors (for example empty cart, insufficient stock, invalid coupon, invalid cursor, invalid saved/inline address combination, or invalid order-status transition) are not mapped to dedicated HTTP statuses by the global exception handler and may currently return generic 500. Do not assume these failures are 4xx until the backend adds explicit mappings. Treat response messages as diagnostics, not as stable machine codes.

## 3. Authentication APIs

All paths in this section are public. All request and response bodies are JSON except logout's empty response.

### `POST /api/v1/auth/register`

Request:

```json
{
  "firstName": "Amina",
  "lastName": "Rahman",
  "email": "amina@example.com",
  "password": "ExamplePass1",
  "phone": "+8801700000000"
}
```

`phone` is optional. First/last name are required, max 100 chars. Email must be valid. Password is 8–100 chars and must contain lowercase, uppercase, and a digit. Phone, when supplied, must be 7–30 permitted phone characters. Success: **201**, `AuthResponse`. Duplicate email: **409**; invalid fields: **400**.

### `POST /api/v1/auth/login`

Request: `{ "email": "amina@example.com", "password": "ExamplePass1" }`. Success: **200**, `AuthResponse`. Invalid credentials: **401**.

### `POST /api/v1/auth/refresh`

Request: `{ "refreshToken": "<current-refresh-token>" }`. Success: **200**, new `AuthResponse`; submitted refresh token is rotated/revoked. Invalid, expired, revoked, or disabled-account token: **401**.

### `POST /api/v1/auth/logout`

Request: `{ "refreshToken": "<current-refresh-token>" }`. Success: **204**, no body. The provided refresh token is revoked. The current access JWT is stateless and remains valid until expiry (up to 15 minutes), unless the account is disabled.

### `AuthResponse`

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque-refresh-token>",
  "tokenType": "Bearer",
  "userId": 42,
  "email": "amina@example.com",
  "firstName": "Amina",
  "lastName": "Rahman",
  "role": "CUSTOMER"
}
```

Roles include `CUSTOMER` and `ADMIN`.

## 4. Public catalog APIs

### `GET /api/v1/categories` — public

Success **200**, array of `CategoryResponse`:

```json
[{"id":1,"name":"Dark Chocolate","slug":"dark-chocolate","description":"Rich cocoa","createdAt":"2026-11-03T12:30:00"}]
```

### `GET /api/v1/categories/{slug}` — public

Success **200**, one `CategoryResponse` as above. Unknown slug: **404**.

### `GET /api/v1/products` — public

Returns active products only, **200**, `CursorPageResponse<ProductResponse>`.

| Query parameter | Type | Meaning |
|---|---|---|
| `sort` | enum | `NEWEST` (default), `PRICE_ASC`, or `PRICE_DESC` |
| `categoryId` | integer | Filter to category ID |
| `keyword` | string | Search keyword; whitespace is trimmed |
| `minPrice` | decimal | Lower price filter |
| `maxPrice` | decimal | Upper price filter |
| `limit` | integer | Requested page size; default 20, clamped to 1–50 |
| `cursor` | string | Opaque cursor from the preceding page. Reuse with the same sort/filter choices. |

Example: `GET /api/v1/products?categoryId=1&keyword=dark&minPrice=5&maxPrice=50&sort=PRICE_ASC&limit=20`.

Response:

```json
{
  "items": [{
    "id": 9,
    "name": "70% Cocoa Bar",
    "slug": "70-cocoa-bar",
    "description": "Single origin cocoa",
    "price": 12.50,
    "discountPrice": 10.00,
    "effectivePrice": 10.00,
    "stockQuantity": 18,
    "active": true,
    "categoryId": 1,
    "categoryName": "Dark Chocolate",
    "images": [{"id":31,"imageUrl":"https://cdn.example.com/bar.jpg","displayOrder":0,"primary":true}],
    "createdAt": "2026-11-03T12:30:00",
    "updatedAt": "2026-11-03T12:30:00"
  }],
  "nextCursor": null,
  "hasNext": false
}
```

`discountPrice` can be null; use backend `effectivePrice` for the displayed/current price. Uploaded product images are stored by the backend; the returned relative `imageUrl` can be resolved against `API_BASE_URL`.

### `GET /api/v1/products/{slug}` — public

Success **200**, one `ProductResponse` with the same shape as a product in the list. Unknown slug: **404**. Current implementation looks up by slug without filtering `active`; an inactive product with a known slug may be returned even though it is omitted from public listing. Frontend should not use an inactive flag as an authorization guarantee; consider correcting this backend behavior before relying on product deactivation to hide detail pages.

## 5. Customer APIs (authenticated)

### Cart

#### `GET /api/v1/cart`

Success **200**, `CartResponse`:

```json
{
  "id": 7,
  "items": [{
    "id": 15,
    "productId": 9,
    "productName": "70% Cocoa Bar",
    "productSlug": "70-cocoa-bar",
    "primaryImageUrl": "https://cdn.example.com/bar.jpg",
    "unitPrice": 10.00,
    "quantity": 2,
    "lineTotal": 20.00,
    "availableStock": 18
  }],
  "totalItems": 2,
  "subtotal": 20.00
}
```

`totalItems` is the sum of quantities. Cart prices/stock can change; validate/refresh before checkout.

#### `POST /api/v1/cart/items`

Request `{ "productId": 9, "quantity": 2 }`; `productId` and `quantity` required, quantity >= 1. Success **200**, updated `CartResponse`.

#### `PUT /api/v1/cart/items/{productId}`

Request `{ "quantity": 3 }`; quantity >= 0. Success **200**, updated cart. Quantity zero removes the item. Invalid quantity: **400**.

#### `DELETE /api/v1/cart/items/{productId}`

Success **200**, updated cart after removal.

#### `DELETE /api/v1/cart`

Success **204**, no response body.

### Addresses

#### `GET /api/v1/addresses`

Success **200**, array of `AddressResponse` for the signed-in user:

```json
[{"id":12,"label":"Home","recipientName":"Amina Rahman","phone":"+8801700000000","line1":"12 Example Road","line2":null,"city":"Dhaka","state":"Dhaka","postalCode":"1207","country":"Bangladesh","isDefault":true}]
```

#### `POST /api/v1/addresses`

Request (also the full replacement body for PUT):

```json
{
  "label": "Home",
  "recipientName": "Amina Rahman",
  "phone": "+8801700000000",
  "line1": "12 Example Road",
  "line2": null,
  "city": "Dhaka",
  "state": "Dhaka",
  "postalCode": "1207",
  "country": "Bangladesh",
  "isDefault": true
}
```

`recipientName`, `phone`, `line1`, `city`, `postalCode`, and `country` are required. `label`, `line2`, `state`, and `isDefault` are optional. Success **201**, one `AddressResponse`.

#### `PUT /api/v1/addresses/{id}`

Requires the same complete request body as POST. Success **200**, updated `AddressResponse`. Another user's address is returned as **404**.

#### `DELETE /api/v1/addresses/{id}`

Success **204**. Nonexistent or not-owned address: **404**.

### Checkout

#### `POST /api/v1/checkout`

Authenticated only. Requires items in cart and exactly one of `shippingAddressId` or inline `shippingAddress` (same fields as address request). Optional `couponCode`; `paymentMethod` required.

Saved-address example:

```json
{"shippingAddressId":12,"couponCode":"SAVE10","paymentMethod":"COD"}
```

Inline-address example:

```json
{
  "shippingAddress": {
    "label":"Delivery",
    "recipientName":"Amina Rahman",
    "phone":"+8801700000000",
    "line1":"12 Example Road",
    "line2":null,
    "city":"Dhaka",
    "state":"Dhaka",
    "postalCode":"1207",
    "country":"Bangladesh",
    "isDefault":false
  },
  "paymentMethod":"CARD"
}
```

Allowed payment methods: `CARD`, `COD`, `BKASH`. Success **201**, `OrderResponse` (schema in Orders below). Empty cart, inactive/out-of-stock product, invalid coupon, or invalid address can fail; several such service errors currently surface as generic 500 as described above. Checkout recalculates product prices, stock, discounts, and shipping on the server and clears the cart on success. Current shipping is free when post-discount subtotal is at least 50.00; otherwise 5.00. Do not calculate/submit totals from the browser.

Card payment is recorded as `PENDING`; the backend has no provider confirmation endpoint yet. Do not show a card payment as completed based only on checkout success.

### Orders

#### `GET /api/v1/orders`

Success **200**, array of the signed-in user's `OrderResponse` objects. No pagination/filter parameters currently exist on this customer route.

#### `GET /api/v1/orders/{id}`

Success **200**, one `OrderResponse`. Other users' orders return **404**.

`OrderResponse`:

```json
{
  "id": 101,
  "orderNumber": "ORD-20261103-AB12CD34",
  "status": "PENDING",
  "subtotal": 20.00,
  "discountTotal": 0.00,
  "shippingFee": 5.00,
  "grandTotal": 25.00,
  "couponCode": null,
  "shippingAddressId": 12,
  "items": [{"id":201,"productId":9,"productName":"70% Cocoa Bar","unitPrice":10.00,"quantity":2,"lineTotal":20.00}],
  "payment": {"id":88,"method":"COD","status":"PENDING","providerReference":null,"amount":25.00,"paidAt":null},
  "guestOrder": false,
  "guestEmail": null,
  "createdAt": "2026-11-03T12:30:00",
  "updatedAt": "2026-11-03T12:30:00"
}
```

Order item fields: `id`, `productId`, `productName`, `unitPrice`, `quantity`, `lineTotal`. Payment fields: `id`, `method`, `status`, `providerReference`, `amount`, `paidAt`. Payment status enum: `PENDING`, `PAID`, `FAILED`, `REFUNDED`. Order status enum: `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`. `guestOrder`/`guestEmail` are legacy response fields; new guest checkout is disabled.

`POST /api/v1/orders/{id}/payment/confirm` is **not implemented** and returns 404; do not call it.

## 6. Admin APIs (authenticated role ADMIN)

### Dashboard

#### `GET /api/v1/admin/dashboard/summary`

Success **200**, `SalesSummaryResponse`:

```json
{
  "totalRevenue": 1200.00,
  "totalOrders": 40,
  "ordersByStatus": {"PENDING":3,"PROCESSING":4,"SHIPPED":5,"DELIVERED":27,"CANCELLED":1},
  "totalCustomers": 25,
  "totalProducts": 12,
  "totalCategories": 4,
  "lowStockProductCount": 2
}
```

### Products

#### `GET /api/v1/admin/products`

Same query parameters and cursor response as public product listing; includes inactive products. Requires admin.

#### `POST /api/v1/admin/products`

Request:

```json
{
  "categoryId": 1,
  "name": "70% Cocoa Bar",
  "description": "Single origin cocoa",
  "price": 12.50,
  "discountPrice": 10.00,
  "stockQuantity": 18,
  "active": true
}
```

`categoryId`, `name`, `price`, and `stockQuantity` are required. Name max 200; price/discount cannot be negative; stock >= 0. `active` defaults true when omitted. Product creation does not accept image URLs or file bytes; upload images through the multipart image endpoints below. Success **201**, `ProductResponse` with `images: []` until images are uploaded.

#### `PUT /api/v1/admin/products/{id}`

Uses the same JSON body and validations as POST; it updates product fields and leaves the current image list unchanged. Replace or remove images through the dedicated image endpoints. Success **200**, `ProductResponse`; unknown product/category: **404**.

#### `DELETE /api/v1/admin/products/{id}`

Success **204**; unknown product **404**; referenced product may return **409**.

#### `POST /api/v1/admin/products/{id}/images`

Send `multipart/form-data` with fields:

| Field | Required | Meaning |
|---|---|---|
| `file` | Yes | JPEG or PNG image; max 5 MB; max 8000 px per side and 25 megapixels total. |
| `displayOrder` | No | Integer order in the product gallery; defaults to the next position. |
| `primary` | No | Boolean; when true, this becomes the primary photo. First image is automatically primary. |

Example with curl:

```bash
curl -X POST "$API_BASE/api/v1/admin/products/42/images" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "file=@bar-photo.jpg;type=image/jpeg" \
  -F "displayOrder=0" \
  -F "primary=true"
```

Success **201**, updated `ProductResponse`. The backend assigns a random filename, stores the file under `PRODUCT_IMAGE_DIRECTORY` (default `uploads/products`), writes its public path to `product_images.image_url`, and returns the URL in `images[].imageUrl`, for example `/uploads/products/27a8c960-89f2-4d82-9759-b5d02eef8702.jpg`. Use `new URL(imageUrl, API_BASE_URL)` in the frontend to resolve the relative path.

#### `PUT /api/v1/admin/products/{id}/images/{imageId}`

Use the same multipart fields to replace a specific existing image. The row keeps its ID, receives the new stored file URL, and the old backend-managed file is deleted after the database update commits. If the update fails, the old image remains and the newly uploaded file is cleaned up. Success **200**, updated `ProductResponse`.

#### `DELETE /api/v1/admin/products/{id}/images/{imageId}`

Success **200**, updated `ProductResponse`; the database row is removed and the backend-managed file is deleted after commit. Missing product or image **404**. Deleting a remote legacy/demo URL only deletes its database row.

#### `GET /uploads/products/{filename}` — public

Returns the stored image bytes with `image/jpeg` or `image/png`, `X-Content-Type-Options: nosniff`, and long-lived immutable caching. Unknown or invalid filenames return **404**. The filename is generated by the backend; never construct filesystem paths from user input.

Product response shape is shown under Public Catalog. Uploaded image object: `{ "id": 31, "imageUrl": "/uploads/products/<uuid>.jpg", "displayOrder": 0, "primary": true }`.

### Categories

There is no admin category-list endpoint; use public `GET /api/v1/categories` to populate selection lists.

#### `POST /api/v1/admin/categories`

Request `{ "name": "Dark Chocolate", "description": "Rich cocoa" }`. Name required, max 100; description max 500. Success **201**, `CategoryResponse`.

#### `PUT /api/v1/admin/categories/{id}`

Same complete request body. Success **200**, `CategoryResponse`; unknown ID **404**.

#### `DELETE /api/v1/admin/categories/{id}`

Success **204**; unknown ID **404**; category referenced by products may return **409**.

### Coupons

#### `GET /api/v1/admin/coupons`

Success **200**, array of `CouponResponse`:

```json
[{"id":5,"code":"SAVE10","discountPercent":10.00,"discountAmount":null,"validFrom":"2026-01-01T00:00:00","validUntil":"2027-01-01T00:00:00","maxUses":100,"timesUsed":3,"active":true}]
```

#### `POST /api/v1/admin/coupons` and `PUT /api/v1/admin/coupons/{id}`

Request:

```json
{
  "code": "SAVE10",
  "discountPercent": 10.00,
  "discountAmount": null,
  "validFrom": "2026-01-01T00:00:00",
  "validUntil": "2027-01-01T00:00:00",
  "maxUses": 100,
  "active": true
}
```

Provide exactly one of `discountPercent` (0.01–100) or `discountAmount` (>0). Both dates required as ISO local date-time strings; `maxUses` optional but if present >= 1; `code` required, max 50. Success POST **201**, PUT **200**, both return `CouponResponse`. Duplicate/conflicting coupon may be **409**; missing update ID **404**.

#### `DELETE /api/v1/admin/coupons/{id}`

Success **204**; unknown ID **404**; coupon referenced by orders may return **409**.

### Orders

#### `GET /api/v1/admin/orders`

Optional `status` (`PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`), `cursor`, `limit`. Success **200**, cursor page of summaries:

```json
{
  "items": [{"id":101,"orderNumber":"ORD-20261103-AB12CD34","status":"PENDING","customerName":"Amina Rahman","customerEmail":"amina@example.com","guestOrder":false,"grandTotal":25.00,"createdAt":"2026-11-03T12:30:00"}],
  "nextCursor": null,
  "hasNext": false
}
```

#### `GET /api/v1/admin/orders/{id}`

Success **200**, `AdminOrderResponse`:

```json
{
  "id":101,
  "orderNumber":"ORD-20261103-AB12CD34",
  "status":"PENDING",
  "customerId":42,
  "customerName":"Amina Rahman",
  "customerEmail":"amina@example.com",
  "guestOrder":false,
  "guestIpAddress":null,
  "subtotal":20.00,
  "discountTotal":0.00,
  "shippingFee":5.00,
  "grandTotal":25.00,
  "couponCode":null,
  "shippingAddressId":12,
  "items":[{"id":201,"productId":9,"productName":"70% Cocoa Bar","unitPrice":10.00,"quantity":2,"lineTotal":20.00}],
  "payment":{"id":88,"method":"COD","status":"PENDING","providerReference":null,"amount":25.00,"paidAt":null},
  "createdAt":"2026-11-03T12:30:00",
  "updatedAt":"2026-11-03T12:30:00"
}
```

#### `PATCH /api/v1/admin/orders/{id}/status`

Request `{ "status": "PROCESSING" }`; status required and must be an enum. Success **200**, updated `AdminOrderResponse`. Allowed transitions: `PENDING` -> `PROCESSING` or `CANCELLED`; `PROCESSING` -> `SHIPPED` or `CANCELLED`; `SHIPPED` -> `DELIVERED`; `DELIVERED` and `CANCELLED` are terminal. Requesting the current state is a successful no-op. Invalid transitions currently surface as generic **500** because they are not mapped to a client error. Unknown order **404**.

### Customers

#### `GET /api/v1/admin/customers`

Optional `cursor`, `limit`. Success **200**, cursor page of `AdminCustomerResponse`:

```json
{
  "items": [{"id":42,"firstName":"Amina","lastName":"Rahman","email":"amina@example.com","phone":"+8801700000000","role":"CUSTOMER","enabled":true,"createdAt":"2026-11-03T12:30:00"}],
  "nextCursor": null,
  "hasNext": false
}
```

#### `GET /api/v1/admin/customers/{id}`

Success **200**, one `AdminCustomerResponse` matching an item above; unknown ID **404**.

#### `PATCH /api/v1/admin/customers/{id}/status`

Request `{ "enabled": false }`; `enabled` required boolean. Success **200**, updated `AdminCustomerResponse`. Disabling a user causes their current JWT requests and refresh attempts to be rejected. Unknown ID **404**.

## 7. Health endpoint

### `GET /actuator/health` — public

Success **200** for healthy app, e.g. `{ "status": "UP" }`. Intended for monitoring; do not use it as a catalog or user-session API.

## 8. Optional development catalog

When the backend is started with the explicit `dev` Spring profile, `DemoDataSeeder` inserts four categories, twelve active products, one chocolate photo per product, and the valid `WELCOME10` 10% coupon if those demo slugs/codes do not already exist. It replaces placeholder URLs while preserving real custom image URLs. It also creates `admin@localhost.test` with password `ChocolateAdmin2026!` if the email is unused; override these dev-only credentials with `DEMO_ADMIN_EMAIL` and `DEMO_ADMIN_PASSWORD`. The password is BCrypt-hashed and existing accounts are never changed. Use only with a local/disposable database. Setup instructions and photo details are in [DEMO_DATA.md](DEMO_DATA.md). The images load from Pexels/Unsplash hosts.

## 9. Security and frontend implementation notes

- **Authorization / BOLA:** customer cart, addresses, checkout, and orders require authentication. Server-side lookups bind records to the authenticated user; do not rely on hidden UI controls or client-supplied user IDs. Cross-user address/order access behaves as 404.
- **Roles:** backend enforces admin permissions; a frontend role check is for navigation only, never security enforcement. A customer calling admin APIs receives 403.
- **CSRF:** current API uses stateless bearer headers and does not set auth cookies; CSRF is disabled for this design. Do not change to cookie-based auth without CSRF protections.
- **CORS:** production must set `CORS_ORIGINS` to exact trusted frontend origins. Wildcard origins are rejected. Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS. Allowed headers: Authorization, Content-Type, Accept. Credentials are disabled.
- **XSS:** product/category/customer text returned by the API is untrusted input. Render it as text; do not inject strings into HTML or execute markup. Only support sanitized rich text if the product explicitly needs it.
- **Files and images:** admin upload accepts JPEG/PNG only, verifies actual image content and dimensions, caps file size at 5 MB, generates server-side filenames, and stores files in a configurable local directory. Mount persistent storage at `PRODUCT_IMAGE_DIRECTORY` in deployments and back it up with the database. Existing seeded product photos are remote URLs until an admin replaces them with uploads.
- **Payments:** checkout does not establish that external payment succeeded. Card payment remains pending until a real provider integration verifies payment server-side. No payment-confirm endpoint or webhook endpoint currently exists.
- **Secrets:** database credentials and `JWT_SECRET` are server configuration only. Never include backend secrets or third-party secret API keys in frontend source/build variables.
- **Logging:** do not log passwords, access/refresh tokens, payment secrets, or complete authorization headers from the frontend or telemetry.
- **Rate limiting:** current application limit is 20 requests/minute on `/api/v1/auth/**` per observed remote socket IP and per application instance. On 429, wait for `Retry-After`; avoid aggressive login/refresh retry loops. Public catalog endpoints do not have this app-level limiter.
- **Token storage:** this backend cannot determine browser storage strategy. Prefer a design that limits persistent token exposure; never place bearer tokens in local storage without consciously accepting its XSS risk. Do not cache auth responses.
- **Unsupported checklist features:** there is no MFA, upload pipeline, webhook receiver, browser code/source map, or RLS integration in this backend. Those require separate implementation/review.

## 10. Verification scope

The backend's earlier `target/api-curl-smoke.ps1` run exercised 74 curl requests against the test Spring profile using isolated in-memory H2. That run predates multipart image upload and does not verify the new file endpoints. CORS configured-origin allow and unconfigured-origin rejection were tested separately. Production PostgreSQL migrations, persistent upload volumes, external payment providers, browser rendering/token storage, production secrets, and proxy configuration remain unverified.
