package fi.toikarin.koppi;

import java.text.DateFormat;

import android.os.Bundle;
import android.os.ResultReceiver;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
    private TextView playerCountTextView;
    private TextView lastCheckedTextView;
    private Button updateButton;
    private ProgressBar progressBar;
    private ToggleButton automaticUpdatesEnabledToggleButton;

    private Main main;
    private BroadcastReceiver broadcastReceiver = new ActivityBroadcastReceiver();

    private NotificationManager notificationManager;

    private static final String TAG = "Koppi";
    private static final DateFormat df = DateFormat.getTimeInstance();
    private static final int RUNNING_NOTIFICATION_ID = 1;

    public static final String UPDATE_EVENT_ID
        = MainActivity.class.getPackage().getName() + ".UPDATE_EVENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        main = (Main) getApplicationContext();
        Rumbler.getInstance(main);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        playerCountTextView = (TextView) findViewById(R.id.playerCountTextView);
        lastCheckedTextView = (TextView) findViewById(R.id.lastCheckedTextView);
        updateButton = (Button) findViewById(R.id.updateButton);
        automaticUpdatesEnabledToggleButton = (ToggleButton) findViewById(R.id.automaticUpdatesEnabledToggleButton);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (main.canUpdate()) {
                    update();
                }
            }
        });

        automaticUpdatesEnabledToggleButton.setChecked(main.updateAutomatically());
        automaticUpdatesEnabledToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                main.setUpdateAutomatically(isChecked);

                if (isChecked) {
                    Scheduler.start(MainActivity.this);
                } else {
                    Scheduler.stop(MainActivity.this);
                }
            }
        });

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
        hideRunningNotification();
        updateUI();

        if (main.updateAutomatically()) {
            Scheduler.start(MainActivity.this);
        }

        automaticUpdatesEnabledToggleButton.setChecked(main.updateAutomatically());

        Log.i(TAG, "Activity resumed.");
    }

    @Override
    public void onPause() {
        unregisterReceiver(broadcastReceiver);

        Log.i(TAG, "Activity paused.");

        super.onPause();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "Activity stopped.");

        if (!main.isBackgroundEnabled() && main.updateAutomatically()) {
            Log.i(TAG, "Background processing disabled, stopping scheduler.");

            Scheduler.stop(this);
        } else if (main.isBackgroundEnabled() && main.updateAutomatically()) {
            showRunningNotification();
        }

        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Activity destroyed.");

        super.onDestroy();
    }

    @Override
    public void finish() {
        Scheduler.stop(this);

        Log.i(TAG, "Activity finished.");

        super.onDestroy();
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

                if (!response.isOk()) {
                    Toast.makeText(MainActivity.this, response.getMessage(),
                        Toast.LENGTH_LONG).show();
                }

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

        progressBar.setVisibility(ProgressBar.VISIBLE);
    }

    private class ActivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received update broadcast.");

            Response response = intent.getParcelableExtra(UpdateService.RESPONSE_ID);

            handle(response);
        }
    }

    private void showRunningNotification() {
        Notification notification = new Notification();

        notification.icon = R.drawable.ic_launcher;
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        notification.contentIntent = PendingIntent.getActivity(this, RUNNING_NOTIFICATION_ID,
                new Intent(this, MainActivity.class), 0);
        notification.setLatestEventInfo(this, getString(R.string.app_name),
                getString(R.string.running), notification.contentIntent);

        notificationManager.notify(RUNNING_NOTIFICATION_ID, notification);
    }

    private void hideRunningNotification() {
        notificationManager.cancel(RUNNING_NOTIFICATION_ID);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch(menuItem.getItemId()) {
            case R.id.preferencesMenuItem:
                startActivity(new Intent(this, PrefsActivity.class));
                return true;
            case R.id.aboutMenuItem:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }
}
