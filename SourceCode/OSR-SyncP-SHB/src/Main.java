import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {

        if(args.length == 2){
            String algo = args[0];
            String trace_dir = args[1];

            if(algo.equals("SyncP")){
                SyncPNoOrderVars.analysis(trace_dir);
            } else if(algo.equals("SHB")) {
                SHB.analysis(trace_dir);
            } else if(algo.equals("SPPerVar")){
                SPPerVar.analysis(trace_dir);
            } else if(algo.equals("SHBPerVar")){
                SHBPerVar.analysis(trace_dir);
            } else if(algo.equals("OSR")){
                OSR.analysis(trace_dir);
            } else if(algo.equals("OSRPerVar")){
                OSRPerVar.analysis(trace_dir);
            } else {
                System.out.println("No matching algo, please check your input!");
            }
        } else {
            System.out.println("Wrong input arguments, see readme.md for input format");
            System.exit(1);
        }


    }
}
