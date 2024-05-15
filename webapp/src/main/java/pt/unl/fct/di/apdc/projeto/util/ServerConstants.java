package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;

public class ServerConstants {

    public static final String USER = "USER", EP = "EP"/*, GC = "GC"*/, GBO = "GBO", GA = "GA", GS = "GS", SU = "SU";

	public static final String ACTIVE = "ACTIVE", INACTIVE = "INACTIVE";

    public static final String PUBLIC = "PUBLIC", PRIVATE = "PRIVATE";

    private final Datastore datastore;

    private final KeyFactory userKeyFactory, communityKeyFactory;

    private static ServerConstants singleton = null;

    private ServerConstants() {
       //this.datastore = DatastoreOptions.newBuilder().setProjectId("apdc-grupo-7").setHost("localhost:8081").build().getService();
        this.datastore = DatastoreOptions.getDefaultInstance().getService();
        this.userKeyFactory = datastore.newKeyFactory().setKind("User");
        this.communityKeyFactory = datastore.newKeyFactory().setKind("Community");
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

    public Entity getUser(String username) {
        return getUser(null, username);
    }

    public Entity getUser(Transaction txn, String username) {
        return txn == null ? datastore.get(getUserKey(username)) : txn.get(getUserKey(username));
    }

    public Key getTokenKey(String username, String tokenID) {
        Key tokenKey = datastore.newKeyFactory()
				.addAncestor(PathElement.of("User", username))
				.setKind("Token")
				.newKey(tokenID);
        return tokenKey;
    }

    public Entity getToken(String username, String tokenID) {
        return getToken(null, username, tokenID);
    }

    public Entity getToken(Transaction txn, String username, String tokenID) {
        Key tokenKey = getTokenKey(username, tokenID);
        return txn == null ? datastore.get(tokenKey) : txn.get(tokenKey);
    }

    public void removeToken(String username, String tokenID) {
        this.removeToken(null, username, tokenID);
    }

    public void removeToken(Transaction txn, String username, String tokenID) {
        Key tokenKey = this.getTokenKey(username, tokenID);
        if ( txn == null )
            this.datastore.delete(tokenKey);
        else
            txn.delete(tokenKey);
    }

    public Key getCommunityKey(String key) {
        return communityKeyFactory.newKey(key);
    }

    public Entity getCommunity(String key) {
        return getCommunity(null, key);
    }

    public Entity getCommunity(Transaction txn, String key) {
        return txn == null ? datastore.get(getCommunityKey(key)) : txn.get(getCommunityKey(key));
    }
}