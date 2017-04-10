package is.hello.sense.flows.nightmode;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static is.hello.sense.flows.nightmode.NightMode.AUTO;
import static is.hello.sense.flows.nightmode.NightMode.OFF;
import static is.hello.sense.flows.nightmode.NightMode.ON;

/**
 * Values currently set to match subset of {@link android.support.v7.app.AppCompatDelegate.NightMode}
 */
@Retention(RetentionPolicy.SOURCE)
@IntDef({OFF, ON, AUTO})
public @interface NightMode {
    int OFF = 1;
    int ON = 2;
    int AUTO = 0;
}
