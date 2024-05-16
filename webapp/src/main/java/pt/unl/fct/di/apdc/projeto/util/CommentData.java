package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.Timestamp;

public class CommentData extends PostData {

    public String commentID;

    public String parentID;

    public CommentData() {}

    public CommentData(String postID, String title, String postBody, String username, Timestamp postDate, Timestamp lastEdit, long likes, long dislikes, long comments, boolean isLocked, boolean isPinned, Timestamp pinDate, boolean isLiked, boolean isDisliked, String commentID, String parentID) {
        super(postID, title, postBody, username, postDate, lastEdit, likes, dislikes, comments, isLocked, isPinned, pinDate, isLiked, isDisliked);
        this.commentID = commentID;
        this.parentID = parentID;
    }
}