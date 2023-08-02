import engine.accesstimes.orderedvars.OrderedVarsEngine;
import parse.ParserType;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;

public class FilterTrace {
    public static void main(String[] args) throws Exception {
        String traceDir = args[0];
        String saveLoc = args[1];

        filter(traceDir, saveLoc);
//        filter("C:\\Research\\traces\\RaceInjector\\shb_missed\\treeset\\injectedTrace97.std",
//                "C:\\Research\\traces\\RaceInjector\\shb_missed\\treeset\\injectedTrace97_filter.std");
    }

    public static OrderedVarsEngine findOrderdVars(String traceDir){
        OrderedVarsEngine orderedVarsEngine = new OrderedVarsEngine(ParserType.STD, traceDir, 0);
        orderedVarsEngine.analyzeTrace(true, 0);
        return orderedVarsEngine;
    }

    public static void filter(String traceDir, String saveLocation) throws Exception{
        OrderedVarsEngine engine = findOrderdVars(traceDir);
        HashSet<String> orderdVars = engine.getOrdredVars();
        HashMap<String, HashSet<String>> lockToThreadSet = engine.getLockToThreadSet();


        File file = new File(traceDir);
        File wFile = new File(saveLocation);
        InputStreamReader input = new InputStreamReader(new FileInputStream(file));

        BufferedReader br = new BufferedReader(input);
        BufferedWriter out = new BufferedWriter(new FileWriter(wFile));
        String line = "";

        line = br.readLine();
        while(line != null){
            String op = line.split("\\|")[1];
            String tmp = op.split("\\(")[1];
            String var = tmp.substring(0, tmp.length() - 1);
            boolean flag = true;
            if(orderdVars.contains(var)){
                flag = false;
            }

            if(lockToThreadSet.containsKey(var) && lockToThreadSet.get(var).size() == 1){
                flag = false;
            }

            if(flag) {
                out.write(line + "\n");
                out.flush();
            }
            line = br.readLine();
        }

        out.close();
        input.close();
    }
}
