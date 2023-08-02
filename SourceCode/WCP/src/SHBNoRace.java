import cmd.CmdOptions;
import cmd.GetOptions;
import debug.CacheStatistics;
import debug.EventStatistics;
import engine.racedetection.shb_no_race.optjoin.SHBEngine;

public class SHBNoRace {

	public SHBNoRace() {

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
		engine.analyzeTrace(options.multipleRace);

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}

		if (EventStatistics.isEnabled()) {
			EventStatistics.print();
			CacheStatistics.print();
		}
	}

}
