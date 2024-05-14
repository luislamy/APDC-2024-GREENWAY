package pt.unl.fct.di.apdc.projeto.util;

public class ConversationClass {
    
    public String sender;

    public String receiver;

    public AuthToken token;

    public ConversationClass() {

    }

    public ConversationClass(String sender, String receiver) {
        this.sender = sender;
        this.receiver = receiver;
    }
}
