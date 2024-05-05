package pt.unl.fct.di.apdc.projeto.util;

import java.util.Date;

public class MessageClass {
    
    public String sender;

    public String receiver;

    public String message;

    public Date timeStamp;

    public AuthToken token;

    public MessageClass() {

    }

    public MessageClass(String sender, String receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }

    public MessageClass(String sender, String receiver, String message, Date timeStamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.timeStamp = timeStamp;
    }

    public Date getTimeStamp() {
        return this.timeStamp;
    }
}
