package pt.unl.fct.di.apdc.projeto.resources;

import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.codec.digest.DigestUtils;

import pt.unl.fct.di.apdc.projeto.util.*;
import java.util.Map;
import java.util.HashMap;

import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Key;
import com.google.api.client.json.Json;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.Filter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

@Path("/communities")
@Produces(MediaType.APPLICATION_JSON)
public class GetCommunitiesResource {

    /**
     * Logger Object
     */
    private static final Logger LOG = Logger.getLogger(GetCommunitiesResource.class.getName());

    private final Gson g = new Gson();

    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    public GetCommunitiesResource() {}


    @GET
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getCommunities(
        @HeaderParam("username") String username,
        @HeaderParam("tokenID") String tokenID,
        @HeaderParam("expirationTime") long expirationTime
    ) {
        // user changing the state
        Key tokenKey = datastore.newKeyFactory().addAncestor(PathElement.of("User", username)).setKind("Token").newKey(tokenID);
        Entity token = datastore.get(tokenKey);
        if (token == null)
            return Response.status(Status.BAD_REQUEST).entity("User is not logged in.").build();
        else if (expirationTime < System.currentTimeMillis()) {
            datastore.delete(tokenKey);
            return Response.status(Status.BAD_REQUEST).entity("User is not logged in because the token expired.").build();
        }
        Key userKey = datastore.newKeyFactory().setKind("User").newKey(username);
        Entity user = datastore.get(userKey);
        if (user == null)
            return Response.status(Status.BAD_REQUEST).entity("User does not exist.").build();
        // Get all users through Query<Entity>

        Query<Entity> query = Query.newEntityQueryBuilder().setKind("Community").build();
		QueryResults<Entity> tokens = datastore.run(query);
        List<String> list = new LinkedList<>();
        tokens.forEachRemaining(communityLog -> {
            Map<String, Object> m = new HashMap<>();
            String name = communityLog.getString("name");
            String description = communityLog.getString("description");
            Long members = communityLog.getLong("num_members");
            m.put("name", name); m.put("description", description); m.put("num_members", members);
            list.add(g.toJson(m));
		});
        return Response.ok(g.toJson(list)).status(Status.ACCEPTED).build();
    } 

}
