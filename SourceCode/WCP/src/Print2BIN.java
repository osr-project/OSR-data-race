import cmd.CmdOptions;
import cmd.GetOptions;
import engine.print.binary.BinaryFormatPrintEngine;

public class Print2BIN {

	public Print2BIN() {

	}
	
	public static void main(String[] args) {	
		CmdOptions options = new GetOptions(args).parse();
		BinaryFormatPrintEngine engine = new BinaryFormatPrintEngine(options.parserType, options.path, false, options.output_path);
		engine.analyzeTrace();
	}
}
