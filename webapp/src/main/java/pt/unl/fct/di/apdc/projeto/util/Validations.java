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
    LOAD_CONVERSATION = 13;


    public static Response checkValidation(int operation, Entity user, LoginData data) {
        return checkValidation(operation, user, null, null, null, data);
    }


    public static Response checkValidation(int operation, Entity user, Entity authToken, AuthToken token) {
        return checkValidation(operation, user, null, authToken, token, null);
    }


    public static <T> Response checkValidation(int operation, Entity user, Entity authToken, AuthToken token, T data) {
        return checkValidation(operation, user, null, authToken, token, data);
    }


    public static <T> Response checkValidation(int operation, Entity admin, Entity user, Entity authToken, AuthToken token, T data) {
        if ( operation == LOGIN )
            return checkLoginValidation(admin, (LoginData) data);
        else if ( operation == GET_TOKEN )
            return checkGetTokenValidation(admin, authToken, token);
        else if ( operation == LOGOUT )
            return checkLogoutValidation(admin, authToken, token);
        else if ( operation == LIST_USERS )
            return checkListUsersValidation(admin, authToken, token);
        else if ( operation == CHANGE_USER_DATA )
            return checkChangeUserDataValidation(admin, user, authToken, token, (ChangeData) data);
        else if ( operation == CHANGE_PASSWORD )
            return checkChangePasswordValidation(admin, authToken, token, (PasswordData) data);
        else if ( operation == CHANGE_USER_ROLE )
            return checkChangeUserRoleValidation(admin, user, authToken, token, (RoleData) data);
        else if ( operation == CHANGE_USER_STATE )
            return checkChangeUserStateValidation(admin, user, authToken, token, (UsernameData) data);
        else if ( operation == REMOVE_USER )
            return checkRemoveUserValidation(admin, user, authToken, token, (UsernameData) data);
        else if ( operation == SEARCH_USER )
            return checkSearchUserValidation(admin, user, authToken, token, (UsernameData) data);
        else if ( operation == USER_PROFILE )
            return checkUserProfileValidation(admin, authToken, token);
        else if ( operation == SEND_MESSAGE )
            return checkSendMessageValidation(admin, user, authToken, token, (MessageClass) data);
        else if ( operation == RECEIVE_MESSAGES )
            return checkReceiveMessagesValidation(admin, authToken, token);
        else if ( operation == LOAD_CONVERSATION )
            return checkLoadConversationValidation(admin, user, authToken, token, (ConversationClass) data);
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
            return Response.status(Status.UNAUTHORIZED).entity("User's account is inactive.").build();
        }
        String hashedPassword = (String) user.getString("password");
        if ( !hashedPassword.equals(DigestUtils.sha3_512Hex(data.password)) ) {
            LOG.warning(operation + data.username + " provided wrong password.");
            return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response checkGetTokenValidation(Entity user, Entity authToken, AuthToken token) {
        String operation = "Token: ";
        if ( validateUser(operation, user, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, authToken, token);
    }

    private static Response checkLogoutValidation(Entity user, Entity authToken, AuthToken token) {
        String operation = "Logout: ";
        if ( validateUser(operation, user, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, authToken, token);
    }

    private static Response checkListUsersValidation(Entity user, Entity authToken, AuthToken token) {
        String operation = "List users: ";
        if ( validateUser(operation, user, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, authToken, token);
    }

    private static Response checkChangeUserDataValidation(Entity admin, Entity user, Entity authToken, AuthToken token, ChangeData data) {
        String operation = "Data change: ";
        if ( token.role.equals(ServerConstants.USER) && !data.username.equals(token.username) ) {
            LOG.warning(operation + token.username + " cannot change other user's data.");
            return Response.status(Status.UNAUTHORIZED).entity("User role cannot change other users data.").build();
        }
        int validData = data.validData();
		if ( validData != 0 ) {
			LOG.warning(operation + "data change attempt using invalid " + data.getInvalidReason(validData) + ".");
			return Response.status(Status.BAD_REQUEST).entity("Invalid " + data.getInvalidReason(validData) + ".").build();
		}
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, admin, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, authToken, token);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            if ( adminRole.equals(ServerConstants.USER) ) {
                if ( data.password != null && data.password.trim().isEmpty() && 
                    data.email != null && data.email.trim().isEmpty() && 
                    data.name != null && data.name.trim().isEmpty() )
                    return Response.ok().build();
                else
                    return Response.status(Status.BAD_REQUEST).entity("User cannot change password, email or name.").build();
            } else if ( adminRole.equals(ServerConstants.GBO) ) {
                if ( !user.getString("role").equals(ServerConstants.USER) ) {
                    LOG.warning(operation + token.username + " cannot change non USER users data.");
                    return Response.status(Status.UNAUTHORIZED).entity("GBO users cannot change data of non USER users.").build();
                }
                if ( data.role != null && !data.role.trim().isEmpty() ) {
                    LOG.warning(operation + token.username + " cannot change users' role.");
                    return Response.status(Status.UNAUTHORIZED).entity("GBO users cannot change users' role.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GA) ) {
                if ( !user.getString("role").equals(ServerConstants.USER) && !user.getString("role").equals(ServerConstants.GBO) ) {
                    LOG.warning(operation + token.username + " cannot change GA or SU users data.");
                    return Response.status(Status.UNAUTHORIZED).entity("GA users cannot change data of GA or SU users.").build();
                } else if ( data.role.equals(ServerConstants.GA) || data.role.equals(ServerConstants.SU) ) {
                    LOG.warning(operation + token.username + " cannot change users' role to GA or SU roles.");
                    return Response.status(Status.UNAUTHORIZED).entity("GA users cannot change users' role to GA or SU roles.").build();
                }
            } else if ( adminRole.equals(ServerConstants.SU) ) {
                if ( !user.getString("role").equals(ServerConstants.USER) && !user.getString("role").equals(ServerConstants.GBO) && !user.getString("role").equals(ServerConstants.GA) ) {
                    LOG.warning(operation + token.username + " cannot change SU users data.");
                    return Response.status(Status.UNAUTHORIZED).entity("SU users cannot change data of SU users.").build();
                }
            } else {
                LOG.severe(operation +  "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkChangePasswordValidation(Entity user, Entity authToken, AuthToken token, PasswordData data) {
        String operation = "Password change: ";
        if ( !data.validPasswordData() ) {
			LOG.warning(operation + "password change attempt using missing or invalid parameters.");
			return Response.status(Status.BAD_REQUEST).entity("Missing or invalid parameter.").build();
        }
        if ( validateUser(operation, user, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, user, authToken, token);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String hashedPassword = (String) user.getString("password");
            if ( !hashedPassword.equals(DigestUtils.sha3_512Hex(data.oldPassword)) ) {
                LOG.warning(operation + token.username + " provided wrong password.");
                return Response.status(Status.UNAUTHORIZED).entity("Wrong password.").build();
            } else {
                return Response.ok().build();
            }
        }
    }

    private static Response checkChangeUserRoleValidation(Entity admin, Entity user, Entity authToken, AuthToken token, RoleData data) {
        String operation = "Role change: ";
        if ( token.role.equals(ServerConstants.USER) || token.role.equals(ServerConstants.GBO) || 
            ( token.role.equals(ServerConstants.GA) && (data.role.equals(ServerConstants.GA) || data.role.equals(ServerConstants.SU) ) ) ) {
            LOG.warning(operation + "unauthorized attempt to change the role of a user.");
            return Response.status(Status.UNAUTHORIZED).entity("User is not authorized to change user accounts role.").build();
        }
        if ( validateUser(operation, admin, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        if ( user.getString("role").equals(data.role) ) {
            LOG.fine("Role change: User already has the same role.");
            return Response.status(Status.NOT_MODIFIED).entity("User already had the same role, role remains unchanged.").build();
        }
        return validateToken(operation, admin, authToken, token);
    }

    private static Response checkChangeUserStateValidation(Entity admin, Entity user, Entity authToken, AuthToken token, UsernameData data) {
        String operation = "State change: ";
        if ( token.role.equals(ServerConstants.USER) ) {
            LOG.warning(operation + "unauthorized attempt to change the state of a user.");
            return Response.status(Status.UNAUTHORIZED).entity("USER roles cannot change any user states.").build();
        }
        if ( validateUser(operation, admin, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, authToken, token);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            String userRole = user.getString("role");
            if ( adminRole.equals(ServerConstants.GBO) ) {
                if ( !userRole.equals(ServerConstants.USER) ) {
                    // GBO users can only change USER states
                    LOG.warning(operation + token.username + " attmepted to change the state of a non USER role.");
                    return Response.status(Status.UNAUTHORIZED).entity("GBO users cannot change non USER roles' states.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GA) ) {
                if ( !userRole.equals(ServerConstants.USER) && !userRole.equals(ServerConstants.GBO) ) {
                    // GA users can change USER and GBO states
                    LOG.warning(operation + token.username + " attmepted to change the state of a non USER or GBO role.");
                    return Response.status(Status.UNAUTHORIZED).entity("GA users cannot change non USER and GBO roles' states.").build();
                }
            } else if ( adminRole.equals(ServerConstants.SU) ) {
            } else if ( adminRole.equals(ServerConstants.USER) ) {
                LOG.warning(operation + token.username + " attmepted to change the state of a user as a USER role.");
                return Response.status(Status.UNAUTHORIZED).entity("USER users cannot change states.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkRemoveUserValidation(Entity admin, Entity user, Entity authToken, AuthToken token, UsernameData data) {
        String operation = "Remove user: ";
        if ( token.role.equals(ServerConstants.GBO) ) {
            LOG.warning(operation + "GBO users cannot remove any accounts.");
            return Response.status(Status.UNAUTHORIZED).entity("GBO users cannot remove any accounts.").build();
        } else if ( token.role.equals(ServerConstants.USER) && !token.username.equals(data.username) ) {
            LOG.warning(operation + "USER users cannot remove any accounts other than their own.");
            return Response.status(Status.UNAUTHORIZED).entity("USER users cannot remove any accounts other than their own.").build();
        }
        if ( validateUser(operation, admin, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, authToken, token);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            String role = user.getString("role");
            if ( adminRole.equals(ServerConstants.USER) ) {
                if ( !role.equals(ServerConstants.USER) || !user.equals(admin) ) {
                    LOG.warning(operation + token.username + " (USER role) attempted to delete other user.");
                    return Response.status(Status.UNAUTHORIZED).entity("USER roles cannot remove other users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GA) ) {
                if ( !role.equals(ServerConstants.GBO) && !role.equals(ServerConstants.USER) ) {
                    LOG.warning(operation + token.username + " (GA role) attempted to delete SU or GA user.");
                    return Response.status(Status.UNAUTHORIZED).entity("GA roles cannot remove GA or SU users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.SU) ) {
            } else if ( adminRole.equals(ServerConstants.GBO) ) {
                LOG.warning(operation + token.username + " (GBO role) attempted to delete user.");
                return Response.status(Status.UNAUTHORIZED).entity("GBO roles cannot remove users from the database.").build();
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkSearchUserValidation(Entity admin, Entity user, Entity authToken, AuthToken token, UsernameData data) {
        String operation = "Search user: ";
        if ( validateUser(operation, admin, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        if ( validateUser(operation, user, data.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.username + " is not a registered user.").build();
        }
        Response validateTokenResponse = validateToken(operation, admin, authToken, token);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String adminRole = admin.getString("role");
            String role = user.getString("role");
            String state = user.getString("state");
            String profile = user.getString("profile");
            if ( adminRole.equals(ServerConstants.USER) ) {
                if ( !role.equals(ServerConstants.USER) || !state.equals(ServerConstants.ACTIVE) || !profile.equals(ServerConstants.PUBLIC) ) {
                    LOG.warning(operation + token.username + " (USER role) attempted to search non USER, inactive or private users' information.");
                    return Response.status(Status.UNAUTHORIZED).entity("USER roles cannot search for other non USER, inactive or private users' from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GBO) ) {
                if ( !role.equals(ServerConstants.GBO) && !role.equals(ServerConstants.USER) ) {
                    LOG.warning(operation + token.username + " (GBO role) attempted to search higher user.");
                    return Response.status(Status.UNAUTHORIZED).entity("GBO roles cannot search for higher users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.GA) ) {
                if ( role.equals(ServerConstants.SU) ) {
                    LOG.warning(operation + token.username + " (GA role) attempted to search higher user.");
                    return Response.status(Status.UNAUTHORIZED).entity("GA roles cannot search for higher users from the database.").build();
                }
            } else if ( adminRole.equals(ServerConstants.SU) ) {
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkUserProfileValidation(Entity user, Entity authToken, AuthToken token) {
        String operation = "Get user: ";
        if ( validateUser(operation, user, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, authToken, token);
    }

    private static Response checkSendMessageValidation(Entity sender, Entity receiver, Entity authToken, AuthToken token, MessageClass message) {
        String operation = "Send Message: ";
        if ( validateUser(operation, sender, message.sender).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(message.sender + " is not a registered user.").build();
        }
        if ( validateUser(operation, receiver, message.receiver).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(message.receiver + " is not a registered user.").build();
        }
        if ( receiver.getString("state").equals(ServerConstants.INACTIVE) ) {
            LOG.warning(operation + message.receiver + " is not an active user.");
            return Response.status(Status.UNAUTHORIZED).entity("Receiver's account is inactive.").build();
        }
        Response validateTokenResponse = validateToken(operation, sender, authToken, token);
        if ( validateTokenResponse.getStatus() != Status.OK.getStatusCode() ) {
            return validateTokenResponse;
        } else {
            String senderRole = sender.getString("role");
            String receiverRole = receiver.getString("role");
            if ( senderRole.equals(ServerConstants.USER) ) {
                if ( !receiverRole.equals(ServerConstants.USER) || !receiver.getString("profile").equals(ServerConstants.PUBLIC) ) {
                    LOG.fine(operation + "USER roles cannot send messages to non USER roles.");
                    return Response.status(Status.UNAUTHORIZED).entity("USER roles cannot send messages to non USER roles or users with private profiles.").build();
                }
            } else if ( senderRole.equals(ServerConstants.GBO) ) {
                if ( !receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.GBO) ) {
                    LOG.fine(operation + "GBO roles cannot send messages to higher roles.");
                    return Response.status(Status.UNAUTHORIZED).entity("GBO roles cannot send messages to higher roles.").build();
                }
            } else if ( senderRole.equals(ServerConstants.GA) ) {
                if ( !receiverRole.equals(ServerConstants.USER) && !receiverRole.equals(ServerConstants.GBO) && !receiverRole.equals(ServerConstants.GA) ) {
                    LOG.fine(operation + "GA roles cannot send messages to higher roles.");
                    return Response.status(Status.UNAUTHORIZED).entity("GA roles cannot send messages to higher roles.").build();
                }
            } else if ( senderRole.equals(ServerConstants.SU) ) {
            } else {
                LOG.severe(operation + "Unrecognized role.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
            return Response.ok().build();
        }
    }

    private static Response checkReceiveMessagesValidation(Entity user, Entity authToken, AuthToken token) {
        String operation = "Receive Messages: ";
        if ( validateUser(operation, user, token.username).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(token.username + " is not a registered user.").build();
        }
        return validateToken(operation, user, authToken, token);
    }

    private static Response checkLoadConversationValidation(Entity sender, Entity receiver, Entity authToken, AuthToken token, ConversationClass data) {
        String operation = "Load Conversation: ";
        if ( validateUser(operation, sender, data.sender).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.sender + " is not a registered user.").build();
        }
        if ( validateUser(operation, receiver, data.receiver).getStatus() != Status.OK.getStatusCode() ) {
            return Response.status(Status.NOT_FOUND).entity(data.receiver + " is not a registered user.").build();
        }
        return validateToken(operation, sender, authToken, token);
    }

    private static Response validateUser(String operation, Entity user, String username) {
        if ( user == null ) {
			LOG.warning(operation + username + " is not a registered user.");
			return Response.status(Status.NOT_FOUND).entity(username + " is not a registered user.").build();
        } else {
            return Response.ok().build();
        }
    }

    private static Response validateToken(String operation, Entity user, Entity authToken, AuthToken token) {
        int validation = token.isStillValid(authToken, user.getString("role"));
        if ( validation == 1 ) {
            return Response.ok().build();
        } else if ( validation == 0 ) { // Token time has run out
            LOG.fine(operation + token.username + "'s' authentication token expired.");
            return Response.status(Status.UNAUTHORIZED).entity("Token time limit exceeded, make new login.").build();
        } else if ( validation == -1 ) { // Role is different
            LOG.warning(operation + token.username + "'s' authentication token has different role.");
            return Response.status(Status.UNAUTHORIZED).entity("User role has changed, make new login.").build();
        } else if ( validation == -2 ) { // token is false
            LOG.severe(operation + token.username + "'s' authentication token is different, possible attempted breach.");
            return Response.status(Status.UNAUTHORIZED).entity("Token is incorrect, make new login").build();
        } else {
            LOG.severe(operation + token.username + "'s' authentication token validity error.");
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}