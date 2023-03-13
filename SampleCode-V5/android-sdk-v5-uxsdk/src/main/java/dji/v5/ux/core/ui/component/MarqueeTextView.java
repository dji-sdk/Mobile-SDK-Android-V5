/**
 * @filename    : DJIMarqueeTextView.java
 * @package     : dji.pilot.publics.widget
 * @date        : 2014年12月16日下午4:12:23
 * @author      : gashion.fang
 * @description : 
 * 
 * Copyright (c) 2014, DJI All Rights Reserved.
 * 
 */

package dji.v5.ux.core.ui.component;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.Objects;

@SuppressLint("AppCompatCustomView")
public class MarqueeTextView extends TextView {

    public MarqueeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEllipsize(TextUtils.TruncateAt.MARQUEE);
        setMarqueeRepeatLimit(-1);
        setSingleLine();
        setFocusable(true);
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        //这里text会为null，导致SmartController遥控器必现出现空指针问题：（其他遥控器没有问题，猜测跟Android版本有关）
        // Android NullPointerException at android.text.BoringLayout.isBoring，必须加上该过滤逻辑。
        if (TextUtils.isEmpty(text)) {
            super.setText("", type);
            return;
        }
        if (!Objects.equals(text, getText())) {
            super.setText(text, type);
            requestFocus();
        }
    }
}
