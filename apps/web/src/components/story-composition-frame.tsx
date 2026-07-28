"use client";

import { useMemo, type CSSProperties, type ReactNode } from "react";

type CompositionLayer =
  | {
      type: "text";
      id?: string;
      text: string;
      x: number;
      y: number;
      color?: string;
      fontSize?: number;
      align?: "left" | "center" | "right";
      style?: "plain" | "bold" | "highlight";
    }
  | {
      type: "sticker";
      id?: string;
      value: string;
      x: number;
      y: number;
      size?: number;
    }
  | {
      type: "drawing";
      id?: string;
      color?: string;
      width?: number;
      points: Array<{ x: number; y: number }>;
    };

type StoryComposition = {
  version: 1;
  canvas: { width: 1080; height: 1920 };
  media: {
    fit: "cover" | "contain";
    scale: number;
    rotationDegrees: number;
    offsetX: number;
    offsetY: number;
  };
  layers: CompositionLayer[];
};

function clamp(value: unknown, min: number, max: number, fallback: number) {
  const number = Number(value);
  return Number.isFinite(number) ? Math.max(min, Math.min(max, number)) : fallback;
}

function parseComposition(value?: string | null): StoryComposition | null {
  if (!value || value.length > 65_536) return null;
  try {
    const parsed = JSON.parse(value) as Partial<StoryComposition>;
    if (
      parsed.version !== 1 ||
      parsed.canvas?.width !== 1080 ||
      parsed.canvas?.height !== 1920 ||
      !parsed.media ||
      !Array.isArray(parsed.layers)
    ) {
      return null;
    }
    return parsed as StoryComposition;
  } catch {
    return null;
  }
}

export function StoryCompositionFrame({
  compositionJson,
  children,
}: {
  compositionJson?: string | null;
  children: ReactNode;
}) {
  const composition = useMemo(() => parseComposition(compositionJson), [compositionJson]);
  if (!composition) return <>{children}</>;

  const mediaStyle = {
    "--story-media-fit": composition.media.fit,
    "--story-media-scale": clamp(composition.media.scale, 0.25, 5, 1),
    "--story-media-rotate": `${clamp(composition.media.rotationDegrees, -1080, 1080, 0)}deg`,
    "--story-media-x": `${clamp(composition.media.offsetX, -2, 2, 0) * 100}%`,
    "--story-media-y": `${clamp(composition.media.offsetY, -2, 2, 0) * 100}%`,
  } as CSSProperties;

  return (
    <div className="vyb-story-composition" style={mediaStyle}>
      <div className="vyb-story-composition-media">{children}</div>
      <svg
        className="vyb-story-composition-overlay"
        viewBox="0 0 1080 1920"
        preserveAspectRatio="xMidYMid meet"
        aria-hidden="true"
      >
        {composition.layers.map((layer, index) => {
          const key = layer.id ?? `${layer.type}-${index}`;
          if (layer.type === "drawing") {
            const points = layer.points
              .slice(0, 12_000)
              .map((point) => `${clamp(point.x, 0, 1, 0) * 1080},${clamp(point.y, 0, 1, 0) * 1920}`)
              .join(" ");
            return (
              <polyline
                key={key}
                points={points}
                fill="none"
                stroke={layer.color ?? "#fff"}
                strokeWidth={clamp(layer.width, 0.001, 0.1, 0.01) * 1080}
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            );
          }

          const x = clamp(layer.x, 0, 1, 0.5) * 1080;
          const y = clamp(layer.y, 0, 1, 0.5) * 1920;
          if (layer.type === "sticker") {
            return (
              <text
                key={key}
                x={x}
                y={y}
                textAnchor="middle"
                dominantBaseline="central"
                fontSize={clamp(layer.size, 0.015, 0.3, 0.08) * 1920}
                fontFamily='"Segoe UI Emoji", sans-serif'
              >
                {layer.value}
              </text>
            );
          }

          const anchor = layer.align === "left" ? "start" : layer.align === "right" ? "end" : "middle";
          return (
            <text
              key={key}
              x={x}
              y={y}
              textAnchor={anchor}
              dominantBaseline="central"
              fontSize={clamp(layer.fontSize, 0.01, 0.2, 0.047) * 1920}
              fontWeight={layer.style === "bold" ? 800 : 600}
              fill={layer.color ?? "#fff"}
              stroke={layer.style === "highlight" ? "rgba(0,0,0,.78)" : "none"}
              strokeWidth={layer.style === "highlight" ? 24 : 0}
              paintOrder="stroke"
            >
              {layer.text.slice(0, 120)}
            </text>
          );
        })}
      </svg>
    </div>
  );
}
