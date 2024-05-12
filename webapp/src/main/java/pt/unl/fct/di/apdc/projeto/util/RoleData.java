package pt.unl.fct.di.apdc.projeto.util;

public class RoleData {
    
    public String username;
    public String role;
    public AuthToken token;

    public RoleData() {

    }

    public RoleData(String username, String role, AuthToken token) {
        this.username = username;
        this.role = role;
        this.token = token;
    }
}
