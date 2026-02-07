package com.vigza.markweave.common.util;

import java.sql.Time;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

public class IdGenerator {
    private static Snowflake snowflake;
    
    static {
        long workerId = System.currentTimeMillis() % 31; 
        long datacenterId = System.currentTimeMillis() % 31;
        snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }
    
    public static long nextId() {
        return snowflake.nextId();
    }
    
    public static String nextIdStr() {
        return snowflake.nextIdStr();
    }
}
