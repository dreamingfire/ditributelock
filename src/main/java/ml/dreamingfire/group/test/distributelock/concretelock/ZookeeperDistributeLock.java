package ml.dreamingfire.group.test.distributelock.concretelock;

import ml.dreamingfire.group.test.distributelock.api.DistributeLockApi;
import ml.dreamingfire.group.test.distributelock.util.PropertyFileUtil;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class ZookeeperDistributeLock implements DistributeLockApi {
    private final static int ZOOKEEPER_TIME_OUT = 300 * 1000;
    private final static String LOCK_PATH = "/zk_lock";
    private final static String SEPARATOR = "/";

    private ZooKeeper zkCli;
    private static ZookeeperDistributeLock distributeLock;
    private ThreadLocal<String> currentPath;
    private ThreadLocal<String> beforePath;
    private Map<String, CountDownLatch> latchForLock;

    private ZookeeperDistributeLock() {
        this.currentPath = new ThreadLocal<>();
        this.beforePath = new ThreadLocal<>();
        this.latchForLock = new ConcurrentHashMap<>();
        StringBuilder host = new StringBuilder("");
        String zkUrls = PropertyFileUtil.get("ZOOKEEPER_URL");
        String[] zkUrlArr = zkUrls.split("\\s*#\\s*");
        for (String zkUrl: zkUrlArr) {
            host.append(zkUrl.substring(5)).append(",");
        }
        host.deleteCharAt(host.length() - 1);
        CountDownLatch latch = new CountDownLatch(1);
        try {
            this.zkCli = new ZooKeeper(host.toString(), ZOOKEEPER_TIME_OUT, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    latch.countDown();
                }
                // when node is deleted, the next thread get the lock
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                    this.latchForLock.get(getChildPath(watchedEvent.getPath())).countDown();
                }
            });
            latch.await();
            // init the parent node
            if (this.zkCli.exists(LOCK_PATH, false) == null) {
                this.zkCli.create(LOCK_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (IOException | InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public static ZookeeperDistributeLock getInstance() {
        if (distributeLock == null) {
            synchronized (ZookeeperDistributeLock.class) {
                if (distributeLock == null) {
                    distributeLock = new ZookeeperDistributeLock();
                }
            }
        }
        return distributeLock;
    }

    @Override
    public void lock() throws Throwable {
        CountDownLatch latch = new CountDownLatch(1);
        String actualPath = this.zkCli.create(LOCK_PATH + SEPARATOR + "zk_child_path", new byte[0],
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        String actualChildPath = getChildPath(actualPath);
        this.currentPath.set(actualChildPath);
        if (!tryLock(actualChildPath)) {
            this.latchForLock.put(this.beforePath.get(), latch);
            latch.await();
        }
    }

    @Override
    public void unlock() throws Throwable {
        this.zkCli.delete(LOCK_PATH + SEPARATOR + this.currentPath.get(), -1);
    }

    @Override
    public void shutdown() {
        System.out.println("zookeeper connection shutting down ...");
        if (this.zkCli != null) {
            try {
                this.zkCli.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean tryLock(String lockPath) throws KeeperException, InterruptedException {
        List<String> nodes = this.zkCli.getChildren(LOCK_PATH, false);
        Collections.sort(nodes);
        int index = nodes.indexOf(lockPath);
        if (index <= 0) {
            return true;
        } else {
            // if there are smaller number nodes, fail to get lock and block
            if (this.zkCli.exists(LOCK_PATH + SEPARATOR + nodes.get(index - 1), true) == null) {
                return true;
            } else {
                this.beforePath.set(nodes.get(index - 1));
                return false;
            }
        }
    }

    private String getChildPath(String realPath) {
        String[] paths = realPath.split(SEPARATOR);
        return paths[paths.length - 1];
    }
}
