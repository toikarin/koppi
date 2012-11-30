package fi.toikarin.koppi;

import java.util.Calendar;
import java.util.Timer;

import android.app.Application;

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
    private Timer timer = null;
    private boolean muted = false;
    private boolean enabled = true;

    public static final int UPDATE_INTERVAL = 1000 * 60;
    public static final int UPDATE_THRESHOLD = 1000 * 2;
    public static final boolean DEBUG = true;

    @Override
    public void onCreate() {
        super.onCreate();

        ACRA.init(this);
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

    public Timer getUpdateTimer() {
        return timer;
    }

    public void setUpdateTimer(Timer updateTimer) {
        this.timer = updateTimer;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean canUpdate() {
        return timeElapsed(getLastUpdated(), UPDATE_THRESHOLD);
    }

    public boolean shouldUpdate() {
        return timeElapsed(getLastUpdated(), UPDATE_INTERVAL - 100);
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
