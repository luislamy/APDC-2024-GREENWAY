package pt.unl.fct.di.apdc.projeto.util;

import java.util.List;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.StringValue;

public class ThreadData {

    public String threadID;

    public String title;

    public String username;

    public Timestamp threadStartDate;

    public String lastReplyUsername;

    public Timestamp lastReplyDate;

    public long replies;

    public boolean isLocked;

    public boolean isPinned;

    public Timestamp pinDate;

    public List<String> tags;

    public boolean isTagging;

    public ReplyData lastReply;

    public ThreadData() {}

    public ThreadData(String threadID, String title, String username, Timestamp threadStartDate, long replies, boolean isLocked, boolean isPinned, Timestamp pinDate, List<String> tags, String lastReplyUsername, Timestamp lastReplyDate) {
        this.threadID = threadID;
        this.title = title;
        this.username = username;
        this.threadStartDate = threadStartDate;
        this.replies = replies;
        this.isLocked = isLocked;
        this.isPinned = isPinned;
        this.pinDate = pinDate;
        this.tags = tags;
        this.lastReplyUsername = lastReplyUsername;
        this.lastReplyDate = lastReplyDate;
    }

    public boolean isPinned() {
        return this.isPinned;
    }

    public Timestamp pinDate() {
        return this.pinDate;
    }

    public Timestamp lastReplyDate() {
        return this.lastReplyDate;
    }

    public int isValidToStartThread(String username) {
        if ( this.lastReply == null || !this.username.equals(username) ) {
            return -1;
        } else if ( this.title == null || this.title.trim().isEmpty() ) {
            return -2;
        } else if ( this.lastReply.isValidReply(username) < 1 ) {
            return -3;
        }
        return 1;
    }

    public boolean isValidTagging(Entity thread) {
        List<StringValue> threadTags = thread.getList("tags");
        if ( isTagging ) {
            boolean newTag = false;
            for ( StringValue tag : threadTags ) {
                newTag = !this.tags.contains(tag.get());
                if ( newTag )
                    break;
            }
            return newTag;
        } else {
            boolean oldTag = false;
            for ( StringValue tag : threadTags ) {
                oldTag = this.tags.contains(tag.get());
                if ( oldTag )
                    break;
            }
            return oldTag;
        }
    }
}