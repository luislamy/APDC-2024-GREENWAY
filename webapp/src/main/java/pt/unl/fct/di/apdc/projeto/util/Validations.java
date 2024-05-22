package pt.unl.fct.di.apdc.projeto.util;

import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.datastore.Entity;

public class Validations {

    /**
     * Logger Object
     */
    private static Logger LOG = Logger.getLogger(Validations.class.getName());

    /** Operation codes */
    public static final int LOGIN = 101, GET_TOKEN = 102, LOGOUT = 103, LIST_USERS = 104, CHANGE_USER_DATA = 105,
            CHANGE_PASSWORD = 106, CHANGE_USER_ROLE = 107, CHANGE_USER_STATE = 108, REMOVE_USER = 109,
            SEARCH_USER = 110,
            USER_PROFILE = 111, SEND_MESSAGE = 112, RECEIVE_MESSAGES = 113, LOAD_CONVERSATION = 114;

    public static final int CREATE_COMMUNITY = 201, GET_COMMUNITIES = 202, GET_COMMUNITY = 203, JOIN_COMMUNITY = 204,
            EDIT_COMMUNITY = 205, REQUEST_REMOVE_COMMUNITY = 206, LOCK_COMMUNITY = 207, REMOVE_COMMUNITY = 208,
            LEAVE_COMMUNITY = 209, UPDATE_COMMUNITY_MANAGER = 210, LIST_COMMUNITY_MEMBERS = 211,
            LIST_COMMUNITY_POSTS = 212,
            LIST_COMMUNITY_THREADS = 213;

    public static final int ADD_POST = 241, GET_POST = 242, EDIT_POST = 243, REMOVE_POST = 244, LOCK_POST = 245,
            PIN_POST = 246, LIKE_POST = 247, DISLIKE_POST = 248, REPORT_POST = 249, LIST_COMMENTS = 250;

    public static final int ADD_COMMENT = 261, EDIT_COMMENT = 262, REMOVE_COMMENT = 263, LIKE_COMMENT = 264,
            DISLIKE_COMMENT = 265, PIN_COMMENT = 266, REPORT_COMMENT = 267;

    public static final int START_THREAD = 281, LOCK_THREAD = 282, PIN_THREAD = 283, REMOVE_THREAD = 284,
            ADD_THREAD_TAGS = 285, POST_THREAD_REPLY = 286, EDIT_THREAD_REPLY = 287, ADD_THREADMARK = 288,
            REMOVE_THREAD_REPLY = 289, LIKE_THREAD_REPLY = 290, REPORT_THREAD_REPLY = 291, GET_THREAD = 292;

    public static <T> Response checkValidation(int operation, Entity user, T data) {
        return checkValidation(operation, user, null, null, null, data);
    }

    public static Response checkValidation(int operation, Entity user, Entity token, AuthToken authToken) {
        return checkValidation(operation, user, null, token, authToken, null);
    }

    public static <T> Response checkValidation(int operation, Entity user, Entity token, AuthToken authToken, T data) {
        return checkValidation(operation, user, null, token, authToken, data);
    }

    public static <T> Response checkValidation(int operation, Entity admin, Entity user, Entity token,
            AuthToken authToken, T data) {
        if (operation == LOGIN)
            return checkLoginValidation(admin, (LoginData) data);
        else if (operation == GET_TOKEN)
            return checkGetTokenValidation(admin, token, authToken);
        else if (operation == LOGOUT)
            return checkLogoutValidation(admin, token, authToken);
        else if (operation == LIST_USERS)
            return checkListUsersValidation(admin, token, authToken);
        else if (operation == CHANGE_USER_DATA)
            return checkChangeUserDataValidation(admin, user, token, authToken, (ChangeData) data);
        else if (operation == CHANGE_PASSWORD)
            return checkChangePasswordValidation(admin, token, authToken, (PasswordData) data);
        else if (operation == CHANGE_USER_ROLE)
            return checkChangeUserRoleValidation(admin, user, token, authToken, (RoleData) data);
        else if (operation == CHANGE_USER_STATE)
            return checkChangeUserStateValidation(admin, user, token, authToken, (UsernameData) data);
        else if (operation == REMOVE_USER)
            return checkRemoveUserValidation(admin, user, token, authToken, (UsernameData) data);
        else if (operation == SEARCH_USER)
            return checkSearchUserValidation(admin, user, token, authToken, (UsernameData) data);
        else if (operation == USER_PROFILE)
            return checkUserProfileValidation(admin, token, authToken);
        else if (operation == SEND_MESSAGE)
            return checkSendMessageValidation(admin, user, token, authToken, (MessageClass) data);
        else if (operation == RECEIVE_MESSAGES)
            return checkReceiveMessagesValidation(admin, token, authToken);
        else if (operation == LOAD_CONVERSATION)
            return checkLoadConversationValidation(admin, user, token, authToken, (ConversationClass) data);
        else
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal server validation error.").build();
    }

