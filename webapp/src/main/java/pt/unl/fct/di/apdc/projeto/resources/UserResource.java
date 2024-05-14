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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.ChangeData;
import pt.unl.fct.di.apdc.projeto.util.PasswordData;
import pt.unl.fct.di.apdc.projeto.util.RoleData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.User;
import pt.unl.fct.di.apdc.projeto.util.UsernameData;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/user")
public class UserResource {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** Class that stores the server constants to use in operations */
	public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

	/** The data store to store users in */
	private static final Datastore datastore = serverConstants.getDatastore();
	
	/** The converter to JSON */
	private final Gson g = new Gson();

    public UserResource() {

    }

    @POST
    @Path("/change/data")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response alterData(ChangeData data) {
        AuthToken token = data.token;
        LOG.fine("Data change: " + token.username + " attempted to change user data.");
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = serverConstants.getUserKey(data.username);
            Key adminKey = serverConstants.getUserKey(token.username);
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            Entity authToken = serverConstants.getToken(txn, token);
            var validation = Validations.checkValidation(Validations.CHANGE_USER_DATA, admin, user, authToken, token, data);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                user = Entity.newBuilder(userKey)
                    .set("username", user.getString("username"))
                    .set("password", data.password == null || data.password.trim().isEmpty() ? user.getString("password") : DigestUtils.sha3_512Hex(data.password))
                    .set("email", data.email == null || data.email.trim().isEmpty() ? user.getString("email") : data.email)
                    .set("name", data.name == null || data.name.trim().isEmpty() ? user.getString("name") : data.name)
                    .set("phone", data.phone == null || data.phone.trim().isEmpty() ? user.getString("phone") : data.phone)
                    .set("profile", data.profile == null || data.profile.trim().isEmpty() ? user.getString("profile") : data.profile)
                    .set("work", data.work == null || data.work.trim().isEmpty() ? user.getString("work") : data.work)
                    .set("workplace", data.workplace == null || data.workplace.trim().isEmpty() ? user.getString("workplace") : data.workplace)
                    .set("address", data.address == null || data.address.trim().isEmpty() ? user.getString("address") : data.address)
                    .set("postalcode", data.postalcode == null || data.postalcode.trim().isEmpty() ? user.getString("postalcode") : data.postalcode)
                    .set("fiscal", data.fiscal == null || data.fiscal.trim().isEmpty() ? user.getString("fiscal") : data.fiscal)
                    .set("role", data.role == null || data.role.trim().isEmpty() ? user.getString("role") : data.role)
                    .set("state", data.state == null || data.state.trim().isEmpty() ? user.getString("state") : data.state)
                    .set("userCreationTime", user.getTimestamp("userCreationTime"))
                    .set("photo", StringValue.newBuilder(data.photo == null || data.photo.trim().isEmpty() ? user.getString("photo") : data.photo).setExcludeFromIndexes(true).build())
                    .build();
                txn.update(user);
                txn.commit();
                LOG.fine("Data change: " + data.username + "'s data was updated in the database.");
                return Response.ok().entity("User's data updated.").build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Data change: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Data change: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/change/password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response alterPassword(PasswordData data) {
        AuthToken token = data.token;
        LOG.fine("Password change: " + token.username + " attempted to change their password.");
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = serverConstants.getUserKey(token.username);
            Entity user = txn.get(userKey);
            Entity authToken = serverConstants.getToken(txn, token);
            var validation = Validations.checkValidation(Validations.CHANGE_PASSWORD, user, authToken, token, data);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                user = Entity.newBuilder(user)
                    .set("username", user.getString("username"))
                    .set("password", DigestUtils.sha3_512Hex(data.newPassword))
                    .set("email", user.getString("email"))
                    .set("name", user.getString("name"))
                    .set("phone", user.getString("phone"))
                    .set("profile", user.getString("profile"))
                    .set("work", user.getString("work"))
                    .set("workplace", user.getString("workplace"))
                    .set("address", user.getString("address"))
                    .set("postalcode", user.getString("postalcode"))
                    .set("fiscal", user.getString("fiscal"))
                    .set("role", user.getString("role"))
                    .set("state", user.getString("state"))
                    .set("userCreationTime", user.getTimestamp("userCreationTime"))
                    .set("photo", StringValue.newBuilder(user.getString("photo")).setExcludeFromIndexes(true).build())
                    .build();
                txn.update(user);
                txn.commit();
                LOG.fine("Password change: " + token.username + "'s' password was changed.");
                return Response.ok(g.toJson(token)).build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Password change: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Password change: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/change/role")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response changeRole(RoleData data) {
        AuthToken token = data.token;
        LOG.fine("Role change: attempt to change role of " + data.username + " by " + token.username + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = serverConstants.getUserKey(data.username);
            Key adminKey = serverConstants.getUserKey(token.username);
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            Entity authToken = serverConstants.getToken(txn, token);
            var validation = Validations.checkValidation(Validations.CHANGE_USER_ROLE, admin, user, authToken, token, data);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                user = Entity.newBuilder(user)
                    .set("username", user.getString("username"))
                    .set("password", user.getString("password"))
                    .set("email", user.getString("email"))
                    .set("name", user.getString("name"))
                    .set("phone", user.getString("phone"))
                    .set("profile", user.getString("profile"))
                    .set("work", user.getString("work"))
                    .set("workplace", user.getString("workplace"))
                    .set("address", user.getString("address"))
                    .set("postalcode", user.getString("postalcode"))
                    .set("fiscal", user.getString("fiscal"))
                    .set("role", data.role)
                    .set("state", user.getString("state"))
                    .set("userCreationTime", user.getTimestamp("userCreationTime"))
                    .set("photo", StringValue.newBuilder(user.getString("photo")).setExcludeFromIndexes(true).build())
                    .build();
                txn.update(user);
                txn.commit();
                LOG.fine("Role change: " + data.username + "'s' role was changed to " + data.role + ".");
                return Response.ok().entity("User's role changed.").build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Role change: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Role change: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }


    @POST
    @Path("/change/state")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response changeState(UsernameData data) {
        AuthToken token = data.token;
        LOG.fine("State changing attempt of: " + data.username + " by " + token.username);
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = serverConstants.getUserKey(data.username);
            Key adminKey = serverConstants.getUserKey(token.username);
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            Entity authToken = serverConstants.getToken(txn, token);
            var validation = Validations.checkValidation(Validations.CHANGE_USER_STATE, admin, user, authToken, token, data);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                user = Entity.newBuilder(user)
                    .set("username", user.getString("username"))
                    .set("password", user.getString("password"))
                    .set("email", user.getString("email"))
                    .set("name", user.getString("name"))
                    .set("phone", user.getString("phone"))
                    .set("profile", user.getString("profile"))
                    .set("work", user.getString("work"))
                    .set("workplace", user.getString("workplace"))
                    .set("address", user.getString("address"))
                    .set("postalcode", user.getString("postalcode"))
                    .set("fiscal", user.getString("fiscal"))
                    .set("role", user.getString("role"))
                    .set("state", user.getString("state").equals(ServerConstants.ACTIVE) ? ServerConstants.INACTIVE : ServerConstants.ACTIVE)
                    .set("userCreationTime", user.getTimestamp("userCreationTime"))
                    .set("photo", StringValue.newBuilder(user.getString("photo")).setExcludeFromIndexes(true).build())
                    .build();
                txn.update(user);
                txn.commit();
                LOG.fine("State change: " + data.username + "'s role changed by " + token.username + ".");
                return Response.ok().entity("User state changed.").build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("State change: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("State change: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/remove")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response removeUser(UsernameData data) {
        AuthToken token = data.token;
        LOG.fine("Remove User: removal attempt of " + data.username + " by " + token.username + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Key userKey = serverConstants.getUserKey(data.username);
            Key adminKey = serverConstants.getUserKey(token.username);
            Entity user = txn.get(userKey);
            Entity admin = txn.get(adminKey);
            Entity authToken = serverConstants.getToken(txn, token);
            var validation = Validations.checkValidation(Validations.REMOVE_USER, admin, user, authToken, token, data);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Message")
                    .setFilter(PropertyFilter.hasAncestor(userKey))
                    .build();
                QueryResults<Entity> results = txn.run(query);
                while ( results.hasNext() ) {
                    Entity next = results.next();
                    txn.delete(next.getKey());
                }
                query = Query.newEntityQueryBuilder()
                    .setKind("Message")
                    .setFilter(PropertyFilter.eq("sender", data.username))
                    .build();
                results = txn.run(query);
                while ( results.hasNext() ) {
                    Entity next = results.next();
                    txn.delete(next.getKey());
                }
                Query<Key> tokensQuery = Query.newKeyQueryBuilder()
                    .setKind("Token")
                    .setFilter(PropertyFilter.hasAncestor(userKey))
                    .build();
                QueryResults<Key> tokenKeys = txn.run(tokensQuery);
                while ( tokenKeys.hasNext() ) {
                    txn.delete(tokenKeys.next());
                }
                txn.delete(userKey);
                txn.commit();
                LOG.fine("Remove User: " + data.username + " removed from the database.");
                return Response.ok().entity("User removed from database.").build();
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Remove User: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Remove User: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response searchUser(UsernameData data) {
        AuthToken token = data.token;
        LOG.fine("Search User: search attempt of " + data.username + " by " + token.username + ".");
        try {
            Key userKey = serverConstants.getUserKey(data.username);
            Key adminKey = serverConstants.getUserKey(token.username);
            Entity user = datastore.get(userKey);
            Entity admin = datastore.get(adminKey);
            Entity authToken = serverConstants.getToken(token);
            var validation = Validations.checkValidation(Validations.SEARCH_USER, admin, user, authToken, token, data);
            if ( validation.getStatus() != Status.OK.getStatusCode() )
				return validation;
			else {
                User searchUser = new User(user.getString("username"), user.getString("password"), user.getString("email"), 
                                            user.getString("name"), user.getString("phone"), user.getString("profile"), 
                                            user.getString("work"), user.getString("workplace"), user.getString("address"), 
                                            user.getString("postalcode"), user.getString("fiscal"), user.getString("role"), 
                                            user.getString("state"), user.getTimestamp("userCreationTime").toDate(), user.getString("photo"));
                LOG.fine("Search User: " + data.username + "'s information' sent to user.");
                return Response.ok(g.toJson(searchUser)).build();
            }
        } catch ( Exception e ) {
			LOG.severe("Search User: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }

    @POST
    @Path("/profile")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getUser(AuthToken token) {
        LOG.fine("Get User: get user attempt by " + token.username + ".");
        try {
            Key userKey = serverConstants.getUserKey(token.username);
            Entity user = datastore.get(userKey);
            Entity authToken = serverConstants.getToken(token);
            var validation = Validations.checkValidation(Validations.USER_PROFILE, user, authToken, token);
            if ( validation.getStatus() != Status.OK.getStatusCode() )
				return validation;
			else {
                User searchUser = new User(user.getString("username"), user.getString("email"), user.getString("name"), 
                                            user.getString("phone"), user.getString("profile"), user.getString("work"), 
                                            user.getString("workplace"), user.getString("address"), user.getString("postalcode"), 
                                            user.getString("fiscal"), user.getTimestamp("userCreationTime").toDate(), user.getString("photo"));
                LOG.fine("Get User: " + token.username + "'s information' sent to user.");
                return Response.ok(g.toJson(searchUser)).build();
            }
        } catch ( Exception e ) {
			LOG.severe("Get User: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
    }
}