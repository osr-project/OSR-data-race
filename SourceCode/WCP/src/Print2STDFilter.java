import cmd.CmdOptions;
import cmd.GetOptions;
import engine.print.PrintEngine;

public class Print2STDFilter {

	public Print2STDFilter() {

	}
	
	public static void main(String[] args) {	
		CmdOptions options = new GetOptions(args).parse();
		PrintEngine engine = new PrintEngine(options.parserType, options.path, true);
		engine.analyzeTrace();
	}
}
