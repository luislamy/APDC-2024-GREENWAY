package pt.unl.fct.di.apdc.projeto.util;

import java.util.List;

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

    public boolean isLocked;

    public boolean isPinned;

    public Timestamp pinDate;

    public boolean isLiked;

    public boolean isDisliked;

    public List<String> tags;

    public PostData() {}

    public PostData(String postID, String title, String postBody, String username, Timestamp postDate, Timestamp lastEdit, long likes, long dislikes, long comments, boolean isLocked, boolean isPinned, Timestamp pinDate, boolean isLiked, boolean isDisliked) {
        this.postID = postID;
        this.title = title;
        this.postBody = postBody;
        this.username = username;
        this.postDate = postDate;
        this.lastEdit = lastEdit;
        this.likes = likes;
        this.dislikes = dislikes;
        this.comments = comments;
        this.isLocked = isLocked;
        this.isPinned = isPinned;
        this.pinDate = pinDate;
        this.isLiked = isLiked;
        this.isDisliked = isDisliked;
    }

    public int isValidToPost(String username) {
        if ( title == null || title.trim().isEmpty() )
            return -1;
        else if ( postBody == null || postBody.trim().isEmpty() )
            return -2;
        else if (!this.username.equals(username))
            return -3;
        return 1;
    }

    public String getInvalidReason(int code) {
        switch(code) {
            case -1: return "no title";
            case -2: return "no post body";
            case -3: return "different username";
            default: return "internal error";
        }
    }
}