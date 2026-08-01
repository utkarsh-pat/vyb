"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
} from "react";

export type StoryOverlayMetadata = {
  version: 1;
  canvas: { width: number; height: number };
  transform: {
    zoom: number;
    rotation: number;
    panX: number;
    panY: number;
    fit: "fit" | "fill";
  };
  layers: Array<
    | {
        id: string;
        kind: "text";
        text: string;
        x: number;
        y: number;
        color: string;
        size: number;
        align: "left" | "center" | "right";
        style: "plain" | "bold" | "highlight";
      }
    | {
        id: string;
        kind: "sticker";
        value: string;
        x: number;
        y: number;
        size: number;
      }
  >;
  drawings: Array<{
    id: string;
    color: string;
    width: number;
    points: Array<{ x: number; y: number }>;
  }>;
};

type BuilderAsset = {
  id: string;
  url: string;
  file: File;
  kind: "image" | "video";
  overlayMetadata?: StoryOverlayMetadata | null;
};

type StoryBuilderProps = {
  asset: BuilderAsset;
  purpose?: "story" | "post";
  onApply: (result: {
    file: File;
    overlayMetadata: StoryOverlayMetadata | null;
    compositionJson: string | null;
  }) => void;
  onClose: () => void;
};

const CANVAS_WIDTH = 360;
const CANVAS_HEIGHT = 640;
const STICKERS = ["✨", "🔥", "💜", "😂", "🎓", "📍", "🎉", "💯"];
const MORE_EMOJIS = ["🤩", "😎", "🤝", "💪", "🌈", "🌟", "🚀", "🎯", "🏀", "⚽", "🎮", "💻", "📚", "📸", "💡", "✅"];

