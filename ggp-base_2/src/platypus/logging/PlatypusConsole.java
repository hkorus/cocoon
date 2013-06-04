package platypus.logging;

import java.util.logging.ConsoleHandler;

public class PlatypusConsole extends ConsoleHandler {

	public PlatypusConsole(){
		setOutputStream(System.out);
	}
}
