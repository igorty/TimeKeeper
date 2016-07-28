package main_package;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import main_package.events.Locale_change_listener;
import time_obj.Settings;
import time_obj.Settings.Locale_setting;


/**
 * Contains <i>graphic user interface</i> settings.<br>
 * <i>Note.</i> This class is <u>a&nbsp;singleton</u>. Its instance can be
 * obtained using {@link #get_instance()} method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class GUI_settings implements Serializable
{
	///// Enums private ===================================================/////
	/**
	 * Contains resource bundles <i>.properties</i> file&nbsp;paths.
	 * 
	 * @version 1.0
	 * @author Igor Taranenko
	 */
	private enum Resource
	{
		/** Provides dialog messages. */
		R_messages("main_package/resources/messages"),
		/** Provides specific time&nbsp;counter settings names. */
		R_time_counter("main_package/resources/time_counter"),
		/** Provides buttons names. */
		R_buttons("main_package/resources/GUI_elements/buttons"),
		/** Provides hints and tooltips texts. */
		R_hints("main_package/resources/GUI_elements/hints"),
		/** Provides labels names. */
		R_labels("main_package/resources/GUI_elements/labels"),
		/** Provides text&nbsp;fields texts. */
		R_text_fields("main_package/resources/GUI_elements/text_fields");
		
		
		///// Fields default-access of-instance ===========================/////
		/** Resource bundle <i>.properties</i> file&nbsp;path. */
		final String file_path;
		
		
		///// Constructors private ========================================/////
		/**
		 * @param file_path Recource bundle <i>.properties</i> file&nbsp;path.
		 */
		private Resource(final String file_path)
		{
			this.file_path = file_path;
		}
	}
	
	
	///// Fields private static ===========================================/////
	/** Logs this class's events. */
	private static final Logger logger;
	
	/** The&nbsp;only class's instance. */
	private static final GUI_settings instance;
	
	// TODO: Declare string which contains GUI settings file name

	
	static
	{
		logger = Logger.getLogger(GUI_settings.class.getName());
		instance = new GUI_settings();
	}
	
	
	///// Fields private of-instance ======================================/////
	// TODO: "readObject()" initialization
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * dialog messages. */
	private transient ResourceBundle messages_resources;
	// TODO: "readObject()" initialization
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * specific time&nbsp;counter settings names. */
	private transient ResourceBundle time_counter_resources;
	// TODO: "readObject()" initialization
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * buttons names. */
	private transient ResourceBundle buttons_resources;
	// TODO: "readObject()" initialization
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * hints and tooltips texts. */
	private transient ResourceBundle hints_resources;
	// TODO: "readObject()" initialization
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * labels names. */
	private transient ResourceBundle labels_resources;
	// TODO: "readObject()" initialization
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * text&nbsp;fields texts. */
	private transient ResourceBundle text_fields_resources;
	
	/** Contains listeners subscribed for {@link Locale_change_listener} event. */
	private transient ArrayList<Locale_change_listener> locale_change_listeners;
	
	/** Synchronizes access to {@link #locale_change_listeners} field. */
	private transient ReentrantLock locale_change_listeners_lock;
	
	
	///// Instance initializer ============================================/////
	{
		messages_resources =
				ResourceBundle.getBundle(Resource.R_messages.file_path);
		time_counter_resources =
				ResourceBundle.getBundle(Resource.R_time_counter.file_path);
		buttons_resources =
				ResourceBundle.getBundle(Resource.R_buttons.file_path);
		hints_resources = ResourceBundle.getBundle(Resource.R_hints.file_path);
		labels_resources = ResourceBundle.getBundle(Resource.R_labels.file_path);
		text_fields_resources =
				ResourceBundle.getBundle(Resource.R_text_fields.file_path);
		
		locale_change_listeners = new ArrayList<>();
		locale_change_listeners_lock = new ReentrantLock();
	}
	
	
	///// Constructors private ============================================/////
	/**
	 * Sole private constructor to implement singleton pattern.
	 */
	private GUI_settings()
	{
	}
	
	
	///// Methods public static ===========================================/////
	/**
	 * @return Sole class's instance.
	 */
	public static GUI_settings get_instance()
	{
		return instance;
	}
	
	
	///// Methods public of-instance ======================================/////
	/**
	 * Adds specified {@code listener} to receive <i>program&nbsp;locale change
	 * event</i>. Same {@code listener} <u>can</u> be&nbsp;added multiple times.<br>
	 * <i>Note.</i> It is recommended to unsubscribe listener using
	 * {@link #remove_Locale_change_listener(Locale_change_listener)} method
	 * when there&nbsp;is no&nbsp;need to receive such event. Such action
	 * reduces resource usage and prevents resource leaks.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with (private methods <u>are&nbsp;not</u> listed):
	 * <ul><li>{@link #remove_Locale_change_listener(Locale_change_listener)}.</li></ul>
	 * 
	 * @param listener Listener to be subscribed on event.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	public void add_Locale_change_listener(final Locale_change_listener listener)
	{
		// Passed argument cannot be null
		if (listener == null)
		{
			throw new NullPointerException(
					Locale_change_listener.class.getName() + " argument is null");
		}
		
		try
		{
			locale_change_listeners_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			locale_change_listeners.add(listener);
		}
		finally
		{
			locale_change_listeners_lock.unlock();
		}
	}
	
	
	/**
	 * Removes <u>first occurrence</u> of specified {@code listener} argument
	 * <u>if such present</u>.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with (private methods <u>are&nbsp;not</u> listed):
	 * <ul><li>{@link #add_Locale_change_listener(Locale_change_listener)}.</li></ul>
	 * 
	 * @param listener Listener to be unsubscribed from event notifying.
	 * 
	 * @return {@code true}&nbsp;&#0151; <u>first occurrence</u> of
	 * {@code listener} argument <u>successfully removed</u> (unsubscribed) from
	 * event notifying. {@code false}&nbsp;&#0151; <u>there&nbsp;is no</u> such
	 * {@code listener} (i.e.&nbsp;nothing to remove).
	 * 
	 * @exception NullPointerException {@code listener} argument is {@code null}.
	 */
	public boolean remove_Locale_change_listener(
			final Locale_change_listener listener)
	{
		// Passed argument cannot be null
		if (listener == null)
		{
			throw new NullPointerException(
					Locale_change_listener.class.getName() + " argument is null");
		}
		
		try
		{
			locale_change_listeners_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			return locale_change_listeners.remove(listener);
		}
		finally
		{
			locale_change_listeners_lock.unlock();
		}
	}
	
	
	/**
	 * @return Resource bundle representing <i>.properties</i> resource which
	 * contains dialog messages.
	 */
	public ResourceBundle get_messages_resources()
	{
		return messages_resources;
	}
	
	
	/**
	 * @return Resource bundle representing <i>.properties</i> resource which
	 * contains specific time&nbsp;counter settings names.
	 */
	public ResourceBundle get_time_counter_resources()
	{
		return time_counter_resources;
	}
	
	
	/**
	 * @return Resource bundle representing <i>.properties</i> resource which
	 * contains buttons names.
	 */
	public ResourceBundle get_buttons_resources()
	{
		return buttons_resources;
	}
	
	
	/**
	 * @return Resource bundle representing <i>.properties</i> resource which
	 * contains hints and tooltips texts.
	 */
	public ResourceBundle get_hints_resources()
	{
		return hints_resources;
	}
	
	
	/**
	 * @return Resource bundle representing <i>.properties</i> resource which
	 * contains labels names.
	 */
	public ResourceBundle get_labels_resources()
	{
		return labels_resources;
	}
	
	
	/**
	 * @return Resource bundle representing <i>.properties</i> resource which
	 * contains text&nbsp;fields texts.
	 */
	public ResourceBundle get_text_fields_resources()
	{
		return text_fields_resources;
	}
	
	
	///// Methods default-access of-instance ==============================/////
	/**
	 * Sets program locale.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #add_Locale_change_listener(Locale_change_listener)};</li>
	 * <li>{@link #remove_Locale_change_listener(Locale_change_listener)}.</li></ul>
	 * 
	 * @param locale Locale to be set.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	void set_program_locale(final Locale_setting locale)
	{
		Settings.get_instance().set_locale_setting(locale);
		
		// New locale according to which new resource bundles will be obtained
		final Locale new_locale = new Locale(
				locale.language_code, locale.country_code, locale.variant_code);
		
		messages_resources = ResourceBundle.getBundle(
				Resource.R_messages.file_path, new_locale);
		time_counter_resources = ResourceBundle.getBundle(
				Resource.R_time_counter.file_path, new_locale);
		buttons_resources =
				ResourceBundle.getBundle(Resource.R_buttons.file_path, new_locale);
		hints_resources =
				ResourceBundle.getBundle(Resource.R_hints.file_path, new_locale);
		labels_resources =
				ResourceBundle.getBundle(Resource.R_labels.file_path, new_locale);
		text_fields_resources = ResourceBundle.getBundle(
				Resource.R_text_fields.file_path, new_locale);
		
		locale_change_listeners_lock.lock();
		
		/* Locale change listeners notifier to notify each listener in
		 * a separate thread */
		ThreadPoolExecutor notifier = null;
		
		try
		{
			final int locale_change_listeners_quantity =
					locale_change_listeners.size();
			
			notifier = new ThreadPoolExecutor(
					locale_change_listeners_quantity,
					locale_change_listeners_quantity,
					0, TimeUnit.NANOSECONDS,
					new ArrayBlockingQueue<>(locale_change_listeners_quantity));
			notifier.prestartAllCoreThreads();
			
			// Locale change listeners notifying (each in a separate thread)
			for (final Locale_change_listener i : locale_change_listeners)
			{
				notifier.execute(new Runnable()
				{
					@Override
					public void run()
					{
						i.locale_changed();
					}
				});
			}
		}
		finally
		{
			locale_change_listeners_lock.unlock();
			
			// Shutdown executor
			if (notifier != null)
			{
				notifier.setCorePoolSize(0);
			}
		}
	}
	
	
	///// Methods private of-instance =====================================/////
	// TODO: Declare "writeObject()" method for serialization purposes
	
	
	// TODO: Declare "readObject()" method for deserialization purposes
	
	
	// TODO: Declare "readResolve()" method to provide singleton realization
}
