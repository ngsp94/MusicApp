package com.example.link.opencvtest;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class PlayWave {

    private final int SAMPLE_RATE = 44100;
    private AudioTrack mAudio;
    int buffsize = AudioTrack.getMinBufferSize(SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private int sampleCount;
    private int amplitude = 32767;

    // ADSR envelopes
    private int[] pianoEnv;
    private int[] guitarEnv;
    private int[] trumpetEnv;
    private int[] organEnv;

    public PlayWave() {

        mAudio = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                buffsize, AudioTrack.MODE_STATIC);

        initEnvelopes();
    }

    /**
     * Makes an ADSR envelope of max size (sample rate), without Release with the following parameters (0 to 1):
     * @param a length of Attack
     * @param d length Decay
     * @param s value Sustain
     * @param amplitude Amplitude
     * @return ADSR envelope, with infinite sustain
     */
    public int[] adsrEnvelope(float a, float d, float s, int amplitude) {
        int envelope[] = new int[SAMPLE_RATE];

        // Attack
        for (int i = 0; i < SAMPLE_RATE * a; i++) {
            envelope[i] = (int) (amplitude * (i / (SAMPLE_RATE*a)));
        }

        // Decay
        for (int i = (int) (SAMPLE_RATE * a); i < SAMPLE_RATE * d; i++) {
            envelope[i] = (int) (amplitude * (s + (1.0-s) * ((SAMPLE_RATE - i) / SAMPLE_RATE)));
        }
        for (int i = (int) (SAMPLE_RATE * (1.0-a-d)); i < SAMPLE_RATE; i++) {
            envelope[i] = (int) (s * amplitude);
        }

        return envelope;
    }

    /**
     * Creates envelopes for all instruments
     */
    public void initEnvelopes() {
        // create envelopes at max sample rate
        pianoEnv = adsrEnvelope(0.0f, 0.5f, 0.6f, amplitude);
        guitarEnv = adsrEnvelope(0.2f,0.2f, 0.2f, amplitude);
        trumpetEnv = adsrEnvelope(0.5f,0.0f, 0.6f, amplitude);
        organEnv = new int[SAMPLE_RATE];
        for (int i=0; i<SAMPLE_RATE; i++) {
            organEnv[i] = (int)(amplitude*0.7f);
        }
    }



    public void setWave(int frequency, String instrument) {
        sampleCount = (int) ((float) SAMPLE_RATE / frequency);
        short samples[] = new short[sampleCount];
        double twopi = 8. * Math.atan(1.);
        double phase = 0.0;

        // create envelope
        int envelope[] = new int[sampleCount];

        // default envelope
        if (instrument.compareTo("Piano") == 0) {
            envelope = pianoEnv;
        }

        if (instrument.compareTo("Guitar") == 0) {
            envelope = guitarEnv;
        }

        if (instrument.compareTo("Trumpet") == 0) {
            envelope = trumpetEnv;
        }

        if (instrument.compareTo("Organ") == 0) {
            envelope = organEnv;
        }

        for (int i = 0; i < sampleCount; i++) {
            // to save on computation, we sample the pre-computed ADSR
            // envelope at the appropriate frequency
            samples[i] = (short) (envelope[i*frequency] * Math.sin(phase));
            phase += twopi * frequency / SAMPLE_RATE;
        }
        mAudio.write(samples, 0, sampleCount);

    }

    public void start() {
        mAudio.reloadStaticData();
        mAudio.setLoopPoints(sampleCount/2, sampleCount, -1);
        mAudio.play();
    }

    public void stop() {
        mAudio.stop();
    }

}
