package pt.unl.fct.di.apdc.projeto.resources;

import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.CommentData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/communities/{communityID}/{postID}/comment")
@Produces(MediaType.APPLICATION_JSON)
public class CommentResource {
    
    /**
     * Logger Object
     */
    private static Logger LOG = Logger.getLogger(CommentResource.class.getName());

    /** Class that stores the server constants to use in operations */
    public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

    /** The data store to store users in */
    private static final Datastore datastore = serverConstants.getDatastore();

    /** The converter to JSON */
    private final Gson g = new Gson();

    public CommentResource() {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/")
    public Response addComment(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, @PathParam("postID") String postID, CommentData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Add comment: " + authToken.username + " attempted to comment on post with id " + postID + " to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity parentComment = data.parentID != null && !data.parentID.trim().isEmpty() ? serverConstants.getParentComment(txn, communityID, postID, data.parentID) : null;
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkAddCommentValidation(user, community, post, parentComment, member, token, authToken, data);
            //TODO: make validations
            validation = Response.ok().build();
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                String commentID = UUID.randomUUID().toString();
                Key commentKey = data.parentID != null && data.parentID.trim().isEmpty() ? serverConstants.getTopLevelCommentKey(communityID, postID, commentID) : serverConstants.getChildCommentKey(communityID, postID, data.parentID, commentID);
                Entity comment = Entity.newBuilder(commentKey)
                    .set("commentID", commentID)
                    .set("postID", postID)
                    .set("parentID", data.parentID != null && !data.parentID.trim().isEmpty() ? data.parentID : null)
                    .set("commentBody", data.postBody)
                    .set("username", authToken.username)
                    .set("postDate", Timestamp.now())
                    .set("lastEdit", Timestamp.MIN_VALUE)
                    .set("likes", 0L)
                    .set("dislikes", 0L)
                    .set("isPinned", data.isPinned)
                    .set("pinDate", Timestamp.MIN_VALUE)
                    .build();
                post = Entity.newBuilder(post).set("comments", post.getLong("comments") + 1L).build();
                txn.put(comment, post);
                txn.commit();
                LOG.fine("Add comment: " + authToken.username + " posted to the community with id " + communityID + ".");
                return Response.ok().entity("Post successful.").build();
            }
        } catch  ( Exception e ) {
			txn.rollback();
			LOG.severe("Add comment: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Add comment: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/edit")
    public Response editComment(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, @PathParam("postID") String postID, CommentData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Edit comment: " + authToken.username + " attempted to edit comment with id " + data.commentID + " on post with id " + postID + " to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity comment = data.parentID != null && !data.parentID.trim().isEmpty() ? 
                serverConstants.getChildComment(communityID, postID, data.parentID, data.commentID) : 
                serverConstants.getTopLevelComment(communityID, postID, data.commentID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkEditCommentValidation(user, community, post, comment, member, token, authToken, data);
            //TODO: make validations
            validation = Response.ok().build();
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                comment = Entity.newBuilder(comment.getKey())
                    .set("commentID", comment.getString("commentID"))
                    .set("postID", comment.getString("postID"))
                    .set("parentID", comment.getString("parentID"))
                    .set("commentBody", data.postBody != null && !data.postBody.trim().isEmpty() ? data.postBody : comment.getString("commentBody"))
                    .set("username", comment.getString("username"))
                    .set("postDate", comment.getTimestamp("postDate"))
                    .set("lastEdit", Timestamp.now())
                    .set("likes", comment.getLong("likes"))
                    .set("dislikes", comment.getLong("dislikes"))
                    .set("isPinned", comment.getBoolean("isPinned"))
                    .set("pinDate", comment.getTimestamp("pinDate"))
                    .build();
                txn.put(comment);
                txn.commit();
                LOG.fine("Edit comment: " + authToken.username + " edited the comment to the community with id " + communityID + ".");
                return Response.ok().entity("Edit successful.").build();
            }
        } catch  ( Exception e ) {
			txn.rollback();
			LOG.severe("Edit comment: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Edit comment: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/remove")
    public Response removeComment(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, @PathParam("postID") String postID, CommentData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Remove comment: " + authToken.username + " attempted to edit comment with id " + data.commentID + " on post with id " + postID + " to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity comment = data.parentID != null && !data.parentID.trim().isEmpty() ? 
                serverConstants.getChildComment(communityID, postID, data.parentID, data.commentID) : 
                serverConstants.getTopLevelComment(communityID, postID, data.commentID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkRemoveCommentValidation(user, community, post, comment, member, token, authToken, data);
            //TODO: make validations
            validation = Response.ok().build();
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                comment = Entity.newBuilder(comment.getKey())
                    .set("commentID", comment.getString("commentID"))
                    .set("postID", comment.getString("postID"))
                    .set("parentID", comment.getString("parentID"))
                    .set("commentBody", "Deleted")
                    .set("username", "Redacted")
                    .set("postDate", comment.getTimestamp("postDate"))
                    .set("lastEdit", Timestamp.now())
                    .set("likes", comment.getLong("likes"))
                    .set("dislikes", comment.getLong("dislikes"))
                    .set("isPinned", false)
                    .set("pinDate", Timestamp.MIN_VALUE)
                    .build();
                txn.put(comment);
                txn.commit();
                LOG.fine("Remove comment: " + authToken.username + " removed the comment to the community with id " + communityID + ".");
                return Response.ok().entity("Remove successful.").build();
            }
        } catch  ( Exception e ) {
			txn.rollback();
			LOG.severe("Remove comment: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Remove comment: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/like")
    public Response likeComment(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, @PathParam("postID") String postID, CommentData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String like = data.isLiked ? "like" : "unlike";
        LOG.fine("Like comment: " + authToken.username + " attempted to " + like + " comment with id " + data.commentID + " on post with id " + postID + " to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity comment = data.parentID != null && !data.parentID.trim().isEmpty() ? 
                serverConstants.getChildComment(communityID, postID, data.parentID, data.commentID) : 
                serverConstants.getTopLevelComment(communityID, postID, data.commentID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Key likeKey = serverConstants.getCommentLikeKey(communityID, postID, data.commentID, authToken.username);
            Entity likeRelation = serverConstants.getCommentLike(txn, communityID, postID, data.commentID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkLikeCommentValidation(user, community, post, comment, member, likeRelation, token, authToken, data);
            //TODO: make validations
            validation = Response.ok().build();
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                comment = Entity.newBuilder(comment.getKey())
                    .set("commentID", comment.getString("commentID"))
                    .set("postID", comment.getString("postID"))
                    .set("parentID", comment.getString("parentID"))
                    .set("commentBody", comment.getString("commentBody"))
                    .set("username", comment.getString("username"))
                    .set("postDate", comment.getTimestamp("postDate"))
                    .set("lastEdit", comment.getTimestamp("lastEdit"))
                    .set("likes", data.isLiked ? comment.getLong("likes") + 1L : comment.getLong("likes") - 1L)
                    .set("dislikes", comment.getLong("dislikes"))
                    .set("isPinned", comment.getBoolean("isPinned"))
                    .set("pinDate", comment.getTimestamp("pinDate"))
                    .build();
                if ( data.isLiked ) {
                    likeRelation = Entity.newBuilder(likeKey)
                        .set("username", authToken.username)
                        .set("likeDate", Timestamp.now())
                        .build();
                    txn.put(comment, likeRelation);
                } else {
                    txn.put(comment);
                    txn.delete(likeKey);
                }
                txn.commit();
                LOG.fine("Like comment: " + authToken.username + " " + like + "d the comment to the post with id " + postID + ".");
                return Response.ok().entity("Like successful.").build();
            }
        } catch  ( Exception e ) {
			txn.rollback();
			LOG.severe("Like comment: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Like comment: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/dislike")
    public Response dislikeComment(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, @PathParam("postID") String postID, CommentData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String dislike = data.isLiked ? "dislike" : "undislike";
        LOG.fine("Like comment: " + authToken.username + " attempted to " + dislike + " comment with id " + data.commentID + " on post with id " + postID + " to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity comment = data.parentID != null && !data.parentID.trim().isEmpty() ? 
                serverConstants.getChildComment(communityID, postID, data.parentID, data.commentID) : 
                serverConstants.getTopLevelComment(communityID, postID, data.commentID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Key dislikeKey = serverConstants.getCommentLikeKey(communityID, postID, data.commentID, authToken.username);
            Entity dislikeRelation = serverConstants.getCommentLike(txn, communityID, postID, data.commentID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkDislikeCommentValidation(user, community, post, comment, member, dislikeRelation, token, authToken, data);
            //TODO: make validations
            validation = Response.ok().build();
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                comment = Entity.newBuilder(comment.getKey())
                    .set("commentID", comment.getString("commentID"))
                    .set("postID", comment.getString("postID"))
                    .set("parentID", comment.getString("parentID"))
                    .set("commentBody", comment.getString("commentBody"))
                    .set("username", comment.getString("username"))
                    .set("postDate", comment.getTimestamp("postDate"))
                    .set("lastEdit", comment.getTimestamp("lastEdit"))
                    .set("likes", comment.getLong("likes"))
                    .set("dislikes", data.isDisliked ? comment.getLong("dislikes") + 1L : comment.getLong("dislikes") - 1L)
                    .set("isPinned", comment.getBoolean("isPinned"))
                    .set("pinDate", comment.getTimestamp("pinDate"))
                    .build();
                if ( data.isDisliked ) {
                    dislikeRelation = Entity.newBuilder(dislikeKey)
                        .set("username", authToken.username)
                        .set("dislikeDate", Timestamp.now())
                        .build();
                    txn.put(comment, dislikeRelation);
                } else {
                    txn.put(comment);
                    txn.delete(dislikeKey);
                }
                txn.commit();
                LOG.fine("Dislike comment: " + authToken.username + " " + dislike + "d the comment to the post with id " + postID + ".");
                return Response.ok().entity("Dislike successful.").build();
            }
        } catch  ( Exception e ) {
			txn.rollback();
			LOG.severe("Dislike comment: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Dislike comment: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/pin")
    public Response pinComment(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, @PathParam("postID") String postID, CommentData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String pin = data.isPinned ? "pin" : "unpin";
        LOG.fine("Pin comment: " + authToken.username + " attempted to " + pin + " comment with id " + data.commentID + " on post with id " + postID + " to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity comment = data.parentID != null && !data.parentID.trim().isEmpty() ? 
                serverConstants.getChildComment(communityID, postID, data.parentID, data.commentID) : 
                serverConstants.getTopLevelComment(communityID, postID, data.commentID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkPinCommentValidation(user, community, post, comment, member, token, authToken, data);
            //TODO: make validations
            validation = Response.ok().build();
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                comment = Entity.newBuilder(comment.getKey())
                    .set("commentID", comment.getString("commentID"))
                    .set("postID", comment.getString("postID"))
                    .set("parentID", comment.getString("parentID"))
                    .set("commentBody", comment.getString("commentBody"))
                    .set("username", comment.getString("username"))
                    .set("postDate", comment.getTimestamp("postDate"))
                    .set("lastEdit", comment.getTimestamp("lastEdit"))
                    .set("likes", comment.getLong("likes"))
                    .set("dislikes", comment.getLong("dislikes"))
                    .set("isPinned", data.isPinned)
                    .set("pinDate", data.isPinned ? Timestamp.now() : Timestamp.MIN_VALUE)
                    .build();
                txn.put(comment);
                txn.commit();
                LOG.fine("Pin comment: " + authToken.username + " " + pin + "d the comment to the post with id " + postID + ".");
                return Response.ok().entity("Pin successful.").build();
            }
        } catch  ( Exception e ) {
			txn.rollback();
			LOG.severe("Pin comment: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Pin comment: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }
}