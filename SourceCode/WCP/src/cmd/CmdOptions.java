package cmd;

import parse.ParserType;

public class CmdOptions {
	
	public ParserType parserType;
	public boolean online;
	public boolean multipleRace;
	public boolean forceOrdering;
	public boolean tickClockOnAccess;
	public String path;
	public String output_path;
	public int verbosity;
	public String excludeList;
	public Long limitEvents;

	public CmdOptions() {
		this.parserType = ParserType.RV;
		this.online = true;
		this.multipleRace = true;
		this.forceOrdering = false;
		this.tickClockOnAccess = false;
		this.path = null;
		this.output_path = null;
		this.verbosity = 0;
		this.excludeList = null;
		this.limitEvents = -1L;
	}
	
	public String toString(){
		String str = "";
		str += "parserType			" + " = " + this.parserType.toString() 	+ "\n";
		str += "online				" + " = " + this.online					+ "\n";
		str += "multipleRace		" + " = " + this.multipleRace			+ "\n";	
		str += "forceOrdering		" + " = " + this.forceOrdering			+ "\n";
		str += "tickClockOnAccess	" + " = " + this.tickClockOnAccess		+ "\n";	
		str += "path				" + " = " + this.path					+ "\n";
		str += "output_path			" + " = " + this.output_path			+ "\n";
		str += "verbosity			" + " = " + this.verbosity				+ "\n";
		str += "excludeList			" + " = " + this.excludeList			+ "\n";
		str += "limitEvents			" + " = " + this.limitEvents			+ "\n";
		return str;
	}

}