    private static Response checkLoginValidation(Entity user, LoginData data) {
        String operation = "Login: ";
        if (validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        if (user.getString("state").equals(ServerConstants.INACTIVE)) {
            LOG.warning(operation + data.username + " not an active user.");
            return Response.status(Status.FORBIDDEN).entity("User's account is inactive.").build();
        }
        String hashedPassword = (String) user.getString("password");
        if (!hashedPassword.equals(DigestUtils.sha3_512Hex(data.password))) {
            LOG.warning(operation + data.username + " provided wrong password.");
            return Response.status(Status.FORBIDDEN).entity("Wrong password.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response checkGetTokenValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Token: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkLogoutValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Logout: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkListUsersValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "List users: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkChangeUserDataValidation(Entity admin, Entity user, Entity token, AuthToken authToken,
            ChangeData data) {
        String operation = "Data change: ";
        if (validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        if (validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var validateTokenResponse = validateToken(operation, admin, token, authToken);
        if (validateTokenResponse.getStatus() != Status.OK.getStatusCode()) {
            return validateTokenResponse;
        } else {
            if (authToken.role.equals(ServerConstants.USER) && !data.username.equals(authToken.username)) {
                LOG.warning(operation + authToken.username + " cannot change other user's data.");
                return Response.status(Status.FORBIDDEN).entity("User role cannot change other user's data.").build();
            }
            int validData = data.validData();
            if (validData != 0) {
                LOG.warning(operation + "data change attempt using invalid " + data.getInvalidReason(validData) + ".");
                return Response.status(Status.BAD_REQUEST).entity("Invalid " + data.getInvalidReason(validData) + ".")
                        .build();
            }
            String adminRole = admin.getString("role");
            if (adminRole.equals(ServerConstants.USER) || adminRole.equals(ServerConstants.EP)
                    || adminRole.equals(ServerConstants.GC)) {
                if (data.password != null && data.password.trim().isEmpty() &&
                        data.email != null && data.email.trim().isEmpty() &&
                        data.name != null && data.name.trim().isEmpty())
                    return Response.ok().build();
                else
                    return Response.status(Status.BAD_REQUEST).entity("User cannot change password, email or name.")
                            .build();
            } else if (adminRole.equals(ServerConstants.GBO)) {
                if (!user.getString("role").equals(ServerConstants.USER)) {
                    LOG.warning(operation + authToken.username + " cannot change non USER users data.");
                    return Response.status(Status.FORBIDDEN).entity("GBO users cannot change data of non USER users.")
                            .build();
                }
                if (data.role != null && !data.role.trim().isEmpty()) {
                    LOG.warning(operation + authToken.username + " cannot change users' role.");
                    return Response.status(Status.FORBIDDEN).entity("GBO users cannot change users' role.").build();
                }
            } else if (adminRole.equals(ServerConstants.GA)) {
                if (!user.getString("role").equals(ServerConstants.USER)
                        && !user.getString("role").equals(ServerConstants.GBO)) {
                    LOG.warning(operation + authToken.username + " cannot change GA or SU users data.");
                    return Response.status(Status.FORBIDDEN).entity("GA users cannot change data of GA or SU users.")
                            .build();
                } else if (data.role.equals(ServerConstants.GA) || data.role.equals(ServerConstants.SU)) {
                    LOG.warning(operation + authToken.username + " cannot change users' role to GA or SU roles.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GA users cannot change users' role to GA or SU roles.").build();
                }
            } else if (adminRole.equals(ServerConstants.GS)) {
                if (user.getString("role").equals(ServerConstants.SU)) {
                    LOG.warning(operation + authToken.username + " cannot change SU users data.");
                    return Response.status(Status.FORBIDDEN).entity("GS users cannot change data of SU users.").build();
                }
            } else if (adminRole.equals(ServerConstants.SU)) {
                if (!user.getString("role").equals(ServerConstants.USER)
                        && !user.getString("role").equals(ServerConstants.GBO)
                        && !user.getString("role").equals(ServerConstants.GA)) {
                    LOG.warning(operation + authToken.username + " cannot change SU users data.");
                    return Response.status(Status.FORBIDDEN).entity("SU users cannot change data of SU users.").build();
                }
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkChangePasswordValidation(Entity user, Entity token, AuthToken authToken,
            PasswordData data) {
        String operation = "Password change: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var validateTokenResponse = validateToken(operation, user, token, authToken);
        if (validateTokenResponse.getStatus() != Status.OK.getStatusCode()) {
            return validateTokenResponse;
        } else {
            if (!data.validPasswordData()) {
                LOG.warning(operation + "password change attempt using missing or invalid parameters.");
                return Response.status(Status.BAD_REQUEST).entity("Missing or invalid parameter.").build();
            }
            String hashedPassword = (String) user.getString("password");
            if (!hashedPassword.equals(DigestUtils.sha3_512Hex(data.oldPassword))) {
                LOG.warning(operation + authToken.username + " provided wrong password.");
                return Response.status(Status.FORBIDDEN).entity("Wrong password.").build();
            } else {
                return Response.ok().build();
            }
        }
    }

    private static Response checkChangeUserRoleValidation(Entity admin, Entity user, Entity token, AuthToken authToken,
            RoleData data) {
        String operation = "Role change: ";
        if (validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if (validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        var validateTokenResponse = validateToken(operation, user, token, authToken);
        if (validateTokenResponse.getStatus() != Status.OK.getStatusCode()) {
            return validateTokenResponse;
        } else {
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)
                    || authToken.role.equals(ServerConstants.GC)
                    || authToken.role.equals(ServerConstants.GBO)) {
                LOG.warning(operation + "unauthorized attempt to change the role of a user.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to change user accounts role.")
                        .build();
            } else if (authToken.role.equals(ServerConstants.GA) && (data.role.equals(ServerConstants.GA) ||
                    data.role.equals(ServerConstants.GS) || data.role.equals(ServerConstants.SU))) {
                LOG.warning(operation + "GA users cannot change users into GA, GS or SU roles.");
                return Response.status(Status.FORBIDDEN)
                        .entity("User is not authorized to change users to GA, GS or SU roles.").build();
            } else if (authToken.role.equals(ServerConstants.GS) &&
                    (data.role.equals(ServerConstants.GS) || data.role.equals(ServerConstants.SU))) {
                LOG.warning(operation + "GS users cannot change users into GS or SU roles.");
                return Response.status(Status.FORBIDDEN)
                        .entity("User is not authorized to change users to GS or SU roles.").build();
            }
            if (user.getString("role").equals(data.role)) {
                LOG.fine("Role change: User already has the same role.");
                return Response.status(Status.NOT_MODIFIED)
                        .entity("User already had the same role, role remains unchanged.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkChangeUserStateValidation(Entity admin, Entity user, Entity token, AuthToken authToken,
            UsernameData data) {
        String operation = "State change: ";
        if (validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if (validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        var validateTokenResponse = validateToken(operation, admin, token, authToken);
        if (validateTokenResponse.getStatus() != Status.OK.getStatusCode()) {
            return validateTokenResponse;
        } else {
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)
                    || authToken.role.equals(ServerConstants.GC)) {
                LOG.warning(operation + "unauthorized attempt to change the state of a user.");
                return Response.status(Status.FORBIDDEN).entity("User cannot change state of any user.").build();
            }
            String adminRole = admin.getString("role");
            String userRole = user.getString("role");
            if (adminRole.equals(ServerConstants.GBO)) {
                if (!userRole.equals(ServerConstants.USER)) {
                    // GBO users can only change USER states
                    LOG.warning(operation + authToken.username + " attempted to change the state of a non USER role.");
                    return Response.status(Status.FORBIDDEN).entity("GBO users cannot change non USER roles' states.")
                            .build();
                }
            } else if (adminRole.equals(ServerConstants.GA)) {
                if (!userRole.equals(ServerConstants.USER) && !userRole.equals(ServerConstants.GBO)) {
                    // GA users can change USER and GBO states
                    LOG.warning(operation + authToken.username
                            + " attempted to change the state of a non USER or GBO role.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GA users cannot change non USER and GBO roles' states.").build();
                }
            } else if (adminRole.equals(ServerConstants.GS)) {
                if (userRole.equals(ServerConstants.SU)) {
                    LOG.warning(operation + authToken.username + " attempted to change the state of a SU role.");
                    return Response.status(Status.FORBIDDEN).entity("GS users cannot change SU users' states.").build();
                }
            } else if (adminRole.equals(ServerConstants.SU)) {
            } else if (adminRole.equals(ServerConstants.USER) || adminRole.equals(ServerConstants.EP)
                    || adminRole.equals(ServerConstants.GC)) {
                LOG.warning(operation + authToken.username
                        + " attempted to change the state of a user as a USER, EP or GC role.");
                return Response.status(Status.FORBIDDEN).entity("User cannot change states.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemoveUserValidation(Entity admin, Entity user, Entity token, AuthToken authToken,
            UsernameData data) {
        String operation = "Remove user: ";
        if (validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if (validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        var validateTokenResponse = validateToken(operation, admin, token, authToken);
        if (validateTokenResponse.getStatus() != Status.OK.getStatusCode()) {
            return validateTokenResponse;
        } else {
            if (authToken.role.equals(ServerConstants.GBO)) {
                LOG.warning(operation + "GBO users cannot remove any accounts.");
                return Response.status(Status.FORBIDDEN).entity("GBO users cannot remove any accounts.").build();
            } else if (authToken.role.equals(ServerConstants.USER) && !authToken.username.equals(data.username)) {
                LOG.warning(operation + "USER users cannot remove any accounts other than their own.");
                return Response.status(Status.FORBIDDEN)
                        .entity("USER users cannot remove any accounts other than their own.").build();
            } else if (authToken.role.equals(ServerConstants.EP) && !authToken.username.equals(data.username)) {
                LOG.warning(operation + "EP users cannot remove any accounts other than their own.");
                return Response.status(Status.FORBIDDEN)
                        .entity("EP users cannot remove any accounts other than their own.")
                        .build();
            }
            String adminRole = admin.getString("role");
            String role = user.getString("role");
            if (adminRole.equals(ServerConstants.USER)) {
                if (!role.equals(ServerConstants.USER) || !user.equals(admin)) {
                    LOG.warning(operation + authToken.username + " (USER role) attempted to delete other user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("USER roles cannot remove other users from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.EP)) {
                if (!role.equals(ServerConstants.EP) || !user.equals(admin)) {
                    LOG.warning(operation + authToken.username + " (EP role) attempted to delete other user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("EP roles cannot remove other users from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.GC)) {
                if (!role.equals(ServerConstants.GC) || !user.equals(admin)) {
                    LOG.warning(operation + authToken.username + " (GC role) attempted to delete other user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GC roles cannot remove other users from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.GA)) {
                if (!role.equals(ServerConstants.GBO) && !role.equals(ServerConstants.USER)
                        && !role.equals(ServerConstants.EP)) {
                    LOG.warning(operation + authToken.username + " (GA role) attempted to delete SU, GS or GA user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GA roles cannot remove GA, GS or SU user from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.GS)) {
                if (!role.equals(ServerConstants.GBO) && !role.equals(ServerConstants.USER) &&
                        !role.equals(ServerConstants.EP) && !role.equals(ServerConstants.GA)) {
                    LOG.warning(operation + authToken.username + " (GS role) attempted to delete SU or GS user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GS roles cannot remove GS or SU users from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.SU)) {
            } else if (adminRole.equals(ServerConstants.GBO)) {
                LOG.warning(operation + authToken.username + " (GBO role) attempted to delete user.");
                return Response.status(Status.FORBIDDEN).entity("GBO roles cannot remove users from the database.")
                        .build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkSearchUserValidation(Entity admin, Entity user, Entity token, AuthToken authToken,
            UsernameData data) {
        String operation = "Search user: ";
        if (validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if (validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, token, authToken);
        if (validateTokenResponse.getStatus() != Status.OK.getStatusCode()) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            String role = user.getString("role");
            String state = user.getString("state");
            String profile = user.getString("profile");
            if (adminRole.equals(ServerConstants.USER) || adminRole.equals(ServerConstants.EP)) {
                if ((!role.equals(ServerConstants.USER) && !role.equals(ServerConstants.EP))
                        && !state.equals(ServerConstants.ACTIVE) && !profile.equals(ServerConstants.PUBLIC)) {
                    LOG.warning(operation + authToken.username
                            + " (USER/EP role) attempted to search non USER/EP, inactive or private users' information.");
                    return Response.status(Status.FORBIDDEN).entity(
                            "USER/EP roles cannot search for other non USER/EP, inactive or private users' from the database.")
                            .build();
                }
            } else if (adminRole.equals(ServerConstants.GC)) {
                if (!role.equals(ServerConstants.USER) && !role.equals(ServerConstants.EP)
                        && !role.equals(ServerConstants.GC)) {
                    LOG.warning(operation + authToken.username + " (GC role) attempted to search higher user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GC roles cannot search for higher users from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.GBO)) {
                if (!role.equals(ServerConstants.GBO) && !role.equals(ServerConstants.USER)
                        && !role.equals(ServerConstants.EP) && !role.equals(ServerConstants.GC)) {
                    LOG.warning(operation + authToken.username + " (GBO role) attempted to search higher user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GBO roles cannot search for higher users from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.GA)) {
                if (role.equals(ServerConstants.SU) || role.equals(ServerConstants.GS)) {
                    LOG.warning(operation + authToken.username + " (GA role) attempted to search higher user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GA roles cannot search for higher users from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.GS)) {
                if (role.equals(ServerConstants.SU)) {
                    LOG.warning(operation + authToken.username + " (GS role) attempted to search higher user.");
                    return Response.status(Status.FORBIDDEN)
                            .entity("GS roles cannot search for higher users from the database.").build();
                }
            } else if (adminRole.equals(ServerConstants.SU)) {
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkUserProfileValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Get user: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkSendMessageValidation(Entity sender, Entity receiver, Entity token,
            AuthToken authToken, MessageClass message) {
        String operation = "Send Message: ";
        if (validateUser(operation, sender, message.sender).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(message.sender + " is not a registered user.").build();
        }
        if (validateUser(operation, receiver, message.receiver).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(message.receiver + " is not a registered user.").build();
        }
        var validateTokenResponse = validateToken(operation, sender, token, authToken);
        if (validateTokenResponse.getStatus() != Status.OK.getStatusCode()) {
            return validateTokenResponse;
        } else {
            if (receiver.getString("state").equals(ServerConstants.INACTIVE)) {
                LOG.warning(operation + message.receiver + " is not an active user.");
                return Response.status(Status.FORBIDDEN).entity("Receiver's account is inactive.").build();
            }
            String senderRole = sender.getString("role");
            String receiverRole = receiver.getString("role");
            if (senderRole.equals(ServerConstants.USER) || senderRole.equals(ServerConstants.EP)) {
                if ((!receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.EP))
                        && !receiver.getString("profile").equals(ServerConstants.PUBLIC)) {
                    LOG.fine(
                            operation + "USER/EP roles cannot send messages to non USER/EP roles or private profiles.");
                    return Response.status(Status.FORBIDDEN).entity(
                            "USER/EP roles cannot send messages to non USER/EP roles or users with private profiles.")
                            .build();
                }
            } else if (senderRole.equals(ServerConstants.GC)) {
                if (!receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.EP)
                        || !receiverRole.equals(ServerConstants.GC) || !receiverRole.equals(ServerConstants.GBO)) {
                    LOG.fine(operation + "GC roles cannot send messages to GA/GS/SU roles.");
                    return Response.status(Status.FORBIDDEN).entity("GC roles cannot send messages to GA/GS/SU roles.")
                            .build();
                }
            } else if (senderRole.equals(ServerConstants.GBO)) {
                if (receiverRole.equals(ServerConstants.GS) && !receiverRole.equals(ServerConstants.SU)) {
                    LOG.fine(operation + "GBO roles cannot send messages to GS/SU roles.");
                    return Response.status(Status.FORBIDDEN).entity("GBO roles cannot send messages to GS/SU roles.")
                            .build();
                }
            } else if (senderRole.equals(ServerConstants.GA) || senderRole.equals(ServerConstants.GS)) {
                if (receiverRole.equals(ServerConstants.SU)) {
                    LOG.fine(operation + "GA roles cannot send messages to SU roles.");
                    return Response.status(Status.FORBIDDEN).entity("GA roles cannot send messages to SU roles.")
                            .build();
                }
            } else if (senderRole.equals(ServerConstants.SU)) {
                LOG.fine(operation + "SU roles cannot send messages.");
                return Response.status(Status.FORBIDDEN).entity("SU roles cannot send messages.")
                        .build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkReceiveMessagesValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Receive Messages: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var validateTokenResponse = validateToken(operation, user, token, authToken);
        if (validateTokenResponse.getStatus() != Status.OK.getStatusCode()) {
            return validateTokenResponse;
        } else {
            String role = user.getString("role");
            if (role.equals(ServerConstants.USER) || role.equals(ServerConstants.EP) || role.equals(ServerConstants.GC)
                    || role.equals(ServerConstants.GBO) || role.equals(ServerConstants.GA)
                    || role.equals(ServerConstants.GS)) {
                return Response.ok().build();
            } else if (role.equals(ServerConstants.SU)) {
                LOG.fine(operation + "SU roles cannot receive messages.");
                return Response.status(Status.FORBIDDEN).entity("SU roles cannot receive messages.")
                        .build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    private static Response checkLoadConversationValidation(Entity sender, Entity receiver, Entity token,
            AuthToken authToken, ConversationClass data) {
        String operation = "Load Conversation: ";
        if (validateUser(operation, sender, data.sender).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(data.sender + " is not a registered user.").build();
        }
        if (validateUser(operation, receiver, data.receiver).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(data.receiver + " is not a registered user.").build();
        }
        return validateToken(operation, sender, token, authToken);
    }

    /*********************** Communities Validations ***********************/

    public static Response checkCommunitiesValidations(int operation, Entity user, Entity token, AuthToken authToken) {
        return checkCommunitiesValidations(operation, user, null, token, authToken);
    }

    public static Response checkCommunitiesValidations(int operation, Entity user, Entity token, AuthToken authToken,
            CommunityData data) {
        return checkCommunitiesValidations(operation, user, null, null, token, authToken, data);
    }

    public static Response checkCommunitiesValidations(int operation, Entity user, Entity community, Entity token,
            AuthToken authToken) {
        return checkCommunitiesValidations(operation, user, community, null, token, authToken, null);
    }

    public static Response checkCommunitiesValidations(int operation, Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken) {
        return checkCommunitiesValidations(operation, user, community, member, token, authToken, null);
    }

    public static <T> Response checkCommunitiesValidations(int operation, Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken, T data) {
        return checkCommunitiesValidations(operation, user, community, member, null, token, authToken, data);
    }

    public static Response checkCommunitiesValidations(int operation, Entity user, Entity community, Entity member,
            Entity adminMember, Entity token, AuthToken authToken) {
        return checkCommunitiesValidations(operation, user, community, member, adminMember, token, authToken, null);
    }

    public static <T> Response checkCommunitiesValidations(int operation, Entity user, Entity community, Entity member,
            Entity adminMember, Entity token, AuthToken authToken, T data) {
        switch (operation) {
            case GET_COMMUNITIES:
                return checkGetCommunitiesValidation(user, token, authToken);
            case CREATE_COMMUNITY:
                return checkCreateCommunityValidation(user, token, authToken, (CommunityData) data);
            case GET_COMMUNITY:
                return checkGetCommunityValidation(user, community, member, token, authToken);
            case JOIN_COMMUNITY:
                return checkJoinCommunityValidation(user, community, member, token, authToken);
            case EDIT_COMMUNITY:
                return checkEditCommunityValidation(user, community, member, token, authToken, (CommunityData) data);
            case REQUEST_REMOVE_COMMUNITY:
                return checkRequestRemoveCommunityValidation(user, community, member, token, authToken,
                        (RemoveCommunityRequest) data);
            case LOCK_COMMUNITY:
                return checkLockCommunityValidation(user, community, member, token, authToken, (CommunityData) data);
            case REMOVE_COMMUNITY:
                return checkRemoveCommunityValidation(user, community, token, authToken);
            case LEAVE_COMMUNITY:
                return checkLeaveCommunityValidation(user, community, member, adminMember, token, authToken,
                        (CommunityData) data);
            case UPDATE_COMMUNITY_MANAGER:
                return checkUpdateCommunityManagerValidation(user, community, member, adminMember, token, authToken,
                        (boolean) data);
            case LIST_COMMUNITY_MEMBERS:
                return checkListCommunityMembersValidation(user, community, member, token, authToken);
            case LIST_COMMUNITY_POSTS:
                return checkListCommunityPostsValidation(user, community, member, token, authToken);
            case LIST_COMMUNITY_THREADS:
                return checkListCommunityThreadsValidation(user, community, member, token, authToken);
            default:
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal server error during communities validations.").build();
        }
    }

    private static Response checkGetCommunitiesValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Get communities: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkCreateCommunityValidation(Entity user, Entity token, AuthToken authToken,
            CommunityData data) {
        String operation = "Create community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            var userRole = user.getString("role");
            if (userRole.equals(ServerConstants.USER) || userRole.equals(ServerConstants.EP)
                    || userRole.equals(ServerConstants.GC)) {
                var code = data.isValid();
                if (code < 1) {
                    LOG.fine(operation + data.getInvalidReason(code) + "provided.");
                    return Response.status(Status.BAD_REQUEST)
                            .entity(operation + data.getInvalidReason(code) + "provided.").build();
                }
                return Response.ok().build();
            } else {
                return Response.status(Status.FORBIDDEN).entity("User is not allowed to create communities").build();
            }
        }
    }

    private static Response checkGetCommunityValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken) {
        String operation = "Get community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (member == null && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "user is not a member of the community or a communities manager.");
                return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked.");
                return Response.status(Status.CONFLICT).entity("Community is locked.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkJoinCommunityValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken) {
        String operation = "Join community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked")) {
                LOG.info(operation + "community is locked.");
                return Response.status(Status.CONFLICT).entity("Community is locked.").build();
            }
            if (member != null) {
                LOG.info(operation + "user is already a member of the community.");
                return Response.status(Status.CONFLICT).entity("User is already a member of the community.").build();
            }
            var userRole = user.getString("role");
            if (userRole.equals(ServerConstants.USER) || userRole.equals(ServerConstants.EP)
                    || userRole.equals(ServerConstants.GC)) {
                return Response.ok().build();
            } else {
                return Response.status(Status.FORBIDDEN).entity("User is not allowed to join communities.").build();
            }
        }
    }

    private static Response checkEditCommunityValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken, CommunityData data) {
        String operation = "Edit community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation
                        + "community is locked and user is not a manager of the community or a communities manager.");
                return Response.status(Status.FORBIDDEN).entity(
                        "Community is locked and user is not a manager of the community or a communities manager.")
                        .build();
            }
            boolean nameChange = data.name != null && !data.name.trim().isEmpty()
                    ? !data.name.equals(community.getString("name"))
                    : false;
            boolean descriptionChange = data.description != null && !data.description.trim().isEmpty()
                    ? !data.description.equals(community.getString("description"))
                    : false;
            if (!nameChange && !descriptionChange) {
                LOG.fine(operation + "user made no change to the name or description of the community.");
                return Response.status(Status.BAD_REQUEST).entity("No changes to be made.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRequestRemoveCommunityValidation(Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken, RemoveCommunityRequest data) {
        String operation = "Remove community request: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (member == null) {
                LOG.info(operation + "user is not a member of the community.");
                return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
            } else if (!member.getBoolean("isManager")) {
                LOG.info(operation + "user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
            }
            if (data.message == null || data.message.trim().isEmpty()) {
                LOG.fine(operation + "user gave no reason for removing the community.");
                return Response.status(Status.BAD_REQUEST).entity("User gave no reason for removing the community.")
                        .build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkLockCommunityValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken, CommunityData data) {
        String operation = "Lock community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
            }
            if ((community.getBoolean("isLocked") && data.isLocked)
                    || (!community.getBoolean("isLocked") && !data.isLocked)) {
                LOG.fine(operation + "user made no change to the lock status of the community.");
                return Response.status(Status.BAD_REQUEST).entity("No changes to be made.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemoveCommunityValidation(Entity user, Entity community, Entity token,
            AuthToken authToken) {
        String operation = "Remove community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (!authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "user is not a communities manager.");
                return Response.status(Status.FORBIDDEN)
                        .entity("User is not a communities manager. Only communities managers can remove communities.")
                        .build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkLeaveCommunityValidation(Entity user, Entity community, Entity member,
            Entity adminMember, Entity token, AuthToken authToken, CommunityData data) {
        String operation = "Leave community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (member == null) {
                LOG.info(operation + "user is not a member of the community.");
                return Response.status(Status.CONFLICT).entity("User is not a member of the community.").build();
            } else if (member != adminMember && (adminMember == null || !adminMember.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
            } else if (member != adminMember && member.getBoolean("isManager") && adminMember.getBoolean("isManager")
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community managers can only be banned from the community by a GC user.");
                return Response.status(Status.FORBIDDEN)
                        .entity("Community managers can only be banned from the community by a GC user.").build();
            } else if (member == adminMember && !member.getBoolean("isManager")
                    && !authToken.role.equals(ServerConstants.GC) && community.getBoolean("isLocked")) {
                LOG.info(operation + "community is locked.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked.").build();
            }
            return Response.ok().build();
            /*
             * if ( member != null && !member.getBoolean("isManager") ) {
             * if ( adminMember != null && adminMember == member ) {
             * return Response.ok().build();
             * } else if ( adminMember != null && adminMember != member &&
             * !community.getBoolean("isLocked") ) {
             * if ( adminMember.getBoolean("isManager") ) {
             * return Response.ok().build();
             * } else {
             * LOG.info(operation + "user is not a manager of the community.");
             * return Response.status(Status.FORBIDDEN).
             * entity("User is not a manager of the community.").build();
             * }
             * } else {
             * if ( authToken.role.equals(ServerConstants.GC) ) {
             * return Response.ok().build();
             * } else {
             * LOG.info(operation + "user is not a manager of the communities.");
             * return Response.status(Status.FORBIDDEN).
             * entity("User is not a manager of the communities.").build();
             * }
             * }
             * } else if ( member != null && member.getBoolean("isManager") ) {
             * if ( adminMember != null && adminMember == member ) {
             * return Response.ok().build();
             * } else if ( adminMember != null && adminMember != member ) {
             * if ( adminMember.getBoolean("isManager") ) {
             * LOG.info(operation +
             * "community managers are not allowed to ban other community managers.");
             * return Response.status(Status.FORBIDDEN).
             * entity("Community managers are not allowed to ban other community managers.")
             * .build();
             * } else {
             * LOG.info(operation + "user is not a manager of the community.");
             * return Response.status(Status.FORBIDDEN).
             * entity("User is not a manager of the community.").build();
             * }
             * } else {
             * if ( authToken.role.equals(ServerConstants.GC) ) {
             * return Response.ok().build();
             * } else {
             * LOG.info(operation + "user is not a manager of the communities.");
             * return Response.status(Status.FORBIDDEN).
             * entity("User is not a manager of the communities.").build();
             * }
             * }
             * } else {
             * LOG.info(operation + "user is not a member of the community.");
             * return Response.status(Status.CONFLICT).
             * entity("User is not a member of the community.").build();
             * }
             */
        }
    }

    private static Response checkUpdateCommunityManagerValidation(Entity user, Entity community, Entity member,
            Entity adminMember, Entity token, AuthToken authToken, boolean isManager) {
        String operation = "Update manager status: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (member == null) {
                LOG.info(operation + "user is not a member of the community.");
                return Response.status(Status.CONFLICT).entity("User is not a member of the community.").build();
            } else if (!adminMember.getBoolean("isManager") && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
            }
            if ((isManager && member.getBoolean("isManager")) || (!isManager && member.getBoolean("isManager"))) {
                LOG.fine(operation + "user made no change to the manager status of the member.");
                return Response.status(Status.BAD_REQUEST).entity("No changes to be made.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkListCommunityMembersValidation(Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken) {
        String operation = "List community members: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && !member.getBoolean("isManager")
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked and user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN)
                        .entity("Community is locked and user is not a manager of the community.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkListCommunityPostsValidation(Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken) {
        String operation = "List community posts: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && !member.getBoolean("isManager")
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked and user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN)
                        .entity("Community is locked and user is not a manager of the community.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkListCommunityThreadsValidation(Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken) {
        String operation = "List community threads: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && !member.getBoolean("isManager")
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked and user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN)
                        .entity("Community is locked and user is not a manager of the community.").build();
            }
            return Response.ok().build();
        }
    }

    /************************ Posts Validations ********************/

    public static Response checkPostsValidations(int operation, Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken, PostData data) {
        return checkPostsValidations(operation, user, community, null, member, token, authToken, data);
    }

    public static Response checkPostsValidations(int operation, Entity user, Entity community, Entity post,
            Entity member, Entity token, AuthToken authToken) {
        return checkPostsValidations(operation, user, community, post, member, token, authToken, null);
    }

    public static Response checkPostsValidations(int operation, Entity user, Entity community, Entity post,
            Entity member, Entity token, AuthToken authToken, PostData data) {
        return checkPostsValidations(operation, user, community, post, member, null, token, authToken, data);
    }

    public static Response checkPostsValidations(int operation, Entity user, Entity community, Entity post,
            Entity member, Entity relation, Entity token, AuthToken authToken, PostData data) {
        switch (operation) {
            case ADD_POST:
                return checkAddPostValidation(user, community, member, token, authToken, data);
            case GET_POST:
                return checkGetPostValidation(user, community, post, member, token, authToken);
            case EDIT_POST:
                return checkEditPostValidation(user, community, post, member, token, authToken, data);
            case REMOVE_POST:
                return checkRemovePostValidation(user, community, post, member, token, authToken);
            case LOCK_POST:
                return checkLockPostValidation(user, community, post, member, token, authToken, data);
            case PIN_POST:
                return checkPinPostValidation(user, community, post, member, token, authToken, data);
            case LIKE_POST:
                return checkLikePostValidation(user, community, post, member, relation, token,
                        authToken, data);
            case DISLIKE_POST:
                return checkDislikePostValidation(user, community, post, member, relation, token,
                        authToken, data);
            case LIST_COMMENTS:
                return checkListCommentsValidation(user, community, post, member, token, authToken);
            default:
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal server error during posts validations.").build();
        }
    }

    private static Response checkAddPostValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken, PostData data) {
        String operation = "Add post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not add posts.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not add posts.").build();
            }
            if (member == null && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "user is not a member of the community or a community manager.");
                return Response.status(Status.FORBIDDEN)
                        .entity("User is not a member of the community or a community manager.").build();
            } else if (!authToken.role.equals(ServerConstants.GC) && !authToken.role.equals(ServerConstants.USER)
                    && !authToken.role.equals(ServerConstants.EP)) {
                LOG.info(operation + "user may not post to communities.");
                return Response.status(Status.FORBIDDEN).entity("User may not post to communities.").build();
            }
            var code = data.isValidToPost(authToken.username);
            if (code < 1) {
                LOG.fine(operation + "invalid data to post, " + data.getInvalidReason(code) + ".");
                return Response.status(Status.BAD_REQUEST)
                        .entity("Invalid data to post, " + data.getInvalidReason(code) + ".").build();
            }
            if ((data.isLocked || data.isPinned)
                    && !authToken.role.equals(ServerConstants.GC)
                    && (member == null || !member.getBoolean("isManager"))) {
                LOG.fine(operation + "non-manager user tried to pin/lock post.");
                return Response.status(Status.FORBIDDEN).entity("User is not allowed to pin/lock posts.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkGetPostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity token, AuthToken authToken) {
        String operation = "Get post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not access posts.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not access posts.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkEditPostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity token, AuthToken authToken, PostData data) {
        String operation = "Edit post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not edit posts.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not edit posts.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (post.getBoolean("isLocked")) {
                LOG.info(operation + "post is locked, user may not edit post.");
                return Response.status(Status.FORBIDDEN).entity("Post is locked, user may not edit post.").build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
                if (!post.getString("username").equals(user.getString("username"))) {
                    LOG.info(operation + "user is not authorized to edit this post.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to edit this post.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
                if (!post.getString("username").equals(user.getString("username"))) {
                    LOG.info(operation + "user is not authorized to edit this post.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to edit this post.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GBO)
                    || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS) || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GC/GBO/GA/GS/SU users are not authorized to edit posts.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to edit posts.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            var code = data.isValidToPost(authToken.username);
            if (code < 1) {
                LOG.fine(operation + "invalid data to post, " + data.getInvalidReason(code) + ".");
                return Response.status(Status.BAD_REQUEST)
                        .entity("Invalid data to post, " + data.getInvalidReason(code) + ".").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemovePostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity token, AuthToken authToken) {
        String operation = "Remove post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not remove post.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not remove post.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
                if (!post.getString("username").equals(user.getString("username")) && !member.getBoolean("isManager")) {
                    LOG.info(operation + "user is not authorized to remove this post.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to remove this post.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GC) || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)) {
                LOG.info(operation + "GA/GBO users are not authorized to remove posts.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to remove posts.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkLockPostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity token, AuthToken authToken, PostData data) {
        String operation = "Lock post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not lock post.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not lock post.").build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
                if (!post.getString("username").equals(user.getString("username")) && !member.getBoolean("isManager")) {
                    LOG.info(operation + "user is not authorized to lock this post.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to lock this post.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to lock posts.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to lock posts.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkPinPostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity token, AuthToken authToken, PostData data) {
        String operation = "Pin post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not pin post.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not pin post.").build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
                if (!member.getBoolean("isManager")) {
                    LOG.info(operation + "user is not authorized to pin posts.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to pin posts.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to pin posts.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to pin posts.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isPinned && post.getBoolean("isPinned")) {
                LOG.info(operation + "post is already pinned.");
                return Response.status(Status.CONFLICT).entity("Post is already pinned.").build();
            } else if (!data.isPinned && !post.getBoolean("isPinned")) {
                LOG.info(operation + "post is not pinned.");
                return Response.status(Status.CONFLICT).entity("Post is not pinned.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkLikePostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity likeRelation, Entity token, AuthToken authToken, PostData data) {
        String operation = "Like post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not like post.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not like post.").build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to like posts.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to like posts.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isLiked && likeRelation != null) {
                LOG.info(operation + "user already liked post.");
                return Response.status(Status.CONFLICT).entity("User already liked post.").build();
            } else if (!data.isLiked && likeRelation == null) {
                LOG.info(operation + "user hasn't liked the post.");
                return Response.status(Status.CONFLICT).entity("User hasn't liked the post.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkDislikePostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity dislikeRelation, Entity token, AuthToken authToken, PostData data) {
        String operation = "Dislike post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not dislike post.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not dislike post.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to dislike posts.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to dislike posts.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isDisliked && dislikeRelation != null) {
                LOG.info(operation + "user already disliked post.");
                return Response.status(Status.CONFLICT).entity("User already disliked post.").build();
            } else if (!data.isDisliked && dislikeRelation == null) {
                LOG.info(operation + "user hasn't disliked the post.");
                return Response.status(Status.CONFLICT).entity("User hasn't disliked the post.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkListCommentsValidation(Entity user, Entity community, Entity post, Entity member,
            Entity token, AuthToken authToken) {
        String operation = "List comments: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not view posts.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not view posts.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            return Response.ok().build();
        }
    }

    /************** Comments Validations *******************/

    public static Response checkCommentsValidations(int operation, Entity user, Entity community, Entity post,
            Entity comment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        return checkCommentsValidations(operation, user, community, post, comment, member, null, token, authToken,
                data);
    }

    public static Response checkCommentsValidations(int operation, Entity user, Entity community, Entity post,
            Entity comment, Entity member, Entity relation, Entity token, AuthToken authToken, CommentData data) {
        switch (operation) {
            case ADD_COMMENT:
                return checkAddCommentValidation(user, community, post, comment, member, token, authToken, data);
            case EDIT_COMMENT:
                return checkEditCommentValidation(user, community, post, comment, member, token, authToken, data);
            case REMOVE_COMMENT:
                return checkRemoveCommentValidation(user, community, post, comment, member, token, authToken, data);
            case LIKE_COMMENT:
                return checkLikeCommentValidation(user, community, post, comment, member, relation, token, authToken,
                        data);
            case DISLIKE_COMMENT:
                return checkDislikeCommentValidation(user, community, post, comment, member, relation, token, authToken,
                        data);
            case PIN_COMMENT:
                return checkPinCommentValidation(user, community, post, comment, member, token, authToken, data);
            default:
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal server error during comments validations.").build();
        }
    }

    private static Response checkAddCommentValidation(Entity user, Entity community, Entity post, Entity parentComment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        String operation = "Add comment: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not comment on post.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not comment on post.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (post.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "post is locked, user may not add comment.");
                return Response.status(Status.FORBIDDEN).entity("Post is locked, user may not add comment.").build();
            }
            if (data.parentID != null && !data.parentID.trim().isEmpty()
                    && validateComment(operation, parentComment).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Parent comment is not registered.").build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to add comments.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to add comments.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            var code = data.isValidToComment(authToken.username);
            if (code < 1) {
                LOG.fine(operation + "invalid data for commenting, " + data.getInvalidReason(code) + ".");
                return Response.status(Status.BAD_REQUEST)
                        .entity("Invalid data for commenting, " + data.getInvalidReason(code) + ".").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkEditCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        String operation = "Edit comment: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not edit comment.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not edit comment.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (post.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "post is locked, user may not edit comment.");
                return Response.status(Status.FORBIDDEN).entity("Post is locked, user may not edit comment.").build();
            }
            if (validateComment(operation, comment).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Comment is not registered.").build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
                if (!comment.getString("username").equals(user.getString("username"))) {
                    LOG.info(operation + "user is not authorized to edit this comment.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to edit this comment.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GC) || authToken.role.equals(ServerConstants.GBO)
                    || authToken.role.equals(ServerConstants.GA) || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GC/GBO/GA/GS/SU users are not authorized to edit comments.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to edit comments.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemoveCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        String operation = "Remove comment: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not remove comment.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not remove comment.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (validateComment(operation, comment).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Comment is not registered.").build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
                if (!comment.getString("username").equals(user.getString("username"))
                        && !member.getBoolean("isManager")) {
                    LOG.info(operation + "user is not authorized to remove this comment.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to remove this comment.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO)
                    || authToken.role.equals(ServerConstants.GA) || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to remove comments.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to remove comments.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkLikeCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity likeRelation, Entity token, AuthToken authToken, CommentData data) {
        String operation = "Like comment: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not like comment.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not like comment.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (validateComment(operation, comment).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Comment is not registered.").build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO)
                    || authToken.role.equals(ServerConstants.GA) || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to like comments.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to like comments.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isLiked && likeRelation != null) {
                LOG.info(operation + "user already liked comment.");
                return Response.status(Status.CONFLICT).entity("User already liked comment.").build();
            } else if (!data.isLiked && likeRelation == null) {
                LOG.info(operation + "user hasn't liked the comment.");
                return Response.status(Status.CONFLICT).entity("User hasn't liked the comment.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkDislikeCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity dislikeRelation, Entity token, AuthToken authToken, CommentData data) {
        String operation = "Dislike comment: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not dislike comment.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not dislike comment.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (validateComment(operation, comment).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Comment is not registered.").build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO)
                    || authToken.role.equals(ServerConstants.GA) || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to dislike comments.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to dislike comments.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isDisliked && dislikeRelation != null) {
                LOG.info(operation + "user already disliked comment.");
                return Response.status(Status.CONFLICT).entity("User already disliked comment.").build();
            } else if (!data.isDisliked && dislikeRelation == null) {
                LOG.info(operation + "user hasn't disliked the comment.");
                return Response.status(Status.CONFLICT).entity("User hasn't disliked the comment.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkPinCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        String operation = "Pin comment: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not pin comment.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not pin comment.")
                        .build();
            }
            if (validatePost(operation, post).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Post is not registered.")
                        .build();
            }
            if (post.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "post is locked, user may not pin comment.");
                return Response.status(Status.FORBIDDEN).entity("Post is locked, user may not pin comment.").build();
            }
            if (validateComment(operation, comment).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Comment is not registered.").build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
                if (!post.getString("username").equals(authToken.username)
                        && !member.getBoolean("isManager")) {
                    LOG.info(operation + "user is not authorized to pin this comment.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to pin this comment.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO)
                    || authToken.role.equals(ServerConstants.GA) || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to pin comments.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to pin comments.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isPinned && comment.getBoolean("isPinned")) {
                LOG.info(operation + "comment is already pinned.");
                return Response.status(Status.CONFLICT).entity("Comment is already pinned.").build();
            } else if (!data.isPinned && !comment.getBoolean("isPinned")) {
                LOG.info(operation + "comment is not pinned.");
                return Response.status(Status.CONFLICT).entity("Comment is not pinned.").build();
            }
            return Response.ok().build();
        }
    }

    /************** Threads Validations *******************/

    public static Response checkThreadsValidations(int operation, Entity user, Entity community, Entity thread,
            Entity member,
            Entity token, AuthToken authToken) {
        return checkThreadsValidations(operation, user, community, thread, member, token, authToken, null);
    }

    public static Response checkThreadsValidations(int operation, Entity user, Entity community, Entity thread,
            Entity reply, Entity member, Entity token, AuthToken authToken) {
        return checkThreadsValidations(operation, user, community, thread, reply, member, token, authToken, null);
    }

    public static <T> Response checkThreadsValidations(int operation, Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken, T data) {
        return checkThreadsValidations(operation, user, community, null, member, token, authToken, data);
    }

    public static <T> Response checkThreadsValidations(int operation, Entity user, Entity community, Entity thread,
            Entity member, Entity token, AuthToken authToken, T data) {
        return checkThreadsValidations(operation, user, community, thread, null, member, token, authToken, data);
    }

    public static <T> Response checkThreadsValidations(int operation, Entity user, Entity community, Entity thread,
            Entity reply, Entity member, Entity token, AuthToken authToken, T data) {
        return checkThreadsValidations(operation, user, community, thread, reply, null, member, token, authToken, data);
    }

    public static <T> Response checkThreadsValidations(int operation, Entity user, Entity community, Entity thread,
            Entity reply, Entity threadmarkOrLikeRelation, Entity member, Entity token, AuthToken authToken, T data) {
        switch (operation) {
            case START_THREAD:
                return checkStartThreadValidation(user, community, member, token, authToken, (ThreadData) data);
            case LOCK_THREAD:
                return checkLockThreadValidation(user, community, thread, member, token, authToken, (boolean) data);
            case PIN_THREAD:
                return checkPinThreadValidation(user, community, thread, member, token, authToken, (boolean) data);
            case REMOVE_THREAD:
                return checkRemoveThreadValidation(user, community, thread, member, token, authToken);
            case ADD_THREAD_TAGS:
                return checkAddThreadTagsValidation(user, community, thread, member, token, authToken,
                        (ThreadData) data);
            case POST_THREAD_REPLY:
                return checkReplyToThreadValidation(user, community, thread, member, token, authToken,
                        (ReplyData) data);
            case EDIT_THREAD_REPLY:
                return checkEditReplyValidation(user, community, thread, reply, member, token, authToken,
                        (ReplyData) data);
            case ADD_THREADMARK:
                return checkThreadmarkReplyValidation(user, community, thread, reply, threadmarkOrLikeRelation, member,
                        token, authToken, (ThreadmarkData) data);
            case REMOVE_THREAD_REPLY:
                return checkRemoveReplyValidation(user, community, thread, reply, member, token, authToken);
            case LIKE_THREAD_REPLY:
                return checkLikeReplyValidation(user, community, thread, reply, threadmarkOrLikeRelation, member, token,
                        authToken, (boolean) data);
            case REPORT_THREAD_REPLY:
                return Response.status(Status.NOT_IMPLEMENTED).build();
            case GET_THREAD:
                return checkGetThreadValidation(user, community, thread, member, token, authToken);
            default:
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static Response checkStartThreadValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken, ThreadData data) {
        String operation = "Start thread: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not start thread.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not start thread.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to start threads.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to start threads.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            var code = data.isValidToStartThread(authToken.username);
            if (code < 1) {
                LOG.fine(operation + "invalid data for starting thread.");
                return Response.status(Status.BAD_REQUEST)
                        .entity("Invalid data for starting thread.").build();
            }
            if ((data.isLocked || data.isPinned) && !member.getBoolean("isManager")
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkLockThreadValidation(Entity user, Entity community, Entity thread, Entity member,
            Entity token, AuthToken authToken, boolean isLocked) {
        String operation = "Lock thread: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not lock thread.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not lock/unlock thread.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not unlock thread.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not unlock thread.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                } else if (!member.getBoolean("isManager")
                        && !authToken.username.equals(thread.getString("username"))) {
                    LOG.info(operation + "user is not a manager of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to lock threads.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to lock threads.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if ((thread.getBoolean("isLocked") && isLocked) || (!thread.getBoolean("isLocked") && !isLocked)) {
                LOG.info(operation + "no change to the thread lock status.");
                return Response.status(Status.BAD_REQUEST).entity("No change to the thread lock status.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkPinThreadValidation(Entity user, Entity community, Entity thread, Entity member,
            Entity token, AuthToken authToken, boolean isPinned) {
        String operation = "Pin thread: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not pin thread.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not pin thread.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not pin thread.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not pin thread.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                } else if (!member.getBoolean("isManager")) {
                    LOG.info(operation + "user is not a manager of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to pin threads.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to pin threads.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if ((thread.getBoolean("isPinned") && isPinned) || (!thread.getBoolean("isPinned") && !isPinned)) {
                LOG.info(operation + "no change to the thread pin status.");
                return Response.status(Status.BAD_REQUEST).entity("No change to the thread pin status.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemoveThreadValidation(Entity user, Entity community, Entity thread, Entity member,
            Entity token, AuthToken authToken) {
        String operation = "Remove thread: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not remove thread.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not remove thread.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not remove thread.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not remove thread.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                } else if (!member.getBoolean("isManager")
                        && !authToken.username.equals(thread.getString("username"))) {
                    LOG.info(operation + "user is not a manager of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to remove threads.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to remove threads.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkAddThreadTagsValidation(Entity user, Entity community, Entity thread, Entity member,
            Entity token, AuthToken authToken, ThreadData data) {
        String operation = "Tag thread: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not tag thread.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not tag thread.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not tag thread.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not tag thread.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                } else if (!member.getBoolean("isManager")
                        && !authToken.username.equals(thread.getString("username"))) {
                    LOG.info(operation + "user is not a manager of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to tag threads.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to tag threads.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (!data.isValidTagging(thread)) {
                LOG.info(operation + "Invalid tagging.");
                return Response.status(Status.FORBIDDEN).entity("Invalid tagging.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkReplyToThreadValidation(Entity user, Entity community, Entity thread, Entity member,
            Entity token, AuthToken authToken, ReplyData data) {
        String operation = "Post reply: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not post replies.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not post replies.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not post replies.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not post replies.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to post replies.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to post replies.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isValidReply(authToken.username) < 1) {
                LOG.info(operation + "Invalid reply data.");
                return Response.status(Status.FORBIDDEN).entity("Invalid reply data.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkEditReplyValidation(Entity user, Entity community, Entity thread, Entity reply,
            Entity member, Entity token, AuthToken authToken, ReplyData data) {
        String operation = "Edit reply: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not edit reply.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not edit reply.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not edit reply.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not edit reply.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)
                    || authToken.role.equals(ServerConstants.GC)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                } else if (!member.getBoolean("isManager")
                        && !authToken.username.equals(thread.getString("username"))) {
                    LOG.info(operation + "user is not a manager of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to edit replies.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to edit replies.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isValidReply(authToken.username) < 1) {
                LOG.info(operation + "Invalid reply data.");
                return Response.status(Status.FORBIDDEN).entity("Invalid reply data.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkThreadmarkReplyValidation(Entity user, Entity community, Entity thread, Entity reply,
            Entity previousThreadmark, Entity member, Entity token, AuthToken authToken, ThreadmarkData data) {
        String operation = "Add threadmark: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not threadmark reply.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not threadmark reply.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not threadmark reply.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not threadmark reply.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)
                    || authToken.role.equals(ServerConstants.GC)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                } else if (!member.getBoolean("isManager")
                        && !authToken.username.equals(thread.getString("username"))) {
                    LOG.info(operation + "user is not a manager of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to threadmark replies.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to threadmark replies.")
                        .build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (data.isValidThreadmark(previousThreadmark)) {
                LOG.info(operation + "Invalid threadmark data.");
                return Response.status(Status.FORBIDDEN).entity("Invalid threadmark data.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemoveReplyValidation(Entity user, Entity community, Entity thread, Entity reply,
            Entity member, Entity token, AuthToken authToken) {
        String operation = "Remove reply: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not remove reply.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not remove reply.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not remove reply.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not remove reply.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                } else if (!member.getBoolean("isManager")
                        && !authToken.username.equals(thread.getString("username"))) {
                    LOG.info(operation + "user is not a manager of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to remove replies.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to remove replies.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkLikeReplyValidation(Entity user, Entity community, Entity thread, Entity reply,
            Entity likeRelation, Entity member, Entity token, AuthToken authToken, boolean isLiked) {
        String operation = "Like reply: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may not like reply.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not like reply.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not like reply.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not like reply.")
                        .build();
            }
            if (authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP)) {
                if (member == null) {
                    LOG.info(operation + "user is not a member of the community.");
                    return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GBO/GA/GS/SU users are not authorized to like replies.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to like replies.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (!isLiked && likeRelation == null) {
                LOG.info(operation + "user tried to unlike a reply they have not liked.");
                return Response.status(Status.BAD_REQUEST).entity("User tried to unlike a reply they have not liked.")
                        .build();
            } else if (isLiked && likeRelation != null) {
                LOG.info(operation + "user tried to like a reply they have liked.");
                return Response.status(Status.BAD_REQUEST).entity("User tried to like a reply they have liked.")
                        .build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkGetThreadValidation(Entity user, Entity community, Entity thread, Entity member,
            Entity token, AuthToken authToken) {
        String operation = "Get thread: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Community is not registered.")
                        .build();
            }
            if (community.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "community is locked, user may view threads.");
                return Response.status(Status.FORBIDDEN).entity("Community is locked, user may not view threads.")
                        .build();
            }
            if (validateThread(operation, thread).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity("Thread is not registered.")
                        .build();
            }
            if (thread.getBoolean("isLocked") && (member == null || !member.getBoolean("isManager"))
                    && !authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "thread is locked, user may not view thread.");
                return Response.status(Status.FORBIDDEN).entity("Thread is locked, user may not view thread.")
                        .build();
            }
            return Response.ok().build();
        }
    }

    /**********************************************************************
     * Private methods
     **********************************************************************/

    private static Response validateUser(String operation, Entity user, String username) {
        if (user == null) {
            LOG.warning(operation + username + " is not a registered user.");
            return Response.status(Status.NOT_FOUND).entity(username + " is not a registered user.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response validateCommunity(String operation, Entity community) {
        if (community == null) {
            LOG.warning(operation + " not a registered community.");
            return Response.status(Status.NOT_FOUND).entity("Community is not registered.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response validatePost(String operation, Entity post) {
        if (post == null) {
            LOG.warning(operation + " is not a registered post.");
            return Response.status(Status.NOT_FOUND).entity("Post is not registered.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response validateComment(String operation, Entity comment) {
        if (comment == null) {
            LOG.warning(operation + " is not a registered comment.");
            return Response.status(Status.NOT_FOUND).entity("Comment is not registered.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response validateThread(String operation, Entity thread) {
        if (thread == null) {
            LOG.warning(operation + " is not a registered thread.");
            return Response.status(Status.NOT_FOUND).entity("Thread is not registered.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response validateToken(String operation, Entity user, Entity token, AuthToken authToken) {
        int validation = authToken.isStillValid(token, user.getString("role"));
        if (validation == 1) {
            return Response.ok().build();
        } else if (validation == 0) { // authToken time has run out
            LOG.fine(operation + authToken.username + "'s' authentication token expired.");
            return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
        } else if (validation == -1) { // Role is different
            LOG.warning(operation + authToken.username + "'s' authentication token has different role.");
            return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
        } else if (validation == -2) { // authToken is false
            LOG.severe(operation + authToken.username
                    + "'s' authentication token is different, possible attempted breach.");
            return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
        } else {
            LOG.severe(operation + authToken.username + "'s' authentication token validity error.");
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}