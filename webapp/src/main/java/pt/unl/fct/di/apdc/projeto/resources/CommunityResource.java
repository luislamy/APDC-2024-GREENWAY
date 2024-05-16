package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import pt.unl.fct.di.apdc.projeto.util.Community;
import pt.unl.fct.di.apdc.projeto.util.CommunityData;
import pt.unl.fct.di.apdc.projeto.util.JoinCommunityData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/communities")
public class CommunityResource {

    /**
     * Logger Object
     */
    private static Logger LOG = Logger.getLogger(CommunityResource.class.getName());

    /** Class that stores the server constants to use in operations */
    public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

    /** The data store to store users in */
    private static final Datastore datastore = serverConstants.getDatastore();

    /** The converter to JSON */
    private final Gson g = new Gson();

    public CommunityResource() {
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response createCommunity(@HeaderParam("authToken") String jsonToken, CommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Attempt to create community by: " + authToken.username + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkCreateCommunityValidation(user, token, authToken);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(authToken.username, authToken.tokenID);
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                return validation;
            } else {
                String communityID = data.communityID;
                if (serverConstants.getCommunity(txn, communityID) == null) {
                    Entity community = Entity.newBuilder(serverConstants.getCommunityKey(communityID))
                            .set("communityID", data.communityID)
                            .set("name", data.name)
                            .set("description", data.description)
                            .set("num_members", 1)
                            .set("username", authToken.username)
                            .set("creationDate", Timestamp.now())
                            .build();
                    Entity member = Entity
                            .newBuilder(serverConstants.getCommunityMemberKey(communityID, authToken.username))
                            .set("communityID", data.communityID)
                            .set("username", authToken.username)
                            .set("joinDate", Timestamp.now())
                            .set("isManager", true)
                            .build();
                    txn.add(community, member);
                    txn.commit();
                    LOG.fine("Create community: " + data.name + " was registered in the database.");
                    return Response.ok().build();
                } else {
                    txn.rollback();
                    LOG.fine("Create community: duplicate username.");
                    return Response.status(Status.CONFLICT).entity("Community already exists.").build();
                }
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Create community: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Create community: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }

    }

    @GET
    @Path("/{communityID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommunity(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Get community: " + authToken.username + " attempted to get community with id " + communityID + ".");
        Entity user = serverConstants.getUser(authToken.username);
        Entity community = serverConstants.getCommunity(communityID);
        Entity token = serverConstants.getToken(authToken.username, authToken.tokenID);
        var validation = Validations.checkGetCommunityValidation(user, community, token, authToken, communityID);
        if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
            serverConstants.removeToken(authToken.username, authToken.tokenID);
            return validation;
        } else if (validation.getStatus() != Status.OK.getStatusCode()) {
            return validation;
        } else {
            // TODO: send boolean true if member of community, false otherwise
            LOG.fine("Get community: " + authToken.username + " received the community.");
            Community communityData = new Community(community.getString("communityID"), community.getString("name"),
                    community.getString("description"),
                    community.getLong("num_members"), community.getString("username"),
                    community.getTimestamp("creationDate"));
            return Response.ok(g.toJson(communityData)).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/join")
    public Response joinCommunity(@HeaderParam("authToken") String jsonToken, JoinCommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Join community: " + authToken.username + " attempted to join the community with id "
                + data.communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, data.communityID);
            Entity member = serverConstants.getCommunityMember(txn, data.communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkJoinCommunityValidation(user, community, member, token, authToken, data.communityID);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                Key memberKey = serverConstants.getCommunityMemberKey(data.communityID, authToken.username);
                member = Entity.newBuilder(memberKey)
                        .set("communityID", data.communityID)
                        .set("username", authToken.username)
                        .set("joinDate", Timestamp.now())
                        .set("isManager", false)
                        .build();
                community = Entity.newBuilder(community.getKey())
                        .set("communityID", community.getString("communityID"))
                        .set("name", community.getString("name"))
                        .set("description", community.getString("description"))
                        .set("num_members", community.getLong("num_members") + 1L)
                        .set("username", community.getString("username"))
                        .set("creationDate", community.getTimestamp("creationDate"))
                        .build();
                txn.put(community, member);
                txn.commit();
                return Response.ok().entity("Community joined successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Login: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Login: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{communityID}/edit")
    public Response editCommunity(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID, CommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Edit community: " + authToken.username + " attempted to edit the community with id " + communityID
                + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity member = serverConstants.getCommunityMember(txn, data.communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkEditCommunityValidation(user, community, member, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                community = Entity.newBuilder(community.getKey())
                        .set("communityID", community.getString("communityID"))
                        .set("name",
                                data.name == null || data.name.trim().isEmpty() ? community.getString("name")
                                        : data.name)
                        .set("description",
                                data.description == null || data.description.trim().isEmpty()
                                        ? community.getString("description")
                                        : data.description)
                        .set("num_members", community.getLong("num_members"))
                        .set("username", community.getString("username"))
                        .set("creationDate", community.getTimestamp("creationDate"))
                        .build();
                txn.update(community);
                txn.commit();
                return Response.ok().entity("Community edited successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Edit community: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Edit community: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

}