import { getAuth } from "firebase-admin/auth";
import { getDatabase } from "firebase-admin/database";
import { getStorage } from "firebase-admin/storage";
import { getFirebaseAdminApp as getSharedFirebaseAdminApp } from "@vyb/config";

export function getFirebaseAdminApp() {
  return getSharedFirebaseAdminApp("web-auth");
}

export function getFirebaseAdminAuth() {
  return getAuth(getFirebaseAdminApp());
}

export function getFirebaseAdminDatabase() {
  return getDatabase(getFirebaseAdminApp());
}

export function getFirebaseAdminStorageBucket() {
  return getStorage(getFirebaseAdminApp()).bucket();
}
