package de.christeck.classicalmetronome;

import android.media.AudioTrack;
import android.util.Log;


public class Playback {
	
	public static final int DEFAULT_SPEED = 100;
	public static final int BYTES_PER_SAMPLE = 2;
	public static final double SECONDS_PER_MINUTE = 60.0;
	
	private int bpm;
	private boolean playing;
	private byte[] sound;
	private AudioTrack audioTrack;
	private Runnable playbackRunnable;
	private Thread playbackThread;
	
	public Playback(int speed, byte[] sound, AudioTrack audioTrack) {
		playing = false;
		this.setSound(sound);
		this.setBPM(speed);
		this.setAudioTrack(audioTrack);
	}

	public int getBPM() {
		return bpm;
	}

	public void setBPM(int speed) {
		this.bpm = speed;
	}
	
	public void start() {
		audioTrack.play();
		playing = true;
		
		playbackRunnable = new Runnable() {
			@Override
			public void run() {		
				while (playing) {
					int beatLength = (int) Math.round((SECONDS_PER_MINUTE/bpm)*audioTrack.getSampleRate());
					beatLength = beatLength * BYTES_PER_SAMPLE;
					int soundLength = sound.length;
					if(soundLength > beatLength)
						soundLength = beatLength; //with higher BPMs, the full sound is too long
					audioTrack.write(sound, 0, soundLength);
					byte[] space = buildSpace(beatLength, soundLength);
					audioTrack.write(space, 0, space.length);
		        }
			}
		};

		playbackThread = new Thread(playbackRunnable);
		playbackThread.start();
	}
	
	public void stop() {
		playing = false;
		audioTrack.pause();
		audioTrack.flush();
	}
	
	public void togglePlayback() {
		if(playing) {
			this.stop();
		}
		else {
			this.start();
		}
	}
	
	public boolean isPlaying() {
		return playing;
	}

	public void setSound(byte[] sound) {
		this.sound = sound;
	}

	public void setAudioTrack(AudioTrack audioTrack) {
		this.audioTrack = audioTrack;
	}

	private byte[] buildSpace(int beatLength, int soundLength) {
		int spaceLength = beatLength - soundLength;
		byte[] space = new byte[spaceLength];
		return space;
	}

}