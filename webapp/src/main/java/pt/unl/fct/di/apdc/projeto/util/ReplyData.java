package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.Timestamp;

public class ReplyData {

    public long replyID;

    public String username;

    public String replyBody;

    public Timestamp replyDate;

    public Timestamp lastEdit;

    public long likes;

    public ThreadmarkData threadmark;

    public ReplyData() {}

    public ReplyData(long replyID, String username, String replyBody, Timestamp replyDate, Timestamp lastEdit, long likes) {
        this.replyID = replyID;
        this.username = username;
        this.replyBody = replyBody;
        this.replyDate = replyDate;
        this.lastEdit = lastEdit;
        this.likes = likes;
    }

    public ReplyData(long replyID, String username, String replyBody, Timestamp replyDate, Timestamp lastEdit, long likes, ThreadmarkData threadmark) {
        this(replyID, username, replyBody, replyDate, lastEdit, likes);
        this.threadmark = threadmark;
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