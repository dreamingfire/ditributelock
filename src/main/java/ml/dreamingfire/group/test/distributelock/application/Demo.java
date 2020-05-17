package ml.dreamingfire.group.test.distributelock.application;

import ml.dreamingfire.group.test.distributelock.api.DistributeLockApi;
import ml.dreamingfire.group.test.distributelock.factory.DistributeLockFactory;
import ml.dreamingfire.group.test.distributelock.util.Counter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * a simple demo about the usage of distribute lock
 * */
public class Demo {
    public static void main(String[] args) throws ClassNotFoundException {
        Class.forName("ml.dreamingfire.group.test.distributelock.util.PropertyFileUtil");
//        DistributeLockApi distributeLock = DistributeLockFactory.create("redis");
        DistributeLockApi distributeLock = DistributeLockFactory.create("zookeeper");
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Counter counter = new Counter();
        for (int i=0; i < 10; i++) {
            executor.execute(()->{
                String threadName = counter.getName();
                int sleepTime = counter.getSleepTime();
                try {
                    distributeLock.lock();
                    System.out.println("Thread " + threadName + " get lock");
                    TimeUnit.SECONDS.sleep(sleepTime);
                    System.out.println("Thread " + threadName + " release lock");
                    distributeLock.unlock();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            });
        }
        executor.shutdown();
        Runtime.getRuntime().addShutdownHook(new Thread(distributeLock::shutdown));
    }
}
