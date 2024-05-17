package pt.unl.fct.di.apdc.projeto.resources;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/communities")
public class GetCommunitiesResource {

    /**
     * Logger Object
     */
    private static final Logger LOG = Logger.getLogger(GetCommunitiesResource.class.getName());

	/** Class that stores the server constants to use in operations */
	public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

    private final Gson g = new Gson();

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public GetCommunitiesResource() {}


    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommunities(@HeaderParam("authToken") String jsonToken) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Get Communities: attempt to get the communities by user " + authToken.username + ".");
        Entity user = serverConstants.getUser(authToken.username);
        Entity token = serverConstants.getToken(authToken.username, authToken.tokenID);
        var validation = Validations.checkCommunitiesValidations(Validations.GET_COMMUNITIES, user, token, authToken);
        if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
			serverConstants.removeToken(authToken.username, authToken.tokenID);
			return validation;
		} else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
			return validation;
		} else {
            //TODO: don't show locked communities.
            Query<Entity> query = Query.newEntityQueryBuilder().setKind("Community").build();
            QueryResults<Entity> tokens = datastore.run(query);
            List<String> list = new LinkedList<>();
            tokens.forEachRemaining(communityLog -> {
                Map<String, Object> m = new HashMap<>();
                String name = communityLog.getString("name");
                String communityID = communityLog.getString("communityID");
                String description = communityLog.getString("description");
                Long members = communityLog.getLong("num_members");
                m.put("name", name); m.put("communityID", communityID); m.put("description", description); m.put("num_members", members);
                list.add(g.toJson(m));
            });
            return Response.ok(g.toJson(list)).status(Status.ACCEPTED).build();
		}
    }
}