package com.chessmaster.ui;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Zero-dependency synthesized audio engine producing crisp Lichess/Chess.com style sound effects
 * (wood tap, capture thump, check chime, castle double-tap, puzzle success arpeggio, wrong move buzz).
 */
public final class SoundManager {
    private static final float SAMPLE_RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

    private static final byte[] MOVE_PCM = synthWoodClick(240, 60, 0.45);
    private static final byte[] CAPTURE_PCM = synthWoodClick(165, 95, 0.72);
    private static final byte[] CASTLE_PCM = synthDoubleClick();
    private static final byte[] CHECK_PCM = synthTone(660, 140, 0.35);
    private static final byte[] SUCCESS_PCM = synthArpeggio(new double[]{523.25, 659.25, 783.99}, 75);
    private static final byte[] ERROR_PCM = synthTone(170, 180, 0.38);

    private volatile boolean enabled = true;
    private final ExecutorService audioExec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ChessAudio");
        t.setDaemon(true);
        return t;
    });

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void playMove() { play(MOVE_PCM); }
    public void playCapture() { play(CAPTURE_PCM); }
    public void playCastle() { play(CASTLE_PCM); }
    public void playCheck() { play(CHECK_PCM); }
    public void playSuccess() { play(SUCCESS_PCM); }
    public void playError() { play(ERROR_PCM); }

    private void play(byte[] pcm) {
        if (!enabled || pcm == null) return;
        audioExec.submit(() -> {
            try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {
                line.open(FORMAT, pcm.length);
                line.start();
                line.write(pcm, 0, pcm.length);
                line.drain();
            } catch (Exception ignored) {
            }
        });
    }

    private static byte[] synthWoodClick(double baseFreq, int ms, double amp) {
        int samples = (int) (SAMPLE_RATE * ms / 1000.0);
        byte[] buf = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double env = Math.exp(-t * 65.0);
            double wave = Math.sin(2 * Math.PI * baseFreq * t) * 0.65
                    + Math.sin(2 * Math.PI * (baseFreq * 2.3) * t) * 0.35;
            short val = (short) Math.max(-32767, Math.min(32767, wave * env * amp * 32767));
            buf[2 * i] = (byte) (val & 0xFF);
            buf[2 * i + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buf;
    }

    private static byte[] synthDoubleClick() {
        byte[] single = synthWoodClick(260, 55, 0.45);
        int gap = (int) (SAMPLE_RATE * 0.045) * 2;
        byte[] out = new byte[single.length * 2 + gap];
        System.arraycopy(single, 0, out, 0, single.length);
        System.arraycopy(single, 0, out, single.length + gap, single.length);
        return out;
    }

    private static byte[] synthTone(double freq, int ms, double amp) {
        int samples = (int) (SAMPLE_RATE * ms / 1000.0);
        byte[] buf = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = i / SAMPLE_RATE;
            double env = Math.min(1.0, i / 120.0) * Math.exp(-t * 14.0);
            double wave = Math.sin(2 * Math.PI * freq * t);
            short val = (short) Math.max(-32767, Math.min(32767, wave * env * amp * 32767));
            buf[2 * i] = (byte) (val & 0xFF);
            buf[2 * i + 1] = (byte) ((val >> 8) & 0xFF);
        }
        return buf;
    }

    private static byte[] synthArpeggio(double[] freqs, int noteMs) {
        int noteSamples = (int) (SAMPLE_RATE * noteMs / 1000.0);
        byte[] buf = new byte[noteSamples * freqs.length * 2];
        for (int n = 0; n < freqs.length; n++) {
            for (int i = 0; i < noteSamples; i++) {
                double t = i / SAMPLE_RATE;
                double env = Math.min(1.0, i / 80.0) * Math.exp(-t * 10.0);
                double wave = Math.sin(2 * Math.PI * freqs[n] * t) * 0.8
                        + Math.sin(4 * Math.PI * freqs[n] * t) * 0.2;
                short val = (short) Math.max(-32767, Math.min(32767, wave * env * 0.35 * 32767));
                int idx = (n * noteSamples + i) * 2;
                buf[idx] = (byte) (val & 0xFF);
                buf[idx + 1] = (byte) ((val >> 8) & 0xFF);
            }
        }
        return buf;
    }
}
