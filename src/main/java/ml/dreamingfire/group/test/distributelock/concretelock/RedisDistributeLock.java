package ml.dreamingfire.group.test.distributelock.concretelock;

import ml.dreamingfire.group.test.distributelock.api.DistributeLockApi;
import ml.dreamingfire.group.test.distributelock.util.PropertyFileUtil;
import redis.clients.jedis.Jedis;

public class RedisDistributeLock implements DistributeLockApi {
    private static RedisDistributeLock distributeLock = null;
    // Jedis is non-thread-safe class while cannot use singleton in different threads
    private static ThreadLocal<Jedis> redisPool;
    // use service id as content so A lock won't be unlocked by B
    // it is always used when a thread blocked during unlock and try unlock after expired
    private static String SERVICE_ID;
    private final static String LOCK_KEY = "redis_distribute_lock";
    private final static int MAX_TIME_SECOND = 600;
    private final static int REDIS_TIME_OUT = 20000000;

    private RedisDistributeLock(String serviceId) {
        redisPool = new ThreadLocal<>();
        SERVICE_ID = serviceId;
    }

    public static RedisDistributeLock getInstance(String serviceId) {
        if (distributeLock == null) {
            synchronized(RedisDistributeLock.class) {
                if (distributeLock == null) {
                    distributeLock = new RedisDistributeLock(serviceId);
                }
            }
        }
        return distributeLock;
    }

    @Override
    public void lock() throws Throwable {
        if (redisPool.get() == null) {
            String redisUrl = PropertyFileUtil.get("REDIS_URL");
            String[] redisConfig = redisUrl.substring(8).split("\\s*:\\s*");
            redisPool.set(new Jedis(redisConfig[0], Integer.parseInt(redisConfig[1]), REDIS_TIME_OUT));
        }
        while (!tryLock(SERVICE_ID + "_" + Thread.currentThread().getName())) {}
        redisPool.get().expire(LOCK_KEY, MAX_TIME_SECOND);
    }

    @Override
    public void unlock() throws Throwable {
        if (redisPool.get() == null) {
            throw new Exception("please lock first");
        }
        if (hasLockWithContent(SERVICE_ID + "_" + Thread.currentThread().getName())) {
            redisPool.get().del(LOCK_KEY);
        }
    }

    @Override
    public void shutdown() {
        redisPool = null;
    }

    private boolean tryLock(String content) {
        // redis setNX won't change the content when key exists
        redisPool.get().setnx(LOCK_KEY, content);
        String value = redisPool.get().get(LOCK_KEY);
        return value != null && value.equals(content);
    }

    private boolean hasLockWithContent(String content) {
        String value = redisPool.get().get(LOCK_KEY);
        return value != null && !value.equals("") && value.equals(content);
    }
}
