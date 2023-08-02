package debug;

import cache.Cache;

/**
 * Created by matt on 4/18/18.
 */
public class CacheStatistics {


    public static long getLvtCacheCalls() {
        return Cache.getCacheCalls();
    }

    public static long getLvtCacheLoads() {
        return Cache.getCacheLoads();
    }

    public static void print() {
        System.out.println("---------------Cache Statistics---------------");
        System.out.println("LVT Cache: " + getLvtCacheCalls() +"/" + getLvtCacheLoads() + "(calls/loads)");
    }

}
