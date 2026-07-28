# Marketplace Listing Query Review

Owner: Marketplace
Date: 2026-07-28
Status: Approved target query
Linked LLD: [Marketplace Module LLD](../../lld/phase-1/MARKETPLACE_SERVICE_LLD.md)

## Purpose

Return a stable, tenant-scoped page of active Marketplace inventory without offset scans or cross-tenant leakage.

## Query Shape

- table: `market_listings`;
- filters: `tenant_id`, `status=active`, `deleted_at is null`;
- optional filters: category, condition, price range;
- cursor: `(created_at, id)`;
- sort: `created_at desc, id desc`;
- limit: 21 rows for a 20-item page;
- media and viewer-save state are fetched in bounded second queries by returned IDs or through an reviewed aggregate query.

## Expected Scale

| Item | Assumption |
|---|---:|
| Total listings at 30k users | 150,000 historical |
| Active listings per tenant | below 15,000 |
| Returned rows | 20 |
| Peak browse rate | 100 requests/second |
| Target p95 DB time | below 100 ms |

## Supporting Indexes

```sql
create index market_listings_tenant_status_created_idx
on market_listings (tenant_id, status, created_at desc, id desc)
where deleted_at is null;

create index market_listings_tenant_category_status_created_idx
on market_listings (tenant_id, category, status, created_at desc, id desc)
where deleted_at is null;
```

If text search is enabled:

```sql
create index market_listings_search_trgm_idx
on market_listings using gin
((lower(title || ' ' || description)) gin_trgm_ops)
where deleted_at is null and status = 'active';
```

## Safety

- tenant comes from the verified membership, not a query parameter;
- deleted/removed/sold inventory is excluded;
- cursor is signed and scoped to the filter hash;
- maximum page size is 50;
- `EXPLAIN (ANALYZE, BUFFERS)` must show an index-backed plan on launch-like data;
- response composition must avoid per-row author/media/save queries.

## Degradation

If search is slow, disable free-text search with a feature flag and preserve category browsing. A database timeout returns a retryable `503`; no JSON/local fallback is allowed.
