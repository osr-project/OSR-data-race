package parse;

public enum ParserType {
	RV, RR, CSV, STD, BIN;
	
	public boolean isRV(){
		return this.ordinal() == RV.ordinal();
	}
	
	public boolean isRR(){
		return this.ordinal() == RR.ordinal();
	}
	
	public boolean isCSV(){
		return this.ordinal() == CSV.ordinal();
	}
	
	public boolean isSTD(){
		return this.ordinal() == STD.ordinal();
	}
	
	public boolean isBIN(){
		return this.ordinal() == BIN.ordinal();
	}

//	public boolean isLogType() {
//		return this.isRR() || this.isCSV() || this.isSTD();
//	}
//	
//	public boolean isBinType() {
//		return this.isRV();
//	}
	
	public String toString(){
		String str = "";
		if(isRV()) str = "RV";
		else if (isCSV()) str = "CSV";
		else if (isRR()) str = "RR";
		else if (isSTD()) str = "STD";
		else if (isBIN()) str = "BIN";
		return str;
	}
	
	public static ParserType getType(String str){
		if(str.equals("csv")) return CSV;
		else if (str.equals("rr")) return RR;
		else if (str.equals("std")) return STD;
		else if (str.equals("rv")) return RV;
		else return BIN;
	}
}
