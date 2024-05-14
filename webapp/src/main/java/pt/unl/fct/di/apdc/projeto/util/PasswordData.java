package pt.unl.fct.di.apdc.projeto.util;

public class PasswordData {

    public String oldPassword;
	public String newPassword;
    public String confirmation;
	public AuthToken token;
	
	public PasswordData() {
	}
	
	public PasswordData(String oldPassword, String newPassword, String confirmation, AuthToken token) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
        this.confirmation = confirmation;
		this.token = token;
	}
	
	/**
	 * Method to check if the data is valid for password alteration.
	 * @return true if all the data fields are not null, newPassword obeys the password rules and newPassword is the same as confirmation, false otherwise.
	 */
	public boolean validPasswordData() {
		if ( this.confirmation == null || this.oldPassword == null || this.newPassword == null || this.invalidPassword() ) {
			return false;
		} else {
			return true;
		}
	}

    /**
	 * Method to check if the password is invalid.
	 * Password is invalid if it's null, the confirmation password is null, the password and confirmation don't match,
	 * the password has fewer than 10 characters, has only lower case, only upper case characters or has fewer than 4 numbers.
	 * @return true if the password is invalid, false otherwise.
	 */
	protected boolean invalidPassword() {
		if ( this.newPassword == null || this.confirmation == null || !this.newPassword.equals(this.confirmation) )
			return true;
		if ( this.newPassword.length() < 10 || 
			this.newPassword.equals(this.newPassword.toLowerCase()) || 
			this.newPassword.equals(this.newPassword.toUpperCase()) ||
			this.invalidPasswordNumbers() )
			return true;
		return false;
	}

	/**
	 * Method to check if the password has less than 4 numbers.
	 * @return true if the password has less than 4 numbers, false otherwise.
	 */
	protected boolean invalidPasswordNumbers() {
		String password = this.newPassword;
		int passwordCount = password.length();
		int numberCount = 0;
		for ( int i = 0; i < passwordCount; i++ ) {
			for ( int j = 0; j < 10; j++ ) {
				if ( password.charAt(i) == (char) ('0' + j) ) {
					numberCount++;
					break;
				}
				if ( numberCount > 3 )
					return false;
			}
		}
		return true;
	}
}