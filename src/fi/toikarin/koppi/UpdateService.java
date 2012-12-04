package fi.toikarin.koppi;

import java.io.IOException;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

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
            try {
                String result = getCurrentStatus();
                Log.d(TAG, "Received data: " + result);

                /**
                 * Parse data
                 */
                int count = parse(result);

                if (enoughReady(count)) {
                    rumbler.rumble(!main.isMuted());
                } else if (almostEnoughReady(count)) {
                    rumbler.rumble(false);
                }

                /**
                 * Update state
                 */
                main.setLastCount(count);
                main.setLastUpdated(Calendar.getInstance());

                /**
                 * Send response
                 */
                sendResponse(intent, true, count, null);
            } catch (IOException ioe) {
                Log.w(TAG, "Error occurred while downloading data.", ioe);

                /**
                 * Send error response
                 */
                String msg = this.getResources().getString(R.string.error_downloading_data);
                sendResponse(intent, false, -1, msg + " " + ioe.getLocalizedMessage());
            }
        } else {
            Log.w(TAG, "No network connection available.");

            String msg = this.getResources().getString(R.string.no_network_connection);
            sendResponse(intent, false, -1, msg);
        }
    }

    private String getCurrentStatus() throws IOException {
        return (!Main.DEBUG
                ? download()
                : "<p>" + testCount() + " pelaajaa valmiina</p>");
    }

    private String download() throws IOException {
        Log.d(TAG, "Downloading updates.");

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet req = new HttpGet(url);

        HttpResponse response = httpClient.execute(req);

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException(response.getStatusLine().getReasonPhrase());
        }

        return EntityUtils.toString(response.getEntity());
    }

    private int parse(String data) {
        Matcher matcher = patternWaiting.matcher(data);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return -1;
    }

    private void sendResponse(Intent intent, boolean ok, int count, String msg) {
        ResultReceiver resultReceiver = intent.getParcelableExtra(RESULT_RECEIVER_ID);

        Bundle bundle = new Bundle();
        bundle.putParcelable(RESPONSE_ID, new Response(ok, count, msg));
        resultReceiver.send(0, bundle);
    }

    private boolean enoughReady(int count) {
        return (count >= 5 &&
                (main.getLastCount() == null || main.getLastCount() < 5));
    }

    private boolean almostEnoughReady(int count) {
        return (count == 4 && (main.getLastCount() == null || main.getLastCount() < 4));
    }

    private int testCount() {
        int[] arr = {3, 4, 5, 6, 4, 5};
        return arr[counter++ % arr.length];
    }
}
