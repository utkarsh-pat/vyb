"use client";

import { useEffect, useRef, type ReactNode } from "react";
import { flushContentEvents, recordContentEvent } from "../lib/content-measurement";

export function ContentViewTracker({ postId, className, children }: { postId: string; className?: string; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  const enteredAt = useRef(0);
  const lastQualifiedAt = useRef(0);
  const impressionTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const qualifiedTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;
    const clear = () => {
      if (impressionTimer.current) clearTimeout(impressionTimer.current);
      if (qualifiedTimer.current) clearTimeout(qualifiedTimer.current);
      impressionTimer.current = null;
      qualifiedTimer.current = null;
      enteredAt.current = 0;
    };
    const observer = new IntersectionObserver(([entry]) => {
      if (!entry || entry.intersectionRatio < 0.5 || document.visibilityState !== "visible") {
        clear();
        return;
      }
      if (enteredAt.current) return;
      enteredAt.current = performance.now();
      impressionTimer.current = setTimeout(() => recordContentEvent(postId, "impression", { visibleMs: 500 }), 500);
      qualifiedTimer.current = setTimeout(() => {
        const now = Date.now();
        if (now - lastQualifiedAt.current >= 5_000) {
          recordContentEvent(postId, "qualified_view", { visibleMs: 1000 });
          lastQualifiedAt.current = now;
        }
      }, 1000);
    }, { threshold: [0, 0.5, 1] });
    observer.observe(element);
    return () => { clear(); observer.disconnect(); void flushContentEvents(); };
  }, [postId]);

  return <div ref={ref} className={className}>{children}</div>;
}
