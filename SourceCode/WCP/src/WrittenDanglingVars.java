import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import cmd.CmdOptions;
import cmd.GetOptions;
import engine.annotation.written_dangling_vars.WrittenDanglingVarsEngine;

public class WrittenDanglingVars {

	public WrittenDanglingVars() {

	}

	public static void main(String[] args) {
		CmdOptions options = new GetOptions(args).parse();

		boolean time_reporting = false;
		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
		}

		WrittenDanglingVarsEngine engine = new WrittenDanglingVarsEngine(options.parserType, options.path);
		engine.enablePrintStatus = false;
		engine.analyzeTrace(true);

		HashMap<String, LinkedList<Long>> boundaries = engine.getBoundaries();
		HashMap<String, LinkedList<HashSet<String>>> writtenVars = engine.getWrittenVars();
		HashMap<String, LinkedList<HashSet<String>>> danglingVars = engine.getDanglingVars();

		System.out.println("Boundaries: " + boundaries.toString());
		System.out.println("WrittenVars: " + writtenVars.toString());
		System.out.println("DanglingVars: " + danglingVars.toString());

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
