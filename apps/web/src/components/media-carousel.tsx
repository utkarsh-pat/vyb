"use client";

import { motion, useMotionValue, useTransform, animate, AnimatePresence } from "framer-motion";
import { useEffect, useRef, useState, type ReactNode } from "react";

type MediaVariant = {
  url: string;
  mimeType?: string;
  height?: number | null;
  width?: number | null;
  label?: string | null;
};

type MediaCarouselItem = {
  url: string;
  kind: "image" | "video";
  variants?: MediaVariant[];
};

interface MediaCarouselProps {
  /** Array of media items — url + kind pairs */
  items: MediaCarouselItem[];
  /** Alt text for images */
  alt?: string;
  /** Called on double-tap (like gesture) */
  onDoubleTap?: () => void;
  /** Called on single-click (open lightbox) */
  onClick?: () => void;
  /** Post ID for heart burst */
  showHeartBurst?: boolean;
  heartBurstNode?: ReactNode;
}

export function MediaCarousel({
  items,
  alt = "Post media",
  onDoubleTap,
  onClick,
  showHeartBurst,
  heartBurstNode,
}: MediaCarouselProps) {
  const [activeIndex, setActiveIndex] = useState(0);
  const [isHovered, setIsHovered] = useState(false);
  const constraintsRef = useRef<HTMLDivElement>(null);
  const trackRef = useRef<HTMLDivElement>(null);
  const x = useMotionValue(0);
  const total = items.length;

  // Double-tap detection — only fires when user did NOT drag
  const lastTap = useRef(0);
  const dragOccurred = useRef(false);

  function handleTap() {
    // If the user swiped/dragged, suppress the tap entirely
    if (dragOccurred.current) {
      dragOccurred.current = false;
      return;
    }
    const now = Date.now();
    if (now - lastTap.current < 310) {
      onDoubleTap?.();
    } else {
      onClick?.();
    }
    lastTap.current = now;
  }

  function snapTo(index: number) {
    const clampedIndex = Math.max(0, Math.min(index, total - 1));
    setActiveIndex(clampedIndex);
    const containerWidth = constraintsRef.current?.offsetWidth ?? 0;
    void animate(x, -clampedIndex * containerWidth, {
      type: "spring",
      stiffness: 300,
      damping: 30,
    });
  }

  function handleDragStart() {
    dragOccurred.current = false;
  }

  function handleDrag(_: unknown, info: { offset: { x: number } }) {
    // Mark as drag if finger moved more than 5px horizontally
    if (Math.abs(info.offset.x) > 5) {
      dragOccurred.current = true;
    }
  }

  function handleDragEnd(_: unknown, info: { offset: { x: number }; velocity: { x: number } }) {
    const containerWidth = constraintsRef.current?.offsetWidth ?? 0;
    const threshold = containerWidth * 0.25;
    const direction = info.offset.x < -threshold || info.velocity.x < -500 ? 1 : info.offset.x > threshold || info.velocity.x > 500 ? -1 : 0;
    snapTo(activeIndex + direction);
  }

  // re-snap on resize
  useEffect(() => {
    const el = constraintsRef.current;
    if (!el) return;
    const ro = new ResizeObserver(() => {
      const w = el.offsetWidth;
      x.set(-activeIndex * w);
    });
    ro.observe(el);
    return () => ro.disconnect();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeIndex]);

  if (total === 0) return null;

  // Single item — no carousel chrome needed
  if (total === 1) {
    const item = items[0]!;
    return (
      <div
        className="feed-carousel feed-carousel--single"
        role="button"
        tabIndex={0}
        onClick={handleTap}
        onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); handleTap(); } }}
      >
        {item.kind === "video" ? (
          <FeedVideo src={item.url} variants={item.variants} />
        ) : (
          <img
            src={item.url}
            alt={alt}
            className="feed-carousel__slide-img"
            loading="lazy"
          />
        )}
        {showHeartBurst && heartBurstNode}
      </div>
    );
  }

  return (
    <div
      className="feed-carousel"
      ref={constraintsRef}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Track */}
      <div className="feed-carousel__viewport" ref={trackRef}>
        <motion.div
          className="feed-carousel__track"
          style={{ x, width: `${total * 100}%` }}
          drag="x"
          dragConstraints={{ left: 0, right: 0 }}
          dragElastic={0.12}
          onDragStart={handleDragStart}
          onDrag={handleDrag}
          onDragEnd={handleDragEnd}
          onTap={handleTap}
          whileTap={{ cursor: "grabbing" }}
        >
          {items.map((item, i) => (
            <div
              key={i}
              className="feed-carousel__slide"
              style={{ width: `${100 / total}%` }}
            >
              {item.kind === "video" ? (
                <FeedVideo src={item.url} variants={item.variants} isActive={i === activeIndex} />
              ) : (
                <img
                  src={item.url}
                  alt={`${alt} ${i + 1}`}
                  className="feed-carousel__slide-img"
                  loading="lazy"
                />
              )}
            </div>
          ))}
        </motion.div>
      </div>

      {/* Prev / Next arrows — appear on hover (desktop) */}
      <button
        type="button"
        className={`feed-carousel__arrow feed-carousel__arrow--prev${isHovered && activeIndex > 0 ? " is-visible" : ""}`}
        aria-label="Previous"
        onClick={(e) => { e.stopPropagation(); snapTo(activeIndex - 1); }}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 18l-6-6 6-6" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" /></svg>
      </button>
      <button
        type="button"
        className={`feed-carousel__arrow feed-carousel__arrow--next${isHovered && activeIndex < total - 1 ? " is-visible" : ""}`}
        aria-label="Next"
        onClick={(e) => { e.stopPropagation(); snapTo(activeIndex + 1); }}
      >
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 18l6-6-6-6" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" /></svg>
      </button>

      <span className="feed-carousel__multiple-icon" aria-label={`${total} media items`}>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="4" y="4" width="13" height="13" rx="2" />
          <path d="M8 20h10a2 2 0 0 0 2-2V8" />
        </svg>
      </span>

      {/* Pagination dots sit below the media, immediately above post metrics. */}
      <div className="feed-carousel__dots" aria-hidden="true">
        {items.map((_, i) => (
          <button
            key={i}
            type="button"
            className={`feed-carousel__dot${i === activeIndex ? " is-active" : ""}`}
            aria-label={`Go to slide ${i + 1}`}
            onClick={(e) => { e.stopPropagation(); snapTo(i); }}
          />
        ))}
      </div>

      {showHeartBurst && heartBurstNode}
    </div>
  );
}

