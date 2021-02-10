package cookiedragon.eventsystem;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class StaticTests {

    private static boolean invoked;

    private static class Listener {
        @Subscriber
        private void on(Boolean b) {
            invoked = b;
        }
    }

    private static final Listener listener = new Listener();

    @Test
    void test() {

        EventDispatcher.Companion.register(listener);

        EventDispatcher.Companion.subscribe(listener);
        EventDispatcher.Companion.dispatch(true);
        Assertions.assertTrue(invoked);

        EventDispatcher.Companion.unsubscribe(listener);
        EventDispatcher.Companion.dispatch(false);
        Assertions.assertTrue(invoked);

    }

}
