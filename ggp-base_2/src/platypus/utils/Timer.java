package platypus.utils;

public class Timer {
	public long startTime;
	public long finishTime;
	private Timer(long startTime, long finishTime) {
		super();
		this.startTime = startTime;
		this.finishTime = finishTime;
	}
	
	public static Timer createDeadlineTimer(long deadline){
		return new Timer(System.currentTimeMillis(), deadline);
	}
	
	public static Timer createStopwatchTimer(long totalTime){
		long c = System.currentTimeMillis();
		return new Timer(c, c+totalTime);
	}
	
	public long getTimeRemaining(){
		return finishTime - System.currentTimeMillis();
	}
	
	public long getTotalTimeRunning(){
		return System.currentTimeMillis() - startTime;
	}
	
	public long getTotalTime(){
		return finishTime - startTime;
	}
}
