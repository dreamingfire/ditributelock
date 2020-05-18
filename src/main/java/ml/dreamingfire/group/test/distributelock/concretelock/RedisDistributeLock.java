package ml.dreamingfire.group.test.distributelock.concretelock;

import ml.dreamingfire.group.test.distributelock.api.DistributeLockApi;
import ml.dreamingfire.group.test.distributelock.util.PropertyFileUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

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
    private final static String REDIS_BLOCK_QUEUE_NAME = "redis_distribute_lock_bq";

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
        while (!tryLock(SERVICE_ID + "_" + Thread.currentThread().getName())) {
            // use loop will cause a high CPU load, try to use redis block queue to reduce it
            redisPool.get().blpop(MAX_TIME_SECOND, REDIS_BLOCK_QUEUE_NAME);
        }
        redisPool.get().expire(LOCK_KEY, MAX_TIME_SECOND);
    }

    @Override
    public void unlock() throws Throwable {
        if (redisPool.get() == null) {
            throw new Exception("please lock first");
        }
        // use transaction
        String content = SERVICE_ID + "_" + Thread.currentThread().getName();
        if (hasLockWithContent(content)) {
            try(Transaction transaction = redisPool.get().multi()) {
                transaction.del(LOCK_KEY);
                transaction.lpush(REDIS_BLOCK_QUEUE_NAME, content);
                transaction.exec();
            }
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
        redisPool.get().watch(LOCK_KEY);
        String value = redisPool.get().get(LOCK_KEY);
        return value != null && !value.equals("") && value.equals(content);
    }

}
