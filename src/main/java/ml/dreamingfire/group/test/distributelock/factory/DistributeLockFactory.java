package ml.dreamingfire.group.test.distributelock.factory;

import ml.dreamingfire.group.test.distributelock.api.DistributeLockApi;
import ml.dreamingfire.group.test.distributelock.concretelock.RedisDistributeLock;
import ml.dreamingfire.group.test.distributelock.concretelock.ZookeeperDistributeLock;
import ml.dreamingfire.group.test.distributelock.util.PropertyFileUtil;

import java.util.Objects;

/**
 * factory pattern
 * */
public class DistributeLockFactory {
    private final static String ZOOKEEPER_NAME_CODE = "zookeeper";

    /**
     * Create a distribute lock instance by name code
     * @param nameCode enum("redis", "zookeeper")
     * */
    public static DistributeLockApi create(String nameCode) {
        Objects.requireNonNull(nameCode, "param nameCode cannot be null, you can use \"\" for default distribute lock" +
                " using redis");
        if (nameCode.equals(ZOOKEEPER_NAME_CODE)) {
            return new ZookeeperDistributeLock();
        }
        return RedisDistributeLock.getInstance(PropertyFileUtil.get("SERVICE_ID"));
    }
}
