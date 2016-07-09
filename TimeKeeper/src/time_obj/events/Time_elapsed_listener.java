package time_obj.events;

import java.util.EventListener;

import time_obj.Time_counter;


/**
 * The&nbsp;listener interface for receiving <i>time elapsed event</i>. It is
 * provided with the&nbsp;aim for GUI to implement alarm sound or some visual
 * effect to notify user about time has elapsed.<br>
 * The&nbsp;class that is interested in processing the&nbsp;event implements
 * this interface, and the&nbsp;object created with that class is subscribed for
 * the&nbsp;event using
 * {@link Time_counter#add_Time_elapsed_listener(Time_elapsed_listener)} method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
@FunctionalInterface
public interface Time_elapsed_listener extends EventListener
{
	/**
	 * Invoked when time&nbsp;counter time&nbsp;value reaches zero.<br>
	 * To be correct the&nbsp;method invoked <u>by zero value</u> when there is
	 * transition <u>from negative to positive</u> time&nbsp;value. When
	 * transition is <u>from positive to negative</u> time&nbsp;value, method
	 * invoked <u>by &#8209;1&nbsp;second</u> value.
	 * 
	 * @param event {@link Time_counter} object which has generated
	 * the&nbsp;event.
	 */
	void time_counter_reached_zero(Time_counter_event<?> event);
}
