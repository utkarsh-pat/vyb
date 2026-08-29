export default function NativeGameBootstrapPage() {
  return (
    <main
      aria-label="Preparing game"
      style={{
        minHeight: "100dvh",
        display: "grid",
        placeItems: "center",
        background: "#061326",
        color: "#aebbd0",
        fontFamily: "system-ui, sans-serif"
      }}
    >
      <p>Preparing secure game session…</p>
    </main>
  );
}
