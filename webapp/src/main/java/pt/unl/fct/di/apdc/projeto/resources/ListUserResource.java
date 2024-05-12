package pt.unl.fct.di.apdc.projeto.resources;

import java.util.LinkedList;
import java.util.List;
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
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.ProjectionEntity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.User;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.UserQuery;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/list")
public class ListUserResource {
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** Class that stores the server constants to use in operations */
	public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

	/** The data store to store users in */
	private static final Datastore datastore = serverConstants.getDatastore();
	
	/** The converter to JSON */
	private final Gson g = new Gson();

    public ListUserResource() {

    }

    @POST
    @Path("/users")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response listUsers(AuthToken token) {
        LOG.fine("List users: " + token.username + " attempted to list users.");
        try {
            Key userKey = serverConstants.getUserKey(token.username);
            Key tokenKey = serverConstants.getTokenKey(token.username);
            Entity user = datastore.get(userKey);
            Entity authToken = datastore.get(tokenKey);
            var validation = Validations.checkValidation(Validations.LIST_USERS, user, authToken, token);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
				return validation;
			} else {
                String userRole = user.getString("role");
				Query<Entity> query;
                if ( userRole.equals(ServerConstants.USER) ) {
                    Query<ProjectionEntity> projectionQuery = Query.newProjectionEntityQueryBuilder()
				            .setKind("User")
				            .setFilter(CompositeFilter.and(
                                        PropertyFilter.eq("state", ServerConstants.ACTIVE),
                                        PropertyFilter.eq("profile", ServerConstants.PUBLIC),
                                        PropertyFilter.eq("role", ServerConstants.USER)))
				            .setProjection("username", "email", "name")
				            .build();
                    List<UserQuery> projection = new LinkedList<>();
                    QueryResults<ProjectionEntity> results = datastore.run(projectionQuery);
                    while ( results.hasNext() ) {
                        ProjectionEntity result = results.next();
                        projection.add(new UserQuery(result.getString("username"), result.getString("email"), result.getString("name")));
                    }
				    LOG.info("List users: " + token.username + " received list of active and public USER users.");
				    return Response.ok(g.toJson(projection)).build();
                } else if ( userRole.equals(ServerConstants.GBO) ) {
                    query = Query.newEntityQueryBuilder()
				            .setKind("User")
				            .setFilter(PropertyFilter.eq("role", ServerConstants.USER))
				            .build();
                } else if ( userRole.equals(ServerConstants.GA) ) {
                    query = Query.newEntityQueryBuilder()
				            .setKind("User")
				            .setFilter(PropertyFilter.in("role", ListValue.of(ServerConstants.USER, ServerConstants.GBO, ServerConstants.GA)))
                            .setOrderBy(OrderBy.desc("role"))
				            .build();
                } else if ( userRole.equals(ServerConstants.SU) ) {
                    query = Query.newEntityQueryBuilder()
				            .setKind("User")
                            .setOrderBy(OrderBy.desc("role"))
				            .build();
                } else {
                    LOG.severe("List users: Unrecognized role.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
                List<User> userList = new LinkedList<>();
                QueryResults<Entity> results = datastore.run(query);
                while ( results.hasNext() ) {
                    Entity next = results.next();
                    userList.add(new User(next.getString("username"), next.getString("password"), next.getString("email"), 
                                    next.getString("name"), next.getString("phone"), next.getString("profile"), 
                                    next.getString("work"), next.getString("workplace"), next.getString("address"), 
                                    next.getString("postalcode"), next.getString("fiscal"), next.getString("role"), 
                                    next.getString("state"), next.getTimestamp("userCreationTime").toDate(), next.getString("photo")));
                }
                LOG.info("List users: " + token.username + " received list of all users.");
                return Response.ok(g.toJson(userList)).build();
			}
        } catch ( Exception e ) {
			LOG.severe("List users: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
    }
}