package fi.toikarin.koppi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

public class UpdateService extends IntentService {
    private static final String TAG = "KoppiUpdateService";
    private static final String url = "http://peku.kapsi.fi/koppi.php";
    private static final Pattern patternWaiting = Pattern.compile("<p>(\\d) pelaajaa valmiina</p>");

    public static final String RESULT_RECEIVER_ID
        = UpdateService.class.getName() + ".RESULT_RECEIVER";
    public static final String RESPONSE_ID
        = UpdateService.class.getName() + ".RESPONSE";

    private static int counter;

    private Main main;
    private Rumbler rumbler;

    public UpdateService() {
        super("Koppi Update Service");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        main = (Main) this.getApplicationContext();
        rumbler = Rumbler.getInstance(main);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "Received request to fetch current status.");
        update(intent);
    }

    private void update(Intent intent) {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            /**
             * Fetch current status
             */
            String result = getCurrentStatus();
            Log.d(TAG, "Received data: " + result);

            /**
             * Parse data
             */
            int count = parse(result);
            if (shouldRumble(count)) {
                rumbler.rumble(!main.isMuted());
            }

            /**
             * Update state
             */
            main.setLastCount(count);
            main.setLastUpdated(Calendar.getInstance());

            /**
             * Send response
             */
            sendResponse(intent, count);
        } else {
            Log.w(TAG, "No network connection available.");
        }
    }

    private String getCurrentStatus() {
        try {
            if (!Main.DEBUG) {
                return download();
            } else {
                return "<p>" + testCount() + " pelaajaa valmiina</p>";
            }
        } catch (IOException e) {
            Log.w(TAG, "Error occurred while downloading data.", e);
        }

        return null;
    }

    private String download() throws IOException {
        InputStream is = null;

        Log.d(TAG, "Downloading updates.");

        try {
            URL u = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
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

    private int parse(String data) {
        Matcher matcher = patternWaiting.matcher(data);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return -1;
    }

    private void sendResponse(Intent intent, int count) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER_ID);

        Bundle bundle = new Bundle();
        bundle.putParcelable(RESPONSE_ID, new Response(true, count, ""));
        resultReceiver.send(0, bundle);
    }

    private boolean shouldRumble(int count) {
        return (count >= 5 &&
                (main.getLastCount() == null || main.getLastCount() < 5));
    }

    private int testCount() {
        int[] arr = {3, 4, 5, 6, 4, 5};
        return arr[counter++ % arr.length];
    }
}
