package com.vigza.markweave.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;

public class IdGenerator {
    private static Snowflake snowflake;
    
    static {
        long workerId = 0; 
        long datacenterId = 0;
        snowflake = IdUtil.getSnowflake(workerId, datacenterId);
    }
    
    public static long nextId() {
        return snowflake.nextId();
    }
    
    public static String nextIdStr() {
        return snowflake.nextIdStr();
    }
}
