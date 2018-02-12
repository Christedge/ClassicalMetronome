package de.christeck.classicalmetronome;

import android.app.*;
import android.os.*;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Math;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.view.View.OnClickListener;
import android.view.MotionEvent;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.os.SystemClock;
import com.github.shchurov.horizontalwheelview.HorizontalWheelView;


public class MainActivity extends Activity {
	
	static final int WAV_INFO_BYTES = 44;
	static final int MIN_BPM = 20;
	static final int MAX_BPM = 220;
	static final int DEFAULT_BPM = 100;

	TextView tempoName;
	Button buttonCurrentTempo;
	HorizontalWheelView tempoWheel;
	ImageButton playButton;

	Playback player;
	TapCounter tapCounter;
	AudioTrack audioTrack;
	boolean isPlaying = false;
	boolean tapHintShown = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		LinearLayout mainLayout = (LinearLayout) findViewById(R.id.MainLayout);

		tempoName = (TextView) this.findViewById(R.id.tempoName);

		buttonCurrentTempo = (Button) this.findViewById(R.id.buttonCurrentTempo);
		buttonCurrentTempo.setOnClickListener(new OnClickListener() {
			@Override

	        public void onClick(View view) {
				if(!tapHintShown){
					Toast.makeText(getApplicationContext(), "Tap multiple times to determine the tempo of a piece.", Toast.LENGTH_LONG).show();
					tapHintShown = true;
				}

				int newTempo = tapCounter.calculateTempo(SystemClock.elapsedRealtime());
				if(tempoIsValid(newTempo)){
					setBPM(newTempo);
					tempoWheel.setRadiansAngle(radiansByBpm(newTempo));
				}
	        }
		});

		tempoWheel = (HorizontalWheelView) findViewById(R.id.tempoWheel);
		tempoWheel.setMarksCount( (MAX_BPM - MIN_BPM) / 5 );
		tempoWheel.setListener(new HorizontalWheelView.Listener() {
            @Override
            public void onRotationChanged(double radians) {
                int bpm = bpmByRadians(radians);
				setBPM(bpm);
            }
        });


		playButton = (ImageButton) this.findViewById(R.id.playButton);
		playButton.setOnClickListener(new OnClickListener() {
			@Override
			
		    public void onClick(View v) {
				togglePlayback();
		    }
		});

		InputStream is = this.getResources().openRawResource(R.raw.taktelljunior);
		
		int length = 0;
		
		try {
			length = is.available();
		} catch (IOException e) {
			Log.e("Classical Metronome", "Cannot determine length of sound file.");
		}
		
		byte[] wavInfo = new byte[WAV_INFO_BYTES];
		byte[] sound = new byte[length - WAV_INFO_BYTES];
		
		try {
			is.read(wavInfo);
			is.read(sound);
		} catch (IOException e) {
			Log.e("Classical Metronome", "Cannot read data from sound file.");
		}
		
		audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 44100, AudioTrack.MODE_STREAM);
		
		player = new Playback(DEFAULT_BPM, sound, audioTrack);
		tapCounter = new TapCounter(MIN_BPM);

	}


	public void setBPM(int bpm) {
		player.setBPM(bpm);
		buttonCurrentTempo.setText(String.valueOf(bpm));
		tempoName.setText(tempoName(bpm));
	}

	public void togglePlayback() {
		player.togglePlayback();

		if(player.isPlaying()){
			isPlaying = true;
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		else{
			isPlaying = false;
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		playButton.setSelected(isPlaying);
	}


	private void saveData(){
		SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
		editor.putInt("bpm", player.getBPM());
		editor.putBoolean("wasPlaying", isPlaying);
		editor.apply();
	}


	private void loadData(){
		SharedPreferences prefs = getPreferences(MODE_PRIVATE); 
		int bpm = prefs.getInt("bpm", DEFAULT_BPM);
		if(bpm < MIN_BPM && bpm > MAX_BPM){
			bpm = DEFAULT_BPM;
		}

		setBPM(bpm);
		tempoWheel.setRadiansAngle(radiansByBpm(bpm));
		
		boolean wasPlaying = prefs.getBoolean("wasPlaying", false);
		if(wasPlaying == true){
			togglePlayback();
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		if(player != null) player.stop();
		loadData();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if(player != null) player.stop();
		saveData();
	}

	private boolean tempoIsValid(int newTempo){
		boolean isValid = false;
		if(newTempo >= MIN_BPM && newTempo <= MAX_BPM){
			isValid = true;
		}
		return isValid;
	}

	private double radiansByBpm(int bpm){
		double range = (double) MAX_BPM - (double) MIN_BPM;
		double segment = 2 *  java.lang.Math.PI / range;
		double radians = segment * ( bpm - MIN_BPM);
		double invertedRadians = (2 *  java.lang.Math.PI) - radians;
		return invertedRadians;
	}


	private int bpmByRadians(double radians){
		double range = (double) MAX_BPM - (double) MIN_BPM;
		double segment = 2 *  java.lang.Math.PI / range;
		// The wheel's default direction is inverse to what the user might expect
		double invertedRadians = ( 2 * java.lang.Math.PI ) - radians;
		int bpm = (int) Math.round(invertedRadians / segment);
		bpm = bpm + MIN_BPM;
		return bpm;
	}

    private String tempoName(int speed) {
		String tempoName = "";
		if(speed < 25){
			tempoName = getString(R.string.larghissimo);
		}
		else if(speed >= 25 && speed < 40){
			tempoName = getString(R.string.grave);
		}
		else if(speed >= 40 && speed < 45){
			tempoName = getString(R.string.grave) + "\n" + getString(R.string.largo);
		}
		else if(speed >= 45 && speed < 60){
			tempoName = getString(R.string.largo) + "\n" + getString(R.string.lento);
		}
		else if(speed >= 60 && speed < 66){
			tempoName = getString(R.string.larghetto);
		}
		else if(speed >= 66 && speed < 72){
			tempoName = getString(R.string.adagio);
		}
		else if(speed >= 72 && speed < 76){
			tempoName = getString(R.string.adagio) + "\n" + getString(R.string.adagietto);
		}
		else if(speed >= 76 && speed < 80){
			tempoName = getString(R.string.andante);
		}
		else if(speed >= 80 && speed < 83){
			tempoName = getString(R.string.andante) + "\n" + getString(R.string.andantino);
		}
		else if(speed >= 83 && speed < 85){
			tempoName = getString(R.string.andante) + "\n" + getString(R.string.andantino) + "\n" + getString(R.string.marcia_moderato);
		}
		else if(speed >= 85 && speed < 92){
			tempoName = getString(R.string.andante) + "\n" + getString(R.string.andantino);
		}
		else if(speed >= 92 && speed < 108){
			tempoName = getString(R.string.andante_moderato) + "\n" + getString(R.string.andantino);
		}
		else if(speed >= 108 && speed < 112){
			tempoName = getString(R.string.andante_moderato) + "\n" + getString(R.string.moderato);
		}
		else if(speed >= 112 && speed < 116){
			tempoName = getString(R.string.moderato) + "\n" + getString(R.string.allegretto);
		}
		else if(speed >= 116 && speed < 120){
			tempoName = getString(R.string.moderato) + "\n" + getString(R.string.allegro_moderato);
		}
		else if(speed >= 120 && speed < 168){
			tempoName = getString(R.string.allegro);
		}
		else if(speed >= 168 && speed < 172){
			tempoName = getString(R.string.vivace);
		}
		else if(speed >= 172 && speed < 176){
			tempoName = getString(R.string.vivace) + "\n" + getString(R.string.vivacissimo) + "\n" + getString(R.string.allegrissimo);
		}
		else if(speed >= 176 && speed < 200){
			tempoName = getString(R.string.presto);
		}
		else if(speed >= 200){
			tempoName = getString(R.string.prestissimo);
		}

		return tempoName;
    }

}
