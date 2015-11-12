package is.hello.sense.ui.adapter;

import android.view.View;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.RecyclerAdapterTesting;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SmartAlarmSoundAdapterTests extends SenseTestCase {
    private final FrameLayout fakeParent = new FrameLayout(getContext());
    private SmartAlarmSoundAdapter adapter;

    @Before
    public void setUp() {
        this.adapter = new SmartAlarmSoundAdapter(getContext());
    }


    //region Rendering

    @Test
    public void nowPlayingRendering() throws Exception {
        adapter.add(new Alarm.Sound(1, "Chimes", "http://does.not/exist.mp3"));
        adapter.add(new Alarm.Sound(2, "Bells", "http://does.not/exist.mp3"));

        adapter.setPlayingSoundId(1, true);

        final SmartAlarmSoundAdapter.ViewHolder holder1 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent,
                                                         adapter.getItemViewType(0), 0);

        assertThat(holder1.busy.getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(holder1.checked.getVisibility(), is(equalTo(View.INVISIBLE)));
        assertThat(holder1.name.getText().toString(), is(equalTo("Chimes")));


        final SmartAlarmSoundAdapter.ViewHolder holder2 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent,
                                                         adapter.getItemViewType(1), 1);

        assertThat(holder2.busy.getVisibility(), is(equalTo(View.GONE)));
        assertThat(holder2.checked.getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(holder2.name.getText().toString(), is(equalTo("Bells")));
    }

    @Test
    public void selectedRendering() throws Exception {
        adapter.add(new Alarm.Sound(1, "Chimes", "http://does.not/exist.mp3"));
        adapter.add(new Alarm.Sound(2, "Bells", "http://does.not/exist.mp3"));

        adapter.setSelectedSoundId(1);

        final SmartAlarmSoundAdapter.ViewHolder holder1 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent,
                                                         adapter.getItemViewType(0), 0);

        assertThat(holder1.busy.getVisibility(), is(equalTo(View.GONE)));
        assertThat(holder1.checked.getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(holder1.name.getText().toString(), is(equalTo("Chimes")));


        final SmartAlarmSoundAdapter.ViewHolder holder2 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent,
                                                         adapter.getItemViewType(1), 1);

        assertThat(holder2.busy.getVisibility(), is(equalTo(View.GONE)));
        assertThat(holder2.checked.getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(holder2.name.getText().toString(), is(equalTo("Bells")));
    }

    //endregion
}
