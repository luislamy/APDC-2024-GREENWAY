package pt.unl.fct.di.apdc.projeto.resources;

import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.*;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.RegisterData;

@Path("/register")
public class RegisterResource {

	/** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** Class that stores the server constants to use in operations */
	public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

	/** The data store to store users in */
	private static final Datastore datastore = serverConstants.getDatastore();

	public RegisterResource() {
	}

	@POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response register(RegisterData data) {
		LOG.fine("Resgister: attempt to register " + data.username + ".");
		int validRegister = data.validRegistration();
		if ( validRegister != 0 ) {
			LOG.warning("Register: Register attempt using invalid " + data.getInvalidReason(validRegister) + ".");
			return Response.status(Status.BAD_REQUEST).entity("Invalid " + data.getInvalidReason(validRegister) + ".").build();
		}
		Transaction txn = datastore.newTransaction();
		try {
			Key userKey = serverConstants.getUserKey(data.username);
			if ( txn.get(userKey) == null ) {
				Entity user = Entity.newBuilder(userKey)
						.set("username", data.username)
						.set("password", DigestUtils.sha3_512Hex(data.password))
						.set("email", data.email)
						.set("name", data.name)
						.set("phone", data.phone)
						.set("profile", data.profile == null || data.profile.trim().isEmpty() ? ServerConstants.PRIVATE : data.profile)
						.set("work", data.work == null || data.work.trim().isEmpty() ? "" : data.work)
						.set("workplace", data.workplace == null || data.workplace.trim().isEmpty() ? "" : data.workplace)
						.set("address", data.address == null || data.address.trim().isEmpty() ? "" : data.address)
						.set("postalcode", data.postalcode == null || data.postalcode.trim().isEmpty() ? "" : data.postalcode)
						.set("fiscal", data.fiscal == null || data.fiscal.trim().isEmpty() ? "" : data.fiscal)
						.set("role", ServerConstants.USER)
						.set("state", ServerConstants.INACTIVE)
						.set("userCreationTime", Timestamp.now())
						.set("photo", StringValue.newBuilder(data.photo == null || data.photo.trim().isEmpty() ? "" : data.photo).setExcludeFromIndexes(true).build())
						.build();
				txn.add(user);
				txn.commit();
				LOG.fine("Register: " + data.username + "'s was registered in the database.");
				return Response.ok().build();
			} else {
				txn.rollback();
				LOG.fine("Register: duplicate username.");
				return Response.status(Status.CONFLICT).entity("User already exists. Pick a different username.").build();
			}
		} catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Register: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Register: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
	}
}
