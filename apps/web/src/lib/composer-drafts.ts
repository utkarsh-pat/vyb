"use client";

export type ComposerDraftMode = "moment" | "story" | "vibe";
export type ComposerDraftVisibility = "public" | "followers" | "community";

export type ComposerDraftMomentAsset = {
  id: string;
  file: File;
};

export type ComposerDraftStoryAsset = {
  id: string;
  file: File;
  kind: "image" | "video";
  durationSeconds: number | null;
  overlayMetadata?: unknown;
  compositionJson?: string | null;
};

export type ComposerDraftStoryMusic = {
  track: unknown;
  clipDurationSeconds: number;
  trimSeconds: number;
  stickerPosition: { x: number; y: number };
} | null;

export type ComposerDraftRecord = {
  id: string;
  owner: string;
  mode: ComposerDraftMode;
  caption: string;
  savedAt: string;
  scheduledFor: string | null;
  isAnonymous: boolean;
  allowAnonymousComments: boolean;
  postVisibility: ComposerDraftVisibility;
  communityId: string;
  momentAssets: ComposerDraftMomentAsset[];
  storyAssets: ComposerDraftStoryAsset[];
  storyMusic: ComposerDraftStoryMusic;
  vibeVideoFile: File | null;
};

export type ComposerDraftSummary = Pick<
  ComposerDraftRecord,
  "id" | "mode" | "caption" | "savedAt" | "scheduledFor"
> & { mediaCount: number };

const DATABASE_NAME = "vyb-composer-drafts";
const DATABASE_VERSION = 1;
const STORE_NAME = "drafts";
const MAX_DRAFTS_PER_OWNER = 20;

function openDraftDatabase() {
  if (typeof indexedDB === "undefined") {
    return Promise.resolve<IDBDatabase | null>(null);
  }

  return new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open(DATABASE_NAME, DATABASE_VERSION);
    request.onupgradeneeded = () => {
      const database = request.result;
      if (!database.objectStoreNames.contains(STORE_NAME)) {
        const store = database.createObjectStore(STORE_NAME, { keyPath: "id" });
        store.createIndex("owner", "owner", { unique: false });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error("Could not open the local draft store."));
  });
}

function requestResult<T>(request: IDBRequest<T>) {
  return new Promise<T>((resolve, reject) => {
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error ?? new Error("Local draft operation failed."));
  });
}

function transactionDone(transaction: IDBTransaction) {
  return new Promise<void>((resolve, reject) => {
    transaction.oncomplete = () => resolve();
    transaction.onerror = () => reject(transaction.error ?? new Error("Local draft transaction failed."));
    transaction.onabort = () => reject(transaction.error ?? new Error("Local draft transaction was cancelled."));
  });
}

export function createComposerDraftId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `draft-${Date.now()}-${Math.round(Math.random() * 1_000_000)}`;
}

export function summarizeComposerDraft(draft: ComposerDraftRecord): ComposerDraftSummary {
  return {
    id: draft.id,
    mode: draft.mode,
    caption: draft.caption,
    savedAt: draft.savedAt,
    scheduledFor: draft.scheduledFor,
    mediaCount: draft.momentAssets.length + draft.storyAssets.length + (draft.vibeVideoFile ? 1 : 0)
  };
}

export async function listComposerDrafts(owner: string) {
  const database = await openDraftDatabase();
  if (!database) return [];

  try {
    const transaction = database.transaction(STORE_NAME, "readonly");
    const index = transaction.objectStore(STORE_NAME).index("owner");
    const drafts = await requestResult(index.getAll(owner) as IDBRequest<ComposerDraftRecord[]>);
    return drafts.sort((left, right) => Date.parse(right.savedAt) - Date.parse(left.savedAt));
  } finally {
    database.close();
  }
}

export async function getComposerDraft(id: string) {
  const database = await openDraftDatabase();
  if (!database) return null;

  try {
    const transaction = database.transaction(STORE_NAME, "readonly");
    return (await requestResult(
      transaction.objectStore(STORE_NAME).get(id) as IDBRequest<ComposerDraftRecord | undefined>
    )) ?? null;
  } finally {
    database.close();
  }
}

export async function saveComposerDraft(draft: ComposerDraftRecord) {
  const database = await openDraftDatabase();
  if (!database) return;

  try {
    const write = database.transaction(STORE_NAME, "readwrite");
    write.objectStore(STORE_NAME).put(draft);
    await transactionDone(write);

    const drafts = await listComposerDrafts(draft.owner);
    const overflow = drafts.slice(MAX_DRAFTS_PER_OWNER);
    if (overflow.length > 0) {
      const cleanup = database.transaction(STORE_NAME, "readwrite");
      const store = cleanup.objectStore(STORE_NAME);
      overflow.forEach((item) => store.delete(item.id));
      await transactionDone(cleanup);
    }
  } finally {
    database.close();
  }
}

export async function deleteComposerDraft(id: string) {
  const database = await openDraftDatabase();
  if (!database) return;

  try {
    const transaction = database.transaction(STORE_NAME, "readwrite");
    transaction.objectStore(STORE_NAME).delete(id);
    await transactionDone(transaction);
  } finally {
    database.close();
  }
}
