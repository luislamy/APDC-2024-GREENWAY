package pt.unl.fct.di.apdc.projeto.util;

public class RemoveCommunityRequest {

    public String communityID, userID, message;

    public RemoveCommunityRequest() {
    }

    public RemoveCommunityRequest(String communityID, String userID, String message) {
        this.communityID = communityID;
        this.userID = userID;
        this.message = message;
    }
}
