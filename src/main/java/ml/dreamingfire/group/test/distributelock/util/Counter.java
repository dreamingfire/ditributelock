package ml.dreamingfire.group.test.distributelock.util;

import java.util.concurrent.atomic.AtomicInteger;

public final class Counter {
    private AtomicInteger nameSequence;
    private AtomicInteger numberSequence;

    public Counter() {
        this.nameSequence = new AtomicInteger(1);
        this.numberSequence = new AtomicInteger(1);
    }

    public String getName() {
        return String.valueOf(nameSequence.getAndIncrement());
    }

    public int getSleepTime() {
        return numberSequence.getAndAdd(2);
    }
}
