import cmd.CmdOptions;
import cmd.GetOptions;
import debug.CacheStatistics;
import debug.EventStatistics;
import engine.racedetection.shb_treeclock.no_race_detectionAL.SHBEngine;

public class SHBTreeClockNoRaceAL {

	public SHBTreeClockNoRaceAL() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
		}

		EventStatistics.enable();

		SHBEngine engine = new SHBEngine(options.parserType, options.path,
				options.verbosity);
		System.out.println("Number of threads: " + engine.state.numThreads);
		engine.analyzeTrace(options.multipleRace);

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");

			// System.out.println("numTimesCopyWasDeep: " +
			// engine.state.numTimesCopyWasDeep);
			// System.out.println("numTimesCopyWasMonotone: " +
			// engine.state.numTimesCopyWasMonotone);
		}

		if (EventStatistics.isEnabled()) {
			EventStatistics.print();
			CacheStatistics.print();
		}
	}

}
