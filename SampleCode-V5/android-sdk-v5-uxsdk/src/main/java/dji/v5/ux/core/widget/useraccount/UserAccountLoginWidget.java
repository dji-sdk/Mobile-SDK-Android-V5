/*
 * Copyright (c) 2018-2020 DJI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dji.v5.ux.core.widget.useraccount;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.fragment.app.FragmentActivity;

import java.util.HashMap;
import java.util.Map;

import dji.v5.manager.account.LoginInfo;
import dji.v5.manager.account.LoginState;
import dji.v5.manager.account.UserAccountManager;
import dji.v5.utils.common.LogUtils;
import dji.v5.ux.R;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.SchedulerProvider;
import dji.v5.ux.core.base.UXSDKError;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

/**
 * User Account Login Widget
 * <p>
 * Widget will display the current login status of the user's DJI account.
 * Tapping on the widget will provide the option to login or logout of the DJI account.
 */
public class UserAccountLoginWidget extends ConstraintLayoutWidget<Boolean> implements OnClickListener {
    //region Fields
    private static final String TAG = "LoginWidget";
    private TextView widgetStateTextView;
    private ImageView widgetActionImageView;
    private ImageView widgetUserImageView;
    private TextView widgetMessageTextView;
    private View widgetDivider;
    private UserAccountLoginWidgetModel widgetModel;
    private Map<LoginState, Integer> widgetStateTextColorMap;
    private Map<LoginState, Integer> widgetMessageTextColorMap;
    private Map<LoginState, Drawable> widgetActionIconMap;
    private Map<LoginState, Drawable> widgetUserIconMap;
    private LoginInfo currentLoginInfo = new LoginInfo();

    //endregion

    //region Lifecycle
    public UserAccountLoginWidget(Context context) {
        super(context);
    }

    public UserAccountLoginWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UserAccountLoginWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        inflate(context, R.layout.uxsdk_widget_user_account_login, this);
        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
        widgetDivider = findViewById(R.id.widget_divider);
        widgetStateTextView = findViewById(R.id.textview_widget_status);
        widgetActionImageView = findViewById(R.id.imageview_widget_status);
        widgetUserImageView = findViewById(R.id.imageview_widget_user);
        widgetMessageTextView = findViewById(R.id.textview_widget_message);
        widgetStateTextColorMap = new HashMap<>();
        widgetMessageTextColorMap = new HashMap<>();
        widgetActionIconMap = new HashMap<>();
        widgetUserIconMap = new HashMap<>();

        if (!isInEditMode()) {
            widgetModel = new UserAccountLoginWidgetModel(DJISDKModel.getInstance(),
                    ObservableInMemoryKeyedStore.getInstance(),
                    UserAccountManager.getInstance());
        }
        setOnClickListener(this);
        initDefaults();

