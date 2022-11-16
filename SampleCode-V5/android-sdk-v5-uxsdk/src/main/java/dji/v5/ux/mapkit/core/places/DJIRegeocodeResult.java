package dji.v5.ux.mapkit.core.places;

/**
 * Created by eleven on 2018/2/3.
 */

public class DJIRegeocodeResult {
    private String country;
    private String region;
    private String city;
    private String district;
    private String street;
    private String subStreet;
    private String address;

    public DJIRegeocodeResult() {
        //init something
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getSubStreet() {
        return subStreet;
    }

    public void setSubStreet(String subStreet) {
        this.subStreet = subStreet;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "DJIRegeocodeResult{" +
                "country='" + country + '\'' +
                ", region='" + region + '\'' +
                ", city='" + city + '\'' +
                ", district='" + district + '\'' +
                ", street='" + street + '\'' +
                ", subStreet='" + subStreet + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
