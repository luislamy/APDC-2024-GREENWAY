package pt.unl.fct.di.apdc.projeto.util;

public class CommunityData {

    public String communityID, name, description;
    public boolean isLocked;

    public CommunityData() {}

    public CommunityData(String communityID, String name, String description, boolean isLocked) {
        this.communityID = communityID;
        this.name = name;
        this.description = description;
        this.isLocked = isLocked;
    }
}