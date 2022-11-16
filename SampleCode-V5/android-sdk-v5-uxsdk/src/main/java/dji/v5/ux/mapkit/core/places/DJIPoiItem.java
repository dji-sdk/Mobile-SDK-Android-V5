package dji.v5.ux.mapkit.core.places;

import androidx.annotation.NonNull;

import dji.v5.ux.mapkit.core.models.DJILatLng;

/**
 * Created by joeyang on 10/10/17.
 */
public class DJIPoiItem {
    String name;
    String address;
    DJILatLng location;

    private DJIPoiItem() {

    }

    public DJIPoiItem(@NonNull String name, @NonNull String address, @NonNull DJILatLng location) {
        this.name = name;
        this.address = address;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public DJILatLng getLocation() {
        return location;
    }

    public void setLocation(DJILatLng location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DJIPoiItem poiItem = (DJIPoiItem) o;

        if (!name.equals(poiItem.name)) return false;
        if (!address.equals(poiItem.address)) return false;
        return location.equals(poiItem.location);

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + address.hashCode();
        result = 31 * result + location.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Name: " + name +
                ", Address: " + address +
                ", location: " + location;
    }
}
