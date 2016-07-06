package time_obj.events;

import java.util.EventListener;

import time_obj.Settings;
import time_obj.Time_counter_control;
import time_obj.dialog.User_notification_type;
import time_obj.dialog.User_notification_dialog;


/**
 * The&nbsp;listener interface for receiving IO&nbsp;error event from
 * {@link time_obj} classes. The&nbsp;class that is interested in processing
 * the&nbsp;event implements this interface, and the&nbsp;object created whith
 * that class is subscribed for the&nbsp;event using
 * {@link User_notification_dialog#set_IO_error_listener(IO_error_listener)} method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public interface User_notification_listener extends EventListener
{
	/**
	 * Invoked when IO&nbsp;error event occurs.
	 * 
	 * @param event Object which has generated the&nbsp;event.<br>
	 * IO&nbsp;error may occur in next objects:
	 * <ul><li>{@link Settings};</li>
	 * <li>{@link Time_counter_control}.</li></ul>
	 * 
	 * @param error_type IO&nbsp;error type.
	 * 
	 * @param error_message Text message for user to show.
	 */
	void io_error_occurred(
			User_notification_event event, User_notification_type error_type, String error_message);
}
