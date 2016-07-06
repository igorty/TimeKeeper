package time_obj.dialog;

import time_obj.events.IO_error_listener;

/**
 * Possible IO&nbsp;error event notifying types which can be passed with
 * {@link IO_error_listener#io_error_occurred(time_obj.events.IO_error_event, IO_error_type, String)}
 * method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public enum IO_error_type
{
	/** IO&nbsp;error event belongs to read&nbsp;error. */
	IOET_read_error,
	/** IO&nbsp;error event belongs to write&nbsp;error. */
	IOET_write_error
}
