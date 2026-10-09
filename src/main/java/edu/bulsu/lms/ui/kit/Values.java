package edu.bulsu.lms.ui.kit;

import java.util.LinkedHashMap;
import java.util.Map;

/** The validated answers collected from the fields of a form or wizard, keyed by field id. */
public final class Values {

    private final Map<String, Object> data = new LinkedHashMap<>();

    public void put(String id, Object value) {
        data.put(id, value);
    }

    public void putAll(Values other) {
        data.putAll(other.data);
    }

    public void clear() {
        data.clear();
    }

    public Values copy() {
        Values copy = new Values();
        copy.data.putAll(data);
        return copy;
    }

    /** @throws IllegalStateException if no field with that id was collected: a programming error */
    public <T> T get(String id, Class<T> type) {
        if (!data.containsKey(id)) {
            throw new IllegalStateException("No value was collected for field '" + id + "'");
        }
        return type.cast(data.get(id));
    }

    public String text(String id) {
        return get(id, String.class);
    }
}
