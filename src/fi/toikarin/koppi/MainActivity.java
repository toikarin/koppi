package fi.toikarin.koppi;

import java.text.DateFormat;

import android.os.Bundle;
import android.os.ResultReceiver;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
    private TextView playerCountTextView;
    private TextView lastCheckedTextView;
    private Button updateButton;
    private ProgressBar progressBar;
    private ToggleButton enabledToggleButton;
    private ToggleButton mutedToggleButton;

    private Main main;
    private BroadcastReceiver broadcastReceiver = new ActivityBroadcastReceiver();

    private static final String TAG = "Koppi";
    private static final DateFormat df = DateFormat.getTimeInstance();

    public static final String UPDATE_EVENT_ID
        = MainActivity.class.getPackage().getName() + ".UPDATE_EVENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main = (Main) getApplicationContext();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        playerCountTextView = (TextView) findViewById(R.id.playerCountTextView);
        lastCheckedTextView = (TextView) findViewById(R.id.lastCheckedTextView);
        updateButton = (Button) findViewById(R.id.updateButton);
        enabledToggleButton = (ToggleButton) findViewById(R.id.enabledToggleButton);
        mutedToggleButton = (ToggleButton) findViewById(R.id.mutedToggleButton);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (main.canUpdate()) {
                    update();
                }
            }
        });

        enabledToggleButton.setChecked(main.isEnabled());
        enabledToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Scheduler.start(MainActivity.this);
                } else {
                    Scheduler.stop(MainActivity.this);
                }
            }
        });

        mutedToggleButton.setChecked(main.isMuted());
        mutedToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                main.setMuted(isChecked);
            }
        });

        /*
         * Start scheduler
         */
        if (main.isEnabled()) {
            Scheduler.start(this);
        }

        /**
         * Update UI
         */
        updateUI();

        /**
         * Set background transparency
         */
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.layout);
        Drawable background = layout.getBackground();
        background.setAlpha(80);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.i(TAG, "Activity started.");
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(broadcastReceiver, new IntentFilter(UPDATE_EVENT_ID));

        Log.i(TAG, "Activity resumed.");
    }

    @Override
    public void onPause() {
        super.onPause();

        unregisterReceiver(broadcastReceiver);

        Log.i(TAG, "Activity paused.");
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.i(TAG, "Activity stopped.");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    private void updateUI() {
        if (main.getLastUpdated() != null) {
            lastCheckedTextView.setText(getResources().getString(R.string.last_checked) + ":\n"
                    + df.format(main.getLastUpdated().getTime()));
        }

        if (main.getLastCount() != null) {
            String newText = String.format(getResources().getString(R.string.players_ready),
                    main.getLastCount());
            playerCountTextView.setText(newText);

            if (main.getLastCount() >= 5) {
                playerCountTextView.setBackgroundColor(0xFF99FF99);
            } else {
                playerCountTextView.setBackgroundColor(0xFFFFFF99);
            }
        }
    }

    private void handle(final Response response) {

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(ProgressBar.INVISIBLE);

                updateUI();
            }
        });
    }

    private void update() {
        Intent serviceIntent = new Intent(this, UpdateService.class);

        serviceIntent.putExtra(UpdateService.RESULT_RECEIVER_ID, new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                Log.d(TAG, "Received results.");

                Response response = resultData.getParcelable(UpdateService.RESPONSE_ID);

                handle(response);
            }
        });

        startService(serviceIntent);
    }

    private class ActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received update broadcast.");

            Response response = intent.getParcelableExtra(UpdateService.RESPONSE_ID);

            handle(response);
        }
    }
}
