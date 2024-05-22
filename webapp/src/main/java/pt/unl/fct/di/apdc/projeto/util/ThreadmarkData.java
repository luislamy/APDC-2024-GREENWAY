package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.datastore.Entity;

public class ThreadmarkData {
    
    public String type;
    
    public long previous, next;

    public ThreadmarkData() {}

    public ThreadmarkData(String type) {
        this.type = type;
    }

    public ThreadmarkData(String type, long previous) {
        this.type = type;
        this.previous = previous;
    }

    public ThreadmarkData(String type, long next, boolean isFirst) {
        this.type = type;
        this.next = next;
    }

    public ThreadmarkData(String type, long previous, long next) {
        this.type = type;
        this.previous = previous;
        this.next = next;
    }

    public boolean isValidThreadmark(Entity previous) {
        if ( type == null || type.trim().isEmpty() )
            return false;
        return previous.getLong("replyID") == this.previous;
    }
}