package io.openems.edge.apartmentmodule;

/**
 * The ModbusId's for the ApartmentModules.
 */
public enum ModbusId {
    ID_1(1, "1 - Bottom"), //
    ID_2(2, "2 - Top"), //
    ID_3(3, "3 - Top"), //
    ID_4(4, "4 - Bottom"), //
    ID_5(5, "5 - Bottom"); //

    private final int value;
    private final String name;

    ModbusId(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get the Value of the Enum.
     *
     * @return the value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Get the Name of the Enum.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

}