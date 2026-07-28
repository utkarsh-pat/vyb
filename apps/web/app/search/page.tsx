import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { CampusSearchShell } from "../../src/components/campus-search-shell";
import {
  getCampusFeed,
  getCampusVibes,
  getSuggestedCampusUsers,
  getViewerProfile,
  searchCampusUsers
} from "../../src/lib/backend";
import { getDisplayCollegeName } from "../../src/lib/college-access";
import { readDevSessionFromCookieStore } from "../../src/lib/dev-session";
import { getMarketDashboard } from "../../src/lib/market-data";

export default async function SearchPage({
  searchParams
}: {
  searchParams: Promise<{
    q?: string;
  }>;
}) {
  const viewer = readDevSessionFromCookieStore(await cookies());

  if (!viewer) {
    redirect("/login");
  }

  const [{ q = "" }, profile] = await Promise.all([searchParams, getViewerProfile(viewer).catch(() => null)]);

  if (!profile?.profileCompleted || !profile.profile?.username) {
    redirect("/onboarding");
  }

  const viewerUsername = profile.profile.username;
  const viewerName = profile.profile.fullName ?? viewer.displayName;
  const displayCollegeName = getDisplayCollegeName(profile.collegeName);
  const trimmedQuery = q.trim();

  const emptyFeedResponse = {
    tenantId: viewer.tenantId,
    communityId: null,
    items: [],
    nextCursor: null
  };
  const emptySuggestedResponse = {
    query: "",
    items: []
  };
  const emptyMarketDashboard = {
      tenantId: viewer.tenantId,
      viewer: {
        userId: viewer.userId,
        username: viewerUsername,
        savedCount: 0
      },
      listings: [],
      requests: [],
      viewerActiveListings: [],
      viewerActiveRequests: []
  };

  const resultsPromise = trimmedQuery
    ? searchCampusUsers(viewer, trimmedQuery).catch(() => ({
        query: trimmedQuery,
        items: []
      }))
    : Promise.resolve({
        query: "",
        items: []
      });

  const discoveryPromise = trimmedQuery
    ? Promise.resolve([emptyFeedResponse, emptyFeedResponse, emptySuggestedResponse, emptyMarketDashboard] as const)
    : Promise.all([
        getCampusFeed(viewer, { limit: 18 }).catch(() => emptyFeedResponse),
        getCampusVibes(viewer, 18).catch(() => emptyFeedResponse),
        getSuggestedCampusUsers(viewer, 6).catch(() => emptySuggestedResponse),
        getMarketDashboard(viewer).catch(() => emptyMarketDashboard)
      ] as const);

  const [results, [feedResponse, vibesResponse, suggestedResponse, marketDashboard]] = await Promise.all([
    resultsPromise,
    discoveryPromise
  ]);

  return (
    <CampusSearchShell
      initialQuery={trimmedQuery}
      results={results.items}
      viewerName={viewerName}
      viewerUsername={viewerUsername}
      viewerEmail={viewer.email}
      collegeName={displayCollegeName}
      course={profile.profile.course}
      stream={profile.profile.stream}
      hasSearched={Boolean(trimmedQuery)}
      initialFeedItems={feedResponse.items}
      initialVibeItems={vibesResponse.items}
      suggestedUsers={suggestedResponse.items}
      marketListings={marketDashboard.listings}
      marketRequests={marketDashboard.requests}
    />
  );
}
