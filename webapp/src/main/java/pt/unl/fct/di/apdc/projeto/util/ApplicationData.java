package pt.unl.fct.di.apdc.projeto.util;

public class ApplicationData {

    public String companyName, slogan, description, city, country, address, contact;

    public ApplicationData() {
    }

    public ApplicationData(String companyName, String slogan, String description, String city, String country, String address, String contact) {
        this.companyName = companyName;
        this.slogan = slogan;
        this.description = description;
        this.city = city;
        this.country = country;
        this.address = address;
        this.contact = contact;
    }

    public int isValid() {
        if ( this.companyName == null || this.companyName.trim().isEmpty() )
            return -1;
        if ( this.slogan == null || this.slogan.trim().isEmpty() )
            return -2;
        if ( this.description == null || this.description.trim().isEmpty() )
            return -3;
        if ( this.city == null || this.city.trim().isEmpty() )
            return -4;
        if ( this.country == null || this.country.trim().isEmpty() )
            return -5;
        if ( this.address == null || this.address.trim().isEmpty() )
            return -6;
        if ( this.contact == null || this.contact.trim().isEmpty() )
            return -7;
        return 1;
    }

    public String getInvalidReason(int code) {
        switch (code) {
            case -1: return "no company name";
            case -2: return "no slogan";
            case -3: return "no description";
            case -4: return "no city";
            case -5: return "no country";
            case -6: return "no address";
            case -7: return "no contact";
            default: return "internal error";
        }
    }

}
