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
    public static final int LOGIN = 0, GET_TOKEN = 1, LOGOUT = 2, LIST_USERS = 3,
            CHANGE_USER_DATA = 4, CHANGE_PASSWORD = 5, CHANGE_USER_ROLE = 6,
            CHANGE_USER_STATE = 7, REMOVE_USER = 8, SEARCH_USER = 9,
            USER_PROFILE = 10, SEND_MESSAGE = 11, RECEIVE_MESSAGES = 12,
            LOAD_CONVERSATION = 13, CREATE_COMMUNITY = 14, GET_COMMUNITIES = 15,
            GET_COMMUNITY = 16, JOIN_COMMUNITY = 17, EDIT_COMMUNITY = 18;

    public static final int ADD_POST = 301, GET_POST = 302, EDIT_POST = 303, REMOVE_POST = 304,
            LOCK_POST = 305, PIN_POST = 306, LIKE_POST = 307, DISLIKE_POST = 308;

    public static final int ADD_COMMENT = 311, EDIT_COMMENT = 312, REMOVE_COMMENT = 313,
            LIKE_COMMENT = 314, DISLIKE_COMMENT = 315, PIN_COMMENT = 316;

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

    public static Response checkGetCommunitiesValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Get communities: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    public static Response checkCreateCommunityValidation(Entity user, Entity token, AuthToken authToken) {
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
                return Response.ok().build();
            } else {
                return Response.status(Status.FORBIDDEN).entity("User is not allowed to create communities").build();
            }
        }
    }

    public static Response checkGetCommunityValidation(Entity user, Entity community, Entity token, AuthToken authToken,
            String communityID) {
        String operation = "Get community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
                        .build();
            }
            return Response.ok().build();
        }
    }

    public static Response checkJoinCommunityValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken, String communityID) {
        String operation = "Join community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.").build();
        }
        if (member != null) {
            LOG.info(operation + "user is already a member of the community.");
            return Response.status(Status.CONFLICT).entity("User is already a member of the community.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            var userRole = user.getString("role");
            if (userRole.equals(ServerConstants.USER) || userRole.equals(ServerConstants.EP)
                    || userRole.equals(ServerConstants.GC)) {
                return Response.ok().build();
            } else {
                return Response.status(Status.FORBIDDEN).entity("User is not allowed to join communities.").build();
            }
        }
    }

    public static Response checkEditCommunityValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken, CommunityData data) {
        String operation = "Edit community: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, data.communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(data.communityID + " is not a registered community.")
                        .build();
            }
            if (member == null) {
                LOG.info(operation + "user is not a member of the community.");
                return Response.status(Status.FORBIDDEN).entity("User is not a member of the community.").build();
            } else if (!member.getBoolean("isManager")) {
                LOG.info(operation + "user is not a manager of the community.");
                return Response.status(Status.FORBIDDEN).entity("User is not a manager of the community.").build();
            } else if (!authToken.role.equals(ServerConstants.GC)) {
                LOG.info(operation + "user is not a community manager.");
                return Response.status(Status.FORBIDDEN).entity("User is not a community manager.").build();
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

    /************************ Posts Validations ********************/

    public static Response checkPostsValidations(int operation, Entity user, Entity community, Entity member,
            Entity token, AuthToken authToken, PostData data, String communityID) {
        return checkPostsValidations(operation, user, community, null, member, token, authToken, data, communityID);
    }

    public static Response checkPostsValidations(int operation, Entity user, Entity community, Entity post,
            Entity member, Entity token, AuthToken authToken, String communityID) {
        return checkPostsValidations(operation, user, community, post, member, token, authToken, null, communityID);
    }

    public static Response checkPostsValidations(int operation, Entity user, Entity community, Entity post,
            Entity member, Entity token, AuthToken authToken, PostData data, String communityID) {
        return checkPostsValidations(operation, user, community, post, member, null, token, authToken, data,
                communityID);
    }

    public static Response checkPostsValidations(int operation, Entity user, Entity community, Entity post,
            Entity member, Entity relation, Entity token, AuthToken authToken,
            PostData data, String communityID) {
        switch (operation) {
            case ADD_POST:
                return checkAddPostValidation(user, community, member, token, authToken, data, communityID);
            case GET_POST:
                return checkGetPostValidation(user, community, post, member, token, authToken, communityID);
            case EDIT_POST:
                return checkEditPostValidation(user, community, post, member, token, authToken, data, communityID);
            case REMOVE_POST:
                return checkRemovePostValidation(user, community, post, member, token, authToken, communityID);
            case LOCK_POST:
                return checkLockPostValidation(user, community, post, member, token, authToken, data, communityID);
            case PIN_POST:
                return checkPinPostValidation(user, community, post, member, token, authToken, data, communityID);
            case LIKE_POST:
                return checkLikePostValidation(user, community, post, member, relation, token,
                        authToken, data,
                        communityID);
            case DISLIKE_POST:
                return checkDislikePostValidation(user, community, post, member, relation, token,
                        authToken, data,
                        communityID);
            default:
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal server error during posts validations.").build();
        }
    }

    private static Response checkAddPostValidation(Entity user, Entity community, Entity member, Entity token,
            AuthToken authToken, PostData data, String communityID) {
        String operation = "Add post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
                        .build();
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
            if (!data.isValidToPost()) {
                LOG.fine(operation + "title or postBody are blank.");
                return Response.status(Status.BAD_REQUEST).entity("Please give the post a title and body.").build();
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
            Entity token, AuthToken authToken, String communityID) {
        String operation = "Get post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
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
            } else if (authToken.role.equals(ServerConstants.GC) || authToken.role.equals(ServerConstants.GBO)
                    || authToken.role.equals(ServerConstants.GA) || authToken.role.equals(ServerConstants.GS)
                    || authToken.role.equals(ServerConstants.SU)) {
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkEditPostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity token, AuthToken authToken, PostData data, String communityID) {
        String operation = "Edit post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
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
                    LOG.info(operation + "user is not authorized to edit this post.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to edit this post.")
                            .build();
                }
            } else if (authToken.role.equals(ServerConstants.GC)) {
            } else if (authToken.role.equals(ServerConstants.GBO) || authToken.role.equals(ServerConstants.GA)
                    || authToken.role.equals(ServerConstants.GS) || authToken.role.equals(ServerConstants.SU)) {
                LOG.info(operation + "GA/GBO users are not authorized to edit posts.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to edit posts.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            if (!data.isValidToPost()) {
                LOG.fine(operation + "title or postBody are blank.");
                return Response.status(Status.BAD_REQUEST).entity("Please give the post a title and body.").build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemovePostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity token, AuthToken authToken, String communityID) {
        String operation = "Remove post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
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
            Entity token, AuthToken authToken, PostData data, String communityID) {
        String operation = "Lock post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
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
            Entity token, AuthToken authToken, PostData data, String communityID) {
        String operation = "Pin post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
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
                if (!member.getBoolean("isManager")) {
                    LOG.info(operation + "user is not authorized to pin posts.");
                    return Response.status(Status.FORBIDDEN).entity("User is not authorized to pin posts.")
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

    private static Response checkLikePostValidation(Entity user, Entity community, Entity post, Entity member,
            Entity likeRelation, Entity token, AuthToken authToken, PostData data, String communityID) {
        String operation = "Like post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
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
            Entity dislikeRelation, Entity token, AuthToken authToken, PostData data, String communityID) {
        String operation = "Dislike post: ";
        if (validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode()) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        var tokenValidation = validateToken(operation, user, token, authToken);
        if (tokenValidation.getStatus() != Status.OK.getStatusCode()) {
            return tokenValidation;
        } else {
            if (validateCommunity(operation, community, communityID).getStatus() != Status.OK.getStatusCode()) {
                return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.")
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

    /************** Comments Validations *******************/

    public static Response checkAddCommentValidation(Entity user, Entity community, Entity post, Entity parentComment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        // TODO: make validation
        return Response.ok().build();
    }

    public static Response checkEditCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        // TODO: make validation
        return Response.ok().build();
    }

    public static Response checkRemoveCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        // TODO: make validation
        return Response.ok().build();
    }

    public static Response checkLikeCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity likeRelation, Entity token, AuthToken authToken, CommentData data) {
        // TODO: make validation
        return Response.ok().build();
    }

    public static Response checkDislikeCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity dislikeRelation, Entity token, AuthToken authToken, CommentData data) {
        // TODO: make validation
        return Response.ok().build();
    }

    public static Response checkPinCommentValidation(Entity user, Entity community, Entity post, Entity comment,
            Entity member, Entity token, AuthToken authToken, CommentData data) {
        // TODO: make validation
        return Response.ok().build();
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

    private static Response validateCommunity(String operation, Entity community, String communityID) {
        if (community == null) {
            LOG.warning(operation + communityID + " is not a registered community.");
            return Response.status(Status.NOT_FOUND).entity(communityID + " is not a registered community.").build();
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