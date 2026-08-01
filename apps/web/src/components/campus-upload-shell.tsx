"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { Reorder, useDragControls } from "framer-motion";
import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type CSSProperties,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from "react";
import type {
  CampusUploadKind,
  CampusUploadMediaKind,
} from "../lib/campus-upload-store";
import { formatBytes } from "../lib/social-media-client";
import { enqueueBackgroundPublish } from "../lib/background-publish";
import {
  createComposerDraftId,
  deleteComposerDraft,
  getComposerDraft,
  listComposerDrafts,
  saveComposerDraft,
  summarizeComposerDraft,
  type ComposerDraftRecord,
  type ComposerDraftSummary,
} from "../lib/composer-drafts";
import {
  STORY_MUSIC_CLIP_OPTIONS,
  STORY_MUSIC_DEFAULT_CLIP_SECONDS,
  searchStoryMusicTracks,
  type StoryMusicStickerPosition,
  type StoryMusicTrack,
} from "../lib/story-music";
import { CampusAvatarContent } from "./campus-avatar";
import { buildPrimaryCampusNav, CampusDesktopNavigation } from "./campus-navigation";
import {
  StoryBuilder,
  type StoryOverlayMetadata,
} from "./story-builder";

/* ─── Types ─────────────────────────────────────────────────────────────── */
type CampusUploadShellProps = {
  collegeName: string;
  viewerEmail: string;
  viewerName: string;
  viewerUsername: string;
  communities?: Array<{ id: string; name: string; type: string }>;
};

type CreationMode = "choice" | "story" | "vibe" | "moment";
type PublishableCreationMode = Exclude<CreationMode, "choice">;
type PostVisibility = "public" | "followers" | "community";

type StoryComposerAsset = {
  id: string;
  url: string;
  file: File;
  kind: "image" | "video";
  durationSeconds: number | null;
  overlayMetadata?: StoryOverlayMetadata | null;
  compositionJson?: string | null;
};

/* ─── Constants ─────────────────────────────────────────────────────────── */
const MAX_IMAGE_BYTES = 4 * 1024 * 1024;
const MAX_VIDEO_BYTES = 40 * 1024 * 1024;
const MAX_POST_MEDIA_ITEMS = 8;
const STORY_IMAGE_DURATION_SECONDS = 15;
const STORY_MAX_TOTAL_SECONDS = 60;
const STORY_MAX_IMAGES = STORY_MAX_TOTAL_SECONDS / STORY_IMAGE_DURATION_SECONDS;
const VIBE_TARGET_ASPECT_RATIO = 9 / 16;
const VIBE_ASPECT_RATIO_TOLERANCE = 0.08;

const CREATION_MODE_OPTIONS: Array<{ value: PublishableCreationMode; label: string }> = [
  { value: "moment", label: "Post" },
  { value: "story", label: "Story" },
  { value: "vibe", label: "Vibe" }
];

function layoutStyle() {
  return {
    "--vyb-campus-left-width": "260px"
  } as CSSProperties;
}

/* ─── Icon components ────────────────────────────────────────────────────── */
function Ico({ children }: { children: ReactNode }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="cs-icon">
      {children}
    </svg>
  );
}

