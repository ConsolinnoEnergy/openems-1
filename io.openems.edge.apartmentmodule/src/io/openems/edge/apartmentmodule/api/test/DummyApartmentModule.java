package io.openems.edge.apartmentmodule.api.test;

import io.openems.edge.apartmentmodule.api.ApartmentModule;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyApartmentModule extends AbstractOpenemsComponent implements OpenemsComponent, ApartmentModule {
    final boolean isTopAm;

    public DummyApartmentModule(String id, boolean isTopAm) {

        super(OpenemsComponent.ChannelId.values(),
                ApartmentModule.ChannelId.values());
        super.activate(null, id, null, true);
        this.isTopAm = isTopAm;
    }

    @Override
    public boolean isTopAm() {
        return this.isTopAm;
    }
}