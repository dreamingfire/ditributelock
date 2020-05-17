package ml.dreamingfire.group.test.distributelock.api;

/**
 * <i>Api for java distribute lock demo</i>
 * @see ml.dreamingfire.group.test.distributelock.concretelock.RedisDistributeLock
 * @see ml.dreamingfire.group.test.distributelock.concretelock.ZookeeperDistributeLock
 * */
public interface DistributeLockApi {

    /**
     * get distribute lock for concurrency
     * @throws Throwable Exceptions or Errors when lock
     * */
    void lock() throws Throwable;

    /**
     * release the distribute lock so that others can get the lock
     * @throws Throwable Exceptions or Errors when unlock
     * */
    void unlock() throws Throwable;

    /**
     * release the instance resource
     * */
    void shutdown();
}
