package is.hello.sense.ui.adapter;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import is.hello.sense.api.model.v2.sensors.Sensor;

//todo remove if not used when PR is ready
public class RoomCheckAdapter extends ArrayRecyclerAdapter<Sensor, RoomCheckAdapter.ViewHolder> {

    protected RoomCheckAdapter(@NonNull List<Sensor> storage) {
        super(storage);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class ViewHolder extends ArrayRecyclerAdapter.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
