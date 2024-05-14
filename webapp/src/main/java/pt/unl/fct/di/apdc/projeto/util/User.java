package pt.unl.fct.di.apdc.projeto.util;

import java.util.Date;

public class User {
    
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

    public Date userCreationTime;

    public String photo;


    public User() {

    }

    public User(String username, String email, String name, String photo) {
        this.username = username;
		this.email = email;
		this.name = name;
        this.photo = photo;
    }

    public User(String username, String email, String name, String phone, String profile, String work, String workplace, 
                String address, String postalcode, String fiscal, Date userCreationTime, String photo) {
        this.username = username;
		this.email = email;
		this.name = name;
		this.phone = phone;
        this.profile = profile;
        this.work = work;
        this.workplace = workplace;
        this.address = address;
        this.postalcode = postalcode;
        this.fiscal = fiscal;
        this.userCreationTime = userCreationTime;
        this.photo = photo;
    }

    public User(String username, String password, String email, String name, String phone, String profile, String work, String workplace, 
                String address, String postalcode, String fiscal, String role, String state, Date userCreationTime, String photo) {
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
        this.userCreationTime = userCreationTime;
        this.photo = photo;
    }
}