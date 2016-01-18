package com.hmsphr.jdj.Class;

import android.os.SystemClock;

import com.hmsphr.jdj.Class.Utils.SortedArrayList;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class TimePool {

    public class TimeSample {
        private Long LT1; // Local time on send
        private Long ST;  // Server time
        private Long LT2; // Local time on recv

        private Long RTD; // RoundTrip Duration
        private Long TS;  // TimeShift to apply


        public TimeSample() {
        }

        public Long initLT1() {
            LT1 = SystemClock.elapsedRealtime();
            return LT1;
        }

        public Long getTS() {
            return TS;
        }

        public Long getRTD() {
            return RTD;
        }

        public void setST(Long st) {
            ST = st;
            LT2 = SystemClock.elapsedRealtime();
            RTD = LT2 - LT1;
            TS = ST - LT1 - RTD/2;
        }
    }

    public int MIN_PROCESS_SIZE = 1;

    protected ArrayList<TimeSample> samplesPool = new ArrayList<>();
    protected final ReadWriteLock locker = new ReentrantReadWriteLock();
    protected final Lock readLock = locker.readLock();
    protected final Lock writeLock = locker.writeLock();

    protected long AVTS = 0;
    protected long ETTS = 0;
    protected long AVTS_L1 = 0;
    protected long AVTS_L2 = 0;
    protected long AVTS_L3 = 0;

    protected boolean processed = false;

    public TimePool() {
        clear();
    }

    public TimeSample newSample() {
        return new TimeSample();
    }

    public long getTimeMilliSynced() {
        if (getTimeShift() == 0) return System.currentTimeMillis();
        return SystemClock.elapsedRealtime() + getTimeShift();
    }

    public long getTimeShift() {
        if (!processed) process();
        long val = 0;
        readLock.lock();
        try {
            if (AVTS_L1 > 0) val = AVTS_L1;
            else if (AVTS_L2 > 0) val = AVTS_L2;
            else if (AVTS_L3 > 0) val = AVTS_L3;
            else val = AVTS;
        }
        finally {
            readLock.unlock();
        }
        return val;
    }

    public void clear() {
        writeLock.lock();
        try {
            samplesPool.clear();
            AVTS = 0;
            ETTS = 0;
            AVTS_L1 = 0;
            AVTS_L2 = 0;
            AVTS_L3 = 0;
            processed = false;
        }
        finally {
            writeLock.unlock();
        }
    }

    public void add(TimeSample sample) {
        boolean reProcess = false;
        writeLock.lock();
        try {
            samplesPool.add(sample);
            if (samplesPool.size() >= MIN_PROCESS_SIZE) reProcess = true;
        }
        finally {
            writeLock.unlock();
        }
        if (reProcess) process();
    }

    private void process() {
        writeLock.lock();
        try {
            if (samplesPool.size() < 2) return;

            // Determine RoundTrip delays limit to eliminate the longest ones (30%)
            // A long RTD means less accuracy..
            SortedArrayList<Long> RTDs = new SortedArrayList<>();
            for(TimeSample sample : samplesPool) RTDs.insertSorted(sample.getRTD());
            Long ignoreAbove = RTDs.get( Math.min((int) (RTDs.size() * 0.7), (RTDs.size()-1)) );

            // Average TimeShift
            AVTS = 0;
            int weightSum = 0;
            for(TimeSample sample : samplesPool)
                if (sample.getRTD() <= ignoreAbove) {
                    AVTS += sample.getTS()*(ignoreAbove - sample.getRTD() + 1);
                    weightSum+=(ignoreAbove - sample.getRTD() + 1);
                }
            if (weightSum > 0) AVTS = AVTS/weightSum;
            else AVTS = 0;

            // Log.v("jdj-TimeSync", "Valid samples: "+validSamples);

            // Ecart type TimeShift
            ETTS = 0;
            weightSum = 0;
            int weightCount = 0;
            for(TimeSample sample : samplesPool)
                if (sample.getRTD() <= ignoreAbove) {
                    ETTS += (sample.getTS()-AVTS)*(sample.getTS()-AVTS)*(ignoreAbove - sample.getRTD() + 1);
                    weightSum+=(ignoreAbove - sample.getRTD() + 1);
                    weightCount++;
                }
            if (weightCount > 0 && weightSum > 0) ETTS = Math.max(1, (long) Math.sqrt(ETTS / (weightSum - weightSum*1.0/weightCount)) );
            else ETTS = 1;

            // Average for Level 1 confidence
            AVTS_L1 = 0;
            weightSum = 0;
            for(TimeSample sample : samplesPool)
                if (sample.getRTD() <= ignoreAbove) {
                    if (sample.getTS() <= (AVTS+ETTS) && sample.getTS() >= (AVTS-ETTS)) {
                        AVTS_L1 += sample.getTS()*(ignoreAbove - sample.getRTD() + 1);
                        weightSum+=(ignoreAbove - sample.getRTD() + 1);
                    }
                }
            if (weightSum > 0) AVTS_L1 = AVTS_L1/weightSum;
            else AVTS_L1 = 0;

            // Average for Level 2 confidence
            AVTS_L2 = 0;
            weightSum = 0;
            for(TimeSample sample : samplesPool)
                if (sample.getRTD() <= ignoreAbove) {
                    if (sample.getTS() <= (AVTS+2*ETTS) && sample.getTS() >= (AVTS-2*ETTS)) {
                        AVTS_L2 += sample.getTS()*(ignoreAbove - sample.getRTD() + 1);
                        weightSum+=(ignoreAbove - sample.getRTD() + 1);
                    }
                }
            if (weightSum > 0) AVTS_L2 = AVTS_L2/weightSum;
            else AVTS_L2 = 0;

            // Average for Level 1 confidence
            AVTS_L3 = 0;
            weightSum = 0;
            for(TimeSample sample : samplesPool)
                if (sample.getRTD() <= ignoreAbove) {
                    if (sample.getTS() <= (AVTS+3*ETTS) && sample.getTS() >= (AVTS-3*ETTS)) {
                        AVTS_L3 += sample.getTS()*(ignoreAbove - sample.getRTD() + 1);
                        weightSum+=(ignoreAbove - sample.getRTD() + 1);
                    }
                }
            if (weightSum > 0) AVTS_L3 = AVTS_L3/weightSum;
            else AVTS_L3 = 0;

            processed = true;
        }
        finally {
            writeLock.unlock();
        }
    }

    // DEBUG
    public long getAccuracy() {
        if (!processed) process();
        long val = 0;
        readLock.lock();
        try {
            if (AVTS > 0 && AVTS_L1 > 0) val = Math.max(1, Math.abs(AVTS_L1 - AVTS));
            else val = 0;
        }
        finally {
            readLock.unlock();
        }
        return val;
    }

    // DEBUG
    public long getAverage(int level) {
        if (!processed) process();
        long val = 0;
        readLock.lock();
        try {
            if (level == 1) val =  AVTS_L1;
            else if (level == 2) val =  AVTS_L2;
            else if (level == 3) val =  AVTS_L3;
            else val = AVTS;
        }
        finally {
            readLock.unlock();
        }
        return val;
    }

    // DEBUG
    public long getDispersion() {
        if (!processed) process();
        long val = 0;
        readLock.lock();
        try {
            val = ETTS;
        }
        finally {
            readLock.unlock();
        }
        return val;
    }
}
