package engine.racedetection.zeroreversal.v1;

import util.vectorclock.FlexibleVC;

//Alias for Vector clocks indexed using Locks.
public class LockVC extends FlexibleVC{
	LockVC(int d) {
		super(d);
	}
	
	LockVC(LockVC vc){
		super(vc);
	}
};
