package time_obj.events;

import java.util.EventListener;

import time_obj.Solo_counter;


/**
 * The&nbsp;listener interface for receiving numeric overflow event from
 * {@link Solo_counter} object. The&nbsp;class that is interested in processing
 * the&nbsp;event implements this interface, and the&nbsp;object created with
 * that class is subscribed for the&nbsp;event using
 * {@link Solo_counter#add_Numeric_overflow_listener(Numeric_overflow_listener)}
 * method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public interface Numeric_overflow_listener extends EventListener
{
	/**
	 * Invoked when numeric overflow occurs.
	 * 
	 * @param event {@link Solo_counter} object which has generated
	 * the&nbsp;event.
	 */
	void numeric_overflow_occurred(Numeric_overflow_event event);
}
