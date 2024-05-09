package pt.unl.fct.di.apdc.projeto.resources;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.ConversationClass;
import pt.unl.fct.di.apdc.projeto.util.MessageClass;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

@Path("/message")
public class MessageResource {

	/**
	 * Logger Object
	 */
	private static Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** Class that stores the server constants to use in operations */
	public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

	/** The data store to store users in */
	private static final Datastore datastore = serverConstants.getDatastore();
	
	/** The converter to JSON */
	private final Gson g = new Gson();
    
    public MessageResource() {

    }


    @POST
	@Path("/user")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response sendMessage(MessageClass message) {
        LOG.fine("Send Message: send message attempt by: " + message.sender + " to " + message.receiver + ".");
        AuthToken token = message.token;
		Key senderKey = serverConstants.getUserKey(message.sender);
		Key senderTokenKey = serverConstants.getTokenKey(message.sender);
        Key receiverKey = serverConstants.getUserKey(message.receiver);
        Key messageKey = datastore.allocateId(
            datastore.newKeyFactory()
            .addAncestor(PathElement.of("User", message.receiver))
            .setKind("Message")
            .newKey());
		Transaction txn = datastore.newTransaction();
		try {
			Entity sender = txn.get(senderKey);
            Entity receiver = txn.get(receiverKey);
            Entity authToken = datastore.get(senderTokenKey);
            var validation = Validations.checkValidation(Validations.SEND_MESSAGE, sender, receiver, authToken, token, message);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
                txn.rollback();
				return validation;
			} else {
                Entity messageEntity = Entity.newBuilder(messageKey)
                    .set("receiver", message.receiver)
                    .set("sender", message.sender)
                    .set("message", message.message)
                    .set("timestamp", Timestamp.now())
                    .build();
                txn.add(messageEntity);
                txn.commit();
                LOG.fine("Send Message: " + message.sender + " send message to message.receiver.");
                return Response.ok().entity("Message sent.").build();
            }
		} catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Send Message: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		} finally {
			if ( txn.isActive() ) {
				txn.rollback();
                LOG.severe("Send Message: Internal server error.");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		}
    }


    @POST
	@Path("/receive")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response receiveMessages(AuthToken token) {
        LOG.fine("Receive Messages: receive messages attempt by: " + token.username + ".");
		Key userKey = serverConstants.getUserKey(token.username);
        Key tokenKey = serverConstants.getTokenKey(token.username);
		try {
			Entity user = datastore.get(userKey);
            Entity authToken = datastore.get(tokenKey);
            var validation = Validations.checkValidation(Validations.RECEIVE_MESSAGES, user, authToken, token);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
				return validation;
			} else {
                Query<Entity> query = Query.newEntityQueryBuilder()
                    .setKind("Message")
                    .setFilter(PropertyFilter.hasAncestor(userKey))
                    .build();
                List<MessageClass> messages = new LinkedList<>();
                QueryResults<Entity> results = datastore.run(query);
                while ( results.hasNext() ) {
                    Entity next = results.next();
                    messages.add(new MessageClass(next.getString("sender"), next.getString("receiver"), 
                    next.getString("message"), next.getTimestamp("timestamp").toDate()));
                }
                messages.sort(Comparator.comparing(MessageClass::getTimeStamp).reversed());
                LOG.fine("Receive Messages: " + token.username + " received messages.");
                return Response.ok(g.toJson(messages)).build();
            }
		} catch ( Exception e ) {
			LOG.severe("Receive Messages: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
    }


    @POST
	@Path("/conversation")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getMessages(ConversationClass conversation) {
        AuthToken  token = conversation.token;
        LOG.fine("Receive Message: receive messages attempt by: " + token.username + ".");
		Key senderKey = serverConstants.getUserKey(conversation.sender);
        Key receiverKey = serverConstants.getUserKey(conversation.receiver);
        Key tokenKey = serverConstants.getTokenKey(token.username);
		try {
			Entity sender = datastore.get(senderKey);
            Entity receiver = datastore.get(receiverKey);
            Entity authToken = datastore.get(tokenKey);
            var validation = Validations.checkValidation(Validations.LOAD_CONVERSATION, sender, receiver, authToken, token, conversation);
            if ( validation.getStatus() != Status.OK.getStatusCode() ) {
				return validation;
			} else {
                Query<Entity> senderQuery = Query.newEntityQueryBuilder()
                    .setKind("Message")
                    .setFilter(CompositeFilter.and(PropertyFilter.hasAncestor(senderKey), PropertyFilter.eq("sender", conversation.receiver)))
                    .build();
                Query<Entity> receiverQuery = Query.newEntityQueryBuilder()
                    .setKind("Message")
                    .setFilter(CompositeFilter.and(PropertyFilter.hasAncestor(receiverKey), PropertyFilter.eq("sender", conversation.sender)))
                    .build();
                List<MessageClass> messages = new LinkedList<>();
                QueryResults<Entity> results = datastore.run(senderQuery);
                while ( results.hasNext() ) {
                    Entity next = results.next();
                    messages.add(new MessageClass(next.getString("sender"), next.getString("receiver"), 
                                    next.getString("message"), next.getTimestamp("timestamp").toDate()));
                }
                results = datastore.run(receiverQuery);
                while ( results.hasNext() ) {
                    Entity next = results.next();
                    messages.add(new MessageClass(next.getString("sender"), next.getString("receiver"), 
                                    next.getString("message"), next.getTimestamp("timestamp").toDate()));
                }
                messages.sort(Comparator.comparing(MessageClass::getTimeStamp).reversed());
                LOG.fine("Receive Message: " + token.username + " received messages.");
                return Response.ok(g.toJson(messages)).build();
            }
		} catch ( Exception e ) {
			LOG.severe("Receive Message: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
    }
}
