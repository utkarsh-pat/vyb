import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { CampusUploadShell } from "../../src/components/campus-upload-shell";
import { getMyCampusCommunities, getViewerProfile } from "../../src/lib/backend";
import { getDisplayCollegeName } from "../../src/lib/college-access";
import { readDevSessionFromCookieStore } from "../../src/lib/dev-session";

export default async function CreatePage() {
  const viewer = readDevSessionFromCookieStore(await cookies());

  if (!viewer) {
    redirect("/login");
  }

  const [profile, communityResponse] = await Promise.all([
    getViewerProfile(viewer).catch(() => null),
    getMyCampusCommunities(viewer).catch(() => null)
  ]);

  if (!profile?.profileCompleted) {
    redirect("/onboarding");
  }

  const viewerName = profile.profile?.fullName ?? viewer.displayName;
  const viewerUsername = profile.profile?.username ?? viewer.email.split("@")[0];
  const displayCollegeName = getDisplayCollegeName(profile.collegeName);

  return (
    <CampusUploadShell
      viewerName={viewerName}
      viewerUsername={viewerUsername}
      viewerEmail={viewer.email}
      collegeName={displayCollegeName}
      communities={(communityResponse?.communities ?? [])
        .filter((community) => community.isMember !== false && community.membershipStatus !== "not_member" && community.membershipStatus !== "left")
        .map((community) => ({ id: community.id, name: community.name, type: community.type }))}
    />
  );
}
