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
}
