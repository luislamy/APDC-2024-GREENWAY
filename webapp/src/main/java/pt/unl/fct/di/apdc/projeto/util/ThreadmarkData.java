package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.datastore.Entity;

public class ThreadmarkData {
    
    public String type;
    
    public long previous, next;

    public ThreadmarkData() {}

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