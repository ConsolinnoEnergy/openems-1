package io.openems.edge.bridge.mqtt.api;

/**
 * This Wrapper Class is needed by the SubscribeTask. It holds the Value and Expiration Time for a specific Method.
 */
public class CommandWrapper {

    private String value;
    private String expiration;
    private boolean infinite;

    public CommandWrapper(String value, String expiration) {
        this.value = value;
        this.expiration = expiration;
    }

    /**
     * Get the Value send by the Command subscription.
     *
     * @return the value.
     */
    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    /**
     * get the Expirationtime of this command. (Date is saved in the Task. ExpirationTime in seconds is stored here.
     *
     * @return the expirationTime.
     */
    public String getExpiration() {
        return this.expiration;
    }

    /**
     * If no expirationTime is given, this will be true.
     *
     * @return if the command holds up forever.
     */
    public boolean isInfinite() {
        return this.infinite;
    }

    /**
     * Sets the Expiration. If The Expiration should be infinite. The Infinite Boolean will be set and called later in
     * MqttConfigurationComponent.
     *
     * @param expiration expirationTime usually set by MqttSubscribeTaskImpl.
     */
    void setExpiration(String expiration) {
        this.infinite = expiration.toUpperCase().trim().equals("INFINITE");

        this.expiration = expiration;
    }
}
