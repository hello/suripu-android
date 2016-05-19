package is.hello.sense.graph.presenters;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;

import javax.inject.Inject;

import is.hello.sense.api.fb.FacebookApiService;
import is.hello.sense.api.fb.model.FacebookProfilePicture;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.Logger;
import rx.Observable;

public class FacebookPresenter extends ValuePresenter<FacebookProfilePicture> {


    @Inject FacebookApiService apiService;
    @Inject CallbackManager callbackManager;

    public final PresenterSubject<FacebookProfilePicture> profilePicture = this.subject;
    @NonNull private final Context context;

    public @Inject FacebookPresenter(@NonNull Context context){
        this.context = context;
    }

    @Override
    protected boolean isDataDisposable() {
        return true;
    }

    @Override
    protected boolean canUpdate() {
        return true;
    }

    @Override
    protected Observable<FacebookProfilePicture> provideUpdateObservable() {
        return apiService.getProfilePicture("0", "large", getAuthTokenString())
                         .doOnNext(profilePicture ->
                                           logEvent("fetched profile picture from facebook")
                                  );
    }

    //region Updates
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode,resultCode, data);
    }

    public void setAuthToken(AccessToken token){
        AccessToken.setCurrentAccessToken(token);
    }

    public void init() {
        LoginManager.getInstance()
                    .registerCallback(
                            callbackManager,
                            new FacebookCallback<LoginResult>() {
                                @Override
                                public void onSuccess(LoginResult loginResult) {
                                    // App code
                                    setAuthToken(loginResult.getAccessToken());
                                    FacebookPresenter.this.update();
                                }

                                @Override
                                public void onCancel() {
                                    // App code
                                }

                                @Override
                                public void onError(FacebookException exception) {
                                    // App code
                                    Logger.debug(FacebookPresenter.class.getSimpleName(), "login failed", exception.fillInStackTrace());
                                }
                            });
    }

    public void login(Fragment container) {
        LoginManager.getInstance().logInWithReadPermissions(container, Arrays.asList("public_profile"));
    }

    public boolean isLoggedIn(){
        return profilePicture.hasValue() && AccessToken.getCurrentAccessToken() != null;
    }

    public void logout() {
        AccessToken.setCurrentAccessToken(null);
        profilePicture.forget();
    }

    //endregion


    private String getAuthTokenString(){
        return String.format("Bearer %s", AccessToken.getCurrentAccessToken().getToken());
    }
}
