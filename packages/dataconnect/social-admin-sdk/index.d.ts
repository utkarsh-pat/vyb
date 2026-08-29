import { ConnectorConfig, DataConnect, OperationOptions, ExecuteOperationResponse } from 'firebase-admin/data-connect';

export const connectorConfig: ConnectorConfig;

export type TimestampString = string;
export type UUIDString = string;
export type Int64String = string;
export type DateString = string;


export interface ActivateFollowData {
  follow_update?: Follow_Key | null;
}

export interface ActivateFollowVariables {
  id: UUIDString;
}

export interface ActivateUserBlockData {
  userBlock_update?: UserBlock_Key | null;
}

export interface ActivateUserBlockVariables {
  id: UUIDString;
}

export interface AuditLog_Key {
  id: UUIDString;
  __typename?: 'AuditLog_Key';
}

export interface CampusEventStore_Key {
  id: UUIDString;
  __typename?: 'CampusEventStore_Key';
}

export interface ChatConversation_Key {
  id: UUIDString;
  __typename?: 'ChatConversation_Key';
}

export interface ChatIdentity_Key {
  id: UUIDString;
  __typename?: 'ChatIdentity_Key';
}

export interface ChatMessageReaction_Key {
  id: UUIDString;
  __typename?: 'ChatMessageReaction_Key';
}

export interface ChatMessage_Key {
  id: UUIDString;
  __typename?: 'ChatMessage_Key';
}

export interface ChatParticipant_Key {
  id: UUIDString;
  __typename?: 'ChatParticipant_Key';
}

export interface CommentReaction_Key {
  id: UUIDString;
  __typename?: 'CommentReaction_Key';
}

export interface Comment_Key {
  id: UUIDString;
  __typename?: 'Comment_Key';
}

export interface CommunityMembership_Key {
  id: UUIDString;
  __typename?: 'CommunityMembership_Key';
}

export interface Community_Key {
  id: UUIDString;
  __typename?: 'Community_Key';
}

export interface ConnectScore_Key {
  id: string;
  __typename?: 'ConnectScore_Key';
}

export interface ConnectSession_Key {
  id: string;
  __typename?: 'ConnectSession_Key';
}

export interface ContentDailyInsight_Key {
  id: UUIDString;
  __typename?: 'ContentDailyInsight_Key';
}

export interface ContentEvent_Key {
  id: UUIDString;
  __typename?: 'ContentEvent_Key';
}

export interface ContentMeasurementPreference_Key {
  id: UUIDString;
  __typename?: 'ContentMeasurementPreference_Key';
}

export interface ContentPurgeRequest_Key {
  id: UUIDString;
  __typename?: 'ContentPurgeRequest_Key';
}

export interface ContentUniqueViewerDay_Key {
  id: UUIDString;
  __typename?: 'ContentUniqueViewerDay_Key';
}

export interface ContentUniqueViewer_Key {
  id: UUIDString;
  __typename?: 'ContentUniqueViewer_Key';
}

export interface Course_Key {
  id: UUIDString;
  __typename?: 'Course_Key';
}

export interface CreateCommentData {
  comment_insert: Comment_Key;
}

export interface CreateCommentReactionData {
  commentReaction_insert: CommentReaction_Key;
}

export interface CreateCommentReactionVariables {
  id: UUIDString;
  commentReactionKey: string;
  commentId: UUIDString;
  membershipId: UUIDString;
  reactionType: string;
}

export interface CreateCommentVariables {
  id: UUIDString;
  tenantId: UUIDString;
  postId: UUIDString;
  membershipId: UUIDString;
  authorUserId: UUIDString;
  authorEmail?: string | null;
  isAnonymous?: boolean;
  parentCommentId?: UUIDString | null;
  body: string;
  mediaUrl?: string | null;
  mediaType?: string | null;
  mediaMimeType?: string | null;
  mediaSizeBytes?: Int64String | null;
}

export interface CreateContentDailyInsightData {
  contentDailyInsight_insert: ContentDailyInsight_Key;
}

export interface CreateContentDailyInsightVariables {
  id: UUIDString;
  insightKey: string;
  tenantId: UUIDString;
  postId: UUIDString;
  insightDate: DateString;
  impressionCount: number;
  qualifiedViewCount: number;
  uniqueReachCount: number;
  videoPlayCount: number;
  videoViewCount: number;
  replayCount: number;
  watchMsTotal: Int64String;
  completionCount: number;
  carouselSlideCount: number;
}

export interface CreateContentEventData {
  contentEvent_insert: ContentEvent_Key;
}

export interface CreateContentEventVariables {
  id: UUIDString;
  eventKey: string;
  tenantId: UUIDString;
  postId: UUIDString;
  viewerKey: string;
  sessionKey: string;
  eventType: string;
  source: string;
  visibleMs: number;
  watchMs: number;
  progressBasisPoints: number;
  occurredAt: TimestampString;
  expiresAt: TimestampString;
}

export interface CreateContentUniqueViewerData {
  contentUniqueViewer_insert: ContentUniqueViewer_Key;
}

export interface CreateContentUniqueViewerDayData {
  contentUniqueViewerDay_insert: ContentUniqueViewerDay_Key;
}

export interface CreateContentUniqueViewerDayVariables {
  id: UUIDString;
  uniqueKey: string;
  tenantId: UUIDString;
  postId: UUIDString;
  viewerKey: string;
  viewDate: DateString;
  viewedAt: TimestampString;
  expiresAt: TimestampString;
}

export interface CreateContentUniqueViewerVariables {
  id: UUIDString;
  uniqueKey: string;
  tenantId: UUIDString;
  postId: UUIDString;
  viewerKey: string;
  viewedAt: TimestampString;
}

export interface CreateFollowData {
  follow_insert: Follow_Key;
}

export interface CreateFollowVariables {
  id: UUIDString;
  followKey: string;
  tenantId: UUIDString;
  followerUserId: UUIDString;
  followingUserId: UUIDString;
}

export interface CreatePostData {
  post_insert: Post_Key;
}

export interface CreatePostMediaData {
  postMedia_insert: PostMedia_Key;
}

export interface CreatePostMediaVariables {
  tenantId: UUIDString;
  postId: UUIDString;
  mediaUrl?: string | null;
  storagePath: string;
  mediaType: string;
  mimeType: string;
  sizeBytes: Int64String;
  position?: number;
  width?: number | null;
  height?: number | null;
  durationMs?: number | null;
}

export interface CreatePostSaveData {
  postSave_insert: PostSave_Key;
}

export interface CreatePostSaveVariables {
  id: UUIDString;
  tenantId: UUIDString;
  postId: UUIDString;
  userId: UUIDString;
}

export interface CreatePostVariables {
  id?: UUIDString | null;
  tenantId: UUIDString;
  communityId?: UUIDString | null;
  membershipId: UUIDString;
  authorUserId?: UUIDString | null;
  authorUsername?: string;
  authorName?: string;
  authorEmail?: string | null;
  isAnonymous?: boolean;
  allowAnonymousComments?: boolean;
  placement?: string;
  kind: string;
  title?: string | null;
  body: string;
  mediaUrl?: string | null;
  storagePath?: string | null;
  mediaMimeType?: string | null;
  mediaSizeBytes?: Int64String | null;
  location?: string | null;
  status: string;
}

export interface CreatePostWithFeedChangeData {
  post_insert: Post_Key;
  feedChangeEvent_insert: FeedChangeEvent_Key;
}

export interface CreatePostWithFeedChangeVariables {
  id: UUIDString;
  tenantId: UUIDString;
  communityId?: UUIDString | null;
  membershipId: UUIDString;
  authorUserId?: UUIDString | null;
  authorUsername?: string;
  authorName?: string;
  authorEmail?: string | null;
  isAnonymous?: boolean;
  allowAnonymousComments?: boolean;
  visibility?: string;
  placement?: string;
  kind: string;
  title?: string | null;
  body: string;
  mediaUrl?: string | null;
  storagePath?: string | null;
  mediaMimeType?: string | null;
  mediaSizeBytes?: Int64String | null;
  location?: string | null;
  status: string;
  feedChangeId: UUIDString;
  feedChangeEventKey: string;
  feedChangeExpiresAt: TimestampString;
}

export interface CreateReactionData {
  reaction_insert: Reaction_Key;
}

export interface CreateReactionVariables {
  id: UUIDString;
  reactionKey: string;
  postId: UUIDString;
  membershipId: UUIDString;
  reactionType: string;
}

export interface CreateStoryData {
  story_insert: Story_Key;
}

export interface CreateStoryReactionData {
  storyReaction_insert: StoryReaction_Key;
}

export interface CreateStoryReactionVariables {
  id: UUIDString;
  storyReactionKey: string;
  storyId: UUIDString;
  membershipId: UUIDString;
  reactionType: string;
}

export interface CreateStoryVariables {
  id: UUIDString;
  tenantId: UUIDString;
  communityId?: UUIDString | null;
  userId: UUIDString;
  username: string;
  displayName: string;
  mediaType: string;
  mediaUrl: string;
  storagePath?: string | null;
  mediaMimeType?: string | null;
  mediaSizeBytes?: Int64String | null;
  caption?: string | null;
  compositionJson?: string | null;
  visibility?: string;
  expiresAt: TimestampString;
}

export interface CreateStoryViewData {
  storyView_insert: StoryView_Key;
}

export interface CreateStoryViewVariables {
  id: UUIDString;
  storyViewKey: string;
  storyId: UUIDString;
  membershipId: UUIDString;
}

export interface CreateUserBlockData {
  userBlock_insert: UserBlock_Key;
}

export interface CreateUserBlockVariables {
  id: UUIDString;
  blockKey: string;
  tenantId: UUIDString;
  blockerUserId: UUIDString;
  blockedUserId: UUIDString;
}

