package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.bluetooth.devices.transmission.protobuf.MorpheusBle;

public class WifiNetworkAdapter extends ArrayAdapter<MorpheusBle.wifi_endpoint> {
    private final LayoutInflater inflater;

    public WifiNetworkAdapter(Context context) {
        super(context, R.layout.item_wifi_network);

        this.inflater = LayoutInflater.from(context);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_wifi_network, parent, false);
            view.setTag(new ViewHolder(view));
        }

        MorpheusBle.wifi_endpoint item = getItem(position);
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.name.setText(item.getSsid());
        if (item.getSecurityType() == MorpheusBle.wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN) {
            holder.locked.setVisibility(View.GONE);
        } else {
            holder.locked.setVisibility(View.VISIBLE);
        }

        return view;
    }


    private static class ViewHolder {
        final TextView name;
        final ImageView locked;

        ViewHolder(@NonNull View view) {
            this.name = (TextView) view.findViewById(R.id.item_wifi_network_name);
            this.locked = (ImageView) view.findViewById(R.id.item_wifi_network_locked);
        }
    }
}
