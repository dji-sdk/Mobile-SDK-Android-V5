package dji.v5.ux.remotecontroller.calibration;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RadioButton;

import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;


/**
 * Created by richard.liao on 2018/6/27:17:46
 */
public class CalibrationRadioButton extends RadioButton {

    private final static int LABEL_WIDTH_DP = 25;
    private final static int LABEL_HEIGHT_DP = 25;

    private Drawable finishedLabel;

    public CalibrationRadioButton(Context context) {
        super(context);
        init();
    }

    public CalibrationRadioButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        finishedLabel = getResources().getDrawable(R.drawable.uxsdk_setting_ui_rc_calibration_finished_label);
        finishedLabel.setBounds(0, 0, AndUtil.dip2px(getContext(), LABEL_WIDTH_DP), AndUtil.dip2px(getContext(), LABEL_HEIGHT_DP));
    }

    public void setCalirationComplete(boolean isComplete) {
        if (isComplete) {
            setCompoundDrawables(null, null, finishedLabel, null);
            setCompoundDrawablePadding(-AndUtil.dip2px(getContext(), LABEL_WIDTH_DP));
        } else {
            setCompoundDrawables(null, null, null, null);
        }
    }
}
