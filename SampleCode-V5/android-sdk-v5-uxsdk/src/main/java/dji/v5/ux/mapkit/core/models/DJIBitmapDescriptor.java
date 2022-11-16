package dji.v5.ux.mapkit.core.models;
import android.graphics.Bitmap;
import android.view.View;

public final class DJIBitmapDescriptor {

   // private static final String TAG = "DJIBitmapDescriptor";

    private String id;

    private Type mType;

    private Bitmap mBitmap;
    private String mPathString;
    private int mResourceId;
    private View mView;

    DJIBitmapDescriptor(Bitmap bitmap) {
        mType = Type.BITMAP;
        mBitmap = bitmap;
        mPathString = null;
        mResourceId = 0;
    }

    DJIBitmapDescriptor(String pathString, Type type) {
        mType = type;
        mBitmap = null;
        mPathString = pathString;
        mResourceId = 0;
    }

    DJIBitmapDescriptor(int resourceId) {
        mType = Type.RESOURCE_ID;
        mBitmap = null;
        mPathString = null;
        mResourceId = resourceId;
    }

    DJIBitmapDescriptor(View view) {
        mType = Type.VIEW;
        mView = view;
        mBitmap = null;
        mPathString = null;
        mResourceId = 0;
    }

    /**
     * 获取该描述子对应的图片
     * @return
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void updateBitmap(Bitmap bitmap) {
        mType = Type.BITMAP;
        mBitmap = bitmap;
        mPathString = null;
        mResourceId = 0;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return mType;
    }

    public String getPath() {
        return mPathString;
    }

    public int getResourceId() {
        return mResourceId;
    }

    public View getView(){
        return mView;
    }

    /**
     * 传进来的资源类型
     */
    public enum Type {
        BITMAP,
        PATH_ABSOLUTE,
        PATH_ASSET,
        PATH_FILEINPUT,
        RESOURCE_ID,
        VIEW,
    }
}
