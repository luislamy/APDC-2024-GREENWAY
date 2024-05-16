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
    public static int LOGIN = 0, GET_TOKEN = 1, LOGOUT = 2, LIST_USERS = 3, 
    CHANGE_USER_DATA = 4, CHANGE_PASSWORD = 5, CHANGE_USER_ROLE = 6, 
    CHANGE_USER_STATE = 7, REMOVE_USER = 8, SEARCH_USER = 9, 
    USER_PROFILE = 10, SEND_MESSAGE = 11, RECEIVE_MESSAGES = 12, 
    LOAD_CONVERSATION = 13, CREATE_COMMUNITY = 14, GET_COMMUNITIES = 15, 
    GET_COMMUNITY = 16, JOIN_COMMUNITY = 17, EDIT_COMMUNITY = 18, ADD_POST = 19;


    public static <T> Response checkValidation(int operation, Entity user, T data) {
        return checkValidation(operation, user, null, null, null, data);
    }


    public static Response checkValidation(int operation, Entity user, Entity token, AuthToken authToken) {
        return checkValidation(operation, user, null, token, authToken, null);
    }


    public static <T> Response checkValidation(int operation, Entity user, Entity token, AuthToken authToken, T data) {
        return checkValidation(operation, user, null, token, authToken, data);
    }

    public static <T> Response checkValidation(int operation, Entity user, Entity token, Entity community, AuthToken authToken, T data) {
        return checkValidation(operation, user, null, community, token, authToken, data);
    }


    public static <T> Response checkValidation(int operation, Entity admin, Entity user, Entity community, Entity token, AuthToken authToken, T data) {
        if ( operation == LOGIN )
            return checkLoginValidation(admin, (LoginData) data);
        else if ( operation == GET_TOKEN )
            return checkGetTokenValidation(admin, token, authToken);
        else if ( operation == LOGOUT )
            return checkLogoutValidation(admin, token, authToken);
        else if ( operation == LIST_USERS )
            return checkListUsersValidation(admin, token, authToken);
        else if ( operation == CHANGE_USER_DATA )
            return checkChangeUserDataValidation(admin, user, token, authToken, (ChangeData) data);
        else if ( operation == CHANGE_PASSWORD )
            return checkChangePasswordValidation(admin, token, authToken, (PasswordData) data);
        else if ( operation == CHANGE_USER_ROLE )
            return checkChangeUserRoleValidation(admin, user, token, authToken, (RoleData) data);
        else if ( operation == CHANGE_USER_STATE )
            return checkChangeUserStateValidation(admin, user, token, authToken, (UsernameData) data);
        else if ( operation == REMOVE_USER )
            return checkRemoveUserValidation(admin, user, token, authToken, (UsernameData) data);
        else if ( operation == SEARCH_USER )
            return checkSearchUserValidation(admin, user, token, authToken, (UsernameData) data);
        else if ( operation == USER_PROFILE )
            return checkUserProfileValidation(admin, token, authToken);
        else if ( operation == SEND_MESSAGE )
            return checkSendMessageValidation(admin, user, token, authToken, (MessageClass) data);
        else if ( operation == RECEIVE_MESSAGES )
            return checkReceiveMessagesValidation(admin, token, authToken);
        else if ( operation == LOAD_CONVERSATION )
            return checkLoadConversationValidation(admin, user, token, authToken, (ConversationClass) data);
        else
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Internal server validation error.").build();
    }

    private static Response checkLoginValidation(Entity user, LoginData data) {
        String operation = "Login: ";
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        if ( user.getString("state").equals(ServerConstants.INACTIVE) ) {
            LOG.warning(operation + data.username + " not an active user.");
            return Response.status(Status.FORBIDDEN).entity("User's account is inactive.").build();
        }
        String hashedPassword = (String) user.getString("password");
        if ( !hashedPassword.equals(DigestUtils.sha3_512Hex(data.password)) ) {
            LOG.warning(operation + data.username + " provided wrong password.");
            return Response.status(Status.FORBIDDEN).entity("Wrong password.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response checkGetTokenValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Token: ";
        if ( validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkLogoutValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Logout: ";
        if ( validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkListUsersValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "List users: ";
        if ( validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkChangeUserDataValidation(Entity admin, Entity user, Entity token, AuthToken authToken, ChangeData data) {
        String operation = "Data change: ";
        if ( authToken.role.equals(ServerConstants.USER) && !data.username.equals(authToken.username) ) {
            LOG.warning(operation + authToken.username + " cannot change other user's data.");
            return Response.status(Status.FORBIDDEN).entity("User role cannot change other users data.").build();
        }
        int validData = data.validData();
		if ( validData != 0 ) {
			LOG.warning(operation + "data change attempt using invalid " + data.getInvalidReason(validData) + ".");
			return Response.status(Status.BAD_REQUEST).entity("Invalid " + data.getInvalidReason(validData) + ".").build();
		}
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, token, authToken);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            if ( adminRole.equals(ServerConstants.USER) || adminRole.equals(ServerConstants.EP) ) {
                if ( data.password != null && data.password.trim().isEmpty() && 
                    data.email != null && data.email.trim().isEmpty() && 
                    data.name != null && data.name.trim().isEmpty() )
                    return Response.ok().build();
                else
                    return Response.status(Status.BAD_REQUEST).entity("User cannot change password, email or name.").build();
            } else if ( adminRole.equals(ServerConstants.GBO) ) {
                if ( !user.getString("role").equals(ServerConstants.USER) ) {
                    LOG.warning(operation + authToken.username + " cannot change non USER users data.");
                    return Response.status(Status.FORBIDDEN).entity("GBO users cannot change data of non USER users.").build();
                }
                if ( data.role != null && !data.role.trim().isEmpty() ) {
                    LOG.warning(operation + authToken.username + " cannot change users' role.");
                    return Response.status(Status.FORBIDDEN).entity("GBO users cannot change users' role.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GA) ) {
                if ( !user.getString("role").equals(ServerConstants.USER) && !user.getString("role").equals(ServerConstants.GBO) ) {
                    LOG.warning(operation + authToken.username + " cannot change GA or SU users data.");
                    return Response.status(Status.FORBIDDEN).entity("GA users cannot change data of GA or SU users.").build();
                } else if ( data.role.equals(ServerConstants.GA) || data.role.equals(ServerConstants.SU) ) {
                    LOG.warning(operation + authToken.username + " cannot change users' role to GA or SU roles.");
                    return Response.status(Status.FORBIDDEN).entity("GA users cannot change users' role to GA or SU roles.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GS) ) {
                if ( user.getString("role").equals(ServerConstants.SU) ) {
                    LOG.warning(operation + authToken.username + " cannot change SU users data.");
                    return Response.status(Status.FORBIDDEN).entity("GS users cannot change data of SU users.").build();
                }
            } else if ( adminRole.equals(ServerConstants.SU) ) {
                if ( !user.getString("role").equals(ServerConstants.USER) && !user.getString("role").equals(ServerConstants.GBO) && !user.getString("role").equals(ServerConstants.GA) ) {
                    LOG.warning(operation + authToken.username + " cannot change SU users data.");
                    return Response.status(Status.FORBIDDEN).entity("SU users cannot change data of SU users.").build();
                }
            } else {
                LOG.severe(operation +  "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkChangePasswordValidation(Entity user, Entity token, AuthToken authToken, PasswordData data) {
        String operation = "Password change: ";
        if ( !data.validPasswordData() ) {
			LOG.warning(operation + "password change attempt using missing or invalid parameters.");
			return Response.status(Status.BAD_REQUEST).entity("Missing or invalid parameter.").build();
        }
        if ( validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, user, token, authToken);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String hashedPassword = (String) user.getString("password");
            if ( !hashedPassword.equals(DigestUtils.sha3_512Hex(data.oldPassword)) ) {
                LOG.warning(operation + authToken.username + " provided wrong password.");
                return Response.status(Status.FORBIDDEN).entity("Wrong password.").build();
            } else {
                return Response.ok().build();
            }
        }
    }

    private static Response checkChangeUserRoleValidation(Entity admin, Entity user, Entity token, AuthToken authToken, RoleData data) {
        String operation = "Role change: ";
        if ( authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP) || authToken.role.equals(ServerConstants.GBO) ) {
            LOG.warning(operation + "unauthorized attempt to change the role of a user.");
            return Response.status(Status.FORBIDDEN).entity("User is not authorized to change user accounts role.").build();
        } else if ( authToken.role.equals(ServerConstants.GA) && (data.role.equals(ServerConstants.GA) || 
            data.role.equals(ServerConstants.GS) || data.role.equals(ServerConstants.SU) ) ) {
            LOG.warning(operation + "GA users cannot change user's into GA, GS or SU roles.");
            return Response.status(Status.FORBIDDEN).entity("User is not authorized to change user's to GA, GS or SU roles.").build();
        } else if ( authToken.role.equals(ServerConstants.GS) && 
            ( data.role.equals(ServerConstants.GS) || data.role.equals(ServerConstants.SU) ) ) {
                LOG.warning(operation + "GS users cannot change user's into GS or SU roles.");
                return Response.status(Status.FORBIDDEN).entity("User is not authorized to change user's to GS or SU roles.").build();
        }
        if ( validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        if ( user.getString("role").equals(data.role) ) {
            LOG.fine("Role change: User already has the same role.");
            return Response.status(Status.NOT_MODIFIED).entity("User already had the same role, role remains unchanged.").build();
        }
        return validateToken(operation, admin, token, authToken);
    }

    private static Response checkChangeUserStateValidation(Entity admin, Entity user, Entity token, AuthToken authToken, UsernameData data) {
        String operation = "State change: ";
        if ( authToken.role.equals(ServerConstants.USER) || authToken.role.equals(ServerConstants.EP) ) {
            LOG.warning(operation + "unauthorized attempt to change the state of a user.");
            return Response.status(Status.FORBIDDEN).entity("User cannot change state of any user.").build();
        }
        if ( validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, token, authToken);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            String userRole = user.getString("role");
            if ( adminRole.equals(ServerConstants.GBO) ) {
                if ( !userRole.equals(ServerConstants.USER) ) {
                    // GBO users can only change USER states
                    LOG.warning(operation + authToken.username + " attempted to change the state of a non USER role.");
                    return Response.status(Status.FORBIDDEN).entity("GBO users cannot change non USER roles' states.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GA) ) {
                if ( !userRole.equals(ServerConstants.USER) && !userRole.equals(ServerConstants.GBO) ) {
                    // GA users can change USER and GBO states
                    LOG.warning(operation + authToken.username + " attempted to change the state of a non USER or GBO role.");
                    return Response.status(Status.FORBIDDEN).entity("GA users cannot change non USER and GBO roles' states.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GS) ) {
                if ( userRole.equals(ServerConstants.SU) ) {
                    LOG.warning(operation + authToken.username + " attempted to change the state of a SU role.");
                    return Response.status(Status.FORBIDDEN).entity("GS users cannot change SU users' states.").build();
                }
            } else if ( adminRole.equals(ServerConstants.SU) ) {
            } else if ( adminRole.equals(ServerConstants.USER) || adminRole.equals(ServerConstants.EP) ) {
                LOG.warning(operation + authToken.username + " attempted to change the state of a user as a USER or EP role.");
                return Response.status(Status.FORBIDDEN).entity("User cannot change states.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemoveUserValidation(Entity admin, Entity user, Entity token, AuthToken authToken, UsernameData data) {
        String operation = "Remove user: ";
        if ( authToken.role.equals(ServerConstants.GBO) ) {
            LOG.warning(operation + "GBO users cannot remove any accounts.");
            return Response.status(Status.FORBIDDEN).entity("GBO users cannot remove any accounts.").build();
        } else if ( authToken.role.equals(ServerConstants.USER) && !authToken.username.equals(data.username) ) {
            LOG.warning(operation + "USER users cannot remove any accounts other than their own.");
            return Response.status(Status.FORBIDDEN).entity("USER users cannot remove any accounts other than their own.").build();
        } else if ( authToken.role.equals(ServerConstants.EP) && !authToken.username.equals(data.username) ) {
            LOG.warning(operation + "EP users cannot remove any accounts other than their own.");
            return Response.status(Status.FORBIDDEN).entity("EP users cannot remove any accounts other than their own.").build();
        }
        if ( validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, token, authToken);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            String role = user.getString("role");
            if ( adminRole.equals(ServerConstants.USER) ) {
                if ( !role.equals(ServerConstants.USER) || !user.equals(admin) ) {
                    LOG.warning(operation + authToken.username + " (USER role) attempted to delete other user.");
                    return Response.status(Status.FORBIDDEN).entity("USER roles cannot remove other users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.EP) ) {
                if ( !role.equals(ServerConstants.EP) || !user.equals(admin) ) {
                    LOG.warning(operation + authToken.username + " (EP role) attempted to delete other user.");
                    return Response.status(Status.FORBIDDEN).entity("EP roles cannot remove other users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GA) ) {
                if ( !role.equals(ServerConstants.GBO) && !role.equals(ServerConstants.USER) && !role.equals(ServerConstants.EP) ) {
                    LOG.warning(operation + authToken.username + " (GA role) attempted to delete SU, GS or GA user.");
                    return Response.status(Status.FORBIDDEN).entity("GA roles cannot remove GA, GS or SU user from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GS) ) {
                if ( !role.equals(ServerConstants.GBO) && !role.equals(ServerConstants.USER) && 
                    !role.equals(ServerConstants.EP) && !role.equals(ServerConstants.GA) ) {
                    LOG.warning(operation + authToken.username + " (GS role) attempted to delete SU or GS user.");
                    return Response.status(Status.FORBIDDEN).entity("GS roles cannot remove GS or SU users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.SU) ) {
            } else if ( adminRole.equals(ServerConstants.GBO) ) {
                LOG.warning(operation + authToken.username + " (GBO role) attempted to delete user.");
                return Response.status(Status.FORBIDDEN).entity("GBO roles cannot remove users from the database.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkSearchUserValidation(Entity admin, Entity user, Entity token, AuthToken authToken, UsernameData data) {
        String operation = "Search user: ";
        if ( validateUser(operation, admin, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, token, authToken);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            String role = user.getString("role");
            String state = user.getString("state");
            String profile = user.getString("profile");
            if ( adminRole.equals(ServerConstants.USER) || adminRole.equals(ServerConstants.EP) ) {
                if ( ( !role.equals(ServerConstants.USER) && !role.equals(ServerConstants.EP) ) || !state.equals(ServerConstants.ACTIVE) || !profile.equals(ServerConstants.PUBLIC) ) {
                    LOG.warning(operation + authToken.username + " (USER/EP role) attempted to search non USER/EP, inactive or private users' information.");
                    return Response.status(Status.FORBIDDEN).entity("USER/EP roles cannot search for other non USER/EP, inactive or private users' from the database.").build();
                }
            }else if ( adminRole.equals(ServerConstants.GBO) ) {
                if ( !role.equals(ServerConstants.GBO) && !role.equals(ServerConstants.USER) ) {
                    LOG.warning(operation + authToken.username + " (GBO role) attempted to search higher user.");
                    return Response.status(Status.FORBIDDEN).entity("GBO roles cannot search for higher users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GA) ) {
                if ( role.equals(ServerConstants.SU) || role.equals(ServerConstants.GS) ) {
                    LOG.warning(operation + authToken.username + " (GA role) attempted to search higher user.");
                    return Response.status(Status.FORBIDDEN).entity("GA roles cannot search for higher users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GS) ) {
                if ( role.equals(ServerConstants.SU) ) {
                    LOG.warning(operation + authToken.username + " (GS role) attempted to search higher user.");
                    return Response.status(Status.FORBIDDEN).entity("GS roles cannot search for higher users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.SU) ) {
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkUserProfileValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Get user: ";
        if ( validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkSendMessageValidation(Entity sender, Entity receiver, Entity token, AuthToken authToken, MessageClass message) {
        String operation = "Send Message: ";
        if ( validateUser(operation, sender, message.sender).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(message.sender + " is not a registered user.").build();
        }
        if ( validateUser(operation, receiver, message.receiver).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(message.receiver + " is not a registered user.").build();
        }
        if ( receiver.getString("state").equals(ServerConstants.INACTIVE) ) {
            LOG.warning(operation + message.receiver + " is not an active user.");
            return Response.status(Status.FORBIDDEN).entity("Receiver's account is inactive.").build();
        }
        Response validateTokenResponse = validateToken(operation, sender, token, authToken);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String senderRole = sender.getString("role");
            String receiverRole = receiver.getString("role");
            if ( senderRole.equals(ServerConstants.USER) || senderRole.equals(ServerConstants.EP) ) {
                if ( ( !receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.EP) ) || !receiver.getString("profile").equals(ServerConstants.PUBLIC) ) {
                    LOG.fine(operation + "USER/EP roles cannot send messages to non USER/EP roles or private profiles.");
                    return Response.status(Status.FORBIDDEN).entity("USER/EP roles cannot send messages to non USER/EP roles or users with private profiles.").build();
                }
            } else if ( senderRole.equals(ServerConstants.GBO) ) {
                if ( !receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.GBO) ) {
                    LOG.fine(operation + "GBO roles cannot send messages to higher roles.");
                    return Response.status(Status.FORBIDDEN).entity("GBO roles cannot send messages to higher roles.").build();
                }
            } else if ( senderRole.equals(ServerConstants.GA) ) {
                if ( !receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.GBO) && !receiverRole.equals(ServerConstants.GA) ) {
                    LOG.fine(operation + "GA roles cannot send messages to higher roles.");
                    return Response.status(Status.FORBIDDEN).entity("GA roles cannot send messages to higher roles.").build();
                }
            }else if ( senderRole.equals(ServerConstants.SU) || senderRole.equals(ServerConstants.GS) ) {
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkReceiveMessagesValidation(Entity user, Entity token, AuthToken authToken) {
        String operation = "Receive Messages: ";
        if ( validateUser(operation, user, authToken.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(authToken.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, token, authToken);
    }

    private static Response checkLoadConversationValidation(Entity sender, Entity receiver, Entity token, AuthToken authToken, ConversationClass data) {
        String operation = "Load Conversation: ";
        if ( validateUser(operation, sender, data.sender).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.sender + " is not a registered user.").build();
        }
        if ( validateUser(operation, receiver, data.receiver).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.receiver + " is not a registered user.").build();
        }
        return validateToken(operation, sender, token, authToken);
    }

    private static Response validateUser(String operation, Entity user, String username) {
        if ( user == null ) {
			LOG.warning(operation + username + " is not a registered user.");
			return Response.status(Status.NOT_FOUND).entity(username + " is not a registered user.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response validateToken(String operation, Entity user, Entity token, AuthToken authToken) {
        int validation = authToken.isStillValid(token, user.getString("role"));
        if ( validation == 1 ) {
            return Response.ok().build();
        } else if ( validation == 0 ) { // authToken time has run out
            LOG.fine(operation + authToken.username + "'s' authentication token expired.");
            return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
        } else if ( validation == -1 ) { // Role is different
            LOG.warning(operation + authToken.username + "'s' authentication token has different role.");
            return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
        } else if ( validation == -2 ) { // authToken is false
            LOG.severe(operation + authToken.username + "'s' authentication token is different, possible attempted breach.");
            return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
        } else {
            LOG.severe(operation + authToken.username + "'s' authentication token validity error.");
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}