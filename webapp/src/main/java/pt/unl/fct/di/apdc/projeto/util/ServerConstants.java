package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;

public class ServerConstants {

    public static final String USER = "USER", GBO = "GBO", GA = "GA", EP = "EP", GS = "GS", SU = "SU";

	public static final String ACTIVE = "ACTIVE", INACTIVE = "INACTIVE";

    public static final String PUBLIC = "PUBLIC", PRIVATE = "PRIVATE";

    private final Datastore datastore;

    private final KeyFactory userKeyFactory;

    private static ServerConstants singleton = null;

    private ServerConstants() {
       //this.datastore = DatastoreOptions.newBuilder().setProjectId("apdc-grupo-7").setHost("localhost:8081").build().getService();
        this.datastore = DatastoreOptions.getDefaultInstance().getService();
        this.userKeyFactory = datastore.newKeyFactory().setKind("User");
    }

    public static ServerConstants getServerConstants() {
        if ( singleton == null )
            singleton = new ServerConstants();
        return singleton;
    }

    public Datastore getDatastore() {
        return this.datastore;
    }

    public Key getUserKey(String username) {
        return userKeyFactory.newKey(username);
    }

    public Key getTokenKey(String username) {
        Key tokenKey = datastore.allocateId(
				datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", username))
				.setKind("Token")
				.newKey());
        return tokenKey;
    }

    public Key getTokenKey(String username, String tokenID) {
        Key tokenKey = datastore.allocateId(
				datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", username))
				.setKind("Token")
				.newKey(tokenID));
        return tokenKey;
    }

    public Entity getToken(String username, String tokenID) {
        return getToken(null, username, tokenID);
    }

    public Entity getToken(Transaction txn, String username, String tokenID) {
        Key tokenKey = getTokenKey(username, tokenID);
        Entity token = txn == null ? datastore.get(tokenKey) : txn.get(tokenKey);
        return token;
    }

    public void removeToken(String username, String tokenID) {
        this.removeToken(null, username, tokenID);
    }

    public void removeToken(Transaction txn, String username, String tokenID) {
        Key tokenKey = this.getTokenKey(username, tokenID);
        if ( txn == null ) {
            this.datastore.delete(tokenKey);
        } else {
            txn.delete(tokenKey);
        }
    }
}