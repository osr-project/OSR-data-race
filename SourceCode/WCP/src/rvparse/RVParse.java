package rvparse;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RVParse {
	public static void main(String[] args) {
		Path path = Paths.get("/Users/umang/Downloads/airlinetickets/0_trace.bin");
		try {
			RVEventReader reader = new RVEventReader(path);
			try {
				while (true) {
					System.out.println(reader.readEvent().toString());
				}
			} catch (EOFException e) {
				reader.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
