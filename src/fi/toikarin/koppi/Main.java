package fi.toikarin.koppi;

import java.util.Calendar;

import android.app.Application;

public class Main extends Application {
    private Integer lastCount = null;
    private Calendar lastUpdated = null;
    
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
}