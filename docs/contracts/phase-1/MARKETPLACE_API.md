# Marketplace API Contract

Owner module: Marketplace
Runtime: Cloud Run modular monolith
Consumers: Web/PWA and Android
Version: `/v1` with `marketplace_api_v2` compatibility flag
Status: Target MVP contract
Linked LLD: [Marketplace Module LLD](../../lld/phase-1/MARKETPLACE_SERVICE_LLD.md)

## Authentication and Authorization

- Firebase bearer token required;
- active verified tenant membership required;
- tenant and owner are resolved server-side;
- `marketplace_enabled` tenant flag required;
- blocked-user, target-state, moderation, and rate-limit checks apply.

## Endpoints

| Method | Path | Idempotency |
|---|---|---|
| `GET` | `/v1/market/listings` | n/a |
| `GET` | `/v1/market/listings/{id}` | n/a |
| `POST` | `/v1/market/listings` | required |
| `PATCH` | `/v1/market/listings/{id}` | version required |
| `POST` | `/v1/market/listings/{id}/reserve` | required |
| `POST` | `/v1/market/listings/{id}/sold` | required |
| `DELETE` | `/v1/market/listings/{id}` | idempotent |
| `PUT/DELETE` | `/v1/market/listings/{id}/save` | idempotent |
| `GET` | `/v1/market/requests` | n/a |
| `GET` | `/v1/market/requests/{id}` | n/a |
| `POST` | `/v1/market/requests` | required |
| `PATCH` | `/v1/market/requests/{id}` | version required |
| `POST` | `/v1/market/requests/{id}/fulfilled` | required |
| `DELETE` | `/v1/market/requests/{id}` | idempotent |
| `POST` | `/v1/market/contact` | required |

## Browse Contract

Query parameters:

```text
cursor: opaque, optional
limit: 1..50, default 20
kind: sale|wanted|lend, optional
category: approved category slug, optional
condition: approved condition slug, optional
minPrice/maxPrice: integer minor units, optional
q: normalized 2..80 characters, optional
sort: newest, default and only MVP value
```

Response:

```json
{
  "data": {
    "items": [],
    "nextCursor": "opaque-or-null"
  },
  "meta": {
    "requestId": "uuid"
  }
}
```

The response never contains seller email, phone, precise address, or internal moderation fields.

## Create Listing

```json
{
  "title": "Engineering calculator",
  "description": "Working condition with cover.",
  "category": "electronics",
  "condition": "good",
  "priceMinor": 120000,
  "currencyCode": "INR",
  "campusSpot": "Main library gate",
  "mediaUploadIds": ["uuid"]
}
```

Validation:

- title 5–120 characters;
- description 10–2,000 characters;
- `priceMinor` positive and within configured cap;
- one to four ready image uploads;
- campus spot selected from a tenant allowlist or sanitized text;
- category/condition in server allowlists.

## Contact

```json
{
  "targetType": "listing",
  "targetId": "uuid",
  "messageTemplate": "is_available"
}
```

Response returns `conversationId`. The backend resolves the recipient from the target. The first contact cannot include arbitrary free text.

## Error Codes

- `MARKET_DISABLED`;
- `MARKET_LISTING_NOT_FOUND`;
- `MARKET_REQUEST_NOT_FOUND`;
- `MARKET_TARGET_UNAVAILABLE`;
- `MARKET_SELF_CONTACT`;
- `MARKET_BLOCKED_USER`;
- `MARKET_PUBLISH_LIMIT`;
- `MARKET_CONTACT_LIMIT`;
- `MARKET_MEDIA_NOT_READY`;
- `MARKET_PROHIBITED_CATEGORY`;
- `VERSION_CONFLICT`;
- `IDEMPOTENCY_CONFLICT`.

Errors use the system standard envelope and hide cross-tenant existence.

## Side Effects

- create/edit/state transitions write Marketplace-owned rows;
- contact calls Chat through its application interface;
- publish, contact, sold, report, and moderation actions append audit/activity events;
- notifications are added to the transactional outbox;
- no payment, wallet, or settlement side effect exists.

## Compatibility

The existing `GET/POST /v1/market` endpoint remains temporarily available as a facade. It must internally call the same application layer and may not maintain a separate repository implementation.
