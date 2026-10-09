package com.cwnu.kitchenmood.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.cwnu.kitchenmood.model.Mood;

public final class MoodAudioEngine {
    private static final int SAMPLE_RATE = 44_100;
    private static final int CHUNK_FRAMES = 1_024;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double[][] CALM_CHORDS = frequencies(new int[][]{
            {60, 64, 67, 71}, {57, 60, 64, 67}, {53, 57, 60, 64}, {55, 59, 62, 67}
    });
    private static final double[][] GROOVE_CHORDS = frequencies(new int[][]{
            {60, 64, 67}, {65, 69, 72}, {57, 60, 64}, {55, 59, 62}
    });
    private static final double[][] FOCUS_CHORDS = frequencies(new int[][]{
            {62, 65, 69}, {58, 62, 65}, {60, 64, 67}, {57, 60, 64}
    });

    private final Object lock = new Object();
    private final AudioManager audioManager;
    private volatile Mood mood = Mood.CALM;
    private volatile float volume = 0.20f;
    private volatile boolean playing;
    private volatile long generation;
    private boolean released;
    private AudioTrack track;
    private AudioFocusRequest focusRequest;
    private Thread worker;

    public MoodAudioEngine(Context context) {
        Context application = context.getApplicationContext();
        Context owner = application != null ? application : context;
        audioManager = (AudioManager) owner.getSystemService(Context.AUDIO_SERVICE);
    }

