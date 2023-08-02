import cmd.CmdOptions;
import cmd.GetOptions;
import engine.metainfo.MetaInfoEngine;

public class MetaInfo {

	public MetaInfo() {

	}
	
	public static void main(String[] args) {	
		CmdOptions options = new GetOptions(args).parse();
		MetaInfoEngine engine = new MetaInfoEngine(options.parserType, options.path);
		if(options.limitEvents > 0) {
			engine.analyzeTrace(options.limitEvents);
		}
		else {
			engine.analyzeTrace();
		}
	}
}
