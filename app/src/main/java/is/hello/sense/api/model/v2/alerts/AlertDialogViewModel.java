package is.hello.sense.api.model.v2.alerts;

import android.content.res.Resources;
import android.support.annotation.NonNull;

import is.hello.sense.R;
import is.hello.sense.api.model.ApiResponse;

/**
 * Encapsulates display values for corresponding {@link Alert}
 */

public class AlertDialogViewModel extends ApiResponse
implements DialogViewModel<Category>{

    private final String positiveButtonText;

    private final String neutralButtonText;

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

    @Override
    public String getTitle(){
        return alert.getTitle();
    }

    @Override
    public String getBody(){
        return alert.getBody();
    }

    @Override
    public String getPositiveButtonText() {
        return positiveButtonText;
    }

    @Override
    public String getNeutralButtonText() {
        return neutralButtonText;
    }

    @Override
    public Category getAnalyticPropertyType() {
        return alert.getCategory();
    }

    private String getPositiveButtonText(final Category category,
                                         final Resources resources) {
        switch (category) {
            case SENSE_MUTED:
                return resources.getString(R.string.action_unmute);
            case SENSE_NOT_PAIRED:
            case SENSE_NOT_SEEN:
            case SLEEP_PILL_NOT_PAIRED:
            case SLEEP_PILL_NOT_SEEN:
                return resources.getString(R.string.action_fix_now);
            default:
                return resources.getString(R.string.action_okay);
        }
    }

    private String getNeutralButtonText(final Category category,
                                        final Resources resources) {
        switch (category){
            case SENSE_MUTED:
                return resources.getString(R.string.action_okay);
            case SENSE_NOT_PAIRED:
            case SENSE_NOT_SEEN:
            case SLEEP_PILL_NOT_PAIRED:
            case SLEEP_PILL_NOT_SEEN:
                return resources.getString(R.string.action_fix_later);
            default:
                return resources.getString(R.string.empty);
        }
    }
}
