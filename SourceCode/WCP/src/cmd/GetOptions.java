package cmd;

import java.util.logging.Level;
import java.util.logging.Logger;

import debug.EventStatistics;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import parse.ParserType;

public class GetOptions {

    private static final Logger log = Logger.getLogger(GetOptions.class.getName());
    private String[] args = null;
    private Options options = new Options();

    public GetOptions(String[] args) {
        this.args = args;
        options.addOption("h", 	"help", false, "generate this message");
        options.addOption("f", 	"format", true, "format of the trace. Possible choices include rv, csv, std and rr (Default : rv) ");
        options.addOption("o", 	"offline", false, "for offline analysis");
        options.addOption("s", 	"single", false, "force the algorithm to terminate after the first race is detected");
        options.addOption("e", 	"explicit", false, "force the algorithm to explicitly order the concurrent conflicting events after a race is detected. Effective only when the \'single\' option is switched off.");
        options.addOption("i", 	"incrementclock", false, "force the algorithm to increment a thread's local clock on a read/write event for the thread.");
        options.addOption("p", 	"path", true, "the path to the trace file/folder (Required)");
        options.addOption("q",	"output-path", true, "the path to output ");
        options.addOption("v", 	"verbosity", true, "for setting verbosity: Allowed levels = 0, 1, 2 (Default : 0)");
        options.addOption("d", 	"debug", false, "print debuging information");
        options.addOption("m", 	"excluded-methods", true, "path to file that lists methods to be excluded");
        options.addOption("l", 	"limit-events", true, "number of events to consider");
    }

    public CmdOptions parse() {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        CmdOptions cmdOpt = new CmdOptions();;
        
        try {
            cmd = parser.parse(options, args);
            if (cmd.hasOption("h"))
                help();
            
            if (cmd.hasOption("f")) {
                //log.log(Level.INFO, "Using cli argument -f " + cmd.getOptionValue("f"));
                cmdOpt.parserType = ParserType.getType(cmd.getOptionValue("f")) ;   
            } 
            
            if (cmd.hasOption("o")) {
            	//log.log(Level.INFO, "Using cli argument -o ");
                cmdOpt.online = false;  
            }
            
            if (cmd.hasOption("s")) {
            	//log.log(Level.INFO, "Using cli argument -s ");
                cmdOpt.multipleRace = false;  
            }
            
            if (cmd.hasOption("e")) {
            	//log.log(Level.INFO, "Using cli argument -e ");
                cmdOpt.forceOrdering = true;  
            }
            
            if (cmd.hasOption("i")) {
            	//log.log(Level.INFO, "Using cli argument -i ");
                cmdOpt.tickClockOnAccess = true;  
            }
            
            if (cmd.hasOption("v")) {
            	//log.log(Level.INFO, "Using cli argument -s ");
            	try{
            		cmdOpt.verbosity = Integer.parseInt(cmd.getOptionValue("v"));
            		if(cmdOpt.verbosity < 0 || cmdOpt.verbosity > 3){
            			log.log(Level.INFO, "Invalid verbosity level : " + cmdOpt.verbosity);
            		}
            	}
            	catch (NumberFormatException nfe){
            		log.log(Level.INFO, "Invalid verbosity option : " + cmd.getOptionValue("v"));
            	}
            }

            if(cmd.hasOption("d")) {
                EventStatistics.enable();
            }

            if (cmd.hasOption("p")) {
            	//log.log(Level.INFO, "Using cli argument -p=" + cmd.getOptionValue("p"));
                cmdOpt.path = cmd.getOptionValue("p") ;   
            }
            else {
                log.log(Level.INFO, "Missing path to file/folder");
                help();
            }
            
            if (cmd.hasOption("q")) {
                cmdOpt.output_path = cmd.getOptionValue("q") ;   
            }
            
            if (cmd.hasOption("m")) {
                cmdOpt.excludeList = cmd.getOptionValue("m") ;   
            }
            
            if (cmd.hasOption("l")) {
            	//log.log(Level.INFO, "Using cli argument -p=" + cmd.getOptionValue("p"));
                cmdOpt.limitEvents = Long.parseLong(cmd.getOptionValue("l")) ;   
            }
 
        } catch (ParseException e) {
        	log.log(Level.INFO, "Failed to parse command line properties", e);
            help();
        }
        
//        System.out.println(cmdOpt.toString());
        return cmdOpt;
    }

    private void help() {
        // This prints out some help
        HelpFormatter formater = new HelpFormatter();

        formater.printHelp("RAPID", options);
        System.exit(0);
    }

    public static void main(String[] args) {
        new GetOptions(args).parse();
    }
}
