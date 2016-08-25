package is.hello.sense.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import is.hello.commonsense.bluetooth.model.protobuf.SenseCommandProtos.wifi_endpoint;
import is.hello.sense.R;
import is.hello.sense.api.model.WiFiSignalStrength;

public class WifiNetworkAdapter extends ArrayAdapter<wifi_endpoint> {
    private final Resources resources;
    private final LayoutInflater inflater;

    public WifiNetworkAdapter(@NonNull Context context) {
        super(context, R.layout.item_wifi_network);

        this.resources = context.getResources();
        this.inflater = LayoutInflater.from(context);
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.item_wifi_network, parent, false);
            view.setTag(new ViewHolder(view));
        }

        final wifi_endpoint item = getItem(position);
        final ViewHolder holder = (ViewHolder) view.getTag();

        final WiFiSignalStrength signalStrength = WiFiSignalStrength.fromRssi(item.getRssi());
        holder.strength.setImageResource(signalStrength.icon);
        holder.strength.setContentDescription(resources.getString(signalStrength.accessibilityString));

        holder.name.setText(item.getSsid());

        if (item.getSecurityType() == wifi_endpoint.sec_type.SL_SCAN_SEC_TYPE_OPEN) {
            holder.locked.setVisibility(View.GONE);
        } else {
            holder.locked.setVisibility(View.VISIBLE);
        }

        return view;
    }

    public boolean isEmpty(){
        return getCount() == 0;
    }


    public static class ViewHolder {
        public final ImageView strength;
        public final TextView name;
        public final ImageView locked;

        public ViewHolder(@NonNull View view) {
            this.strength = (ImageView) view.findViewById(R.id.item_wifi_network_strength);
            this.name = (TextView) view.findViewById(R.id.item_wifi_network_name);
            this.locked = (ImageView) view.findViewById(R.id.item_wifi_network_locked);
        }
    }
}
