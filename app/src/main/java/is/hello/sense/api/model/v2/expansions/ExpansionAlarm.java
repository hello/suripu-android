package is.hello.sense.api.model.v2.expansions;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import is.hello.sense.api.model.ApiResponse;

/**
 * Provide to {@link is.hello.sense.api.model.Alarm} to specify which
 * expansions are enabled
 */
public class ExpansionAlarm extends ApiResponse {
    @SerializedName("id")
    private final long id;

    @SerializedName("category")
    private Category category;

    @SerializedName("service_name")
    private String serviceName;

    @SerializedName("enabled")
    public boolean enabled;

    @SerializedName("target_value")
    private ExpansionValueRange expansionRange;

    //@Exclude
    private String displayValue = "empty value";
    //@Exclude
    @DrawableRes
    private int displayIcon;

    public ExpansionAlarm(final long id,
                          @NonNull final Category category,
                          @NonNull final String serviceName,
                          final boolean enabled,
                          @Nullable final ExpansionValueRange range) {
        this.id = id;
        this.category = category;
        this.serviceName = serviceName;
        this.enabled = enabled;
        this.expansionRange = range;
    }

    public ExpansionAlarm(@NonNull final Expansion expansion,
                          final boolean isEnabled) {
        this(expansion.getId(),
             expansion.getCategory(),
             expansion.getServiceName(),
             isEnabled,
             null);
    }

    public long getId() {
        return id;
    }

    public Category getCategory() {
        if (category == null) {
            return Category.UNKNOWN;
        } else {
            return category;
        }
    }

    public void setCategory(@NonNull final Category category) {
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(@NonNull final String displayValue) {
        this.displayValue = displayValue;
    }

    @DrawableRes
    public int getDisplayIcon() {
        return displayIcon;
    }

    public void setDisplayIcon(@DrawableRes final int displayIcon) {
        this.displayIcon = displayIcon;
    }

    /**
     * Because we only track one value for each expansion we can use it for both the min and max
     * of the expansion range. At a future time we may see expansion range condense into a single
     * integer value.
     *
     * @param selectedValue the value the user choose. Not to be confused with index position.
     */
    public void setExpansionRange(final float selectedValue) {
        this.expansionRange = new ExpansionValueRange(selectedValue, selectedValue);
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }


    public boolean hasExpansionRange() {
        return expansionRange != null;
    }

    public ExpansionValueRange getExpansionRange() {
        return expansionRange;
    }
}