function IcoClose() {
  return (
    <Ico>
      <path
        d="m7 7 10 10M17 7 7 17"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function IcoVideo() {
  return (
    <Ico>
      <path
        d="M5 6.5A2.5 2.5 0 0 1 7.5 4H14a2.5 2.5 0 0 1 2.5 2.5v1.2l3.5-2.1v12.8l-3.5-2.1v1.2A2.5 2.5 0 0 1 14 20H7.5A2.5 2.5 0 0 1 5 17.5z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function IcoImage() {
  return (
    <Ico>
      <path
        d="M4 6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5v11a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 17.5zm0 9 4.5-4.5 3 3 4.5-5.5 4 5"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="9" cy="9" r="1.6" fill="currentColor" />
    </Ico>
  );
}

function IcoMusic() {
  return (
    <Ico>
      <path
        d="M15 5v9.2a2.8 2.8 0 1 1-1.8-2.63V7.3L8 8.5V16a2.8 2.8 0 1 1-1.8-2.63V6.9z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function IcoSearch() {
  return (
    <Ico>
      <path
        d="m21 21-4.35-4.35M10.8 18a7.2 7.2 0 1 1 0-14.4 7.2 7.2 0 0 1 0 14.4Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function IcoUpload() {
  return (
    <Ico>
      <path
        d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function IcoPlus() {
  return (
    <Ico>
      <path
        d="M12 5v14M5 12h14"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.1"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function IcoChevronDown() {
  return (
    <Ico>
      <path
        d="m6 9 6 6 6-6"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function IcoSettings() {
  return (
    <Ico>
      <circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path
        d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.05.05-2.83 2.83-.05-.05A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 .6 1.7 1.7 0 0 0-.4 1.1V21h-4v-.08A1.7 1.7 0 0 0 8.5 19.4a1.7 1.7 0 0 0-1.88.34l-.05.05-2.83-2.83.05-.05A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-.6-1 1.7 1.7 0 0 0-1.1-.4H3v-4h.08A1.7 1.7 0 0 0 4.6 8.5a1.7 1.7 0 0 0-.34-1.88l-.05-.05 2.83-2.83.05.05A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-.6 1.7 1.7 0 0 0 .4-1.1V3h4v.08A1.7 1.7 0 0 0 15.5 4.6a1.7 1.7 0 0 0 1.88-.34l.05-.05 2.83 2.83-.05.05A1.7 1.7 0 0 0 19.4 9c.15.37.36.7.6 1 .3.3.7.4 1.1.4h.08v4h-.08A1.7 1.7 0 0 0 19.4 15Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.45"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function IcoTrash() {
  return (
    <Ico>
      <path
        d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </Ico>
  );
}

function toDateTimeLocalValue(value: string | null) {
  const date = value ? new Date(value) : new Date(Date.now() + 60 * 60 * 1000);
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function fromDateTimeLocalValue(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

type MomentImage = { id: string; url: string; file: File };

function MomentReorderItem({
  image,
  index,
  total,
  disabled,
  onSelect,
  onEdit,
  onRemove,
}: {
  image: MomentImage;
  index: number;
  total: number;
  disabled: boolean;
  onSelect: () => void;
  onEdit: () => void;
  onRemove: () => void;
}) {
  const dragControls = useDragControls();
  return (
    <Reorder.Item
      as="div"
      value={image}
      className="cs-moment-img-thumb"
      dragListener={false}
      dragControls={dragControls}
      whileDrag={{ scale: 1.06, zIndex: 12, boxShadow: "0 18px 42px rgba(0,0,0,.42)" }}
      onClick={onSelect}
    >
      <img src={image.url} alt={`Upload ${index + 1}`} draggable={false} />
      <button
        type="button"
        className="cs-moment-img-drag"
        aria-label={`Hold and drag media ${index + 1} to reorder. ${index + 1} of ${total}`}
        disabled={disabled}
        onPointerDown={(event) => {
          event.stopPropagation();
          dragControls.start(event);
        }}
      >
        ⋮⋮
      </button>
      <button type="button" className="cs-moment-img-edit" onClick={(event) => { event.stopPropagation(); onEdit(); }} aria-label={`Edit image ${index + 1}`}>
        ✎
      </button>
      <button type="button" className="cs-moment-img-remove" onClick={(event) => { event.stopPropagation(); onRemove(); }} aria-label={`Remove image ${index + 1}`}>
        <IcoTrash />
      </button>
    </Reorder.Item>
  );
}

/* ─── Helpers ────────────────────────────────────────────────────────────── */
function getInitials(name: string, username: string) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" style={{ width: "100%", height: "100%", opacity: 0.55 }}>
      <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
    </svg>
  );
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value));
}

function formatDuration(seconds: number) {
  const total = Math.max(1, Math.round(seconds));
  const m = Math.floor(total / 60);
  const s = total % 60;
  return `${m}:${String(s).padStart(2, "0")}`;
}

function isVibeAspectRatio(width: number, height: number) {
  if (width <= 0 || height <= 0 || height <= width) {
    return false;
  }

  return Math.abs(width / height - VIBE_TARGET_ASPECT_RATIO) <= VIBE_ASPECT_RATIO_TOLERANCE;
}

function loadVideoMetadata(file: File) {
  return new Promise<{ duration: number; height: number; width: number }>(
    (resolve, reject) => {
      const url = URL.createObjectURL(file);
      const v = document.createElement("video");
      const cleanup = () => {
        v.removeAttribute("src");
        v.load();
        URL.revokeObjectURL(url);
      };
      v.preload = "metadata";
      v.onloadedmetadata = () => {
        resolve({ duration: v.duration, height: v.videoHeight, width: v.videoWidth });
        cleanup();
      };
      v.onerror = () => { cleanup(); reject(new Error("Unable to read this video.")); };
      v.src = url;
    }
  );
}

function parseKind(value: string | null): CampusUploadKind {
  if (value === "story" || value === "vibe") return value;
  return "post";
}

function makeComposerAssetId() {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }

  return `asset-${Date.now()}-${Math.round(Math.random() * 1_000_000)}`;
}

function getModeLabel(mode: CreationMode) {
  if (mode === "choice") {
    return "Creation Studio";
  }

  return CREATION_MODE_OPTIONS.find((option) => option.value === mode)?.label ?? "Post";
}

function getPublishLabel(mode: PublishableCreationMode) {
  if (mode === "vibe") {
    return "Publish Vibe";
  }

  if (mode === "story") {
    return "Publish Story";
  }

  return "Publish Post";
}

/* ─── Shimmer skeleton ───────────────────────────────────────────────────── */
/* ─── Progress bar ───────────────────────────────────────────────────────── */
/* ══════════════════════════════════════════════════════════════════════════
   CampusUploadShell — main export
   ══════════════════════════════════════════════════════════════════════════ */
export function CampusUploadShell({
  collegeName,
  viewerEmail,
  viewerName,
  viewerUsername,
  communities = [],
}: CampusUploadShellProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const defaultKind = parseKind(searchParams.get("kind"));
  const draftParam = searchParams.get("draft") ?? "";
  const returnTo =
    searchParams.get("from") || (defaultKind === "vibe" ? "/vibes" : "/home");

  /* ── Creation mode (choice / vibe / moment) ──────────────────────────── */
  const [mode, setMode] = useState<CreationMode>(() =>
    defaultKind === "vibe"
      ? "vibe"
      : defaultKind === "story"
        ? "story"
        : defaultKind === "post"
          ? "moment"
          : "choice"
  );

  /* ── Form state ──────────────────────────────────────────────────────── */
  const [caption, setCaption] = useState(draftParam);
  const [message, setMessage] = useState<string | null>(null);
  const [isPublishing, setIsPublishing] = useState(false);
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [allowAnonymousComments, setAllowAnonymousComments] = useState(true);
  const [postVisibility, setPostVisibility] = useState<PostVisibility>("public");
  const [communityId, setCommunityId] = useState("");
  const [isPostSettingsOpen, setIsPostSettingsOpen] = useState(false);
  const [isUtilityMenuOpen, setIsUtilityMenuOpen] = useState(false);
  const [isScheduleMenuOpen, setIsScheduleMenuOpen] = useState(false);
  const [scheduledFor, setScheduledFor] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<ComposerDraftSummary[]>([]);
  const [activeDraftId, setActiveDraftId] = useState<string | null>(null);
  const [isDraftManagerOpen, setIsDraftManagerOpen] = useState(false);
  const [isDraftBusy, setIsDraftBusy] = useState(false);
  const activeDraftIdRef = useRef<string | null>(null);
  const skipPageHideDraftRef = useRef(false);

  /* ── Vibe (single video) ─────────────────────────────────────────────── */
  const [vibeVideoUrl, setVibeVideoUrl] = useState<string | null>(null);
  const [vibeVideoFile, setVibeVideoFile] = useState<File | null>(null);
  const [vibeDuration, setVibeDuration] = useState<number | null>(null);
  const [vibeIsPortrait, setVibeIsPortrait] = useState<boolean | null>(null);
  const [vibeIsDragOver, setVibeIsDragOver] = useState(false);
  const vibeInputRef = useRef<HTMLInputElement | null>(null);
  const vibeVideoRef = useRef<HTMLVideoElement | null>(null);

  /* ── Story / Moment media ────────────────────────────────────────────── */
  const [storyAssets, setStoryAssets] = useState<StoryComposerAsset[]>([]);
  const [activeStoryAssetId, setActiveStoryAssetId] = useState<string | null>(null);
  const [momentImages, setMomentImages] = useState<MomentImage[]>([]);
  const [momentPreviewIndex, setMomentPreviewIndex] = useState(0);
  const effectiveMomentPreviewIndex = Math.max(0, Math.min(momentPreviewIndex, momentImages.length - 1));
  const [editingMomentImageId, setEditingMomentImageId] = useState<string | null>(null);
  const [isStoryMusicLibraryOpen, setIsStoryMusicLibraryOpen] = useState(false);
  const [isStoryBuilderOpen, setIsStoryBuilderOpen] = useState(false);

  const [storyMusicQuery, setStoryMusicQuery] = useState("");
  const [storyMusicTracks, setStoryMusicTracks] = useState<StoryMusicTrack[]>([]);
  const [isStoryMusicLoading, setIsStoryMusicLoading] = useState(false);
  const [storyMusicTrack, setStoryMusicTrack] = useState<StoryMusicTrack | null>(null);
  const [storyMusicClipDurationSeconds, setStoryMusicClipDurationSeconds] = useState(
    STORY_MUSIC_DEFAULT_CLIP_SECONDS
  );
  const [storyMusicTrimSeconds, setStoryMusicTrimSeconds] = useState(0);
  const [storyMusicStatus, setStoryMusicStatus] = useState<string | null>(null);
  const [storyMusicStickerPosition, setStoryMusicStickerPosition] = useState<StoryMusicStickerPosition>({
    x: 0.18,
    y: 0.72
  });
  const [isDraggingMusicSticker, setIsDraggingMusicSticker] = useState(false);
  const [isStoryMusicPreviewPlaying, setIsStoryMusicPreviewPlaying] = useState(false);
  const [storyMusicPreviewCurrentTime, setStoryMusicPreviewCurrentTime] = useState(0);
  const momentInputRef = useRef<HTMLInputElement | null>(null);
  const storyPreviewRef = useRef<HTMLDivElement | null>(null);
  const storyMusicPreviewRef = useRef<HTMLAudioElement | null>(null);
  const storyMusicPreviewTimeoutRef = useRef<number | null>(null);
  const stickerDragOffsetRef = useRef({ x: 0, y: 0 });
  const latestVibeVideoUrlRef = useRef<string | null>(null);
  const latestStoryAssetsRef = useRef<StoryComposerAsset[]>([]);
  const latestMomentImagesRef = useRef<MomentImage[]>([]);

  /* ── Derived ─────────────────────────────────────────────────────────── */
  const avatarInitials = useMemo(
    () => getInitials(viewerName, viewerUsername),
    [viewerName, viewerUsername]
  );
  const canPostAnonymously = mode === "vibe" || mode === "moment";
  const composerDisplayName = canPostAnonymously && isAnonymous ? "Anonymous Vyber" : viewerName;
  const composerUsername = canPostAnonymously && isAnonymous ? "anonymous" : viewerUsername;
  const composerInitials = canPostAnonymously && isAnonymous ? "AN" : avatarInitials;

  const activeStoryAsset = useMemo(
    () => storyAssets.find((asset) => asset.id === activeStoryAssetId) ?? storyAssets[0] ?? null,
    [activeStoryAssetId, storyAssets]
  );
  const activeEditorAsset = useMemo(() => {
    if (mode === "story") return activeStoryAsset;
    const image = momentImages.find((item) => item.id === editingMomentImageId);
    return image ? { ...image, kind: "image" as const } : null;
  }, [activeStoryAsset, editingMomentImageId, mode, momentImages]);

  const canAddStoryMusic = mode === "story" && storyAssets.length === 1;
  const storyMusicTrimMax = useMemo(() => {
    if (!storyMusicTrack) {
      return 0;
    }

    return Math.max(0, Math.floor(storyMusicTrack.durationSeconds - storyMusicClipDurationSeconds));
  }, [storyMusicClipDurationSeconds, storyMusicTrack]);

  const storyMusicPreviewEndSeconds = useMemo(() => {
    return storyMusicTrimSeconds + storyMusicClipDurationSeconds;
  }, [storyMusicClipDurationSeconds, storyMusicTrimSeconds]);

  const canPublish = useMemo(() => {
    if (mode === "vibe") {
      return Boolean(vibeVideoUrl);
    }
    if (mode === "story") {
      return storyAssets.length > 0;
    }
    if (mode === "moment") {
      return Boolean(caption.trim() || momentImages.length > 0);
    }
    return false;
  }, [mode, vibeVideoUrl, caption, momentImages, storyAssets]);

  const scheduleOptions = useMemo(() => {
    const now = new Date();
    const inOneHour = new Date(now.getTime() + 60 * 60 * 1000);
    const evening = new Date(now);
    evening.setHours(20, 0, 0, 0);
    if (evening.getTime() <= now.getTime()) {
      evening.setDate(evening.getDate() + 1);
    }
    const tomorrowMorning = new Date(now);
    tomorrowMorning.setDate(tomorrowMorning.getDate() + 1);
    tomorrowMorning.setHours(9, 0, 0, 0);
    return [
      { label: "In 1 hour", value: inOneHour.toISOString() },
      { label: "Evening", value: evening.toISOString() },
      { label: "Tomorrow morning", value: tomorrowMorning.toISOString() }
    ];
  }, []);

  /* ── progress simulator for demo (real upload doesn't expose events) ─── */
  useEffect(() => {
    if (!draftParam) {
      return;
    }

    setCaption((current) => (current.trim().length > 0 ? current : draftParam));
  }, [draftParam]);

  useEffect(() => {
    let cancelled = false;

    async function loadDraftShelf() {
      try {
        const legacyKey = `vybnet-post-draft:${viewerUsername}`;
        const legacyRaw = window.localStorage.getItem(legacyKey);
        if (legacyRaw) {
          const legacy = JSON.parse(legacyRaw) as {
            caption?: string;
            mode?: PublishableCreationMode;
            isAnonymous?: boolean;
            allowAnonymousComments?: boolean;
            postVisibility?: PostVisibility;
            communityId?: string;
            scheduledFor?: string | null;
            savedAt?: string;
          };
          const legacyMode = legacy.mode === "story" || legacy.mode === "vibe" ? legacy.mode : "moment";
          const migrated: ComposerDraftRecord = {
            id: createComposerDraftId(),
            owner: viewerUsername,
            mode: legacyMode,
            caption: legacy.caption ?? "",
            savedAt: legacy.savedAt ?? new Date().toISOString(),
            scheduledFor: legacy.scheduledFor ?? null,
            isAnonymous: Boolean(legacy.isAnonymous),
            allowAnonymousComments: legacy.allowAnonymousComments !== false,
            postVisibility:
              legacy.postVisibility === "followers" || legacy.postVisibility === "community"
                ? legacy.postVisibility
                : "public",
            communityId: legacy.communityId ?? "",
            momentAssets: [],
            storyAssets: [],
            storyMusic: null,
            vibeVideoFile: null,
          };
          if (migrated.caption.trim()) {
            await saveComposerDraft(migrated);
          }
          window.localStorage.removeItem(legacyKey);
        }

        const stored = await listComposerDrafts(viewerUsername);
        if (!cancelled) {
          setDrafts(stored.map(summarizeComposerDraft));
        }
      } catch {
        if (!cancelled) setMessage("Local drafts could not be read on this browser.");
      }
    }

    void loadDraftShelf();
    return () => {
      cancelled = true;
    };
  }, [viewerUsername]);

  useEffect(() => {
    function saveOnUnexpectedExit() {
      if (skipPageHideDraftRef.current || isPublishing || !canPublish) return;
      void persistCurrentDraft();
      try {
        window.sessionStorage.setItem("vybnet-composer-notice", "saved as draft");
      } catch {
        // The draft itself is persisted in IndexedDB; the notice is best-effort.
      }
    }

    window.addEventListener("pagehide", saveOnUnexpectedExit);
    window.addEventListener("popstate", saveOnUnexpectedExit);
    return () => {
      window.removeEventListener("pagehide", saveOnUnexpectedExit);
      window.removeEventListener("popstate", saveOnUnexpectedExit);
    };
  }, [
    allowAnonymousComments,
    canPublish,
    caption,
    communityId,
    isAnonymous,
    isPublishing,
    mode,
    momentImages,
    postVisibility,
    scheduledFor,
    storyAssets,
    storyMusicClipDurationSeconds,
    storyMusicStickerPosition,
    storyMusicTrack,
    storyMusicTrimSeconds,
    vibeVideoFile,
    viewerUsername,
  ]);

  useEffect(() => {
    if (storyAssets.length === 0) {
      setActiveStoryAssetId(null);
      if (storyMusicTrack) {
        setStoryMusicTrack(null);
        setStoryMusicClipDurationSeconds(STORY_MUSIC_DEFAULT_CLIP_SECONDS);
        setStoryMusicTrimSeconds(0);
        setStoryMusicStatus(null);
      }
      return;
    }

    if (!storyAssets.some((asset) => asset.id === activeStoryAssetId)) {
      setActiveStoryAssetId(storyAssets[0]?.id ?? null);
    }

    if (storyAssets.length > 1 && storyMusicTrack) {
      setStoryMusicTrack(null);
      setStoryMusicClipDurationSeconds(STORY_MUSIC_DEFAULT_CLIP_SECONDS);
      setStoryMusicTrimSeconds(0);
      setStoryMusicStatus("Music export works with one story item at a time right now.");
    }
  }, [activeStoryAssetId, storyAssets, storyMusicTrack]);

  useEffect(() => {
    setStoryMusicTrimSeconds((current) => clamp(current, 0, storyMusicTrimMax));
  }, [storyMusicTrimMax]);

  useEffect(() => {
    return () => {
      if (storyMusicPreviewTimeoutRef.current !== null) {
        window.clearTimeout(storyMusicPreviewTimeoutRef.current);
      }
      storyMusicPreviewRef.current?.pause();
    };
  }, []);

  useEffect(() => {
    let ignore = false;
    const controller = new AbortController();
    const id = window.setTimeout(async () => {
      if (!isStoryMusicLibraryOpen) {
        return;
      }

      setIsStoryMusicLoading(true);
      try {
        const items = await searchStoryMusicTracks(storyMusicQuery);
        if (!ignore) {
          setStoryMusicTracks(items);
        }
      } catch (error) {
        if (!ignore) {
          setStoryMusicTracks([]);
          setStoryMusicStatus(error instanceof Error ? error.message : "We could not load the music library.");
        }
      } finally {
        if (!ignore) {
          setIsStoryMusicLoading(false);
        }
      }
    }, 220);

    return () => {
      ignore = true;
      controller.abort();
      window.clearTimeout(id);
    };
  }, [isStoryMusicLibraryOpen, storyMusicQuery]);

  useEffect(() => {
    latestVibeVideoUrlRef.current = vibeVideoUrl;
    latestStoryAssetsRef.current = storyAssets;
    latestMomentImagesRef.current = momentImages;
  }, [momentImages, storyAssets, vibeVideoUrl]);

  useEffect(() => {
    return () => {
      const latestVibeVideoUrl = latestVibeVideoUrlRef.current;
      if (latestVibeVideoUrl?.startsWith("blob:")) {
        URL.revokeObjectURL(latestVibeVideoUrl);
      }

      latestStoryAssetsRef.current.forEach((asset) => {
        if (asset.url.startsWith("blob:")) {
          URL.revokeObjectURL(asset.url);
        }
      });

      latestMomentImagesRef.current.forEach((entry) => {
        if (entry.url.startsWith("blob:")) {
          URL.revokeObjectURL(entry.url);
        }
      });
    };
  }, []);

  /* ── Helpers ─────────────────────────────────────────────────────────── */
  function hasDraftContent() {
    return caption.trim().length > 0
      || momentImages.length > 0
      || storyAssets.length > 0
      || Boolean(vibeVideoFile);
  }

  function buildCurrentDraft(id = activeDraftIdRef.current ?? createComposerDraftId()): ComposerDraftRecord | null {
    if (mode === "choice" || !hasDraftContent()) return null;

    return {
      id,
      owner: viewerUsername,
      mode,
      caption,
      savedAt: new Date().toISOString(),
      scheduledFor,
      isAnonymous: mode === "story" ? false : isAnonymous,
      allowAnonymousComments,
      postVisibility,
      communityId,
      momentAssets: momentImages.map((asset) => ({ id: asset.id, file: asset.file })),
      storyAssets: storyAssets.map((asset) => ({
        id: asset.id,
        file: asset.file,
        kind: asset.kind,
        durationSeconds: asset.durationSeconds,
        overlayMetadata: asset.overlayMetadata ?? null,
        compositionJson: asset.compositionJson ?? null,
      })),
      storyMusic: storyMusicTrack
        ? {
            track: storyMusicTrack,
            clipDurationSeconds: storyMusicClipDurationSeconds,
            trimSeconds: storyMusicTrimSeconds,
            stickerPosition: storyMusicStickerPosition,
          }
        : null,
      vibeVideoFile,
    };
  }

  async function refreshDraftShelf() {
    const stored = await listComposerDrafts(viewerUsername);
    setDrafts(stored.map(summarizeComposerDraft));
  }

  async function persistCurrentDraft() {
    const draft = buildCurrentDraft();
    if (!draft) return null;

    await saveComposerDraft(draft);
    activeDraftIdRef.current = draft.id;
    setActiveDraftId(draft.id);
    await refreshDraftShelf();
    return draft.id;
  }

  function revokeComposerObjectUrls() {
    if (vibeVideoUrl?.startsWith("blob:")) URL.revokeObjectURL(vibeVideoUrl);
    storyAssets.forEach((asset) => {
      if (asset.url.startsWith("blob:")) URL.revokeObjectURL(asset.url);
    });
    momentImages.forEach((asset) => {
      if (asset.url.startsWith("blob:")) URL.revokeObjectURL(asset.url);
    });
  }

  async function handleLoadDraft(id: string) {
    setIsDraftBusy(true);
    try {
      const draft = await getComposerDraft(id);
      if (!draft || draft.owner !== viewerUsername) {
        setMessage("That local draft is no longer available.");
        await refreshDraftShelf();
        return;
      }

      revokeComposerObjectUrls();
      activeDraftIdRef.current = draft.id;
      setActiveDraftId(draft.id);
      setMode(draft.mode);
      setCaption(draft.caption);
      setScheduledFor(draft.scheduledFor);
      setIsAnonymous(draft.mode === "story" ? false : draft.isAnonymous);
      setAllowAnonymousComments(draft.allowAnonymousComments);
      setPostVisibility(draft.postVisibility);
      setCommunityId(draft.communityId);
      setMomentImages(draft.momentAssets.map((asset) => ({
        id: asset.id,
        file: asset.file,
        url: URL.createObjectURL(asset.file),
      })));
      setMomentPreviewIndex(0);
      setStoryAssets(draft.storyAssets.map((asset) => ({
        id: asset.id,
        file: asset.file,
        url: URL.createObjectURL(asset.file),
        kind: asset.kind,
        durationSeconds: asset.durationSeconds,
        overlayMetadata: (asset.overlayMetadata ?? null) as StoryOverlayMetadata | null,
        compositionJson: asset.compositionJson ?? null,
      })));
      setActiveStoryAssetId(draft.storyAssets[0]?.id ?? null);
      const storedMusic = draft.storyMusic;
      setStoryMusicTrack((storedMusic?.track ?? null) as StoryMusicTrack | null);
      setStoryMusicClipDurationSeconds(storedMusic?.clipDurationSeconds ?? STORY_MUSIC_DEFAULT_CLIP_SECONDS);
      setStoryMusicTrimSeconds(storedMusic?.trimSeconds ?? 0);
      setStoryMusicStickerPosition(storedMusic?.stickerPosition ?? { x: 0.18, y: 0.72 });

      if (draft.vibeVideoFile) {
        const url = URL.createObjectURL(draft.vibeVideoFile);
        setVibeVideoFile(draft.vibeVideoFile);
        setVibeVideoUrl(url);
        void loadVideoMetadata(draft.vibeVideoFile).then((metadata) => {
          setVibeDuration(metadata.duration);
          setVibeIsPortrait(isVibeAspectRatio(metadata.width, metadata.height));
        }).catch(() => {
          setVibeDuration(null);
          setVibeIsPortrait(null);
        });
      } else {
        setVibeVideoFile(null);
        setVibeVideoUrl(null);
        setVibeDuration(null);
        setVibeIsPortrait(null);
      }

      setIsDraftManagerOpen(false);
      setIsUtilityMenuOpen(false);
      setMessage("Draft loaded.");
    } finally {
      setIsDraftBusy(false);
    }
  }

  async function handleDiscardDraft(id: string) {
    setIsDraftBusy(true);
    try {
      await deleteComposerDraft(id);
      if (activeDraftIdRef.current === id) {
        activeDraftIdRef.current = null;
        setActiveDraftId(null);
      }
      await refreshDraftShelf();
    } finally {
      setIsDraftBusy(false);
    }
  }

  async function handleClose() {
    if (hasDraftContent() && !isPublishing) {
      await persistCurrentDraft();
      window.sessionStorage.setItem("vybnet-composer-notice", "saved as draft");
    }
    skipPageHideDraftRef.current = true;
    router.replace(returnTo);
  }

  function handleDiscardCreation() {
    skipPageHideDraftRef.current = true;
    router.replace(returnTo);
  }

  async function handleSaveDraft() {
    setIsDraftBusy(true);
    try {
      const saved = await persistCurrentDraft();
      if (saved) {
        window.sessionStorage.setItem("vybnet-composer-notice", "saved as draft");
      }
      skipPageHideDraftRef.current = true;
      router.replace(returnTo);
    } finally {
      setIsDraftBusy(false);
    }
  }

  function handleModeChange(nextMode: PublishableCreationMode) {
    stopStoryMusicPreview();
    setIsStoryMusicLibraryOpen(false);
    setIsPostSettingsOpen(false);
    setMessage(null);
    if (nextMode === "story") {
      setIsAnonymous(false);
      setAllowAnonymousComments(true);
    }
    setMode(nextMode);
  }

  /* ── Vibe video pick ─────────────────────────────────────────────────── */
  async function processVibeFile(file: File) {
    if (!file.type.startsWith("video/")) {
      setMessage("Vibes only accept video files.");
      return;
    }
    setMessage(null);

    try {
      const objectUrl = URL.createObjectURL(file);
      if (vibeVideoUrl?.startsWith("blob:")) URL.revokeObjectURL(vibeVideoUrl);
      setVibeVideoUrl(objectUrl);
      setVibeVideoFile(file);
      setVibeDuration(null);
      setVibeIsPortrait(null);

      const meta = await loadVideoMetadata(file);
      const isPortrait = isVibeAspectRatio(meta.width, meta.height);
      setVibeDuration(meta.duration);
      setVibeIsPortrait(isPortrait);
      setMessage(
        file.size > MAX_VIDEO_BYTES
            ? `Large video detected (${formatBytes(file.size)}). We'll optimize it in background after you post.`
            : isPortrait
              ? "Portrait video selected. It will fill the Vibes feed nicely."
              : "Landscape or square video selected. It will be shown fully without cropping."
      );
    } catch (err) {
      setMessage(err instanceof Error ? err.message : "Could not load video preview.");
    }
  }

  function handleVibeInputChange(e: ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (file) void processVibeFile(file);
  }

  function handleVibeDrop(e: React.DragEvent<HTMLDivElement>) {
    e.preventDefault();
    setVibeIsDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) void processVibeFile(file);
  }

  /* ── Story / Moment media pick ──────────────────────────────────────── */
  async function handleStoryInputChange(e: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    e.target.value = "";

    const valid = files.filter(
      (file) => file.type.startsWith("image/") || file.type.startsWith("video/")
    );

    if (valid.length === 0) {
      setMessage("Stories support image and video files only.");
      return;
    }

    const videoFile = valid.find((file) => file.type.startsWith("video/"));
    if (videoFile) {
      if (valid.length > 1 || storyAssets.length > 0) {
        setMessage("Story video works one clip at a time. Remove other media first.");
        return;
      }

      setMessage(null);

      try {
        const meta = await loadVideoMetadata(videoFile);
        const entry: StoryComposerAsset = {
          id: makeComposerAssetId(),
          url: URL.createObjectURL(videoFile),
          file: videoFile,
          kind: "video",
          durationSeconds: meta.duration
        };

        setStoryAssets([entry]);
        setActiveStoryAssetId(entry.id);
        setStoryMusicStatus(
          videoFile.size > MAX_VIDEO_BYTES
            ? `Large story video detected (${formatBytes(videoFile.size)}). We'll optimize it in background after you post.`
            : "Music stories export a 15-second MP4 clip."
        );
      } catch (error) {
        setMessage(error instanceof Error ? error.message : "We could not load this story video.");
      }
      return;
    }

    const imageFiles = valid.filter((file) => file.type.startsWith("image/"));
    const hasVideoStory = storyAssets.some((asset) => asset.kind === "video");
    if (hasVideoStory) {
      setMessage("Remove the current story video before adding photos.");
      return;
    }

    const availableSlots = Math.max(0, STORY_MAX_IMAGES - storyAssets.length);
    const nextFiles = imageFiles.slice(0, availableSlots);
    const entries = nextFiles.map((file) => ({
      id: makeComposerAssetId(),
      url: URL.createObjectURL(file),
      file,
      kind: "image" as const,
      durationSeconds: STORY_IMAGE_DURATION_SECONDS
    }));

    setStoryAssets((prev) => [...prev, ...entries]);
    setActiveStoryAssetId((current) => current ?? entries[0]?.id ?? null);
    setMessage(
      imageFiles.length > availableSlots
        ? `Stories support up to ${STORY_MAX_IMAGES} photos so the full sequence stays within ${STORY_MAX_TOTAL_SECONDS} seconds.`
        : null
    );
  }

  function handleMomentInputChange(e: ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    e.target.value = "";
    const valid = files.filter((file) => file.type.startsWith("image/"));
    if (valid.length === 0) {
      setMessage("Only image files are supported for posts.");
      return;
    }

    const availableSlots = Math.max(0, MAX_POST_MEDIA_ITEMS - momentImages.length);
    const nextFiles = valid.slice(0, availableSlots);
    const entries = nextFiles.map((file) => ({
      id: makeComposerAssetId(),
      url: URL.createObjectURL(file),
      file
    }));

    setMomentImages((prev) => [...prev, ...entries]);
    setMessage(
      valid.length > availableSlots
        ? `Posts support up to ${MAX_POST_MEDIA_ITEMS} photos.`
        : null
    );
  }

  function removeStoryAsset(id: string) {
    setStoryAssets((prev) => {
      const next = prev.filter((asset) => asset.id !== id);
      const removed = prev.find((asset) => asset.id === id);
      if (removed?.url.startsWith("blob:")) {
        URL.revokeObjectURL(removed.url);
      }
      return next;
    });
  }

  function applyStoryBuilderResult(result: {
    file: File;
    overlayMetadata: StoryOverlayMetadata | null;
    compositionJson: string | null;
  }) {
    if (mode === "moment" && editingMomentImageId) {
      setMomentImages((images) => images.map((image) => {
        if (image.id !== editingMomentImageId) return image;
        if (image.url.startsWith("blob:")) URL.revokeObjectURL(image.url);
        return { ...image, file: result.file, url: URL.createObjectURL(result.file) };
      }));
      setEditingMomentImageId(null);
      setIsStoryBuilderOpen(false);
      setMessage("Media edits applied.");
      return;
    }
    if (!activeStoryAsset) return;
    setStoryAssets((assets) =>
      assets.map((asset) => {
        if (asset.id !== activeStoryAsset.id) return asset;
        if (asset.url.startsWith("blob:")) URL.revokeObjectURL(asset.url);
        return {
          ...asset,
          file: result.file,
          url: URL.createObjectURL(result.file),
          overlayMetadata: result.overlayMetadata,
          compositionJson: result.compositionJson,
        };
      }),
    );
    setIsStoryBuilderOpen(false);
    setStoryMusicStatus(
      activeStoryAsset.kind === "image"
        ? "Story design flattened and ready to publish."
        : "Video overlay design saved as editable metadata.",
    );
  }

  function removeMomentImage(index: number) {
    setMomentImages((prev) => {
      const next = [...prev];
      const removed = next.splice(index, 1)[0];
      if (removed?.url.startsWith("blob:")) URL.revokeObjectURL(removed.url);
      return next;
    });
  }

  function openStoryMusicLibrary() {
    if (storyAssets.length === 0) {
      setMessage("Pick one story photo or video before adding music.");
      return;
    }

    if (storyAssets.length !== 1) {
      setMessage("Music export currently works with one story photo or video at a time.");
      return;
    }

    setStoryMusicStatus(null);
    setIsStoryMusicLibraryOpen(true);
  }

  function stopStoryMusicPreview() {
    if (storyMusicPreviewTimeoutRef.current !== null) {
      window.clearTimeout(storyMusicPreviewTimeoutRef.current);
      storyMusicPreviewTimeoutRef.current = null;
    }
    if (storyMusicPreviewRef.current) {
      storyMusicPreviewRef.current.pause();
    }
    setIsStoryMusicPreviewPlaying(false);
  }

  async function playSelectedStoryMusicClip() {
    if (!storyMusicTrack || !storyMusicPreviewRef.current) {
      return;
    }

    stopStoryMusicPreview();
    const audio = storyMusicPreviewRef.current;
    audio.currentTime = storyMusicTrimSeconds;

    try {
      await audio.play();
      setIsStoryMusicPreviewPlaying(true);
      storyMusicPreviewTimeoutRef.current = window.setTimeout(() => {
        stopStoryMusicPreview();
      }, storyMusicClipDurationSeconds * 1000);
    } catch (error) {
      setStoryMusicStatus(
        error instanceof Error ? error.message : "We could not play this song preview right now."
      );
    }
  }

  function selectStoryMusicTrack(track: StoryMusicTrack) {
    stopStoryMusicPreview();
    setStoryMusicTrack(track);
    setStoryMusicClipDurationSeconds(STORY_MUSIC_DEFAULT_CLIP_SECONDS);
    setStoryMusicTrimSeconds(0);
    setStoryMusicPreviewCurrentTime(0);
    setStoryMusicStatus(`Selected ${track.title} by ${track.artistName}.`);
    setIsStoryMusicLibraryOpen(false);
  }

  function handleStoryStickerPointerDown(event: ReactPointerEvent<HTMLButtonElement>) {
    if (!storyPreviewRef.current) {
      return;
    }

    const previewRect = storyPreviewRef.current.getBoundingClientRect();
    const currentX = storyMusicStickerPosition.x * previewRect.width;
    const currentY = storyMusicStickerPosition.y * previewRect.height;
    stickerDragOffsetRef.current = {
      x: event.clientX - currentX,
      y: event.clientY - currentY
    };
    setIsDraggingMusicSticker(true);
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function handleStoryStickerPointerMove(event: ReactPointerEvent<HTMLButtonElement>) {
    if (!isDraggingMusicSticker || !storyPreviewRef.current) {
      return;
    }

    const previewRect = storyPreviewRef.current.getBoundingClientRect();
    const nextX = (event.clientX - previewRect.left - stickerDragOffsetRef.current.x) / previewRect.width;
    const nextY = (event.clientY - previewRect.top - stickerDragOffsetRef.current.y) / previewRect.height;
    setStoryMusicStickerPosition({
      x: clamp(nextX, 0.05, 0.78),
      y: clamp(nextY, 0.08, 0.82)
    });
  }

  function handleStoryStickerPointerUp(event: ReactPointerEvent<HTMLButtonElement>) {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    setIsDraggingMusicSticker(false);
  }

  /* ── Publish ────────────────────────────────────────────────────────── */
  async function handlePublish(nextScheduledFor: string | null = null) {
    if (mode === "vibe") {
      if (!vibeVideoFile) {
        setMessage("Add a video before posting.");
        return;
      }
    }

    if (mode === "moment" && !caption.trim() && momentImages.length === 0) {
      setMessage("Add a caption or photo before posting.");
      return;
    }
    if (postVisibility === "community" && !communityId) {
      setMessage("Choose a community before publishing.");
      setIsPostSettingsOpen(true);
      return;
    }
    if (mode === "story" && storyAssets.length === 0) {
      setMessage("Add at least one photo or video before posting your Story.");
      return;
    }

    setIsPublishing(true);
    setMessage("Posting will continue in background.");

    try {
      if (mode === "vibe" && vibeVideoFile) {
        enqueueBackgroundPublish({
          kind: "vibe",
          caption,
          collegeName,
          videoFile: vibeVideoFile,
          isAnonymous,
          allowAnonymousComments,
          visibility: postVisibility,
          communityId: postVisibility === "community" ? communityId : null
        }, { scheduledFor: nextScheduledFor });
      } else if (mode === "story") {
        enqueueBackgroundPublish({
          kind: "story",
          caption,
          storyAssets: storyAssets.map((asset) => ({
            file: asset.file,
            mediaType: asset.kind,
            mimeType: asset.file.type || null,
            compositionJson: asset.compositionJson ?? null
          })),
          storyMusic:
            storyMusicTrack && activeStoryAsset
              ? {
                  visualFile: activeStoryAsset.file,
                  visualKind: activeStoryAsset.kind,
                  track: storyMusicTrack,
                  clipDurationSeconds: storyMusicClipDurationSeconds,
                  trimStartSeconds: storyMusicTrimSeconds,
                  stickerPosition: storyMusicStickerPosition
                }
                : null,
          allowAnonymousComments,
          visibility: postVisibility,
          communityId: postVisibility === "community" ? communityId : null
        }, { scheduledFor: nextScheduledFor });
      } else {
        enqueueBackgroundPublish({
          kind: "post",
          caption,
          collegeName,
          mediaFiles: momentImages.map((asset) => asset.file),
          isAnonymous,
          allowAnonymousComments,
          visibility: postVisibility,
          communityId: postVisibility === "community" ? communityId : null
        }, { scheduledFor: nextScheduledFor });
      }

      const publishedDraftId = activeDraftIdRef.current;
      if (publishedDraftId) {
        await deleteComposerDraft(publishedDraftId);
        activeDraftIdRef.current = null;
        setActiveDraftId(null);
      }
      window.localStorage.removeItem(`vybnet-post-draft:${viewerUsername}`);
      skipPageHideDraftRef.current = true;
      router.replace(returnTo);
    } catch (err) {
      setIsPublishing(false);
      setMessage(err instanceof Error ? err.message : "Could not queue this post right now.");
    }
  }

  /* ════════════════════════════════════════════════════════════════════════
     RENDER
     ════════════════════════════════════════════════════════════════════════ */
  const navItems = useMemo(() => buildPrimaryCampusNav("home", { profileHref: "/dashboard" }), []);

  return (
    <main className="vyb-campus-home cs-route-shell" style={layoutStyle()}>
      <CampusDesktopNavigation navItems={navItems} viewerName={viewerName} viewerUsername={viewerUsername} />

      <section className="vyb-campus-main cs-route-main">
    <div className="cs-overlay">
      {/* backdrop blur */}
      <div className="cs-backdrop" onClick={handleClose} aria-hidden="true" />

      <div
        className={`cs-shell cs-shell--${mode}`}
        role="dialog"
        aria-modal="true"
        aria-label="Creation Studio"
      >
        {/* ── Header ────────────────────────────────────────────────────── */}
        <div className="cs-header">
          <div className="cs-header-brand">
            <span className="cs-header-label">{getModeLabel(mode)}</span>
          </div>

          <div className="cs-header-actions">
            {mode !== "choice" && (
              <div className="cs-mode-switch">
                <select
                  className="cs-mode-select"
                  value={mode}
                  onChange={(event) => handleModeChange(event.target.value as PublishableCreationMode)}
                  disabled={isPublishing}
                  aria-label="Choose what to create"
                >
                  {CREATION_MODE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
                <span className="cs-mode-select-chevron" aria-hidden="true">
                  <IcoChevronDown />
                </span>
              </div>
            )}
            {mode !== "choice" && (
              <button
                type="button"
                className={`cs-settings-btn${isPostSettingsOpen ? " is-active" : ""}`}
                onClick={() => setIsPostSettingsOpen(true)}
                disabled={isPublishing}
                aria-label={`${getModeLabel(mode)} settings`}
                aria-haspopup="dialog"
                aria-expanded={isPostSettingsOpen}
              >
                <IcoSettings />
              </button>
            )}
            <button type="button" className="cs-close-btn" onClick={handleClose} aria-label="Close">
              <IcoClose />
            </button>
          </div>
        </div>

        {/* ── Body ──────────────────────────────────────────────────────── */}

        {/* ─── CHOICE SCREEN ─────────────────────────────────────────── */}
        {mode === "choice" && (
          <div className="cs-choice-screen">
            <p className="cs-choice-sub">What do you want to share today?</p>
            <div className="cs-choice-cards">

              <button
                type="button"
                className="cs-choice-card cs-choice-card--moment"
                onClick={() => setMode("story")}
              >
                <div className="cs-choice-card-glow cs-choice-card-glow--moment" />
                <div className="cs-choice-icon-wrap cs-choice-icon-wrap--moment">
                  <IcoImage />
                </div>
                <strong className="cs-choice-title">Story</strong>
                <span className="cs-choice-desc">Photo sequence for your campus story ring</span>
                <span className="cs-choice-badge cs-choice-badge--teal">Story</span>
              </button>

              <button
                type="button"
                className="cs-choice-card cs-choice-card--vibe"
                onClick={() => setMode("vibe")}
              >
                <div className="cs-choice-card-glow cs-choice-card-glow--vibe" />
                <div className="cs-choice-icon-wrap cs-choice-icon-wrap--vibe">
                  <IcoVideo />
                </div>
                <strong className="cs-choice-title">Vibe</strong>
                <span className="cs-choice-desc">Portrait, landscape, or square video</span>
                <span className="cs-choice-badge">Video</span>
              </button>

              <button
                type="button"
                className="cs-choice-card cs-choice-card--moment"
                onClick={() => setMode("moment")}
              >
                <div className="cs-choice-card-glow cs-choice-card-glow--moment" />
                <div className="cs-choice-icon-wrap cs-choice-icon-wrap--moment">
                  <IcoImage />
                </div>
                <strong className="cs-choice-title">Post</strong>
                <span className="cs-choice-desc">Photo or text · Campus feed post</span>
                <span className="cs-choice-badge cs-choice-badge--teal">Photo / Text</span>
              </button>

            </div>
          </div>
        )}

        {/* ─── VIBE SCREEN ───────────────────────────────────────────── */}
        {mode === "vibe" && (
          <div className="cs-vibe-screen cs-vibe-screen--composer">
            {/* Left: 9:16 video area */}
            <div className="cs-vibe-left">
              <div
                className={`cs-vibe-dropzone${vibeIsDragOver ? " cs-vibe-dropzone--drag" : ""}${vibeVideoUrl ? " cs-vibe-dropzone--has-media" : ""}`}
                onDragOver={(e) => { e.preventDefault(); setVibeIsDragOver(true); }}
                onDragLeave={() => setVibeIsDragOver(false)}
                onDrop={handleVibeDrop}
                onClick={() => !vibeVideoUrl && vibeInputRef.current?.click()}
              >
                {vibeVideoUrl ? (
                  <>
                    <video
                      ref={vibeVideoRef}
                      src={vibeVideoUrl}
                      className="cs-vibe-preview-video"
                      autoPlay
                      loop
                      muted
                      playsInline
                    />
                    <div className="cs-vibe-video-overlay">
                      <button
                        type="button"
                        className="cs-vibe-replace-btn"
                        onClick={(e) => { e.stopPropagation(); vibeInputRef.current?.click(); }}
                      >
                        Replace video
                      </button>
                      {vibeDuration && (
                        <span className="cs-vibe-duration">{formatDuration(vibeDuration)}</span>
                      )}
                    </div>
                    {vibeIsPortrait === false && (
                      <div className="cs-vibe-warning">
                        Full video will fit without cropping
                      </div>
                    )}
                  </>
                ) : (
                  <div className="cs-vibe-empty">
                    <div className="cs-vibe-empty-icon">
                      <IcoUpload />
                    </div>
                    <strong>Drop your video here</strong>
                    <span>or click to browse</span>
                    <span className="cs-vibe-empty-hint">MP4 / MOV / WEBM · Max 40 MB</span>
                  </div>
                )}
              </div>
              <input
                ref={vibeInputRef}
                type="file"
                accept="video/*"
                className="cs-file-input"
                disabled={isPublishing}
                onChange={handleVibeInputChange}
              />
            </div>

            {/* Right: caption + details */}
            <div className="cs-vibe-right">
              {/* User pill */}
              <div className="cs-user-row">
                  <div className="cs-avatar" aria-hidden="true">
                    <CampusAvatarContent
                      username={composerUsername}
                      email={isAnonymous ? null : viewerEmail}
                      displayName={composerDisplayName}
                      fallback={composerInitials}
                      decorative
                    />
                  </div>
                <div className="cs-user-info">
                  <strong>{composerDisplayName}</strong>
                  <span>@{composerUsername}</span>
                </div>
              </div>

              {/* Caption */}
              <div className="cs-caption-wrap">
                <textarea
                  className="cs-caption-area"
                  value={caption}
                  onChange={(e) => setCaption(e.target.value)}
                  placeholder="Tell everyone what this vibe is about… #campus @friends"
                  rows={6}
                  disabled={isPublishing}
                />
              </div>

              {/* Meta info */}
              {vibeVideoFile && (
                <div className="cs-vibe-meta">
                  <span>{formatBytes(vibeVideoFile.size)}</span>
                  {vibeDuration && <span>{formatDuration(vibeDuration)}</span>}
                  <span className="cs-meta-ok">
                    {vibeIsPortrait === false ? "Auto-fit" : vibeIsPortrait ? "Portrait" : "-"}
                  </span>
                </div>
              )}

              {message && <p className="cs-message">{message}</p>}
            </div>
          </div>
        )}

        {/* ─── STORY / MOMENT SCREEN ─────────────────────────────────── */}
        {(mode === "story" || mode === "moment") && (
          <div className="cs-moment-screen">
            <div className="cs-user-row cs-user-row--moment">
                <div className="cs-avatar" aria-hidden="true">
                   <CampusAvatarContent
                    username={composerUsername}
                    email={isAnonymous ? null : viewerEmail}
                    displayName={composerDisplayName}
                    fallback={composerInitials}
                    decorative
                  />
                </div>
              <div className="cs-user-info">
                <strong>{composerDisplayName}</strong>
                <span>@{composerUsername}</span>
              </div>
            </div>

            {mode === "story" ? (
              <>
                <div className="cs-moment-caption-wrap">
                  <textarea
                    className="cs-moment-caption"
                    value={caption}
                    onChange={(event) => setCaption(event.target.value)}
                    placeholder="Add a caption for your story..."
                    rows={4}
                    disabled={isPublishing}
                  />
                </div>

                <div className="cs-story-editor">
                  <div className="cs-story-preview-shell">
                    <div className="cs-story-preview-stage" ref={storyPreviewRef}>
                      {activeStoryAsset ? (
                        <>
                          {activeStoryAsset.kind === "video" ? (
                            <video
                              src={activeStoryAsset.url}
                              className="cs-story-preview-media"
                              autoPlay
                              loop
                              muted
                              playsInline
                            />
                          ) : (
                            <img
                              src={activeStoryAsset.url}
                              alt="Story preview"
                              className="cs-story-preview-media"
                            />
                          )}
                          <div className="cs-story-preview-gradient" />
                          <div className="cs-story-preview-meta">
                            <span>{activeStoryAsset.kind === "video" ? "Story clip" : "Story photo"}</span>
                            <strong>
                              {activeStoryAsset.kind === "video"
                                ? `${Math.min(
                                    storyMusicTrack
                                      ? storyMusicClipDurationSeconds
                                      : Math.max(
                                          1,
                                          Math.round(
                                            activeStoryAsset.durationSeconds ?? STORY_MUSIC_DEFAULT_CLIP_SECONDS
                                          )
                                        ),
                                    Math.max(
                                      1,
                                      Math.round(
                                        activeStoryAsset.durationSeconds ?? STORY_MUSIC_DEFAULT_CLIP_SECONDS
                                      )
                                    )
                                  )}s`
                                : `${storyMusicTrack ? storyMusicClipDurationSeconds : STORY_IMAGE_DURATION_SECONDS}s`}
                            </strong>
                          </div>
                          <button
                            type="button"
                            className="cs-story-music-trigger"
                            onClick={openStoryMusicLibrary}
                            disabled={isPublishing}
                          >
                            <IcoMusic />
                            <span>{storyMusicTrack ? "Change music" : "Add music"}</span>
                          </button>
                          <button
                            type="button"
                            className="cs-story-builder-trigger"
                            onClick={() => setIsStoryBuilderOpen(true)}
                            disabled={isPublishing}
                          >
                            Edit Story
                          </button>
                          {storyMusicTrack && (
                            <button
                              type="button"
                              className={`cs-story-music-sticker${isDraggingMusicSticker ? " cs-story-music-sticker--dragging" : ""}`}
                              style={{
                                left: `${storyMusicStickerPosition.x * 100}%`,
                                top: `${storyMusicStickerPosition.y * 100}%`
                              }}
                              onPointerDown={handleStoryStickerPointerDown}
                              onPointerMove={handleStoryStickerPointerMove}
                              onPointerUp={handleStoryStickerPointerUp}
                              onPointerCancel={handleStoryStickerPointerUp}
                            >
                              <IcoMusic />
                              <span>{storyMusicTrack.title}</span>
                              <small>{storyMusicTrack.artistName}</small>
                            </button>
                          )}
                        </>
                      ) : (
                        <button
                          type="button"
                          className="cs-story-preview-empty"
                          onClick={() => momentInputRef.current?.click()}
                        >
                          <div className="cs-story-preview-empty-icon">
                            <IcoPlus />
                          </div>
                          <strong>Build your story scene</strong>
                          <span>Add a photo or a short video, then layer music on top.</span>
                        </button>
                      )}
                    </div>

                    {storyMusicTrack && (
                        <div className="cs-story-music-panel">
                        <audio
                          ref={storyMusicPreviewRef}
                          className="cs-story-music-audio"
                          src={storyMusicTrack.streamUrl}
                          preload="metadata"
                          controls
                          onPlay={() => setIsStoryMusicPreviewPlaying(true)}
                          onPause={() => setIsStoryMusicPreviewPlaying(false)}
                          onEnded={stopStoryMusicPreview}
                          onTimeUpdate={(event) => {
                            setStoryMusicPreviewCurrentTime(event.currentTarget.currentTime);
                          }}
                        />
                        <div className="cs-story-music-panel-head">
                          <div>
                            <strong>{storyMusicTrack.title}</strong>
                            <span>{storyMusicTrack.artistName}</span>
                          </div>
                          <button
                            type="button"
                            className="cs-story-music-reset"
                            onClick={() => {
                              stopStoryMusicPreview();
                              setStoryMusicTrack(null);
                              setStoryMusicClipDurationSeconds(STORY_MUSIC_DEFAULT_CLIP_SECONDS);
                              setStoryMusicTrimSeconds(0);
                              setStoryMusicStatus("Music removed from this story.");
                            }}
                          >
                            Remove
                          </button>
                        </div>
                        <div className="cs-story-music-clip-options" role="group" aria-label="Choose clip duration">
                          {STORY_MUSIC_CLIP_OPTIONS.filter((seconds) => seconds <= storyMusicTrack.durationSeconds).map((seconds) => (
                            <button
                              key={seconds}
                              type="button"
                              className={`cs-story-music-clip-chip${storyMusicClipDurationSeconds === seconds ? " is-active" : ""}`}
                              onClick={() => {
                                stopStoryMusicPreview();
                                setStoryMusicClipDurationSeconds(seconds);
                              }}
                            >
                              {seconds}s
                            </button>
                          ))}
                        </div>
                        <div className="cs-story-music-preview-actions">
                          <button
                            type="button"
                            className={`cs-story-music-preview-btn${isStoryMusicPreviewPlaying ? " is-active" : ""}`}
                            onClick={() => {
                              if (isStoryMusicPreviewPlaying) {
                                stopStoryMusicPreview();
                                return;
                              }
                              void playSelectedStoryMusicClip();
                            }}
                          >
                            {isStoryMusicPreviewPlaying ? "Stop selected clip" : "Play selected clip"}
                          </button>
                          <span className="cs-story-music-preview-meta">
                            {formatDuration(storyMusicTrimSeconds)} to {formatDuration(storyMusicPreviewEndSeconds)}
                          </span>
                        </div>
                        <label className="cs-story-trim-wrap">
                          <span>Pick the {storyMusicClipDurationSeconds}s song window</span>
                          <input
                            type="range"
                            min={0}
                            max={storyMusicTrimMax}
                            step={1}
                            value={storyMusicTrimSeconds}
                            onChange={(event) => {
                              stopStoryMusicPreview();
                              setStoryMusicTrimSeconds(Number(event.target.value));
                            }}
                            className="cs-story-trim-slider"
                          />
                          <div className="cs-story-trim-meta">
                            <span>Start at {formatDuration(storyMusicTrimSeconds)}</span>
                            <span>{storyMusicClipDurationSeconds}s clip</span>
                          </div>
                        </label>
                        <span className="cs-story-music-playback-readout">
                          Live preview: {formatDuration(Math.floor(storyMusicPreviewCurrentTime))} / {formatDuration(storyMusicTrack.durationSeconds)}
                        </span>
                      </div>
                    )}
                  </div>

                  <div className="cs-story-editor-side">
                    <div className="cs-moment-images cs-moment-images--story">
                      {storyAssets.map((asset) => (
                        <div
                          key={asset.id}
                          className={`cs-moment-img-thumb cs-moment-img-thumb--story${activeStoryAsset?.id === asset.id ? " cs-moment-img-thumb--active" : ""}`}
                        >
                          <button
                            type="button"
                            className="cs-story-thumb-select"
                            onClick={() => setActiveStoryAssetId(asset.id)}
                          >
                            {asset.kind === "video" ? (
                              <video src={asset.url} muted playsInline />
                            ) : (
                              <img src={asset.url} alt="Story asset" />
                            )}
                            <span className="cs-story-thumb-badge">{asset.kind === "video" ? "Video" : "Photo"}</span>
                          </button>
                          <button
                            type="button"
                            className="cs-moment-img-remove"
                            onClick={() => removeStoryAsset(asset.id)}
                            aria-label="Remove story media"
                          >
                            <IcoTrash />
                          </button>
                        </div>
                      ))}
                      {storyAssets.length > 0 && storyAssets.length < STORY_MAX_IMAGES && !storyAssets.some((asset) => asset.kind === "video") && (
                        <button
                          type="button"
                          className="cs-moment-img-add"
                          onClick={() => momentInputRef.current?.click()}
                          aria-label="Add story media"
                        >
                          <IcoPlus />
                          <span>{storyAssets.length === 0 ? "Add story" : "More"}</span>
                        </button>
                      )}
                      {storyAssets.length === 0 && (
                        <button
                          type="button"
                          className="cs-moment-img-add"
                          onClick={() => momentInputRef.current?.click()}
                          aria-label="Add story media"
                        >
                          <IcoPlus />
                          <span>Add story</span>
                        </button>
                      )}
                    </div>
                  </div>
                </div>

                {storyMusicStatus && <p className="cs-story-music-status">{storyMusicStatus}</p>}
                {message && <p className="cs-message">{message}</p>}

                <input
                  ref={momentInputRef}
                  type="file"
                  accept="image/*,video/*"
                  multiple
                  className="cs-file-input"
                  disabled={isPublishing}
                  onChange={handleStoryInputChange}
                />
              </>
            ) : (
              <>
                <div className="cs-moment-caption-wrap">
                  <textarea
                    className="cs-moment-caption"
                    value={caption}
                    onChange={(e) => setCaption(e.target.value)}
                    placeholder="What's on your mind? #hashtag @mention"
                    rows={5}
                    disabled={isPublishing}
                  />
                </div>

                {momentImages.length > 0 ? (
                  <div className="cs-moment-carousel-preview" aria-label="Post media preview carousel">
                    <img src={momentImages[effectiveMomentPreviewIndex]?.url} alt={`Selected media ${effectiveMomentPreviewIndex + 1}`} />
                    <span>{effectiveMomentPreviewIndex + 1}/{momentImages.length}</span>
                    {momentImages.length > 1 ? (
                      <>
                        <button type="button" className="is-prev" disabled={effectiveMomentPreviewIndex === 0} onClick={() => setMomentPreviewIndex(Math.max(0, effectiveMomentPreviewIndex - 1))} aria-label="Previous media">‹</button>
                        <button type="button" className="is-next" disabled={effectiveMomentPreviewIndex === momentImages.length - 1} onClick={() => setMomentPreviewIndex(Math.min(momentImages.length - 1, effectiveMomentPreviewIndex + 1))} aria-label="Next media">›</button>
                      </>
                    ) : null}
                  </div>
                ) : null}

                <Reorder.Group as="div" axis="x" values={momentImages} onReorder={setMomentImages} className="cs-moment-images" aria-label="Selected post media order">
                  {momentImages.map((img, i) => (
                    <MomentReorderItem
                      key={img.id}
                      image={img}
                      index={i}
                      total={momentImages.length}
                      disabled={isPublishing}
                      onSelect={() => setMomentPreviewIndex(i)}
                      onEdit={() => {
                          setEditingMomentImageId(img.id);
                          setIsStoryBuilderOpen(true);
                      }}
                      onRemove={() => removeMomentImage(i)}
                    />
                  ))}
                  {momentImages.length < MAX_POST_MEDIA_ITEMS && (
                    <button
                      type="button"
                      className="cs-moment-img-add"
                      onClick={() => momentInputRef.current?.click()}
                      aria-label="Add photo"
                    >
                      <IcoPlus />
                      <span>{momentImages.length === 0 ? "Add photo" : "More"}</span>
                    </button>
                  )}
                </Reorder.Group>

                {message && <p className="cs-message">{message}</p>}

                <input
                  ref={momentInputRef}
                  type="file"
                  accept="image/*"
                  multiple
                  className="cs-file-input"
                  disabled={isPublishing}
                  onChange={handleMomentInputChange}
                />
              </>
            )}
          </div>
        )}

        {mode === "story" && isStoryMusicLibraryOpen && (
          <div className="cs-story-music-modal" role="dialog" aria-modal="true" aria-label="Music library">
            <button
              type="button"
              className="cs-story-music-backdrop"
              onClick={() => setIsStoryMusicLibraryOpen(false)}
              aria-label="Close music library"
            />
            <div className="cs-story-music-dialog">
              <div className="cs-story-music-dialog-head">
                <div>
                  <strong>Music library</strong>
                  <span>Royalty-free tracks for your next story drop.</span>
                </div>
                <button
                  type="button"
                  className="cs-story-music-close"
                  onClick={() => setIsStoryMusicLibraryOpen(false)}
                  aria-label="Close music library"
                >
                  <IcoClose />
                </button>
              </div>

              <label className="cs-story-music-search">
                <IcoSearch />
                <input
                  type="search"
                  value={storyMusicQuery}
                  onChange={(event) => setStoryMusicQuery(event.target.value)}
                  placeholder="Search by song or artist"
                />
              </label>

              <div className="cs-story-music-results">
                {isStoryMusicLoading ? (
                  <p className="cs-story-music-empty">Loading tracks...</p>
                ) : storyMusicTracks.length === 0 ? (
                  <p className="cs-story-music-empty">No tracks found yet. Try another search.</p>
                ) : (
                  storyMusicTracks.map((track) => (
                    <button
                      type="button"
                      key={track.id}
                      className="cs-story-music-item"
                      onClick={() => selectStoryMusicTrack(track)}
                    >
                      <div className="cs-story-music-item-art">
                        {track.artworkUrl ? (
                          <img src={track.artworkUrl} alt="" />
                        ) : (
                          <IcoMusic />
                        )}
                      </div>
                      <div className="cs-story-music-item-copy">
                        <strong>{track.title}</strong>
                        <span>{track.artistName}</span>
                      </div>
                      <span className="cs-story-music-item-duration">
                        {formatDuration(track.durationSeconds)}
                      </span>
                    </button>
                  ))
                )}
              </div>
            </div>
          </div>
        )}

        {/* ── Footer (always visible in story/vibe/moment) ───────────────── */}
        {mode !== "choice" && isPostSettingsOpen && (
          <div
            className="cs-settings-backdrop"
            role="presentation"
            onMouseDown={(event) => {
              if (event.currentTarget === event.target) setIsPostSettingsOpen(false);
            }}
          >
            <section className="cs-settings-dialog" role="dialog" aria-modal="true" aria-labelledby="cs-post-settings-title">
              <div className="cs-settings-dialog-head">
                <div>
                  <span>Publishing controls</span>
                  <h2 id="cs-post-settings-title">{getModeLabel(mode)} settings</h2>
                </div>
                <button type="button" onClick={() => setIsPostSettingsOpen(false)} aria-label="Close post settings">
                  <IcoClose />
                </button>
              </div>

              <div className="cs-settings-toggle-list">
                {mode !== "story" && (
                  <label>
                    <span>
                      <strong>Publish anonymously</strong>
                      <small>Your name and profile stay hidden on this {mode === "vibe" ? "vibe" : "post"}.</small>
                    </span>
                    <input type="checkbox" role="switch" checked={isAnonymous}
                      disabled={isPublishing}
                      onChange={(event) => setIsAnonymous(event.target.checked)} />
                  </label>
                )}
                {mode === "story" ? (
                  <p className="cs-settings-empty">Stories always show the verified publisher.</p>
                ) : (
                  <label>
                    <span>
                      <strong>Allow anonymous comments</strong>
                      <small>Readers can choose to hide their identity when replying.</small>
                    </span>
                    <input type="checkbox" role="switch" checked={allowAnonymousComments}
                      disabled={isPublishing}
                      onChange={(event) => setAllowAnonymousComments(event.target.checked)} />
                  </label>
                )}
              </div>

              <fieldset className="cs-reach-options">
                <legend>Who can see this {mode === "moment" ? "post" : mode}?</legend>
                {([
                  ["public", "Public", "Everyone in your verified campus network"],
                  ["followers", "Followers only", "Only people who follow you"],
                  ["community", "Community only", "Members of one selected community"],
                ] as const).map(([value, title, description]) => (
                  <label key={value} className={postVisibility === value ? "is-active" : ""}>
                    <input type="radio" name="post-visibility" value={value}
                      checked={postVisibility === value}
                      disabled={isPublishing || (value === "community" && communities.length === 0)}
                      onChange={() => {
                        setPostVisibility(value);
                      }} />
                    <span><strong>{title}</strong><small>{description}</small></span>
                  </label>
                ))}
              </fieldset>

              {postVisibility === "community" && (
                <label className="cs-community-select">
                  <span>Choose community</span>
                  <select value={communityId} onChange={(event) => setCommunityId(event.target.value)} disabled={isPublishing}>
                    <option value="">Select a joined community</option>
                    {communities.map((community) => (
                      <option key={community.id} value={community.id}>{community.name} · {community.type}</option>
                    ))}
                  </select>
                </label>
              )}
              {communities.length === 0 && <p className="cs-settings-empty">Join a community to enable community-only reach.</p>}
              <button type="button" className="cs-settings-done"
                disabled={postVisibility === "community" && !communityId}
                onClick={() => setIsPostSettingsOpen(false)}>Done</button>
            </section>
          </div>
        )}

        {isStoryBuilderOpen && activeEditorAsset && (
          <StoryBuilder
            asset={activeEditorAsset}
            purpose={mode === "moment" ? "post" : "story"}
            onApply={applyStoryBuilderResult}
            onClose={() => {
              setEditingMomentImageId(null);
              setIsStoryBuilderOpen(false);
            }}
          />
        )}

        {mode !== "choice" && (
          <div className="cs-footer">
            <div className="cs-footer-hint">
                {mode === "vibe"
                  ? "Portrait, landscape, and square videos publish with no-crop playback"
                : mode === "story"
                  ? "Stories support photos or one video · music clips can export at 15s, 30s, 45s, or 60s"
                  : `Up to ${MAX_POST_MEDIA_ITEMS} photos · Text-only posts are fine too`}
            </div>
            <div className="cs-footer-actions">
              <div className="cs-utility-wrap">
                <button
                  type="button"
                  className="cs-utility-trigger"
                  onClick={() => setIsUtilityMenuOpen((current) => !current)}
                  disabled={isPublishing || (!canPublish && drafts.length === 0)}
                  aria-label="Open post utilities"
                  aria-expanded={isUtilityMenuOpen}
                >
                  <IcoUtility />
                  {drafts.length > 0 ? <span className="cs-draft-count">{drafts.length > 99 ? "99+" : drafts.length}</span> : null}
                </button>
                {isUtilityMenuOpen ? (
                  <div className="cs-utility-menu" role="menu">
                    <button type="button" role="menuitem" onClick={handleDiscardCreation}>
                      <span className="cs-utility-action-icon cs-utility-action-icon--cancel">
                        <IcoClose />
                      </span>
                      <span><strong>Cancel creation</strong><small>Discard this editing session</small></span>
                    </button>
                    <button type="button" role="menuitem" onClick={() => void handleSaveDraft()} disabled={!canPublish || isDraftBusy}>
                      <span className="cs-utility-action-icon cs-utility-action-icon--draft">
                        <IcoDraft />
                      </span>
                      <span><strong>Save draft</strong><small>Continue this {mode === "moment" ? "post" : mode} later</small></span>
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      disabled={drafts.length === 0 || isDraftBusy}
                      onClick={() => {
                        setIsUtilityMenuOpen(false);
                        setIsDraftManagerOpen(true);
                      }}
                    >
                      <span className="cs-utility-action-icon cs-utility-action-icon--draft">
                        <IcoDraft />
                      </span>
                      <span><strong>Drafts ({drafts.length})</strong><small>Load or discard a saved draft</small></span>
                    </button>
                    <button
                      type="button"
                      role="menuitem"
                      disabled={!canPublish}
                      onClick={() => {
                        setScheduledFor(scheduleOptions[0]?.value ?? null);
                        setIsUtilityMenuOpen(false);
                        setIsScheduleMenuOpen(true);
                      }}
                    >
                      <span className="cs-utility-action-icon cs-utility-action-icon--schedule">
                        <IcoClock />
                      </span>
                      <span><strong>Schedule {mode === "moment" ? "post" : mode}</strong><small>Choose a future publish time</small></span>
                    </button>
                  </div>
                ) : null}
              </div>
              <button
                type="button"
                className={`cs-post-btn${canPublish ? " cs-post-btn--active" : ""}`}
                onClick={() => void handlePublish()}
                disabled={!canPublish || isPublishing}
              >
                {isPublishing ? "Queueing..." : `${getPublishLabel(mode)} ✦`}
              </button>
            </div>
          </div>
        )}

        {isDraftManagerOpen ? (
          <div className="cs-schedule-backdrop" role="presentation">
            <div className="cs-draft-dialog" role="dialog" aria-modal="true" aria-label={`Drafts (${drafts.length})`}>
              <div className="cs-draft-dialog-head">
                <div>
                  <span className="cs-draft-eyebrow">ON THIS DEVICE</span>
                  <h2>Drafts ({drafts.length})</h2>
                </div>
                <button type="button" className="cs-draft-close" onClick={() => setIsDraftManagerOpen(false)} aria-label="Close drafts">
                  <IcoClose />
                </button>
              </div>
              {drafts.length === 0 ? (
                <p className="cs-draft-empty">No drafts saved on this device.</p>
              ) : (
                <div className="cs-draft-list">
                  {drafts.map((draft) => (
                    <article key={draft.id} className={`cs-draft-row${activeDraftId === draft.id ? " is-active" : ""}`}>
                      <button type="button" className="cs-draft-load" disabled={isDraftBusy} onClick={() => void handleLoadDraft(draft.id)}>
                        <span className="cs-draft-mode">{draft.mode === "moment" ? "Post" : draft.mode === "story" ? "Story" : "Vibe"}</span>
                        <strong>{draft.caption.trim() || `${draft.mode === "moment" ? "Post" : draft.mode} draft`}</strong>
                        <small>
                          {new Date(draft.savedAt).toLocaleString([], { dateStyle: "medium", timeStyle: "short" })}
                          {draft.mediaCount > 0 ? ` · ${draft.mediaCount} media` : ""}
                          {draft.scheduledFor ? ` · scheduled ${new Date(draft.scheduledFor).toLocaleString([], { dateStyle: "short", timeStyle: "short" })}` : ""}
                        </small>
                      </button>
                      <button type="button" className="cs-draft-delete" disabled={isDraftBusy} onClick={() => void handleDiscardDraft(draft.id)} aria-label={`Discard ${draft.caption.trim() || "draft"}`}>
                        <IcoTrash />
                      </button>
                    </article>
                  ))}
                </div>
              )}
              <button type="button" className="cs-draft-done" onClick={() => setIsDraftManagerOpen(false)}>Done</button>
            </div>
          </div>
        ) : null}

        {isScheduleMenuOpen ? (
          <div className="cs-schedule-backdrop" role="presentation">
            <div className="cs-schedule-dialog" role="dialog" aria-modal="true" aria-label={`Schedule ${mode === "moment" ? "post" : mode}`}>
              <div className="cs-schedule-icon"><IcoClock /></div>
              <h2>Schedule {mode === "moment" ? "post" : mode}</h2>
              <p>Choose when Vybnet should publish this {mode === "moment" ? "post" : mode}.</p>
              <div className="cs-schedule-options">
                {scheduleOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={scheduledFor === option.value ? "is-active" : ""}
                    onClick={() => setScheduledFor(option.value)}
                  >
                    <span className="cs-schedule-dot" />
                    <span>
                      <strong>{option.label}</strong>
                      <small>
                        {new Date(option.value).toLocaleString([], {
                          dateStyle: "medium",
                          timeStyle: "short"
                        })}
                      </small>
                    </span>
                  </button>
                ))}
              </div>
              <label className="cs-schedule-custom">
                <strong>Exact date &amp; time</strong>
                <input
                  type="datetime-local"
                  min={toDateTimeLocalValue(new Date(Date.now() + 60_000).toISOString())}
                  value={toDateTimeLocalValue(scheduledFor)}
                  onChange={(event) => setScheduledFor(fromDateTimeLocalValue(event.target.value))}
                />
              </label>
              <small className="cs-schedule-note">
                Scheduled uploads resume when Vybnet is open and an internet connection is available.
              </small>
              <div className="cs-schedule-actions">
                <button type="button" onClick={() => setIsScheduleMenuOpen(false)}>Back</button>
                <button
                  type="button"
                  className="is-primary"
                  disabled={!scheduledFor || isPublishing}
                  onClick={() => void handlePublish(scheduledFor)}
                >
                  Schedule {mode === "moment" ? "Post" : mode === "story" ? "Story" : "Vibe"}
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </div>
    </div>
      </section>
    </main>
  );
}

function IcoClock() {
  return (
    <Ico>
      <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M12 7.5V12l3.2 2" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </Ico>
  );
}

function IcoUtility() {
  return (
    <Ico>
      <path
        d="M4 7h6M14 7h6M4 17h3M11 17h9M10 4v6M7 14v6"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <circle cx="12" cy="7" r="2" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="9" cy="17" r="2" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </Ico>
  );
}

function IcoDraft() {
  return (
    <Ico>
      <path d="M4.5 7.5h15v11h-15zM7 7.5 9.2 5h5.6L17 7.5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M8 12h8M8 15h5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </Ico>
  );
}
