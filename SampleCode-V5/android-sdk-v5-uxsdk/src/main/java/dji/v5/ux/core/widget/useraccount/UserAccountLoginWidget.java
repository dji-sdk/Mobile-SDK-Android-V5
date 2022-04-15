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
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import dji.v5.ux.core.base.widget.ConstraintLayoutWidget;

/**
 * User Account Login Widget
 * <p>
 * Widget will display the current login status of the user's DJI account.
 * Tapping on the widget will provide the option to login or logout of the DJI account.
 */
public class UserAccountLoginWidget extends ConstraintLayoutWidget implements OnClickListener {
//    //region Fields
//    private static final String TAG = "LoginWidget";
//    private TextView widgetStateTextView;
//    private ImageView widgetActionImageView;
//    private ImageView widgetUserImageView;
//    private TextView widgetMessageTextView;
//    private View widgetDivider;
//    private UserAccountLoginWidgetModel widgetModel;
//    private Map<UserAccountState, Integer> widgetStateTextColorMap;
//    private Map<UserAccountState, Integer> widgetMessageTextColorMap;
//    private Map<UserAccountState, Drawable> widgetActionIconMap;
//    private Map<UserAccountState, Drawable> widgetUserIconMap;
//    private OnStateChangeCallback onStateChangeCallback;
//
//    //endregion
//
//    //region Lifecycle
//    public UserAccountLoginWidget(Context context) {
//        super(context);
//    }
//
//    public UserAccountLoginWidget(Context context, AttributeSet attrs) {
//        super(context, attrs);
//    }
//
//    public UserAccountLoginWidget(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//    }
//
//    @Override
//    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        inflate(context, R.layout.uxsdk_widget_user_account_login, this);
//        setBackgroundResource(R.drawable.uxsdk_background_black_rectangle);
//        widgetDivider = findViewById(R.id.widget_divider);
//        widgetStateTextView = findViewById(R.id.textview_widget_status);
//        widgetActionImageView = findViewById(R.id.imageview_widget_status);
//        widgetUserImageView = findViewById(R.id.imageview_widget_user);
//        widgetMessageTextView = findViewById(R.id.textview_widget_message);
//        widgetStateTextColorMap = new HashMap<>();
//        widgetMessageTextColorMap = new HashMap<>();
//        widgetActionIconMap = new HashMap<>();
//        widgetUserIconMap = new HashMap<>();
//
//        if (!isInEditMode()) {
//            widgetModel = new UserAccountLoginWidgetModel(DJISDKModel.getInstance(),
//                    ObservableInMemoryKeyedStore.getInstance(),
//                    UserAccountManager.getInstance());
//        }
//        setOnClickListener(this);
//        initDefaults();
//
//        if (attrs != null) {
//            initAttributes(context, attrs);
//        }
//    }
//
//    @Override
//    protected void reactToModelChanges() {
//        addReaction(reactToAccountStateChange());
//    }
//
//    @Override
//    protected void onAttachedToWindow() {
//        super.onAttachedToWindow();
//        if (!isInEditMode()) {
//            widgetModel.setup();
//        }
//    }
//
//    @Override
//    protected void onDetachedFromWindow() {
//        if (!isInEditMode()) {
//            widgetModel.cleanup();
//        }
//        super.onDetachedFromWindow();
//    }
//
//    @Override
//    public void onClick(View v) {
//        addDisposable(widgetModel.getUserAccountState()
//                .firstOrError()
//                .observeOn(SchedulerProvider.ui())
//                .subscribe(userAccountState -> {
//                    if (userAccountState == UserAccountState.AUTHORIZED) {
//                        logoutUser();
//                    } else {
//                        loginUser();
//                    }
//                }, RxUtil.logErrorConsumer(TAG, "TAP LOGIN ")));
//    }
//
//    @NonNull
//    @Override
//    public String getIdealDimensionRatioString() {
//        return getResources().getString(R.string.uxsdk_widget_user_account_login_ratio);
//    }
//
//    public void setOnStateChangeCallback(OnStateChangeCallback onStateChangeCallback) {
//        this.onStateChangeCallback = onStateChangeCallback;
//    }
//
//    //endregion
//
//    //region private methods
//    private Flowable<Pair<UserAccountState, UserAccountInformation>> getAccountState() {
//        return Flowable.combineLatest(widgetModel.getUserAccountState(),
//                widgetModel.getUserAccountInformation(),
//                Pair::create);
//    }
//
//    private Disposable reactToAccountStateChange() {
//        return getAccountState()
//                .observeOn(SchedulerProvider.ui())
//                .subscribe(values -> updateUI(values.first, values.second),
//                        RxUtil.logErrorConsumer(TAG, "react to User Account "));
//    }
//
//    private void checkAndUpdateUI() {
//        if (!isInEditMode()) {
//            addDisposable(getAccountState().firstOrError()
//                    .observeOn(SchedulerProvider.ui())
//                    .subscribe(values -> updateUI(values.first, values.second),
//                            RxUtil.logErrorConsumer(TAG, "react to User Account ")));
//        }
//    }
//
//    private void loginUser() {
//        addDisposable(widgetModel.loginUser(getContext())
//                .observeOn(SchedulerProvider.ui())
//                .subscribe(() -> {
//                }, error -> {
//                    if (error instanceof UXSDKError) {
//                        DJILog.e(TAG, error.toString());
//                    }
//                }));
//    }
//
//    private void logoutUser() {
//        addDisposable(widgetModel.logoutUser().observeOn(SchedulerProvider.ui()).subscribe(() -> {
//
//        }, error -> {
//            if (error instanceof UXSDKError) {
//                DJILog.e(TAG, error.toString());
//            }
//        }));
//    }
//
//    private void updateUI(UserAccountState userAccountState, UserAccountInformation userAccountInformation) {
//        if (onStateChangeCallback != null) {
//            onStateChangeCallback.onStateChange(userAccountState);
//        }
//        switch (userAccountState) {
//            case AUTHORIZED:
//                if (userAccountInformation != null && !userAccountInformation.getAccount().isEmpty()) {
//                    widgetStateTextView.setText(userAccountInformation.getAccount());
//                } else {
//                    widgetStateTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_logout));
//                }
//                widgetMessageTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_logged_in_authorized));
//
//                break;
//            case NOT_AUTHORIZED:
//                widgetStateTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_logout));
//                widgetMessageTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_logged_in_not_authorized));
//                break;
//            case NOT_LOGGED_IN:
//                widgetStateTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_login));
//                widgetMessageTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_not_logged_in));
//                break;
//            case TOKEN_OUT_OF_DATE:
//                widgetStateTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_refresh));
//                widgetMessageTextView.setText(getResources().getString(R.string.uxsdk_user_login_widget_token));
//                break;
//            case UNKNOWN:
//            default:
//                return;
//        }
//
//        widgetMessageTextView.setTextColor(getWidgetMessageTextColor(userAccountState));
//        widgetActionImageView.setImageDrawable(getActionIcon(userAccountState));
//        widgetUserImageView.setImageDrawable(getUserIcon(userAccountState));
//        widgetStateTextView.setTextColor(getWidgetStateTextColor(userAccountState));
//    }
//
//    private void initAttributes(Context context, AttributeSet attrs) {
//        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.UserAccountLoginWidget);
//        int textAppearance =
//                typedArray.getResourceId(R.styleable.UserAccountLoginWidget_uxsdk_messageTextAppearance, INVALID_RESOURCE);
//        if (textAppearance != INVALID_RESOURCE) {
//            setWidgetMessageTextAppearance(textAppearance);
//        }
//        int color =
//                typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_messageTextColorAuthorized, INVALID_COLOR);
//        if (color != INVALID_COLOR) {
//            setWidgetMessageTextColor(UserAccountState.AUTHORIZED, color);
//        }
//        color =
//                typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_messageTextColorNotAuthorized, INVALID_COLOR);
//        if (color != INVALID_COLOR) {
//            setWidgetMessageTextColor(UserAccountState.NOT_AUTHORIZED, color);
//        }
//        color =
//                typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_messageTextColorNotLoggedIn, INVALID_COLOR);
//        if (color != INVALID_COLOR) {
//            setWidgetMessageTextColor(UserAccountState.NOT_LOGGED_IN, color);
//        }
//        color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_messageTextColorTokenOutOfDate,
//                INVALID_COLOR);
//        if (color != INVALID_COLOR) {
//            setWidgetMessageTextColor(UserAccountState.TOKEN_OUT_OF_DATE, color);
//        }
//        setWidgetMessageTextSize(typedArray.getDimension(R.styleable.UserAccountLoginWidget_uxsdk_messageTextSize,
//                getResources().getDimension(R.dimen.uxsdk_user_account_message_text_size)));
//        setWidgetMessageBackground(typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_messageTextBackground));
//
//        textAppearance =
//                typedArray.getResourceId(R.styleable.UserAccountLoginWidget_uxsdk_stateTextAppearance, INVALID_RESOURCE);
//        if (textAppearance != INVALID_RESOURCE) {
//            setWidgetStateTextAppearance(textAppearance);
//        }
//
//        color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_stateTextColorAuthorized, INVALID_COLOR);
//        if (color != INVALID_COLOR) {
//            setWidgetStateTextColor(UserAccountState.AUTHORIZED, color);
//        }
//        color =
//                typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_stateTextColorNotAuthorized, INVALID_COLOR);
//        if (color != INVALID_COLOR) {
//            setWidgetStateTextColor(UserAccountState.NOT_AUTHORIZED, color);
//        }
//        color = typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_stateTextColorNotLoggedIn, INVALID_COLOR);
//        if (color != INVALID_COLOR) {
//            setWidgetStateTextColor(UserAccountState.NOT_LOGGED_IN, color);
//        }
//        color =
//                typedArray.getColor(R.styleable.UserAccountLoginWidget_uxsdk_stateTextColorTokenOutOfDate, INVALID_COLOR);
//        if (color != INVALID_COLOR) {
//            setWidgetStateTextColor(UserAccountState.TOKEN_OUT_OF_DATE, color);
//        }
//        setWidgetStateTextSize(typedArray.getDimension(R.styleable.UserAccountLoginWidget_uxsdk_messageTextSize,
//                getResources().getDimension(R.dimen.uxsdk_user_account_state_text_size)));
//        if (typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_messageTextBackground) != null) {
//            setWidgetStateBackground(typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_messageTextBackground));
//        }
//        Drawable drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconAuthorized);
//        if (drawable != null) {
//            setActionIcon(UserAccountState.AUTHORIZED, drawable);
//        }
//
//        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconNotAuthorized);
//        if (drawable != null) {
//            setActionIcon(UserAccountState.NOT_AUTHORIZED, drawable);
//        }
//
//        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconNotLoggedIn);
//        if (drawable != null) {
//            setActionIcon(UserAccountState.NOT_LOGGED_IN, drawable);
//        }
//
//        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconTokenOutOfDate);
//        if (drawable != null) {
//            setActionIcon(UserAccountState.TOKEN_OUT_OF_DATE, drawable);
//        }
//        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconAuthorized);
//        if (drawable != null) {
//            setUserIcon(UserAccountState.AUTHORIZED, drawable);
//        }
//
//        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconNotAuthorized);
//        if (drawable != null) {
//            setUserIcon(UserAccountState.NOT_AUTHORIZED, drawable);
//        }
//
//        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconNotLoggedIn);
//        if (drawable != null) {
//            setUserIcon(UserAccountState.NOT_LOGGED_IN, drawable);
//        }
//
//        drawable = typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconTokenOutOfDate);
//        if (drawable != null) {
//            setUserIcon(UserAccountState.TOKEN_OUT_OF_DATE, drawable);
//        }
//
//        if (typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconBackground) != null) {
//            setUserIconBackground(typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_userIconBackground));
//        }
//        if (typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconBackground) != null) {
//            setActionIconBackground(typedArray.getDrawable(R.styleable.UserAccountLoginWidget_uxsdk_actionIconBackground));
//        }
//        setMessageTextEnabled(typedArray.getBoolean(R.styleable.UserAccountLoginWidget_uxsdk_messageTextEnabled,
//                true));
//        setWidgetStateTextEnabled(typedArray.getBoolean(R.styleable.UserAccountLoginWidget_uxsdk_stateTextEnabled,
//                true));
//        setUserIconEnabled(typedArray.getBoolean(R.styleable.UserAccountLoginWidget_uxsdk_userIconEnabled, true));
//        setActionIconEnabled(typedArray.getBoolean(R.styleable.UserAccountLoginWidget_uxsdk_actionIconEnabled, true));
//        typedArray.recycle();
//    }
//
//    private void initDefaults() {
//        if (isInEditMode()){
//            return;
//        }
//        setWidgetStateTextColor(UserAccountState.NOT_LOGGED_IN,
//                getResources().getColor(R.color.uxsdk_white_80_percent));
//        setWidgetStateTextColor(UserAccountState.NOT_AUTHORIZED,
//                getResources().getColor(R.color.uxsdk_white_80_percent));
//        setWidgetStateTextColor(UserAccountState.AUTHORIZED, getResources().getColor(R.color.uxsdk_white_80_percent));
//        setWidgetStateTextColor(UserAccountState.TOKEN_OUT_OF_DATE,
//                getResources().getColor(R.color.uxsdk_white_80_percent));
//        setWidgetStateTextColor(UserAccountState.UNKNOWN, getResources().getColor(R.color.uxsdk_white_80_percent));
//
//        setWidgetMessageTextColor(UserAccountState.NOT_LOGGED_IN,
//                getResources().getColor(R.color.uxsdk_red_material_800));
//        setWidgetMessageTextColor(UserAccountState.NOT_AUTHORIZED, getResources().getColor(R.color.uxsdk_yellow_500));
//        setWidgetMessageTextColor(UserAccountState.AUTHORIZED,
//                getResources().getColor(R.color.uxsdk_green_material_400));
//        setWidgetMessageTextColor(UserAccountState.TOKEN_OUT_OF_DATE,
//                getResources().getColor(R.color.uxsdk_yellow_500));
//        setWidgetMessageTextColor(UserAccountState.UNKNOWN, getResources().getColor(R.color.uxsdk_red_material_800));
//
//        setUserIcon(UserAccountState.NOT_LOGGED_IN, getResources().getDrawable(R.drawable.uxsdk_ic_person));
//        setUserIcon(UserAccountState.NOT_AUTHORIZED, getResources().getDrawable(R.drawable.uxsdk_ic_person));
//        setUserIcon(UserAccountState.AUTHORIZED, getResources().getDrawable(R.drawable.uxsdk_ic_person));
//        setUserIcon(UserAccountState.TOKEN_OUT_OF_DATE, getResources().getDrawable(R.drawable.uxsdk_ic_person));
//        setUserIcon(UserAccountState.UNKNOWN, getResources().getDrawable(R.drawable.uxsdk_ic_person));
//
//        setActionIcon(UserAccountState.NOT_LOGGED_IN, getResources().getDrawable(R.drawable.uxsdk_ic_person_log_in));
//        setActionIcon(UserAccountState.NOT_AUTHORIZED, getResources().getDrawable(R.drawable.uxsdk_ic_person_log_out));
//        setActionIcon(UserAccountState.AUTHORIZED, getResources().getDrawable(R.drawable.uxsdk_ic_person_log_out));
//        setActionIcon(UserAccountState.TOKEN_OUT_OF_DATE,
//                getResources().getDrawable(R.drawable.uxsdk_ic_person_log_out));
//        setActionIcon(UserAccountState.UNKNOWN, getResources().getDrawable(R.drawable.uxsdk_ic_person_log_in));
//
//        setWidgetStateTextSize(getResources().getDimension(R.dimen.uxsdk_user_account_state_text_size));
//        setWidgetMessageTextSize(getResources().getDimension(R.dimen.uxsdk_user_account_message_text_size));
//    }
//
//    //endregion
//
//    //region customizations
//
//    /**
//     * Set the color of the widget state text based on user login state
//     *
//     * @param userAccountState {@link UserAccountState} for which the color should be used
//     * @param color            integer value of color to be used for the user login state
//     */
//    public void setWidgetStateTextColor(@NonNull UserAccountState userAccountState, @ColorInt int color) {
//        widgetStateTextColorMap.put(userAccountState, color);
//        checkAndUpdateUI();
//    }
//
//    /**
//     * Get the color of the widget state text
//     *
//     * @param userAccountState {@link UserAccountState} for which the color is being used
//     * @return integer value representing the color
//     */
//    @ColorInt
//    public int getWidgetStateTextColor(@NonNull UserAccountState userAccountState) {
//        return widgetStateTextColorMap.get(userAccountState);
//    }
//
//    /**
//     * Set text appearance of the widget state text
//     *
//     * @param textAppearance style resource id for the text appearance
//     */
//    public void setWidgetStateTextAppearance(@StyleRes int textAppearance) {
//        widgetStateTextView.setTextAppearance(getContext(), textAppearance);
//    }
//
//    /**
//     * Get the current background of the widget state text
//     *
//     * @return Drawable representing widget state text background
//     */
//    @Nullable
//    public Drawable getWidgetStateBackground() {
//        return widgetStateTextView.getBackground();
//    }
//
//    /**
//     * Set the background resource of the widget state text
//     *
//     * @param resourceId to be used as background of widget state text
//     */
//    public void setWidgetStateBackground(@DrawableRes int resourceId) {
//        setWidgetStateBackground(getResources().getDrawable(resourceId));
//    }
//
//    /**
//     * Set the background of the widget state text
//     *
//     * @param drawable drawable to be used as the background of widget state text
//     */
//    public void setWidgetStateBackground(@Nullable Drawable drawable) {
//        widgetStateTextView.setBackground(drawable);
//    }
//
//    /**
//     * Get the current text size of the widget state text
//     *
//     * @return float value representing text size
//     */
//    @Dimension
//    public float getWidgetStateTextSize() {
//        return widgetStateTextView.getTextSize();
//    }
//
//    /**
//     * Set the text size of widget state text
//     *
//     * @param textSize float value for size of widget state text
//     */
//    public void setWidgetStateTextSize(@Dimension float textSize) {
//        widgetStateTextView.setTextSize(textSize);
//    }
//
//    /**
//     * Set the color of the widget message text based on user login state
//     *
//     * @param userAccountState {@link UserAccountState} for which the color should be used
//     * @param color            integer value of color to be used for the message text
//     */
//    public void setWidgetMessageTextColor(@NonNull UserAccountState userAccountState, @ColorInt int color) {
//        widgetMessageTextColorMap.put(userAccountState, color);
//        checkAndUpdateUI();
//    }
//
//    /**
//     * Get the color of the widget message text
//     *
//     * @param userAccountState {@link UserAccountState} for which the color is being used
//     * @return integer value representing the color
//     */
//    @ColorInt
//    public int getWidgetMessageTextColor(@NonNull UserAccountState userAccountState) {
//        return widgetMessageTextColorMap.get(userAccountState);
//    }
//
//    /**
//     * Set text appearance of the widget message text
//     *
//     * @param textAppearance style resource id for the text appearance
//     */
//    public void setWidgetMessageTextAppearance(@StyleRes int textAppearance) {
//        widgetMessageTextView.setTextAppearance(getContext(), textAppearance);
//    }
//
//    /**
//     * Get the current background of the widget message text
//     *
//     * @return Drawable representing widget message text background
//     */
//    @Nullable
//    public Drawable getWidgetMessageBackground() {
//        return widgetMessageTextView.getBackground();
//    }
//
//    /**
//     * Set the background resource of the widget message text
//     *
//     * @param resourceId to be used as background of widget message text
//     */
//    public void setWidgetMessageBackground(@DrawableRes int resourceId) {
//        setWidgetMessageBackground(getResources().getDrawable(resourceId));
//    }
//
//    /**
//     * Set the background of the widget message text
//     *
//     * @param drawable Drawable to be used as the background of widget message text
//     */
//    public void setWidgetMessageBackground(@Nullable Drawable drawable) {
//        widgetMessageTextView.setBackground(drawable);
//    }
//
//    /**
//     * Get the current text size of the widget message text
//     *
//     * @return float value representing text size
//     */
//    @Dimension
//    public float getWidgetMessageTextSize() {
//        return widgetMessageTextView.getTextSize();
//    }
//
//    /**
//     * Set the text size of widget message text
//     *
//     * @param textSize float value for size of widget message text
//     */
//    public void setWidgetMessageTextSize(@Dimension float textSize) {
//        widgetMessageTextView.setTextSize(textSize);
//    }
//
//    /**
//     * Set icon for the action based on {@link UserAccountState}
//     *
//     * @param userAccountState {@link UserAccountState} for which icon should be used
//     * @param resourceId       resource id for the icon
//     */
//    public void setActionIcon(@NonNull UserAccountState userAccountState, @DrawableRes int resourceId) {
//        setActionIcon(userAccountState, getResources().getDrawable(resourceId));
//    }
//
//    /**
//     * Set icon for the action based on {@link UserAccountState}
//     *
//     * @param userAccountState {@link UserAccountState} for which icon should be used
//     * @param drawable         drawable for the icon
//     */
//    public void setActionIcon(@NonNull UserAccountState userAccountState, @Nullable Drawable drawable) {
//        widgetActionIconMap.put(userAccountState, drawable);
//        checkAndUpdateUI();
//    }
//
//    /**
//     * Get current icon for the action based on {@link UserAccountState}
//     *
//     * @param userAccountState {@link UserAccountState} for which icon is used
//     * @return drawable used as action icon
//     */
//    public Drawable getActionIcon(@NonNull UserAccountState userAccountState) {
//        return widgetActionIconMap.get(userAccountState);
//    }
//
//    /**
//     * Get current background of action icon
//     *
//     * @return drawable used as background of icon
//     */
//    public Drawable getActionIconBackground() {
//        return widgetActionImageView.getBackground();
//    }
//
//    /**
//     * Set the background of the action icon
//     *
//     * @param resourceId to be used as background of action icon
//     */
//    public void setActionIconBackground(@DrawableRes int resourceId) {
//        setActionIconBackground(getResources().getDrawable(resourceId));
//    }
//
//    /**
//     * Set the background of the action icon
//     *
//     * @param drawable to be used as background of action icon
//     */
//    public void setActionIconBackground(@Nullable Drawable drawable) {
//        widgetActionImageView.setBackground(drawable);
//    }
//
//    /**
//     * Set icon for the user based on {@link UserAccountState}
//     *
//     * @param userAccountState {@link UserAccountState} for which icon should be used
//     * @param resourceId       resource id for the icon
//     */
//    public void setUserIcon(@NonNull UserAccountState userAccountState, @DrawableRes int resourceId) {
//        setUserIcon(userAccountState, getResources().getDrawable(resourceId));
//    }
//
//    /**
//     * Set icon for the user based on {@link UserAccountState}
//     *
//     * @param userAccountState {@link UserAccountState} for which icon should be used
//     * @param drawable         drawable for the icon
//     */
//    public void setUserIcon(@NonNull UserAccountState userAccountState, @Nullable Drawable drawable) {
//        widgetUserIconMap.put(userAccountState, drawable);
//        checkAndUpdateUI();
//    }
//
//    /**
//     * Get current icon for the user based on {@link UserAccountState}
//     *
//     * @param userAccountState {@link UserAccountState} for which icon is used
//     * @return drawable used as user icon
//     */
//    public Drawable getUserIcon(@NonNull UserAccountState userAccountState) {
//        return widgetUserIconMap.get(userAccountState);
//    }
//
//    /**
//     * Get current background of user icon
//     *
//     * @return drawable used as background of icon
//     */
//    public Drawable getUserIconBackground() {
//        return widgetUserImageView.getBackground();
//    }
//
//    /**
//     * Set the background of the user icon
//     *
//     * @param resourceId to be used as background of user icon
//     */
//    public void setUserIconBackground(@DrawableRes int resourceId) {
//        setUserIconBackground(getResources().getDrawable(resourceId));
//    }
//
//    /**
//     * Set the background of the user icon
//     *
//     * @param drawable to be used as background of user icon
//     */
//    public void setUserIconBackground(@Nullable Drawable drawable) {
//        widgetUserImageView.setBackground(drawable);
//    }
//
//    /**
//     * Check if action icon is enabled
//     *
//     * @return boolean value true - visible  false - not visible
//     */
//    public boolean isActionIconEnabled() {
//        return widgetActionImageView.getVisibility() == VISIBLE;
//    }
//
//    /**
//     * Enable action icon to show/hide user icon
//     *
//     * @param isEnabled boolean flag true - visible false - not visible
//     */
//    public void setActionIconEnabled(boolean isEnabled) {
//        widgetActionImageView.setVisibility(isEnabled ? VISIBLE : GONE);
//        widgetDivider.setVisibility(isEnabled ? VISIBLE : GONE);
//    }
//
//    /**
//     * Check if user icon is enabled
//     *
//     * @return boolean value true - visible  false - not visible
//     */
//    public boolean isUserIconEnabled() {
//        return widgetUserImageView.getVisibility() == VISIBLE;
//    }
//
//    /**
//     * Enable user icon to show/hide user icon
//     *
//     * @param isEnabled boolean flag true - visible false - not visible
//     */
//    public void setUserIconEnabled(boolean isEnabled) {
//        widgetUserImageView.setVisibility(isEnabled ? VISIBLE : GONE);
//        widgetDivider.setVisibility(isEnabled ? VISIBLE : GONE);
//    }
//
//    /**
//     * Check if message text is enabled
//     *
//     * @return boolean value true - visible  false - not visible
//     */
//    public boolean isMessageTextEnabled() {
//        return widgetMessageTextView.getVisibility() == VISIBLE;
//    }
//
//    /**
//     * Enable widget message text to show/hide message state text
//     */
//    public void setMessageTextEnabled(boolean isEnabled) {
//        widgetMessageTextView.setVisibility(isEnabled ? VISIBLE : GONE);
//    }
//
//    /**
//     * Check if widget state text is enabled
//     *
//     * @return boolean value true - visible  false - not visible
//     */
//    public boolean isWidgetStateTextEnabled() {
//        return widgetStateTextView.getVisibility() == VISIBLE;
//    }
//
//    /**
//     * Enable widget state text to show/hide widget state text
//     *
//     * @param isEnabled boolean flag true - visible false - not visible
//     */
//    public void setWidgetStateTextEnabled(boolean isEnabled) {
//        widgetStateTextView.setVisibility(isEnabled ? VISIBLE : GONE);
//    }
//
//    //endregion


    public UserAccountLoginWidget(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public UserAccountLoginWidget(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public UserAccountLoginWidget(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onClick(View v) {
//        LogUtils.d(TAG,MSG);
    }

    @Override
    protected void initView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        LogUtils.d(TAG,MSG);
    }

    @Override
    protected void reactToModelChanges() {
//        LogUtils.d(TAG,MSG);
    }

    @Nullable
    @Override
    public String getIdealDimensionRatioString() {
        return null;
    }
}
