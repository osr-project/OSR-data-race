package parse.bin;

import java.util.HashMap;

import event.EventType;

public class BinaryFormat {

	/* A valid file consists of a sequence of bytes.
	 * The first few bytes store metadata such as number of events, threads, locks or memory locations,
	 * and the rest of the file is a sequence of event representations.
	 * The metadata information we store (in the same order) is:
	 * 1. Number of threads (15 bits + 1 signed bit)
	 * 2. Number of locks (31 bits + 1 signed bit)
	 * 3. Number of memory locations (31 bits + 1 signed bit)
	 * 4. Number of events (63 bits + 1 signed bit)
	 * Each event is represented using a long type (8 consecutive bytes), 
	 * having the following bit representation.
	 * S <--- L ---> <--- D ----> <--- O ---> <--- T ---> 
	 * The above denotes the representation of events. 
	 * The most significant bit S is not used as it may represent sign bit.
	 * The next L significant bits store the location identifier for this event.
	 * The next D bits denote the decor (variable, lock or thread)
	 * identifier. The next O bits denote the operation
	 * (read/write/acq/release/fork/join). The last T bits (also the first T least
	 * significant bits) are used to store the thread identifier.
	 */
	
	public static short NUMBER_OF_THREADS_MASK = 0x7FFF;
	public static int NUMBER_OF_LOCKS_MASK = 0x7FFFFFFF;
	public static int NUMBER_OF_VARS_MASK = 0x7FFFFFFF;
	public static long NUMBER_OF_EVENTS_MASK = 0x7FFFFFFFFFFFFFFFL;

	public static short THREAD_NUM_BITS = 10; // MAX_THREADS = 1024
	public static short THREAD_BIT_OFFSET = 0;
	public static short OP_NUM_BITS = 4; // 16 different kinds of operations
	public static short OP_BIT_OFFSET = THREAD_NUM_BITS;
	public static short DECOR_NUM_BITS = 34; // 2^34 locks or threads
	public static short DECOR_BIT_OFFSET = (short) (THREAD_NUM_BITS + OP_NUM_BITS);
	public static short LOC_NUM_BITS = 15; // 2^15 locations
	public static short LOC_BIT_OFFSET = (short) (THREAD_NUM_BITS + OP_NUM_BITS + DECOR_NUM_BITS);

	public static long THREAD_MASK = ((1L << THREAD_NUM_BITS) - 1) << THREAD_BIT_OFFSET;
	public static long OP_MASK = ((1L << OP_NUM_BITS) - 1) << OP_BIT_OFFSET;
	public static long DECOR_MASK = ((1L << DECOR_NUM_BITS) - 1) << DECOR_BIT_OFFSET;
	public static long LOC_MASK = ((1L << LOC_NUM_BITS) - 1) << LOC_BIT_OFFSET;

	public static EventType[] event_types = { EventType.ACQUIRE, EventType.RELEASE,
			EventType.READ, EventType.WRITE, EventType.FORK, EventType.JOIN,
			EventType.BEGIN, EventType.END, EventType.REQUEST, EventType.BRANCH };

	public static EventType getBinaryEventType(short op) throws IllegalArgumentException {
		if (op < 0 || op > event_types.length) {
			throw new IllegalArgumentException("Op is invalid. Expecting a number in [0,"
					+ (event_types.length - 1) + "]. Got " + op);
		}
		return event_types[op];
	}
	
	public static HashMap<EventType, Short> eventTypeBinaryMap = new HashMap<EventType, Short> ()  {/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	{
        put(EventType.ACQUIRE, (short) 0);
        put(EventType.RELEASE, (short) 1);
        put(EventType.READ, (short) 2);
        put(EventType.WRITE, (short) 3);
        put(EventType.FORK, (short) 4);
        put(EventType.JOIN, (short) 5);
        put(EventType.BEGIN, (short) 6);
        put(EventType.END, (short) 7);
        put(EventType.REQUEST, (short) 8);
        put(EventType.BRANCH, (short) 9);
    }};
    
    public static short getEventType2Binary(EventType tp) throws IllegalArgumentException {
		if (!eventTypeBinaryMap.containsKey(tp)) {
			throw new IllegalArgumentException("Invalid type :" + tp.toString());
		}
		return eventTypeBinaryMap.get(tp);
	}

	public static void demo() {
		// event = 0 [(0)^11  1 0 1 0] [(0)^29 1 1 1 0 1 ] [0 1 0 0] [(0)^6 1 0 0 0]
		// = 00000000 00001010 00000000 00000000 00000000 00000111 01010000 00001000
		long event = 2814749767585800L;
		long tid = (event & THREAD_MASK) >> THREAD_BIT_OFFSET;
		long op = (event & OP_MASK) >> OP_BIT_OFFSET;
		long decor = (event & DECOR_MASK) >> DECOR_BIT_OFFSET;
		long loc = (event & LOC_MASK) >> LOC_BIT_OFFSET;

		System.out.println("Thread Mask = " + THREAD_MASK); // 1023
		System.out.println("Op Mask = " + OP_MASK); // 15360
		System.out.println("Decor Mask = " + DECOR_MASK); // 281474976694272
		System.out.println("Loc Mask = " + LOC_MASK); // 9223090561878065152

		System.out.println("Thread = " + tid); // 8
		System.out.println("Op = " + op); // 4
		System.out.println("Decor = " + decor); // 29
		System.out.println("Loc = " + loc); // 10
	}

	public static void main(String[] args) {
		demo();
	}
}
