package io.openems.edge.heater.api;

import io.openems.edge.timer.api.TimerHandler;

import java.util.Optional;

/**
 * The concrete Implementation of the EnableSignalHandler. It provides an implementation to handle the EnableSignal in
 * the correct way to facilitate use of the EnableSignal.
 * The channel EnableSignal is supposed to be used in the following way:
 * True in the ’nextWriteValue’ of the channel will turn the heater on. When the heater is on and no ’true’ is detected
 * in the ’nextWriteValue’, a timer is started. When no ’true’ is detected in ’nextWriteValue’ until the timer runs out,
 * the heater stops heating. The value in ’nextWriteValue’ is collected with ’getAndReset’, meaning a controller needs
 * to regularly write ’true’ in ’nextWriteValue’ of EnableSignal to keep a heater running. How often is dependant on the
 * duration of the timer.
 * The timer can be set to any value and is given as an argument upon initialization of the handler class. It can be set
 * to use seconds or OpenEMS cycles as a measurement unit of time. For more information on the timer, see
 * {@link io.openems.edge.timer.api.Timer}.
 * The design choice to use the absence of a value as the off signal is based on the possibility of having multiple
 * controllers. The heater turns on when any of multiple controllers gives a signal. If no controller gives the ’on’
 * signal, the heater will switch off. If ’false’ was used as the off signal, a controller hierarchy would be needed to
 * avoid one controllers off signal overwriting another ones on signal.
 *
 * <p>Note:
 * If the component controlling the heater device has just been started, it will most likely take a few cycles before
 * the component receives an EnableSignal command from a controller. Since no EnableSignal means ’turn off the device’,
 * the component will always turn off the device during the first few cycles. If the component is restarted while the
 * device is running, this will then also restart the device.
 * For some heaters (a CHP for example), this is is bad for the lifetime and should be avoided. A method to do that is
 * to check at component startup if the device is already heating. If it is, simply call 'this.setEnableSignal(true)'
 * once (the component sends the EnableSignal to itself). Any controllers then have until the timer runs out to send
 * their EnableSignal if the device is supposed to keep heating.</p>
 */
public class EnableSignalHandlerImpl implements EnableSignalHandler {
    private final TimerHandler timer;
    private boolean currentlyEnabled;

    // This is the identifier of the timer. Needed to get the timer from TimerHandler.
    private final String enableSignalTimerIdentifier;

    public EnableSignalHandlerImpl(TimerHandler timer, String enableSignalTimerIdentifier) {
        this.timer = timer;
        this.enableSignalTimerIdentifier = enableSignalTimerIdentifier;
        this.currentlyEnabled = false;
    }

    @Override
    public boolean deviceShouldBeHeating(Heater heaterComponent) {

        /* Decide state of enabledSignal.
           The convention of EnableSignal is to write "true" in nextWrite to turn the device on, or nothing to turn it
           off (after the timer runs out). This way multiple controllers can access the device without needing controller
           hierarchy.
           So in the nextWrite of the EnableSignal channel is an optional containing true or null, but never false.
           If it contains false, someone is using the EnableSignal channel in the wrong way.
           Fetch the value with get and reset. If a controller wants the device to stay on, it needs to write "true" in
           nextWrite of EnableSignal again before the timer runs out. */

        Optional<Boolean> enabledSignal = heaterComponent.getEnableSignalChannel().getNextWriteValueAndReset();

        // If for some reason enabledSignal is false, treat it as no signal.
        if (enabledSignal.isPresent() && enabledSignal.get()) {
            this.currentlyEnabled = true;
            this.timer.resetTimer(this.enableSignalTimerIdentifier);
            heaterComponent._setEnableSignal(true);     // Set status in ’nextValue’ and ’value’ part of the channel.
            return true;    // enabledSignal is 'true’. Return true, meaning ’turn on device’.
        } else {
            // No value in the Optional or enabledSignal = false.
            if (this.currentlyEnabled
                    && this.timer.checkTimeIsUp(this.enableSignalTimerIdentifier) == false) {
                // Timer has not run out yet. Return true, meaning the device should stay on.
                return true;
            } else {
                // No ’true’ in enabledSignal and no timer running. Return false, meaning device should be off.
                heaterComponent._setEnableSignal(false);
                return false;
            }
        }
    }
}
