public class Main {
    public static void main(String[] args) {
        String algo = args[0];
        String trace_dir = args[1];

        if(algo.equals("WCP")){
            WCP.analysis(trace_dir);
        } else if(algo.equals("WCPPerVar")) {
            WCPPerVar.analysis(trace_dir);
        }
    }
}
