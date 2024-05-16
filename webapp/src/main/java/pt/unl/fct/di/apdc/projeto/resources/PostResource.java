package pt.unl.fct.di.apdc.projeto.resources;

import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.PostData;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/communities")
public class PostResource {
    
    /**
     * Logger Object
     */
    private static Logger LOG = Logger.getLogger(PostResource.class.getName());

    /** Class that stores the server constants to use in operations */
    public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

    /** The data store to store users in */
    private static final Datastore datastore = serverConstants.getDatastore();

    /** The converter to JSON */
    private final Gson g = new Gson();

    public PostResource() {}

    @POST
    @Path("/{communityID}/post")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addPost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Add post: " + authToken.username + " attempted to post to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            Entity community = serverConstants.getCommunity(txn, communityID);
            var validation = Validations.checkValidation(Validations.ADD_POST, user, community, token, authToken, data);
            validation = Response.ok().build();
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                String postID = UUID.randomUUID().toString();
                Entity post = Entity.newBuilder(serverConstants.getPostKey(communityID, postID))
                    .set("postID", postID)
                    .set("title", data.title)
                    .set("postBody", data.postBody)
                    .set("username", authToken.username)
                    .set("postDate", Timestamp.now())
                    .set("lastEdit", Timestamp.now())
                    .set("likes", 0L)
                    .set("dislikes", 0L)
                    .set("comments", 0L)
                    .build();
                txn.put(post);
                txn.commit();
                LOG.fine("Add post: " + authToken.username + " posted to the community with id " + communityID + ".");
                return Response.ok().build();
            }
        } catch  ( Exception e ) {
			txn.rollback();
			LOG.severe("Add post: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Add post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }

    @POST
    @Path("/{communityID}/post/edit")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response editPost(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID, PostData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Edit post: " + authToken.username + " attempted to post to the community with id " + communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            Entity community = serverConstants.getCommunity(txn, communityID);
            var validation = Validations.checkValidation(Validations.ADD_POST, user, community, token, authToken, data);
            validation = Response.ok().build();
            if ( validation.getStatus() == Status.UNAUTHORIZED.getStatusCode() ) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                Entity post = serverConstants.getPost(communityID, data.postID);
                post = Entity.newBuilder(serverConstants.getPostKey(communityID, data.postID))
                    .set("title", data.title)
                    .set("postBody", data.postBody)
                    .set("username", authToken.username)
                    .set("postDate", post.getTimestamp("postDate"))
                    .set("lastEdit", Timestamp.now())
                    .set("likes", 0L)
                    .set("dislikes", 0L)
                    .set("comments", 0L)
                    .build();
                txn.put(post);
                txn.commit();
                LOG.fine("Add post: " + authToken.username + " posted to the community with id " + communityID + ".");
                return Response.ok().build();
            }
        } catch  ( Exception e ) {
			txn.rollback();
			LOG.severe("Edit post: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Edit post: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }
}
