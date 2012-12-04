package fi.toikarin.koppi;

import java.util.Calendar;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(formKey = "",
        mailTo="tomi.oikarinen@iki.fi",
        mode = ReportingInteractionMode.TOAST,
        resToastText=R.string.crash_toast_text)
public class Main extends Application {
    private Integer lastCount = null;
    private Calendar lastUpdated = null;
    private boolean muted = false;
    private boolean updateAutomatically = false;
    private boolean backgroundEnabled = false;
    private int updateInterval = DEFAULT_UPDATE_INTERVAL;

    public static final int DEFAULT_UPDATE_INTERVAL = 1000 * 60;
    public static final int UPDATE_THRESHOLD = 1000 * 2;
    public static final boolean DEBUG = false;

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);

        setState(PreferenceManager.getDefaultSharedPreferences(this));
    }

    public void setState(SharedPreferences prefs) {
        muted = prefs.getBoolean("muted", false);
        updateAutomatically = prefs.getBoolean("automatic_updates", false);
        backgroundEnabled = prefs.getBoolean("background_updates", false);
        updateInterval = Integer.parseInt(prefs.getString("update_interval", Integer.toString(DEFAULT_UPDATE_INTERVAL)));
    }

    public Integer getLastCount() {
        return lastCount;
    }

    public void setLastCount(int lastCount) {
        this.lastCount = lastCount;
    }

    public void setLastUpdated(Calendar lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Calendar getLastUpdated() {
        return lastUpdated;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean updateAutomatically() {
        return updateAutomatically;
    }

    public void setUpdateAutomatically(boolean updateAutomatically) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Editor editor = preferences.edit();

        editor.putBoolean("automatic_updates", updateAutomatically);
        editor.commit();

        this.updateAutomatically = updateAutomatically;
    }

    public boolean isBackgroundEnabled() {
        return backgroundEnabled;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public boolean canUpdate() {
        return timeElapsed(getLastUpdated(), UPDATE_THRESHOLD);
    }

    private static boolean timeElapsed(Calendar curCalendar, int interval) {
        if (curCalendar == null) {
            return true;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, -interval);

        return calendar.after(curCalendar);
    }
}
