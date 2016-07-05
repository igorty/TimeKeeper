package time_obj.dialog;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import time_obj.Settings;
import time_obj.Time_counter_control;
import time_obj.events.IO_error_event;
import time_obj.events.IO_error_listener;


/**
 * Stores object implementing {@link IO_error_listener} and notifies it about
 * IO&nbsp;error occurred.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Read_write_dialog
{
	///// Fields private static ===========================================/////
	/** Logs this class's events. */
	private static final Logger logger;
	
	/** Listener to be notified about IO&nbsp;error if such occurs. */
	private static IO_error_listener listener;
	
	/** Synchronizes access to {@link #listener}. */
	private static ReentrantLock lock;
	
	
	static
	{
		logger = Logger.getLogger(Read_write_dialog.class.getName());
		listener = null;
		lock = new ReentrantLock();
	}
	
	
	///// Методы public статические =======================================/////
	/**
	 * Sets specified {@code listener} to be notified about IO&nbsp;error events.<br>
	 * <b>Important!</b> <u>Only one</u> {@link IO_error_listener} can be
	 * subscribed to receive IO&nbsp;error event simultaneously. Calling this
	 * method <u>when there&nbsp;is another listener already set</u> owerwrites
	 * existing listener with new&nbsp;one.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with {@link #notify_listener(IO_error_event, IO_error_type, String)}.
	 * 
	 * @param listener Listener to be subscribed to IO&nbsp;error events
	 * notifying. <u>Can</u> be {@code null}. In this case existing listener (if
	 * such present) is unsubscribed from receiving the&nbsp;events (removed).
	 */
	public static void set_IO_error_listener(final IO_error_listener listener)
	{
		try
		{
			lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			Read_write_dialog.listener = listener;
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/* TODO: ? Is it better to move this class in "time_obj" package and change
	 * this methods's access modifier to default? */
	/**
	 * Notifies listener (if such present) about IO&nbsp;error event occurred.<br>
	 * <b>Important!</b> Notification is performed in <u>a&nbsp;single</u>
	 * thread. This means that {@code event} object's method, in which
	 * error&nbsp;event has&nbsp;occurred, waits receiving listener to complete
	 * notification processing. It is expected receiving listener to implement
	 * UI&nbsp;dialog&nbsp;window to inform user about the&nbsp;event.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with {@link #set_IO_error_listener(IO_error_listener)}.
	 * 
	 * @param event Object which has generated the&nbsp;event.<br>
	 * {@link IO_error_event#getSource()} <u>must</u> return one of these
	 * objects:
	 * <ul><li>{@link Settings};</li>
	 * <li>{@link Time_counter_control}.</li></ul>
	 * 
	 * @param error_type IO&nbsp;error type.
	 * 
	 * @param error_message Text message for user to show.
	 * 
	 * @exception NullPointerException At least one passed argument
	 * is&nbsp;{@code null}.
	 * 
	 * @exception IllegalArgumentException Passed {@code event}&nbsp;object's
	 * method {@link IO_error_event#getSource()} is not one of the&nbsp;listed
	 * possible types.
	 */
	public static void notify_listener(final IO_error_event event,
			final IO_error_type error_type, final String error_message)
	{
		// Method argumetns cannot be null
		if (event == null || error_type == null || error_message == null)
		{
			throw new NullPointerException(
					"At least one of passed argumetns is null");
		}
		
		// Generated event source object
		final Object source = event.getSource();
		
		// If "source" is not one of the listed possible types
		if (!(source instanceof Settings) && !(source instanceof Time_counter_control))
		{
			throw new IllegalArgumentException(
					"source argument has incorrect " + source.getClass().getName() + " type");
		}
		
		try
		{
			lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			// If there is listener subscribed to notifications
			if (listener != null)
			{
				listener.io_error_occurred(event, error_type, error_message);
			}
		}
		finally
		{
			lock.unlock();
		}
	}
}
