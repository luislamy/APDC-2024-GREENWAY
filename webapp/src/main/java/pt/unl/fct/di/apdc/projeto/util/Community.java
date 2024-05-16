package pt.unl.fct.di.apdc.projeto.util;

import com.google.cloud.Timestamp;

public class Community {
    
    public String name, description, creator, communityID;

    public long members;

    public Timestamp creationDate;

    public Community() {}

    public Community(String communityID, String name, String description, long members, String creator, Timestamp creationDate) {
        this.communityID = communityID;
        this.name = name;
        this.description = description;
        this.members = members;
        this.creator = creator;
        this.creationDate = creationDate;
    }
}
