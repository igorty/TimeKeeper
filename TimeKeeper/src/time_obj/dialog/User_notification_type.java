package time_obj.dialog;

import time_obj.events.User_notification_listener;


/**
 * Possible notification types which can be passed with
 * {@link User_notification_listener#user_notification_occurred(time_obj.events.User_notification_event, User_notification_type, String)}
 * method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public enum User_notification_type
{
	/** Error occurred during IO&nbsp;operation performing. */
	UNT_IO_error,
	/** Cannot obtain system time&nbsp;zone rules. */
	UNT_time_zone_error,
	/** Informing notification. */
	UNT_informing
}
