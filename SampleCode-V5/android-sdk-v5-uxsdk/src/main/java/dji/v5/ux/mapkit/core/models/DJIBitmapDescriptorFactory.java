package dji.v5.ux.mapkit.core.models;

import android.graphics.Bitmap;
import android.view.View;

//Doc key: DJIMap_DJIBitmapDescriptorFactory
/**
 * Used to create instances of DJIBitmapDescriptor.
 */
public class DJIBitmapDescriptorFactory {

    private DJIBitmapDescriptorFactory() {}

    private static final DJIBitmapDescriptor DEFAULT_MARKER = new DJIBitmapDescriptor((Bitmap)null);

    public static DJIBitmapDescriptor defaultMarker() {
        return DEFAULT_MARKER;
    }

    //Doc key: DJIMap_DJIBitmapDescriptorFactory_fromAsset
    /**
     * Creates a DJIBitmapDescriptor using the name of a bitmap file in the assets directory.
     *
     * @param assetName The name of a bitmap in the assets directory.
     * @return A DJIBitmapDescriptor object.
     */
    public static DJIBitmapDescriptor fromAsset(String assetName) {
        return new DJIBitmapDescriptor(assetName, DJIBitmapDescriptor.Type.PATH_ASSET);
    }

    //Doc key: DJIMap_DJIBitmapDescriptorFactory_fromBitmap
    /**
     * Creates a DJIBitmapDescriptor from the given Bitmap.
     *
     * @param image A Bitmap image.
     * @return A DJIBitmapDescriptor object.
     */
    public static DJIBitmapDescriptor fromBitmap(Bitmap image) {
        return new DJIBitmapDescriptor(image);
    }

    //Doc key: DJIMap_DJIBitmapDescriptorFactory_fromFile
    /**
     * Creates a DJIBitmapDescriptor using the name of a bitmap image file located in the
     * internal storage.
     *
     * @param fileName The name of the bitmap image file.
     * @return A DJIBitmapDescriptor object.
     */
    public static DJIBitmapDescriptor fromFile(String fileName) {
        return new DJIBitmapDescriptor(fileName, DJIBitmapDescriptor.Type.PATH_FILEINPUT);
    }

    //Doc key: DJIMap_DJIBitmapDescriptorFactory_fromPath
    /**
     * Creates a DJIBitmapDescriptor from the absolute file path of a bitmap image.
     *
     * @param path The absolute file path of the bitmap image.
     * @return A DJIBitmapDescriptor object.
     */
    public static DJIBitmapDescriptor fromPath(String path) {
        return new DJIBitmapDescriptor(path, DJIBitmapDescriptor.Type.PATH_ABSOLUTE);
    }

    //Doc key: DJIMap_DJIBitmapDescriptorFactory_fromResource
    /**
     * Creates a DJIBitmapDescriptor from the resource id of a bitmap image.
     *
     * @param resourceId The resource id of a bitmap image.
     * @return A DJIBitmapDescriptor object.
     */
    public static DJIBitmapDescriptor fromResource(int resourceId) {
        return new DJIBitmapDescriptor(resourceId);
    }

    /**
     * {@hide}
     * Only supported by AMaps.
     * @param view
     * @return A DJIBitmapDescriptor object.
     */
    public static DJIBitmapDescriptor fromView(View view) {
        return new DJIBitmapDescriptor(view);
    }
}
