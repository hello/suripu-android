package is.hello.sense.ui.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import is.hello.sense.R;
import is.hello.sense.ui.common.SenseDialogFragment;
import is.hello.sense.ui.widget.SenseListDialog;
import is.hello.sense.util.DateFormatter;

public class AlarmRepeatDialogFragment extends SenseDialogFragment
        implements SenseListDialog.Listener<Integer> {
    public static final String TAG = AlarmRepeatDialogFragment.class.getSimpleName();

    private static final String ARG_DAYS = AlarmRepeatDialogFragment.class.getName() + ".ARG_DAYS";
    public static final String RESULT_DAYS = AlarmRepeatDialogFragment.class.getName() + ".RESULT_DAYS";

    private DayAdapter dayAdapter;

    public static AlarmRepeatDialogFragment newInstance(@NonNull Set<Integer> days) {
        final AlarmRepeatDialogFragment fragment = new AlarmRepeatDialogFragment();

        final Bundle arguments = new Bundle();
        arguments.putIntegerArrayList(ARG_DAYS, new ArrayList<>(days));
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final SenseListDialog<Integer> dialog = new SenseListDialog<>(getActivity());

        dialog.setMessage(R.string.title_alarm_repeat);
        dialog.setListener(this);

        final int firstCalendarDayOfWeek = Calendar.getInstance().getFirstDayOfWeek();
        final int firstJodaTimeDayOfWeek = DateFormatter.calendarDayToJodaTimeDay(firstCalendarDayOfWeek);
        final List<Integer> daysOfWeek = DateFormatter.getDaysOfWeek(firstJodaTimeDayOfWeek);
        this.dayAdapter = new DayAdapter(getActivity(), daysOfWeek);
        dialog.setAdapter(dayAdapter);

        final ArrayList<Integer> selectedDays = getArguments().getIntegerArrayList(ARG_DAYS);
        if (selectedDays != null) {
            dayAdapter.addSelectedDays(selectedDays);
        }

        return dialog;
    }


    @Override
    public void onItemClicked(@NonNull SenseListDialog<Integer> dialog,
                              int position,
                              @NonNull Integer day) {
        dayAdapter.toggleDaySelected(day);
    }

    @Override
    public void onDoneClicked(@NonNull SenseListDialog<Integer> dialog) {
        final Intent response = new Intent();
        response.putIntegerArrayListExtra(RESULT_DAYS, dayAdapter.copySelectedDays());
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, response);
    }


    static class DayAdapter extends ArrayAdapter<Integer> {
        private final LayoutInflater inflater;
        private final Set<Integer> selectedDays = new HashSet<>();

        public DayAdapter(@NonNull Context context, @NonNull List<Integer> days) {
            super(context, R.layout.item_static_check, days);

            this.inflater = LayoutInflater.from(context);
        }

        public void addSelectedDays(@NonNull Collection<Integer> selectedDays) {
            this.selectedDays.addAll(selectedDays);
            notifyDataSetChanged();
        }

        public void toggleDaySelected(int selectedDay) {
            if (selectedDays.contains(selectedDay)) {
                selectedDays.remove(selectedDay);
            } else {
                selectedDays.add(selectedDay);
            }
            notifyDataSetChanged();
        }

        public ArrayList<Integer> copySelectedDays() {
            return new ArrayList<>(selectedDays);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item_static_check, parent, false);
                view.setTag(new ViewHolder(view));
            }

            final int day = getItem(position);
            final ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.checkBox.setChecked(selectedDays.contains(day));
            viewHolder.title.setText(new DateTime().withDayOfWeek(day).toString("EEEE"));

            return view;
        }


        static class ViewHolder {
            final CheckBox checkBox;
            final TextView title;

            ViewHolder(@NonNull View view) {
                this.checkBox = (CheckBox) view.findViewById(R.id.item_static_check_box);
                this.title = (TextView) view.findViewById(R.id.item_static_check_title);
            }
        }
    }
}
