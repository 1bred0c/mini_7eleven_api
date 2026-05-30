# Mini 7-Eleven API for FE (raw)

## Overview
- Base path: `/api/v1`
- Auth header: `Authorization: Bearer <accessToken>`
- Token type from login/register: `tokenType` (usually `Bearer`)
- Security note: all endpoints require JWT except:
  - `POST /api/v1/auth/register`
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`

## Enums
- Role: `ADMIN`, `USER`
- ProductStatus: `ACTIVE`, `INACTIVE`, `OUT_OF_STOCK`
- OrderStatus: `PENDING`, `CONFIRMED`, `PREPARING`, `COMPLETED`, `CANCELLED`
- PaymentMethod: `WALLET`, `CASH_ON_DELIVERY`, `CASH_AT_STORE`
- PaymentStatus: `UNPAID`, `PAID`, `FAILED`, `REFUNDED`
- WalletTransactionType: `TOP_UP`, `PAYMENT`, `RECEIVE_PAYMENT`, `REFUND`

## Error response format
```json
{
  "timestamp": "2026-05-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "code": "VALIDATION_ERROR",
  "path": "/api/v1/products",
  "errors": {
    "fieldName": "Error message"
  }
}
```
- `errors` only present for validation errors.
- Common codes: `VALIDATION_ERROR`, `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `DATA_INTEGRITY_VIOLATION`, `METHOD_NOT_ALLOWED`, `INTERNAL_SERVER_ERROR`.

## Pagination (Spring Data Page)
Endpoints returning `Page<T>` return a JSON object with at least:
- `content` (array)
- `totalElements`, `totalPages`, `size`, `number`
- `sort`, `first`, `last`, `numberOfElements`, `empty`, `pageable`

---

# Auth
Base: `/api/v1/auth`

### POST `/register`
- Auth: public
- Body: `RegisterRequest`
- Response: `LoginResponse`
- Error codes: `AUTH_EMAIL_EXISTS`, `VALIDATION_ERROR`

### POST `/login`
- Auth: public
- Body: `LoginRequest`
- Response: `LoginResponse`
- Error codes: `AUTH_INVALID_CREDENTIALS`, `VALIDATION_ERROR`

### POST `/refresh`
- Auth: public
- Body: `RefreshTokenRequest`
- Response: `LoginResponse` (refresh token is rotated)
- Error codes: `AUTH_INVALID_REFRESH_TOKEN`, `AUTH_REFRESH_TOKEN_REVOKED`, `AUTH_REFRESH_TOKEN_EXPIRED`, `VALIDATION_ERROR`

### POST `/logout`
- Auth: public
- Body: `RefreshTokenRequest`
- Response: `204 No Content`

### GET `/me`
- Auth: JWT
- Response: `MeResponse`

---

# Category
Base: `/api/v1/categories`

### POST `/`
- Auth: ADMIN
- Body: `CategoryRequest`
- Response: `CategoryResponse`

### PUT `/{id}`
- Auth: ADMIN
- Body: `CategoryRequest`
- Response: `CategoryResponse`
- Error codes: `CATEGORY_NOT_FOUND`

### DELETE `/{id}`
- Auth: ADMIN
- Response: `204 No Content`
- Error codes: `CATEGORY_NOT_FOUND`

### GET `/{id}`
- Auth: ADMIN, USER
- Response: `CategoryResponse`
- Error codes: `CATEGORY_NOT_FOUND`

### GET `/`
- Auth: ADMIN, USER
- Response: `Page<CategoryResponse>`

---

# Product
Base: `/api/v1/products`

### POST `/`
- Auth: ADMIN
- Body: `ProductCreateRequest`
- Response: `ProductResponse`
- Error codes: `CATEGORY_NOT_FOUND`, `PRODUCT_STATUS_INVALID`

### PUT `/{id}`
- Auth: ADMIN
- Body: `ProductUpdateRequest`
- Response: `ProductResponse`
- Error codes: `PRODUCT_NOT_FOUND`, `CATEGORY_NOT_FOUND`, `PRODUCT_STATUS_INVALID`

### PATCH `/{id}/status`
- Auth: ADMIN
- Body: `ProductStatusUpdateRequest`
- Response: `ProductResponse`
- Error codes: `PRODUCT_NOT_FOUND`, `PRODUCT_STATUS_INVALID`

### PATCH `/{id}/stock`
- Auth: ADMIN
- Body: `ProductStockUpdateRequest`
- Response: `ProductResponse`
- Error codes: `PRODUCT_NOT_FOUND`

### DELETE `/{id}`
- Auth: ADMIN
- Response: `204 No Content`
- Error codes: `PRODUCT_NOT_FOUND`

### GET `/{id}`
- Auth: ADMIN, USER
- Response: `ProductResponse`
- Error codes: `PRODUCT_NOT_FOUND`

### GET `/`
- Auth: ADMIN, USER
- Query: `keyword`, `status`, `categoryId`, `minPrice`, `maxPrice`, `minStock`, `maxStock`, `page`, `size`, `sort`
- Response: `Page<ProductResponse>`

