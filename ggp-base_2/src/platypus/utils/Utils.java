package platypus.utils;

public class Utils {
	public static void timeout(long time){
		
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