    public boolean start(Mood selectedMood) {
        synchronized (lock) {
            if (released || audioManager == null) return false;
            mood = selectedMood != null ? selectedMood : Mood.CALM;
            if (playing) return true;
            long session = ++generation;
            AudioTrack output = null;
            AudioFocusRequest request = null;
            try {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                request = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(attributes)
                        .setAcceptsDelayedFocusGain(false)
                        .setWillPauseWhenDucked(true)
                        .setOnAudioFocusChangeListener(change -> {
                            if (change == AudioManager.AUDIOFOCUS_LOSS
                                    || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                                    || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                                stopSession(session);
                            }
                        })
                        .build();
                if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    abandonFocus(request);
                    return false;
                }
                if (generation != session) {
                    abandonFocus(request);
                    return false;
                }
                int minimumBuffer = AudioTrack.getMinBufferSize(
                        SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (minimumBuffer <= 0) {
                    abandonFocus(request);
                    return false;
                }
                output = new AudioTrack.Builder()
                        .setAudioAttributes(attributes)
                        .setAudioFormat(new AudioFormat.Builder()
                                .setSampleRate(SAMPLE_RATE)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(Math.max(minimumBuffer, CHUNK_FRAMES * 8))
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();
                if (output.getState() != AudioTrack.STATE_INITIALIZED) {
                    releaseTrack(output);
                    abandonFocus(request);
                    return false;
                }
                output.play();
                track = output;
                focusRequest = request;
                playing = true;
                AudioTrack sessionTrack = output;
                AudioFocusRequest sessionFocus = request;
                worker = new Thread(() -> render(session, sessionTrack, sessionFocus), "KitchenMoodAudio");
                worker.setDaemon(true);
                worker.start();
                return true;
            } catch (RuntimeException exception) {
                playing = false;
                track = null;
                focusRequest = null;
                worker = null;
                releaseTrack(output);
                abandonFocus(request);
                return false;
            }
        }
    }

    public void stop() {
        stopSession(-1L);
    }

    public void setMood(Mood mood) {
        this.mood = mood != null ? mood : Mood.CALM;
    }

    public void setVolume(float volume) {
        this.volume = Float.isNaN(volume) ? 0.20f : Math.max(0.0f, Math.min(1.0f, volume));
    }

    public boolean isPlaying() {
        return playing;
    }

    public void release() {
        synchronized (lock) {
            released = true;
        }
        stop();
    }

    private void stopSession(long session) {
        AudioTrack output;
        AudioFocusRequest request;
        Thread audioThread;
        synchronized (lock) {
            if (session >= 0L && session != generation) return;
            generation++;
            playing = false;
            output = track;
            request = focusRequest;
            audioThread = worker;
            track = null;
            focusRequest = null;
            worker = null;
        }
        abandonFocus(request);
        if (output != null) {
            try { output.pause(); } catch (RuntimeException ignored) { }
            try { output.flush(); } catch (RuntimeException ignored) { }
            try { output.stop(); } catch (RuntimeException ignored) { }
        }
        if (audioThread != null && audioThread != Thread.currentThread()) {
            audioThread.interrupt();
            try {
                audioThread.join(350L);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            if (audioThread.isAlive()) releaseTrack(output);
        } else if (audioThread == null) {
            releaseTrack(output);
        }
    }

    private boolean sessionActive(long session) {
        return playing && generation == session && !Thread.currentThread().isInterrupted();
    }

    private void render(long session, AudioTrack output, AudioFocusRequest request) {
        short[] samples = new short[CHUNK_FRAMES];
        long frames = 0L;
        Mood activeMood = mood;
        Mood previousMood = activeMood;
        int transition = 0;
        int transitionFrames = SAMPLE_RATE / 6;
        double currentVolume = volume;
        try {
            while (sessionActive(session)) {
                Mood requestedMood = mood;
                if (requestedMood != activeMood) {
                    previousMood = activeMood;
                    activeMood = requestedMood;
                    transition = transitionFrames;
                }
                for (int index = 0; index < samples.length; index++) {
                    double seconds = (frames + index) / (double) SAMPLE_RATE;
                    double signal = soundAt(activeMood, seconds);
                    if (transition > 0) {
                        double blend = transition / (double) transitionFrames;
                        signal = signal * (1.0 - blend) + soundAt(previousMood, seconds) * blend;
                        transition--;
                    }
                    currentVolume += (volume - currentVolume) * 0.001;
                    double fadeIn = Math.min(1.0, seconds / 0.15);
                    double sample = signal * 0.06 * currentVolume * fadeIn;
                    samples[index] = (short) Math.round(Math.max(-1.0, Math.min(1.0, sample)) * Short.MAX_VALUE);
                }
                frames += samples.length;
                int offset = 0;
                while (offset < samples.length && sessionActive(session)) {
                    int written = output.write(samples, offset, samples.length - offset, AudioTrack.WRITE_BLOCKING);
                    if (written < 0) return;
                    if (written == 0) {
                        Thread.sleep(4L);
                    } else {
                        offset += written;
                    }
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException ignored) {
            // An output device can disappear during playback.
        } finally {
            releaseTrack(output);
            boolean ownsFocus = false;
            synchronized (lock) {
                if (generation == session) {
                    generation++;
                    playing = false;
                    track = null;
                    worker = null;
                    focusRequest = null;
                    ownsFocus = true;
                }
            }
            if (ownsFocus) abandonFocus(request);
        }
    }

    private static double soundAt(Mood mood, double seconds) {
        double chordSeconds = mood == Mood.GROOVE ? 4.0 : mood == Mood.FOCUS ? 5.0 : 6.0;
        double[][] progression = mood == Mood.GROOVE ? GROOVE_CHORDS : mood == Mood.FOCUS ? FOCUS_CHORDS : CALM_CHORDS;
        int chordIndex = (int) (seconds / chordSeconds) % progression.length;
        double[] chord = progression[chordIndex];
        double withinChord = seconds % chordSeconds;
        double envelope = Math.min(1.0, withinChord / 0.65)
                * Math.min(1.0, (chordSeconds - withinChord) / 0.65);
        double harmony = 0.0;
        for (double frequency : chord) {
            double phase = TWO_PI * frequency * seconds;
            harmony += mood == Mood.GROOVE ? triangle(phase) : Math.sin(phase);
        }
        harmony = harmony / chord.length * envelope;
        if (mood == Mood.GROOVE) {
            double beatTime = seconds % (2.0 / 3.0);
            double bassEnvelope = Math.min(1.0, beatTime / 0.018) * Math.exp(-beatTime * 11.0);
            double bass = Math.sin(TWO_PI * (chord[0] / 2.0) * seconds) * bassEnvelope;
            return harmony * 0.65 + bass * 0.30;
        }
        if (mood == Mood.FOCUS) {
            double accentTime = seconds % 1.5;
            double accentEnvelope = Math.min(1.0, accentTime / 0.03) * Math.exp(-accentTime * 6.0);
            double accent = Math.sin(TWO_PI * (chord[2] * 2.0) * seconds) * accentEnvelope;
            return harmony * 0.72 + accent * 0.20;
        }
        return harmony * 0.82;
    }

    private static double[][] frequencies(int[][] notes) {
        double[][] result = new double[notes.length][];
        for (int chord = 0; chord < notes.length; chord++) {
            result[chord] = new double[notes[chord].length];
            for (int note = 0; note < notes[chord].length; note++) {
                result[chord][note] = 440.0 * Math.pow(2.0, (notes[chord][note] - 69) / 12.0);
            }
        }
        return result;
    }

    private static double triangle(double phase) {
        return (2.0 / Math.PI) * Math.asin(Math.sin(phase));
    }

    private void abandonFocus(AudioFocusRequest request) {
        if (audioManager == null || request == null) return;
        try { audioManager.abandonAudioFocusRequest(request); } catch (RuntimeException ignored) { }
    }

    private static void releaseTrack(AudioTrack output) {
        if (output == null) return;
        try { output.release(); } catch (RuntimeException ignored) { }
    }
}
