package fi.toikarin.koppi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView playerCountTextView;
    private TextView checkStatusTextView;
    private Button updateButton;
    private ProgressBar progressBar;

    private Main main;
    private Timer timer = new Timer();
    private int counter = 0;

    private static final int updateInterval = 1000 * 60;
    private static final int updateThreshold = 2000;
    private static final boolean debug = false;
    private static final String TAG = "Koppi";
    private static final String url = "http://peku.kapsi.fi/koppi.php";
    private static final Pattern patternWaiting = Pattern.compile("<p>(\\d) pelaajaa valmiina</p>");
    private static final SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");

    public int testPlayerCount() {
        int old = ++counter;

        switch (old) {
            case 7:
                return 5;
            case 8:
                return 4;
            case 9:
                return 5;
            default:
                return old;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        playerCountTextView = (TextView) findViewById(R.id.status);
        checkStatusTextView = (TextView) findViewById(R.id.textView);
        updateButton = (Button) findViewById(R.id.updateButton);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (canUpdate()) {
                    myClickHandler(view);
                }
            }
        });

        main = (Main) getApplicationContext();

        timer.scheduleAtFixedRate(new UpdateTask(), 0, updateInterval);

        setCheckStatus();
        if (main.getLastCount() != null) {
            setReadyCount(main.getLastCount());
        }
    }

    @Override
    public void onPause() {
        if (timer != null) {
            timer.cancel();
        }

        Log.i(TAG, "pause");

        super.onPause();
    }

    public boolean canUpdate() {
        if (main.getLastUpdated() == null) {
            return true;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, -updateThreshold);

        return calendar.after(main.getLastUpdated());
    }

    public boolean shouldUpdate() {
        Log.w(TAG, "" + main.getLastUpdated());
        if (main.getLastUpdated() == null) {
            return true;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, -updateInterval);

        return calendar.after(main.getLastUpdated());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    public void myClickHandler(View view) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new DownloadKoppiStatus().execute();
                }
            });
        } else {
            playerCountTextView.setText("No network connection available.");
        }
    }

    private class UpdateTask extends TimerTask {
        @Override
        public void run() {

            if (shouldUpdate()) {
                Log.d(TAG, "TimerTask.run() " + df.format(new Date()));
                myClickHandler(null);
            }
        }
    }

    private void setCheckStatus() {
        if (main.getLastUpdated() == null) {
            return;
        }

        checkStatusTextView.setText(getResources().getString(R.string.last_checked) + ":\n"
                + df.format(main.getLastUpdated().getTime()));
    }

    private void setReadyCount(int readyCount) {
        String newText = String.format(getResources().getString(R.string.players_ready), readyCount);
        playerCountTextView.setText(newText);

        if (readyCount >= 5) {
            playerCountTextView.setBackgroundColor(0xFF99FF99);
        } else {
            playerCountTextView.setBackgroundColor(0xFFFFFF99);
        }
    }

    private class DownloadKoppiStatus extends AsyncTask<Object, Object, String> {
        @Override
        protected String doInBackground(Object... params) {

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });

            try {
                if (!debug) {
                    return download();
                } else {
                    return "<p>" + testPlayerCount() + " pelaajaa valmiina</p>";
                }
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d(TAG, "Received: " + result);

            final int readyCount = parse(result);
            Integer lastCount = main.getLastCount();

            main.setLastCount(readyCount);
            main.setLastUpdated(Calendar.getInstance());

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(ProgressBar.GONE);

                    setCheckStatus();
                    setReadyCount(readyCount);
                }

            });

            if (readyCount >= 5 && (lastCount == null || lastCount < 5)) {
                MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.koppi);
                mp.start();
            }
        }

        private int parse(String data) {
            Matcher matcher = patternWaiting.matcher(data);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }

            return -1;
        }

        private String download() throws IOException {
            InputStream is = null;

            Log.d(TAG, "Downloading: " + url);

            try {
                URL u = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                conn.connect();

                if (conn.getResponseCode() != 200) {
                    throw new NullPointerException();
                }

                is = conn.getInputStream();
                return readIt(is);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        public String readIt(InputStream stream) throws IOException, UnsupportedEncodingException {
            BufferedReader in = null;
            StringBuilder sb = new StringBuilder();

            try {
                in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                String str;

                while ((str = in.readLine()) != null) {
                    sb.append(str).append('\n');
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }

            return sb.toString();
        }
    }
}
