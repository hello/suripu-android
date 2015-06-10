package is.hello.sense.ui.activities;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import is.hello.sense.R;
import is.hello.sense.util.markup.MarkupProcessor;
import is.hello.sense.util.markup.text.MarkupString;

public class TestActivity extends SenseActivity {
    public static final String EXTRA_TEXT = TestActivity.class.getName() + ".EXTRA_TEXT";

    public static Bundle getArguments() {
        MarkupProcessor processor = new MarkupProcessor();
        MarkupString text = processor.render("This is _italic_ text. This text is also *italic*.\n\n" +
                "This is __very important__ text. _**This**_ may crash your **phone**.\n\n" +
                "[Here's an example link](https://google.com) and some test text after it.\n\n" +
                "- This is a list item.\n" +
                "- This is also a list item.\n\n" +
                "* This is a different sort of list.\n" +
                "+ That's kind of mixed.\n\n" +
                "1. This is an ordered list.\n" +
                "5. It's the same, really.\n");

        Bundle arguments = new Bundle();
        arguments.putParcelable(EXTRA_TEXT, text);
        return arguments;
    }

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


        CharSequence text = getArguments().getParcelable(EXTRA_TEXT);
        textView.setText(text);
    }
}
