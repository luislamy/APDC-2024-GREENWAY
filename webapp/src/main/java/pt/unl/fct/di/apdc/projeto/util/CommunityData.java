package pt.unl.fct.di.apdc.projeto.util;

public class CommunityData {

    public String name, nickname, description; //nickname é o ID

    public CommunityData() {}

    public CommunityData(String name, String nickname, String description) {
        this.name = name;
        this.nickname = nickname;
        this.description = description;
    }

}