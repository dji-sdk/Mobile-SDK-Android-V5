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
import androidx.fragment.app.FragmentActivity;

import dji.v5.common.callback.CommonCallbacks;
import dji.v5.common.error.IDJIError;
import dji.v5.manager.account.LoginInfo;
import dji.v5.manager.account.LoginInfoUpdateListener;
import dji.v5.manager.interfaces.IUserAccountManager;
import dji.v5.ux.core.base.DJISDKModel;
import dji.v5.ux.core.base.UXSDKError;
import dji.v5.ux.core.base.WidgetModel;
import dji.v5.ux.core.communication.ObservableInMemoryKeyedStore;
import dji.v5.ux.core.util.DataProcessor;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;

/**
 * User Account Login Widget Model
 * <p>
 * Widget Model for {@link UserAccountLoginWidget} used to define the
 * underlying logic and communication
 */
public class UserAccountLoginWidgetModel extends WidgetModel {

    //region Fields
    private DataProcessor<LoginInfo> loginInfoDataProcessor;
    private IUserAccountManager userAccountManager;
    private LoginInfoUpdateListener loginInfoUpdateListener = loginInfo -> loginInfoDataProcessor.onNext(loginInfo);


    //endregion

    public UserAccountLoginWidgetModel(@NonNull DJISDKModel djiSdkModel,
                                       @NonNull ObservableInMemoryKeyedStore keyedStore,
                                       @NonNull IUserAccountManager userAccountManager) {
        super(djiSdkModel, keyedStore);
        this.userAccountManager = userAccountManager;
        loginInfoDataProcessor = DataProcessor.create(new LoginInfo());
    }


    @Override
    protected void inSetup() {
        if (userAccountManager != null) {
            userAccountManager.addLoginInfoUpdateListener(loginInfoUpdateListener);
        }
    }

    @Override
    protected void inCleanup() {
        userAccountManager.removeLoginInfoUpdateListener(loginInfoUpdateListener);
    }

    @Override
    protected void updateStates() {
        //empty
    }


    //region Data

    /**
     * Get user account information
     *
     * @return Flowable of type UserAccountInformation
     */
    public Flowable<LoginInfo> getUserAccountInformation() {
        return loginInfoDataProcessor.toFlowable();
    }


    //endregion

    //region Actions

    /**
     * Log into user account
     *
     * @param context for showing logging pop up
     * @return completable indicating the success or failure
     */
    public Completable loginUser(@NonNull FragmentActivity context) {

        return Completable.create(emitter -> userAccountManager.logInDJIUserAccount(context, false, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                emitter.onComplete();
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                if (!emitter.isDisposed()) {
                    UXSDKError uxsdkError = new UXSDKError(error);
                    emitter.onError(uxsdkError);
                }
            }
        }));
    }

    /**
     * Log out the current logged in user account
     *
     * @return completable indicating the success or failure
     */
    public Completable logoutUser() {
        return Completable.create(emitter -> userAccountManager.logOutDJIUserAccount(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onSuccess() {
                emitter.onComplete();
            }

            @Override
            public void onFailure(@NonNull IDJIError error) {
                emitter.onError(new UXSDKError(error));
            }
        }));
    }

    //endregion


}
