package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.*;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;

@Path("/logout")
public class LogoutResource {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** Class that stores the server constants to use in operations */
	public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

	/** The data store to store users in */
	private static final Datastore datastore = serverConstants.getDatastore();

    public LogoutResource() {

    }

    @POST
    @Path("/user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response logout(AuthToken token) {
        LOG.fine("Logout: " + token.username + " attempt to logout.");
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = serverConstants.getUserKey(token.username);
            Key tokenKey = serverConstants.getTokenKey(token.username);
            Entity user = txn.get(userKey);
            if ( user == null ) {
                txn.rollback();
				LOG.warning("Logout: " + token.username + " not registered as user.");
                return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            }
            Entity authToken = txn.get(tokenKey);
            int validation = token.isStillValid(authToken, user.getString("role"));
            if ( validation == 1 ) {
                txn.delete(tokenKey);
                txn.commit();
                LOG.fine("Logout: " + token.username + " logged out.");
                return Response.ok().entity("User logged out.").build();
            } else if ( validation == 0 ) { // Token time has run out
                txn.rollback();
                LOG.fine("Logout: " + token.username + "'s' authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                LOG.warning("Logout: " + token.username + "'s' authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // token is false
                txn.rollback();
                LOG.severe("Logout: " + token.username + "'s' authentication token is different, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("Logout: " + token.username + "'s' authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Logout: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Logout: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
}
