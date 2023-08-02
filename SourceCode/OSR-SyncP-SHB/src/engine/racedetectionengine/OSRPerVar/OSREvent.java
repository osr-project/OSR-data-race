package engine.racedetectionengine.OSRPerVar;

import engine.racedetectionengine.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import util.Triplet;
import util.vectorclock.VectorClock;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OSREvent extends RaceDetectionEvent<OSRState> {

    public OSREvent() {
        super();
    }

    @Override
    public boolean Handle(OSRState state, int verbosity) {
        return this.HandleSub(state, verbosity);
    }

    public void onNewLockFound(OSRState state){
        if(!state.locks.contains(this.lock)){
            state.locks.add(this.getLock());
            state.numLocks++;
            int lockId = this.getLock().getId();

            state.acqList.put(lockId, (ArrayList<AcqEventInfo>[]) Array.newInstance(ArrayList.class, state.numThreads));
            state.acqListPtr.put(lockId, new int[state.numThreads]);
            state.openAcquiresExist.put(lockId, new boolean[state.numThreads]);
            state.lockToOpenAcquireNum.put(lockId, (short) 0);


            for(int i=0; i<state.numThreads; i++){
                state.acqList.get(lockId)[i] = new ArrayList<>();
                state.acqListPtr.get(lockId)[i] = 0;
                state.openAcquiresExist.get(lockId)[i] = false;
            }
        }
    }

    public void onNewVarFound(OSRState state){
        Variable v = this.getVariable();
        int varId = v.getId();
        if (!state.variables.contains(v)) {
            state.variables.add(v);
            state.recentWriteMap.put(varId, new VectorClock(state.numThreads));

            for(int thId=0; thId<state.numThreads; thId++){
                state.eventsVarsRead[thId].put(varId, new ArrayList<>());
                state.eventsVarsWrite[thId].put(varId, new ArrayList<>());
            }
        }
    }

    @Override
    public boolean HandleSubAcquire(OSRState state, int verbosity) {
        Lock l = this.getLock();
        int lockId = l.getId();

        Thread t = this.getThread();
        int threadId = t.getId();

        this.onNewLockFound(state);

        AcqEventInfo eventInfo = new AcqEventInfo();
        eventInfo.auxId = this.getAuxId();
        ArrayList<AcqEventInfo> curAcqList = state.acqList.get(lockId)[threadId];

        // check fork-join requests
        this.onExistForkEvent(state, threadId);

        this.updateTLClosure(state);


        int curThCnt = state.threadIdToCnt[threadId];
        curThCnt++;
        eventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadId] = curThCnt;

        state.threadToLockset[t.getId()].add(l.getId());
        curAcqList.add(eventInfo);
        state.inThreadIdToEventType[threadId].add(true);


        return false;
    }

    @Override
    public boolean HandleSubRelease(OSRState state, int verbosity) {
        Lock l = this.getLock();
        int lockId = l.getId();

        Thread t = this.getThread();
        int threadId = t.getId();

        this.onNewLockFound(state);
        RelEventInfo relEventInfo = new RelEventInfo();

        this.updateTLClosure(state);


        // update in-thread id and auxId
        int curThCnt = state.threadIdToCnt[threadId];
        curThCnt++;
        relEventInfo.inThreadId = curThCnt;
        state.threadIdToCnt[threadId] = curThCnt;
        relEventInfo.auxId = this.getAuxId();

        ArrayList<AcqEventInfo> curList = state.acqList.get(lockId)[threadId];
        AcqEventInfo matchAcq = curList.get(curList.size() - 1);
        matchAcq.relEventInfo = relEventInfo;
        relEventInfo.TLClosure = new VectorClock(state.clockThread[threadId]);

        state.threadToLockset[t.getId()].remove(l.getId());
        state.inThreadIdToEventType[threadId].add(false);
        return false;
    }

    @Override
    public boolean HandleSubRead(OSRState state, int verbosity) {
        Thread t = this.getThread();
        Variable v = this.getVariable();
        int threadIdx = this.getThread().getId();
        int varId = v.getId();
        this.onNewVarFound(state);

        if(!state.racyVars.contains(varId)){
            AccessEventInfo accessEventInfo = new AccessEventInfo();
            state.eventsVarsRead[this.getThread().getId()].get(varId).add(accessEventInfo);
            accessEventInfo.auxId = this.getAuxId();
            accessEventInfo.location = this.getLocId();

            // check fork-join requests
            this.onExistForkEvent(state, threadIdx);
            accessEventInfo.prevTLC = new VectorClock(state.clockThread[threadIdx]);

            this.updateTLClosure(state);


            int curThCnt = state.threadIdToCnt[threadIdx];
            curThCnt++;
            accessEventInfo.inThreadId = curThCnt;
            state.threadIdToCnt[threadIdx] = curThCnt;

            // add entry for last-write map
            VectorClock lastWriteTLC = state.recentWriteMap.get(v.getId());

            if (lastWriteTLC != null) {
                VectorClock tlc = state.clockThread[threadIdx];
                tlc.updateWithMax(tlc, lastWriteTLC);
            }

            // update recentReadMap
            state.inThreadIdToEventType[threadIdx].add(false);
            return checkRead(state, verbosity, accessEventInfo);
        } else {
            // check fork-join requests
            this.onExistForkEvent(state, threadIdx);

            this.updateTLClosure(state);


            state.threadIdToCnt[threadIdx]++;

            // add entry for last-write map
            VectorClock lastWriteTLC = state.recentWriteMap.get(v.getId());

            if (lastWriteTLC != null) {
                VectorClock tlc = state.clockThread[threadIdx];
                tlc.updateWithMax(tlc, lastWriteTLC);
            }

            // update recentReadMap
            state.inThreadIdToEventType[threadIdx].add(false);
            return false;
        }
    }



    @Override
    public boolean HandleSubWrite(OSRState state, int verbosity) {
        Variable v = this.getVariable();
        Thread t = this.getThread();
        int threadIdx = t.getId();
        int varId = v.getId();
        this.onNewVarFound(state);

        if(!state.racyVars.contains(varId)){
            AccessEventInfo accessEventInfo = new AccessEventInfo();
            state.eventsVarsWrite[threadIdx].get(varId).add(accessEventInfo);
            accessEventInfo.auxId = this.getAuxId();
            accessEventInfo.location = this.getLocId();

            // check fork-join requests
            this.onExistForkEvent(state, threadIdx);

            accessEventInfo.prevTLC = new VectorClock(state.clockThread[threadIdx]);

            this.updateTLClosure(state);


            int curThCnt = state.threadIdToCnt[threadIdx];
            curThCnt++;
            accessEventInfo.inThreadId = curThCnt;
            state.threadIdToCnt[threadIdx] = curThCnt;

            // update ConfClosure
            VectorClock recentWriteTLC = state.recentWriteMap.get(v.getId());
            VectorClock prevTLC = state.clockThread[threadIdx];


            // update recentWriteMap
            recentWriteTLC.copyFrom(prevTLC);

            state.inThreadIdToEventType[threadIdx].add(false);
            return checkWrite(state, verbosity, accessEventInfo);
        } else {
            // check fork-join requests
            this.onExistForkEvent(state, threadIdx);

            this.updateTLClosure(state);

            state.threadIdToCnt[threadIdx]++;

            // update ConfClosure
            VectorClock recentWriteTLC = state.recentWriteMap.get(v.getId());
            VectorClock prevTLC = state.clockThread[threadIdx];

            // update recentWriteMap
            recentWriteTLC.copyFrom(prevTLC);

            state.inThreadIdToEventType[threadIdx].add(false);
            return false;
        }
    }

    @Override
    public boolean HandleSubFork(OSRState state, int verbosity) {
        // add fork request
        int targetThId = this.getTarget().getId();

        // Initialize TLClosure of current event e to be the TlClosure(prev(e)) + e
        int threadIdx = this.getThread().getId();

        // check fork-join requests
        this.onExistForkEvent(state, threadIdx);

        this.updateTLClosure(state);

        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        state.threadIdToCnt[threadIdx] = curThCnt;

        // add fork event to the target thread
        VectorClock forkTLC = new VectorClock(state.clockThread[threadIdx]);
        state.threadToForkEvent[targetThId] = forkTLC;
        state.inThreadIdToEventType[threadIdx].add(false);
        return false;
    }

    @Override
    public boolean HandleSubJoin(OSRState state, int verbosity) {
        Thread target = this.target;
        int targetThreadId = target.getId();

        // Initialize TLClosure of current event e to be the TlClosure(prev(e)) + e
        int threadIdx = this.getThread().getId();

        // check fork-join requests
        this.onExistForkEvent(state, threadIdx);

        this.updateTLClosure(state);


        int curThCnt = state.threadIdToCnt[threadIdx];
        curThCnt++;
        state.threadIdToCnt[threadIdx] = curThCnt;

        // update ConfClosure and TLClosure
        VectorClock prevTLC = state.clockThread[threadIdx];
        VectorClock targetTLC = state.clockThread[targetThreadId];

        prevTLC.updateWithMax(prevTLC, targetTLC);

        state.inThreadIdToEventType[threadIdx].add(true);
        return false;
    }

    public void onExistForkEvent(OSRState state, int threadIdx){
        VectorClock forkEventTLC = state.threadToForkEvent[threadIdx];

        if (forkEventTLC != null) {
            state.clockThread[threadIdx].updateWithMax(state.clockThread[threadIdx], forkEventTLC);
            state.threadToForkEvent[threadIdx] = null;
        }
    }

    public void updateTLClosure(OSRState state){
        int threadIdx = this.getThread().getId();
        VectorClock prevTLC = state.clockThread[threadIdx];
        int original = prevTLC.getClockIndex(threadIdx);
        prevTLC.setClockIndex(threadIdx, original + 1);
    }


    public boolean checkWrite(OSRState state, int verbosity, AccessEventInfo e2) {
        return checkAccessTwoLists(state, verbosity, state.eventsVarsRead, state.eventsVarsWrite, e2);
    }

    public boolean checkAccess(OSRState state, int verbosity, HashMap<Integer, ArrayList<AccessEventInfo>>[] events, AccessEventInfo e2) {
        int varId = this.variable.getId();

        for (int thId=0; thId<state.numThreads; thId++) {
            if (thId == this.getThread().getId() || events[thId].get(varId).size() == 0) continue;

            ArrayList<AccessEventInfo> eventsInTh = events[thId].get(varId);

            int start = binarySearchByTLC(state, eventsInTh, e2, thId);

            // update with prev(e2) or fork event of e2
            state.sspEventSet.updateWithMax(state.sspEventSet, e2.prevTLC);

            for (int pos = start; pos < eventsInTh.size(); pos++) {
                AccessEventInfo e1 = eventsInTh.get(pos);

                if(checkEventInVectorstamp(state.sspEventSet, e1, thId)){
                    // e1 \in SSP set, should increase e1 till e1 \notin SSP
                    continue;
                }

                if (checkRace(e1, e2, thId, this.getThread().getId(), state)) {
//                    System.out.println("race between " + e1.auxId + ", " + e2.auxId);
//                    System.out.println("SSP set between " + e1.auxId + ", " + e2.auxId + " " + state.sspEventSet);
                    state.racyEvents.add(e2.auxId);
                    state.racyVars.add(varId);

                    for(int i=0;i<state.numThreads;i++){
                        state.eventsVarsRead[i].get(varId).clear();
                        state.eventsVarsWrite[i].get(varId).clear();

                        state.eventsVarsWrite[i].remove(varId);
                        state.eventsVarsWrite[i].remove(varId);
                    }

                    long curTime = System.currentTimeMillis();
                    long diff = curTime - state.startTime;
                    System.out.println(state.traceDir + "|" + diff + "|" + state.racyVars.size() + "|OSR-PerVar");

                    reInit(state);
                    return true;
                }
            }

            // re-init data structures that are modified during checkRace
            reInit(state);
        }
        return false;
    }

    public boolean checkAccessTwoLists(OSRState state, int verbosity, HashMap<Integer, ArrayList<AccessEventInfo>>[] readList,
                                       HashMap<Integer, ArrayList<AccessEventInfo>>[] writeList, AccessEventInfo e2) {
        int varId = this.variable.getId();

        for (int thId=0; thId<state.numThreads; thId++) {
            if (thId == this.getThread().getId()) continue;
            else if(readList[thId].get(varId).size() == 0 && writeList[thId].get(varId).size() == 0) continue;

            ArrayList<AccessEventInfo> readsInTh = readList[thId].get(varId);
            ArrayList<AccessEventInfo> writesInTh = writeList[thId].get(varId);

            int posRead = binarySearchByTLC(state, readsInTh, e2, thId);
            int posWrite = binarySearchByTLC(state, writesInTh, e2, thId);
            int readsLimit = readsInTh.size();
            int writesLimit = writesInTh.size();

            // update with prev(e2) or fork event of e2
            state.sspEventSet.updateWithMax(state.sspEventSet, e2.prevTLC);
            boolean isE1Read;
            AccessEventInfo e1;

            while(posRead < readsLimit || posWrite < writesLimit){
                if(posRead >= readsLimit) {
                    isE1Read = false;
                } else if(posWrite >= writesLimit) {
                    isE1Read = true;
                } else if(readsInTh.get(posRead).inThreadId < writesInTh.get(posWrite).inThreadId){
                    isE1Read = true;
                } else {
                    isE1Read = false;
                }

                e1 = isE1Read? readsInTh.get(posRead) : writesInTh.get(posWrite);

                if(isE1Read) posRead++;
                else posWrite++;

                if(checkEventInVectorstamp(state.sspEventSet, e1, thId)){
                    // e1 \in SSP set, should increase e1 till e1 \notin SSP
                    continue;
                }

                if (checkRace(e1, e2, thId, this.getThread().getId(), state)) {
//                    System.out.println("race between " + e1.auxId + ", " + e2.auxId);
//                    System.out.println("SSP set between " + e1.auxId + ", " + e2.auxId + " " + state.sspEventSet);
                    state.racyEvents.add(e2.auxId);
                    state.racyVars.add(varId);

                    for(int i=0;i<state.numThreads;i++){
                        state.eventsVarsRead[i].get(varId).clear();
                        state.eventsVarsWrite[i].get(varId).clear();

                        state.eventsVarsWrite[i].remove(varId);
                        state.eventsVarsWrite[i].remove(varId);
                    }

                    long curTime = System.currentTimeMillis();
                    long diff = curTime - state.startTime;
                    System.out.println(state.traceDir + "|" + diff + "|" + state.racyVars.size() + "|OSR-PerVar");

                    reInit(state);
                    return true;
                }
            }

            // re-init data structures that are modified during checkRace
            reInit(state);
        }
        return false;
    }

    public boolean checkRead(OSRState state, int verbosity, AccessEventInfo e2) {
        return checkAccess(state, verbosity, state.eventsVarsWrite, e2);
    }

    public boolean checkEventInVectorstamp(VectorClock v, EventInfo toCheck, int thIdx) {
        return v.getClock().get(thIdx) >= toCheck.inThreadId;
    }


    public boolean checkOpenAcquires(OSRState state) {
        for(Lock lock : state.locks){
            short openNum = state.lockToOpenAcquireNum.get(lock.getId());
            if(openNum > 1) return false;
        }

        return true;
    }

    private int binarySearchByTLC(OSRState state, ArrayList<AccessEventInfo> events1, AccessEventInfo e2, int e1ThId) {
        VectorClock prevE2TLC = e2.prevTLC;
        if (prevE2TLC == null) return 0;

        int left = 0, right = events1.size() - 1;
        int mid = 0;

        while (left < right) {
            mid = (left + right) / 2;
            AccessEventInfo temp = events1.get(mid);
            if (checkEventInVectorstamp(prevE2TLC, temp, e1ThId)) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return mid;
    }


    // given e1 and e2, check whether they are races
    public boolean checkRace(AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId, OSRState state) {
        // e1 -> curr event   e2 -> cmp event
        calcSSP(e1, e2, e1ThId, e2ThId, state); // state.sspEventSet has been updated

        // if e1 or e2 in SSP return false;
        if (checkEventInVectorstamp(state.sspEventSet, e1, e1ThId)) {
            return false;
        }

        // feasibility check
        if (!checkOpenAcquires(state)) {
            return false;
        }

        List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges = checkCondition1(state);

        boolean hasCycle = this.buildGraph(state, backwardEdges, e1, e2, e1ThId, e2ThId);

//        if(e1.auxId == 104L && e2.auxId == 436L){
//            System.out.println(hasCycle);
//            System.out.println(backwardEdges.size());
//            System.out.println(state.graph.vertexSet());
//            System.out.println(state.graph.edgeSet());
//        }


        return !hasCycle;
    }

    public boolean buildGraph(OSRState state, List<Triplet<Integer, AcqEventInfo, Integer>> backwardEdges,
                              AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId){
        // backwardEdge = <lockId, acqO, thId of acqO>
        List<Triplet<Integer, Integer, Long>> nodes = new ArrayList<>(); // <thId, inThreadId-1, auxId>
        SimpleDirectedGraph<Long, DefaultEdge> graph = new SimpleDirectedGraph<>(DefaultEdge.class);


        // add nodes and backward edges into the graph
        for(Triplet<Integer, AcqEventInfo, Integer> backwardEdge : backwardEdges){
            int lockId = backwardEdge.first;
            long acqOAuxId = backwardEdge.second.auxId;
            RelEventInfo lastRel = state.recentRelMapAlgo.get(lockId);
            long lastRelAuxId = state.recentRelMapAlgo.get(lockId).auxId;
            int lastRelThId = state.recentRelThreadId.get(lockId);

            graph.addVertex(acqOAuxId);
            nodes.add(new Triplet<>(backwardEdge.third, backwardEdge.second.inThreadId, acqOAuxId));
            graph.addVertex(lastRelAuxId);
            nodes.add(new Triplet<>(lastRelThId, lastRel.inThreadId, lastRelAuxId));
            graph.addEdge(lastRelAuxId, acqOAuxId);
        }

//        System.out.println(nodes);
        List<VectorClock> vectorClocks = state.partialOrder.queryForEventLists(nodes, state.sspEventSet);

        // add forward edges
        for (int i=0; i < nodes.size(); i++) {
            Triplet<Integer, Integer, Long> curNode = nodes.get(i);
            VectorClock vc = vectorClocks.get(i);

            for (int j=0; j<nodes.size(); j++){
                if(i == j) continue;

                Triplet<Integer, Integer, Long> testNode = nodes.get(j); // <thId, inThId, auxId>

                if(testNode.first.intValue() == curNode.first.intValue()) {
                    if (testNode.second > curNode.second) {
                        graph.addEdge(curNode.third, testNode.third);
                    }
                } else if (vc.getClockIndex(testNode.first) != -1 && testNode.second >= vc.getClockIndex(testNode.first)) {
//                    System.out.println("add edge : " + curNode.third + " -> " + testNode.third);
                    graph.addEdge(curNode.third, testNode.third);
                }
            }
        }

//        if(e1.auxId == 104L && e2.auxId == 436L){
//            // event = <threadId, inThreadId, auxId>
//            Triplet<Integer, Integer, Long> event = nodes.get(0);
//            VectorClock ret = new VectorClock(state.numThreads);
//            int threadId = event.first;
//            int inThreadId = event.second;
//            int limit = state.sspEventSet.getClockIndex(threadId);
//
//            // init with direct edges
//            for(int i=0; i<state.numThreads; i++){
//                if(i == threadId) {
//                    ret.setClockIndex(i, inThreadId);
//                    continue;
//                }
//
//                RangeMinima rangeMinima = state.partialOrder.successors.get(threadId).get(i);
//
//                int firstInToThread = rangeMinima.getMinWithRange(inThreadId, limit);
//                ret.setClockIndex(i, firstInToThread);
//            }
//            System.out.println("ffff:" + ret);
//        }

        state.graph = graph;

        CycleDetector<Long, DefaultEdge> cd = new CycleDetector<>(graph);

        return cd.detectCycles();
    }


    public void calcSSP(AccessEventInfo e1, AccessEventInfo e2, int e1ThId, int e2ThId, OSRState state) {
        // e1 -> cmp event   e2 -> curr event
        state.sspEventSet.updateWithMax(state.sspEventSet, e1.prevTLC);

        //e1 in TLC(prev(e2)), no race from e2 onwards
        if (checkEventInVectorstamp(state.sspEventSet, e1, e1ThId)) {
            return;
        }

        // repeat updating SSP
        boolean hasChanged = true;

        while (hasChanged) {
            hasChanged = false;
            // add acq events from acqList
            for (Lock l : state.locks) {
                int lockId = l.getId();
                boolean[] openAcq = state.openAcquiresExist.get(lockId);
                int[] curAcqListPtrs = state.acqListPtr.get(lockId);

                for (int thId=0; thId<state.numThreads; thId++) {
                    ArrayList<AcqEventInfo> acqs = state.acqList.get(lockId)[thId];

                    // get current ptr;  if pointing to the end, skip this loop
                    int ptr = curAcqListPtrs[thId];

                    if (ptr >= acqs.size()) continue;

                    RelEventInfo curRelEvent = null;
                    RelEventInfo toUpdateRelEvent = null;

                    boolean hasOpenAcq = openAcq[thId];

                    boolean initOpenAcq = openAcq[thId];

                    while (ptr < acqs.size()) {
                        AcqEventInfo curAcqEvent = acqs.get(ptr);

                        if (!checkEventInVectorstamp(state.sspEventSet, curAcqEvent, thId)) break; // curAcq not in S  =>  break

                        curRelEvent = curAcqEvent.relEventInfo;

                        if(curRelEvent == null || checkEventInVectorstamp(curRelEvent.TLClosure, e1, e1ThId)
                                || checkEventInVectorstamp(curRelEvent.TLClosure, e2, e2ThId)) {
                            // cannot be added, i.e. Open Acquire

                            hasOpenAcq = true;
                            break;
                        } else {
                            // curRelEvent can be added into SSP
                            hasOpenAcq = false;
                            toUpdateRelEvent = curRelEvent;
                        }

                        ptr++;
                    }

                    if (toUpdateRelEvent != null) {
                        if(!checkEventInVectorstamp(state.sspEventSet, toUpdateRelEvent, thId)){
                            state.sspEventSet.updateWithMax(state.sspEventSet, toUpdateRelEvent.TLClosure);
                        }

                        RelEventInfo curLastRel = state.recentRelMapAlgo.get(lockId);
                        if (curLastRel == null || curLastRel.auxId < toUpdateRelEvent.auxId) {
                            state.recentRelMapAlgo.put(lockId, toUpdateRelEvent);
                            state.recentRelThreadId.put(lockId, thId);
                        }

                        hasChanged = true;
                    }
                    curAcqListPtrs[thId] = ptr;
                    openAcq[thId] = hasOpenAcq;

                    if(initOpenAcq != openAcq[thId]){
                        short prevNum = state.lockToOpenAcquireNum.get(lockId);
                        if(initOpenAcq){
                            state.lockToOpenAcquireNum.put(lockId, (short) (prevNum - 1));
                        } else {
                            state.lockToOpenAcquireNum.put(lockId, (short) (prevNum + 1));
                        }
                    }
                }
            }
        }

//        System.out.println("SSP[" + e1.getAuxId() + ", " + e2.getAuxId()
//                + "] : " + ret);
    }

    public void reInit(OSRState state) {
        state.recentRelMapAlgo.clear();
        state.recentRelThreadId.clear();

        for (Lock l : state.locks) {
            int l_id = l.getId();

            state.lockToOpenAcquireNum.put(l_id, (short) 0);

            int[] curAcqListPtrs = state.acqListPtr.get(l_id);
            boolean[] badAcqListPtrs = state.openAcquiresExist.get(l_id);

            for (int thId=0; thId<state.numThreads; thId++) {
                curAcqListPtrs[thId] = 0;
                badAcqListPtrs[thId] = false;
            }
        }

        state.sspEventSet.setToZero();
    }

    private List<Triplet<Integer, AcqEventInfo, Integer>> checkCondition1(OSRState state) {
        // return list(<lockId, acqO, thId of acqO>)

        List<Triplet<Integer, AcqEventInfo, Integer>> ret = new ArrayList<>();

        for (Lock l : state.locks) {
            int lockId = l.getId();
            // recent rel(l) = null => no backward edge
            RelEventInfo recentRelEvent = state.recentRelMapAlgo.get(lockId);
            if (recentRelEvent == null) continue;

            boolean[] hasOpenAcq = state.openAcquiresExist.get(lockId);
            int[] acqPtrList = state.acqListPtr.get(lockId);
            long recentRelId = recentRelEvent.auxId;

            for (int thId=0; thId<state.numThreads; thId++) {
                int acqPos = acqPtrList[thId];

                if (hasOpenAcq[thId]) {
                    AcqEventInfo acqO_Event = state.acqList.get(lockId)[thId].get(acqPos);
                    long curAcqOId = acqO_Event.auxId;

                    if (recentRelId > curAcqOId) {
                        Triplet<Integer, AcqEventInfo, Integer> curPair = new Triplet<>(lockId, acqO_Event, thId);
                        ret.add(curPair);
                    }
                    break;
                }
                // no need to check for (second - first > 1), it's done in checkBadAcquires()
            }
        }
        return ret;
    }


    @Override
    public void printRaceInfoLockType(OSRState state, int verbosity) {

    }

    @Override
    public void printRaceInfoAccessType(OSRState state, int verbosity) {

    }

    @Override
    public void printRaceInfoExtremeType(OSRState state, int verbosity) {

    }

    @Override
    public void printRaceInfoTransactionType(OSRState state, int verbosity) {

    }

    @Override
    public boolean HandleSubBegin(OSRState state, int verbosity) {
        return false;
    }

    @Override
    public boolean HandleSubEnd(OSRState state, int verbosity) {
        return false;
    }
}
