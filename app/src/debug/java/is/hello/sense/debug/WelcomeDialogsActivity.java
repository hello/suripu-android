package is.hello.sense.debug;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import is.hello.sense.R;
import is.hello.sense.adapter.WelcomeDialogsAdapter;
import is.hello.sense.ui.common.InjectionActivity;
import is.hello.sense.ui.handholding.WelcomeDialogFragment;
import is.hello.sense.ui.recycler.InsetItemDecoration;

public class WelcomeDialogsActivity extends InjectionActivity {
    WelcomeDialogStringResource[] dialogs = new WelcomeDialogStringResource[]{
            new WelcomeDialogStringResource("Room Conditions", R.xml.welcome_dialog_current_conditions),
            new WelcomeDialogStringResource("Temperature", R.xml.welcome_dialog_sensor_temperature),
            new WelcomeDialogStringResource("Humidity", R.xml.welcome_dialog_sensor_humidity),
            new WelcomeDialogStringResource("Air Quality", R.xml.welcome_dialog_sensor_airquality),
            new WelcomeDialogStringResource("Sound", R.xml.welcome_dialog_sensor_noise),
            new WelcomeDialogStringResource("Light", R.xml.welcome_dialog_sensor_light),
            new WelcomeDialogStringResource("Smart Alarm", R.xml.welcome_dialog_alarm),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.static_recycler);

        final RecyclerView recyclerView = (RecyclerView) findViewById(R.id.static_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        final InsetItemDecoration decoration = new InsetItemDecoration();
        recyclerView.addItemDecoration(decoration);
        final WelcomeDialogsAdapter adapter = new WelcomeDialogsAdapter(this, getDialogs(true));
        recyclerView.setAdapter(adapter);
    }

    private List<WelcomeDialogStringResource> getDialogs(boolean wasSeen) {

        ArrayList<WelcomeDialogStringResource> screensSeen = new ArrayList<>();

        for (WelcomeDialogStringResource dialog : dialogs) {
            if (dialog.wasSeen() == wasSeen) {
                screensSeen.add(dialog);
            }
        }
        return screensSeen;
    }

    public class WelcomeDialogStringResource {
        public String dialogName;
        public int resource;

        public WelcomeDialogStringResource(String dialogName, int resource) {
            this.dialogName = dialogName;
            this.resource = resource;
        }

        public boolean wasSeen() {
            return !WelcomeDialogFragment.shouldShow(getApplicationContext(), resource);
        }
    }
}
