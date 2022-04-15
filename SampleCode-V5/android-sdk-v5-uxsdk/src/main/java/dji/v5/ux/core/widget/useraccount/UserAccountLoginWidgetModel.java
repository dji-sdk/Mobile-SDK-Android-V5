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

import androidx.annotation.NonNull;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;

/**
 * User Account Login Widget Model
 * <p>
 * Widget Model for {@link UserAccountLoginWidget} used to define the
 * underlying logic and communication
 */
public class UserAccountLoginWidgetModel extends WidgetModel
       /** implements UserAccountManager.UserAccountStateChangeListener**/ {

//    //region Fields
//    private static final String TAG = "LoginWidgetModel";
//    private final DataProcessor<UserAccountState> userAccountStateDataProcessor;
//    private final DataProcessor<UserAccountInformation> userAccountInformationDataProcessor;
//    private UserAccountManager userAccountManager;
//
//    //endregion
//
//    public UserAccountLoginWidgetModel(@NonNull DJISDKModel djiSdkModel,
//                                       @NonNull ObservableInMemoryKeyedStore keyedStore,
//                                       @NonNull UserAccountManager userAccountManager) {
//        super(djiSdkModel, keyedStore);
//        this.userAccountManager = userAccountManager;
//        userAccountStateDataProcessor = DataProcessor.create(UserAccountState.UNKNOWN);
//        userAccountInformationDataProcessor = DataProcessor.create(new UserAccountInformation("", false));
//    }
//
//    @Override
//    protected void inSetup() {
//        if (userAccountManager != null) {
//            userAccountManager.addUserAccountStateChangeListener(this);
//            UserAccountState userAccountState = userAccountManager.getUserAccountState();
//            userAccountStateDataProcessor.onNext(userAccountState);
//            if (userAccountState == UserAccountState.AUTHORIZED) {
//                userAccountManager.getLoggedInDJIUserAccountName(new CommonCallbacks.CompletionCallbackWith<String>() {
//                    @Override
//                    public void onSuccess(String s) {
//                        userAccountInformationDataProcessor.onNext(new UserAccountInformation(s, true));
//                    }
//
//                    @Override
//                    public void onFailure(DJIError error) {
//                        DJILog.e(TAG, error.getDescription());
//                    }
//                });
//            }
//        }
//    }
//
//    @Override
//    protected void inCleanup() {
//        userAccountManager.removeUserAccountStateChangeListener(this);
//    }
//
//    @Override
//    protected void updateStates() {
//        //empty
//    }
//
//    @Override
//    public void onUserAccountStateChanged(UserAccountState state, UserAccountInformation information) {
//        userAccountStateDataProcessor.onNext(state);
//        userAccountInformationDataProcessor.onNext(information);
//    }
//
//    //region Data
//
//    /**
//     * Get user account information
//     *
//     * @return Flowable of type UserAccountInformation
//     */
//    public Flowable<UserAccountInformation> getUserAccountInformation() {
//        return userAccountInformationDataProcessor.toFlowable();
//    }
//
//    /**
//     * Get user account state
//     *
//     * @return Flowable of type UserAccountState
//     */
//    public Flowable<UserAccountState> getUserAccountState() {
//        return userAccountStateDataProcessor.toFlowable();
//    }
//
//    //endregion
//
//    //region Actions
//
//    /**
//     * Log into user account
//     *
//     * @param context for showing logging pop up
//     * @return completable indicating the success or failure
//     */
//    public Completable loginUser(@NonNull Context context) {
//
//        return Completable.create(emitter -> {
//            if (userAccountManager == null) {
//                emitter.onError(new UXSDKError(UXSDKErrorDescription.USER_ACCOUNT_MANAGER_ERROR));
//                return;
//            }
//            userAccountManager.logIntoDJIUserAccount(context,
//                    new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
//                        @Override
//                        public void onSuccess(UserAccountState userAccountState) {
//                            emitter.onComplete();
//                        }
//
//                        @Override
//                        public void onFailure(DJIError error) {
//                            if (!emitter.isDisposed()) {
//                                UXSDKError uxsdkError = new UXSDKError(error);
//                                emitter.onError(uxsdkError);
//                            }
//                        }
//                    });
//        });
//    }
//
//    /**
//     * Log out the current logged in user account
//     *
//     * @return completable indicating the success or failure
//     */
//    public Completable logoutUser() {
//        return Completable.create(emitter -> {
//            if (userAccountManager == null) {
//                emitter.onError(new UXSDKError(UXSDKErrorDescription.USER_ACCOUNT_MANAGER_ERROR));
//                return;
//            }
//            userAccountManager.logoutOfDJIUserAccount(error -> {
//                if (error == null) {
//                    emitter.onComplete();
//                } else {
//                    emitter.onError(new UXSDKError(error));
//                }
//            });
//        });
//    }

    //endregion


    public UserAccountLoginWidgetModel(@NonNull DJISDKModel djiSdkModel, @NonNull ObservableInMemoryKeyedStore uxKeyManager) {
        super(djiSdkModel, uxKeyManager);
    }

    @Override
    protected void inSetup() {
        //暂无实现
    }

    @Override
    protected void inCleanup() {
        //暂无实现
    }
}
