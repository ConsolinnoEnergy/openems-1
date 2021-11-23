package io.openems.edge.controller.filewriter;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.utility.api.DummyMetaDataImpl;
import io.openems.edge.utility.api.MetaData;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MyControllerTest {
    private static final String id = "test";
    private static final String fileLocation = "testFile.list";
    private static final String metaDataId = "MetaData";
    private static final String[] keyValuePairs =
            {"Straße:Street", "Hausnummer:HouseNumber", "Postleitzahl:PostalCode", "Ort:PlaceOfResidence", "SerienNummer:SerialNumber", "Inbetriebnahme:InstallationDate"};
    private DummyComponentManager cpm;
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);
    private MetaData metaData;


    @Before
    public void setup() {
        this.cpm = new DummyComponentManager(clock);
        this.metaData = new DummyMetaDataImpl(metaDataId);
        this.cpm.addComponent(this.metaData);
        this.metaData.getSerialNumber().setNextValue("1337-420-69-1337");
        this.metaData.getHouseNumber().setNextValue("2");
        this.metaData.getPostalCode().setNextValue("00000");
        this.metaData.getStreet().setNextValue("MasterStreet");
        this.metaData.getPlaceOfResidence().setNextValue("MasterHome");
        this.metaData.getInstallationDate().setNextValue(new Date().toString());
    }

    @Test
    public void simpleTest() throws Exception {
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];

        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(new LeafletInventoryListFileWriter(), components)
                .addReference("cpm", this.cpm)
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setAlias(id)
                        .setChannels(this.update())
                        .setService_pid("simpleTest")
                        .setOtherComponentId(metaDataId)
                        .setFileLocation(fileLocation)
                        .setKeyValuePairs(keyValuePairs)
                        .setConfigurationDone(true)
                        .build())
                .next(new TestCase()
                        .timeleap(this.clock, 1, ChronoUnit.SECONDS))
                .getSut().run();
    }

    private String[] update() {
           List<Channel<?>> channels =  this.metaData.channels().stream().filter(entry ->
                            !entry.channelId().id().startsWith("_Property")
                    ).collect(Collectors.toList());
        AtomicInteger counter = new AtomicInteger(0);
        String[] channelIdArray = new String[channels.size()];
        channels.forEach(channel -> channelIdArray[counter.getAndIncrement()] = channel.channelId().id());

        return channelIdArray;
        }

    }
