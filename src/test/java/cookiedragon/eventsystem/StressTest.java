package cookiedragon.eventsystem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Execution(ExecutionMode.SAME_THREAD)
public class StressTest {

    private static final String event = "";
    private static int hits = 0;

    @Test
    void test() {

        final int subs = 1_000;
        final int pubs = 1_000;
        System.out.println("subs: " + subs);
        System.out.println("pubs: " + pubs);

        List<Listener> listenerContainers = new ArrayList<>();

        IntStream.range(0, subs).forEach(i -> {
            listenerContainers.add(new Listener());
        });

        final long start = System.nanoTime();

        IntStream
                .range(0, subs)
                .forEach(i -> {
                    EventDispatcher.Companion.register(listenerContainers.get(i));
                    EventDispatcher.Companion.subscribe(listenerContainers.get(i));
                });

        IntStream
                .range(0, pubs)
                .forEach(i -> EventDispatcher.Companion.dispatch(event));

        final long end = System.nanoTime() - start;

        System.out.println("hits: " + hits);
        System.out.printf("%,dns (%,dms)\n", end, end / 1000000);

    }

    private class Listener {
        @Subscriber
        private void on(String e) {
            hits++;
        }
    }

}
