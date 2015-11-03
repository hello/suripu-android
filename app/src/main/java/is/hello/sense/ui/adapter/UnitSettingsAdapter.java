package is.hello.sense.ui.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import is.hello.sense.R;

public class UnitSettingsAdapter extends RecyclerView.Adapter<UnitSettingsAdapter.UnitSettingsHolder> {

    private final LayoutInflater inflater;
    private final OnRadioChangeListener radioChangeListener;

    private @Nullable List<UnitItem> items;
    private @Nullable HashMap<String, Boolean> itemValues;

    public UnitSettingsAdapter(@NonNull Context context, @NonNull OnRadioChangeListener radioChangeListener){
        this.radioChangeListener = radioChangeListener;
        this.inflater = LayoutInflater.from(context);
    }

    //region Binding

    public void addItem(@NonNull UnitItem item, boolean value){
        if (this.items == null){
            this.items = new ArrayList<>();
        }
        items.add(item);
        if (this.itemValues == null){
            itemValues = new HashMap<>();
        }
        itemValues.put(item.key, value);

        notifyDataSetChanged();
    }

    private UnitItem getUnitItem(int position){
        if (items != null){
            return items.get(position);
        } else {
            return null;
        }
    }

    private boolean getUnitItemValue(String key){
        if (itemValues == null || !itemValues.containsKey(key)){
            return false;
        }
        return itemValues.get(key);
    }

    private void setUnitItemValue(String key, boolean value){
        if (itemValues == null){
            itemValues = new HashMap<>();
        }
        itemValues.put(key, value);
    }

    //endregion

    //region Data

    @Override
    public int getItemCount() {
        if (items != null) {
            return items.size();
        }
        return 0;
    }

    //endregion


    //region Views


    @Override
    public void onBindViewHolder(UnitSettingsHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public UnitSettingsHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.item_section_radio, parent, false);
        return new UnitSettingsHolder(view);
    }

    class UnitSettingsHolder extends  RecyclerView.ViewHolder{
        final TextView title;
        final RadioButton leftButton;
        final RadioButton rightButton;
        final RadioGroup radioGroup;

        UnitSettingsHolder(@NonNull View view) {
            super(view);
            this.title = (TextView) view.findViewById(R.id.item_section_radio_text);
            this.leftButton = (RadioButton) view.findViewById(R.id.item_section_radio_button_left);
            this.rightButton = (RadioButton) view.findViewById(R.id.item_section_radio_button_right);
            this.radioGroup = (RadioGroup) view.findViewById(R.id.item_section_radio_group);
        }

        void bind(int position) {
            UnitItem item = getUnitItem(position);
            if (item == null){
                return;
            }
            title.setText(item.titleRes);
            title.setAllCaps(false);
            leftButton.setText(item.leftValueRes);
            rightButton.setText(item.rightValueRes);
            radioGroup.setOnCheckedChangeListener(null);
            boolean itemValue = getUnitItemValue(item.key);
            if (itemValue){
                rightButton.setChecked(true);
                leftButton.setChecked(false);
            } else {
                leftButton.setChecked(true);
                rightButton.setChecked(false);
            }
            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    boolean newValue = ((RadioButton) group.getChildAt(1)).isChecked();
                    setUnitItemValue(item.key, newValue);
                    radioChangeListener.onRadioValueChanged(item.key, newValue);
                    notifyDataSetChanged();
                }
            });
        }
    }

    //endregion

    public static class UnitItem {
        @StringRes public final String key;
        @StringRes public int titleRes;
        @StringRes public int leftValueRes;
        @StringRes public int rightValueRes;

        public UnitItem(@NonNull String key,
                        @StringRes int titleRes,
                        @StringRes int leftValueRes,
                        @StringRes int rightValueRes) {
            this.key = key;
            this.titleRes = titleRes;
            this.leftValueRes = leftValueRes;
            this.rightValueRes = rightValueRes;
        }
    }

    public interface OnRadioChangeListener{
        void onRadioValueChanged(@NonNull String key, boolean newValue);
    }

}
