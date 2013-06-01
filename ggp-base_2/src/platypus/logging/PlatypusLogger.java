package platypus.logging;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlatypusLogger{

	public static Logger getLogger(String name, Level level){
		Logger logger = Logger.getLogger(name);
	    logger.setLevel(level);
		try {
			logger.addHandler(new FileHandler("logs/platypus/"+name+".log"));
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        logger.info("Added FileHandler to Logger");
        return logger;
	}
	public static Logger getLogger(String name){
		return getLogger(name, Level.ALL);
	}

}