        if (attrs != null) {
            initAttributes(context, attrs);
        }
    }

    @Override
    protected void reactToModelChanges() {
        addReaction(widgetModel.getUserAccountInformation().observeOn(SchedulerProvider.ui()).subscribe(loginInfo -> {
                    if (loginInfo != null) {
                        currentLoginInfo = loginInfo;
                        updateUI();
                    }
                }

        ));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            widgetModel.setup();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            widgetModel.cleanup();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onClick(View v) {
        if (currentLoginInfo.getLoginState() == LoginState.LOGGED_IN) {
            logoutUser();
        } else {
            loginUser();
        }
    }

    @NonNull
    @Override
    public String getIdealDimensionRatioString() {
        return getResources().getString(R.string.uxsdk_widget_user_account_login_ratio);
    }


    private void loginUser() {
        Context ctx = getContext();
        if (!(ctx instanceof FragmentActivity) || ((FragmentActivity) ctx).isFinishing()) {
            LogUtils.e(TAG, "Context is not activity or had finish: " + ctx);
        } else {
            addDisposable(widgetModel.loginUser((FragmentActivity) ctx)
                    .observeOn(SchedulerProvider.ui())
                    .subscribe(() -> {
                    }, error -> {
                        LogUtils.e(TAG, "login error:" + error);
                    }));
        }

    }

    private void logoutUser() {
        addDisposable(widgetModel.logoutUser().observeOn(SchedulerProvider.ui()).subscribe(() -> {
        }, error -> {
            if (error instanceof UXSDKError) {
                LogUtils.e(TAG, error.toString());
            }
        }));
    }

    private void updateUI() {
        if (currentLoginInfo == null) {
            LogUtils.e(TAG, "currentLoginInfo == null");
            return;
        }
        switch (currentLoginInfo.getLoginState()) {
            case LOGGED_IN:
                widgetStateTextView.setText(currentLoginInfo.getAccount());
                widgetMessageTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_logged_in));
                break;
            case NOT_LOGGED_IN:
                widgetStateTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_login));
                widgetMessageTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_not_logged_in));
                break;
            case TOKEN_OUT_OF_DATE:
                widgetStateTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_refresh));
                widgetMessageTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_token));
                break;
            case UNKNOWN:
            default:
                return;
        }

        widgetMessageTextView.setTextColor(getWidgetMessageTextColor(currentLoginInfo.getLoginState()));
        widgetActionImageView.setImageDrawable(getActionIcon(currentLoginInfo.getLoginState()));
        widgetUserImageView.setImageDrawable(getUserIcon(currentLoginInfo.getLoginState()));
        widgetStateTextView.setTextColor(getWidgetStateTextColor(currentLoginInfo.getLoginState()));
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.UserAccountLoginWidget);
        initActionIcon(typedArray);
        initStateText(typedArray);
        initMessageTest(typedArray);
        typedArray.recycle();
    }

    private void initStateText(TypedArray typedArray) {
        int textAppearance = typedArray.getResourceId(R.styleable.UserAccountLoginWidget_uxsdk_stateTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setWidgetStateTextAppearance(textAppearance);
        }

        int color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_stateTextColorLoggedIn, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setWidgetStateTextColor(LoginState.LOGGED_IN, color);
        }
        color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_stateTextColorNotLoggedIn, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setWidgetStateTextColor(LoginState.NOT_LOGGED_IN, color);
        }
        color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_stateTextColorTokenOutOfDate, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setWidgetStateTextColor(LoginState.TOKEN_OUT_OF_DATE, color);
        }

        setWidgetStateTextEnabled(typedArray.getBoolean(R.styleable.UserAccountLoginWidget_uxsdk_stateTextEnabled,
                true));
    }

    private void initMessageTest(TypedArray typedArray) {
        int textAppearance = typedArray.getResourceId(R.styleable.UserAccountLoginWidget_uxsdk_messageTextAppearance, INVALID_RESOURCE);
        if (textAppearance != INVALID_RESOURCE) {
            setWidgetMessageTextAppearance(textAppearance);
        }
        int color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_messageTextColorLoggedIN, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setWidgetMessageTextColor(LoginState.LOGGED_IN, color);
        }
        color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_messageTextColorNotLoggedIn, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setWidgetMessageTextColor(LoginState.NOT_LOGGED_IN, color);
        }
        color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_messageTextColorTokenOutOfDate, INVALID_COLOR);
        if (color != INVALID_COLOR) {
            setWidgetMessageTextColor(LoginState.TOKEN_OUT_OF_DATE, color);
        }
        setWidgetMessageTextSize(typedArray.getDimension(R.styleable.UserAccountLoginWidget_uxsdk_messageTextSize,
                getResources().getDimension(R.dimen.uxsdk_user_account_message_text_size)));
        setWidgetMessageBackground(typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_messageTextBackground));
        setWidgetStateTextSize(typedArray.getDimension(R.styleable.UserAccountLoginWidget_uxsdk_messageTextSize,
                getResources().getDimension(R.dimen.uxsdk_user_account_state_text_size)));
        if (typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_messageTextBackground) != null) {
            setWidgetStateBackground(typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_messageTextBackground));
        }
        initActionIcon(typedArray);
        setMessageTextEnabled(typedArray.getBoolean(R.styleable.UserAccountLoginWidget_uxsdk_messageTextEnabled,
                true));
    }

    private void initActionIcon(TypedArray typedArray) {
        Drawable drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconLoggedIn);
        if (drawable != null) {
            setActionIcon(LoginState.LOGGED_IN, drawable);
        }

        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconNotLoggedIn);
        if (drawable != null) {
            setActionIcon(LoginState.NOT_LOGGED_IN, drawable);
        }

        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconTokenOutOfDate);
        if (drawable != null) {
            setActionIcon(LoginState.TOKEN_OUT_OF_DATE, drawable);
        }
        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconLoggedIn);
        if (drawable != null) {
            setUserIcon(LoginState.LOGGED_IN, drawable);
        }

        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconNotLoggedIn);
        if (drawable != null) {
            setUserIcon(LoginState.NOT_LOGGED_IN, drawable);
        }

        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconTokenOutOfDate);
        if (drawable != null) {
            setUserIcon(LoginState.TOKEN_OUT_OF_DATE, drawable);
        }

        if (typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconBackground) != null) {
            setUserIconBackground(typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconBackground));
        }
        if (typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconBackground) != null) {
            setActionIconBackground(typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconBackground));
        }
        setUserIconEnabled(typedArray.getBoolean(R.styleable.UserAccountLoginWidget_uxsdk_userIconEnabled, true));
        setActionIconEnabled(typedArray.getBoolean(R.styleable.UserAccountLoginWidget_uxsdk_actionIconEnabled, true));
    }

    private void initDefaults() {
        if (isInEditMode()) {
            return;
        }
        setWidgetStateTextColor(LoginState.NOT_LOGGED_IN, getResources().getColor(R.color.uxsdk_white_80_percent));
        setWidgetStateTextColor(LoginState.LOGGED_IN, getResources().getColor(R.color.uxsdk_white_80_percent));
        setWidgetStateTextColor(LoginState.TOKEN_OUT_OF_DATE, getResources().getColor(R.color.uxsdk_white_80_percent));
        setWidgetStateTextColor(LoginState.UNKNOWN, getResources().getColor(R.color.uxsdk_white_80_percent));

        setWidgetMessageTextColor(LoginState.NOT_LOGGED_IN, getResources().getColor(R.color.uxsdk_red_material_800));
        setWidgetMessageTextColor(LoginState.LOGGED_IN, getResources().getColor(R.color.uxsdk_green_material_400));
        setWidgetMessageTextColor(LoginState.TOKEN_OUT_OF_DATE, getResources().getColor(R.color.uxsdk_yellow_500));
        setWidgetMessageTextColor(LoginState.UNKNOWN, getResources().getColor(R.color.uxsdk_red_material_800));

        setUserIcon(LoginState.NOT_LOGGED_IN, getResources().getDrawable(R.drawable.uxsdk_ic_person));
        setUserIcon(LoginState.LOGGED_IN, getResources().getDrawable(R.drawable.uxsdk_ic_person));
        setUserIcon(LoginState.TOKEN_OUT_OF_DATE, getResources().getDrawable(R.drawable.uxsdk_ic_person));
        setUserIcon(LoginState.UNKNOWN, getResources().getDrawable(R.drawable.uxsdk_ic_person));

        setActionIcon(LoginState.NOT_LOGGED_IN, getResources().getDrawable(R.drawable.uxsdk_ic_person_log_in));
        setActionIcon(LoginState.LOGGED_IN, getResources().getDrawable(R.drawable.uxsdk_ic_person_log_out));
        setActionIcon(LoginState.TOKEN_OUT_OF_DATE, getResources().getDrawable(R.drawable.uxsdk_ic_person_log_out));
        setActionIcon(LoginState.UNKNOWN, getResources().getDrawable(R.drawable.uxsdk_ic_person_log_in));

        setWidgetStateTextSize(getResources().getDimension(R.dimen.uxsdk_user_account_state_text_size));
        setWidgetMessageTextSize(getResources().getDimension(R.dimen.uxsdk_user_account_message_text_size));
    }

    //endregion

    //region customizations

    /**
     * Set the color of the widget state text based on user login state
     *
     * @param userAccountState {@link LoginState} for which the color should be used
     * @param color            integer value of color to be used for the user login state
     */
    public void setWidgetStateTextColor(@NonNull LoginState userAccountState, @ColorInt int color) {
        widgetStateTextColorMap.put(userAccountState, color);
        updateUI();
    }

    /**
     * Get the color of the widget state text
     *
     * @param userAccountState {@link LoginState} for which the color is being used
     * @return integer value representing the color
     */
    @ColorInt
    public int getWidgetStateTextColor(@NonNull LoginState userAccountState) {
        return widgetStateTextColorMap.get(userAccountState);
    }

    /**
     * Set text appearance of the widget state text
     *
     * @param textAppearance style resource id for the text appearance
     */
    public void setWidgetStateTextAppearance(@StyleRes int textAppearance) {
        widgetStateTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get the current background of the widget state text
     *
     * @return Drawable representing widget state text background
     */
    @Nullable
    public Drawable getWidgetStateBackground() {
        return widgetStateTextView.getBackground();
    }

    /**
     * Set the background resource of the widget state text
     *
     * @param resourceId to be used as background of widget state text
     */
    public void setWidgetStateBackground(@DrawableRes int resourceId) {
        setWidgetStateBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set the background of the widget state text
     *
     * @param drawable drawable to be used as the background of widget state text
     */
    public void setWidgetStateBackground(@Nullable Drawable drawable) {
        widgetStateTextView.setBackground(drawable);
    }

    /**
     * Get the current text size of the widget state text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getWidgetStateTextSize() {
        return widgetStateTextView.getTextSize();
    }

    /**
     * Set the text size of widget state text
     *
     * @param textSize float value for size of widget state text
     */
    public void setWidgetStateTextSize(@Dimension float textSize) {
        widgetStateTextView.setTextSize(textSize);
    }

    /**
     * Set the color of the widget message text based on user login state
     *
     * @param userAccountState {@link LoginState} for which the color should be used
     * @param color            integer value of color to be used for the message text
     */
    public void setWidgetMessageTextColor(@NonNull LoginState userAccountState, @ColorInt int color) {
        widgetMessageTextColorMap.put(userAccountState, color);
        updateUI();
    }

    /**
     * Get the color of the widget message text
     *
     * @param userAccountState {@link LoginState} for which the color is being used
     * @return integer value representing the color
     */
    @ColorInt
    public int getWidgetMessageTextColor(@NonNull LoginState userAccountState) {
        return widgetMessageTextColorMap.get(userAccountState);
    }

    /**
     * Set text appearance of the widget message text
     *
     * @param textAppearance style resource id for the text appearance
     */
    public void setWidgetMessageTextAppearance(@StyleRes int textAppearance) {
        widgetMessageTextView.setTextAppearance(getContext(), textAppearance);
    }

    /**
     * Get the current background of the widget message text
     *
     * @return Drawable representing widget message text background
     */
    @Nullable
    public Drawable getWidgetMessageBackground() {
        return widgetMessageTextView.getBackground();
    }

    /**
     * Set the background resource of the widget message text
     *
     * @param resourceId to be used as background of widget message text
     */
    public void setWidgetMessageBackground(@DrawableRes int resourceId) {
        setWidgetMessageBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set the background of the widget message text
     *
     * @param drawable Drawable to be used as the background of widget message text
     */
    public void setWidgetMessageBackground(@Nullable Drawable drawable) {
        widgetMessageTextView.setBackground(drawable);
    }

    /**
     * Get the current text size of the widget message text
     *
     * @return float value representing text size
     */
    @Dimension
    public float getWidgetMessageTextSize() {
        return widgetMessageTextView.getTextSize();
    }

    /**
     * Set the text size of widget message text
     *
     * @param textSize float value for size of widget message text
     */
    public void setWidgetMessageTextSize(@Dimension float textSize) {
        widgetMessageTextView.setTextSize(textSize);
    }

    /**
     * Set icon for the action based on {@link LoginState}
     *
     * @param userAccountState {@link LoginState} for which icon should be used
     * @param resourceId       resource id for the icon
     */
    public void setActionIcon(@NonNull LoginState userAccountState, @DrawableRes int resourceId) {
        setActionIcon(userAccountState, getResources().getDrawable(resourceId));
    }

    /**
     * Set icon for the action based on {@link LoginState}
     *
     * @param userAccountState {@link LoginState} for which icon should be used
     * @param drawable         drawable for the icon
     */
    public void setActionIcon(@NonNull LoginState userAccountState, @Nullable Drawable drawable) {
        widgetActionIconMap.put(userAccountState, drawable);
        updateUI();
    }

    /**
     * Get current icon for the action based on {@link LoginState}
     *
     * @param userAccountState {@link LoginState} for which icon is used
     * @return drawable used as action icon
     */
    public Drawable getActionIcon(@NonNull LoginState userAccountState) {
        return widgetActionIconMap.get(userAccountState);
    }

    /**
     * Get current background of action icon
     *
     * @return drawable used as background of icon
     */
    public Drawable getActionIconBackground() {
        return widgetActionImageView.getBackground();
    }

    /**
     * Set the background of the action icon
     *
     * @param resourceId to be used as background of action icon
     */
    public void setActionIconBackground(@DrawableRes int resourceId) {
        setActionIconBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set the background of the action icon
     *
     * @param drawable to be used as background of action icon
     */
    public void setActionIconBackground(@Nullable Drawable drawable) {
        widgetActionImageView.setBackground(drawable);
    }

    /**
     * Set icon for the user based on {@link LoginState}
     *
     * @param userAccountState {@link LoginState} for which icon should be used
     * @param resourceId       resource id for the icon
     */
    public void setUserIcon(@NonNull LoginState userAccountState, @DrawableRes int resourceId) {
        setUserIcon(userAccountState, getResources().getDrawable(resourceId));
    }

    /**
     * Set icon for the user based on {@link LoginState}
     *
     * @param userAccountState {@link LoginState} for which icon should be used
     * @param drawable         drawable for the icon
     */
    public void setUserIcon(@NonNull LoginState userAccountState, @Nullable Drawable drawable) {
        widgetUserIconMap.put(userAccountState, drawable);
        updateUI();
    }

    /**
     * Get current icon for the user based on {@link LoginState}
     *
     * @param userAccountState {@link LoginState} for which icon is used
     * @return drawable used as user icon
     */
    public Drawable getUserIcon(@NonNull LoginState userAccountState) {
        return widgetUserIconMap.get(userAccountState);
    }

    /**
     * Get current background of user icon
     *
     * @return drawable used as background of icon
     */
    public Drawable getUserIconBackground() {
        return widgetUserImageView.getBackground();
    }

    /**
     * Set the background of the user icon
     *
     * @param resourceId to be used as background of user icon
     */
    public void setUserIconBackground(@DrawableRes int resourceId) {
        setUserIconBackground(getResources().getDrawable(resourceId));
    }

    /**
     * Set the background of the user icon
     *
     * @param drawable to be used as background of user icon
     */
    public void setUserIconBackground(@Nullable Drawable drawable) {
        widgetUserImageView.setBackground(drawable);
    }

    /**
     * Check if action icon is enabled
     *
     * @return boolean value true - visible  false - not visible
     */
    public boolean isActionIconEnabled() {
        return widgetActionImageView.getVisibility() == VISIBLE;
    }

    /**
     * Enable action icon to show/hide user icon
     *
     * @param isEnabled boolean flag true - visible false - not visible
     */
    public void setActionIconEnabled(boolean isEnabled) {
        widgetActionImageView.setVisibility(isEnabled ? VISIBLE : GONE);
        widgetDivider.setVisibility(isEnabled ? VISIBLE : GONE);
    }

    /**
     * Check if user icon is enabled
     *
     * @return boolean value true - visible  false - not visible
     */
    public boolean isUserIconEnabled() {
        return widgetUserImageView.getVisibility() == VISIBLE;
    }

    /**
     * Enable user icon to show/hide user icon
     *
     * @param isEnabled boolean flag true - visible false - not visible
     */
    public void setUserIconEnabled(boolean isEnabled) {
        widgetUserImageView.setVisibility(isEnabled ? VISIBLE : GONE);
        widgetDivider.setVisibility(isEnabled ? VISIBLE : GONE);
    }

    /**
     * Check if message text is enabled
     *
     * @return boolean value true - visible  false - not visible
     */
    public boolean isMessageTextEnabled() {
        return widgetMessageTextView.getVisibility() == VISIBLE;
    }

    /**
     * Enable widget message text to show/hide message state text
     */
    public void setMessageTextEnabled(boolean isEnabled) {
        widgetMessageTextView.setVisibility(isEnabled ? VISIBLE : GONE);
    }

    /**
     * Check if widget state text is enabled
     *
     * @return boolean value true - visible  false - not visible
     */
    public boolean isWidgetStateTextEnabled() {
        return widgetStateTextView.getVisibility() == VISIBLE;
    }

    /**
     * Enable widget state text to show/hide widget state text
     *
     * @param isEnabled boolean flag true - visible false - not visible
     */
    public void setWidgetStateTextEnabled(boolean isEnabled) {
        widgetStateTextView.setVisibility(isEnabled ? VISIBLE : GONE);
    }
    //
    //    //endregion


}