export interface DeleteContentDailyInsightData {
  contentDailyInsight_delete?: ContentDailyInsight_Key | null;
}

export interface DeleteContentDailyInsightVariables {
  id: UUIDString;
}

export interface DeleteContentEventData {
  contentEvent_delete?: ContentEvent_Key | null;
}

export interface DeleteContentEventVariables {
  id: UUIDString;
}

export interface DeleteContentPurgeRequestData {
  contentPurgeRequest_delete?: ContentPurgeRequest_Key | null;
}

export interface DeleteContentPurgeRequestVariables {
  id: UUIDString;
}

export interface DeleteContentUniqueViewerData {
  contentUniqueViewer_delete?: ContentUniqueViewer_Key | null;
}

export interface DeleteContentUniqueViewerDayData {
  contentUniqueViewerDay_delete?: ContentUniqueViewerDay_Key | null;
}

export interface DeleteContentUniqueViewerDayVariables {
  id: UUIDString;
}

export interface DeleteContentUniqueViewerVariables {
  id: UUIDString;
}

export interface FeedChangeEvent_Key {
  id: UUIDString;
  __typename?: 'FeedChangeEvent_Key';
}

export interface Follow_Key {
  id: UUIDString;
  __typename?: 'Follow_Key';
}

export interface GameLevel_Key {
  id: string;
  __typename?: 'GameLevel_Key';
}

