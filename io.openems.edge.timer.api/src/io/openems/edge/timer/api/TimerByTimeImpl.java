package io.openems.edge.timer.api;


import io.openems.edge.common.component.OpenemsComponent;
import org.joda.time.DateTime;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = TimerByTimeConfig.class, factory = true)
@Component(name = "Timer.TimerByTime",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true
)
public class TimerByTimeImpl extends AbstractTimer implements OpenemsComponent {

    public TimerByTimeImpl(){
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


    @Override
    public boolean checkIsTimeUp(String id, String identifier) {
        ValueInitializedWrapper wrapper = super.getWrapper(id, identifier);
        if (wrapper.isInitialized()) {
            return DateTime.now().isAfter(wrapper.getInitialDateTime().get().plusSeconds(wrapper.getMaxValue()));
        } else {
            wrapper.setInitialized(true);
            wrapper.getInitialDateTime().set(new DateTime());
        }
        return false;
    }

}
