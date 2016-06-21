package is.hello.sense.graph.presenters;

import android.app.Fragment;
import android.content.Intent;
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

import is.hello.sense.api.fb.FacebookApiService;
import is.hello.sense.api.fb.model.FacebookProfile;
import is.hello.sense.api.fb.model.FacebookProfilePicture;
import is.hello.sense.graph.PresenterSubject;
import is.hello.sense.util.Logger;
import rx.Observable;

public class FacebookPresenter extends ValuePresenter<FacebookProfile> {


    @Inject FacebookApiService apiService;
    @Inject CallbackManager callbackManager;
    @Inject ConnectivityManager connectivityManager;

    public final PresenterSubject<FacebookProfile> profile = this.subject;
    private static final String IMAGE_PARAM = "picture.type(large)";
    private static final String PROFILE_PARAM = "first_name,last_name,email,gender";
    private String queryParams;
    private List<String> permissionList;

    public @Inject FacebookPresenter(){
        this.queryParams = getDefaultQueryParams();
        this.permissionList = getDefaultPermissions();
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
                         .doOnNext(profile ->
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
     * Required to run before binding and subscribing to {@link FacebookPresenter#profile}
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
                                    FacebookPresenter.this.update();
                                }

                                @Override
                                public void onCancel() {
                                    profile.onNext(FacebookProfile.EmptyProfile.newInstance());
                                }

                                @Override
                                public void onError(final FacebookException exception) {
                                    // if error is a CONNECTION_FAILURE it may have been caused by using a proxy like Charles
                                    // consider presenting an error dialog here.
                                    Logger.debug(FacebookPresenter.class.getSimpleName(), "login failed", exception.fillInStackTrace());
                                    profile.onError(exception);
                                }
                            });
    }

    /**
     * Default requests only profile photo from facebook user
     * Use {@link FacebookPresenter#login(Fragment, Boolean)} for more profile permissions
     */
    public void login(@NonNull final Fragment container) {
        login(container, true);
    }

    public void login(@NonNull final Fragment container, @NonNull final Boolean requestOnlyPhoto) {
        if(!isConnected()){
            //Which exception would be more helpful to throw here?
            profile.onError(new Exception("No internet connection found"));
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
        return profile.hasValue() &&
                currentToken != null &&
                !currentToken.isExpired() &&
                currentToken.getPermissions().containsAll(permissionList);
    }

    public void logout() {
        setAuthToken(null);
        profile.forget();
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
