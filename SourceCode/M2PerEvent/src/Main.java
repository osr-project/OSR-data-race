import java.util.Arrays;
import java.util.Set;


//java -cp "m2DecisionSimpleMine/bin:m2DecisionSimpleMine/src/*:m2DecisionSimpleMine/src/jgrapht-1.2.0/lib/*" Main sp-traces/array.m2 false false
public class Main {
	public static void main(String[] args) {

		boolean dropThreadLocalLocations = false;	//Shrink the trace by dropping events on locations that are thread-local
		boolean witness = false;	//Construct a witness when reporting a data race, and verify that it is a valid trace

		if(args.length > 1) {
			dropThreadLocalLocations = args[1].toLowerCase().equals("true");
		}
		if(args.length > 2) {
			witness = args[2].toLowerCase().equals("true");
		}
		raceDetection(args[0], dropThreadLocalLocations, witness);
//		raceDetection("C:\\Research\\traces\\SP-NoEmptyThread-Filter\\twostage.m2", dropThreadLocalLocations, witness);
	}



	public static void raceDetection(String filename, boolean dropThreadLocalLocations, boolean witness) {
		long startTime = System.currentTimeMillis();
		double elapsedTime = 0;

		RaceDetection raceDetection = new RaceDetection(filename, dropThreadLocalLocations, witness);
//		raceDetection.getRaces();
		raceDetection.getRacyEvents();

//		numRaces = raceDetection.numRaces();
		Set<Integer> racyCodeLines = raceDetection.racyCodeLines;
		Set<Integer> racyEventsAdded = raceDetection.racyEventsAdded;

		elapsedTime = (System.currentTimeMillis() - startTime)/1000.0;

		System.out.println("Num racy events: " + racyEventsAdded.size());
		System.out.println("Num racy code lines: " + racyCodeLines.size());
		System.out.println("Elapsed time: " + String.format("%.5f",elapsedTime) + " s");
		System.out.println("============================================================");
	}
}
