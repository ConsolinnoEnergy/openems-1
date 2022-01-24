package io.openems.edge.timer.api;

import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;


/**
 * This Timer is one of the child Implementations of the {@link AbstractTimer} and the {@link Timer}.
 * It gets the {@link ValueInitializedWrapper} and checks if the the current Counter is above or equals to the maximum.
 * Remember on init -> the counter will initialized and set to 1.
 * Call the {@link Timer#reset(String id, String identifier)} method,
 * if you wish to reset (this will do: {@link ValueInitializedWrapper#setInitialized(boolean)} (false)}
 * Usually you call this Timer via the TimerHandler and only once per Cycle (Therefore TimerByCycles)
 * However, you may call this timer more Frequently if you want. Becoming more of a Timer that counts "Calls" and returns
 * true, when X amount of calls are done.
 */
@Designate(ocd = TimerByCyclesConfig.class, factory = true)
@Component(name = "Timer.TimerByCycles",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
public class TimerByCycles extends AbstractTimer implements OpenemsComponent {


    public TimerByCycles() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, TimerByCyclesConfig config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Modified
    void modified(ComponentContext context, TimerByTimeConfig config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
    }

    /**
     * Check if the Time for this Component is up.
     * Get the current Counter of the {@link ValueInitializedWrapper#getCounter()} increment,
     * and check if the maxValue is reached.
     * Note: After Init the return value will always be false.
     * @param id         the OpenemsComponent Id.
     * @param identifier the identifier the component uses.
     * @return true if Time is up.
     */
    @Override
    public boolean checkIsTimeUp(String id, String identifier) {
        ValueInitializedWrapper wrapper = super.getWrapper(id, identifier);
        if (wrapper.isInitialized()) {
            return wrapper.getCounter().getAndIncrement() >= wrapper.getMaxValue();
        } else {
            wrapper.setInitialized(true);
            wrapper.getCounter().set(1);
        }
        return false;
    }
}
