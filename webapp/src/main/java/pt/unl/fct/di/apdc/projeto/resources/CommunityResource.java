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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.CommunityData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;

@Path("communities/community")
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
    public Response createCommunity(@HeaderParam("username") String username, @HeaderParam("tokenID") String tokenID, @HeaderParam("expirationTime") long expirationTime, CommunityData data) {
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
            String key = UUID.randomUUID().toString();
            Key communityKey = datastore.newKeyFactory().setKind("Community").newKey(key);
            if ( txn.get(communityKey) == null ) {
                Entity community = Entity.newBuilder(communityKey)
						.set("id", key)
                        .set("name", data.name)
                        .set("description", data.description)
                        .set("num_members", 1)
                        .set("username", username)
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

}