/* ── FeedVideo: IntersectionObserver auto-play ────────────────────────────── */
interface FeedVideoProps {
  src: string;
  variants?: MediaVariant[];
  isActive?: boolean;
}

function pickVideoSource(src: string, variants: MediaVariant[] | undefined) {
  if (!variants?.length || typeof window === "undefined") {
    return src;
  }

  const viewportHeight = Math.max(window.innerHeight, window.innerWidth);
  const connection = navigator as Navigator & {
    connection?: { saveData?: boolean; effectiveType?: string };
  };
  const saveData = Boolean(connection.connection?.saveData);
  const slowNetwork = /2g|slow-2g/i.test(connection.connection?.effectiveType ?? "");
  const targetHeight = saveData || slowNetwork ? 720 : viewportHeight > 1100 ? 1440 : 1080;
  const sorted = [...variants]
    .filter((variant) => typeof variant.url === "string" && variant.url)
    .sort((left, right) => (left.height ?? 0) - (right.height ?? 0));

  return (
    sorted.find((variant) => (variant.height ?? 0) >= targetHeight)?.url ??
    sorted[sorted.length - 1]?.url ??
    src
  );
}

export function FeedVideo({ src, variants, isActive = true }: FeedVideoProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [playbackSrc, setPlaybackSrc] = useState(src);
  const [isPaused, setIsPaused] = useState(false);
  const [isHolding, setIsHolding] = useState(false);
  const [showPlayIcon, setShowPlayIcon] = useState(false);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    setPlaybackSrc(pickVideoSource(src, variants));
  }, [src, variants]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting && isActive) {
            // Only auto-play if the user hasn't explicitly paused it
            if (!isPaused) {
              video.play().catch(() => { /* user gesture required */ });
            }
          } else {
            video.pause();
            if (isHolding) {
              setIsHolding(false);
              video.playbackRate = 1.0;
            }
          }
        }
      },
      { threshold: 0.5 }
    );

    observer.observe(video);
    return () => observer.disconnect();
  }, [isActive, isPaused, isHolding]);

  const handlePointerDown = () => {
    setIsHolding(true);
    if (videoRef.current) videoRef.current.playbackRate = 2.0;
  };

  const handlePointerUp = () => {
    setIsHolding(false);
    if (videoRef.current) videoRef.current.playbackRate = 1.0;
  };

  const handleTimeUpdate = () => {
    const video = videoRef.current;
    if (video && video.duration) {
      setProgress((video.currentTime / video.duration) * 100);
    }
  };

  return (
    <div
      className="feed-carousel__slide-video-container"
      style={{ position: 'relative', width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
      onPointerDown={handlePointerDown}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
    >
      <video
        ref={videoRef}
        src={playbackSrc}
        className="feed-carousel__slide-img feed-carousel__slide-video"
        muted
        playsInline
        loop
        preload="none"
        onTimeUpdate={handleTimeUpdate}
      />

      {/* 2x Speed Badge */}
      <AnimatePresence>
        {isHolding && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0 }}
            style={{
              position: 'absolute',
              top: '16px',
              left: '16px',
              background: 'rgba(0,0,0,0.6)',
              padding: '6px 12px',
              borderRadius: '12px',
              color: 'white',
              fontSize: '12px',
              fontWeight: 'bold',
              pointerEvents: 'none'
            }}
          >
            2x Speed
          </motion.div>
        )}
      </AnimatePresence>

      {/* Play/Pause Icon Animation */}
      <AnimatePresence>
        {showPlayIcon && (
          <motion.div
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 1.2 }}
            transition={{ duration: 0.2 }}
            style={{
              position: 'absolute',
              background: 'rgba(0,0,0,0.4)',
              borderRadius: '50%',
              width: '72px',
              height: '72px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              pointerEvents: 'none'
            }}
          >
            {isPaused ? (
              <svg width="32" height="32" viewBox="0 0 24 24" fill="white">
                <path d="M8 5v14l11-7z" />
              </svg>
            ) : (
              <svg width="32" height="32" viewBox="0 0 24 24" fill="white">
                <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z" />
              </svg>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Thin Progress Timeline */}
      <div style={{
        position: 'absolute',
        bottom: 0,
        left: 0,
        width: '100%',
        height: '3px',
        background: 'rgba(255, 255, 255, 0.3)',
        pointerEvents: 'none'
      }}>
        <div style={{
          width: `${progress}%`,
          height: '100%',
          background: 'white',
          transition: 'width 0.1s linear'
        }} />
      </div>
    </div>
  );
}
