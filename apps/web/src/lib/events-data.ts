import "server-only";

import type {
  CampusEvent,
  CampusEventRegistration,
  CampusEventRegistrationListResponse,
  CampusEventRegistrationStatus,
  CampusEventsDashboardResponse,
  CampusEventViewerRegistrationResponse,
  CreateCampusEventRequest,
  CreateCampusEventResponse,
  ManageCampusEventRegistrationRequest,
  ManageCampusEventRegistrationResponse,
  ManageCampusEventResponse,
  ToggleCampusEventInterestResponse,
  ToggleCampusEventSaveResponse,
  UpdateCampusEventRequest,
  UpdateCampusEventResponse,
  UpsertCampusEventRegistrationRequest,
  UpsertCampusEventRegistrationResponse
} from "@vyb/contracts";
import {
  fetchBackendJson,
  isBackendRequestError,
  mutateBackendJson,
  postBackendJson
} from "./backend";
import type { DevSession } from "./dev-session";
import type { EventViewerIdentity } from "./events-types";

type StoredEventAudience = CampusEvent & {
  savedByUserIds?: string[];
  interestedUserIds?: string[];
};

function eventPath(eventId: string, suffix = "") {
  return `/v1/events/${encodeURIComponent(eventId)}${suffix}`;
}

function filterRegistrations(
  registrations: CampusEventRegistration[],
  filters?: {
    query?: string | null;
    statuses?: CampusEventRegistrationStatus[];
  }
) {
  const query = filters?.query?.trim().toLowerCase() ?? "";
  const statuses = new Set<CampusEventRegistrationStatus>(filters?.statuses ?? []);

  return registrations
    .filter((registration) => {
      if (statuses.size > 0 && !statuses.has(registration.status)) {
        return false;
      }
      if (!query) {
        return true;
      }

      return [
        registration.attendee.displayName,
        registration.attendee.username,
        registration.attendee.role,
        registration.teamName,
        registration.note,
        registration.reviewNote,
        ...registration.teamMembers.flatMap((member) => [
          member.name,
          member.username,
          member.email,
          member.role
        ]),
        ...registration.answers.flatMap((answer) => [answer.label, answer.value]),
        ...registration.attachments.flatMap((attachment) => [attachment.fileName, attachment.url])
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(query);
    })
    .sort((left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt));
}

function csvEscape(value: string | number | null | undefined) {
  const text = value === null || value === undefined ? "" : String(value);
  return `"${text.replace(/"/g, "\"\"")}"`;
}

function registrationsCsv(registrations: CampusEventRegistration[]) {
  const headers = [
    "Registration ID",
    "Status",
    "Leader Name",
    "Leader Username",
    "Leader Role",
    "Submitted At",
    "Updated At",
    "Team Name",
    "Team Size",
    "Team Members",
    "Attachments",
    "Note",
    "Review Note",
    "Answers"
  ];
  const rows = registrations.map((registration) =>
    [
      registration.id,
      registration.status,
      registration.attendee.displayName,
      registration.attendee.username,
      registration.attendee.role,
      registration.submittedAt,
      registration.updatedAt,
      registration.teamName ?? "",
      registration.teamSize,
      registration.teamMembers
        .map((member) => [member.name, member.username, member.email, member.role].filter(Boolean).join(" / "))
        .join(" | "),
      registration.attachments.map((attachment) => `${attachment.fileName} (${attachment.url})`).join(" | "),
      registration.note ?? "",
      registration.reviewNote ?? "",
      registration.answers.map((answer) => `${answer.label}: ${answer.value}`).join(" | ")
    ]
      .map(csvEscape)
      .join(",")
  );

  return [headers.map(csvEscape).join(","), ...rows].join("\n");
}

export async function getEventsDashboard(viewer: DevSession): Promise<CampusEventsDashboardResponse> {
  return fetchBackendJson<CampusEventsDashboardResponse>("/v1/events", viewer);
}

export async function getEventForViewer(viewer: DevSession, eventId: string): Promise<CampusEvent | null> {
  try {
    const response = await fetchBackendJson<{ event: CampusEvent }>(eventPath(eventId), viewer);
    return response.event;
  } catch (error) {
    if (isBackendRequestError(error) && error.statusCode === 404) {
      return null;
    }
    throw error;
  }
}

export async function getViewerCampusEventRegistration(
  viewer: DevSession,
  eventId: string
): Promise<CampusEventViewerRegistrationResponse> {
  return fetchBackendJson<CampusEventViewerRegistrationResponse>(eventPath(eventId, "/register"), viewer);
}

export async function getCampusEventNotificationAudience(viewer: DevSession, eventId: string) {
  const response = await fetchBackendJson<CampusEventRegistrationListResponse>(
    eventPath(eventId, "/registrations"),
    viewer
  );
  const event = response.event as StoredEventAudience;
  const audienceUserIds = [
    ...new Set([
      ...(event.savedByUserIds ?? []),
      ...(event.interestedUserIds ?? []),
      ...response.registrations.map((registration) => registration.attendee.userId)
    ])
  ].filter((userId) => userId && userId !== event.host.userId);

  return { event: response.event, audienceUserIds };
}

export async function createCampusEvent(
  viewer: DevSession,
  _identity: EventViewerIdentity,
  payload: CreateCampusEventRequest
): Promise<CreateCampusEventResponse> {
  return postBackendJson<CreateCampusEventResponse>("/v1/events", payload, viewer);
}

export async function updateCampusEvent(
  viewer: DevSession,
  payload: UpdateCampusEventRequest
): Promise<UpdateCampusEventResponse> {
  return mutateBackendJson<UpdateCampusEventResponse>(
    eventPath(payload.eventId),
    "PUT",
    payload,
    viewer
  );
}

export async function toggleCampusEventSave(
  viewer: DevSession,
  eventId: string
): Promise<ToggleCampusEventSaveResponse> {
  return mutateBackendJson<ToggleCampusEventSaveResponse>(eventPath(eventId, "/save"), "PUT", {}, viewer);
}

export async function toggleCampusEventInterest(
  viewer: DevSession,
  eventId: string
): Promise<ToggleCampusEventInterestResponse> {
  return mutateBackendJson<ToggleCampusEventInterestResponse>(
    eventPath(eventId, "/interest"),
    "PUT",
    {},
    viewer
  );
}

export async function upsertCampusEventRegistration(
  viewer: DevSession,
  _identity: EventViewerIdentity,
  payload: UpsertCampusEventRegistrationRequest
): Promise<UpsertCampusEventRegistrationResponse> {
  return postBackendJson<UpsertCampusEventRegistrationResponse>(
    eventPath(payload.eventId, "/register"),
    payload,
    viewer
  );
}

export async function getCampusEventRegistrations(
  viewer: DevSession,
  eventId: string,
  filters?: {
    query?: string | null;
    statuses?: CampusEventRegistrationStatus[];
  }
): Promise<CampusEventRegistrationListResponse> {
  const response = await fetchBackendJson<CampusEventRegistrationListResponse>(
    eventPath(eventId, "/registrations"),
    viewer
  );
  return {
    event: response.event,
    registrations: filterRegistrations(response.registrations, filters)
  };
}

export async function manageCampusEventRegistration(
  viewer: DevSession,
  eventId: string,
  registrationId: string,
  payload: ManageCampusEventRegistrationRequest
): Promise<ManageCampusEventRegistrationResponse> {
  return mutateBackendJson<ManageCampusEventRegistrationResponse>(
    eventPath(eventId, `/registrations/${encodeURIComponent(registrationId)}`),
    "PUT",
    payload,
    viewer
  );
}

export async function exportCampusEventRegistrationsCsv(
  viewer: DevSession,
  eventId: string,
  filters?: {
    query?: string | null;
    statuses?: CampusEventRegistrationStatus[];
  }
) {
  const response = await getCampusEventRegistrations(viewer, eventId, filters);
  return registrationsCsv(response.registrations);
}

export async function cancelCampusEvent(
  viewer: DevSession,
  eventId: string
): Promise<ManageCampusEventResponse> {
  return postBackendJson<ManageCampusEventResponse>(eventPath(eventId, "/cancel"), {}, viewer);
}

export async function deleteCampusEvent(
  viewer: DevSession,
  eventId: string
): Promise<ManageCampusEventResponse> {
  return mutateBackendJson<ManageCampusEventResponse>(eventPath(eventId), "DELETE", {}, viewer);
}
