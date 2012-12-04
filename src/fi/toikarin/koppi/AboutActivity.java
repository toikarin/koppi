package fi.toikarin.koppi;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.RelativeLayout;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        /**
         * Set background transparency
         */
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.aboutLayout);
        Drawable background = layout.getBackground();
        background.setAlpha(80);
    }
}
