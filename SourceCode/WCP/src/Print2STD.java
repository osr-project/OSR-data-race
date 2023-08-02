import cmd.CmdOptions;
import cmd.GetOptions;
import engine.print.PrintEngine;

public class Print2STD {

	public Print2STD() {

	}
	
	public static void main(String[] args) {	
		CmdOptions options = new GetOptions(args).parse();
		PrintEngine engine = new PrintEngine(options.parserType, options.path, false);
		engine.analyzeTrace();
	}
}
