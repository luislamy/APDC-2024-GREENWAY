package pt.unl.fct.di.apdc.projeto.resources;

import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import pt.unl.fct.di.apdc.projeto.util.PostData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/communities")
public class PostResource {

    /**
     * Logger Object
     */
    private static Logger LOG = Logger.getLogger(PostResource.class.getName());

    /** Class that stores the server constants to use in operations */
    public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

    /** The data store to store users in */
    private static final Datastore datastore = serverConstants.getDatastore();

    /** The converter to JSON */
    private final Gson g = new Gson();

    public PostResource() {
    }

    @POST
    @Path("/{communityID}/post")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addPost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine(
                "Add post: " + authToken.username + " attempted to post to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkPostsValidations(Validations.ADD_POST, user, community, member, token,
                    authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                String postID;
                do {
                    postID = UUID.randomUUID().toString();
                } while ( serverConstants.getPost(txn, communityID, postID) != null );
                Entity post = Entity.newBuilder(serverConstants.getPostKey(communityID, postID))
                        .set("postID", postID)
                        .set("title", data.title)
                        .set("postBody", data.postBody)
                        .set("username", authToken.username)
                        .set("postDate", Timestamp.now())
                        .set("lastEdit", Timestamp.MIN_VALUE)
                        .set("likes", 0L)
                        .set("dislikes", 0L)
                        .set("comments", 0L)
                        .set("isLocked", data.isLocked)
                        .set("isPinned", data.isPinned)
                        .set("pinDate", Timestamp.MIN_VALUE)
                        .build();
                txn.put(post);
                txn.commit();
                LOG.fine("Add post: " + authToken.username + " posted to the community with id " + communityID + ".");
                return Response.ok().entity("Post successful.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Add post: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Add post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @GET
    @Path("/{communityID}/{postID}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            @PathParam("postID") String postID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Get post: " + authToken.username + " attempted to get post with id " + postID
                + " in the community with id " + communityID + ".");
        try {
            Entity user = serverConstants.getUser(authToken.username);
            Entity community = serverConstants.getCommunity(communityID);
            Entity member = serverConstants.getCommunityMember(communityID, authToken.username);
            Entity post = serverConstants.getPost(communityID, postID);
            Entity token = serverConstants.getToken(authToken.username, authToken.tokenID);
            var validation = Validations.checkPostsValidations(Validations.GET_POST, user, community, post, member,
                    token, authToken);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(authToken.username, authToken.tokenID);
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                return validation;
            } else {

                datastore.put(post);
                LOG.fine("Get post: " + authToken.username + " posted to the community with id " + communityID + ".");
                return Response.ok().build();
            }
        } catch (Exception e) {
            LOG.severe("Get post: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    @Path("/{communityID}/post/edit")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editPost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Edit post: " + authToken.username + " attempted to edit post with id " + data.postID
                + "  to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkPostsValidations(Validations.EDIT_POST, user, community, post, member,
                    token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                post = Entity.newBuilder(post.getKey())
                        .set("postID", post.getString("postID"))
                        .set("title", data.title)
                        .set("postBody", data.postBody)
                        .set("username", post.getString("username"))
                        .set("postDate", post.getTimestamp("postDate"))
                        .set("lastEdit", Timestamp.now())
                        .set("likes", post.getLong("likes"))
                        .set("dislikes", post.getLong("dislikes"))
                        .set("comments", post.getLong("comments"))
                        .set("isLocked", post.getBoolean("isLocked"))
                        .set("isPinned", post.getBoolean("isPinned"))
                        .set("pinDate", post.getTimestamp("pinDate"))
                        .build();
                txn.put(post);
                txn.commit();
                LOG.fine("Edit post: " + authToken.username + " posted to the community with id " + communityID + ".");
                return Response.ok().entity("Post edit successful").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Edit post: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Edit post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{communityID}/{postID}/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removePost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            @PathParam("postID") String postID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Remove post: " + authToken.username + " attempted to remove post with id " + postID
                + " from the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, postID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkPostsValidations(Validations.REMOVE_POST, user, community, post, member,
                    token, authToken);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                var comments = serverConstants.getCommentsFromPost(txn, post.getKey());
                while (comments.hasNext()) {
                    var nextKey = comments.next().getKey();
                    var commentLikes = serverConstants.getLikesFromComment(txn, nextKey);
                    while (commentLikes.hasNext()) {
                        txn.delete(commentLikes.next().getKey());
                    }
                    var commentDislikes = serverConstants.getDislikesFromComment(txn, nextKey);
                    while (commentDislikes.hasNext()) {
                        txn.delete(commentDislikes.next().getKey());
                    }
                    txn.delete(nextKey);
                }
                var likes = serverConstants.getLikesFromPost(txn, post.getKey());
                while (likes.hasNext()) {
                    txn.delete(likes.next().getKey());
                }
                var dislikes = serverConstants.getDislikesFromPost(txn, post.getKey());
                while (dislikes.hasNext()) {
                    txn.delete(dislikes.next().getKey());
                }
                txn.delete(post.getKey());
                txn.commit();
                LOG.fine("Remove post: " + authToken.username + " removed post from the community with id "
                        + communityID + ".");
                return Response.ok().entity("Post removal successful").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Remove post: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Remove post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{communityID}/post/lock")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response lockPost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String lock = data.isLocked ? "lock" : "unlock";
        LOG.fine("Lock post: " + authToken.username + " attempted to " + lock + " post with id " + data.postID
                + " from the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkPostsValidations(Validations.LOCK_POST, user, community, post, member,
                    token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                post = Entity.newBuilder(post.getKey())
                        .set("postID", post.getString("postID"))
                        .set("title", post.getString("title"))
                        .set("postBody", post.getString("postBody"))
                        .set("username", post.getString("username"))
                        .set("postDate", post.getTimestamp("postDate"))
                        .set("lastEdit", post.getTimestamp("lastEdit"))
                        .set("likes", post.getLong("likes"))
                        .set("dislikes", post.getLong("dislikes"))
                        .set("comments", post.getLong("comments"))
                        .set("isLocked",
                                data.isLocked != post.getBoolean("isLocked") ? data.isLocked
                                        : post.getBoolean("isLocked"))
                        .set("isPinned", post.getBoolean("isPinned"))
                        .set("pinDate", post.getTimestamp("pinDate"))
                        .build();
                txn.put(post);
                txn.commit();
                LOG.fine("Lock post: " + authToken.username + " " + lock + "ed post from the community with id "
                        + communityID + ".");
                return Response.ok().entity("Post " + lock + " successful.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Lock post: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Lock post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{communityID}/post/pin")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response pinPost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String pin = data.isPinned ? "pin" : "unpin";
        LOG.fine("Pin post: " + authToken.username + " attempted to " + pin + " post with id " + data.postID
                + " from the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkPostsValidations(Validations.PIN_POST, user, community, post, member,
                    token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                post = Entity.newBuilder(post.getKey())
                        .set("postID", post.getString("postID"))
                        .set("title", post.getString("title"))
                        .set("postBody", post.getString("postBody"))
                        .set("username", post.getString("username"))
                        .set("postDate", post.getTimestamp("postDate"))
                        .set("lastEdit", post.getTimestamp("lastEdit"))
                        .set("likes", post.getLong("likes"))
                        .set("dislikes", post.getLong("dislikes"))
                        .set("comments", post.getLong("comments"))
                        .set("isLocked", post.getBoolean("isLocked"))
                        .set("isPinned", data.isPinned)
                        .set("pinDate", data.isPinned ? Timestamp.now() : Timestamp.MIN_VALUE)
                        .build();
                txn.put(post);
                txn.commit();
                LOG.fine("Pin post: " + authToken.username + " " + pin + "ned post from the community with id "
                        + communityID + ".");
                return Response.ok().entity("Post " + pin + " successful").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Pin post: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Pin post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{communityID}/post/like")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response likePost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String like = data.isLiked ? "like" : "unlike";
        LOG.fine("Like post: " + authToken.username + " attempted to " + like + " post with id " + data.postID
                + " from the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            Key likeKey = serverConstants.getPostLikeKey(communityID, data.postID, authToken.username);
            Entity likeRelation = serverConstants.getPostLike(txn, communityID, data.postID, authToken.username);
            var validation = Validations.checkPostsValidations(Validations.LIKE_POST, user, community, post, member,
                    likeRelation, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                post = Entity.newBuilder(post.getKey())
                        .set("postID", post.getString("postID"))
                        .set("title", post.getString("title"))
                        .set("postBody", post.getString("postBody"))
                        .set("username", post.getString("username"))
                        .set("postDate", post.getTimestamp("postDate"))
                        .set("lastEdit", post.getTimestamp("lastEdit"))
                        .set("likes", data.isLiked ? post.getLong("likes") + 1L : post.getLong("likes") - 1L)
                        .set("dislikes", post.getLong("dislikes"))
                        .set("comments", post.getLong("comments"))
                        .set("isLocked", post.getBoolean("isLocked"))
                        .set("isPinned", data.isPinned)
                        .set("pinDate", data.isPinned ? Timestamp.now() : Timestamp.MIN_VALUE)
                        .build();
                if (data.isLiked) {
                    likeRelation = Entity.newBuilder(likeKey)
                            .set("username", authToken.username)
                            .set("likeDate", Timestamp.now())
                            .build();
                    Entity dislikeRelation = serverConstants.getPostDislike(txn, communityID, data.postID,
                            authToken.username);
                    if (dislikeRelation != null)
                        txn.delete(dislikeRelation.getKey());
                    txn.put(post, likeRelation);
                } else {
                    txn.put(post);
                    txn.delete(likeKey);
                }
                txn.commit();
                LOG.fine("Like post: " + authToken.username + " " + like + "d post from the community with id "
                        + communityID + ".");
                return Response.ok().entity("Post " + like + " successful.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Like post: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Like post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{communityID}/post/dislike")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response dislikePost(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID, PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String dislike = data.isDisliked ? "dislike" : "undislike";
        LOG.fine("Dislike post: " + authToken.username + " attempted to " + dislike + " post with id " + data.postID
                + " from the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity post = serverConstants.getPost(communityID, data.postID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            Key dislikeKey = serverConstants.getPostDislikeKey(communityID, data.postID, authToken.username);
            Entity dislikeRelation = serverConstants.getPostDislike(txn, communityID, data.postID, authToken.username);
            var validation = Validations.checkPostsValidations(Validations.DISLIKE_POST, user, community, post, member,
                    dislikeRelation, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                post = Entity.newBuilder(post.getKey())
                        .set("postID", post.getString("postID"))
                        .set("title", post.getString("title"))
                        .set("postBody", post.getString("postBody"))
                        .set("username", post.getString("username"))
                        .set("postDate", post.getTimestamp("postDate"))
                        .set("lastEdit", post.getTimestamp("lastEdit"))
                        .set("likes", post.getLong("likes"))
                        .set("dislikes",
                                data.isDisliked ? post.getLong("dislikes") + 1L : post.getLong("dislikes") - 1L)
                        .set("comments", post.getLong("comments"))
                        .set("isLocked", post.getBoolean("isLocked"))
                        .set("isPinned", data.isPinned)
                        .set("pinDate", data.isPinned ? Timestamp.now() : Timestamp.MIN_VALUE)
                        .build();
                if (data.isDisliked) {
                    dislikeRelation = Entity.newBuilder(dislikeKey)
                            .set("username", authToken.username)
                            .set("dislikeDate", Timestamp.now())
                            .build();
                    Entity likeRelation = serverConstants.getPostLike(txn, communityID, data.postID,
                            authToken.username);
                    if (likeRelation != null)
                        txn.delete(likeRelation.getKey());
                    txn.put(post, dislikeRelation);
                } else {
                    txn.put(post);
                    txn.delete(dislikeKey);
                }
                txn.commit();
                LOG.fine("Dislike post: " + authToken.username + " " + dislike + "d post from the community with id "
                        + communityID + ".");
                return Response.ok().entity("Post " + dislike + " successful.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Dislike post: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Dislike post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}