import java.util.Arrays;
import java.util.Set;


//java -cp "m2DecisionSimpleMine/bin:m2DecisionSimpleMine/src/*:m2DecisionSimpleMine/src/jgrapht-1.2.0/lib/*" Main sp-traces/array.m2 false false
public class Main {
	public static void main(String[] args) {
		boolean dropThreadLocalLocations = false;    //Shrink the trace by dropping events on locations that are thread-local
		boolean witness = false;    //Construct a witness when reporting a data race, and verify that it is a valid trace

		if(args.length > 1) {
			dropThreadLocalLocations = args[1].toLowerCase().equals("true");
		}
		if(args.length > 2) {
			witness = args[2].toLowerCase().equals("true");
		}
		raceDetection(args[0], dropThreadLocalLocations, witness);
	}


	public static void raceDetection(String filename, boolean dropThreadLocalLocations, boolean witness) {
		long startTime = System.currentTimeMillis();
		double elapsedTime = 0;
		int numRaces = -1;

		RaceDetection raceDetection = new RaceDetection(filename, dropThreadLocalLocations, witness, startTime);
//		raceDetection.getRaces();
		raceDetection.getRacesSkip();
		elapsedTime = (System.currentTimeMillis() - startTime)/1000.0;
		numRaces = raceDetection.numRaces();

		System.out.println("Finished : " + filename + "  racy vars:  " + raceDetection.racyVars.size() +
				"  time spent: " + elapsedTime/60 + "min");
//		printAfterAnalysis(raceDetection, elapsedTime, numRaces, filename);
	}

	public static void printAfterAnalysis(RaceDetection raceDetection, double elapsedTime, int numRaces, String filename){
		int numRacyVars = raceDetection.numRacyVars();
		int numRacyEvents = raceDetection.numRacyEvents();

		double timeInMinutes = elapsedTime / 60;
		System.out.println("Num races: " + numRaces);
		System.out.println("Num racy events: " + numRacyEvents);
		System.out.println("Num racy vars: " + numRacyVars);
		System.out.println("Elapsed time: " + String.format("%.5f", elapsedTime));
		System.out.println("Spent time in minutes: " + String.format("%.1f", timeInMinutes));
		System.out.println(filename + "              M2 Per Var");
		System.out.println("===========================================================");
	}
}
