package engine.annotation.write_block_boundary;

import java.util.LinkedList;

import engine.racedetection.RaceDetectionEvent;

public class WriteBlockBoundaryEvent extends RaceDetectionEvent<WriteBlockBoundaryState> {

	@Override
	public boolean Handle(WriteBlockBoundaryState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(WriteBlockBoundaryState state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void printRaceInfoAccessType(WriteBlockBoundaryState state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void printRaceInfoExtremeType(WriteBlockBoundaryState state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean HandleSubAcquire(WriteBlockBoundaryState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubRelease(WriteBlockBoundaryState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubRead(WriteBlockBoundaryState state) {
		String vname = this.getVariable().getName();
		state.checkAndAddVariable(vname);
		
		LinkedList<Long> boundary_list_v = state.variableToListOfBoundaryIndices.get(vname);
		LinkedList<Boolean> locality_list_v = state.variableToListOfThreadLocality.get(vname);
		
		if(boundary_list_v.isEmpty()) {
			boundary_list_v.addLast(-1L);			
			locality_list_v.addLast(true);
		}
		else {
			boundary_list_v.removeLast();
			boundary_list_v.addLast(this.getAuxId());
			
			boolean is_writer_thread = state.lastWritingThread.get(vname).equals(this.getThread());
			boolean is_local = is_writer_thread && locality_list_v.getLast();
			locality_list_v.removeLast();
			locality_list_v.addLast(is_local);
		}
		
		return false;
	}

	@Override
	public boolean HandleSubWrite(WriteBlockBoundaryState state) {
		String vname = this.getVariable().getName();
		state.checkAndAddVariable(vname);
		
		LinkedList<Long> boundary_list_v = state.variableToListOfBoundaryIndices.get(vname);
		boundary_list_v.addLast(this.getAuxId());
		
		LinkedList<Boolean> locality_list_v = state.variableToListOfThreadLocality.get(vname);
		locality_list_v.addLast(true);
		
		state.lastWritingThread.put(vname, this.getThread());
		
		return false;
	}

	@Override
	public boolean HandleSubFork(WriteBlockBoundaryState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubJoin(WriteBlockBoundaryState state) {
		// TODO Auto-generated method stub
		return false;
	}

}
