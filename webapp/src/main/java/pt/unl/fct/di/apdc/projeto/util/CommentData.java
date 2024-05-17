package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.Timestamp;

public class CommentData extends PostData {

    public String commentID;

    public String parentID;

    public CommentData() {
    }

    public CommentData(String postID, String title, String postBody, String username, Timestamp postDate,
            Timestamp lastEdit, long likes, long dislikes, long comments, boolean isLocked, boolean isPinned,
            Timestamp pinDate, boolean isLiked, boolean isDisliked, String commentID, String parentID) {
        super(postID, title, postBody, username, postDate, lastEdit, likes, dislikes, comments, isLocked, isPinned,
                pinDate, isLiked, isDisliked);
        this.commentID = commentID;
        this.parentID = parentID;
    }

    public int isValidToComment(String username) {
        if (super.postBody == null || super.postBody.trim().isEmpty())
            return -1;
        if ( !super.username.equals(username) )
            return -2;
        return 1;
    }

    public String getInvalidReason(int code) {
        switch(code) {
            case -1: return "no comment body.";
            case -2: return "different username";
            default: return "internal error";
        }
    }
}