package platypus.logging;

import java.io.IOException;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import players.PlatypusConstants;

public class PlatypusLogger{

	public static Logger getLogger(String name, Level level){
		Logger logger = Logger.getLogger(name);
	    logger.setLevel(level);
		try {
			
			Formatter formatter = new Formatter() {

	            @Override
	            public String format(LogRecord arg0) {
	                StringBuilder b = new StringBuilder();
	                b.append(System.getProperty("line.spearator"));
	                b.append("[");
	                b.append(arg0.getLevel());
	                b.append("] ");
	                b.append(" ");
	                b.append(arg0.getMessage());
	                b.append(System.getProperty("line.separator"));
	                return b.toString();
	            }

	        };
			
			if (PlatypusConstants.writeLogToFile){
				logger.addHandler(new FileHandler("logs/platypus/"+name+".log"));
			}
			if (PlatypusConstants.writeLogToConsole){
				logger.addHandler(new PlatypusConsole());
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        //logger.info("Added FileHandler to Logger");
        return logger;
	}
	public static Logger getLogger(String name){
		return getLogger(name, PlatypusConstants.LogLevel);
	}

}
