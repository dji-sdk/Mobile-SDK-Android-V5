package dji.v5.ux.core.ui.setting.taplayout;

import android.content.Context;
import android.widget.Checkable;
import android.widget.FrameLayout;


public abstract class TabView extends FrameLayout implements Checkable{

    protected TabView(Context context) {
        super(context);
    }

    public abstract TabView setBadge(int num);
}
