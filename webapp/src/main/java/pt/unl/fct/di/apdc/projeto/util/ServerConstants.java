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

    public static final String USER = "USER", EP = "EP", GC = "GC", GBO = "GBO", GA = "GA", GS = "GS", SU = "SU";

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

    public Key getPostKey(String communityID, String postID) {
        return datastore.newKeyFactory()
            .addAncestor(PathElement.of("Community", communityID))
            .setKind("Post")
            .newKey(postID);
    }

    public Entity getPost(String communityID, String postID) {
        return getPost(null, communityID, postID);
    }

    public Entity getPost(Transaction txn, String communityID, String postID) {
        return txn == null ? datastore.get(getPostKey(communityID, postID)) : txn.get(getPostKey(communityID, postID));
    }

    public QueryResults<Entity> getPostsFromCommunity(String communityID) {
        return getPostsFromCommunity(null, communityID);
    }
    
    public QueryResults<Entity> getPostsFromCommunity(Transaction txn, String communityID) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("Post")
            .setFilter(PropertyFilter.hasAncestor(getCommunityKey(communityID)))
            .build();
        return txn == null ? datastore.run(query) : txn.run(query);
    }

    public QueryResults<Entity> getCommentsFromPost(Key postKey) {
        return getCommentsFromPost(null, postKey);
    }

    public QueryResults<Entity> getCommentsFromPost(Transaction txn, Key postKey) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("Comment")
            .setFilter(PropertyFilter.hasAncestor(postKey))
            .build();
        return txn == null ? datastore.run(query) : txn.run(query);
    }

    public Key getPostLikeKey(String communityID, String postID, String username) {
        return datastore.newKeyFactory()
            .addAncestor(PathElement.of("Community", communityID))
            .addAncestor(PathElement.of("Post", postID))
            .setKind("PostLike")
            .newKey(username);
    }

    public Entity getPostLike(String communityID, String postID, String username) {
        return getPostLike(null, communityID, postID, username);
    }

    public Entity getPostLike(Transaction txn, String communityID, String postID, String username) {
        return txn == null ? datastore.get(getPostLikeKey(communityID, postID, username)) : txn.get(getPostLikeKey(communityID, postID, username));
    }

    public Key getPostDislikeKey(String communityID, String postID, String username) {
        return datastore.newKeyFactory()
            .addAncestor(PathElement.of("Community", communityID))
            .addAncestor(PathElement.of("Post", postID))
            .setKind("PostDislike")
            .newKey(username);
    }

    public Entity getPostDislike(String communityID, String postID, String username) {
        return getPostDislike(null, communityID, postID, username);
    }

    public Entity getPostDislike(Transaction txn, String communityID, String postID, String username) {
        return txn == null ? datastore.get(getPostDislikeKey(communityID, postID, username)) : txn.get(getPostDislikeKey(communityID, postID, username));
    }

    public Key getCommunityMemberKey(String communityID, String username) {
        return datastore.newKeyFactory()
            .addAncestor(PathElement.of("Community", communityID))
            .setKind("Member")
            .newKey(username);
    }

    public Entity getCommunityMember(String communityID, String username) {
        return getCommunityMember(null, communityID, username);
    }

    public Entity getCommunityMember(Transaction txn, String communityID, String username) {
        return txn ==null ? datastore.get(getCommunityMemberKey(communityID, username)) : txn.get(getCommunityMemberKey(communityID, username));
    }

    public Key getTopLevelCommentKey(String communityID, String postID, String commentID) {
        return datastore.newKeyFactory()
            .addAncestor(PathElement.of("Community", communityID))
            .addAncestor(PathElement.of("Post", postID))
            .setKind("Comment")
            .newKey(commentID);
    }

    public Entity getTopLevelComment(String communityID, String postID, String commentID) {
        return getTopLevelComment(null, communityID, postID, commentID);
    }

    public Entity getTopLevelComment(Transaction txn, String communityID, String postID, String commentID) {
        return txn == null ? datastore.get(getTopLevelCommentKey(communityID, postID, commentID)) : txn.get(getTopLevelCommentKey(communityID, postID, commentID));
    }

    public Key getChildCommentKey(String communityID, String postID, String parentCommentID, String commentID) {
        return datastore.newKeyFactory()
            .addAncestor(PathElement.of("Community", communityID))
            .addAncestor(PathElement.of("Post", postID))
            .addAncestor(PathElement.of("Comment", parentCommentID))
            .setKind("Comment")
            .newKey(commentID);
    }

    public Entity getChildComment(String communityID, String postID, String parentCommentID, String commentID) {
        return getChildComment(null, communityID, postID, parentCommentID, commentID);
    }

    public Entity getChildComment(Transaction txn, String communityID, String postID, String parentCommentID, String commentID) {
        return txn == null ? datastore.get(getChildCommentKey(communityID, postID, parentCommentID, commentID)) : txn.get(getChildCommentKey(communityID, postID, parentCommentID, commentID));
    }

    public Entity getParentComment(Transaction txn, String communityID, String postID, String parentCommentID) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("Comment")
            .setFilter(CompositeFilter.and(
                PropertyFilter.hasAncestor(getCommunityKey(communityID)), 
                CompositeFilter.and(PropertyFilter.hasAncestor(getPostKey(communityID, postID)), 
                PropertyFilter.eq("commentID", parentCommentID))))
            .build();
        var results = txn == null ? datastore.run(query) : txn.run(query);
        return results.hasNext() ? results.next() : null;
    }

    public Key getCommentLikeKey(String communityID, String postID, String commentID, String username) {
        return datastore.newKeyFactory()
            .addAncestor(PathElement.of("Community", communityID))
            .addAncestor(PathElement.of("Post", postID))
            .addAncestor(PathElement.of("Comment", commentID))
            .setKind("CommentLike")
            .newKey(username);
    }

    public Entity getCommentLike(String communityID, String postID, String commentID, String username) {
        return getCommentLike(null, communityID, postID, commentID, username);
    }

    public Entity getCommentLike(Transaction txn, String communityID, String postID, String commentID, String username) {
        return txn == null ? datastore.get(getCommentLikeKey(communityID, postID, commentID, username)) : txn.get(getCommentLikeKey(communityID, postID, commentID, username));
    }

    public Key getCommentDislikeKey(String communityID, String postID, String commentID, String username) {
        return datastore.newKeyFactory()
            .addAncestor(PathElement.of("Community", communityID))
            .addAncestor(PathElement.of("Post", postID))
            .addAncestor(PathElement.of("Comment", commentID))
            .setKind("CommentDislike")
            .newKey(username);
    }

    public Entity getCommentDislike(String communityID, String postID, String commentID, String username) {
        return getCommentDislike(null, communityID, postID, commentID, username);
    }

    public Entity getCommentDislike(Transaction txn, String communityID, String postID, String commentID, String username) {
        return txn == null ? datastore.get(getCommentDislikeKey(communityID, postID, commentID, username)) : txn.get(getCommentDislikeKey(communityID, postID, commentID, username));
    }

    public QueryResults<Entity> getLikesFromPost(Key postKey) {
        return getLikesFromPost(null, postKey);
    }

    public QueryResults<Entity> getLikesFromPost(Transaction txn, Key postKey) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("PostLike")
            .setFilter(PropertyFilter.hasAncestor(postKey))
            .build();
        return txn == null ? datastore.run(query): txn.run(query);
    }

    public QueryResults<Entity> getDislikesFromPost(Key postKey) {
        return getDislikesFromPost(null, postKey);
    }

    public QueryResults<Entity> getDislikesFromPost(Transaction txn, Key postKey) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("PostDislike")
            .setFilter(PropertyFilter.hasAncestor(postKey))
            .build();
        return txn == null ? datastore.run(query): txn.run(query);
    }

    public QueryResults<Entity> getLikesFromComment(Key commentKey) {
        return getLikesFromComment(null, commentKey);
    }

    public QueryResults<Entity> getLikesFromComment(Transaction txn, Key commentKey) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("CommentLike")
            .setFilter(PropertyFilter.hasAncestor(commentKey))
            .build();
        return txn == null ? datastore.run(query): txn.run(query);
    }

    public QueryResults<Entity> getDislikesFromComment(Key commentKey) {
        return getDislikesFromComment(null, commentKey);
    }

    public QueryResults<Entity> getDislikesFromComment(Transaction txn, Key commentKey) {
        Query<Entity> query = Query.newEntityQueryBuilder()
            .setKind("CommentDislike")
            .setFilter(PropertyFilter.hasAncestor(commentKey))
            .build();
        return txn == null ? datastore.run(query): txn.run(query);
    }
}