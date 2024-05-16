package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.Timestamp;

public class PostData {

    public String postID;

    public String title;

    public String postBody;

    public String username;

    public Timestamp postDate;

    public Timestamp lastEdit;

    public long likes;

    public long dislikes;

    public long comments;

    public PostData() {}

    public PostData(String postID, String title, String postBody, String username, Timestamp postDate, Timestamp lastEdit, long likes, long dislikes, long comments) {
        this.postID = postID;
        this.title = title;
        this.postBody = postBody;
        this.username = username;
        this.postDate = postDate;
        this.lastEdit = lastEdit;
        this.likes = likes;
        this.dislikes = dislikes;
        this.comments = comments;
    }
}