#### Product status rules
- `stockQuantity <= 0` => `OUT_OF_STOCK`
- If `stockQuantity > 0` and request sets `status = OUT_OF_STOCK` => `PRODUCT_STATUS_INVALID`
- If `status` not provided and `stockQuantity > 0`, default stays `ACTIVE` or current value.

---

# Address
Base: `/api/v1/addresses`

### POST `/`
- Auth: USER
- Body: `AddressRequest`
- Response: `AddressResponse`
- Behavior: if `isDefault = true`, clears default on other addresses for the same user.

### PUT `/{id}`
- Auth: USER
- Body: `AddressRequest`
- Response: `AddressResponse`
- Error codes: `ADDRESS_NOT_FOUND`

### PATCH `/{id}/default`
- Auth: USER
- Response: `AddressResponse`
- Error codes: `ADDRESS_NOT_FOUND`

### DELETE `/{id}`
- Auth: USER
- Response: `204 No Content`
- Error codes: `ADDRESS_NOT_FOUND`

### GET `/{id}`
- Auth: USER
- Response: `AddressResponse`
- Error codes: `ADDRESS_NOT_FOUND`

### GET `/`
- Auth: USER
- Response: `Page<AddressResponse>`

---

# Order
Base: `/api/v1/orders`

### POST `/`
- Auth: USER
- Body: `OrderCreateRequest`
- Response: `OrderResponse`
- Error codes:
  - `ADDRESS_NOT_FOUND`, `PRODUCT_NOT_FOUND`
  - `PRODUCT_NOT_AVAILABLE`, `PRODUCT_OUT_OF_STOCK`
  - `WALLET_NOT_FOUND`, `WALLET_INSUFFICIENT_BALANCE` (when `paymentMethod = WALLET`)

### GET `/my`
- Auth: USER
- Response: `Page<OrderSummaryResponse>`

### GET `/my/{id}`
- Auth: USER
- Response: `OrderResponse`
- Error codes: `ORDER_NOT_FOUND`, `ORDER_ACCESS_DENIED`

### PATCH `/my/{id}/cancel`
- Auth: USER
- Response: `OrderResponse`
- Error codes: `ORDER_NOT_FOUND`, `ORDER_ACCESS_DENIED`, `ORDER_STATUS_INVALID`

### GET `/`
- Auth: ADMIN
- Query: `status`, `paymentStatus`, `accountId`, `page`, `size`, `sort`
- Response: `Page<OrderSummaryResponse>`

### GET `/{id}`
- Auth: ADMIN
- Response: `OrderResponse`
- Error codes: `ORDER_NOT_FOUND`

### PATCH `/{id}/status`
- Auth: ADMIN
- Body: `OrderStatusUpdateRequest`
- Response: `OrderResponse`
- Error codes: `ORDER_NOT_FOUND`, `ORDER_STATUS_INVALID`, `STATUS_TRANSITION_INVALID`

### PATCH `/{id}/payment-status`
- Auth: ADMIN
- Body: `OrderPaymentStatusUpdateRequest`
- Response: `OrderResponse`
- Error codes: `ORDER_NOT_FOUND`, `PAYMENT_STATUS_INVALID`

#### Order rules
- Items with same `productId` are merged (quantities summed).
- Status transitions:
  - `PENDING` -> `CONFIRMED` or `CANCELLED`
  - `CONFIRMED` -> `PREPARING` or `CANCELLED`
  - `PREPARING` -> `COMPLETED` or `CANCELLED`
  - `COMPLETED` or `CANCELLED` cannot change
- If order is cancelled and paid by wallet, system refunds wallet and restores stock.
- Wallet payment:
  - On create, if `paymentMethod = WALLET`, payment is processed immediately and `paymentStatus = PAID`.
  - Payment status updates for wallet: only `PAID -> REFUNDED` is allowed.

---

# Wallet
Base: `/api/v1/wallets`

### POST `/me/open`
- Auth: USER
- Response: `WalletResponse`
- Behavior: creates wallet if not exists.

### GET `/me`
- Auth: USER
- Response: `WalletResponse`
- Behavior: creates wallet if not exists.

### POST `/me/top-up`
- Auth: USER
- Body: `WalletTopUpRequest`
- Response: `WalletResponse`
- Error codes: `WALLET_INVALID_AMOUNT`

### GET `/me/transactions`
- Auth: USER
- Response: `Page<WalletTransactionResponse>`

#### Wallet rules
- `amount <= 0` => `WALLET_INVALID_AMOUNT`
- Pay order (wallet): `WALLET_INSUFFICIENT_BALANCE` if not enough.

---

# DTO Definitions

## Auth DTOs
### RegisterRequest
```json
{
  "fullName": "string (max 100)",
  "email": "string (email, max 100)",
  "password": "string (8-100)"
}
```

### LoginRequest
```json
{
  "email": "string (email)",
  "password": "string"
}
```

