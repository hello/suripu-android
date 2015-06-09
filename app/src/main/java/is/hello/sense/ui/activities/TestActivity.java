package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.util.markup.MarkupProcessor;

public class TestActivity extends SenseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);

        int padding = getResources().getDimensionPixelSize(R.dimen.gap_outer);
        textView.setPadding(padding, padding, padding, padding);
        textView.setTextAppearance(this, R.style.AppTheme_Text_Body);
        textView.setLinksClickable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        setContentView(textView);


        MarkupProcessor processor = new MarkupProcessor();
        CharSequence result = processor.render("This is *italic* text.\n\nThis is **very important** text.");
        textView.setText(result);
    }
}
