package pt.unl.fct.di.apdc.projeto.util;

import java.util.UUID;

import com.google.cloud.datastore.Entity;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000*60*60*2; //2h
	
	public String username;
	public String role;
	public String tokenID;
	public long creationDate;
	public long expirationDate;
	
	public AuthToken() {
		
	}

	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.tokenID = UUID.randomUUID().toString();
		this.creationDate = System.currentTimeMillis();
		this.expirationDate = this.creationDate + AuthToken.EXPIRATION_TIME;
	}

	/**
	 * Method to check if the token is still valid.
	 * @param role the role of the user attempting to use this token.
	 * @return 1 if the token is still valid, 0 if the time has run out, -1 if the given role is different and -2 if the token is different to the one in the database.
	 */
	public int isStillValid(Entity token, String role) {
		if ( token == null )
			return -2;
		String entityUsername = token.getString("username");
		String entityTokenID = token.getString("tokenID");
		String entityRole = token.getString("role");
		long entityCreationDate = token.getLong("creationDate");
		long entityExpirationDate = token.getLong("expirationDate");
		if ( !entityRole.equals(this.role) || !entityUsername.equals(this.username) || !entityTokenID.equals(this.tokenID) || 
				entityCreationDate != this.creationDate || entityExpirationDate != this.expirationDate ) {
			return -2;
		} else if ( !this.role.equals(role) ) {
			return -1;
		} else if ( System.currentTimeMillis() >= this.expirationDate ) {
			return 0;
		} else {
			return 1;
		}
	}
}