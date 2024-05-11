package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.LoginData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/login")
public class LoginResource {

	/**
	 * Logger Object
	 */
	private static Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** 24 hours in milliseconds */
	public static final long HOURS24 = 1000*60*60*24;

	/** Class that stores the server constants to use in operations */
	public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

	/** The data store to store users in */
	private static final Datastore datastore = serverConstants.getDatastore();
	
	/** The converter to JSON */
	private final Gson g = new Gson();

	public LoginResource() {
	}

	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response login(LoginData data) {
		LOG.fine("Login: login attempt by: " + data.username + ".");
		Key userKey = serverConstants.getUserKey(data.username);
		Key tokenKey = serverConstants.getTokenKey(data.username);
		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);
			var validation = Validations.checkValidation(Validations.LOGIN, user, data);
			if ( validation.getStatus() != Status.OK.getStatusCode() ) {
				txn.rollback();
				return validation;
			} else {
				AuthToken authToken = new AuthToken(data.username, user.getString("role"));
				Entity token = Entity.newBuilder(tokenKey)
						.set("username", authToken.username)
						.set("role", authToken.role)
						.set("tokenID", authToken.tokenID)
						.set("creationDate", authToken.creationDate)
						.set("expirationDate", authToken.expirationDate)
						.build();
				txn.put(token);
				txn.commit();
				LOG.info("Login: " + data.username + " logged in successfully.");
				return Response.ok(g.toJson(authToken)).build();
			}
		} catch ( Exception e ) {
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

	@POST
	@Path("/check")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getToken(AuthToken token) {
		LOG.fine("Token: token check attempt by " + token.username + ".");
		Key userKey = serverConstants.getUserKey(token.username);
		Entity user = datastore.get(userKey);
		Entity authToken = serverConstants.getToken(token);
		var validation = Validations.checkValidation(Validations.GET_TOKEN, user, authToken, token);
		if ( validation.getStatus() != Status.OK.getStatusCode() ) {
			return validation;
		} else {
			LOG.fine("Token: " + token.username + " is still logged in.");
			return Response.ok(g.toJson(token)).build();
		}
	}
}