### RefreshTokenRequest
```json
{
  "refreshToken": "string"
}
```

### LoginResponse
```json
{
  "tokenType": "Bearer",
  "accessToken": "string",
  "refreshToken": "string"
}
```

### MeResponse
```json
{
  "id": 1,
  "fullName": "string",
  "email": "string",
  "role": "USER",
  "createdAt": "2026-05-28T12:00:00"
}
```

## Category DTOs
### CategoryRequest
```json
{
  "name": "string (max 255)",
  "description": "string (max 1000)"
}
```

### CategoryResponse
```json
{
  "id": 1,
  "name": "string",
  "description": "string",
  "createdAt": "2026-05-28T12:00:00",
  "updatedAt": "2026-05-28T12:00:00"
}
```

### CategorySummary
```json
{
  "id": 1,
  "name": "string"
}
```

## Product DTOs
### ProductCreateRequest
```json
{
  "name": "string (max 255)",
  "description": "string (max 2000)",
  "price": 10.5,
  "stockQuantity": 100,
  "imageUrl": "string (max 2048)",
  "status": "ACTIVE",
  "categoryId": 1
}
```

### ProductUpdateRequest
```json
{
  "name": "string (max 255)",
  "description": "string (max 2000)",
  "price": 10.5,
  "stockQuantity": 100,
  "imageUrl": "string (max 2048)",
  "status": "ACTIVE",
  "categoryId": 1
}
```

### ProductStatusUpdateRequest
```json
{
  "status": "ACTIVE"
}
```

### ProductStockUpdateRequest
```json
{
  "stockQuantity": 100
}
```

### ProductResponse
```json
{
  "id": 1,
  "name": "string",
  "description": "string",
  "price": 10.5,
  "stockQuantity": 100,
  "imageUrl": "string",
  "status": "ACTIVE",
  "category": { "id": 1, "name": "string" },
  "createdAt": "2026-05-28T12:00:00",
  "updatedAt": "2026-05-28T12:00:00"
}
```

## Address DTOs
### AddressRequest
```json
{
  "receiverName": "string (max 100)",
  "phoneNumber": "string (max 20)",
  "addressLine": "string (max 255)",
  "ward": "string (max 100)",
  "district": "string (max 100)",
  "city": "string (max 100)",
  "isDefault": true
}
```

### AddressResponse
```json
{
  "id": 1,
  "accountId": 1,
  "receiverName": "string",
  "phoneNumber": "string",
  "addressLine": "string",
  "ward": "string",
  "district": "string",
  "city": "string",
  "isDefault": true,
  "createdAt": "2026-05-28T12:00:00",
  "updatedAt": "2026-05-28T12:00:00"
}
```

## Order DTOs
### OrderItemRequest
```json
{
  "productId": 1,
  "quantity": 2
}
```

### OrderCreateRequest
```json
{
  "addressId": 1,
  "paymentMethod": "WALLET",
  "items": [
    { "productId": 1, "quantity": 2 }
  ]
}
```

### OrderItemResponse
```json
{
  "id": 1,
  "productId": 1,
  "productName": "string",
  "unitPrice": 10.5,
  "quantity": 2,
  "subtotal": 21.0
}
```

### OrderResponse
```json
{
  "id": 1,
  "accountId": 1,
  "customerName": "string",
  "phoneNumber": "string",
  "address": "string",
  "paymentMethod": "WALLET",
  "paymentStatus": "PAID",
  "status": "PENDING",
  "totalAmount": 21.0,
  "items": [
    { "id": 1, "productId": 1, "productName": "string", "unitPrice": 10.5, "quantity": 2, "subtotal": 21.0 }
  ],
  "createdAt": "2026-05-28T12:00:00",
  "updatedAt": "2026-05-28T12:00:00"
}
```

### OrderSummaryResponse
```json
{
  "id": 1,
  "accountId": 1,
  "customerName": "string",
  "phoneNumber": "string",
  "address": "string",
  "paymentMethod": "WALLET",
  "paymentStatus": "PAID",
  "status": "PENDING",
  "totalAmount": 21.0,
  "createdAt": "2026-05-28T12:00:00",
  "updatedAt": "2026-05-28T12:00:00"
}
```

### OrderStatusUpdateRequest
```json
{
  "status": "CONFIRMED"
}
```

### OrderPaymentStatusUpdateRequest
```json
{
  "paymentStatus": "PAID"
}
```

## Wallet DTOs
### WalletTopUpRequest
```json
{
  "amount": 100.0,
  "description": "string (max 255)"
}
```

### WalletResponse
```json
{
  "id": 1,
  "accountId": 1,
  "balance": 100.0,
  "createdAt": "2026-05-28T12:00:00",
  "updatedAt": "2026-05-28T12:00:00"
}
```

### WalletTransactionResponse
```json
{
  "id": 1,
  "type": "TOP_UP",
  "amount": 100.0,
  "balanceBefore": 0.0,
  "balanceAfter": 100.0,
  "description": "string",
  "createdAt": "2026-05-28T12:00:00"
}
```