function makeId(prefix: string) {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`;
}

function cloneMetadata(value: StoryOverlayMetadata): StoryOverlayMetadata {
  return JSON.parse(JSON.stringify(value)) as StoryOverlayMetadata;
}

function initialMetadata(existing: StoryOverlayMetadata | null | undefined, width = CANVAS_WIDTH, height = CANVAS_HEIGHT): StoryOverlayMetadata {
  if (existing) return cloneMetadata(existing);
  return {
    version: 1,
    canvas: { width, height },
    transform: { zoom: 1, rotation: 0, panX: 0, panY: 0, fit: "fill" },
    layers: [],
    drawings: [],
  };
}

function buildCompositionJson(metadata: StoryOverlayMetadata) {
  const composition = {
    version: 1,
    canvas: { width: 1080, height: 1920 },
    media: {
      fit: metadata.transform.fit === "fill" ? "cover" : "contain",
      scale: metadata.transform.zoom,
      rotationDegrees: metadata.transform.rotation,
      offsetX: metadata.transform.panX / metadata.canvas.width,
      offsetY: metadata.transform.panY / metadata.canvas.height,
    },
    layers: [
      ...metadata.layers.map((layer) =>
        layer.kind === "text"
          ? {
              type: "text",
              id: layer.id,
              text: layer.text,
              x: layer.x,
              y: layer.y,
              color: layer.color,
              fontSize: layer.size / metadata.canvas.height,
              align: layer.align,
              style: layer.style,
            }
          : {
              type: "sticker",
              id: layer.id,
              value: layer.value,
              x: layer.x,
              y: layer.y,
              size: layer.size / metadata.canvas.height,
            },
      ),
      ...metadata.drawings.map((drawing) => ({
        type: "drawing",
        id: drawing.id,
        color: drawing.color,
        width: drawing.width / metadata.canvas.width,
        points: drawing.points,
      })),
    ],
  };
  const value = JSON.stringify(composition);
  if (new TextEncoder().encode(value).byteLength > 65_536) {
    throw new Error("Story overlay design is too complex. Undo a few drawing strokes and try again.");
  }
  return value;
}

export function StoryBuilder({ asset, purpose = "story", onApply, onClose }: StoryBuilderProps) {
  const canvasWidth = purpose === "post" ? 640 : CANVAS_WIDTH;
  const canvasHeight = purpose === "post" ? 480 : CANVAS_HEIGHT;
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const imageRef = useRef<HTMLImageElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const renderCanvasRef = useRef<(exportMode?: boolean) => void>(() => undefined);
  const dragRef = useRef<{
    kind: "pan" | "layer" | "draw";
    startX: number;
    startY: number;
    initial: StoryOverlayMetadata;
    layerId?: string;
    drawingId?: string;
  } | null>(null);
  const currentRef = useRef(initialMetadata(asset.overlayMetadata, canvasWidth, canvasHeight));
  const [metadata, setMetadata] = useState(() => initialMetadata(asset.overlayMetadata, canvasWidth, canvasHeight));
  const [undoStack, setUndoStack] = useState<StoryOverlayMetadata[]>([]);
  const [redoStack, setRedoStack] = useState<StoryOverlayMetadata[]>([]);
  const [selectedLayerId, setSelectedLayerId] = useState<string | null>(null);
  const [tool, setTool] = useState<"move" | "draw">("move");
  const [drawColor, setDrawColor] = useState("#ffffff");
  const [drawWidth, setDrawWidth] = useState(5);
  const [textDraft, setTextDraft] = useState("");
  const [isPreview, setIsPreview] = useState(false);
  const [isDirty, setIsDirty] = useState(false);
  const [isExporting, setIsExporting] = useState(false);
  const [showMoreEmojis, setShowMoreEmojis] = useState(false);

  useEffect(() => {
    currentRef.current = metadata;
  }, [metadata]);

  useEffect(() => {
    if (!isDirty) return;
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [isDirty]);

  const renderCanvas = useCallback((exportMode = false) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const context = canvas.getContext("2d");
    if (!context) return;

    context.clearRect(0, 0, canvasWidth, canvasHeight);
    context.fillStyle = "#050817";
    context.fillRect(0, 0, canvasWidth, canvasHeight);

    const source = asset.kind === "image" ? imageRef.current : videoRef.current;
    if (source) {
      const sourceWidth =
        asset.kind === "image"
          ? (source as HTMLImageElement).naturalWidth
          : (source as HTMLVideoElement).videoWidth;
      const sourceHeight =
        asset.kind === "image"
          ? (source as HTMLImageElement).naturalHeight
          : (source as HTMLVideoElement).videoHeight;
      if (sourceWidth > 0 && sourceHeight > 0) {
        const fitScale =
          metadata.transform.fit === "fill"
            ? Math.max(canvasWidth / sourceWidth, canvasHeight / sourceHeight)
            : Math.min(canvasWidth / sourceWidth, canvasHeight / sourceHeight);
        context.save();
        context.translate(
          canvasWidth / 2 + metadata.transform.panX,
          canvasHeight / 2 + metadata.transform.panY,
        );
        context.rotate((metadata.transform.rotation * Math.PI) / 180);
        context.scale(
          fitScale * metadata.transform.zoom,
          fitScale * metadata.transform.zoom,
        );
        context.drawImage(source, -sourceWidth / 2, -sourceHeight / 2);
        context.restore();
      }
    }

    for (const drawing of metadata.drawings) {
      if (drawing.points.length < 2) continue;
      context.beginPath();
      context.strokeStyle = drawing.color;
      context.lineWidth = drawing.width;
      context.lineCap = "round";
      context.lineJoin = "round";
      drawing.points.forEach((point, index) => {
        const x = point.x * canvasWidth;
        const y = point.y * canvasHeight;
        if (index === 0) context.moveTo(x, y);
        else context.lineTo(x, y);
      });
      context.stroke();
    }

    for (const layer of metadata.layers) {
      const x = layer.x * canvasWidth;
      const y = layer.y * canvasHeight;
      context.save();
      if (layer.kind === "sticker") {
        context.font = `${layer.size}px "Segoe UI Emoji", sans-serif`;
        context.textAlign = "center";
        context.textBaseline = "middle";
        context.fillText(layer.value, x, y);
      } else {
        context.font = `${layer.style === "bold" ? "800" : "600"} ${layer.size}px Inter, sans-serif`;
        context.textAlign = layer.align;
        context.textBaseline = "middle";
        const width = context.measureText(layer.text).width;
        if (layer.style === "highlight") {
          const left = layer.align === "center" ? x - width / 2 : layer.align === "right" ? x - width : x;
          context.fillStyle = "rgba(0,0,0,.68)";
          context.fillRect(left - 8, y - layer.size * 0.7, width + 16, layer.size * 1.4);
        }
        context.fillStyle = layer.color;
        context.fillText(layer.text, x, y);
      }
      if (!exportMode && !isPreview && layer.id === selectedLayerId) {
        context.strokeStyle = "#a78bfa";
        context.lineWidth = 2;
        context.setLineDash([5, 4]);
        context.strokeRect(x - 70, y - 28, 140, 56);
      }
      context.restore();
    }
  }, [asset.kind, canvasHeight, canvasWidth, isPreview, metadata, selectedLayerId]);

  useEffect(() => {
    renderCanvasRef.current = renderCanvas;
  }, [renderCanvas]);

  useEffect(() => {
    if (asset.kind === "image") {
      const image = new Image();
      image.onload = () => {
        imageRef.current = image;
        renderCanvasRef.current();
      };
      image.src = asset.url;
      return;
    }

    const video = document.createElement("video");
    video.src = asset.url;
    video.muted = true;
    video.loop = true;
    video.playsInline = true;
    video.onloadeddata = () => {
      videoRef.current = video;
      void video.play().catch(() => undefined);
      renderCanvasRef.current();
    };
    const timer = window.setInterval(() => renderCanvasRef.current(), 80);
    return () => {
      window.clearInterval(timer);
      video.pause();
      video.src = "";
    };
  }, [asset.kind, asset.url]);

  useEffect(() => {
    renderCanvas();
  }, [renderCanvas]);

  function commit(next: StoryOverlayMetadata) {
    setUndoStack((stack) => [...stack.slice(-39), cloneMetadata(currentRef.current)]);
    setRedoStack([]);
    setMetadata(next);
    setIsDirty(true);
  }

  function updateTransform(patch: Partial<StoryOverlayMetadata["transform"]>) {
    commit({
      ...metadata,
      transform: { ...metadata.transform, ...patch },
    });
  }

  function addText() {
    const text = textDraft.trim();
    if (!text) return;
    const id = makeId("text");
    commit({
      ...metadata,
      layers: [
        ...metadata.layers,
        {
          id,
          kind: "text",
          text,
          x: 0.5,
          y: 0.45,
          color: "#ffffff",
          size: 30,
          align: "center",
          style: "bold",
        },
      ],
    });
    setSelectedLayerId(id);
    setTextDraft("");
  }

  function updateSelectedText(patch: Record<string, unknown>) {
    commit({
      ...metadata,
      layers: metadata.layers.map((layer) =>
        layer.id === selectedLayerId && layer.kind === "text"
          ? { ...layer, ...patch }
          : layer,
      ),
    });
  }

  function addSticker(value: string) {
    const id = makeId("sticker");
    commit({
      ...metadata,
      layers: [
        ...metadata.layers,
        { id, kind: "sticker", value, x: 0.5, y: 0.55, size: 52 },
      ],
    });
    setSelectedLayerId(id);
  }

  function undo() {
    const previous = undoStack.at(-1);
    if (!previous) return;
    setRedoStack((stack) => [...stack, cloneMetadata(metadata)]);
    setUndoStack((stack) => stack.slice(0, -1));
    setMetadata(cloneMetadata(previous));
  }

  function redo() {
    const next = redoStack.at(-1);
    if (!next) return;
    setUndoStack((stack) => [...stack, cloneMetadata(metadata)]);
    setRedoStack((stack) => stack.slice(0, -1));
    setMetadata(cloneMetadata(next));
  }

  function canvasPoint(event: ReactPointerEvent<HTMLCanvasElement>) {
    const rect = event.currentTarget.getBoundingClientRect();
    return {
      x: ((event.clientX - rect.left) / rect.width) * canvasWidth,
      y: ((event.clientY - rect.top) / rect.height) * canvasHeight,
    };
  }

  function findLayerAt(x: number, y: number) {
    return [...metadata.layers].reverse().find((layer) => {
      const layerX = layer.x * canvasWidth;
      const layerY = layer.y * canvasHeight;
      return Math.abs(layerX - x) < 78 && Math.abs(layerY - y) < 42;
    });
  }

  function handlePointerDown(event: ReactPointerEvent<HTMLCanvasElement>) {
    if (isPreview) return;
    const point = canvasPoint(event);
    event.currentTarget.setPointerCapture(event.pointerId);
    const initial = cloneMetadata(metadata);
    if (tool === "draw") {
      const drawingId = makeId("stroke");
      setMetadata({
        ...metadata,
        drawings: [
          ...metadata.drawings,
          {
            id: drawingId,
            color: drawColor,
            width: drawWidth,
            points: [{ x: point.x / canvasWidth, y: point.y / canvasHeight }],
          },
        ],
      });
      dragRef.current = { kind: "draw", startX: point.x, startY: point.y, initial, drawingId };
      return;
    }
    const layer = findLayerAt(point.x, point.y);
    if (layer) {
      setSelectedLayerId(layer.id);
      dragRef.current = { kind: "layer", startX: point.x, startY: point.y, initial, layerId: layer.id };
    } else {
      setSelectedLayerId(null);
      dragRef.current = { kind: "pan", startX: point.x, startY: point.y, initial };
    }
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLCanvasElement>) {
    const drag = dragRef.current;
    if (!drag) return;
    const point = canvasPoint(event);
    const dx = point.x - drag.startX;
    const dy = point.y - drag.startY;
    if (drag.kind === "pan") {
      setMetadata({
        ...drag.initial,
        transform: {
          ...drag.initial.transform,
          panX: drag.initial.transform.panX + dx,
          panY: drag.initial.transform.panY + dy,
        },
      });
    } else if (drag.kind === "layer") {
      setMetadata({
        ...drag.initial,
        layers: drag.initial.layers.map((layer) =>
          layer.id === drag.layerId
            ? {
                ...layer,
                x: Math.max(0.04, Math.min(0.96, layer.x + dx / canvasWidth)),
                y: Math.max(0.04, Math.min(0.96, layer.y + dy / canvasHeight)),
              }
            : layer,
        ),
      });
    } else {
      setMetadata((current) => ({
        ...current,
        drawings: current.drawings.map((drawing) =>
          drawing.id === drag.drawingId
            ? {
                ...drawing,
                points: [
                  ...drawing.points,
                  { x: point.x / canvasWidth, y: point.y / canvasHeight },
                ],
              }
            : drawing,
        ),
      }));
    }
  }

  function handlePointerUp(event: ReactPointerEvent<HTMLCanvasElement>) {
    const drag = dragRef.current;
    if (!drag) return;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    dragRef.current = null;
    setUndoStack((stack) => [...stack.slice(-39), drag.initial]);
    setRedoStack([]);
    setIsDirty(true);
  }

  function requestClose() {
    if (!isDirty || window.confirm("Discard unsaved Story edits?")) onClose();
  }

  async function applyStory() {
    setIsExporting(true);
    try {
      if (asset.kind === "video") {
        onApply({
          file: asset.file,
          overlayMetadata: metadata,
          compositionJson: buildCompositionJson(metadata),
        });
        return;
      }
      setIsPreview(true);
      await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()));
      renderCanvas(true);
      const canvas = canvasRef.current;
      if (!canvas) throw new Error("Story canvas is unavailable.");
      const exportCanvas = document.createElement("canvas");
      exportCanvas.width = purpose === "post" ? 1600 : 1080;
      exportCanvas.height = purpose === "post" ? 1200 : 1920;
      const exportContext = exportCanvas.getContext("2d");
      if (!exportContext) throw new Error("Story export canvas is unavailable.");
      exportContext.drawImage(canvas, 0, 0, exportCanvas.width, exportCanvas.height);
      const blob = await new Promise<Blob>((resolve, reject) => {
        exportCanvas.toBlob(
          (value) => (value ? resolve(value) : reject(new Error("Could not export Story image."))),
          "image/jpeg",
          0.92,
        );
      });
      const file = new File(
        [blob],
        `${asset.file.name.replace(/\.[^.]+$/, "")}-story.jpg`,
        { type: "image/jpeg", lastModified: Date.now() },
      );
      onApply({ file, overlayMetadata: null, compositionJson: null });
    } finally {
      setIsExporting(false);
      setIsPreview(false);
    }
  }

  const selectedLayer = metadata.layers.find((layer) => layer.id === selectedLayerId);
  const selectedText = selectedLayer?.kind === "text" ? selectedLayer : null;

  return (
    <div className="cs-story-builder-backdrop" role="dialog" aria-modal="true" aria-label="Story builder">
      <section className={`cs-story-builder${isPreview ? " is-preview" : ""}`}>
        <header className="cs-story-builder-header">
          <button type="button" onClick={requestClose}>Close</button>
          <strong>{purpose === "post" ? "Media editor" : "Story Builder"}</strong>
          <div>
            <button type="button" onClick={undo} disabled={undoStack.length === 0}>Undo</button>
            <button type="button" onClick={redo} disabled={redoStack.length === 0}>Redo</button>
          </div>
        </header>

        <div className="cs-story-builder-body">
          <div className="cs-story-builder-stage-wrap">
            <canvas
              ref={canvasRef}
              width={canvasWidth}
              height={canvasHeight}
              className={`cs-story-builder-canvas${purpose === "post" ? " is-post" : ""}`}
              onPointerDown={handlePointerDown}
              onPointerMove={handlePointerMove}
              onPointerUp={handlePointerUp}
              onPointerCancel={handlePointerUp}
            />
          </div>

          {!isPreview && (
            <aside className="cs-story-builder-controls">
              <div className="cs-story-builder-tool-row">
                <button type="button" className={tool === "move" ? "is-active" : ""} onClick={() => setTool("move")}>Move</button>
                <button type="button" className={tool === "draw" ? "is-active" : ""} onClick={() => setTool("draw")}>Draw</button>
                <button type="button" onClick={() => setIsPreview(true)}>Preview</button>
              </div>

              <fieldset>
                <legend>Canvas</legend>
                <div className="cs-story-builder-tool-row">
                  <button type="button" className={metadata.transform.fit === "fit" ? "is-active" : ""} onClick={() => updateTransform({ fit: "fit" })}>Fit</button>
                  <button type="button" className={metadata.transform.fit === "fill" ? "is-active" : ""} onClick={() => updateTransform({ fit: "fill" })}>Crop</button>
                  <button type="button" onClick={() => updateTransform({ rotation: metadata.transform.rotation - 90 })}>Rotate</button>
                </div>
                <label>Zoom
                  <input type="range" min="0.5" max="3" step="0.05" value={metadata.transform.zoom}
                    onChange={(event) => updateTransform({ zoom: Number(event.target.value) })} />
                </label>
              </fieldset>

              <fieldset>
                <legend>Text</legend>
                <div className="cs-story-builder-text-add">
                  <input value={textDraft} onChange={(event) => setTextDraft(event.target.value)} placeholder="Write something" maxLength={90} />
                  <button type="button" onClick={addText}>Add</button>
                </div>
                {selectedText && (
                  <>
                    <input type="text" value={selectedText.text} onChange={(event) => updateSelectedText({ text: event.target.value })} />
                    <div className="cs-story-builder-tool-row">
                      {(["plain", "bold", "highlight"] as const).map((style) => (
                        <button key={style} type="button" className={selectedText.style === style ? "is-active" : ""} onClick={() => updateSelectedText({ style })}>{style}</button>
                      ))}
                    </div>
                    <label>Size
                      <input type="range" min="18" max="64" value={selectedText.size} onChange={(event) => updateSelectedText({ size: Number(event.target.value) })} />
                    </label>
                    <label>Color
                      <input type="color" value={selectedText.color} onChange={(event) => updateSelectedText({ color: event.target.value })} />
                    </label>
                    <div className="cs-story-builder-tool-row">
                      {(["left", "center", "right"] as const).map((align) => (
                        <button key={align} type="button" className={selectedText.align === align ? "is-active" : ""} onClick={() => updateSelectedText({ align })}>{align}</button>
                      ))}
                    </div>
                  </>
                )}
              </fieldset>

              <fieldset>
                <legend>Emojis</legend>
                <div className="cs-story-builder-stickers">
                  {(showMoreEmojis ? [...STICKERS, ...MORE_EMOJIS] : STICKERS)
                    .map((emoji) => <button type="button" key={emoji} onClick={() => addSticker(emoji)}>{emoji}</button>)}
                </div>
                {!showMoreEmojis ? <button type="button" onClick={() => setShowMoreEmojis(true)}>More emojis</button> : null}
              </fieldset>

              {tool === "draw" && (
                <fieldset>
                  <legend>Brush</legend>
                  <label>Color <input type="color" value={drawColor} onChange={(event) => setDrawColor(event.target.value)} /></label>
                  <label>Width <input type="range" min="2" max="18" value={drawWidth} onChange={(event) => setDrawWidth(Number(event.target.value))} /></label>
                </fieldset>
              )}
            </aside>
          )}
        </div>

        <footer className="cs-story-builder-footer">
          {asset.kind === "video" && (
            <span>Video overlays are saved as editable metadata; video burn-in needs the media processing pipeline.</span>
          )}
          <button type="button" onClick={() => setIsPreview((value) => !value)}>
            {isPreview ? "Continue editing" : "Full preview"}
          </button>
          <button type="button" className="is-primary" onClick={() => void applyStory()} disabled={isExporting}>
            {isExporting ? "Preparing media..." : "Apply edits"}
          </button>
        </footer>
      </section>
    </div>
  );
}
