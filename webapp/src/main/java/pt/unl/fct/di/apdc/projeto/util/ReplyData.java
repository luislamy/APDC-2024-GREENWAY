package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.Timestamp;

public class ReplyData {

    public long replyID;

    public String username;

    public String replyBody;

    public Timestamp replyDate;

    public Timestamp lastEdit;

    public long likes;

    public boolean isLiked;

    public ThreadmarkData threadmark;

    public ReplyData() {}

    public ReplyData(long replyID, String username, String replyBody, Timestamp replyDate, Timestamp lastEdit, long likes, boolean isLiked) {
        this.replyID = replyID;
        this.username = username;
        this.replyBody = replyBody;
        this.replyDate = replyDate;
        this.lastEdit = lastEdit;
        this.likes = likes;
        this.isLiked = isLiked;
    }

    public ReplyData(long replyID, String username, String replyBody, Timestamp replyDate, Timestamp lastEdit, long likes, boolean isLiked, ThreadmarkData threadmark) {
        this(replyID, username, replyBody, replyDate, lastEdit, likes, isLiked);
        this.threadmark = threadmark;
    }

    public long getReplyID() {
        return this.replyID;
    }

    public int isValidReply(String username) {
        if ( !this.username.equals(username) ) {
            return -1;
        } else if ( this.replyBody == null || this.replyBody.trim().isEmpty() ) {
            return -2;
        }
        return 1;
    }
}