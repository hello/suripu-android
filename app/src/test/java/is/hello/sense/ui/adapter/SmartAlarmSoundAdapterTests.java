package is.hello.sense.ui.adapter;

import android.view.View;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;

import is.hello.sense.api.model.Alarm;
import is.hello.sense.graph.SenseTestCase;
import is.hello.sense.util.RecyclerAdapterTesting;

import static org.junit.Assert.assertEquals;

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

        assertEquals(View.VISIBLE, holder1.busy.getVisibility());
        assertEquals(View.INVISIBLE, holder1.checked.getVisibility());
        assertEquals("Chimes", holder1.name.getText().toString());


        final SmartAlarmSoundAdapter.ViewHolder holder2 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent,
                                                         adapter.getItemViewType(1), 1);

        assertEquals(View.GONE, holder2.busy.getVisibility());
        assertEquals(View.VISIBLE, holder2.checked.getVisibility());
        assertEquals("Bells", holder2.name.getText().toString());
    }

    @Test
    public void selectedRendering() throws Exception {
        adapter.add(new Alarm.Sound(1, "Chimes", "http://does.not/exist.mp3"));
        adapter.add(new Alarm.Sound(2, "Bells", "http://does.not/exist.mp3"));

        adapter.setSelectedSoundId(1);

        final SmartAlarmSoundAdapter.ViewHolder holder1 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent,
                                                         adapter.getItemViewType(0), 0);

        assertEquals(View.GONE, holder1.busy.getVisibility());
        assertEquals(View.VISIBLE, holder1.checked.getVisibility());
        assertEquals("Chimes", holder1.name.getText().toString());


        final SmartAlarmSoundAdapter.ViewHolder holder2 =
                RecyclerAdapterTesting.createAndBindView(adapter, fakeParent,
                                                         adapter.getItemViewType(1), 1);

        assertEquals(View.GONE, holder2.busy.getVisibility());
        assertEquals(View.VISIBLE, holder2.checked.getVisibility());
        assertEquals("Bells", holder2.name.getText().toString());
    }

    //endregion
}
