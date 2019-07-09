package org.apache.gora.jet;

import org.apache.gora.persistency.impl.PersistentBase;

public class JetOutputFormat<KeyOut, ValueOut extends PersistentBase> {
    public KeyOut key;
    public ValueOut value;

    public JetOutputFormat(KeyOut key, ValueOut value) {
        this.key = key;
        this.value = value;
    }

    public KeyOut getKey() {
        return key;
    }

    public void setKey(KeyOut key) {
        this.key = key;
    }

    public ValueOut getValue() {
        return value;
    }

    public void setValue(ValueOut value) {
        this.value = value;
    }
}
