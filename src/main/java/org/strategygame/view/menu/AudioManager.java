package org.strategygame.view.menu;

import javax.sound.sampled.*;
import java.io.InputStream;

public class AudioManager {
    private static AudioManager INSTANCE;
    private Clip clip;
    private FloatControl vol;

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
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
        } catch (Exception ignored) {}
    }

    public void setVolume(float level) {
        if (vol == null) return;
        vol.setValue(vol.getMinimum() + (vol.getMaximum() - vol.getMinimum()) * level);
    }
}
