import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class WiretapToSTD {

    private static PrintWriter locationWriter;
    private static Map<String, String> methodMap;

    public static void main(String[] args) {

        methodMap = new HashMap<>();

        File locationsLog = new File(args[1]);
        try {
            locationWriter = new PrintWriter(locationsLog);
        } catch (Exception ex) {
            System.err.println("Cannot open location log file\n");
            System.exit(-1);
        }

        File wiretapLog = new File(args[0]);
        try (BufferedReader br = new BufferedReader(new FileReader(wiretapLog))) {
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        locationWriter.flush();
        locationWriter.close();
    }

    private static void processLine(String line) {
        String[] pieces = line.split(" ");
        int dotIndex = pieces[0].indexOf(".");
        String thread = pieces[0].substring(0, dotIndex);
        String thread_ord = pieces[0].substring(dotIndex + 1);
        thread_ord = Integer.toString(Integer.parseInt(thread_ord));
        pieces[0] = thread + "." + thread_ord;
        
        
        String operation = "";
        switch (pieces[1]) {
            case "enter":
                processEnter(thread, pieces[3]);
                break;
            case "write":
                operation = "w(" + pieces[2] + ")";
                break;
            case "read":
                operation = "r(" + pieces[2] + ")";
                break;
            case "fork":
                operation = "fork(" + pieces[2] + ")";
                break;
            case "join":
                operation = "join(" + pieces[2] + ")";
                break;
            case "request":
                operation = "acq(" + pieces[2] + ")";
                locationWriter.println(pieces[0] + " " + methodMap.get(thread));
                break;
            case "acquire":
                //operation = "acq(" + pieces[2] + ")";
                break;
            case "release":
                operation = "rel(" + pieces[2] + ")";
                break;
            case "branch":
                operation = "branch";
                break;
            case "begin":
                break;
            case "end":
                break;
            default:
                System.err.println("Unknown operation: " + pieces[1] + " \n");
        }
        if (!operation.equals("")) {
            System.out.println(thread + "|" + operation + "|" + thread_ord);
        }

    }

    private static void processEnter(String thread, String methodName) {
        methodMap.put(thread, methodName);
    }
}
