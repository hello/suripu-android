package is.hello.sense.api.model.v2.alerts;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiResponse;

/**
 * Encapsulates display values for corresponding {@link Alert}
 */

public class AlertDialogViewModel extends ApiResponse {

    public final String positiveButtonText;

    public final String neutralButtonText;

    private final Alert alert;

    public static AlertDialogViewModel NewEmptyInstance(@NonNull final Resources resources) {
        return new AlertDialogViewModel(Alert.NewEmptyInstance(),
                                        resources);
    }

    public AlertDialogViewModel(@NonNull final Alert alert,
                                @NonNull final Resources resources){
        this.alert = alert;
        this.positiveButtonText = getPositiveButtonText(alert.getCategory(),
                                                        resources);
        this.neutralButtonText = getNeutralButtonText(alert.getCategory(),
                                                      resources);
    }

    public String getTitle(){
        return alert.getTitle();
    }

    public String getBody(){
        return alert.getBody();
    }

    public Category getCategory(){
        return alert.getCategory();
    }

    private String getPositiveButtonText(final Category category,
                                         final Resources resources) {
        switch (category){
            case SENSE_MUTED:
                return resources.getString(R.string.action_unmute);
            default:
                return resources.getString(R.string.action_okay);
        }
    }

    private String getNeutralButtonText(final Category category,
                                        final Resources resources) {
        switch (category){
            case SENSE_MUTED:
                return resources.getString(R.string.action_okay);
            default:
                return resources.getString(R.string.empty);
        }
    }
}
