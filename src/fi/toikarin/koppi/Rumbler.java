package fi.toikarin.koppi;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;

public class Rumbler {
    private static SoundPool soundPool;
    private static int rumbleId;
    private static Vibrator vibrator;
    private static AudioManager audioManager;

    private static volatile Rumbler rumbler;

    public static synchronized Rumbler getInstance(Context context) {
        if (rumbler == null) {
            rumbler = new Rumbler(context);
        }

        return rumbler;
    }

    private Rumbler(Context context) {
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        rumbleId = soundPool.load(context.getApplicationContext(), R.raw.koppi, 1);

        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void rumble(boolean playSound, boolean obeyRingerMode) {
        if (playSound && (!obeyRingerMode || phoneModeNormal())) {
            soundPool.play(rumbleId, 0.5f, 0.5f, 1, 0, 1.0f);
        }

        if (!obeyRingerMode || !phoneModeSilent()) {
            vibrator.vibrate(300);
        }
    }

    private boolean phoneModeNormal() {
        return audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }

    private boolean phoneModeSilent() {
        return audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
    }
}
