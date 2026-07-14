package org.strategygame.view.menu;

import javax.sound.sampled.*;
import java.io.InputStream;

public class AudioManager {
    private static AudioManager INSTANCE;
    private Clip clip;
    private FloatControl vol;

    private float level = 0.7f;

    private AudioManager() {}

    public static AudioManager getInstance() {
        if (INSTANCE == null) INSTANCE = new AudioManager();
        return INSTANCE;
    }

    public void play(String path) {
        try {
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            clip = AudioSystem.getClip();
            clip.open(ais);
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN))
                vol = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            applyVolume();
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (Exception ignored) {}
    }

    public void setVolume(float level) {
        this.level = Math.max(0f, Math.min(1f, level));
        applyVolume();
    }

    private void applyVolume() {
        if (vol == null) return;
        float dB = (level <= 0f)
                ? vol.getMinimum()
                : (float) (20.0 * Math.log10(level));
        vol.setValue(Math.max(vol.getMinimum(), Math.min(vol.getMaximum(), dB)));
    }
}
