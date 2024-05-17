package pt.unl.fct.di.apdc.projeto.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.gson.Gson;

import pt.unl.fct.di.apdc.projeto.util.*;

@Path("/communities")
public class CommunityResource {

    /**
     * Logger Object
     */
    private static Logger LOG = Logger.getLogger(CommunityResource.class.getName());

    /** Class that stores the server constants to use in operations */
    public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

    /** The data store to store users in */
    private static final Datastore datastore = serverConstants.getDatastore();

    /** The converter to JSON */
    private final Gson g = new Gson();

    public CommunityResource() {
    }

    @POST
    @Path("/create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response createCommunity(@HeaderParam("authToken") String jsonToken, CommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Attempt to create community by: " + authToken.username + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkCreateCommunityValidation(user, token, authToken);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(authToken.username, authToken.tokenID);
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                return validation;
            } else {
                String communityID = data.communityID;
                if (serverConstants.getCommunity(txn, communityID) == null) {
                    Entity community = Entity.newBuilder(serverConstants.getCommunityKey(communityID))
                            .set("communityID", data.communityID)
                            .set("name", data.name)
                            .set("description", data.description)
                            .set("num_members", 1)
                            .set("username", authToken.username)
                            .set("isLocked", false)
                            .set("creationDate", Timestamp.now())
                            .build();
                    Entity member = Entity
                            .newBuilder(serverConstants.getCommunityMemberKey(communityID, authToken.username))
                            .set("communityID", data.communityID)
                            .set("username", authToken.username)
                            .set("joinDate", Timestamp.now())
                            .set("isManager", true)
                            .build();
                    txn.add(community, member);
                    txn.commit();
                    LOG.fine("Create community: " + data.name + " was registered in the database.");
                    return Response.ok().build();
                } else {
                    txn.rollback();
                    LOG.fine("Create community: duplicate username.");
                    return Response.status(Status.CONFLICT).entity("Community already exists.").build();
                }
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Create community: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Create community: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }

    }

    @GET
    @Path("/{communityID}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCommunity(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Get community: " + authToken.username + " attempted to get community with id " + communityID + ".");
        Entity user = serverConstants.getUser(authToken.username);
        Entity community = serverConstants.getCommunity(communityID);
        Entity token = serverConstants.getToken(authToken.username, authToken.tokenID);
        var validation = Validations.checkGetCommunityValidation(user, community, token, authToken, communityID);
        if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
            serverConstants.removeToken(authToken.username, authToken.tokenID);
            return validation;
        } else if (validation.getStatus() != Status.OK.getStatusCode()) {
            return validation;
        } else {
            // TODO: send boolean true if member of community, false otherwise
            LOG.fine("Get community: " + authToken.username + " received the community.");
            Community communityData = new Community(community.getString("communityID"), community.getString("name"),
                    community.getString("description"),
                    community.getLong("num_members"), community.getString("username"),
                    community.getTimestamp("creationDate"));
            return Response.ok(g.toJson(communityData)).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/join")
    public Response joinCommunity(@HeaderParam("authToken") String jsonToken, JoinCommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Join community: " + authToken.username + " attempted to join the community with id "
                + data.communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, data.communityID);
            Entity member = serverConstants.getCommunityMember(txn, data.communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkJoinCommunityValidation(user, community, member, token, authToken, data.communityID);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                Key memberKey = serverConstants.getCommunityMemberKey(data.communityID, authToken.username);
                member = Entity.newBuilder(memberKey)
                        .set("communityID", data.communityID)
                        .set("username", authToken.username)
                        .set("joinDate", Timestamp.now())
                        .set("isManager", false)
                        .build();
                community = Entity.newBuilder(community.getKey())
                        .set("communityID", community.getString("communityID"))
                        .set("name", community.getString("name"))
                        .set("description", community.getString("description"))
                        .set("num_members", community.getLong("num_members") + 1L)
                        .set("username", community.getString("username"))
                        .set("creationDate", community.getTimestamp("creationDate"))
                        .build();
                txn.put(community, member);
                txn.commit();
                return Response.ok().entity("Community joined successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Login: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Login: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{communityID}/edit")
    public Response editCommunity(@HeaderParam("authToken") String jsonToken,
            @PathParam("communityID") String communityID, CommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Edit community: " + authToken.username + " attempted to edit the community with id " + communityID
                + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity member = serverConstants.getCommunityMember(txn, data.communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkEditCommunityValidation(user, community, member, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                community = Entity.newBuilder(community.getKey())
                        .set("communityID", community.getString("communityID"))
                        .set("name",
                                data.name == null || data.name.trim().isEmpty() ? community.getString("name")
                                        : data.name)
                        .set("description",
                                data.description == null || data.description.trim().isEmpty()
                                        ? community.getString("description")
                                        : data.description)
                        .set("num_members", community.getLong("num_members"))
                        .set("username", community.getString("username"))
                        .set("creationDate", community.getTimestamp("creationDate"))
                        .build();
                txn.update(community);
                txn.commit();
                return Response.ok().entity("Community edited successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Edit community: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Edit community: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/remove/request")
    public Response requestRemoveCommunity(@HeaderParam("authToken") String jsonToken, RemoveCommunityRequest data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Remove community request: " + authToken.username + " attempted to edit the community with id " + data.communityID + ".");
        Transaction txn = datastore.newTransaction();
        try {
            String key = data.communityID;
            Key removeCommunityRequestKey = datastore.newKeyFactory().setKind("RemoveCommunityRequest").newKey(key);
            if (serverConstants.getCommunity(txn, key) == null) {
                Entity removeCommunityRequest = Entity.newBuilder(removeCommunityRequestKey)
                        .set("id", key)
                        .set("userID", data.userID)
                        .set("message", data.message)
                        .set("creationDate", Timestamp.now())
                        .build();
                txn.add(removeCommunityRequest);
                txn.commit();
                LOG.fine("Remove community request: " + data.communityID + "'s remove request was registered in the database.");
                return Response.ok().build();
            } else {
                txn.rollback();
                LOG.fine("Remove community request: request already exists.");
                return Response.status(Status.CONFLICT).entity("Request already exists.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Remove community request: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if ( txn.isActive() ) {
                txn.rollback();
                LOG.severe("Remove community request: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/lock")
    public Response lockCommunity(@HeaderParam("authToken") String jsonToken, CommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Lock community: " + authToken.username + " attempted to lock the community with id " + data.communityID
                + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, data.communityID);
            Entity member = serverConstants.getCommunityMember(txn, data.communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkEditCommunityValidation(user, community, member, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                community = Entity.newBuilder(community.getKey())
                        .set("communityID", community.getString("communityID"))
                        .set("name", community.getString("name"))
                        .set("description", community.getString("description"))
                        .set("num_members", community.getLong("num_members"))
                        .set("username", community.getString("username"))
                        .set("isLocked", data.isLocked)
                        .set("creationDate", community.getTimestamp("creationDate"))
                        .build();
                txn.update(community);
                txn.commit();
                return Response.ok().entity("Community locked/unlocked successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Lock community: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Lock community: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/remove")
    public Response removeCommunity(@HeaderParam("authToken") String jsonToken, CommunityData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Remove Community: removal attempt of " + data.communityID + " by " + authToken.username + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, data.communityID);
            Entity member = serverConstants.getCommunityMember(txn, data.communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkEditCommunityValidation(user, community, member, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                var members = serverConstants.getMembersFromCommunity(txn, data.communityID);
                while ( members.hasNext() ) {
                    txn.delete(members.next().getKey());
                }
                var posts = serverConstants.getPostsFromCommunity(txn, data.communityID);
                while (posts.hasNext()) {
                    var postKey = posts.next().getKey();
                    var comments = serverConstants.getCommentsFromPost(txn, postKey);
                    while (comments.hasNext()) {
                        var nextKey = comments.next().getKey();
                        var commentLikes = serverConstants.getLikesFromComment(txn, nextKey);
                        while (commentLikes.hasNext()) {
                            txn.delete(commentLikes.next().getKey());
                        }
                        var commentDislikes = serverConstants.getDislikesFromComment(txn, nextKey);
                        while (commentDislikes.hasNext()) {
                            txn.delete(commentDislikes.next().getKey());
                        }
                        txn.delete(nextKey);
                    }
                    var likes = serverConstants.getLikesFromPost(txn, postKey);
                    while (likes.hasNext()) {
                        txn.delete(likes.next().getKey());
                    }
                    var dislikes = serverConstants.getDislikesFromPost(txn, postKey);
                    while (dislikes.hasNext()) {
                        txn.delete(dislikes.next().getKey());
                    }
                    txn.delete(postKey);
                }
                txn.delete(community.getKey());
                txn.commit();
                return Response.ok().entity("Community removed successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Remove community: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Remove community: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{communityID}/leave")
    public Response leaveCommunity(@HeaderParam("authToken") String jsonToken, @HeaderParam("username") String username, @PathParam("communityID") String communityID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Leave community: " + username + " attempted to leave the community with id " + communityID
                + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity adminMember = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity member = serverConstants.getCommunityMember(txn, communityID, username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            // var validation = Validations.checkLeaveCommunityValidation(user, community, adminMember, member, token, authToken, communityID);
            var validation = Response.status(Status.OK).build();
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                Key memberKey = serverConstants.getCommunityMemberKey(communityID, username);
                community = Entity.newBuilder(community.getKey())
                        .set("communityID", community.getString("communityID"))
                        .set("name", community.getString("name"))
                        .set("description", community.getString("description"))
                        .set("num_members", community.getLong("num_members") - 1L)
                        .set("username", community.getString("username"))
                        .set("isLocked", community.getBoolean("isLocked"))
                        .set("creationDate", community.getTimestamp("creationDate"))
                        .build();
                txn.update(community);
                txn.delete(memberKey);
                txn.commit();
                return Response.ok().entity("Left community successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Leave community: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Leave community: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{communityID}/update/manager")
    public Response updateManagerStatus(@HeaderParam("authToken") String jsonToken, @HeaderParam("username") String username, @HeaderParam("isManager") boolean isManager, @PathParam("communityID") String communityID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Update manager status: " + username + " attempted to be added or removed as a manager of the community with id " + communityID
                + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity adminMember = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity member = serverConstants.getCommunityMember(txn, communityID, username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            // var validation = Validations.checkAddManagerValidation(user, community, adminMember, member, token, authToken, communityID, isManager);
            var validation = Response.status(Status.OK).build();
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                Key memberKey = serverConstants.getCommunityMemberKey(communityID, username);
                member = Entity.newBuilder(memberKey)
                        .set("communityID", member.getString("communityID"))
                        .set("username", member.getString("username"))
                        .set("joinDate", member.getTimestamp("joinDate"))
                        .set("isManager", isManager)
                        .build();
                txn.update(member);
                txn.commit();
                return Response.ok().entity("Manager status updated successfully.").build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Update manager status: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Update manager status: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{communityID}/list/users")
    public Response listCommunityMembers(@HeaderParam("authToken") String jsonToken, @PathParam("communityID") String communityID) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("List community members: " + authToken.username + " attempted to list all members of the community with id " + communityID
                + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity community = serverConstants.getCommunity(txn, communityID);
            Entity member = serverConstants.getCommunityMember(txn, communityID, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            // var validation = Validations.checkAddManagerValidation(user, community, member, token, authToken, communityID);
            var validation = Response.status(Status.OK).build();
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(txn, authToken.username, authToken.tokenID);
                txn.commit();
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                txn.rollback();
                return validation;
            } else {
                List<User> membersList = new ArrayList<>();
                var members = serverConstants.getMembersFromCommunity(txn, communityID);
                while (members.hasNext()) {
                    Entity next = serverConstants.getUser(txn, members.next().getString("username"));
                    membersList.add(new User(next.getString("username"), next.getString("password"), next.getString("email"),
                            next.getString("name"), next.getString("phone"), next.getString("profile"),
                            next.getString("work"), next.getString("workplace"), next.getString("address"),
                            next.getString("postalcode"), next.getString("fiscal"), next.getString("role"),
                            next.getString("state"), next.getTimestamp("userCreationTime").toDate(), next.getString("photo")));
                }
                return Response.ok(g.toJson(membersList)).build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("List community members: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("List community members: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

}