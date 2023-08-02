package util;

public final class TripletHardCodeWordTricks {

	// The arrangement is TID - PCLOCK - CLOCK
	public static final int TID_BITS = 8;
	public static final int CLOCK_BITS = 28;
	public static final int PCLOCK_BITS = 28;
	public static final long TID_MASK = (0xffffffffffffffffL >>> (PCLOCK_BITS
			+ CLOCK_BITS)) << (PCLOCK_BITS + CLOCK_BITS);
	public static final long PCLOCK_MASK = ((0xffffffffffffffffL >>> PCLOCK_BITS) << (TID_BITS
			+ PCLOCK_BITS)) >>> (TID_BITS);
	public static final long CLOCK_MASK = (0xffffffffffffffffL << (TID_BITS
			+ PCLOCK_BITS)) >>> (TID_BITS + PCLOCK_BITS);
	public static final long PCLOCK_CLOCK_MASK = (0xffffffffffffL << (TID_BITS)) >>> (TID_BITS);

	public static short getTid(long data) {
		return (short) (data >>> (PCLOCK_BITS + CLOCK_BITS));
	}

	public static int getClock(long data) {
		return (int) (data & CLOCK_MASK);
	}

	public static int getPclock(long data) {
		return (int) ((data & ~TID_MASK) >> (CLOCK_BITS));
	}

	public static long setDataWithTidAndClock(int tid, int clock) {
		return (((long) tid) << (CLOCK_BITS + PCLOCK_BITS)) + clock;
	}

	public static long setDataWithTid(int tid) {
		return ((long) tid) << (CLOCK_BITS + PCLOCK_BITS);
	}

	public static long copyTid(long from, long to) {
		to &= ~TID_MASK;
		to |= from & TID_MASK;
		return to;
	}

	public static long copyClock(long from, long to) {
		to &= ~CLOCK_MASK;
		to |= from & CLOCK_MASK;
		return to;
	}

	public static long copyPclock(long from, long to) {
		to &= ~PCLOCK_MASK;
		to |= from & PCLOCK_MASK;
		return to;
	}

	public static long copyClockAndPclock(long from, long to) {
		to &= ~PCLOCK_CLOCK_MASK;
		to |= from & PCLOCK_CLOCK_MASK;
		return to;
	}

	public static long copyClockToPclock(long from, long to) {

		to &= ~PCLOCK_MASK;
		to |= (from & CLOCK_MASK) << CLOCK_BITS;

		return to;
	}

	public static long setTid(short tid, long to) {
		to &= ~TID_MASK;
		to |= ((long) tid) << PCLOCK_BITS + CLOCK_BITS;
		return to;
	}

	public static long setPclock(int pclock, long to) {
		to &= ~PCLOCK_MASK;
		to |= ((long) pclock) << CLOCK_BITS;
		return to;
	}

	public static long setClock(int clock, long to) {
		to &= ~CLOCK_MASK;
		to |= clock;
		return to;
	}

	public static long incrementClockBy(int inc, long to) {
		to += inc;
		return to;
	}

	public static boolean clockIsLessThan(long first, long second) {
		return (first & CLOCK_MASK) < (second & CLOCK_MASK);
	}

	public static boolean clockIsLessThanOrEqual(long first, long second) {
		return (first & CLOCK_MASK) <= (second & CLOCK_MASK);
	}

	public static String toString(long data) {

		// String str = String.format("0x%016x", data) + " - ";
		String str = "";
		str += "<";
		str += getTid(data);
		str += ", ";
		str += getClock(data);
		str += ", ";
		str += getPclock(data);
		str += ">";
		return str;
	}
}
