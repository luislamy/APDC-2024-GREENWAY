package pt.unl.fct.di.apdc.projeto.util;

public class CommunityData {

    public String communityID, name, description;

    public CommunityData() {}

    public CommunityData(String communityID, String name, String description) {
        this.communityID = communityID;
        this.name = name;
        this.description = description;
    }


    public int isValid() {
        if ( this.communityID == null || this.communityID.trim().isEmpty() )
            return -1;
        if ( this.name == null || this.name.trim().isEmpty() )
            return -2;
        if ( this.description == null || this.description.trim().isEmpty() )
            return -3;
        return 1;
    }

    public String getInvalidReason(int code) {
        switch (code) {
            case -1: return "no communityID";
            case -2: return "no community name";
            case -3: return "no community description";
            default: return "internal error";
        }
    }
}