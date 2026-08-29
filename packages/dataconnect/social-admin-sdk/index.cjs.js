const { validateAdminArgs } = require('firebase-admin/data-connect');

const connectorConfig = {
  connector: 'social',
  serviceId: 'vyb',
  location: 'asia-south1'
};
exports.connectorConfig = connectorConfig;

function listFeedByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListFeedByTenant', inputVars, inputOpts);
}
exports.listFeedByTenant = listFeedByTenant;

function listPostsByAuthor(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListPostsByAuthor', inputVars, inputOpts);
}
exports.listPostsByAuthor = listPostsByAuthor;

function getPostById(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetPostById', inputVars, inputOpts);
}
exports.getPostById = getPostById;

function listCommentsByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListCommentsByTenant', inputVars, inputOpts);
}
exports.listCommentsByTenant = listCommentsByTenant;

function listCommentsByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListCommentsByPost', inputVars, inputOpts);
}
exports.listCommentsByPost = listCommentsByPost;

function listCommentReactionsByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListCommentReactionsByTenant', inputVars, inputOpts);
}
exports.listCommentReactionsByTenant = listCommentReactionsByTenant;

function listCommentReactionsByComment(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListCommentReactionsByComment', inputVars, inputOpts);
}
exports.listCommentReactionsByComment = listCommentReactionsByComment;

function getCommentReactionByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetCommentReactionByKey', inputVars, inputOpts);
}
exports.getCommentReactionByKey = getCommentReactionByKey;

function listReactionsByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListReactionsByTenant', inputVars, inputOpts);
}
exports.listReactionsByTenant = listReactionsByTenant;

function listReactionsByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListReactionsByPost', inputVars, inputOpts);
}
exports.listReactionsByPost = listReactionsByPost;

function getReactionByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetReactionByKey', inputVars, inputOpts);
}
exports.getReactionByKey = getReactionByKey;

function listActivePostSavesByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListActivePostSavesByTenant', inputVars, inputOpts);
}
exports.listActivePostSavesByTenant = listActivePostSavesByTenant;

function listActivePostSavesByUserAndPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListActivePostSavesByUserAndPost', inputVars, inputOpts);
}
exports.listActivePostSavesByUserAndPost = listActivePostSavesByUserAndPost;

function listActivePostSavesByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListActivePostSavesByPost', inputVars, inputOpts);
}
exports.listActivePostSavesByPost = listActivePostSavesByPost;

function listStoriesByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListStoriesByTenant', inputVars, inputOpts);
}
exports.listStoriesByTenant = listStoriesByTenant;

function getStoryById(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetStoryById', inputVars, inputOpts);
}
exports.getStoryById = getStoryById;

function listStoryReactionsByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListStoryReactionsByTenant', inputVars, inputOpts);
}
exports.listStoryReactionsByTenant = listStoryReactionsByTenant;

function listStoryReactionsByStory(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListStoryReactionsByStory', inputVars, inputOpts);
}
exports.listStoryReactionsByStory = listStoryReactionsByStory;

function getStoryReactionByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetStoryReactionByKey', inputVars, inputOpts);
}
exports.getStoryReactionByKey = getStoryReactionByKey;

function listStoryViewsByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListStoryViewsByTenant', inputVars, inputOpts);
}
exports.listStoryViewsByTenant = listStoryViewsByTenant;

function getStoryViewByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetStoryViewByKey', inputVars, inputOpts);
}
exports.getStoryViewByKey = getStoryViewByKey;

function listFollowingByUser(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListFollowingByUser', inputVars, inputOpts);
}
exports.listFollowingByUser = listFollowingByUser;

function listFollowersByUser(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListFollowersByUser', inputVars, inputOpts);
}
exports.listFollowersByUser = listFollowersByUser;

function getFollowByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetFollowByKey', inputVars, inputOpts);
}
exports.getFollowByKey = getFollowByKey;

function createPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreatePost', inputVars, inputOpts);
}
exports.createPost = createPost;

function createPostWithFeedChange(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreatePostWithFeedChange', inputVars, inputOpts);
}
exports.createPostWithFeedChange = createPostWithFeedChange;

function createPostMedia(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreatePostMedia', inputVars, inputOpts);
}
exports.createPostMedia = createPostMedia;

function createComment(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateComment', inputVars, inputOpts);
}
exports.createComment = createComment;

function createCommentReaction(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateCommentReaction', inputVars, inputOpts);
}
exports.createCommentReaction = createCommentReaction;

function updateCommentReaction(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdateCommentReaction', inputVars, inputOpts);
}
exports.updateCommentReaction = updateCommentReaction;

function softDeleteComment(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('SoftDeleteComment', inputVars, inputOpts);
}
exports.softDeleteComment = softDeleteComment;

function createReaction(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateReaction', inputVars, inputOpts);
}
exports.createReaction = createReaction;

function updateReaction(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdateReaction', inputVars, inputOpts);
}
exports.updateReaction = updateReaction;

