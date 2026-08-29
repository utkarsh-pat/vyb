import assert from "node:assert/strict";
import { test } from "node:test";
import { buildDashboard } from "./repository.mjs";

function snapshot() {
  return {
    listings: [
      { id: "listing-hidden", tenantId: "tenant-1", sellerUserId: "user-b", sellerUsername: "b", sellerName: "B", sellerRole: "student", title: "Hidden", description: "", category: "Books", condition: "Good", priceAmount: 10, location: "Campus", campusSpot: "Gate", createdAt: "2026-01-02T00:00:00.000Z", status: "active" },
      { id: "listing-visible", tenantId: "tenant-1", sellerUserId: "user-c", sellerUsername: "c", sellerName: "C", sellerRole: "student", title: "Visible", description: "", category: "Books", condition: "Good", priceAmount: 20, location: "Campus", campusSpot: "Gate", createdAt: "2026-01-01T00:00:00.000Z", status: "active" }
    ],
    listingMedia: [],
    requests: [
      { id: "request-hidden", tenantId: "tenant-1", requesterUserId: "user-b", requesterUsername: "b", requesterName: "B", requesterRole: "student", title: "Hidden request", detail: "", category: "Books", campusSpot: "Gate", createdAt: "2026-01-01T00:00:00.000Z", status: "active", tab: "buying" }
    ],
    requestMedia: [],
    saves: [],
    listingContacts: [
      { id: "contact-hidden", listingId: "listing-visible", fromUserId: "user-b", toUserId: "user-c", deletedAt: null }
    ],
    requestContacts: []
  };
}

test("market dashboard hides blocked sellers, requesters and inquiry activity", () => {
  const dashboard = buildDashboard(snapshot(), {
    tenantId: "tenant-1",
    userId: "user-a",
    username: "a",
    blockedUserIds: new Set(["user-b"])
  });

  assert.deepEqual(dashboard.listings.map((item) => item.id), ["listing-visible"]);
  assert.deepEqual(dashboard.requests, []);
  assert.equal(dashboard.listings[0].inquiryCount, 0);
});
