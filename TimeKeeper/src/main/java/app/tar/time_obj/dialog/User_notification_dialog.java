/**
 * Copyright 2016 Igor Taranenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.tar.time_obj.dialog;

import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.tar.time_obj.Instance_counter;
import app.tar.time_obj.Settings;
import app.tar.time_obj.Time_counter_control;
import app.tar.time_obj.events.User_notification_event;
import app.tar.time_obj.events.User_notification_listener;


/**
 * Stores object implementing {@link User_notification_listener} and notifies it
 * about <i>user notification event</i> occurred.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class User_notification_dialog
{
	///// Fields private static ===========================================/////
	/** Logs this class's events. */
	private static final Logger logger;
	
	/** Listener to be notified about <i>user notification event</i> if such
	 * occurs. */
	private static User_notification_listener listener;
	
	/** Synchronizes access to {@link #listener}. */
	private static ReentrantLock lock;
	
	
	static
	{
		logger = Logger.getLogger(User_notification_dialog.class.getName());
		listener = null;
		lock = new ReentrantLock();
	}
	
	
	///// Методы public статические =======================================/////
	/**
	 * Sets specified {@code listener} to be notified about <i>user notification
	 * events</i>.<br>
	 * <i>Notes.</i>
	 * <ul><li><u>Only one</u> {@link User_notification_listener} can be
	 * subscribed to receive <i>user notification event</i> simultaneously.
	 * Calling this method <u>when there&nbsp;is another listener already set</u>
	 * overwrites existing listener with new&nbsp;one.</li>
	 * <li>It&nbsp;is recommended to unsubscribe listener from receiving event
	 * when it is&nbsp;not used anymore to prevent possible resource leaks.</li></ul>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized with:
	 * <ul><li>{@link #notify_listener_and_wait(User_notification_event, User_notification_type, String)};</li>
	 * <li>{@link #notify_listener_and_continue(User_notification_event, User_notification_type, String)}.</li></ul>
	 * 
	 * @param listener Listener to be subscribed to <i>user&nbsp;notification
	 * events</i> notifying. <u>Can</u> be {@code null}. In this case existing
	 * listener (if such present) is unsubscribed from receiving the&nbsp;events
	 * (i.e.&nbsp;removed).
	 */
	public static void set_User_notification_listener(
			final User_notification_listener listener)
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
			User_notification_dialog.listener = listener;
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/* TODO: ? Is it better to move this class in "app.tar.time_obj" package and change
	 * this methods's access modifier to default? */
	/**
	 * Notifies listener (if such present) about <i>user notification event</i>
	 * occurred.<br>
	 * <b>Important!</b> Notification is performed in <u>a&nbsp;single</u>
	 * thread. This means that {@code event} object's method, in which <i>user
	 * notification event</i> has&nbsp;occurred, waits receiving listener to
	 * complete notification processing. It is expected receiving listener to
	 * implement UI&nbsp;dialog&nbsp;window to inform user about the&nbsp;event.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #set_User_notification_listener(User_notification_listener)};</li>
	 * <li>{@link #notify_listener_and_continue(User_notification_event, User_notification_type, String)}.</li></ul>
	 * 
	 * @param event Object which has generated the&nbsp;event.<br>
	 * {@link User_notification_event#getSource()} <u>must</u> return one of these
	 * objects:
	 * <ul><li>{@link Settings};</li>
	 * <li>{@link Time_counter_control};</li>
	 * <li>{@link Instance_counter}.</li></ul>
	 * 
	 * @param user_notification_type User notification type.
	 * 
	 * @param message Text message for user to show.
	 * 
	 * @exception NullPointerException At least one passed argument
	 * is&nbsp;{@code null}.
	 * 
	 * @exception IllegalArgumentException Passed {@code event}&nbsp;object's
	 * method {@link User_notification_event#getSource()} returns <u>other
	 * instead</u> of the&nbsp;listed possible types.
	 */
	public static void notify_listener_and_wait(final User_notification_event event,
			final User_notification_type user_notification_type, final String message)
	{
		// Method arguments cannot be null
		if (event == null || user_notification_type == null || message == null)
		{
			throw new NullPointerException(
					"At least one of passed arguments is null");
		}
		
		// Generated event source object
		final Object source = event.getSource();
		
		// If "source" is not one of the listed possible types
		if (!(source instanceof Settings) &&
				!(source instanceof Time_counter_control) &&
				!(source instanceof Instance_counter))
		{
			throw new IllegalArgumentException("event.getSource() returns incorrect "
					+ source.getClass().getName() + " type");
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
				listener.user_notification_occurred(
						event, user_notification_type, message);
			}
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/* TODO: ? Is it better to move this class in "app.tar.time_obj" package and change
	 * this methods's access modifier to default? */
	/**
	 * Notifies listener (if such present) about <i>user notification event</i>
	 * occurred.<br>
	 * <i>Note.</i> Notification is performed in <u>separate</u> thread, so
	 * calling method <u>does&nbsp;not</u> need to wait receiving listener to
	 * complete notification processing.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #set_User_notification_listener(User_notification_listener)};</li>
	 * <li>{@link #notify_listener_and_wait(User_notification_event, User_notification_type, String)}.</li></ul>
	 * 
	 * @param event Object which has generated the&nbsp;event.<br>
	 * {@link User_notification_event#getSource()} <u>must</u> return
	 * {@link Instance_counter} object.
	 * 
	 * @param user_notification_type User notification type.
	 * 
	 * @param message Text message for user to show.
	 * 
	 * @exception NullPointerException At least one passed argument
	 * is&nbsp;{@code null}.
	 * 
	 * @exception IllegalArgumentException Passed {@code event}&nbsp;object's
	 * method {@link User_notification_event#getSource()} returns <u>other
	 * than</u> {@link Instance_counter} object.
	 */
	public static void notify_listener_and_continue(
			final User_notification_event event,
			final User_notification_type user_notification_type, final String message)
	{
		// Method arguments cannot be null
		if (event == null || user_notification_type == null || message == null)
		{
			throw new NullPointerException(
					"At least one of passed arguments is null");
		}
		
		
		// "event.getSource()" must return only "Instance_counter" object
		if (!(event.getSource() instanceof Instance_counter))
		{
			throw new IllegalArgumentException("event.getSource() returns incorrect "
					+ event.getSource().getClass().getName() + " type");
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
			// If there is NO listener subscribed to notification
			if (listener == null)
			{
				return;
			}
			
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					listener.user_notification_occurred(
							event, user_notification_type, message);
				}
			}).start();
		}
		finally
		{
			lock.unlock();
		}
	}
}
