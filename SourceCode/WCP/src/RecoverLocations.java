import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class RecoverLocations {

	private static Map<String, String> methodMap;
	private static HashSet<HashSet<String>> methodPairs;

	public static void main(String[] args) {

		methodMap = new HashMap<>();
		methodPairs = new HashSet<HashSet<String>> ();

		File locationsLog = new File(args[0]);
		try (BufferedReader br = new BufferedReader(new FileReader(locationsLog))) {
			String line;
			while ((line = br.readLine()) != null) {
				processLine(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		File eventsLog = new File(args[1]);
		try (BufferedReader br = new BufferedReader(new FileReader(eventsLog))) {
			String line;
			while ((line = br.readLine()) != null) {
				processEvent(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (HashSet<String> pair: methodPairs){

			for(String method : pair){
				System.out.print(method + " ");
			}
			if(pair.size() == 1){
				for(String method : pair){
					System.out.print(method + " ");
				}
			}
			System.out.println("");
		}
	}

	private static void processLine(String line) {
		String[] pieces = line.split(" ");

		methodMap.put(pieces[0], pieces[1]);
	}

	private static void processEvent(String line) {
		String[] pieces = line.split(" ");

		String method1 = methodMap.get(pieces[0]);
		String method2 = methodMap.get(pieces[1]);

		HashSet<String> pair = new HashSet<String> ();
		pair.add(method1);
		pair.add(method2);
		methodPairs.add(pair);
	}
}
