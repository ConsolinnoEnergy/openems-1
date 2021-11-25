package io.openems.edge.controller.filewriter;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.utility.api.DummyMetaDataImpl;
import io.openems.edge.utility.api.MetaData;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MyControllerTest {
    private static final String id = "test";
    private static final String fileLocation = "test/output/testFile.list";
    private static final String metaDataId = "MetaData";
    private static final String[] keyValuePairs =
            {"Straße:Street", "Hausnummer:HouseNumber", "Postleitzahl:PostalCode", "Ort:PlaceOfResidence", "SerienNummer:SerialNumber", "Inbetriebnahme:InstallationDate"};
    private DummyComponentManager cpm;
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);
    private MetaData metaData;
    private static final String houseNumber = "2";
    private static final String street = "MasterStreet";
    private static final String placeOfResidence = "MasterHome";
    private static final String serialNumber = "1337-420-69";


    @Before
    public void setup() {
        this.cpm = new DummyComponentManager(clock);
        this.metaData = new DummyMetaDataImpl(metaDataId);
        this.cpm.addComponent(this.metaData);
        this.metaData.getSerialNumber().setNextValue(serialNumber);
        this.metaData.getHouseNumber().setNextValue(houseNumber);
        this.metaData.getPostalCode().setNextValue("00000");
        this.metaData.getStreet().setNextValue(street);
        this.metaData.getPlaceOfResidence().setNextValue(placeOfResidence);
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
                .next(new TestCase()
                        .timeleap(this.clock, 1, ChronoUnit.SECONDS))
                .next(new TestCase()
                        .timeleap(this.clock, 1, ChronoUnit.SECONDS))
                .getSut().run();
    }

    @Test
    public void simpleTestAfterFileCreated() throws Exception {
        this.createDirAndfileIfNotExistent();
        Writer writer = new BufferedWriter(new FileWriter(fileLocation));
        writer.write("Foo=Bar\nBaba=You\n");
        writer.flush();
        writer.close();
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
                        .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                        .input(metaData.getPlaceOfResidence().address(), placeOfResidence + "2")
                        .input(metaData.getHouseNumber().address(), houseNumber + "a")
                        .input(metaData.getSerialNumber().address(), serialNumber + "-911")
                        .input(metaData.getStreet().address(), street + "2")
                )
                .next(new TestCase()
                        .timeleap(this.clock, 1, ChronoUnit.SECONDS))
                .next(new TestCase()
                        .timeleap(this.clock, 1, ChronoUnit.SECONDS))
                .getSut().run();
    }

    private String[] update() {
        List<Channel<?>> channels = this.metaData.channels().stream().filter(entry ->
                !entry.channelId().id().startsWith("_Property")
        ).collect(Collectors.toList());
        AtomicInteger counter = new AtomicInteger(0);
        String[] channelIdArray = new String[channels.size()];
        channels.forEach(channel -> channelIdArray[counter.getAndIncrement()] = channel.channelId().id());

        return channelIdArray;
    }

    /**
     * Checks if the csvFile exists.
     *
     * @return true if file is not existing.
     */
    private void createDirAndfileIfNotExistent() {
        File f = new File(fileLocation);
        if (!f.exists() && !f.isDirectory()) {
            if (f.getParentFile() != null && !f.getParentFile().exists()) {
                boolean dirSuccess = f.getParentFile().mkdirs();
            }
            try {
                boolean fileCreateSuccess = f.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }
}
