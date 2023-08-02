package cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

/**
 * Created by matt on 4/18/18.
 */
public class Cache {

    private static final int DEFAULT_CACHE_SIZE = 20000;

    private static LoadingCache<Object, Boolean> CACHE;

    private static long cacheLoads;
    private static long cacheCalls;
    private static int cacheSize;


    static {

        cacheLoads = 0;
        cacheCalls = 0;
        cacheSize = DEFAULT_CACHE_SIZE;
    }

    public static void init(HashMap<String, HashMap<String, HashSet<String>>> existsLockReadVariableThreads,
                            HashMap<String, HashMap<String, HashSet<String>>> existsLockWriteVariableThreads) {

        CACHE = CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .build(
                        new CacheLoader<Object, Boolean>() {
                            public Boolean load(Object obj) throws Exception {

                                return false;
                            }
                        });
    }

    public static Boolean lookup(Object obj) throws ExecutionException {
        cacheCalls++;
        return CACHE.get(obj);
    }


    public static long getCacheLoads() {
        return cacheLoads;
    }

    public static long getCacheCalls() {
        return cacheCalls;
    }

    public static int getCacheSize() {
        return cacheSize;
    }
}
