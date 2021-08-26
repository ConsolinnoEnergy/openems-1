package io.openems.edge.battery.siemens;

import java.util.Objects;

/**
 * Provides a Tuple of appName and the value for the write Request.
 */
public class EventParameter {
    private final String appName;
    private final int value;

    EventParameter(String appName, int value) {
        this.appName = appName;
        this.value = value;
    }

    /**
     * Returns the AppName.
     * @return appName as String
     */
    public String getAppName() {
        return this.appName;
    }

    /**
     * Returns the Value of the write Request.
     * @return value as int
     */
    public int getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventParameter that = (EventParameter) o;
        return Objects.equals(this.appName, that.appName)
                && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.appName, this.value);
    }

    @Override
    public String toString() {
        return this.getAppName() + "\\\\\\\": " + this.getValue();
    }
}
