import assert from "node:assert/strict";
import test from "node:test";
import { isValidSocialUploadMimeType } from "./index.mjs";

test("avatar uploads accept images and reject videos", () => {
  assert.equal(isValidSocialUploadMimeType("avatar", "image/jpeg"), true);
  assert.equal(isValidSocialUploadMimeType("avatar", " IMAGE/PNG "), true);
  assert.equal(isValidSocialUploadMimeType("avatar", "video/mp4"), false);
});

test("post, story, and vibe uploads retain their supported media flexibility", () => {
  assert.equal(isValidSocialUploadMimeType("post", "video/mp4"), true);
  assert.equal(isValidSocialUploadMimeType("story", "video/webm"), true);
  assert.equal(isValidSocialUploadMimeType("vibe", "video/mp4"), true);
});

test("all upload intents reject missing MIME types", () => {
  assert.equal(isValidSocialUploadMimeType("post", ""), false);
  assert.equal(isValidSocialUploadMimeType("avatar", null), false);
});
