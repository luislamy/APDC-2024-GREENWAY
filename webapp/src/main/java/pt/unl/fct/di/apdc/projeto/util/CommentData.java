package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.Timestamp;

public class CommentData {

    public String commentBody, username, commentID, parentID;

    public Timestamp commentDate, lastEdit, pinDate;

    public long likes, dislikes, comments;

    public boolean isPinned, isLiked, isDisliked;

    public CommentData() {
    }

    public CommentData(String commentBody, String username, String commentID, String parentID, Timestamp commentDate,
            Timestamp lastEdit, Timestamp pinDate, long likes, long dislikes, long comments, boolean isPinned,
            boolean isLiked, boolean isDisliked) {
        this.commentBody = commentBody;
        this.username = username;
        this.commentID = commentID;
        this.parentID = parentID;
        this.commentDate = commentDate;
        this.lastEdit = lastEdit;
        this.pinDate = pinDate;
        this.likes = likes;
        this.dislikes = dislikes;
        this.comments = comments;
        this.isPinned = isPinned;
        this.isLiked = isLiked;
        this.isDisliked = isDisliked;
    }

    public boolean isPinned() {
        return this.isPinned;
    }

    public boolean isTopLevelComment() {
        return this.parentID == null || this.parentID.trim().isEmpty();
    }

    public long likeRatio() {
        return this.likes - this.dislikes;
    }

    public int isValidToComment(String username) {
        if (this.commentBody == null || this.commentBody.trim().isEmpty())
            return -1;
        if (!this.username.equals(username))
            return -2;
        return 1;
    }

    public String getInvalidReason(int code) {
        switch (code) {
            case -1:
                return "no comment body.";
            case -2:
                return "different username";
            default:
                return "internal error";
        }
    }
}