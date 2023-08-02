# OSR Artifact

1. ### Organization of the artifact
   - SourceCode
       - OSR-SyncP-SHB
       - M2PerEvent
       - M2PerVar
       - WCP
   - Tools (executables)
       - OSR-SHB-SyncP tools
           - `rapid.jar`
           - `lib`
           - `generateMetainfo.py`
       - M2 tools
           - `M2PerEvent.jar`
           - `M2PerVar.jar`
           - `jgrapht-1.2.0`
           - `std2m2.py`
       - WCP tools
           - `WCP.jar`
           - `WCPPerVar.jar`
           - `lib`
   - Traces
       - Java Benchmarks
       - C/C++ Benchmarks(too large, currently not available)

2. ### Environment
   - JDK-19
   - python3
   - ant (for compiling java project)

3. ### Compilation
   - We already prepared a jar file for each source code project under `Tools` folder
   - Source code for each algorithm is also attached in `SourceCode` folder
   - For WCP source code and OSR-SyncP-SHB source code, users can enter the folder and call `ant jar`, then a jar file will be created in the project folder

4. ### Benchmarks
   - Java Benchmarks: under `./Traces/Java-Benchmarks`, we have prepared a zip file, containing filtered Java traces in std format.
   - C/C++ Benchmarks: The size of C/C++ benchmarks is too large. Even after compression, it's around 100GB. We will release this benchmark later.

5. ### Preprocessing of std traces
   - This preprocessing takes a trace `input.std` as input and outputs a filtered trace `output.std`, in which thread-local events are removed.
   - Additionally, all variables, s.t. all accesses to this variable are ordered by thread order and read-from relation are also removed.
   - To run this, `java -cp "[dir to rapid.jar]:[dir to rapid lib folder]:[dir to rapid lib/jgrapht folder]" FilterTrace [dir to input.std] [dir to output.std]`

6. ### Run race detection on traces for SHB / SyncP / OSR
   - `java -cp "[dir to rapid.jar]:[dir to rapid lib folder]:[dir to rapid lib/jgrapht folder]" Main [algo name] [dir to trace.std]`
   - Available [algo name] values: `OSR OSRPerVar SHB SyncP SHBPerVar SPPerVar`
   - Analysis results: they will be printed in the console 
   - OSR needs some meta-info of the trace. Here we provide a script `generateMetaInfo.py` that traverse the trace once and write the number of events in each threads / locks / variables to a file ending with `.ssp`. Users should run this python script before applying OSR algorithm on traces
   - To run this meta-info script, `python3 generateMetaInfo.py [dir to .std trace]`
   - Example: Suppose there is a trace under `/opt/test.std`. The tool is under `/opt/rapid.jar` and libs are under `/opt/lib`, `/opt/lib/jgrapht`. To apply `SHB` on `/opt/test.std`, run command `java -cp "/opt/rapid.jar:/opt/lib/*:/opt/lib/jgrapht/*" Main SHB /opt/test.std`

7. ### Run race detection on traces for WCP
   - `java -cp "[dir to WCP.jar]:[dir to WCP lib folder]" Main [algo name] [dir to trace.std]`
   - Available [algo name] values: `WCP WCPPerVar`
   - Analysis results: they will be printed in the console 

8.  ### Run M2 algo on traces
   - M2 takes a special format `.m2` which is different from `.std` file. Here we provide a script `std2m2.py` transferring `.std` traces to `.m2` trace
   - To transfer traces, run `python std2m2.py [dir to .std trace]`, it will create a `.m2` file in the same folder as `.std` file
   - To do race per event detection, `java -cp "[dir to M2PerEvent.jar]:[dir to jgrapht-1.2.0]" Main [dir to trace.std]`
   - To do race per variable detection, `java -cp "[dir to M2PerVar.jar]:[dir to jgrapht-1.2.0]" Main [dir to trace.std]`
   - Example: Suppose there is a trace under `/opt/test.m2`, and the tool is under `/opt/M2PerEvent.jar`, `/opt/jgrapht-1.2.0`. To apply `M2 per event` on it, run command `java -cp "/opt/M2PerEvent.jar:/opt/jgrapht-1.2.0/*" Main /opt/test.m2`
