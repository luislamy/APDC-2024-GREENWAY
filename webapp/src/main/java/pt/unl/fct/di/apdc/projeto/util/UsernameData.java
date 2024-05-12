package pt.unl.fct.di.apdc.projeto.util;

public class UsernameData {
    
    public String username;
    public AuthToken token;

    public UsernameData() {

    }

    public UsernameData(String username, AuthToken token) {
        this.username = username;
        this.token= token;
    }
}
