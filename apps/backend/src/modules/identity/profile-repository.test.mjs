import test from "node:test";
import assert from "node:assert/strict";
import { getUsernameAvailability, normalizeUsername } from "./profile-repository.mjs";

test("normalizeUsername accepts the shared profile format", () => {
  assert.equal(normalizeUsername("vyb.student_1"), "vyb.student_1");
  assert.equal(normalizeUsername("UPPERCASE"), "uppercase");
  assert.equal(normalizeUsername("ab"), null);
});

test("username availability fails closed before querying without valid scope", async () => {
  assert.deepEqual(
    await getUsernameAvailability({
      tenantId: null,
      userId: null,
      username: "vyb.student"
    }),
    {
      username: "vyb.student",
      available: false,
      isCurrent: false
    }
  );
});
