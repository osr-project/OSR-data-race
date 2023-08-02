package engine.annotation.written_dangling_vars;

import java.util.HashSet;
import java.util.Set;

import engine.racedetection.RaceDetectionEvent;

public class WrittenDanglingVarsEvent extends RaceDetectionEvent<WrittenDanglingVarsState> {

	@Override
	public boolean Handle(WrittenDanglingVarsState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(WrittenDanglingVarsState state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void printRaceInfoAccessType(WrittenDanglingVarsState state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void printRaceInfoExtremeType(WrittenDanglingVarsState state) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean HandleSubAcquire(WrittenDanglingVarsState state) {
		String lname = this.getLock().getName();
		state.checkAndAddLock(lname);
		state.lockToListOfWrittenVars.get(lname).addLast(new HashSet<String>());
		state.lockToListOfDanglingVars.get(lname).addLast(new HashSet<String>());
		state.updateLocksHeldAcquire(this.getThread(), lname);
		return false;
	}

	@Override
	public boolean HandleSubRelease(WrittenDanglingVarsState state) {
		String lname = this.getLock().getName();
		state.checkAndAddLock(lname);

		boolean outermost = state.updateLocksHeldRelease(this.getThread(), lname);

		if (outermost) {
			HashSet<String> written_vars = state.lockToListOfWrittenVars.get(lname).getLast();
			for (String vname : written_vars) {
				if (state.var_to_boundary.get(vname) > this.getAuxId()) {
					HashSet<String> dvars = state.lockToListOfDanglingVars.get(lname).getLast();
					dvars.add(vname);
				}
			}
		}

		return false;
	}

	@Override
	public boolean HandleSubRead(WrittenDanglingVarsState state) {
		String vname = this.getVariable().getName();
		state.checkAndAddVariable(vname);

		if (state.locksHeld.containsKey(this.getThread())) {
			Set<String> locksHeld = state.locksHeld.get(this.getThread()).keySet();
			for (String lname : locksHeld) {
				HashSet<String> wvars = state.lockToListOfWrittenVars.get(lname).getLast();
				if (!wvars.contains(vname)) {
					HashSet<String> dvars = state.lockToListOfDanglingVars.get(lname).getLast();
					dvars.add(vname);
				}
			}
		}

		return false;
	}

	@Override
	public boolean HandleSubWrite(WrittenDanglingVarsState state) {
		String vname = this.getVariable().getName();
		state.checkAndAddVariable(vname);
		state.newWrite(vname);

		if (state.locksHeld.containsKey(this.getThread())) {
			Set<String> locksHeld = state.locksHeld.get(this.getThread()).keySet();
			for (String lname : locksHeld) {
				HashSet<String> wvars = state.lockToListOfWrittenVars.get(lname).getLast();
				wvars.add(vname);
				if (!state.var_to_locality.get(vname)) {
					HashSet<String> dvars = state.lockToListOfDanglingVars.get(lname).getLast();
					dvars.add(vname);
				}
			}
		}

		return false;
	}

	@Override
	public boolean HandleSubFork(WrittenDanglingVarsState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean HandleSubJoin(WrittenDanglingVarsState state) {
		// TODO Auto-generated method stub
		return false;
	}

}
