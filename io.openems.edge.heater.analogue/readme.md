# io.openems.edge.heater.analogue Provider

The Analogue Heater. It provides the ability to Control a Heater, PowerPlant, Chp or anything, that is connected to an analogue Module such as Relay, AIO, PWM or LucidControl. 
This Class is an abstraction of any Heater that is connected to an analogue Module.
In the Config you can decide, which type of Analogue Device should be used.
However, an AnalogueHeaterComponent will be created and handles Enable/Disable Signals.



## Example

An AnalogueHeater0 is configured. It has the ControlType Percent and the AnalogueType LucidControl
The Active Value is 80, while the inactive value is 0.
The configured LucidControlDeviceOutput0 is the corresponding OpenEmsComponent
Therefore an AnalogueHeaterLucidControl is instantiated by the AnalogueHeater0 with a default active and inactive Value.
After it gets enabled (or is Set to Autorun) it checks for a new SetPoint PowerValue or Else -> default ActivePower.
if it is disabled (EnableSignal Missing and not set to Autorun) -> default InactivePower is set.

