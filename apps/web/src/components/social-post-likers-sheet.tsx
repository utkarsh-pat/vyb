"use client";

import type { FeedCard, PostLikerItem } from "@vyb/contracts";
import { useEffect, useState } from "react";
import { CampusAvatarContent } from "./campus-avatar";

type SocialPostLikersSheetProps = {
  post: FeedCard | null;
  items: PostLikerItem[];
  isLoading: boolean;
  message: string | null;
  onClose: () => void;
};

export function SocialPostLikersSheet({ post, items, isLoading, message, onClose }: SocialPostLikersSheetProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [followedUsernames, setFollowedUsernames] = useState<Set<string>>(() => new Set());
  const [busyUsername, setBusyUsername] = useState<string | null>(null);
  const [followMessage, setFollowMessage] = useState<string | null>(null);

  useEffect(() => {
    setSearchQuery("");
    setFollowMessage(null);
  }, [post?.id]);

  useEffect(() => {
    setFollowedUsernames(new Set(items.filter((item) => item.viewerIsFollowing).map((item) => item.username)));
  }, [post?.id, items]);

  async function toggleFollow(username: string) {
    const shouldFollow = !followedUsernames.has(username);
    setBusyUsername(username);
    setFollowMessage(null);
    try {
      const response = await fetch(`/api/follows/${encodeURIComponent(username)}`, {
        method: shouldFollow ? "PUT" : "DELETE"
      });
      if (!response.ok) {
        throw new Error("Could not update follow status.");
      }
      setFollowedUsernames((current) => {
        const next = new Set(current);
        if (shouldFollow) next.add(username); else next.delete(username);
        return next;
      });
    } catch (error) {
      setFollowMessage(error instanceof Error ? error.message : "Could not update follow status.");
    } finally {
      setBusyUsername(null);
    }
  }

  if (!post) {
    return null;
  }

  const filteredItems = items.filter(item =>
    item.displayName.toLowerCase().includes(searchQuery.toLowerCase()) ||
    item.username.toLowerCase().includes(searchQuery.toLowerCase())
  );

  function getReactionSymbol(reactionType: PostLikerItem["reactionType"]) {
    switch (reactionType) {
      case "fire":
        return "🔥";
      case "support":
        return "👏";
      case "love":
        return "❤️";
      case "insight":
        return "💡";
      case "funny":
        return "😂";
      default:
        return "👍";
    }
  }

  return (
    <div className="vyb-post-likers-backdrop" role="presentation" onClick={onClose}>
      <div className="vyb-post-likers-sheet" role="dialog" aria-modal="true" aria-label="Post reactions" onClick={(event) => event.stopPropagation()}>
        <div className="vyb-post-likers-head" style={{ flexDirection: "column", alignItems: "stretch" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
            <div style={{ width: 24 }} />
            <strong style={{ fontSize: "16px" }}>Likes</strong>
            <button type="button" onClick={onClose} style={{ border: "none", background: "none", cursor: "pointer", padding: "4px" }}>
              ✕
            </button>
          </div>

          <div style={{ display: "flex", justifyContent: "center", marginBottom: "16px" }}>
            <div style={{ textAlign: "center" }}>
              <span style={{ fontSize: "18px", fontWeight: "bold" }}>{post.reactions || 0}</span>
              <span style={{ display: "block", fontSize: "12px", color: "#888" }}>Likes</span>
            </div>
          </div>

          <input
            type="text"
            placeholder="Search"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            style={{ width: "100%", padding: "10px 12px", borderRadius: "12px", border: "none", backgroundColor: "rgba(128,128,128,0.1)", outline: "none", fontSize: "14px" }}
          />
        </div>

        <div className="vyb-post-likers-list">
          {isLoading ? <p className="vyb-post-likers-state">Loading reactions...</p> : null}
          {!isLoading && filteredItems.length === 0 ? <p className="vyb-post-likers-state">No reactions found.</p> : null}

          {filteredItems.map((item) => (
            <article key={`${item.membershipId}-${item.reactedAt}`} className="vyb-post-likers-item">
              <span className="vyb-post-likers-avatar">
                <CampusAvatarContent
                  userId={item.userId}
                  username={item.username}
                  displayName={item.displayName}
                  avatarUrl={item.avatarUrl ?? null}
                  fallback={item.displayName.slice(0, 1).toUpperCase()}
                  decorative
                />
              </span>
              <div style={{ flex: 1 }}>
                <strong>{item.displayName}</strong>
                <span>@{item.username}</span>
              </div>
              <span className="vyb-post-likers-reaction" aria-label={item.reactionType} style={{ marginRight: "12px" }}>
                {getReactionSymbol(item.reactionType)}
              </span>
              {!item.isViewer ? (
                <button
                  type="button"
                  onClick={() => void toggleFollow(item.username)}
                  disabled={busyUsername !== null}
                  style={{ backgroundColor: followedUsernames.has(item.username) ? "rgba(128,128,128,.18)" : "#0095f6", color: "white", border: "none", borderRadius: "8px", padding: "6px 16px", fontWeight: "600", fontSize: "14px", cursor: busyUsername ? "wait" : "pointer" }}
                >
                  {busyUsername === item.username ? "Working..." : followedUsernames.has(item.username) ? "Following" : "Follow"}
                </button>
              ) : null}
            </article>
          ))}
        </div>

        {message || followMessage ? <p className="vyb-post-likers-message">{followMessage ?? message}</p> : null}
      </div>
    </div>
  );
}
