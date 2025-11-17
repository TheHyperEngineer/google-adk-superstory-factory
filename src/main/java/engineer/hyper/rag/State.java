package engineer.hyper.rag;

import java.util.concurrent.ConcurrentHashMap;

public class State extends ConcurrentHashMap<String, Object> {
    // Sentinel value to mark keys for removal
    public static final Object REMOVED = new Object();

    public State() {
        super();
    }

    public State(ConcurrentHashMap<String, Object> map) {
        super(map);
    }
}