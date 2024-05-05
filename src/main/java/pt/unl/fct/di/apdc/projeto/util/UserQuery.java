package pt.unl.fct.di.apdc.projeto.util;

public class UserQuery {

    public String username;

    public String email;

    public String name;

    public UserQuery() {

    }

    public UserQuery(String username, String email, String name) {
        this.username = username;
        this.email = email;
        this.name = name;
    }    
}