function createPostSave(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreatePostSave', inputVars, inputOpts);
}
exports.createPostSave = createPostSave;

function softDeletePostSave(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('SoftDeletePostSave', inputVars, inputOpts);
}
exports.softDeletePostSave = softDeletePostSave;

function createStory(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateStory', inputVars, inputOpts);
}
exports.createStory = createStory;

function createStoryReaction(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateStoryReaction', inputVars, inputOpts);
}
exports.createStoryReaction = createStoryReaction;

function updateStoryReaction(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdateStoryReaction', inputVars, inputOpts);
}
exports.updateStoryReaction = updateStoryReaction;

function createStoryView(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateStoryView', inputVars, inputOpts);
}
exports.createStoryView = createStoryView;

function updatePost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdatePost', inputVars, inputOpts);
}
exports.updatePost = updatePost;

function updatePostWithFeedChange(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdatePostWithFeedChange', inputVars, inputOpts);
}
exports.updatePostWithFeedChange = updatePostWithFeedChange;

function createFollow(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateFollow', inputVars, inputOpts);
}
exports.createFollow = createFollow;

function activateFollow(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('ActivateFollow', inputVars, inputOpts);
}
exports.activateFollow = activateFollow;

function softDeleteFollow(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('SoftDeleteFollow', inputVars, inputOpts);
}
exports.softDeleteFollow = softDeleteFollow;

function softDeletePost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('SoftDeletePost', inputVars, inputOpts);
}
exports.softDeletePost = softDeletePost;

function listUserBlocksByTenant(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUserBlocksByTenant', inputVars, inputOpts);
}
exports.listUserBlocksByTenant = listUserBlocksByTenant;

function getUserBlockByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetUserBlockByKey', inputVars, inputOpts);
}
exports.getUserBlockByKey = getUserBlockByKey;

function createUserBlock(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateUserBlock', inputVars, inputOpts);
}
exports.createUserBlock = createUserBlock;

function activateUserBlock(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('ActivateUserBlock', inputVars, inputOpts);
}
exports.activateUserBlock = activateUserBlock;

function softDeleteUserBlock(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('SoftDeleteUserBlock', inputVars, inputOpts);
}
exports.softDeleteUserBlock = softDeleteUserBlock;

function getContentMeasurementPreference(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetContentMeasurementPreference', inputVars, inputOpts);
}
exports.getContentMeasurementPreference = getContentMeasurementPreference;

function upsertContentMeasurementPreference(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpsertContentMeasurementPreference', inputVars, inputOpts);
}
exports.upsertContentMeasurementPreference = upsertContentMeasurementPreference;

function listRecommendationFeedbackByUser(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListRecommendationFeedbackByUser', inputVars, inputOpts);
}
exports.listRecommendationFeedbackByUser = listRecommendationFeedbackByUser;

function getRecommendationFeedbackByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetRecommendationFeedbackByKey', inputVars, inputOpts);
}
exports.getRecommendationFeedbackByKey = getRecommendationFeedbackByKey;

function upsertRecommendationFeedback(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpsertRecommendationFeedback', inputVars, inputOpts);
}
exports.upsertRecommendationFeedback = upsertRecommendationFeedback;

function softDeletePostWithPurge(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('SoftDeletePostWithPurge', inputVars, inputOpts);
}
exports.softDeletePostWithPurge = softDeletePostWithPurge;

function softDeletePostWithPurgeAndFeedChange(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('SoftDeletePostWithPurgeAndFeedChange', inputVars, inputOpts);
}
exports.softDeletePostWithPurgeAndFeedChange = softDeletePostWithPurgeAndFeedChange;

function getContentEventByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetContentEventByKey', inputVars, inputOpts);
}
exports.getContentEventByKey = getContentEventByKey;

function listRecentContentEventsForViewer(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListRecentContentEventsForViewer', inputVars, inputOpts);
}
exports.listRecentContentEventsForViewer = listRecentContentEventsForViewer;

function createContentEvent(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateContentEvent', inputVars, inputOpts);
}
exports.createContentEvent = createContentEvent;

function listUnrolledContentEvents(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListUnrolledContentEvents', inputVars, inputOpts);
}
exports.listUnrolledContentEvents = listUnrolledContentEvents;

function listContentEventsByPostWindow(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentEventsByPostWindow', inputVars, inputOpts);
}
exports.listContentEventsByPostWindow = listContentEventsByPostWindow;

function markContentEventRolledUp(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('MarkContentEventRolledUp', inputVars, inputOpts);
}
exports.markContentEventRolledUp = markContentEventRolledUp;

function getContentDailyInsightByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetContentDailyInsightByKey', inputVars, inputOpts);
}
exports.getContentDailyInsightByKey = getContentDailyInsightByKey;

function createContentDailyInsight(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateContentDailyInsight', inputVars, inputOpts);
}
exports.createContentDailyInsight = createContentDailyInsight;

function updateContentDailyInsight(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdateContentDailyInsight', inputVars, inputOpts);
}
exports.updateContentDailyInsight = updateContentDailyInsight;

function getContentUniqueViewerDayByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetContentUniqueViewerDayByKey', inputVars, inputOpts);
}
exports.getContentUniqueViewerDayByKey = getContentUniqueViewerDayByKey;

function createContentUniqueViewerDay(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateContentUniqueViewerDay', inputVars, inputOpts);
}
exports.createContentUniqueViewerDay = createContentUniqueViewerDay;

function updateContentUniqueViewerDay(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdateContentUniqueViewerDay', inputVars, inputOpts);
}
exports.updateContentUniqueViewerDay = updateContentUniqueViewerDay;

function getContentUniqueViewerByKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('GetContentUniqueViewerByKey', inputVars, inputOpts);
}
exports.getContentUniqueViewerByKey = getContentUniqueViewerByKey;

function createContentUniqueViewer(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('CreateContentUniqueViewer', inputVars, inputOpts);
}
exports.createContentUniqueViewer = createContentUniqueViewer;

function updateContentUniqueViewer(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('UpdateContentUniqueViewer', inputVars, inputOpts);
}
exports.updateContentUniqueViewer = updateContentUniqueViewer;

function listContentDailyInsightsByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentDailyInsightsByPost', inputVars, inputOpts);
}
exports.listContentDailyInsightsByPost = listContentDailyInsightsByPost;

function listContentUniqueViewerDaysByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentUniqueViewerDaysByPost', inputVars, inputOpts);
}
exports.listContentUniqueViewerDaysByPost = listContentUniqueViewerDaysByPost;

function listContentUniqueViewersByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentUniqueViewersByPost', inputVars, inputOpts);
}
exports.listContentUniqueViewersByPost = listContentUniqueViewersByPost;

function listContentUniqueViewersByPostSince(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentUniqueViewersByPostSince', inputVars, inputOpts);
}
exports.listContentUniqueViewersByPostSince = listContentUniqueViewersByPostSince;

function listExpiredContentEvents(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListExpiredContentEvents', inputVars, inputOpts);
}
exports.listExpiredContentEvents = listExpiredContentEvents;

function deleteContentEvent(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteContentEvent', inputVars, inputOpts);
}
exports.deleteContentEvent = deleteContentEvent;

function listExpiredContentUniqueViewerDays(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListExpiredContentUniqueViewerDays', inputVars, inputOpts);
}
exports.listExpiredContentUniqueViewerDays = listExpiredContentUniqueViewerDays;

function deleteContentUniqueViewerDay(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteContentUniqueViewerDay', inputVars, inputOpts);
}
exports.deleteContentUniqueViewerDay = deleteContentUniqueViewerDay;

function listContentEventIdsByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentEventIdsByPost', inputVars, inputOpts);
}
exports.listContentEventIdsByPost = listContentEventIdsByPost;

function listContentEventIdsByViewerKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentEventIdsByViewerKey', inputVars, inputOpts);
}
exports.listContentEventIdsByViewerKey = listContentEventIdsByViewerKey;

function listContentUniqueViewerDayIdsByViewerKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentUniqueViewerDayIdsByViewerKey', inputVars, inputOpts);
}
exports.listContentUniqueViewerDayIdsByViewerKey = listContentUniqueViewerDayIdsByViewerKey;

function listContentUniqueViewerIdsByViewerKey(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentUniqueViewerIdsByViewerKey', inputVars, inputOpts);
}
exports.listContentUniqueViewerIdsByViewerKey = listContentUniqueViewerIdsByViewerKey;

function listContentUniqueViewerDayIdsByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentUniqueViewerDayIdsByPost', inputVars, inputOpts);
}
exports.listContentUniqueViewerDayIdsByPost = listContentUniqueViewerDayIdsByPost;

function listContentUniqueViewerIdsByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentUniqueViewerIdsByPost', inputVars, inputOpts);
}
exports.listContentUniqueViewerIdsByPost = listContentUniqueViewerIdsByPost;

function listContentDailyInsightIdsByPost(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListContentDailyInsightIdsByPost', inputVars, inputOpts);
}
exports.listContentDailyInsightIdsByPost = listContentDailyInsightIdsByPost;

function deleteContentUniqueViewer(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteContentUniqueViewer', inputVars, inputOpts);
}
exports.deleteContentUniqueViewer = deleteContentUniqueViewer;

function deleteContentDailyInsight(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteContentDailyInsight', inputVars, inputOpts);
}
exports.deleteContentDailyInsight = deleteContentDailyInsight;

function listReadyContentPurgeRequests(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeQuery('ListReadyContentPurgeRequests', inputVars, inputOpts);
}
exports.listReadyContentPurgeRequests = listReadyContentPurgeRequests;

function deleteContentPurgeRequest(dcOrVarsOrOptions, varsOrOptions, options) {
  const { dc: dcInstance, vars: inputVars, options: inputOpts} = validateAdminArgs(connectorConfig, dcOrVarsOrOptions, varsOrOptions, options, true, true);
  dcInstance.useGen(true);
  return dcInstance.executeMutation('DeleteContentPurgeRequest', inputVars, inputOpts);
}
exports.deleteContentPurgeRequest = deleteContentPurgeRequest;

