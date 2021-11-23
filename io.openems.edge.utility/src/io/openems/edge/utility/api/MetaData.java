package io.openems.edge.utility.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface MetaData extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        STREET(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        HOUSE_NUMBER(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        POSTAL_CODE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        PLACE_OF_RESIDENCE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        SERIAL_NUMBER(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        INSTALLATION_DATE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    default WriteChannel<String> getStreet() {
        return this.channel(ChannelId.STREET);
    }

    default WriteChannel<String> getHouseNumber() {
        return this.channel(ChannelId.HOUSE_NUMBER);
    }

    default WriteChannel<String> getPostalCode() {
        return this.channel(ChannelId.POSTAL_CODE);
    }

    default WriteChannel<String> getPlaceOfResidence() {
        return this.channel(ChannelId.PLACE_OF_RESIDENCE);
    }

    default WriteChannel<String> getSerialNumber() {
        return this.channel(ChannelId.SERIAL_NUMBER);
    }

    default Channel<String> getInstallationDate() {
        return this.channel(ChannelId.INSTALLATION_DATE);
    }

}

