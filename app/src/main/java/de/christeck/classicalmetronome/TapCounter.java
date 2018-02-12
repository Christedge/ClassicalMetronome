package de.christeck.classicalmetronome;

import java.util.ArrayList;

public class TapCounter {
	
	static final int MILLISECONDS_PER_MINUTE = 60000;
	int maxDeltaTime = 0;
	ArrayList<Long> timeStamps = new ArrayList<Long>();
	
	public TapCounter(int minTempo) {
		maxDeltaTime = (int) Math.round(MILLISECONDS_PER_MINUTE / minTempo);
	}


	public int calculateTempo(long timeStamp) {
		int newTempo = 0;
		int deltaTime = 0;

		if(timeStamps.size() > 0 ){
			deltaTime = (int) (timeStamp - timeStamps.get(timeStamps.size()-1));
			if(deltaTime > maxDeltaTime){
				timeStamps.clear();
			}
		}

		timeStamps.add(timeStamp);
		if(timeStamps.size() > 10 ){
			timeStamps.remove(0);
		}
	
		if(timeStamps.size() > 3 ){
			ArrayList<Integer> deltaTimes = new ArrayList<Integer>();
			int currentDeltaTime = 0;
			
			for(int i = timeStamps.size()-1; i > 0; i--){
				currentDeltaTime = (int) (timeStamps.get(i) - timeStamps.get(i-1));
				deltaTimes.add(currentDeltaTime);
			}
		
			int averageDeltaTime = 0;
			for(int i = 0; i < deltaTimes.size(); i++){
				averageDeltaTime = averageDeltaTime + deltaTimes.get(i);
			}

			averageDeltaTime = (int) Math.round(averageDeltaTime / deltaTimes.size());
			newTempo = (int) Math.round(60000 / averageDeltaTime);
		}

		return newTempo;
	}

}