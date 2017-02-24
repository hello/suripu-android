package is.hello.sense.interactors;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import is.hello.buruberi.util.Rx;
import is.hello.sense.api.fb.FacebookApiService;
import is.hello.sense.api.fb.model.FacebookProfile;
import is.hello.sense.api.fb.model.FacebookProfilePicture;
import is.hello.sense.api.sessions.ApiSessionManager;
import is.hello.sense.functional.Functions;
import is.hello.sense.mvp.interactors.SenseInteractor;
import is.hello.sense.util.Logger;
import rx.Observable;

public class FacebookInteractor extends SenseInteractor<FacebookProfile> {


    @Inject FacebookApiService apiService;
    @Inject CallbackManager callbackManager;
    @Inject ConnectivityManager connectivityManager;

    private static final String IMAGE_PARAM = "picture.type(large)";
    private static final String PROFILE_PARAM = "first_name,last_name,email,gender";
    private String queryParams;
    private List<String> permissionList;

    public @Inject
    FacebookInteractor(@NonNull final Context context){
        this.queryParams = getDefaultQueryParams();
        this.permissionList = getDefaultPermissions();

        Rx.fromLocalBroadcast(context, new IntentFilter(ApiSessionManager.ACTION_LOGGED_OUT))
        .subscribe( ignored -> logout(), Functions.LOG_ERROR);
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
    protected Observable<FacebookProfile> provideUpdateObservable() {
        return apiService.getProfile(queryParams, true, getAuthTokenString())
                         .doOnNext(facebookProfile ->
                                           logEvent("fetched profile from facebook")
                                  );
    }

    public Observable<FacebookProfilePicture> providePictureUpdateObservable() {
        return apiService.getProfilePicture("0", "large", getAuthTokenString())
                         .doOnNext(profilePicture ->
                                           logEvent("fetched profile picture from facebook")
                                  );
    }

    //region Updates
    public void onActivityResult(final int requestCode,final int resultCode,@NonNull final Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Required to run before binding and subscribing to {@link FacebookInteractor#subscriptionSubject}
     * Otherwise, updates will fail because login callbacks are not handled.
     */
    public void init() {
        LoginManager.getInstance()
                    .registerCallback(
                            callbackManager,
                            new FacebookCallback<LoginResult>() {
                                @Override
                                public void onSuccess(final LoginResult loginResult) {
                                    setAuthToken(loginResult.getAccessToken());
                                    FacebookInteractor.this.update();
                                }

                                @Override
                                public void onCancel() {
                                    subscriptionSubject.onNext(FacebookProfile.EmptyProfile.newInstance());
                                }

                                @Override
                                public void onError(final FacebookException exception) {
                                    // if error is a CONNECTION_FAILURE it may have been caused by using a proxy like Charles
                                    Logger.debug(FacebookInteractor.class.getSimpleName(), "login failed", exception.fillInStackTrace());
                                    subscriptionSubject.onError(exception);
                                }
                            });
    }

    /**
     * Default requests only profile photo from facebook user
     * Use {@link FacebookInteractor#login(Fragment, boolean)} for more profile permissions
     */
    public void login(@NonNull final Fragment container) {
        login(container, true);
    }

    public void login(@NonNull final Fragment container, final boolean requestOnlyPhoto) {
        if(!isConnected()){
            //Which exception would be more helpful to throw here?
            subscriptionSubject.onError(new Exception("No internet connection found"));
            return;
        }
        requestInfo(requestOnlyPhoto);
        if(isLoggedIn()){
            this.update();
        } else {
            LoginManager.getInstance().logInWithReadPermissions(container, permissionList);
        }
    }

    public boolean isLoggedIn(){
        final AccessToken currentToken = AccessToken.getCurrentAccessToken();
        return subscriptionSubject.hasValue() &&
                currentToken != null &&
                !currentToken.isExpired() &&
                currentToken.getPermissions().containsAll(permissionList);
    }

    public void logout() {
        setAuthToken(null);
        subscriptionSubject.forget();
    }

    public boolean isConnected(){
        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }

    //endregion

    private void setAuthToken(@Nullable final AccessToken token){
        AccessToken.setCurrentAccessToken(token);
    }

    private String getAuthTokenString(){
        final AccessToken token = AccessToken.getCurrentAccessToken();
        return String.format("Bearer %s", token != null ? token.getToken() : "");
    }

    /**
     *
     * @param requestOnlyPhoto determines if only to add photo query param to facebook graph api request
     */
    private void requestInfo(final boolean requestOnlyPhoto){
        if(requestOnlyPhoto){
            queryParams = IMAGE_PARAM;
            permissionList = Collections.singletonList("public_profile");
        } else{
            queryParams = getDefaultQueryParams();
            permissionList = getDefaultPermissions();
        }
    }

    private String getDefaultQueryParams(){
        return String.format("%s,%s",IMAGE_PARAM, PROFILE_PARAM);
    }

    private List<String> getDefaultPermissions(){
        return Arrays.asList("public_profile","email");
    }

}
