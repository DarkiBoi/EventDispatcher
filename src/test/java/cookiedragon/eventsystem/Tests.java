package cookiedragon.eventsystem;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class Tests {

    private boolean invoked;

    private class Listener {
        @Subscriber
        private void on(Boolean b) {
            invoked = b;
        }
    }

    private final Listener listener = new Listener();

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
