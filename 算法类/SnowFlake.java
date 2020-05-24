package com.sherbet;

public class SnowFlake {
    private static final long zoneBits = 5;
    private static final long machineBits = 5;
    private static final long sequenceBits = 12;

    private static final long maxZoneID = (1 << zoneBits) - 1;
    private static final long maxMachineID = (1 << machineBits) - 1;
    private static final long maxSequenceID = (1 << sequenceBits) - 1;

    private long zoneID = 0;
    private long machineID = 0;
    private long sequenceID = 0;

    private long lastTimeStamp = -1L;

    public SnowFlake(long zoneID, long machineID) {
        if (zoneID < 0 || zoneID > maxZoneID)
            throw new IllegalArgumentException("zoneID can't be greater than " + maxZoneID + " or less than 0");

        if (machineID < 0 || machineID > maxMachineID)
            throw new IllegalArgumentException("machineID can't be greater than " + maxMachineID + " or less than 0");

        this.zoneID = zoneID;
        this.machineID = machineID;
    }


    /**
     * @return 获取当前时间戳
     */
    private long getCurrentTimeStamp() {
        return System.currentTimeMillis();
    }

    /**
     * @return 在当前线程在单位时间内递增ID达到最大时，调用该阻塞方法获取新的时间戳
     */
    private long blockUntilNextMS() {
        long tempTimeStamp = getCurrentTimeStamp();
        while (tempTimeStamp <= lastTimeStamp)
            tempTimeStamp = getCurrentTimeStamp();
        return tempTimeStamp;
    }

    /**
     * @return 生成分布式有序的全局唯一ID
     * @throws Exception 暂不考虑
     */
    public synchronized long getID() throws Exception {

        long currentTimeStamp = getCurrentTimeStamp();

        //最新时间戳小于最后时间戳的情况，诱因不明
        if (currentTimeStamp < lastTimeStamp)
            throw new RuntimeException("unknow exception");

        //在更新时间后，如果最新时间戳和最后时间戳不一致，序列ID归零
        if (currentTimeStamp != lastTimeStamp)
            sequenceID = 0;

        //在更新时间后，如果在同一单位时间内，则递增序列号
        else {
            sequenceID = ++sequenceID & maxSequenceID;
            //如果递增序列号后归零，则表示2^12个ID已用完，进入阻塞模块等待更新时间
            if (sequenceID == 0)
                currentTimeStamp = blockUntilNextMS();
        }
        //更新最后时间戳
        lastTimeStamp = currentTimeStamp;
        //拼接63位的ID
        return (lastTimeStamp << (zoneBits + machineBits + sequenceBits))
                | (zoneID << (machineBits + sequenceBits))
                | (machineID << sequenceBits)
                | (sequenceID);
    }
}
