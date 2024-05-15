package pt.unl.fct.di.apdc.projeto.resources;

import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.Community;
import pt.unl.fct.di.apdc.projeto.util.CommunityData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/communities/community")
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

    public CommunityResource() {}

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response createCommunity(@HeaderParam("authToken") String jsonToken, CommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String username = authToken.username;
        String tokenID = authToken.tokenID;
        long expirationTime = authToken.expirationDate;
        LOG.fine("Attempt to create community by: " + username + ".");
        Key tokenKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", username)).setKind("Token").newKey(tokenID);
        Entity token = datastore.get(tokenKey);
        if (token == null)
            return Response.status(Status.BAD_REQUEST).entity("User is not logged in.").build();
        else if (expirationTime < System.currentTimeMillis()) {
            datastore.delete(tokenKey);
            return Response.status(Status.BAD_REQUEST).entity("User is not logged in because the token expired.").build();
        }
    
        Key userKey = serverConstants.getUserKey(username);
        Transaction txn = datastore.newTransaction();
		try {
            String key = data.nickname;
            Key communityKey = datastore.newKeyFactory().setKind("Community").newKey(key);
            if ( serverConstants.getCommunity(txn, key) == null ) {
                Entity community = Entity.newBuilder(communityKey)
						.set("id", key)
                        .set("name", data.name)
                        .set("description", data.description)
                        .set("num_members", 1)
                        .set("username", username)
                        .set("creationDate", Timestamp.now())
                        .build();
                txn.add(community);
				txn.commit();
				LOG.fine("Create community: " + data.name + " was registered in the database.");
				return Response.ok().build();
            } else {
                txn.rollback();
				LOG.fine("Create community: duplicate username.");
				return Response.status(Status.CONFLICT).entity("Community already exists.").build();
			}
        } catch  ( Exception e ) {
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


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommunity(@HeaderParam("authToken") String jsonToken, String communityID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Get community: " + authToken.username + " attempted to get community with id " + communityID + ".");
        Entity user = serverConstants.getUser(authToken.username);
        Entity token = serverConstants.getToken(authToken.username, authToken.tokenID);
        Entity community = serverConstants.getCommunity(communityID);
        var validation = Validations.checkValidation(Validations.GET_COMMUNITY, user, token, authToken, community);
        if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
			serverConstants.removeToken(authToken.username, authToken.tokenID);
			return validation;
		} else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
			return validation;
		} else {
			LOG.fine("Get community: " + authToken.username + " received the community.");
            Community communityData = new Community(community.getString("name"), community.getString("description"), 
                                                    community.getLong("num_members"), community.getString("username"), 
                                                    community.getTimestamp("creationDate"));
			return Response.ok(g.toJson(communityData)).build();
		}
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/join")
    public Response joinCommunity(@HeaderParam("authToken") String jsonToken, String communityID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Join community: " + authToken.username + " attempted to join the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            Entity community = serverConstants.getCommunity(txn, communityID);
            var validation = Validations.checkValidation(Validations.JOIN_COMMUNITY, user, token, authToken, community);
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                // TODO: create relation between user and community
                community = Entity.newBuilder(community.getKey())
                    .set("id", community.getString("id"))
                    .set("name", community.getString("name"))
                    .set("description", community.getString("description"))
                    .set("num_members", community.getLong("num_members") + 1)
                    .set("username", community.getString("username"))
                    .set("creationDate", community.getTimestamp("creationDate"))
                    .build();
                txn.put(community);
                txn.commit();
                return Response.ok().entity("Community joined successfully.").build();
            }
        } catch (Exception e) {
			txn.rollback();
			LOG.severe("Login: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
			if ( txn.isActive() ) {
				txn.rollback();
                LOG.severe("Login: Internal server error.");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
        }
    }
}