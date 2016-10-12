package is.hello.sense.flows.expansions.ui.views;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import is.hello.sense.R;
import is.hello.sense.mvp.view.PresenterView;
import is.hello.sense.ui.widget.util.Views;

public class ExpansionDetailView extends PresenterView {

    final TextView deviceNameTextView;
    final TextView serviceNameTextView;
    final ImageView expansionIconImageView;
    final TextView expansionDescriptionTextView;
    final Button actionButton;

    public ExpansionDetailView(@NonNull final Activity activity) {
        super(activity);
        this.deviceNameTextView = (TextView) findViewById(R.id.view_expansion_detail_device_name);
        this.serviceNameTextView = (TextView) findViewById(R.id.view_expansion_detail_device_service_name);
        this.expansionIconImageView = (ImageView) findViewById(R.id.view_expansion_detail_icon);
        this.expansionDescriptionTextView = (TextView) findViewById(R.id.view_expansion_detail_description);
        this.actionButton = (Button) findViewById(R.id.view_expansion_detail_action_button);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.view_expansion_detail;
    }

    @Override
    public void releaseViews() {
        this.actionButton.setOnClickListener(null);
    }

    public void loadExpansionIcon(@NonNull final Picasso picasso,
                                  @NonNull final String url){
        picasso.load(url)
               .into(expansionIconImageView);
    }

    public void setActionButtonClickListener(@Nullable final OnClickListener onClickListener){
        Views.setSafeOnClickListener(this.actionButton, onClickListener);
    }
}
