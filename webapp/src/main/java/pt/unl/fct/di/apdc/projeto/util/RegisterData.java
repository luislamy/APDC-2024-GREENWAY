package pt.unl.fct.di.apdc.projeto.util;

public class RegisterData {

	public String username;

	public String password;
	
	public String confirmation;
	
	public String email;
	
	public String name;

	public String phone;

    public String profile;

    public String work;

    public String workplace;

    public String address;

    public String postalcode;

    public String fiscal;

    public String role;

    public String state;

	public String photo;
	
	public RegisterData() {
		
	}
	
	public RegisterData(String username, String password, String confirmation, String email, String name, String phone, String profile, 
						String work, String workplace, String address, String postalcode, String fiscal, String role, String state, String photo) {
		this.username = username;
		this.password = password;
		this.confirmation = confirmation;
		this.email = email;
		this.name = name;
		this.phone = phone;
        this.profile = profile;
        this.work = work;
        this.workplace = workplace;
        this.address = address;
        this.postalcode = postalcode;
        this.fiscal = fiscal;
        this.role = role;
        this.state = state;
		this.photo = photo;
	}
	
	/**
	 * Method to check if the data is valid for registry.
	 * @return 	-8 if the username is invalid;
	 * 			-7 if the phone is invalid;
	 * 			-6 if the email is invalid;
	 * 			-5 if the name is invalid;
	 * 			-4 if the password is invalid;
	 * 			-3 if the profile is invalid;
	 * 			-2 if the postal code is invalid;
	 * 			-1 if the fiscal number is invalid;
	 * 			0 if everything is valid.
	 */
	public int validRegistration() {
		if ( this.username == null || this.username.trim().isEmpty() ) {
			return -8;
		} else if ( this.phone == null || this.phone.trim().isEmpty() ) {
			return -7;
		} else if ( this.invalidEmail() ) {
			return -6;
		} else if ( this.invalidName() ) {
			return -5;
		} else if ( this.invalidPassword() ) {
			return -4;
		}
		if ( this.profile != null && !this.profile.isEmpty() ) {
			if ( !this.profile.equals("PUBLIC") && !this.profile.equals("PRIVATE") ) {
				return -3;
			}
		}
		if ( this.postalcode != null && !this.postalcode.isEmpty() ) {
			String[] format = this.postalcode.split("-");
			if ( format.length != 2 || format[0].length() != 4 || format[1].length() != 3 )
				return -2;
		}
		if ( this.fiscal != null && !this.fiscal.isEmpty() ) {
			if ( this.fiscal.length() != 9 )
				return -1;
		}
		return 0;
	}

	/**
	 * Method to check if the email provided is invalid.
	 * Email is invalid if it's null, has no @ or more than 1 @ or if it has no domain.
	 * @return true if the email is invalid, false otherwise.
	 */
	protected boolean invalidEmail() {
		if (this.email == null || this.email.trim().isEmpty()) {
			return true;
		}
		String[] parts = this.email.split("@");
		if (parts.length != 2) {
			return true;
		}
		String domain = parts[1];
		String[] domainParts = domain.split("\\.");
		if (domainParts.length < 2) {
			return true;
		}
		for (String part : domainParts) {
			if (part.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Method to check if the name provided is invalid.
	 * Name is invalid if the user provides only one name, or if it's null.
	 * @return true if the name is invalid, false otherwise.
	 */
	protected boolean invalidName() {
		if ( this.name == null )
			return true;
		String[] name = this.name.split(" ");
		if ( name.length < 2 )
			return true;
		return false;
	}

	/**
	 * Method to check if the password is invalid.
	 * Password is invalid if it's null, the confirmation password is null, the password and confirmation don't match,
	 * the password has fewer than 10 characters, has only lower case, only upper case characters or has fewer than 4 numbers.
	 * @return true if the password is invalid, false otherwise.
	 */
	protected boolean invalidPassword() {
		if ( this.password == null || this.confirmation == null || !this.password.equals(this.confirmation) )
			return true;
		if ( this.password.length() < 10 || 
			this.password.equals(this.password.toLowerCase()) || 
			this.password.equals(this.password.toUpperCase()) ||
			this.invalidPasswordNumbers() )
			return true;
		return false;
	}

	/**
	 * Method to check if the password has less than 4 numbers.
	 * @return true if the password has less than 4 numbers, false otherwise.
	 */
	protected boolean invalidPasswordNumbers() {
		String password = this.password;
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

	/**
	 * Method to get the reason for the register data being invalid.
	 * @param code the error code.
	 * @return the message with the reason for the register data being invalid.
	 */
    public String getInvalidReason(int code) {
		switch(code) {
			case -8: return "username";
			case -7: return "phone";
			case -6: return "email";
			case -5: return "name";
			case -4: return "password";
			case -3: return "profile";
			case -2: return "postal code";
			case -1: return "fiscal number";
			default: return "unknown system error";
		}
	}
}