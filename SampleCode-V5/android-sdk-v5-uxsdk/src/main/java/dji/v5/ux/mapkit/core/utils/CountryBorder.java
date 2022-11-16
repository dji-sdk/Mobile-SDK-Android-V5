package dji.v5.ux.mapkit.core.utils;

import java.util.ArrayList;

/**
 * Created by joeyang on 5/25/17.
 */

public class CountryBorder {
    private CountryBorder(){
        //no use
    }
    public static class CountryItem {
        public String id;
        public String name;
        public Geometry geometry;
    }

    public static class Geometry {
        public String type;
        public ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> coordinates;
    }
}
