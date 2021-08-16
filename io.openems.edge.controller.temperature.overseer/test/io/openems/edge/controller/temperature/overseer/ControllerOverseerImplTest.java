package io.openems.edge.controller.temperature.overseer;
/*
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.decentral.test.DummyDecentralHeater;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import io.openems.edge.timer.api.DummyTimer;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerType;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class ControllerOverseerImplTest {

    private static class MyConfig extends AbstractComponentConfig implements Config {

        private final String id;
        private final String alias;
        private final boolean enabled;
        private final String service_pid;
        private final boolean invert;
        private final int startTemperature;
        private final int testPeriod;
        private String sourceThermometer;
        private String targetThermometer;
        private String timerId;
        private final int errorDelta;
        private String componentId;
        private String[] channelIdList;
        private String enableChannel;
        private String expectedValue;
        private String targetComponentId;
        private int errorPeriod;
        private boolean configurationDone;

        MyConfig(String id, String alias, boolean enabled, String service_pid, String sourceThermometer,
                 String targetThermometer, int errorDelta, boolean invert, int startTemperature,
                 int testPeriod, String timerId, String enableChannel, String expectedValue, String targetComponentId,
                 int errorPeriod, boolean configurationDone) {

            super(Config.class, id);
            this.id = id;
            this.alias = alias;
            this.enabled = enabled;
            this.service_pid = service_pid;
            this.sourceThermometer = sourceThermometer;
            this.targetThermometer = targetThermometer;
            this.errorDelta = errorDelta;
            this.invert = invert;
            this.startTemperature = startTemperature;
            this.testPeriod = testPeriod;
            this.timerId = timerId;
            this.enableChannel = enableChannel;
            this.expectedValue = expectedValue;
            this.targetComponentId = targetComponentId;
            this.errorPeriod = errorPeriod;
            this.configurationDone = configurationDone;
        }

        @Override
        public String service_pid() {
            return this.service_pid;
        }

        @Override
        public String componentId() {
            return this.componentId;
        }

        @Override
        public String[] channelIdList() {
            return this.channelIdList;
        }

        @Override
        public String enableChannel() {
            return this.enableChannel;
        }

        @Override
        public String expectedValue() {
            return this.expectedValue;
        }

        @Override
        public String sourceThermometer() {
            return this.sourceThermometer;
        }

        @Override
        public String targetThermometer() {
            return this.targetThermometer;
        }

        @Override
        public String targetComponentId() {
            return this.targetComponentId;
        }

        @Override
        public int errorDelta() {
            return this.errorDelta;
        }

        @Override
        public boolean invert() {
            return this.invert;
        }


        @Override
        public int testPeriod() {
            return this.testPeriod;
        }

        @Override
        public int errorPeriod() {
            return this.errorPeriod;
        }

        @Override
        public String timerId() {
            return this.timerId;
        }

        @Override
        public boolean configurationDone() {
            return this.configurationDone;
        }

    }

    private static ControllerOverseerImpl overseer;
    private static DummyComponentManager cpm;
    private Thermometer sourceThermometer;
    private Thermometer targetThermometer;
    private DummyDecentralHeater heater;
    private DummyTimer timer;
    private ChannelAddress source;
    private ChannelAddress target;
    private ChannelAddress error;
    private MyConfig config;
    private MyConfig configInvers;


    @Before
    public void setUp() throws Exception {

        overseer = new ControllerOverseerImpl();
        cpm = new DummyComponentManager();
        overseer.cpm = cpm;

        config = new MyConfig("ControllerOverseer0", "", true, "", "source",
                "target", 20, false, 200, 0, "test", "source/Temperature", "300", "exception", 0, true);
        configInvers = new MyConfig("ControllerOverseer0", "", true, "", "source",
                "target", 20, true, 200, 0, "test", "source/Temperature", "300", "exception", 0, true);
        sourceThermometer = new DummyThermometer(config.sourceThermometer());
        targetThermometer = new DummyThermometer(config.targetThermometer());
        heater = new DummyDecentralHeater("exception");
        timer = new DummyTimer(config.timerId(), TimerType.TIME);

        source = new ChannelAddress(config.sourceThermometer(), "Temperature");
        target = new ChannelAddress(config.targetThermometer(), "Temperature");
        error = new ChannelAddress("exception", "ExceptionalStateEnableSignal");

        cpm.addComponent(sourceThermometer);
        cpm.addComponent(targetThermometer);
        cpm.addComponent(heater);
        cpm.addComponent(timer);

    }

    @Test
    public void testNoError() throws Exception {


        overseer.activate(null, config);
        overseer.activate(null, config);


        AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, sourceThermometer, targetThermometer, heater, timer, overseer)
                .next(
                        new AbstractComponentTest.TestCase()
                                .input(source, 300)
                                .input(target, 300)
                                .output(error, null)
                );

        controllerTest.run();


    }


    @Test
    public void testError() throws Exception {

        overseer.activate(null, config);
        overseer.activate(null, config);


        AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, sourceThermometer, targetThermometer, heater, timer, overseer)
                .next(
                        new AbstractComponentTest.TestCase()
                                .input(source, 300)
                                .input(target, 400)

                )
                .next(new AbstractComponentTest.TestCase()

                )
                .next(new AbstractComponentTest.TestCase()
                        .output(error, true));

        controllerTest.run();

    }

    @Test
    public void testInversNoError() {
        try {

            overseer.activate(null, configInvers);
            overseer.activate(null, configInvers);


            AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, sourceThermometer, targetThermometer, timer, overseer)
                    .next(
                            new AbstractComponentTest.TestCase()
                                    .input(source, 300)
                                    .input(target, 400)

                    ).next(new AbstractComponentTest.TestCase()
                            .output(error, false));

            controllerTest.run();

        } catch (Exception e) {
            fail();
        }

        assertTrue(true);
    }

    @Test
    public void testInversError() throws Exception {


        overseer.activate(null, configInvers);
        overseer.activate(null, configInvers);


        AbstractComponentTest controllerTest = new ControllerTest(overseer, cpm, sourceThermometer, targetThermometer, heater, timer, overseer)
                .next(
                        new AbstractComponentTest.TestCase()
                                .input(source, 300)
                                .input(target, 300)

                )
                .next(new AbstractComponentTest.TestCase()

                )
                .next(new AbstractComponentTest.TestCase()
                        .output(error, true));

        controllerTest.run();

    }
}
*/