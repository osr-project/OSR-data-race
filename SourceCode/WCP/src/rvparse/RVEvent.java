package rvparse;

public class RVEvent implements Comparable<RVEvent> {
	private long GID;
	private long TID;
	private int ID;
	private long ADDR;
	private long VALUE;
	private RVEventType TYPE;

	private static final int SIZEOF_LONG = 8;
	private static final int SIZEOF_INT = 4;
	private static final int SIZEOF_EVENT_TYPE = 1;

	/**
	 * constant representing the size of the event item on disk (no. of bytes).
	 * This should be updated whenever structure of the class is changed.
	 */
	public static final int SIZEOF = SIZEOF_LONG // GID
			+ SIZEOF_LONG // TID
			+ SIZEOF_INT // ID
			+ SIZEOF_INT // ADDRL
			+ SIZEOF_INT // ADDRR
			+ SIZEOF_LONG // VALUE
			+ SIZEOF_EVENT_TYPE // TYPE
			;

	public RVEvent() {
	}

	/**
	 * @param gid
	 *            global identifier / primary key of the event
	 * @param tid
	 *            thread identifier primary key
	 * @param id
	 *            statement location identifier
	 * @param addrl
	 *            object identifier
	 * @param addrr
	 *            index (for arrays)
	 * @param value
	 *            value for events carrying a value
	 * @param type
	 *            type of event
	 */
	public RVEvent(long gid, long tid, int id, long addr, long value, RVEventType type) {
		this.GID = gid;
		this.TID = tid;
		this.ID = id;
		this.ADDR = addr;
		this.VALUE = value;
		this.TYPE = type;
	}
	
	public void updateRVEvent(long gid, long tid, int id, long addr, long value, RVEventType type) {
		this.GID = gid;
		this.TID = tid;
		this.ID = id;
		this.ADDR = addr;
		this.VALUE = value;
		this.TYPE = type;
	}

	public long getGID() {
		return GID;
	}

	public void setGID(long gid) {
		GID = gid;
	}

	public long getTID() {
		return TID;
	}

	public void setTID(long tid) {
		TID = tid;
	}

	public int getLocId() {
		return ID;
	}

	public void setLocId(int locId) {
		ID = locId;
	}

	public long getValue() {
		return VALUE;
	}

	public void setValue(long value) {
		VALUE = value;
	}

	public RVEventType getType() {
		return TYPE;
	}

	public void setType(RVEventType type) {
		TYPE = type;
	}

	public void setAddr(long addr) {
		ADDR = addr;
	}

	public long getAddr() {
		assert isReadOrWrite();
		return ADDR;
	}

	public int getObjectHashCode() {
		return (int) (getAddr() >> 32);
	}

	public int getFieldIdOrArrayIndex() {
		return (int) getAddr();
	}

	public long getSyncObject() {
		assert getType().isSyncType();
		return ADDR;
	}

	public long getLockId() {
		assert isLock() || isUnlock();
		return getSyncObject();
	}

	public boolean isRead() {
		return TYPE == RVEventType.READ;
	}

	public boolean isWrite() {
		return TYPE == RVEventType.WRITE;
	}

	public boolean isReadOrWrite() {
		return isRead() || isWrite();
	}

	public boolean isStart() {
		return TYPE == RVEventType.START;
	}

	public boolean isJoin() {
		return TYPE == RVEventType.JOIN;
	}

	/**
	 * Returns {@code true} if this event has type
	 * {@link RVEventType#WRITE_LOCK}, {@link RVEventType#READ_LOCK}, or
	 * {@link RVEventType#WAIT_ACQ}; otherwise, {@code false}.
	 */
	public boolean isLock() {
		return TYPE == RVEventType.READ_LOCK || TYPE == RVEventType.WRITE_LOCK || TYPE == RVEventType.WAIT_ACQ;
	}

	public boolean isReadLock() {
		return TYPE == RVEventType.READ_LOCK;
	}

	public boolean isWriteLock() {
		return TYPE == RVEventType.WRITE_LOCK;
	}

	public boolean isWaitAcq() {
		return TYPE == RVEventType.WAIT_ACQ;
	}

	/**
	 * Returns {@code true} if this event has type
	 * {@link RVEventType#WRITE_UNLOCK}, {@link RVEventType#READ_UNLOCK}, or
	 * {@link RVEventType#WAIT_REL}; otherwise, {@code false}.
	 */
	public boolean isUnlock() {
		return TYPE == RVEventType.READ_UNLOCK || TYPE == RVEventType.WRITE_UNLOCK || TYPE == RVEventType.WAIT_REL;
	}

	public boolean isReadUnlock() {
		return TYPE == RVEventType.READ_UNLOCK;
	}

	public boolean isWriteUnlock() {
		return TYPE == RVEventType.WRITE_UNLOCK;
	}

	public boolean isWaitRel() {
		return TYPE == RVEventType.WAIT_REL;
	}

	public boolean isSyncEvent() {
		return TYPE.isSyncType();
	}

	public boolean isMetaEvent() {
		return TYPE.isMetaType();
	}

	public boolean isCallStackEvent() {
		return TYPE == RVEventType.INVOKE_METHOD || TYPE == RVEventType.FINISH_METHOD;
	}

	public boolean isInvokeMethod() {
		return TYPE == RVEventType.INVOKE_METHOD;
	}

	public boolean isSimilarTo(RVEvent event) {
		return TYPE == event.TYPE && ID == event.ID && ADDR == event.ADDR && VALUE == event.VALUE;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof RVEvent) {
			return GID == ((RVEvent) object).GID;
		} else {
			return false;
		}
	}

	@Override
	public int compareTo(RVEvent e) {
		int result = Long.compare(getGID(), e.getGID());
		if (result == 0) {
			// YilongL: dirty hack to deal with the imprecise GID of call stack
			// event
			if (isCallStackEvent()) {
				return e.isCallStackEvent() ? 0 : -1;
			} else {
				return e.isCallStackEvent() ? 1 : 0;
			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		return (int) (GID % Integer.MAX_VALUE);
	}

	@Override
	public String toString() {
		if (isReadOrWrite()) {
			int addrl = (int) (ADDR >> 32);
			int addrr = getFieldIdOrArrayIndex();
			String addr = addrr < 0 ? Integer.toHexString(addrl) + "." + -addrr
					: Integer.toHexString(addrl) + "[" + addrr + "]";
			return String.format("(%s, E%s, T%s, L%s, %s, %s)", TYPE, GID, TID, ID, addr, Long.toHexString(VALUE));
		} else if (isSyncEvent()) {
			return String.format("(%s, E%s, T%s, L%s, %s)", TYPE, GID, TID, ID, Long.toHexString(getSyncObject()));
		} else if (isMetaEvent()) {
			return String.format("(%s, E%s, T%s, L%s)", TYPE, GID, TID, ID);
		} else {
			return "UNKNOWN EVENT";
		}
	}

	public RVEvent copy() {
		return new RVEvent(GID, TID, ID, ADDR, VALUE, TYPE);
	}

}
