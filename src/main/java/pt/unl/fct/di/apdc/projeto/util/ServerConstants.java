package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
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

    public Entity getToken(AuthToken authToken) {
        return getToken(null, authToken);
    }

    public Entity getToken(Transaction txn, AuthToken authToken) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("Token")
            .setFilter(CompositeFilter.and(
                PropertyFilter.hasAncestor(this.getUserKey(authToken.username)), 
                PropertyFilter.eq("tokenID", authToken.tokenID)))
            .build();
        QueryResults<Entity> results = txn != null ? txn.run(query) : datastore.run(query);
        Entity token = null;
        if ( results.hasNext() )
            token = results.next();
        return token;
    }
}