package pt.unl.fct.di.apdc.projeto.resources;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.ReplyData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.ThreadData;
import pt.unl.fct.di.apdc.projeto.util.ThreadmarkData;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/communities/{communityID}")
public class ThreadResource {

    /**
     * Logger Object
     */
    private static Logger LOG = Logger.getLogger(ThreadResource.class.getName());

    /** Class that stores the server constants to use in operations */
    public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

    /** The data store to store users in */
    private static final Datastore datastore = serverConstants.getDatastore();

    /** The converter to JSON */
    private final Gson g = new Gson();

    public ThreadResource() {
    }

    @POST
    @Path("/thread")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startThread(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID, ThreadData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Start thread: " + authToken.username + " attempted to start a thread to the community with id "
                + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.START_THREAD, user, community, member,
                    token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                String threadID;
                do {
                    threadID = UUID.randomUUID().toString();
                } while (serverConstants.getThread(txn, communityID, threadID) != null);
                Entity thread = Entity.newBuilder(serverConstants.getPostKey(communityID, threadID))
                        .set("threadID", threadID)
                        .set("title", data.title)
                        .set("username", authToken.username)
                        .set("threadStartDate", Timestamp.now())
                        .set("replies", 1L)
                        .set("lastReplyUsername", authToken.username)
                        .set("lastReplyDate", Timestamp.now())
                        .set("isLocked", data.isLocked)
                        .set("isPinned", data.isPinned)
                        .set("pinDate", Timestamp.MIN_VALUE)
                        .set("tags",
                                ListValue.of(data.tags.stream().map(StringValue::new).collect(Collectors.toList())))
                        .build();
                Entity reply = Entity.newBuilder(serverConstants.getThreadReplyKey(communityID, threadID, 1L))
                        .set("replyID", 1L)
                        .set("username", data.lastReply.username)
                        .set("replyBody", data.lastReply.replyBody)
                        .set("replyDate", Timestamp.now())
                        .set("lastEdit", Timestamp.MIN_VALUE)
                        .set("likes", 0L)
                        .set("threadmark", "")
                        .set("previousThreadmark", Long.MIN_VALUE)
                        .set("nextThreadmark", Long.MAX_VALUE)
                        .build();
                txn.put(thread, reply);
                txn.commit();
                LOG.fine("Start thread: " + authToken.username + " posted thread to the community with id "
                        + communityID + ".");
                return Response.ok().entity("Thread post successful.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Start thread: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Start thread: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{threadID}/lock")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response lockThread(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            @PathParam("threadID") String threadID, boolean isLocked) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String lock = isLocked ? "lock" : "unlock";
        LOG.fine("Lock thread: " + authToken.username + " attempted to " + lock + " thread with id " + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.PIN_THREAD, user, community, thread,
                    member, token, authToken, isLocked);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                thread = Entity.newBuilder(thread)
                        .set("isLocked", isLocked)
                        .build();
                txn.put(thread);
                txn.commit();
                LOG.fine("Lock thread: " + authToken.username + " " + lock + "ed thread with id " + threadID + ".");
                return Response.ok().entity("Thread " + lock + "ed successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Lock thread: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Lock thread: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{threadID}/pin")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response pinThread(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            @PathParam("threadID") String threadID, boolean isPinned) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String pin = isPinned ? "pin" : "unpin";
        LOG.fine("Pin thread: " + authToken.username + " attempted to " + pin + " a thread with id " + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.PIN_THREAD, user, community, thread,
                    member, token, authToken, isPinned);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                thread = Entity.newBuilder(thread)
                        .set("isPinned", isPinned)
                        .set("pinDate", isPinned ? Timestamp.now() : Timestamp.MIN_VALUE)
                        .build();
                txn.put(thread);
                txn.commit();
                LOG.fine("Pin thread: " + authToken.username + " " + pin + "ed thread with id " + threadID + ".");
                return Response.ok().entity("Thread " + pin + "ed successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Pin thread: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Pin thread: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @DELETE
    @Path("/{threadID}/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeThread(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID, @PathParam("threadID") String threadID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Remove thread: " + authToken.username + " attempted to remove a thread with id " + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.REMOVE_THREAD, user, community, thread,
                    member, token, authToken);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                var replies = serverConstants.getThreadReplies(txn, thread.getKey());
                replies.forEachRemaining(reply -> {
                    var likes = serverConstants.getThreadReplyLikes(txn, reply.getKey());
                    likes.forEachRemaining(like -> {
                        txn.delete(like.getKey());
                    });
                    txn.delete(reply.getKey());
                });
                txn.delete(thread.getKey());
                txn.commit();
                LOG.fine("Remove thread: " + authToken.username + " removed thread with id " + threadID + ".");
                return Response.ok().entity("Thread removed successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Remove thread: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Remove thread: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{threadID}/tags")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response ThreadTags(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            @PathParam("threadID") String threadID, ThreadData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Tag thread: " + authToken.username + " attempted to tag a thread with id " + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.POST_THREAD_REPLY, user, community, thread,
                    member, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                List<StringValue> newTags = new ArrayList<>(thread.getList("tags"));
                if (data.isTagging) {
                    newTags.addAll(data.tags.stream().map(StringValue::new).collect(Collectors.toList()));
                } else {
                    newTags.removeAll(data.tags.stream().map(StringValue::new).collect(Collectors.toList()));
                }
                thread = Entity.newBuilder(thread)
                        .set("tags", newTags)
                        .build();
                txn.put(thread);
                txn.commit();
                LOG.fine("Tag thread: " + authToken.username + " tagged thread with id " + threadID + ".");
                return Response.ok().entity("Thread tagged successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Tag thread: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Tag thread: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{threadID}/reply")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postReply(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID, @PathParam("threadID") String threadID, ReplyData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Post reply: " + authToken.username + " attempted to reply to thread with id " + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.POST_THREAD_REPLY, user, community, thread,
                    member, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                long replyID = thread.getLong("replies") + 1L;
                thread = Entity.newBuilder(thread)
                        .set("replies", replyID)
                        .set("lastReplyUsername", authToken.username)
                        .set("lastReplyDate", Timestamp.now())
                        .build();
                Entity reply = Entity.newBuilder(serverConstants.getThreadReplyKey(communityID, threadID, replyID))
                        .set("replyID", replyID)
                        .set("username", data.username)
                        .set("replyBody", data.replyBody)
                        .set("replyDate", Timestamp.now())
                        .set("lastEdit", Timestamp.MIN_VALUE)
                        .set("likes", 0L)
                        .set("threadmark", "")
                        .set("previousThreadmark", Long.MIN_VALUE)
                        .set("nextThreadmark", Long.MAX_VALUE)
                        .build();
                txn.put(thread, reply);
                txn.commit();
                LOG.fine("Post reply: " + authToken.username + " posted reply to thread with id " + threadID + ".");
                return Response.ok().entity("Thread reply successful.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Post reply: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Post reply: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{threadID}/{replyID}/edit")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editReply(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            @PathParam("threadID") String threadID, @PathParam("replyID") String replyID, ReplyData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Edit reply: " + authToken.username + " attempted to edit a reply in the thread with id " + threadID
                + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity reply = serverConstants.getThreadReply(txn, communityID, threadID, Long.parseLong(replyID));
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.EDIT_THREAD_REPLY, user, community, thread,
                    reply, member, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                reply = Entity.newBuilder(reply)
                        .set("replyBody", data.replyBody)
                        .set("lastEdit", Timestamp.now())
                        .build();
                txn.put(reply);
                txn.commit();
                LOG.fine("Edit reply: " + authToken.username + " edited reply to thread with id " + threadID + ".");
                return Response.ok().entity("Reply edit successful.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Edit reply: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Edit reply: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{threadID}/{replyID}/threadmark")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response threadmarkReply(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID, @PathParam("threadID") String threadID,
            @PathParam("replyID") String replyID, ThreadmarkData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Add threadmark: " + authToken.username + " attempted to threadmark a reply in the thread with id "
                + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity newThreadmark = serverConstants.getThreadReply(txn, communityID, threadID, Long.parseLong(replyID));
            Entity previousThreadmark = serverConstants.getThreadReply(txn, communityID, threadID, data.previous);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.ADD_THREADMARK, user, community, thread,
                    newThreadmark, previousThreadmark, member, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                previousThreadmark = Entity.newBuilder(previousThreadmark)
                        .set("nextTreadmark", newThreadmark.getString("replyID"))
                        .build();
                newThreadmark = Entity.newBuilder(newThreadmark)
                        .set("threadmark", data.type)
                        .set("previousThreadmark", data.previous)
                        .build();
                txn.put(previousThreadmark, newThreadmark);
                txn.commit();
                LOG.fine("Add threadmark: " + authToken.username + " added threadmark to thread with id " + threadID
                        + ".");
                return Response.ok().entity("Reply threadmarked successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Add threadmark: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Add threadmark: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{threadID}/{replyID}/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeReply(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID, @PathParam("threadID") String threadID,
            @PathParam("replyID") String replyID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Remove reply: " + authToken.username + " attempted to remove a reply in the thread with id "
                + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity reply = serverConstants.getThreadReply(txn, communityID, threadID, Long.parseLong(replyID));
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.REMOVE_THREAD_REPLY, user, community,
                    thread, reply, member, token, authToken);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                var likes = serverConstants.getThreadReplyLikes(txn,
                        serverConstants.getThreadReplyKey(communityID, threadID, Long.parseLong(replyID)));
                likes.forEachRemaining(like -> {
                    txn.delete(like.getKey());
                });
                reply = Entity.newBuilder(reply)
                        .set("replyBody", "[Redacted]")
                        .set("lastEdit", Timestamp.now())
                        .set("likes", 0L)
                        .build();
                txn.put(reply);
                txn.commit();
                LOG.fine("Remove reply: " + authToken.username + " removed reply to thread with id " + threadID + ".");
                return Response.ok().entity("Reply removed successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Remove reply: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Remove reply: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/{threadID}/{replyID}/like")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response likeReply(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            @PathParam("threadID") String threadID, @PathParam("replyID") String replyID, boolean isLiked) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String like = isLiked ? "like" : "unlike";
        LOG.fine("Like reply: " + authToken.username + " attempted to " + like + " a reply in the thread with id "
                + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity reply = serverConstants.getThreadReply(txn, communityID, threadID, Long.parseLong(replyID));
            Entity likeRelation = serverConstants.getThreadReplyLike(txn, communityID, threadID,
                    Long.parseLong(replyID), authToken.username);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.LIKE_THREAD_REPLY, user, community, thread,
                    reply, likeRelation, member, token, authToken, isLiked);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                reply = Entity.newBuilder(reply)
                        .set("likes", isLiked ? reply.getLong("likes") + 1L : reply.getLong("likes") - 1L)
                        .build();
                if (isLiked) {
                    likeRelation = Entity
                            .newBuilder(serverConstants.getThreadReplyLikeKey(communityID, threadID,
                                    Long.parseLong(replyID), authToken.username))
                            .set("username", authToken.username)
                            .set("likeDate", Timestamp.now())
                            .build();
                    txn.put(reply, likeRelation);
                } else {
                    txn.delete(likeRelation.getKey());
                    txn.put(reply);
                }
                txn.commit();
                LOG.fine("Like reply: " + authToken.username + " " + like + "d reply to thread with id " + threadID
                        + ".");
                return Response.ok().entity("Reply " + like + "d successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Like reply: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Like reply: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @GET
    @Path("/{threadID}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getThread(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID,
            @PathParam("threadID") String threadID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Get thread: " + authToken.username + " attempted to get a thread with id " + threadID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkThreadsValidations(Validations.GET_THREAD, user, community, thread,
                    member, token, authToken);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                var results = serverConstants.getThreadReplies(txn, thread.getKey());
                List<ReplyData> replies = new LinkedList<>();
                while (results.hasNext()) {
                    var next = results.next();
                    var like = serverConstants.getThreadReplyLike(txn, communityID, threadID, next.getLong("replyID"), authToken.username);
                    ReplyData reply = new ReplyData(next.getLong("replyID"), next.getString("username"),
                            next.getString("replyBody"), next.getTimestamp("replyDate"), next.getTimestamp("lastEdit"),
                            next.getLong("likes"), like != null);
                    var threadmark = next.getString("threadmark");
                    var previousThreadmark = next.getLong("previousThreadmark");
                    var nextThreadmark = next.getLong("nextThreadmark");
                    if (threadmark == null || threadmark.trim().isEmpty()) {
                    } else if (previousThreadmark != Long.MIN_VALUE && nextThreadmark == Long.MAX_VALUE) {
                        reply.threadmark = new ThreadmarkData(threadmark, previousThreadmark);
                    } else if (previousThreadmark == Long.MIN_VALUE && nextThreadmark != Long.MAX_VALUE) {
                        reply.threadmark = new ThreadmarkData(threadmark, nextThreadmark, true);
                    } else if (previousThreadmark == Long.MIN_VALUE && nextThreadmark == Long.MAX_VALUE) {
                        reply.threadmark = new ThreadmarkData(threadmark);
                    } else {
                        reply.threadmark = new ThreadmarkData(threadmark, next.getLong("previousThreadmark"),
                                next.getLong("nextThreadmark"));
                    }
                    replies.add(reply);
                }
                replies.sort(Comparator.comparing(ReplyData::getReplyID));
                txn.commit();
                LOG.fine("Get thread: " + authToken.username + " received thread with id " + threadID + ".");
                return Response.ok(g.toJson(replies)).build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Get thread: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Get thread: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}