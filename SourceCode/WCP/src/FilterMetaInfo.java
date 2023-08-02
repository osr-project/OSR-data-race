import cmd.CmdOptions;
import cmd.GetOptions;
import engine.metainfo.FilterMetaInfoEngine;

public class FilterMetaInfo {

	public FilterMetaInfo() {

	}
	
	public static void main(String[] args) {	
		CmdOptions options = new GetOptions(args).parse();
		FilterMetaInfoEngine engine = new FilterMetaInfoEngine(options.parserType, options.path);
		if(options.limitEvents > 0) {
			engine.analyzeTrace(options.limitEvents);
		}
		else {
			engine.analyzeTrace();
		}
	}
}
