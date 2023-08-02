import cmd.CmdOptions;
import cmd.GetOptions;
import engine.print.ZipTrackPrintEngine;

public class ZipTrackPrint {

	public ZipTrackPrint() {

	}
	
	public static void main(String[] args) {	
		CmdOptions options = new GetOptions(args).parse();
		ZipTrackPrintEngine engine = new ZipTrackPrintEngine(options.parserType, options.path, false);
		engine.analyzeTrace();
	}
}
