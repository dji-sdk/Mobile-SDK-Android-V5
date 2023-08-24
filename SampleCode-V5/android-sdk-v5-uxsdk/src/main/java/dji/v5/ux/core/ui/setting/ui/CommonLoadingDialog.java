package dji.v5.ux.core.ui.setting.ui;

/**
 * Created by Luca.Wu on 2017/4/5.
 */

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import dji.v5.utils.common.AndUtil;
import dji.v5.ux.R;
import dji.v5.ux.core.ui.setting.dialog.BaseDialog;

/**
 * 加载提醒对话框
 */
public class CommonLoadingDialog extends BaseDialog {

    protected View rootView;
    protected TextView mLoadText;

    public CommonLoadingDialog(Context context) {
        this(context, R.style.SimpleProgressDialog);
    }

    public CommonLoadingDialog(Context context, int theme) {
        super(context, theme);
        initialize();
    }

    protected void initialize() {
        rootView = getLayoutInflater().inflate(R.layout.uxsdk_dialog_common_loading_layout, null);
        mLoadText = (TextView) rootView.findViewById(R.id.tv_load_dialog);
    }

    @Override
    public int getDialogWidth() {
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    @Override
    public int getDialogHeight() {
        return (int) AndUtil.getDimension(R.dimen.uxsdk_100_dp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(rootView);
        //设置不可取消，点击其他区域不能取消，实际中可以抽出去封装供外包设置
        setCancelable(false);
        setCanceledOnTouchOutside(false);
    }

    @Override
    public void show() {
        super.show();
    }

    public void setLoadingText(String text) {
        mLoadText.setText(text);
    }
}