export interface GetCommentReactionByKeyData {
  commentReactions: ({
    id: UUIDString;
    commentId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & CommentReaction_Key)[];
}

export interface GetCommentReactionByKeyVariables {
  commentReactionKey: string;
}

export interface GetContentDailyInsightByKeyData {
  contentDailyInsights: ({
    id: UUIDString;
    insightKey: string;
    tenantId: UUIDString;
    postId: UUIDString;
    insightDate: DateString;
    impressionCount: number;
    qualifiedViewCount: number;
    uniqueReachCount: number;
    videoPlayCount: number;
    videoViewCount: number;
    replayCount: number;
    watchMsTotal: Int64String;
    completionCount: number;
    carouselSlideCount: number;
  } & ContentDailyInsight_Key)[];
}

export interface GetContentDailyInsightByKeyVariables {
  insightKey: string;
}

export interface GetContentEventByKeyData {
  contentEvents: ({
    id: UUIDString;
    eventKey: string;
  } & ContentEvent_Key)[];
}

export interface GetContentEventByKeyVariables {
  eventKey: string;
}

export interface GetContentMeasurementPreferenceData {
  contentMeasurementPreferences: ({
    id: UUIDString;
    preferenceKey: string;
    tenantId: UUIDString;
    userId: UUIDString;
    measurementEnabled: boolean;
  } & ContentMeasurementPreference_Key)[];
}

export interface GetContentMeasurementPreferenceVariables {
  preferenceKey: string;
}

export interface GetContentUniqueViewerByKeyData {
  contentUniqueViewers: ({
    id: UUIDString;
    qualifiedViewCount: number;
    lastViewedAt: TimestampString;
  } & ContentUniqueViewer_Key)[];
}

export interface GetContentUniqueViewerByKeyVariables {
  uniqueKey: string;
}

export interface GetContentUniqueViewerDayByKeyData {
  contentUniqueViewerDays: ({
    id: UUIDString;
    qualifiedViewCount: number;
  } & ContentUniqueViewerDay_Key)[];
}

export interface GetContentUniqueViewerDayByKeyVariables {
  uniqueKey: string;
}

export interface GetFollowByKeyData {
  follows: ({
    id: UUIDString;
    tenantId: UUIDString;
    followerUserId: UUIDString;
    followingUserId: UUIDString;
    createdAt: TimestampString;
    deletedAt?: TimestampString | null;
  } & Follow_Key)[];
}

export interface GetFollowByKeyVariables {
  followKey: string;
}

export interface GetPostByIdData {
  post?: {
    id: UUIDString;
    tenantId: UUIDString;
    communityId?: UUIDString | null;
    membershipId: UUIDString;
    authorUserId?: UUIDString | null;
    authorUsername?: string | null;
    authorName?: string | null;
    authorEmail?: string | null;
    isAnonymous: boolean;
    allowAnonymousComments: boolean;
    placement: string;
    kind: string;
    title?: string | null;
    body: string;
    mediaUrl?: string | null;
    storagePath?: string | null;
    mediaMimeType?: string | null;
    mediaSizeBytes?: Int64String | null;
    location?: string | null;
    status: string;
    createdAt: TimestampString;
  } & Post_Key;
}

export interface GetPostByIdVariables {
  id: UUIDString;
}

export interface GetReactionByKeyData {
  reactions: ({
    id: UUIDString;
    postId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & Reaction_Key)[];
}

export interface GetReactionByKeyVariables {
  reactionKey: string;
}

export interface GetRecommendationFeedbackByKeyData {
  recommendationFeedbackRecords: ({
    id: UUIDString;
    feedbackKey: string;
    tenantId: UUIDString;
    postId: UUIDString;
    userId: UUIDString;
    action: string;
    deletedAt?: TimestampString | null;
  } & RecommendationFeedback_Key)[];
}

export interface GetRecommendationFeedbackByKeyVariables {
  feedbackKey: string;
}

export interface GetStoryByIdData {
  story?: {
    id: UUIDString;
    tenantId: UUIDString;
    communityId?: UUIDString | null;
    userId: UUIDString;
    username: string;
    displayName: string;
    mediaType: string;
    mediaUrl: string;
    storagePath?: string | null;
    mediaMimeType?: string | null;
    mediaSizeBytes?: Int64String | null;
    caption?: string | null;
    compositionJson?: string | null;
    visibility: string;
    createdAt: TimestampString;
    expiresAt: TimestampString;
  } & Story_Key;
}

export interface GetStoryByIdVariables {
  id: UUIDString;
}

export interface GetStoryReactionByKeyData {
  storyReactions: ({
    id: UUIDString;
    storyId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & StoryReaction_Key)[];
}

export interface GetStoryReactionByKeyVariables {
  storyReactionKey: string;
}

export interface GetStoryViewByKeyData {
  storyViews: ({
    id: UUIDString;
    storyId: UUIDString;
    membershipId: UUIDString;
    seenAt: TimestampString;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & StoryView_Key)[];
}

export interface GetStoryViewByKeyVariables {
  storyViewKey: string;
}

export interface GetUserBlockByKeyData {
  userBlocks: ({
    id: UUIDString;
    blockKey: string;
    tenantId: UUIDString;
    blockerUserId: UUIDString;
    blockedUserId: UUIDString;
    createdAt: TimestampString;
    updatedAt: TimestampString;
    deletedAt?: TimestampString | null;
  } & UserBlock_Key)[];
}

export interface GetUserBlockByKeyVariables {
  blockKey: string;
}

export interface ListActivePostSavesByPostData {
  postSaves: ({
    id: UUIDString;
    postId: UUIDString;
    userId: UUIDString;
    createdAt: TimestampString;
  } & PostSave_Key)[];
}

export interface ListActivePostSavesByPostVariables {
  postId: UUIDString;
  limit: number;
}

export interface ListActivePostSavesByTenantData {
  postSaves: ({
    id: UUIDString;
    tenantId: UUIDString;
    postId: UUIDString;
    userId: UUIDString;
    createdAt: TimestampString;
  } & PostSave_Key)[];
}

export interface ListActivePostSavesByTenantVariables {
  tenantId: UUIDString;
  limit: number;
}

export interface ListActivePostSavesByUserAndPostData {
  postSaves: ({
    id: UUIDString;
    tenantId: UUIDString;
    postId: UUIDString;
    userId: UUIDString;
    createdAt: TimestampString;
  } & PostSave_Key)[];
}

export interface ListActivePostSavesByUserAndPostVariables {
  tenantId: UUIDString;
  postId: UUIDString;
  userId: UUIDString;
  limit: number;
}

export interface ListCommentReactionsByCommentData {
  commentReactions: ({
    id: UUIDString;
    commentId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & CommentReaction_Key)[];
}

export interface ListCommentReactionsByCommentVariables {
  commentId: UUIDString;
  limit: number;
}

export interface ListCommentReactionsByTenantData {
  commentReactions: ({
    id: UUIDString;
    commentId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & CommentReaction_Key)[];
}

export interface ListCommentReactionsByTenantVariables {
  tenantId: UUIDString;
  limit: number;
}

export interface ListCommentsByPostData {
  comments: ({
    id: UUIDString;
    postId: UUIDString;
    membershipId: UUIDString;
    authorUserId?: UUIDString | null;
    authorEmail?: string | null;
    isAnonymous: boolean;
    parentCommentId?: UUIDString | null;
    body: string;
    mediaUrl?: string | null;
    mediaType?: string | null;
    mediaMimeType?: string | null;
    mediaSizeBytes?: Int64String | null;
    status: string;
    createdAt: TimestampString;
  } & Comment_Key)[];
}

export interface ListCommentsByPostVariables {
  postId: UUIDString;
  limit: number;
}

export interface ListCommentsByTenantData {
  comments: ({
    id: UUIDString;
    postId: UUIDString;
    membershipId: UUIDString;
    authorUserId?: UUIDString | null;
    authorEmail?: string | null;
    isAnonymous: boolean;
    parentCommentId?: UUIDString | null;
    body: string;
    mediaUrl?: string | null;
    mediaType?: string | null;
    mediaMimeType?: string | null;
    mediaSizeBytes?: Int64String | null;
    status: string;
    createdAt: TimestampString;
  } & Comment_Key)[];
}

export interface ListCommentsByTenantVariables {
  tenantId: UUIDString;
  limit: number;
}

export interface ListContentDailyInsightIdsByPostData {
  contentDailyInsights: ({
    id: UUIDString;
  } & ContentDailyInsight_Key)[];
}

export interface ListContentDailyInsightIdsByPostVariables {
  postId: UUIDString;
  limit: number;
}

export interface ListContentDailyInsightsByPostData {
  contentDailyInsights: ({
    insightDate: DateString;
    impressionCount: number;
    qualifiedViewCount: number;
    uniqueReachCount: number;
    videoPlayCount: number;
    videoViewCount: number;
    replayCount: number;
    watchMsTotal: Int64String;
    completionCount: number;
    carouselSlideCount: number;
  })[];
}

export interface ListContentDailyInsightsByPostVariables {
  postId: UUIDString;
  since: DateString;
  limit: number;
}

export interface ListContentEventIdsByPostData {
  contentEvents: ({
    id: UUIDString;
  } & ContentEvent_Key)[];
}

export interface ListContentEventIdsByPostVariables {
  postId: UUIDString;
  limit: number;
}

export interface ListContentEventIdsByViewerKeyData {
  contentEvents: ({
    id: UUIDString;
  } & ContentEvent_Key)[];
}

export interface ListContentEventIdsByViewerKeyVariables {
  viewerKey: string;
  limit: number;
}

export interface ListContentEventsByPostWindowData {
  contentEvents: ({
    id: UUIDString;
    tenantId: UUIDString;
    postId: UUIDString;
    viewerKey: string;
    eventType: string;
    visibleMs: number;
    watchMs: number;
    progressBasisPoints: number;
    occurredAt: TimestampString;
  } & ContentEvent_Key)[];
}

export interface ListContentEventsByPostWindowVariables {
  postId: UUIDString;
  from: TimestampString;
  until: TimestampString;
  limit: number;
}

export interface ListContentUniqueViewerDayIdsByPostData {
  contentUniqueViewerDays: ({
    id: UUIDString;
  } & ContentUniqueViewerDay_Key)[];
}

export interface ListContentUniqueViewerDayIdsByPostVariables {
  postId: UUIDString;
  limit: number;
}

export interface ListContentUniqueViewerDayIdsByViewerKeyData {
  contentUniqueViewerDays: ({
    id: UUIDString;
  } & ContentUniqueViewerDay_Key)[];
}

export interface ListContentUniqueViewerDayIdsByViewerKeyVariables {
  viewerKey: string;
  limit: number;
}

export interface ListContentUniqueViewerDaysByPostData {
  contentUniqueViewerDays: ({
    viewerKey: string;
  })[];
}

export interface ListContentUniqueViewerDaysByPostVariables {
  postId: UUIDString;
  since: DateString;
  limit: number;
}

export interface ListContentUniqueViewerIdsByPostData {
  contentUniqueViewers: ({
    id: UUIDString;
  } & ContentUniqueViewer_Key)[];
}

export interface ListContentUniqueViewerIdsByPostVariables {
  postId: UUIDString;
  limit: number;
}

export interface ListContentUniqueViewerIdsByViewerKeyData {
  contentUniqueViewers: ({
    id: UUIDString;
  } & ContentUniqueViewer_Key)[];
}

export interface ListContentUniqueViewerIdsByViewerKeyVariables {
  viewerKey: string;
  limit: number;
}

export interface ListContentUniqueViewersByPostData {
  contentUniqueViewers: ({
    viewerKey: string;
  })[];
}

export interface ListContentUniqueViewersByPostSinceData {
  contentUniqueViewers: ({
    viewerKey: string;
  })[];
}

export interface ListContentUniqueViewersByPostSinceVariables {
  postId: UUIDString;
  since: TimestampString;
  limit: number;
}

export interface ListContentUniqueViewersByPostVariables {
  postId: UUIDString;
  limit: number;
}

export interface ListExpiredContentEventsData {
  contentEvents: ({
    id: UUIDString;
  } & ContentEvent_Key)[];
}

export interface ListExpiredContentEventsVariables {
  before: TimestampString;
  limit: number;
}

export interface ListExpiredContentUniqueViewerDaysData {
  contentUniqueViewerDays: ({
    id: UUIDString;
  } & ContentUniqueViewerDay_Key)[];
}

export interface ListExpiredContentUniqueViewerDaysVariables {
  before: TimestampString;
  limit: number;
}

export interface ListFeedByTenantData {
  posts: ({
    id: UUIDString;
    tenantId: UUIDString;
    communityId?: UUIDString | null;
    membershipId: UUIDString;
    authorUserId?: UUIDString | null;
    authorUsername?: string | null;
    authorName?: string | null;
    authorEmail?: string | null;
    isAnonymous: boolean;
    allowAnonymousComments: boolean;
    placement: string;
    kind: string;
    title?: string | null;
    body: string;
    mediaUrl?: string | null;
    storagePath?: string | null;
    mediaMimeType?: string | null;
    mediaSizeBytes?: Int64String | null;
    location?: string | null;
    status: string;
    createdAt: TimestampString;
  } & Post_Key)[];
}

export interface ListFeedByTenantVariables {
  tenantId: UUIDString;
  placement?: string;
  limit: number;
}

export interface ListFollowersByUserData {
  follows: ({
    id: UUIDString;
    tenantId: UUIDString;
    followerUserId: UUIDString;
    followingUserId: UUIDString;
    createdAt: TimestampString;
  } & Follow_Key)[];
}

export interface ListFollowersByUserVariables {
  tenantId: UUIDString;
  followingUserId: UUIDString;
  limit: number;
}

export interface ListFollowingByUserData {
  follows: ({
    id: UUIDString;
    tenantId: UUIDString;
    followerUserId: UUIDString;
    followingUserId: UUIDString;
    createdAt: TimestampString;
  } & Follow_Key)[];
}

export interface ListFollowingByUserVariables {
  tenantId: UUIDString;
  followerUserId: UUIDString;
  limit: number;
}

export interface ListPostsByAuthorData {
  posts: ({
    id: UUIDString;
    tenantId: UUIDString;
    communityId?: UUIDString | null;
    membershipId: UUIDString;
    authorUserId?: UUIDString | null;
    authorUsername?: string | null;
    authorName?: string | null;
    authorEmail?: string | null;
    isAnonymous: boolean;
    allowAnonymousComments: boolean;
    placement: string;
    kind: string;
    title?: string | null;
    body: string;
    mediaUrl?: string | null;
    storagePath?: string | null;
    mediaMimeType?: string | null;
    mediaSizeBytes?: Int64String | null;
    location?: string | null;
    status: string;
    createdAt: TimestampString;
  } & Post_Key)[];
}

export interface ListPostsByAuthorVariables {
  tenantId: UUIDString;
  authorUserId: UUIDString;
  placement: string;
  limit: number;
}

export interface ListReactionsByPostData {
  reactions: ({
    id: UUIDString;
    postId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & Reaction_Key)[];
}

export interface ListReactionsByPostVariables {
  postId: UUIDString;
  limit: number;
}

export interface ListReactionsByTenantData {
  reactions: ({
    id: UUIDString;
    postId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & Reaction_Key)[];
}

export interface ListReactionsByTenantVariables {
  tenantId: UUIDString;
  limit: number;
}

export interface ListReadyContentPurgeRequestsData {
  contentPurgeRequests: ({
    id: UUIDString;
    postId: UUIDString;
  } & ContentPurgeRequest_Key)[];
}

export interface ListReadyContentPurgeRequestsVariables {
  before: TimestampString;
  limit: number;
}

export interface ListRecentContentEventsForViewerData {
  contentEvents: ({
    id: UUIDString;
  } & ContentEvent_Key)[];
}

export interface ListRecentContentEventsForViewerVariables {
  postId: UUIDString;
  viewerKey: string;
  eventType: string;
  from: TimestampString;
  limit: number;
}

export interface ListRecommendationFeedbackByUserData {
  recommendationFeedbackRecords: ({
    id: UUIDString;
    feedbackKey: string;
    postId: UUIDString;
    action: string;
    updatedAt: TimestampString;
  } & RecommendationFeedback_Key)[];
}

export interface ListRecommendationFeedbackByUserVariables {
  tenantId: UUIDString;
  userId: UUIDString;
  limit: number;
}

export interface ListStoriesByTenantData {
  stories: ({
    id: UUIDString;
    tenantId: UUIDString;
    communityId?: UUIDString | null;
    userId: UUIDString;
    username: string;
    displayName: string;
    mediaType: string;
    mediaUrl: string;
    storagePath?: string | null;
    mediaMimeType?: string | null;
    mediaSizeBytes?: Int64String | null;
    caption?: string | null;
    compositionJson?: string | null;
    visibility: string;
    createdAt: TimestampString;
    expiresAt: TimestampString;
  } & Story_Key)[];
}

export interface ListStoriesByTenantVariables {
  tenantId: UUIDString;
  limit: number;
}

export interface ListStoryReactionsByStoryData {
  storyReactions: ({
    id: UUIDString;
    storyId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & StoryReaction_Key)[];
}

export interface ListStoryReactionsByStoryVariables {
  storyId: UUIDString;
  limit: number;
}

export interface ListStoryReactionsByTenantData {
  storyReactions: ({
    id: UUIDString;
    storyId: UUIDString;
    membershipId: UUIDString;
    reactionType: string;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & StoryReaction_Key)[];
}

export interface ListStoryReactionsByTenantVariables {
  tenantId: UUIDString;
  limit: number;
}

export interface ListStoryViewsByTenantData {
  storyViews: ({
    id: UUIDString;
    storyId: UUIDString;
    membershipId: UUIDString;
    seenAt: TimestampString;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & StoryView_Key)[];
}

export interface ListStoryViewsByTenantVariables {
  tenantId: UUIDString;
  limit: number;
}

export interface ListUnrolledContentEventsData {
  contentEvents: ({
    id: UUIDString;
    tenantId: UUIDString;
    postId: UUIDString;
    viewerKey: string;
    eventType: string;
    visibleMs: number;
    watchMs: number;
    progressBasisPoints: number;
    occurredAt: TimestampString;
  } & ContentEvent_Key)[];
}

export interface ListUnrolledContentEventsVariables {
  limit: number;
}

export interface ListUserBlocksByTenantData {
  userBlocks: ({
    id: UUIDString;
    blockKey: string;
    tenantId: UUIDString;
    blockerUserId: UUIDString;
    blockedUserId: UUIDString;
    createdAt: TimestampString;
    updatedAt: TimestampString;
  } & UserBlock_Key)[];
}

export interface ListUserBlocksByTenantVariables {
  tenantId: UUIDString;
  limit: number;
}

export interface MarkContentEventRolledUpData {
  contentEvent_update?: ContentEvent_Key | null;
}

export interface MarkContentEventRolledUpVariables {
  id: UUIDString;
}

export interface MarketListingContact_Key {
  id: string;
  __typename?: 'MarketListingContact_Key';
}

export interface MarketListingMedia_Key {
  id: string;
  __typename?: 'MarketListingMedia_Key';
}

export interface MarketListingSave_Key {
  id: string;
  __typename?: 'MarketListingSave_Key';
}

export interface MarketListing_Key {
  id: string;
  __typename?: 'MarketListing_Key';
}

export interface MarketRequestContact_Key {
  id: string;
  __typename?: 'MarketRequestContact_Key';
}

export interface MarketRequestMedia_Key {
  id: string;
  __typename?: 'MarketRequestMedia_Key';
}

export interface MarketRequest_Key {
  id: string;
  __typename?: 'MarketRequest_Key';
}

export interface ModerationCase_Key {
  id: UUIDString;
  __typename?: 'ModerationCase_Key';
}

export interface PostMedia_Key {
  id: UUIDString;
  __typename?: 'PostMedia_Key';
}

export interface PostSave_Key {
  id: UUIDString;
  __typename?: 'PostSave_Key';
}

export interface Post_Key {
  id: UUIDString;
  __typename?: 'Post_Key';
}

export interface Reaction_Key {
  id: UUIDString;
  __typename?: 'Reaction_Key';
}

export interface RecommendationFeedback_Key {
  id: UUIDString;
  __typename?: 'RecommendationFeedback_Key';
}

export interface Report_Key {
  id: UUIDString;
  __typename?: 'Report_Key';
}

export interface ResourceFile_Key {
  id: UUIDString;
  __typename?: 'ResourceFile_Key';
}

export interface Resource_Key {
  id: UUIDString;
  __typename?: 'Resource_Key';
}

export interface SoftDeleteCommentData {
  comment_update?: Comment_Key | null;
}

export interface SoftDeleteCommentVariables {
  id: UUIDString;
}

export interface SoftDeleteFollowData {
  follow_update?: Follow_Key | null;
}

export interface SoftDeleteFollowVariables {
  id: UUIDString;
}

export interface SoftDeletePostData {
  post_update?: Post_Key | null;
}

export interface SoftDeletePostSaveData {
  postSave_update?: PostSave_Key | null;
}

export interface SoftDeletePostSaveVariables {
  id: UUIDString;
}

export interface SoftDeletePostVariables {
  id: UUIDString;
}

export interface SoftDeletePostWithPurgeAndFeedChangeData {
  post_update?: Post_Key | null;
  contentPurgeRequest_insert: ContentPurgeRequest_Key;
  feedChangeEvent_insert: FeedChangeEvent_Key;
}

export interface SoftDeletePostWithPurgeAndFeedChangeVariables {
  id: UUIDString;
  tenantId: UUIDString;
  purgeRequestId: UUIDString;
  purgeKey: string;
  actorUserId?: UUIDString | null;
  feedChangeId: UUIDString;
  feedChangeEventKey: string;
  feedChangeExpiresAt: TimestampString;
}

export interface SoftDeletePostWithPurgeData {
  post_update?: Post_Key | null;
  contentPurgeRequest_insert: ContentPurgeRequest_Key;
}

export interface SoftDeletePostWithPurgeVariables {
  id: UUIDString;
  tenantId: UUIDString;
  purgeRequestId: UUIDString;
  purgeKey: string;
}

export interface SoftDeleteUserBlockData {
  userBlock_update?: UserBlock_Key | null;
}

export interface SoftDeleteUserBlockVariables {
  id: UUIDString;
}

export interface StoryReaction_Key {
  id: UUIDString;
  __typename?: 'StoryReaction_Key';
}

export interface StoryView_Key {
  id: UUIDString;
  __typename?: 'StoryView_Key';
}

export interface Story_Key {
  id: UUIDString;
  __typename?: 'Story_Key';
}

export interface TenantDomain_Key {
  id: UUIDString;
  __typename?: 'TenantDomain_Key';
}

export interface TenantMembership_Key {
  id: UUIDString;
  __typename?: 'TenantMembership_Key';
}

export interface Tenant_Key {
  id: UUIDString;
  __typename?: 'Tenant_Key';
}

export interface UpdateCommentReactionData {
  commentReaction_update?: CommentReaction_Key | null;
}

export interface UpdateCommentReactionVariables {
  id: UUIDString;
  reactionType: string;
}

export interface UpdateContentDailyInsightData {
  contentDailyInsight_update?: ContentDailyInsight_Key | null;
}

export interface UpdateContentDailyInsightVariables {
  id: UUIDString;
  impressionCount: number;
  qualifiedViewCount: number;
  uniqueReachCount: number;
  videoPlayCount: number;
  videoViewCount: number;
  replayCount: number;
  watchMsTotal: Int64String;
  completionCount: number;
  carouselSlideCount: number;
}

export interface UpdateContentUniqueViewerData {
  contentUniqueViewer_update?: ContentUniqueViewer_Key | null;
}

export interface UpdateContentUniqueViewerDayData {
  contentUniqueViewerDay_update?: ContentUniqueViewerDay_Key | null;
}

export interface UpdateContentUniqueViewerDayVariables {
  id: UUIDString;
  qualifiedViewCount: number;
  viewedAt: TimestampString;
}

export interface UpdateContentUniqueViewerVariables {
  id: UUIDString;
  qualifiedViewCount: number;
  viewedAt: TimestampString;
}

export interface UpdatePostData {
  post_update?: Post_Key | null;
}

export interface UpdatePostVariables {
  id: UUIDString;
  title?: string | null;
  body: string;
  location?: string | null;
}

export interface UpdatePostWithFeedChangeData {
  post_update?: Post_Key | null;
  feedChangeEvent_insert: FeedChangeEvent_Key;
}

export interface UpdatePostWithFeedChangeVariables {
  id: UUIDString;
  tenantId: UUIDString;
  title?: string | null;
  body: string;
  location?: string | null;
  allowAnonymousComments?: boolean;
  actorUserId?: UUIDString | null;
  feedChangeId: UUIDString;
  feedChangeEventKey: string;
  feedChangeExpiresAt: TimestampString;
}

export interface UpdateReactionData {
  reaction_update?: Reaction_Key | null;
}

export interface UpdateReactionVariables {
  id: UUIDString;
  reactionType: string;
}

export interface UpdateStoryReactionData {
  storyReaction_update?: StoryReaction_Key | null;
}

export interface UpdateStoryReactionVariables {
  id: UUIDString;
  reactionType: string;
}

export interface UpsertContentMeasurementPreferenceData {
  contentMeasurementPreference_upsert: ContentMeasurementPreference_Key;
}

export interface UpsertContentMeasurementPreferenceVariables {
  id: UUIDString;
  preferenceKey: string;
  tenantId: UUIDString;
  userId: UUIDString;
  measurementEnabled: boolean;
}

export interface UpsertRecommendationFeedbackData {
  recommendationFeedback_upsert: RecommendationFeedback_Key;
}

export interface UpsertRecommendationFeedbackVariables {
  id: UUIDString;
  feedbackKey: string;
  tenantId: UUIDString;
  postId: UUIDString;
  userId: UUIDString;
  action: string;
}

export interface UserActivity_Key {
  id: UUIDString;
  __typename?: 'UserActivity_Key';
}

export interface UserBlock_Key {
  id: UUIDString;
  __typename?: 'UserBlock_Key';
}

export interface User_Key {
  id: UUIDString;
  __typename?: 'User_Key';
}

/** Generated Node Admin SDK operation action function for the 'ListFeedByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listFeedByTenant(dc: DataConnect, vars: ListFeedByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListFeedByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListFeedByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listFeedByTenant(vars: ListFeedByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListFeedByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'ListPostsByAuthor' Query. Allow users to execute without passing in DataConnect. */
export function listPostsByAuthor(dc: DataConnect, vars: ListPostsByAuthorVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListPostsByAuthorData>>;
/** Generated Node Admin SDK operation action function for the 'ListPostsByAuthor' Query. Allow users to pass in custom DataConnect instances. */
export function listPostsByAuthor(vars: ListPostsByAuthorVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListPostsByAuthorData>>;

/** Generated Node Admin SDK operation action function for the 'GetPostById' Query. Allow users to execute without passing in DataConnect. */
export function getPostById(dc: DataConnect, vars: GetPostByIdVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetPostByIdData>>;
/** Generated Node Admin SDK operation action function for the 'GetPostById' Query. Allow users to pass in custom DataConnect instances. */
export function getPostById(vars: GetPostByIdVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetPostByIdData>>;

/** Generated Node Admin SDK operation action function for the 'ListCommentsByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listCommentsByTenant(dc: DataConnect, vars: ListCommentsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListCommentsByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListCommentsByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listCommentsByTenant(vars: ListCommentsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListCommentsByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'ListCommentsByPost' Query. Allow users to execute without passing in DataConnect. */
export function listCommentsByPost(dc: DataConnect, vars: ListCommentsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListCommentsByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListCommentsByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listCommentsByPost(vars: ListCommentsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListCommentsByPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListCommentReactionsByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listCommentReactionsByTenant(dc: DataConnect, vars: ListCommentReactionsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListCommentReactionsByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListCommentReactionsByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listCommentReactionsByTenant(vars: ListCommentReactionsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListCommentReactionsByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'ListCommentReactionsByComment' Query. Allow users to execute without passing in DataConnect. */
export function listCommentReactionsByComment(dc: DataConnect, vars: ListCommentReactionsByCommentVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListCommentReactionsByCommentData>>;
/** Generated Node Admin SDK operation action function for the 'ListCommentReactionsByComment' Query. Allow users to pass in custom DataConnect instances. */
export function listCommentReactionsByComment(vars: ListCommentReactionsByCommentVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListCommentReactionsByCommentData>>;

/** Generated Node Admin SDK operation action function for the 'GetCommentReactionByKey' Query. Allow users to execute without passing in DataConnect. */
export function getCommentReactionByKey(dc: DataConnect, vars: GetCommentReactionByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetCommentReactionByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetCommentReactionByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getCommentReactionByKey(vars: GetCommentReactionByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetCommentReactionByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'ListReactionsByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listReactionsByTenant(dc: DataConnect, vars: ListReactionsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListReactionsByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListReactionsByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listReactionsByTenant(vars: ListReactionsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListReactionsByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'ListReactionsByPost' Query. Allow users to execute without passing in DataConnect. */
export function listReactionsByPost(dc: DataConnect, vars: ListReactionsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListReactionsByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListReactionsByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listReactionsByPost(vars: ListReactionsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListReactionsByPostData>>;

/** Generated Node Admin SDK operation action function for the 'GetReactionByKey' Query. Allow users to execute without passing in DataConnect. */
export function getReactionByKey(dc: DataConnect, vars: GetReactionByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetReactionByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetReactionByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getReactionByKey(vars: GetReactionByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetReactionByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'ListActivePostSavesByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listActivePostSavesByTenant(dc: DataConnect, vars: ListActivePostSavesByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListActivePostSavesByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListActivePostSavesByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listActivePostSavesByTenant(vars: ListActivePostSavesByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListActivePostSavesByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'ListActivePostSavesByUserAndPost' Query. Allow users to execute without passing in DataConnect. */
export function listActivePostSavesByUserAndPost(dc: DataConnect, vars: ListActivePostSavesByUserAndPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListActivePostSavesByUserAndPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListActivePostSavesByUserAndPost' Query. Allow users to pass in custom DataConnect instances. */
export function listActivePostSavesByUserAndPost(vars: ListActivePostSavesByUserAndPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListActivePostSavesByUserAndPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListActivePostSavesByPost' Query. Allow users to execute without passing in DataConnect. */
export function listActivePostSavesByPost(dc: DataConnect, vars: ListActivePostSavesByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListActivePostSavesByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListActivePostSavesByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listActivePostSavesByPost(vars: ListActivePostSavesByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListActivePostSavesByPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListStoriesByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listStoriesByTenant(dc: DataConnect, vars: ListStoriesByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListStoriesByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListStoriesByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listStoriesByTenant(vars: ListStoriesByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListStoriesByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'GetStoryById' Query. Allow users to execute without passing in DataConnect. */
export function getStoryById(dc: DataConnect, vars: GetStoryByIdVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetStoryByIdData>>;
/** Generated Node Admin SDK operation action function for the 'GetStoryById' Query. Allow users to pass in custom DataConnect instances. */
export function getStoryById(vars: GetStoryByIdVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetStoryByIdData>>;

/** Generated Node Admin SDK operation action function for the 'ListStoryReactionsByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listStoryReactionsByTenant(dc: DataConnect, vars: ListStoryReactionsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListStoryReactionsByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListStoryReactionsByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listStoryReactionsByTenant(vars: ListStoryReactionsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListStoryReactionsByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'ListStoryReactionsByStory' Query. Allow users to execute without passing in DataConnect. */
export function listStoryReactionsByStory(dc: DataConnect, vars: ListStoryReactionsByStoryVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListStoryReactionsByStoryData>>;
/** Generated Node Admin SDK operation action function for the 'ListStoryReactionsByStory' Query. Allow users to pass in custom DataConnect instances. */
export function listStoryReactionsByStory(vars: ListStoryReactionsByStoryVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListStoryReactionsByStoryData>>;

/** Generated Node Admin SDK operation action function for the 'GetStoryReactionByKey' Query. Allow users to execute without passing in DataConnect. */
export function getStoryReactionByKey(dc: DataConnect, vars: GetStoryReactionByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetStoryReactionByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetStoryReactionByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getStoryReactionByKey(vars: GetStoryReactionByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetStoryReactionByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'ListStoryViewsByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listStoryViewsByTenant(dc: DataConnect, vars: ListStoryViewsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListStoryViewsByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListStoryViewsByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listStoryViewsByTenant(vars: ListStoryViewsByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListStoryViewsByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'GetStoryViewByKey' Query. Allow users to execute without passing in DataConnect. */
export function getStoryViewByKey(dc: DataConnect, vars: GetStoryViewByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetStoryViewByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetStoryViewByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getStoryViewByKey(vars: GetStoryViewByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetStoryViewByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'ListFollowingByUser' Query. Allow users to execute without passing in DataConnect. */
export function listFollowingByUser(dc: DataConnect, vars: ListFollowingByUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListFollowingByUserData>>;
/** Generated Node Admin SDK operation action function for the 'ListFollowingByUser' Query. Allow users to pass in custom DataConnect instances. */
export function listFollowingByUser(vars: ListFollowingByUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListFollowingByUserData>>;

/** Generated Node Admin SDK operation action function for the 'ListFollowersByUser' Query. Allow users to execute without passing in DataConnect. */
export function listFollowersByUser(dc: DataConnect, vars: ListFollowersByUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListFollowersByUserData>>;
/** Generated Node Admin SDK operation action function for the 'ListFollowersByUser' Query. Allow users to pass in custom DataConnect instances. */
export function listFollowersByUser(vars: ListFollowersByUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListFollowersByUserData>>;

/** Generated Node Admin SDK operation action function for the 'GetFollowByKey' Query. Allow users to execute without passing in DataConnect. */
export function getFollowByKey(dc: DataConnect, vars: GetFollowByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetFollowByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetFollowByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getFollowByKey(vars: GetFollowByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetFollowByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'CreatePost' Mutation. Allow users to execute without passing in DataConnect. */
export function createPost(dc: DataConnect, vars: CreatePostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreatePostData>>;
/** Generated Node Admin SDK operation action function for the 'CreatePost' Mutation. Allow users to pass in custom DataConnect instances. */
export function createPost(vars: CreatePostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreatePostData>>;

/** Generated Node Admin SDK operation action function for the 'CreatePostWithFeedChange' Mutation. Allow users to execute without passing in DataConnect. */
export function createPostWithFeedChange(dc: DataConnect, vars: CreatePostWithFeedChangeVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreatePostWithFeedChangeData>>;
/** Generated Node Admin SDK operation action function for the 'CreatePostWithFeedChange' Mutation. Allow users to pass in custom DataConnect instances. */
export function createPostWithFeedChange(vars: CreatePostWithFeedChangeVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreatePostWithFeedChangeData>>;

/** Generated Node Admin SDK operation action function for the 'CreatePostMedia' Mutation. Allow users to execute without passing in DataConnect. */
export function createPostMedia(dc: DataConnect, vars: CreatePostMediaVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreatePostMediaData>>;
/** Generated Node Admin SDK operation action function for the 'CreatePostMedia' Mutation. Allow users to pass in custom DataConnect instances. */
export function createPostMedia(vars: CreatePostMediaVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreatePostMediaData>>;

/** Generated Node Admin SDK operation action function for the 'CreateComment' Mutation. Allow users to execute without passing in DataConnect. */
export function createComment(dc: DataConnect, vars: CreateCommentVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateCommentData>>;
/** Generated Node Admin SDK operation action function for the 'CreateComment' Mutation. Allow users to pass in custom DataConnect instances. */
export function createComment(vars: CreateCommentVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateCommentData>>;

/** Generated Node Admin SDK operation action function for the 'CreateCommentReaction' Mutation. Allow users to execute without passing in DataConnect. */
export function createCommentReaction(dc: DataConnect, vars: CreateCommentReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateCommentReactionData>>;
/** Generated Node Admin SDK operation action function for the 'CreateCommentReaction' Mutation. Allow users to pass in custom DataConnect instances. */
export function createCommentReaction(vars: CreateCommentReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateCommentReactionData>>;

/** Generated Node Admin SDK operation action function for the 'UpdateCommentReaction' Mutation. Allow users to execute without passing in DataConnect. */
export function updateCommentReaction(dc: DataConnect, vars: UpdateCommentReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateCommentReactionData>>;
/** Generated Node Admin SDK operation action function for the 'UpdateCommentReaction' Mutation. Allow users to pass in custom DataConnect instances. */
export function updateCommentReaction(vars: UpdateCommentReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateCommentReactionData>>;

/** Generated Node Admin SDK operation action function for the 'SoftDeleteComment' Mutation. Allow users to execute without passing in DataConnect. */
export function softDeleteComment(dc: DataConnect, vars: SoftDeleteCommentVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeleteCommentData>>;
/** Generated Node Admin SDK operation action function for the 'SoftDeleteComment' Mutation. Allow users to pass in custom DataConnect instances. */
export function softDeleteComment(vars: SoftDeleteCommentVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeleteCommentData>>;

/** Generated Node Admin SDK operation action function for the 'CreateReaction' Mutation. Allow users to execute without passing in DataConnect. */
export function createReaction(dc: DataConnect, vars: CreateReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateReactionData>>;
/** Generated Node Admin SDK operation action function for the 'CreateReaction' Mutation. Allow users to pass in custom DataConnect instances. */
export function createReaction(vars: CreateReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateReactionData>>;

/** Generated Node Admin SDK operation action function for the 'UpdateReaction' Mutation. Allow users to execute without passing in DataConnect. */
export function updateReaction(dc: DataConnect, vars: UpdateReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateReactionData>>;
/** Generated Node Admin SDK operation action function for the 'UpdateReaction' Mutation. Allow users to pass in custom DataConnect instances. */
export function updateReaction(vars: UpdateReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateReactionData>>;

/** Generated Node Admin SDK operation action function for the 'CreatePostSave' Mutation. Allow users to execute without passing in DataConnect. */
export function createPostSave(dc: DataConnect, vars: CreatePostSaveVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreatePostSaveData>>;
/** Generated Node Admin SDK operation action function for the 'CreatePostSave' Mutation. Allow users to pass in custom DataConnect instances. */
export function createPostSave(vars: CreatePostSaveVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreatePostSaveData>>;

/** Generated Node Admin SDK operation action function for the 'SoftDeletePostSave' Mutation. Allow users to execute without passing in DataConnect. */
export function softDeletePostSave(dc: DataConnect, vars: SoftDeletePostSaveVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeletePostSaveData>>;
/** Generated Node Admin SDK operation action function for the 'SoftDeletePostSave' Mutation. Allow users to pass in custom DataConnect instances. */
export function softDeletePostSave(vars: SoftDeletePostSaveVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeletePostSaveData>>;

/** Generated Node Admin SDK operation action function for the 'CreateStory' Mutation. Allow users to execute without passing in DataConnect. */
export function createStory(dc: DataConnect, vars: CreateStoryVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateStoryData>>;
/** Generated Node Admin SDK operation action function for the 'CreateStory' Mutation. Allow users to pass in custom DataConnect instances. */
export function createStory(vars: CreateStoryVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateStoryData>>;

/** Generated Node Admin SDK operation action function for the 'CreateStoryReaction' Mutation. Allow users to execute without passing in DataConnect. */
export function createStoryReaction(dc: DataConnect, vars: CreateStoryReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateStoryReactionData>>;
/** Generated Node Admin SDK operation action function for the 'CreateStoryReaction' Mutation. Allow users to pass in custom DataConnect instances. */
export function createStoryReaction(vars: CreateStoryReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateStoryReactionData>>;

/** Generated Node Admin SDK operation action function for the 'UpdateStoryReaction' Mutation. Allow users to execute without passing in DataConnect. */
export function updateStoryReaction(dc: DataConnect, vars: UpdateStoryReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateStoryReactionData>>;
/** Generated Node Admin SDK operation action function for the 'UpdateStoryReaction' Mutation. Allow users to pass in custom DataConnect instances. */
export function updateStoryReaction(vars: UpdateStoryReactionVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateStoryReactionData>>;

/** Generated Node Admin SDK operation action function for the 'CreateStoryView' Mutation. Allow users to execute without passing in DataConnect. */
export function createStoryView(dc: DataConnect, vars: CreateStoryViewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateStoryViewData>>;
/** Generated Node Admin SDK operation action function for the 'CreateStoryView' Mutation. Allow users to pass in custom DataConnect instances. */
export function createStoryView(vars: CreateStoryViewVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateStoryViewData>>;

/** Generated Node Admin SDK operation action function for the 'UpdatePost' Mutation. Allow users to execute without passing in DataConnect. */
export function updatePost(dc: DataConnect, vars: UpdatePostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdatePostData>>;
/** Generated Node Admin SDK operation action function for the 'UpdatePost' Mutation. Allow users to pass in custom DataConnect instances. */
export function updatePost(vars: UpdatePostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdatePostData>>;

/** Generated Node Admin SDK operation action function for the 'UpdatePostWithFeedChange' Mutation. Allow users to execute without passing in DataConnect. */
export function updatePostWithFeedChange(dc: DataConnect, vars: UpdatePostWithFeedChangeVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdatePostWithFeedChangeData>>;
/** Generated Node Admin SDK operation action function for the 'UpdatePostWithFeedChange' Mutation. Allow users to pass in custom DataConnect instances. */
export function updatePostWithFeedChange(vars: UpdatePostWithFeedChangeVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdatePostWithFeedChangeData>>;

/** Generated Node Admin SDK operation action function for the 'CreateFollow' Mutation. Allow users to execute without passing in DataConnect. */
export function createFollow(dc: DataConnect, vars: CreateFollowVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateFollowData>>;
/** Generated Node Admin SDK operation action function for the 'CreateFollow' Mutation. Allow users to pass in custom DataConnect instances. */
export function createFollow(vars: CreateFollowVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateFollowData>>;

/** Generated Node Admin SDK operation action function for the 'ActivateFollow' Mutation. Allow users to execute without passing in DataConnect. */
export function activateFollow(dc: DataConnect, vars: ActivateFollowVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ActivateFollowData>>;
/** Generated Node Admin SDK operation action function for the 'ActivateFollow' Mutation. Allow users to pass in custom DataConnect instances. */
export function activateFollow(vars: ActivateFollowVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ActivateFollowData>>;

/** Generated Node Admin SDK operation action function for the 'SoftDeleteFollow' Mutation. Allow users to execute without passing in DataConnect. */
export function softDeleteFollow(dc: DataConnect, vars: SoftDeleteFollowVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeleteFollowData>>;
/** Generated Node Admin SDK operation action function for the 'SoftDeleteFollow' Mutation. Allow users to pass in custom DataConnect instances. */
export function softDeleteFollow(vars: SoftDeleteFollowVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeleteFollowData>>;

/** Generated Node Admin SDK operation action function for the 'SoftDeletePost' Mutation. Allow users to execute without passing in DataConnect. */
export function softDeletePost(dc: DataConnect, vars: SoftDeletePostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeletePostData>>;
/** Generated Node Admin SDK operation action function for the 'SoftDeletePost' Mutation. Allow users to pass in custom DataConnect instances. */
export function softDeletePost(vars: SoftDeletePostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeletePostData>>;

/** Generated Node Admin SDK operation action function for the 'ListUserBlocksByTenant' Query. Allow users to execute without passing in DataConnect. */
export function listUserBlocksByTenant(dc: DataConnect, vars: ListUserBlocksByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListUserBlocksByTenantData>>;
/** Generated Node Admin SDK operation action function for the 'ListUserBlocksByTenant' Query. Allow users to pass in custom DataConnect instances. */
export function listUserBlocksByTenant(vars: ListUserBlocksByTenantVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListUserBlocksByTenantData>>;

/** Generated Node Admin SDK operation action function for the 'GetUserBlockByKey' Query. Allow users to execute without passing in DataConnect. */
export function getUserBlockByKey(dc: DataConnect, vars: GetUserBlockByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetUserBlockByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetUserBlockByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getUserBlockByKey(vars: GetUserBlockByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetUserBlockByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'CreateUserBlock' Mutation. Allow users to execute without passing in DataConnect. */
export function createUserBlock(dc: DataConnect, vars: CreateUserBlockVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateUserBlockData>>;
/** Generated Node Admin SDK operation action function for the 'CreateUserBlock' Mutation. Allow users to pass in custom DataConnect instances. */
export function createUserBlock(vars: CreateUserBlockVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateUserBlockData>>;

/** Generated Node Admin SDK operation action function for the 'ActivateUserBlock' Mutation. Allow users to execute without passing in DataConnect. */
export function activateUserBlock(dc: DataConnect, vars: ActivateUserBlockVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ActivateUserBlockData>>;
/** Generated Node Admin SDK operation action function for the 'ActivateUserBlock' Mutation. Allow users to pass in custom DataConnect instances. */
export function activateUserBlock(vars: ActivateUserBlockVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ActivateUserBlockData>>;

/** Generated Node Admin SDK operation action function for the 'SoftDeleteUserBlock' Mutation. Allow users to execute without passing in DataConnect. */
export function softDeleteUserBlock(dc: DataConnect, vars: SoftDeleteUserBlockVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeleteUserBlockData>>;
/** Generated Node Admin SDK operation action function for the 'SoftDeleteUserBlock' Mutation. Allow users to pass in custom DataConnect instances. */
export function softDeleteUserBlock(vars: SoftDeleteUserBlockVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeleteUserBlockData>>;

/** Generated Node Admin SDK operation action function for the 'GetContentMeasurementPreference' Query. Allow users to execute without passing in DataConnect. */
export function getContentMeasurementPreference(dc: DataConnect, vars: GetContentMeasurementPreferenceVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentMeasurementPreferenceData>>;
/** Generated Node Admin SDK operation action function for the 'GetContentMeasurementPreference' Query. Allow users to pass in custom DataConnect instances. */
export function getContentMeasurementPreference(vars: GetContentMeasurementPreferenceVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentMeasurementPreferenceData>>;

/** Generated Node Admin SDK operation action function for the 'UpsertContentMeasurementPreference' Mutation. Allow users to execute without passing in DataConnect. */
export function upsertContentMeasurementPreference(dc: DataConnect, vars: UpsertContentMeasurementPreferenceVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpsertContentMeasurementPreferenceData>>;
/** Generated Node Admin SDK operation action function for the 'UpsertContentMeasurementPreference' Mutation. Allow users to pass in custom DataConnect instances. */
export function upsertContentMeasurementPreference(vars: UpsertContentMeasurementPreferenceVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpsertContentMeasurementPreferenceData>>;

/** Generated Node Admin SDK operation action function for the 'ListRecommendationFeedbackByUser' Query. Allow users to execute without passing in DataConnect. */
export function listRecommendationFeedbackByUser(dc: DataConnect, vars: ListRecommendationFeedbackByUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListRecommendationFeedbackByUserData>>;
/** Generated Node Admin SDK operation action function for the 'ListRecommendationFeedbackByUser' Query. Allow users to pass in custom DataConnect instances. */
export function listRecommendationFeedbackByUser(vars: ListRecommendationFeedbackByUserVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListRecommendationFeedbackByUserData>>;

/** Generated Node Admin SDK operation action function for the 'GetRecommendationFeedbackByKey' Query. Allow users to execute without passing in DataConnect. */
export function getRecommendationFeedbackByKey(dc: DataConnect, vars: GetRecommendationFeedbackByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetRecommendationFeedbackByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetRecommendationFeedbackByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getRecommendationFeedbackByKey(vars: GetRecommendationFeedbackByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetRecommendationFeedbackByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'UpsertRecommendationFeedback' Mutation. Allow users to execute without passing in DataConnect. */
export function upsertRecommendationFeedback(dc: DataConnect, vars: UpsertRecommendationFeedbackVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpsertRecommendationFeedbackData>>;
/** Generated Node Admin SDK operation action function for the 'UpsertRecommendationFeedback' Mutation. Allow users to pass in custom DataConnect instances. */
export function upsertRecommendationFeedback(vars: UpsertRecommendationFeedbackVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpsertRecommendationFeedbackData>>;

/** Generated Node Admin SDK operation action function for the 'SoftDeletePostWithPurge' Mutation. Allow users to execute without passing in DataConnect. */
export function softDeletePostWithPurge(dc: DataConnect, vars: SoftDeletePostWithPurgeVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeletePostWithPurgeData>>;
/** Generated Node Admin SDK operation action function for the 'SoftDeletePostWithPurge' Mutation. Allow users to pass in custom DataConnect instances. */
export function softDeletePostWithPurge(vars: SoftDeletePostWithPurgeVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeletePostWithPurgeData>>;

/** Generated Node Admin SDK operation action function for the 'SoftDeletePostWithPurgeAndFeedChange' Mutation. Allow users to execute without passing in DataConnect. */
export function softDeletePostWithPurgeAndFeedChange(dc: DataConnect, vars: SoftDeletePostWithPurgeAndFeedChangeVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeletePostWithPurgeAndFeedChangeData>>;
/** Generated Node Admin SDK operation action function for the 'SoftDeletePostWithPurgeAndFeedChange' Mutation. Allow users to pass in custom DataConnect instances. */
export function softDeletePostWithPurgeAndFeedChange(vars: SoftDeletePostWithPurgeAndFeedChangeVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<SoftDeletePostWithPurgeAndFeedChangeData>>;

/** Generated Node Admin SDK operation action function for the 'GetContentEventByKey' Query. Allow users to execute without passing in DataConnect. */
export function getContentEventByKey(dc: DataConnect, vars: GetContentEventByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentEventByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetContentEventByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getContentEventByKey(vars: GetContentEventByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentEventByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'ListRecentContentEventsForViewer' Query. Allow users to execute without passing in DataConnect. */
export function listRecentContentEventsForViewer(dc: DataConnect, vars: ListRecentContentEventsForViewerVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListRecentContentEventsForViewerData>>;
/** Generated Node Admin SDK operation action function for the 'ListRecentContentEventsForViewer' Query. Allow users to pass in custom DataConnect instances. */
export function listRecentContentEventsForViewer(vars: ListRecentContentEventsForViewerVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListRecentContentEventsForViewerData>>;

/** Generated Node Admin SDK operation action function for the 'CreateContentEvent' Mutation. Allow users to execute without passing in DataConnect. */
export function createContentEvent(dc: DataConnect, vars: CreateContentEventVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateContentEventData>>;
/** Generated Node Admin SDK operation action function for the 'CreateContentEvent' Mutation. Allow users to pass in custom DataConnect instances. */
export function createContentEvent(vars: CreateContentEventVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateContentEventData>>;

/** Generated Node Admin SDK operation action function for the 'ListUnrolledContentEvents' Query. Allow users to execute without passing in DataConnect. */
export function listUnrolledContentEvents(dc: DataConnect, vars: ListUnrolledContentEventsVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListUnrolledContentEventsData>>;
/** Generated Node Admin SDK operation action function for the 'ListUnrolledContentEvents' Query. Allow users to pass in custom DataConnect instances. */
export function listUnrolledContentEvents(vars: ListUnrolledContentEventsVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListUnrolledContentEventsData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentEventsByPostWindow' Query. Allow users to execute without passing in DataConnect. */
export function listContentEventsByPostWindow(dc: DataConnect, vars: ListContentEventsByPostWindowVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentEventsByPostWindowData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentEventsByPostWindow' Query. Allow users to pass in custom DataConnect instances. */
export function listContentEventsByPostWindow(vars: ListContentEventsByPostWindowVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentEventsByPostWindowData>>;

/** Generated Node Admin SDK operation action function for the 'MarkContentEventRolledUp' Mutation. Allow users to execute without passing in DataConnect. */
export function markContentEventRolledUp(dc: DataConnect, vars: MarkContentEventRolledUpVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<MarkContentEventRolledUpData>>;
/** Generated Node Admin SDK operation action function for the 'MarkContentEventRolledUp' Mutation. Allow users to pass in custom DataConnect instances. */
export function markContentEventRolledUp(vars: MarkContentEventRolledUpVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<MarkContentEventRolledUpData>>;

/** Generated Node Admin SDK operation action function for the 'GetContentDailyInsightByKey' Query. Allow users to execute without passing in DataConnect. */
export function getContentDailyInsightByKey(dc: DataConnect, vars: GetContentDailyInsightByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentDailyInsightByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetContentDailyInsightByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getContentDailyInsightByKey(vars: GetContentDailyInsightByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentDailyInsightByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'CreateContentDailyInsight' Mutation. Allow users to execute without passing in DataConnect. */
export function createContentDailyInsight(dc: DataConnect, vars: CreateContentDailyInsightVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateContentDailyInsightData>>;
/** Generated Node Admin SDK operation action function for the 'CreateContentDailyInsight' Mutation. Allow users to pass in custom DataConnect instances. */
export function createContentDailyInsight(vars: CreateContentDailyInsightVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateContentDailyInsightData>>;

/** Generated Node Admin SDK operation action function for the 'UpdateContentDailyInsight' Mutation. Allow users to execute without passing in DataConnect. */
export function updateContentDailyInsight(dc: DataConnect, vars: UpdateContentDailyInsightVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateContentDailyInsightData>>;
/** Generated Node Admin SDK operation action function for the 'UpdateContentDailyInsight' Mutation. Allow users to pass in custom DataConnect instances. */
export function updateContentDailyInsight(vars: UpdateContentDailyInsightVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateContentDailyInsightData>>;

/** Generated Node Admin SDK operation action function for the 'GetContentUniqueViewerDayByKey' Query. Allow users to execute without passing in DataConnect. */
export function getContentUniqueViewerDayByKey(dc: DataConnect, vars: GetContentUniqueViewerDayByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentUniqueViewerDayByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetContentUniqueViewerDayByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getContentUniqueViewerDayByKey(vars: GetContentUniqueViewerDayByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentUniqueViewerDayByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'CreateContentUniqueViewerDay' Mutation. Allow users to execute without passing in DataConnect. */
export function createContentUniqueViewerDay(dc: DataConnect, vars: CreateContentUniqueViewerDayVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateContentUniqueViewerDayData>>;
/** Generated Node Admin SDK operation action function for the 'CreateContentUniqueViewerDay' Mutation. Allow users to pass in custom DataConnect instances. */
export function createContentUniqueViewerDay(vars: CreateContentUniqueViewerDayVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateContentUniqueViewerDayData>>;

/** Generated Node Admin SDK operation action function for the 'UpdateContentUniqueViewerDay' Mutation. Allow users to execute without passing in DataConnect. */
export function updateContentUniqueViewerDay(dc: DataConnect, vars: UpdateContentUniqueViewerDayVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateContentUniqueViewerDayData>>;
/** Generated Node Admin SDK operation action function for the 'UpdateContentUniqueViewerDay' Mutation. Allow users to pass in custom DataConnect instances. */
export function updateContentUniqueViewerDay(vars: UpdateContentUniqueViewerDayVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateContentUniqueViewerDayData>>;

/** Generated Node Admin SDK operation action function for the 'GetContentUniqueViewerByKey' Query. Allow users to execute without passing in DataConnect. */
export function getContentUniqueViewerByKey(dc: DataConnect, vars: GetContentUniqueViewerByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentUniqueViewerByKeyData>>;
/** Generated Node Admin SDK operation action function for the 'GetContentUniqueViewerByKey' Query. Allow users to pass in custom DataConnect instances. */
export function getContentUniqueViewerByKey(vars: GetContentUniqueViewerByKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<GetContentUniqueViewerByKeyData>>;

/** Generated Node Admin SDK operation action function for the 'CreateContentUniqueViewer' Mutation. Allow users to execute without passing in DataConnect. */
export function createContentUniqueViewer(dc: DataConnect, vars: CreateContentUniqueViewerVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateContentUniqueViewerData>>;
/** Generated Node Admin SDK operation action function for the 'CreateContentUniqueViewer' Mutation. Allow users to pass in custom DataConnect instances. */
export function createContentUniqueViewer(vars: CreateContentUniqueViewerVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<CreateContentUniqueViewerData>>;

/** Generated Node Admin SDK operation action function for the 'UpdateContentUniqueViewer' Mutation. Allow users to execute without passing in DataConnect. */
export function updateContentUniqueViewer(dc: DataConnect, vars: UpdateContentUniqueViewerVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateContentUniqueViewerData>>;
/** Generated Node Admin SDK operation action function for the 'UpdateContentUniqueViewer' Mutation. Allow users to pass in custom DataConnect instances. */
export function updateContentUniqueViewer(vars: UpdateContentUniqueViewerVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<UpdateContentUniqueViewerData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentDailyInsightsByPost' Query. Allow users to execute without passing in DataConnect. */
export function listContentDailyInsightsByPost(dc: DataConnect, vars: ListContentDailyInsightsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentDailyInsightsByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentDailyInsightsByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listContentDailyInsightsByPost(vars: ListContentDailyInsightsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentDailyInsightsByPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerDaysByPost' Query. Allow users to execute without passing in DataConnect. */
export function listContentUniqueViewerDaysByPost(dc: DataConnect, vars: ListContentUniqueViewerDaysByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerDaysByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerDaysByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listContentUniqueViewerDaysByPost(vars: ListContentUniqueViewerDaysByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerDaysByPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewersByPost' Query. Allow users to execute without passing in DataConnect. */
export function listContentUniqueViewersByPost(dc: DataConnect, vars: ListContentUniqueViewersByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewersByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewersByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listContentUniqueViewersByPost(vars: ListContentUniqueViewersByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewersByPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewersByPostSince' Query. Allow users to execute without passing in DataConnect. */
export function listContentUniqueViewersByPostSince(dc: DataConnect, vars: ListContentUniqueViewersByPostSinceVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewersByPostSinceData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewersByPostSince' Query. Allow users to pass in custom DataConnect instances. */
export function listContentUniqueViewersByPostSince(vars: ListContentUniqueViewersByPostSinceVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewersByPostSinceData>>;

/** Generated Node Admin SDK operation action function for the 'ListExpiredContentEvents' Query. Allow users to execute without passing in DataConnect. */
export function listExpiredContentEvents(dc: DataConnect, vars: ListExpiredContentEventsVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListExpiredContentEventsData>>;
/** Generated Node Admin SDK operation action function for the 'ListExpiredContentEvents' Query. Allow users to pass in custom DataConnect instances. */
export function listExpiredContentEvents(vars: ListExpiredContentEventsVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListExpiredContentEventsData>>;

/** Generated Node Admin SDK operation action function for the 'DeleteContentEvent' Mutation. Allow users to execute without passing in DataConnect. */
export function deleteContentEvent(dc: DataConnect, vars: DeleteContentEventVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentEventData>>;
/** Generated Node Admin SDK operation action function for the 'DeleteContentEvent' Mutation. Allow users to pass in custom DataConnect instances. */
export function deleteContentEvent(vars: DeleteContentEventVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentEventData>>;

/** Generated Node Admin SDK operation action function for the 'ListExpiredContentUniqueViewerDays' Query. Allow users to execute without passing in DataConnect. */
export function listExpiredContentUniqueViewerDays(dc: DataConnect, vars: ListExpiredContentUniqueViewerDaysVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListExpiredContentUniqueViewerDaysData>>;
/** Generated Node Admin SDK operation action function for the 'ListExpiredContentUniqueViewerDays' Query. Allow users to pass in custom DataConnect instances. */
export function listExpiredContentUniqueViewerDays(vars: ListExpiredContentUniqueViewerDaysVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListExpiredContentUniqueViewerDaysData>>;

/** Generated Node Admin SDK operation action function for the 'DeleteContentUniqueViewerDay' Mutation. Allow users to execute without passing in DataConnect. */
export function deleteContentUniqueViewerDay(dc: DataConnect, vars: DeleteContentUniqueViewerDayVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentUniqueViewerDayData>>;
/** Generated Node Admin SDK operation action function for the 'DeleteContentUniqueViewerDay' Mutation. Allow users to pass in custom DataConnect instances. */
export function deleteContentUniqueViewerDay(vars: DeleteContentUniqueViewerDayVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentUniqueViewerDayData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentEventIdsByPost' Query. Allow users to execute without passing in DataConnect. */
export function listContentEventIdsByPost(dc: DataConnect, vars: ListContentEventIdsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentEventIdsByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentEventIdsByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listContentEventIdsByPost(vars: ListContentEventIdsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentEventIdsByPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentEventIdsByViewerKey' Query. Allow users to execute without passing in DataConnect. */
export function listContentEventIdsByViewerKey(dc: DataConnect, vars: ListContentEventIdsByViewerKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentEventIdsByViewerKeyData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentEventIdsByViewerKey' Query. Allow users to pass in custom DataConnect instances. */
export function listContentEventIdsByViewerKey(vars: ListContentEventIdsByViewerKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentEventIdsByViewerKeyData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerDayIdsByViewerKey' Query. Allow users to execute without passing in DataConnect. */
export function listContentUniqueViewerDayIdsByViewerKey(dc: DataConnect, vars: ListContentUniqueViewerDayIdsByViewerKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerDayIdsByViewerKeyData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerDayIdsByViewerKey' Query. Allow users to pass in custom DataConnect instances. */
export function listContentUniqueViewerDayIdsByViewerKey(vars: ListContentUniqueViewerDayIdsByViewerKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerDayIdsByViewerKeyData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerIdsByViewerKey' Query. Allow users to execute without passing in DataConnect. */
export function listContentUniqueViewerIdsByViewerKey(dc: DataConnect, vars: ListContentUniqueViewerIdsByViewerKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerIdsByViewerKeyData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerIdsByViewerKey' Query. Allow users to pass in custom DataConnect instances. */
export function listContentUniqueViewerIdsByViewerKey(vars: ListContentUniqueViewerIdsByViewerKeyVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerIdsByViewerKeyData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerDayIdsByPost' Query. Allow users to execute without passing in DataConnect. */
export function listContentUniqueViewerDayIdsByPost(dc: DataConnect, vars: ListContentUniqueViewerDayIdsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerDayIdsByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerDayIdsByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listContentUniqueViewerDayIdsByPost(vars: ListContentUniqueViewerDayIdsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerDayIdsByPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerIdsByPost' Query. Allow users to execute without passing in DataConnect. */
export function listContentUniqueViewerIdsByPost(dc: DataConnect, vars: ListContentUniqueViewerIdsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerIdsByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentUniqueViewerIdsByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listContentUniqueViewerIdsByPost(vars: ListContentUniqueViewerIdsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentUniqueViewerIdsByPostData>>;

/** Generated Node Admin SDK operation action function for the 'ListContentDailyInsightIdsByPost' Query. Allow users to execute without passing in DataConnect. */
export function listContentDailyInsightIdsByPost(dc: DataConnect, vars: ListContentDailyInsightIdsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentDailyInsightIdsByPostData>>;
/** Generated Node Admin SDK operation action function for the 'ListContentDailyInsightIdsByPost' Query. Allow users to pass in custom DataConnect instances. */
export function listContentDailyInsightIdsByPost(vars: ListContentDailyInsightIdsByPostVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListContentDailyInsightIdsByPostData>>;

/** Generated Node Admin SDK operation action function for the 'DeleteContentUniqueViewer' Mutation. Allow users to execute without passing in DataConnect. */
export function deleteContentUniqueViewer(dc: DataConnect, vars: DeleteContentUniqueViewerVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentUniqueViewerData>>;
/** Generated Node Admin SDK operation action function for the 'DeleteContentUniqueViewer' Mutation. Allow users to pass in custom DataConnect instances. */
export function deleteContentUniqueViewer(vars: DeleteContentUniqueViewerVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentUniqueViewerData>>;

/** Generated Node Admin SDK operation action function for the 'DeleteContentDailyInsight' Mutation. Allow users to execute without passing in DataConnect. */
export function deleteContentDailyInsight(dc: DataConnect, vars: DeleteContentDailyInsightVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentDailyInsightData>>;
/** Generated Node Admin SDK operation action function for the 'DeleteContentDailyInsight' Mutation. Allow users to pass in custom DataConnect instances. */
export function deleteContentDailyInsight(vars: DeleteContentDailyInsightVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentDailyInsightData>>;

/** Generated Node Admin SDK operation action function for the 'ListReadyContentPurgeRequests' Query. Allow users to execute without passing in DataConnect. */
export function listReadyContentPurgeRequests(dc: DataConnect, vars: ListReadyContentPurgeRequestsVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListReadyContentPurgeRequestsData>>;
/** Generated Node Admin SDK operation action function for the 'ListReadyContentPurgeRequests' Query. Allow users to pass in custom DataConnect instances. */
export function listReadyContentPurgeRequests(vars: ListReadyContentPurgeRequestsVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<ListReadyContentPurgeRequestsData>>;

/** Generated Node Admin SDK operation action function for the 'DeleteContentPurgeRequest' Mutation. Allow users to execute without passing in DataConnect. */
export function deleteContentPurgeRequest(dc: DataConnect, vars: DeleteContentPurgeRequestVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentPurgeRequestData>>;
/** Generated Node Admin SDK operation action function for the 'DeleteContentPurgeRequest' Mutation. Allow users to pass in custom DataConnect instances. */
export function deleteContentPurgeRequest(vars: DeleteContentPurgeRequestVariables, options?: OperationOptions): Promise<ExecuteOperationResponse<DeleteContentPurgeRequestData>>;

