package dji.v5.ux.mapkit.core.places;

import java.util.ArrayList;

public class DJIGeocoderResult {
    public String status;
    public ArrayList<FirstLevel> results;


    public static class FirstLevel {
        public ArrayList<SecondLevel> address_components;
        public String formatted_address;
        public ArrayList<String> types;
    }

    public static class SecondLevel {
        public String long_name;
        public String short_name;
        public ArrayList<String> types;
    }

    public static FirstLevel getStreetAdress(DJIGeocoderResult result) {
        for (FirstLevel firstLevel : result.results) {
            if (firstLevel.types.contains("street_address")||firstLevel.types.contains("route")) {
                return firstLevel;
            }
        }
        return null;
    }
}
