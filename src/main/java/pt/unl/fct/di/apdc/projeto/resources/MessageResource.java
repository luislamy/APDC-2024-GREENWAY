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
			if ( sender == null ) {
				LOG.warning("Send Message: " + message.sender + " not registered as user.");
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
			} else if ( receiver == null ) {
				LOG.warning("Send Message: " + message.receiver + " not registered as user.");
				txn.rollback();
				return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            }
			if ( receiver.getString("state").equals(ServerConstants.INACTIVE) ) {
				LOG.warning("Send Message: " + message.receiver + " is not an active user.");
				txn.rollback();
				return Response.status(Status.UNAUTHORIZED).entity("Receiver's account is inactive.").build();
			}
            Entity authToken = datastore.get(senderTokenKey);
            String senderRole = sender.getString("role");
            int validation = token.isStillValid(authToken, senderRole);
			if ( validation == 1 ) {
                String receiverRole = receiver.getString("role");
                if ( senderRole.equals(ServerConstants.USER) ) {
                    if (! receiverRole.equals(ServerConstants.USER) || !receiver.getString("profile").equals(ServerConstants.PUBLIC) ) {
                        txn.rollback();
                        LOG.fine("Send Message: USER roles cannot send messages to non USER roles.");
                        return Response.status(Status.UNAUTHORIZED).entity("USER roles cannot send messages to non USER roles or users with private profiles.").build();
                    }
                } else if ( senderRole.equals(ServerConstants.GBO) ) {
                    if ( !receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.GBO) ) {
                        txn.rollback();
                        LOG.fine("Send Message: GBO roles cannot send messages to higher roles.");
                        return Response.status(Status.UNAUTHORIZED).entity("GBO roles cannot send messages to higher roles.").build();
                    }
                } else if ( senderRole.equals(ServerConstants.GA) ) {
                    if ( !receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.GBO) && !receiverRole.equals(ServerConstants.GA) ) {
                        txn.rollback();
                        LOG.fine("Send Message: GA roles cannot send messages to higher roles.");
                        return Response.status(Status.UNAUTHORIZED).entity("GA roles cannot send messages to higher roles.").build();
                    }
                } else if ( senderRole.equals(ServerConstants.SU) ) {
                } else {
                    txn.rollback();
                    LOG.severe("Send Message User: Unrecognized role.");
                    return Response.status(Status.INTERNAL_SERVER_ERROR).build();
                }
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
            } else if (validation == 0 ) { // Token time has run out
                txn.rollback();                
                LOG.fine("Send Message: " + token.username + "'s authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                txn.rollback();
                LOG.warning("Send Message: " + token.username + "'s authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // token is false
                txn.rollback();
                LOG.severe("Send Message: " + token.username + "'s authentication token is different, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
            } else {
                txn.rollback();
                LOG.severe("Send Message: authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
        LOG.fine("Receive Message: receive messages attempt by: " + token.username + ".");
		Key userKey = serverConstants.getUserKey(token.username);
        Key tokenKey = serverConstants.getTokenKey(token.username);
		try {
			Entity user = datastore.get(userKey);
			if ( user == null ) {
				LOG.warning("Receive Message: " + token.username + " not registered as user.");
				return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
			}
            Entity authToken = datastore.get(tokenKey);
            String role = user.getString("role");
            int validation = token.isStillValid(authToken, role);
			if ( validation == 1 ) {
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
                LOG.fine("Receive Message: " + token.username + " received messages.");
                return Response.ok(g.toJson(messages)).build();
            } else if (validation == 0 ) { // Token time has run out
                LOG.fine("Receive Message: " + token.username + "'s authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                LOG.warning("Receive Message: " + token.username + "'s authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // token is false
                LOG.severe("Receive Message: " + token.username + "'s authentication token is different, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
            } else {
                LOG.severe("Receive Message: authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
		} catch ( Exception e ) {
			LOG.severe("Receive Message: " + e.getMessage());
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
			if ( sender == null ) {
				LOG.warning("Receive Message: " + conversation.sender + " not registered as user.");
				return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
			} else if ( receiver == null ) {
				LOG.warning("Receive Message: " + conversation.receiver + " not registered as user.");
				return Response.status(Status.NOT_FOUND).entity("No such user exists.").build();
            }
            Entity authToken = datastore.get(tokenKey);
            String role = sender.getString("role");
            int validation = token.isStillValid(authToken, role);
			if ( validation == 1 ) {
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
            } else if (validation == 0 ) { // Token time has run out
                LOG.fine("Receive Message: " + token.username + "'s authentication token expired.");
                return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
            } else if ( validation == -1 ) { // Role is different
                LOG.warning("Receive Message: " + token.username + "'s authentication token has different role.");
                return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
            } else if ( validation == -2 ) { // token is false
                LOG.severe("Receive Message: " + token.username + "'s authentication token is different, possible attempted breach.");
                return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
            } else {
                LOG.severe("Receive Message: authentication token validity error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
		} catch ( Exception e ) {
			LOG.severe("Receive Message: " + e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
    }
}
