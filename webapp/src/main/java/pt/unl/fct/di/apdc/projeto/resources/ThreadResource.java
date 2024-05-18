package pt.unl.fct.di.apdc.projeto.resources;

import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
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
import pt.unl.fct.di.apdc.projeto.util.PostData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
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
    
    public ThreadResource() {}

    @POST
    @Path("/thread")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startThread(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Start thread: " + authToken.username + " attempted to start a thread to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkPostsValidations(Validations.ADD_POST, user, community, member, token,
                    authToken, data);
            // TODO: make validations
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                String threadID = UUID.randomUUID().toString();
                Entity thread = Entity.newBuilder(serverConstants.getPostKey(communityID, threadID))
                        .set("threadID", threadID)
                        .set("title", data.title)
                        .set("username", authToken.username)
                        .set("postDate", Timestamp.now())
                        .set("responses", 1L)
                        .set("isLocked", data.isLocked)
                        .set("isPinned", data.isPinned)
                        .set("pinDate", Timestamp.MIN_VALUE)
                        .set("tags", ListValue.of(data.tags.stream().map(StringValue::new).collect(Collectors.toList())))
                        .build();
                Entity firstMessage = Entity.newBuilder(serverConstants.getThreadMessageKey(communityID, threadID, 1L))
                        .set("messageBody", data.postBody)
                        .set("messageDate", Timestamp.now())
                        .set("username", authToken.username)
                        .set("likes", 0L)
                        .build();
                txn.put(thread, firstMessage);
                txn.commit();
                LOG.fine("Start thread: " + authToken.username + " posted thread to the community with id " + communityID + ".");
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
    @Path("/{threadID}/message")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response postResponse(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, @PathParam("threadID") String threadID, PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Message thread: " + authToken.username + " attempted to message a thread to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity thread = serverConstants.getThread(communityID, threadID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkPostsValidations(Validations.ADD_POST, user, community, member, token,
                    authToken, data);
            // TODO: make validations
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                long messageNumber = thread.getLong("responses") + 1L;
                thread = Entity.newBuilder(thread.getKey())
                        .set("threadID", thread.getString("threadID"))
                        .set("title", thread.getString("title"))
                        .set("username", thread.getString("username"))
                        .set("postDate", thread.getTimestamp("postDate"))
                        .set("responses", messageNumber)
                        .set("isLocked", thread.getBoolean("isLocked"))
                        .set("isPinned", thread.getBoolean("isPinned"))
                        .set("pinDate", thread.getTimestamp("pinDate"))
                        .set("tags", thread.getList("tags"))
                        .build();
                Entity message = Entity.newBuilder(serverConstants.getThreadMessageKey(communityID, threadID, messageNumber))
                        .set("messageBody", data.postBody)
                        .set("messageDate", Timestamp.now())
                        .set("username", authToken.username)
                        .set("likes", 0L)
                        .build();
                txn.put(thread, message);
                txn.commit();
                LOG.fine("Message thread: " + authToken.username + " posted message to thread to the community with id " + communityID + ".");
                return Response.ok().entity("Thread post successful.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Message thread: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Message thread: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}
