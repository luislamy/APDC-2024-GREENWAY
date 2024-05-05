package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.LoginData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;

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
			if ( user == null ) {
				LOG.warning("Login: " + data.username + " not registered as user.");
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
			}
			if ( user.getString("state").equals(ServerConstants.INACTIVE) ) {
				LOG.warning("Login: " + data.username + " not an active user.");
				txn.rollback();
				return Response.status(Status.UNAUTHORIZED).entity("User's account is inactive.").build();
			}
			String hashedPassword = (String) user.getString("password");
			if ( hashedPassword.equals(DigestUtils.sha3_512Hex(data.password)) ) {
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
			} else {
				txn.rollback();
				LOG.warning("Login: " + data.username + " provided wrong password.");
				return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
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
		Key tokenKey = serverConstants.getTokenKey(token.username);
		Entity user = datastore.get(userKey);
		if ( user == null ) {
			LOG.warning("Token: " + token.username + " is not a registered user.");
			return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
		}
		Entity authToken = datastore.get(tokenKey);
		String role = user.getString("role");
		int validation = token.isStillValid(authToken, role);
		if ( validation == 1 ) {
			LOG.fine("Token: " + token.username + " is still logged in.");
			return Response.ok(g.toJson(token)).build();
		} else if ( validation == 0 ) { // Token time has run out
			LOG.fine("Token: " + token.username + "'s authentication token expired.");
			return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
		} else if ( validation == -1 ) { // Role is different
			LOG.warning("Token: " + token.username + "'s authentication token has different role.");
			return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
		} else if ( validation == -2 ) { // token is false
			LOG.severe("Token: " + token.username + "'s authentication token is different, possible attempted breach.");
			return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
		} else {
			LOG.fine("Token: authentication token validity error.");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}


	@POST
	@Path("/check")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkToken(AuthToken token) {
		LOG.fine("Check: token check attempt by " + token.username + ".");
		Key userKey = serverConstants.getUserKey(token.username);
		Key tokenKey = serverConstants.getTokenKey(token.username);
		Entity user = datastore.get(userKey);
		if ( user == null ) {
			LOG.warning("Check: " + token.username + " is not a registered user.");
			return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
		}
		Entity authToken = datastore.get(tokenKey);
		String role = user.getString("role");
		int validation = token.isStillValid(authToken, role);
		if ( validation == 1 ) {
			LOG.fine("Check: " + token.username + " is still logged in.");
			return Response.ok().build();
		} else if ( validation == 0 ) { // Token time has run out
			LOG.fine("Check: " + token.username + "'s authentication token expired.");
			return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
		} else if ( validation == -1 ) { // Role is different
			LOG.warning("Check: " + token.username + "'s authentication token has different role.");
			return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
		} else if ( validation == -2 ) { // token is false
			LOG.severe("Check: " + token.username + "'s authentication token is different, possible attempted breach.");
			return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
		} else {
			LOG.fine("Check: authentication token validity error.");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}