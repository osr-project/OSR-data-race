package debug;

import event.EventType;

import java.text.DecimalFormat;

/**
 * Created by matt on 4/17/18.
 */
public class EventStatistics {

	protected static final DecimalFormat formatter = new DecimalFormat("###.###");

	protected static boolean enabled = false;

	private static long beginTime = 0;
	private static long endTime = 0;
	private static long aquireTime = 0;
	private static long releaseTime = 0;
	private static long readTime = 0;
	private static long writeTime = 0;
	private static long forkTime = 0;
	private static long joinTime = 0;
	private static long innerWriteTime = 0;

	private static int numBegin = 0;
	private static int numEnd = 0;
	private static int numAquire = 0;
	private static int numRelease = 0;
	private static int numRead = 0;
	private static int numWrite = 0;
	private static int numFork = 0;
	private static int numJoin = 0;
	
	private static int lightGreyCount = 0;
	private static int darkGreyCount = 0;
	private static int numCopyIsMonotone = 0;
	private static int numCopyIsNotMonotone = 0;

	public static long getBeginTime() {
		return beginTime;
	}

	public static long getEndTime() {
		return endTime;
	}

	public static long getAquireTime() {
		return aquireTime;
	}

	public static long getForkTime() {
		return forkTime;
	}

	public static long getJoinTime() {
		return joinTime;
	}

	public static long getReadTime() {
		return readTime;
	}

	public static long getReleaseTime() {
		return releaseTime;
	}

	public static long getWriteTime() {
		return writeTime;
	}

	public static void updateBeginTime(long beginTime) {
		EventStatistics.numBegin++;
		EventStatistics.beginTime = EventStatistics.beginTime + beginTime;
	}

	public static void updateEndTime(long endTime) {
		EventStatistics.numEnd++;
		EventStatistics.endTime = EventStatistics.endTime + endTime;
	}

	public static void updateAquireTime(long aquireTime) {
		EventStatistics.numAquire++;
		EventStatistics.aquireTime = EventStatistics.aquireTime + aquireTime;
	}

	public static void updateForkTime(long forkTime) {
		EventStatistics.numFork++;
		EventStatistics.forkTime = EventStatistics.forkTime + forkTime;
	}

	public static void updateJoinTime(long joinTime) {
		EventStatistics.numJoin++;
		EventStatistics.joinTime = EventStatistics.joinTime + joinTime;
	}

	public static void updateReadTime(long readTime) {
		EventStatistics.numRead++;
		EventStatistics.readTime = EventStatistics.readTime + readTime;
	}

	public static void updateReleaseTime(long releaseTime) {
		EventStatistics.numRelease++;
		EventStatistics.releaseTime = EventStatistics.releaseTime + releaseTime;
	}

	public static void updateWriteTime(long writeTime) {
		EventStatistics.numWrite++;
		EventStatistics.writeTime = EventStatistics.writeTime + writeTime;
	}

	public static void updateInnerWriteTime(long writeTime) {
		EventStatistics.innerWriteTime = EventStatistics.innerWriteTime + writeTime;
	}
	
	public static void updateLightGreyCount(int x) {
		EventStatistics.lightGreyCount += x;
	}
	
	public static void updateDarkGreyCount(int x) {
		EventStatistics.darkGreyCount += x;
	}
	
	public static void copyIsMonotone() {
		EventStatistics.numCopyIsMonotone ++;
	}
	
	public static void copyNotMonotone() {
		EventStatistics.numCopyIsNotMonotone ++;
	}

	public static boolean isEnabled() {
		return EventStatistics.enabled;
	}

	public static void enable() {
		EventStatistics.enabled = true;
	}

	public static void updateTime(EventType type, long time) {
		switch (type) {
		case BEGIN:
			updateBeginTime(time);
			break;
		case END:
			updateEndTime(time);
			break;
		case ACQUIRE:
			updateAquireTime(time);
			break;
		case RELEASE:
			updateReleaseTime(time);
			break;
		case READ:
			updateReadTime(time);
			break;
		case WRITE:
			updateWriteTime(time);
			break;
		case FORK:
			updateForkTime(time);
			break;
		case JOIN:
			updateJoinTime(time);
			break;
		default:
		}
	}

	public static void print() {
		System.out.println("---------------TC Statistics---------------");

		System.out.println("Light grey area = " + EventStatistics.lightGreyCount);
		
		System.out.println("Dark grey area = " + EventStatistics.darkGreyCount);
		
		System.out.println("Monotone-Copy in Write = " + EventStatistics.numCopyIsMonotone);
		
		System.out.println("Non-Monotone-Copy in Write = " + EventStatistics.numCopyIsNotMonotone);
		
		System.out.println("");

		
		System.out.println("---------------Event Statistics---------------");

		System.out.println("Begin Time: " + beginTime + ", Avg= "
				+ (numBegin != 0 ? formatter.format((beginTime / (numBegin * 1.0))) : 0)
				+ ", Num= " + numBegin);

		System.out.println("End Time: " + endTime + ", Avg= "
				+ (numEnd != 0 ? formatter.format((endTime / (numEnd * 1.0))) : 0)
				+ ", Num= " + numEnd);

		System.out.println("Aquire Time: " + aquireTime + ", Avg= "
				+ (numAquire != 0 ? formatter.format((aquireTime / (numAquire * 1.0)))
						: 0)
				+ ", Num= " + numAquire);

		System.out.println("Release Time: " + releaseTime + ", Avg= "
				+ (numRelease != 0 ? formatter.format((releaseTime / (numRelease * 1.0)))
						: 0)
				+ ", Num= " + numRelease);

		System.out.println("Read Time: " + readTime + ", Avg= "
				+ (numRead != 0 ? formatter.format((readTime / (numRead * 1.0))) : 0)
				+ ", Num= " + numRead);

		System.out.println("Write Time: " + writeTime + ", Avg= "
				+ (numWrite != 0 ? formatter.format((writeTime / (numWrite * 1.0))) : 0)
				+ ", Num= " + numWrite);

		System.out.println("Inner Write Time: " + innerWriteTime + ", Avg= "
				+ (numWrite != 0 ? formatter.format((innerWriteTime / (numWrite * 1.0)))
						: 0)
				+ ", Num= " + numWrite);

		System.out.println("Fork Time: " + forkTime + ", Avg= "
				+ (numFork != 0 ? formatter.format((forkTime / (numFork * 1.0))) : 0)
				+ ", Num= " + numFork);

		System.out.println("Join Time: " + joinTime + ", Avg= "
				+ (numJoin != 0 ? formatter.format((joinTime / (numJoin * 1.0))) : 0)
				+ ", Num= " + numJoin);

		System.out.println("Total Time: " + (beginTime + endTime + aquireTime
				+ releaseTime + readTime + writeTime + forkTime + joinTime));
		
	}
}
