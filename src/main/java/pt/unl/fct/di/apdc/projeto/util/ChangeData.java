package pt.unl.fct.di.apdc.projeto.util;

public class ChangeData {
    
    public String username;

	public String password;
	
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

    public AuthToken token;
	
	public ChangeData() {
		
	}
	
	public ChangeData(String username, String password, String email, String name, String phone, String profile, String work, 
                    String workplace, String address, String postalcode, String fiscal, String role, String state, String photo, AuthToken token) {
		this.username = username;
		this.password = password;
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
        this.token = token;
	}

    /**
	 * Method to check if the data is valid for registry.
	 * @return 	-4 if the username is invalid;
	 * 			-3 if the profile is invalid;
	 * 			-2 if the postal code is invalid;
	 * 			-1 if the fiscal number is invalid;
	 * 			0 if everything is valid.
	 */
	public int validData() {
		if ( this.username == null || this.username.trim().isEmpty() ) {
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
	 * Method to get the reason for the register data being invalid.
	 * @param code the error code.
	 * @return the message with the reason for the register data being invalid.
	 */
    public String getInvalidReason(int code) {
		switch(code) {
			case -4: return "username";
			case -3: return "profile";
			case -2: return "postal code";
			case -1: return "fiscal number";
			default: return "unknown system error";
		}
	}
}