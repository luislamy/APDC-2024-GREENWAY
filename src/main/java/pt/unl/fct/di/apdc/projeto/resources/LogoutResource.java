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
import pt.unl.fct.di.apdc.projeto.util.Validations;

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
            Entity authToken = txn.get(tokenKey);
            var validation = Validations.checkValidation(Validations.LOGOUT, user, authToken, token);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
				txn.rollback();
				return validation;
			} else {
				txn.delete(tokenKey);
                txn.commit();
                LOG.fine("Logout: " + token.username + " logged out.");
                return Response.ok().entity("User logged out.").build();
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
