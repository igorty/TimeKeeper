package time_obj.events;

import java.util.EventListener;

import time_obj.Instance_counter;
import time_obj.Settings;
import time_obj.Time_counter_control;
import time_obj.dialog.User_notification_type;
import time_obj.dialog.User_notification_dialog;


/**
 * The&nbsp;listener interface for receiving <i>user notification event</i> from
 * {@link time_obj} classes. The&nbsp;class that is interested in processing
 * the&nbsp;event implements this interface, and the&nbsp;object created with
 * that class is subscribed for the&nbsp;event using
 * {@link User_notification_dialog#set_User_notification_listener(User_notification_listener)}
 * method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public interface User_notification_listener extends EventListener
{
	/**
	 * Invoked when <i>user notification event</i> occurs. It is expected
	 * receiving listener to implement UI&nbsp;dialog&nbsp;window to inform user
	 * about the&nbsp;event.
	 * 
	 * @param event Object which has generated the&nbsp;event.<br>
	 * <i>User notification event</i> may occur in next objects:
	 * <ul><li>{@link Settings};</li>
	 * <li>{@link Time_counter_control};</li>
	 * <li>{@link Instance_counter}</li>.</ul>
	 * 
	 * @param user_notification_type User notification type.
	 * 
	 * @param message Text message for user to show.
	 */
	void user_notification_occurred(User_notification_event event,
			User_notification_type user_notification_type, String message);
}
