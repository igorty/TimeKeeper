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
package app.tar.time_obj;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.tar.time_obj.dialog.User_notification_dialog;
import app.tar.time_obj.dialog.User_notification_type;
import app.tar.time_obj.events.User_notification_event;


/**
 * Contains program settings.<br>
 * <i>Notes.</i>
 * <ul><li>This class is <u>a&nbsp;singleton</u>.</li>
 * <li>Setter methods <u>do&nbsp;not</u> affect on existing {@link Time_counter}
 * objects layout style. These methods are only intended for GUI adaptation to
 * user preferences.</li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public final class Settings implements Serializable
{
	/** Serializable class version number. */
	private static final long serialVersionUID = -5026490127714810702L;


	///// Enums public ====================================================/////
	/**
	 * Program locale settings. According to available resource bundle
	 * <i>.properties</i> files.
	 * 
	 * @version 1.0
	 * @author Igor Taranenko
	 */
	public enum Locale_setting
	{
		/** Means the&nbsp;default <u>resource bundle <i>.properties</i> file to
		 * be used</u>, not the&nbsp;default system locale. */
		LS_default("", "", "", "program default language (English)"),
		/** English. */
		LS_English("en", "", "", "English"),
		/** Russian. */
		LS_Russian("ru", "", "", "Русский"),
		/** Ukrainian. */
		LS_Ukrainian("uk", "", "", "Українська");
		
		
		///// Fields public of-instance ===================================/////
		/** Language code. */
		public final String language_code;
		/** Country code. */
		public final String country_code;
		/** Locale variant code. */
		public final String variant_code;
		
		/** Used to represent locale in a&nbsp;user&#8209;friendly manner. */
		public final String locale_representation;
		
		
		///// Constructors private ========================================/////
		/**
		 * <b>Important!</b> {@code language_code}, {@code country_code} and
		 * {@code variant_code} combination <u>must be unique</u>.
		 * 
		 * @param language_code Language code to represent.
		 * 
		 * @param country_code Country code to represent.
		 * 
		 * @param variant_code Locale variant code to represent.
		 * 
		 * @param locale_representation User&#8209;friendly locale
		 * representation.
		 */
		private Locale_setting(
				final String language_code, final String country_code,
				final String variant_code, final String locale_representation)
		{
			this.language_code = language_code;
			this.country_code = country_code;
			this.variant_code = variant_code;
			this.locale_representation = locale_representation;
		}
		
		
		///// Methods public static =======================================/////
		/**
		 * Determines the&nbsp;most suitable locale(&#8209;s) based on
		 * the&nbsp;given arguments.<br>
		 * The&nbsp;method follows next priority:
		 * <ol><li><i>Required language.</i>
		 * <ul><li>If <u>no&nbsp;such language</u> is present, returned
		 * container will only have <u>one value</u>
		 * {@link Locale_setting#LS_default}.</li>
		 * <li>If <u>only one</u> matched language is present, returned
		 * container will have this value only.</li>
		 * <li>If <u>several instances</u> of needed language are present,
		 * method will proceed to the&nbsp;next step.</li></ul></li>
		 * <li><i>Required country.</i>
		 * <ul><li>If <u>no&nbsp;such country</u> is provided:
		 * <ul><li>If there is <u>default language</u> locale present
		 * (i.e.&nbsp;with no&nbsp;country set), returned container will have
		 * this value only.</li>
		 * <li>If <u>no&nbsp;default language</u> locale is present, returned
		 * container will have <u>all matched languages</u>.</li></ul></li>
		 * <li>If there is <u>only one</u> matched country is present, returned
		 * container will have this value only.</li>
		 * <li>If <u>several instances</u> of needed country are present, method
		 * will proceed to the&nbsp;next step.</li></ul></li>
		 * <li><i>Required variant.</i>
		 * <ul><li>If <u>no&nbsp;such variant</u> is provided:
		 * <ul><li>If there is <u>default country</u> locale present
		 * (i.e.&nbsp;with no&nbsp;variant set), returned container will have
		 * this value only;</li>
		 * <li>If <u>no&nbsp;default country</u> locale is present, returned
		 * container will have <u>all matched countries</u>.</li></ul></li>
		 * <li>There is <u>full match</u>. Returned container will have this
		 * value only.</li></ul></li></ol>
		 * Any of the&nbsp;arguments <u>can</u> be an&nbsp;empty string.
		 * 
		 * @param language_code Required language.
		 * 
		 * @param country_code Required country.
		 * 
		 * @param variant_code Required variant.
		 * 
		 * @return Container with most suitable locales.
		 * 
		 * @exception NullPointerException At least one of passed arguments is
		 * {@code null}.
		 */
		public static ArrayList<Locale_setting> most_suitable_locales(
				final String language_code, final String country_code,
				final String variant_code)
		{
			// All available locales
			final Locale_setting[] locale_setting_values = Locale_setting.values();
			// Contains suitable languages
			final ArrayList<Locale_setting> suitable_languages =
					new ArrayList<>(locale_setting_values.length);
			
			// Searching for suitable languages
			for (final Locale_setting i : locale_setting_values)
			{
				// If current language matches with required
				if (i.language_code.equals(language_code))
				{
					suitable_languages.add(i);
				}
			}
			
			// If no suitable languages were found
			if (suitable_languages.isEmpty())
			{
				suitable_languages.add(LS_default);
				
				return suitable_languages;
			}
			
			// Contains suitable countries among suitable languages
			final ArrayList<Locale_setting> suitable_countries =
					new ArrayList<>(suitable_languages.size());
			
			// Searching for suitable countries among suitable languages
			for (final Locale_setting i : suitable_languages)
			{
				// If current country matches with required
				if (i.country_code.equals(country_code))
				{
					suitable_countries.add(i);
				}
			}
			
			// If no suitable countries were found among suitable languages
			if (suitable_countries.isEmpty())
			{
				/* May contain default suitable language (i.e. with no country
				 * code set) */
				final Optional<Locale_setting> default_suitable_language =
						suitable_languages.stream().filter(new Predicate<Locale_setting>()
				{
					@Override
					public boolean test(final Locale_setting array_item)
					{
						return array_item.country_code.equals("");
					}
				}).findFirst();
				
				// If default suitable language is provided for required country
				if (default_suitable_language.isPresent())
				{
					suitable_countries.add(default_suitable_language.get());
					
					return suitable_countries;
				}
				
				return suitable_languages;
			}
			
			// Contains suitable variants among suitable languages and countries
			final ArrayList<Locale_setting> suitable_variants =
					new ArrayList<>(suitable_countries.size());
			
			/* Searching for suitable variants among suitable languages and
			 * countries */
			for (final Locale_setting i : suitable_countries)
			{
				// If current variant matches with required
				if (i.variant_code.equals(variant_code))
				{
					suitable_variants.add(i);
				}
			}
			
			/* If no suitable variants were found among suitable languages and
			 * countries */
			if (suitable_variants.isEmpty())
			{
				final Optional<Locale_setting> default_suitable_country =
						suitable_countries.stream().filter(new Predicate<Locale_setting>()
				{
					@Override
					public boolean test(final Locale_setting array_item)
					{
						return array_item.variant_code.equals("");
					}
				}).findFirst();
				
				// If default suitable country is provided for required variant
				if (default_suitable_country.isPresent())
				{
					suitable_variants.add(default_suitable_country.get());
					
					return suitable_variants;
				}
				
				return suitable_countries;
			}
			
			return suitable_variants;
		}
	}
	
	
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
		R_messages("app/tar/time_obj/messages"),
		/** Provides time&nbsp;counters specified resources. */
		R_time_counter("app/tar/time_obj/time_counter_resources");
		
		
		///// Fields default-access of-instance ===========================/////
		/** Resource bundle <i>.properties</i> file&nbsp;path. */
		final String file_path;
		
		
		///// Constructors private ========================================/////
		/**
		 * @param file_path Resource bundle <i>.properties</i> file&nbsp;path.
		 */
		private Resource(final String file_path)
		{
			this.file_path = file_path;
		}
	}
	
	
	///// Поля private статические ========================================/////
	/** Отвечает за логирование событий. */
	private static final Logger logger;
	
	/** Единственный экземпляр класса. */
	private static final Settings instance;
	
	/** Days&#8209;in&#8209;year/days&#8209;in&#8209;month counting type.
	 * {@link Days_in_year#DIY_360} is set <u>by&nbsp;default</u>.
	 * 
	 * @serial Deserialized value <u>cannot</u> be {@code null}. Otherwise
	 * default value is set. */
	private static Days_in_year days_count;
	
	/** Time&nbsp;counter display style.
	 * {@link Time_display_style#TDS_increase_able} is set <u>by&nbsp;default</u>.
	 * 
	 * @serial Deserialized value <u>cannot</u> be {@code null}. Otherwise
	 * default value is set.
	 */
	private static Time_display_style time_display_style_setting;
	
	/** The&nbsp;way in which time&nbsp;units names will be displayed.
	 * {@link Time_unit_layout#TUL_value_sign} is set <u>by&nbsp;default</u>.
	 * 
	 * @serial Deserialized value <u>cannot</u> be {@code null}. Otherwise
	 * default value is set. */
	private static Time_unit_layout time_unit_layout_setting;
	
	/** Time&nbsp;units displaying&nbsp;range setting. Object represents array
	 * which contains <u>2&nbsp;elements</u>, where element&nbsp;{@code [0]}
	 * represents <u>biggest</u> possible displayed time&nbsp;value (is on
	 * the&nbsp;<u>left</u> side), and element&nbsp;{@code [1]} represents
	 * <u>lowest</u> possible displayed time&nbsp;value (is on
	 * the&nbsp;<u>right</u> side). Default values are:
	 * {@link Time_unit_name#TUN_hours} for element&nbsp;{@code [0]} and
	 * {@link Time_unit_name#TUN_seconds} for element&nbsp;{@code [1]}.
	 * 
	 * @serial Deserialized value restrictions:
	 * <ul><li>Cannot be {@code null}.</li>
	 * <li>Cannot contain {@code null}.</li>
	 * <li>Array length must be equal&nbsp;2.</li>
	 * <li>Element&nbsp;{@code [0]} <u>must contain greater</u> time&nbsp;unit
	 * than element&nbsp;{@code [1]}, or be <u>equal</u> to it.<br>
	 * Examples:
	 * <ul><li>element&nbsp;{@code [0]} containing
	 * {@code Time_unit_name.TUN_months} value and element&nbsp;{@code [1]}
	 * containing {@code Time_unit_name.TUN_hours}&nbsp;<u>is&nbsp;right</u>;</li>
	 * <li>element&nbsp;{@code [0]} containing {@code Time_unit_name.TUN_days}
	 * value and element&nbsp;{@code [1]} with <u>the&nbsp;same</u>
	 * {@code Time_unit_name.TUN_days} value&nbsp; <u>is&nbsp;right</u>;</li>
	 * <li>element&nbsp;{@code [0]} containing {@code Time_unit_name.TUN_days}
	 * value and element&nbsp;{@code [1]} containing
	 * {@code Time_unit_name.TUN_years}&nbsp;<u>is&nbsp;wrong</u>.</li></ul></li></ul>
	 * If deserialized value <u>does&nbsp;not</u> fit&nbsp;under listed
	 * restrictions, default values are set. */
	private static Time_unit_name[] time_value_edges;
	
	/** Current program locale.
	 * 
	 * @serial Deserialized value <u>cannot</u> be {@code null}. Otherwise
	 * default value is set. */
	private static Locale_setting locale;
	
	/** Название файла с настройками */
	private static final String file_name;
	
	/** Кол&#8209;во разрешений для семафора {@link #common_semaphore}. */
	private static final int common_semaphore_permits;
	
	
	static
	{
		logger = Logger.getLogger(Settings.class.getName());
		
		// Current system locale
		final Locale system_locale = Locale.getDefault();
		
		/* Program locale and resources are set before an attempt to read
		 * settings file. "set_defaults()" method doesn't provide mentioned
		 * fields value assignment. So in case of exception fields
		 * are guaranteed to have values */
		locale = Locale_setting.most_suitable_locales(
				system_locale.getLanguage(),
				system_locale.getCountry(),
				system_locale.getVariant()).get(0);
		file_name = "settings.tk";  // TODO: Указать правильную директорию
		common_semaphore_permits = 4;
		instance = new Settings();
	}
	
	
	///// Fields private of-instance ======================================/////
	/** Синхронизирует доступ к полю {@link #time_value_edges}. */
	private transient final ReentrantLock time_value_edges_lock;
	/** Synchronizes access to {@link #days_count},
	 * {@link #time_display_style_setting}, {@link #time_unit_layout_setting},
	 * {@link #locale} fields. */
	private transient final Semaphore common_semaphore;
	
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * dialog messages according to the&nbsp;program locale. */
	private transient ResourceBundle message_resources;
	/** Resource bundle representing <i>.properties</i>  which contains
	 * resources for time&nbsp;counters such&nbsp;as time&nbsp;unit names. */
	private transient ResourceBundle time_counter_resources;
	
	
	///// Instance initialization =========================================/////
	{
		time_value_edges_lock = new ReentrantLock();
		common_semaphore = new Semaphore(common_semaphore_permits);
		
		message_resources = ResourceBundle.getBundle(Resource.R_messages.file_path);
		time_counter_resources =
				ResourceBundle.getBundle(Resource.R_time_counter.file_path);
		
		FileInputStream file_input = null;  // Входящий файловый поток
		// Буферизированный входящий поток
		BufferedInputStream buffered_input = null;
		ObjectInputStream object_input = null;  // Входящий поток объектов
		
		try
		{
			file_input = new FileInputStream(file_name);
			buffered_input = new BufferedInputStream(file_input);
			object_input = new ObjectInputStream(buffered_input);
			object_input.readObject();
		}
		catch (final FileNotFoundException exc)
		{
			logger.log(Level.WARNING, "Cannot find \"" + file_name
					+ "\" settings file. Exception stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(
					new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					file_name + message_resources.getString("settings_file_not_found"));
			set_defaults();
		}
		// Ожидается от "object_input"
		catch (final IOException exc)
		{
			logger.log(Level.SEVERE, "Cannot read file \"" + file_name
					+ "\" settings file. Exception stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(
					new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					message_resources.getString("settings_file_read_error"));
			set_defaults();
		}
		catch (final ClassNotFoundException exc)
		{
			logger.log(Level.SEVERE, '\"' + file_name + "\" settings file"
					+ " contains incompatible class type. Exception stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					message_resources.getString("settings_file_read_error"));
			set_defaults();
		}
		finally
		{
			try
			{
				///// Попытки закрытия потоков вниз по цепочке /////
				/* TODO: ? Не вызовет ли конфликт закрытие потока объектов
				 * (а соответственно и файлового потока) после выполнения
				 * "public void set_defaults()", в котором открываются файловый
				 * и объектный потоки для записи в тот же файл при возникновении
				 * исключения? */
				// Если входящий поток объектов был открыт
				if (object_input != null)
				{
					object_input.close();
				}
				// Если программа успела открыть буферизированный входящий поток
				else if (buffered_input != null)
				{
					buffered_input.close();
				}
				// Если программа успела открыть только файловый входящий поток
				else if (file_input != null)
				{
					file_input.close();
				}
			}
			// При неудачной попытке закрытия потока никаких действий не планируется
			catch (final IOException exc)
			{
				logger.log(Level.WARNING, "Cannot close \"" + file_name
						+ "\" settings file. Exception stack trace:", exc);
			}
		}
	}
	
	
	///// Constructors private ============================================/////
	/**
	 * Private&#8209;конструктор для реализации singleton'а.
	 */
	private Settings() {}
	
	
	///// Методы public статические =======================================/////
	/**
	 * Получение единственного экземпляра данного класса.
	 * 
	 * @return Единственный экземпляр класса.
	 */
	public static Settings get_instance()
	{
		return instance;
	}
	
	
	///// Methods public of-instance ======================================/////	
	/**
	 * Returns days&#8209;in&#8209;year/days&#8209;in&#8209;month counting type.
	 */
	public Days_in_year get_days_in_year()
	{
		return days_count;
	}

	
	/**
	 * Sets days&#8209;in&#8209;year/days&#8209;in&#8209;month counting type.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #set_defaults()};</li>
	 * <li>{@link #write_to_file()}.</li></ul>
	 * 
	 * @param days_count_set Days&#8209;in&#8209;year/days&#8209;in&#8209;month
	 * counting type.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	public void set_days_in_year(final Days_in_year days_count_set)
	{
		// Если параметром метода является null
		if (days_count_set == null)
		{
			throw new NullPointerException(
					Days_in_year.class.getName() + " argument is null");
		}
		
		try
		{
			common_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			days_count = days_count_set;
		}
		finally
		{
			common_semaphore.release();
		}
	}

	
	/**
	 * Returns current time&nbsp;counter display style application setting.
	 */
	public Time_display_style get_time_display_style_setting()
	{
		return time_display_style_setting;
	}

	
	/**
	 * Sets time display style setting.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #set_defaults()};</li>
	 * <li>{@link #write_to_file()}.</li></ul>
	 * 
	 * @param new_value New time display style setting.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	public void set_time_display_style_setting(final Time_display_style new_value)
	{
		// Если параметром метода является null
		if (new_value == null)
		{
			throw new NullPointerException(
					Time_display_style.class.getName() + " argument is null");
		}
		
		try
		{
			common_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_display_style_setting = new_value;
		}
		finally
		{
			common_semaphore.release();
		}
	}

	
	/**
	 * Returns current time&nbsp;units layout style application setting.
	 */
	public Time_unit_layout get_time_unit_layout_setting()
	{
		return time_unit_layout_setting;
	}
	

	/**
	 * Sets time&nbsp;units layout style setting.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #set_defaults()};</li>
	 * <li>{@link #write_to_file()}.</li></ul>
	 * 
	 * @param time_unit_layout_set New time&nbsp;units layout style setting.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	public void set_time_unit_layout_setting(
			final Time_unit_layout time_unit_layout_set)
	{
		// Если параметром метода является null
		if (time_unit_layout_set == null)
		{
			throw new NullPointerException(
					Time_unit_layout.class.getName() + " argument is null");
		}
		
		try
		{
			common_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_unit_layout_setting = time_unit_layout_set;
		}
		finally
		{
			common_semaphore.release();
		}
	}
	

	/**
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #write_to_file()};</li>
	 * <li>{@link #set_defaults()};</li>
	 * <li>{@link #set_time_value_edges(Time_unit_name, Time_unit_name)}.</li></ul>
	 * 
	 * @return Array containing <u>2&nbsp;elements</u>, where
	 * element&nbsp;{@code [0]} represents <u>biggest</u> possible displayed
	 * time&nbsp;value (is on the&nbsp;<u>left</u> side), and
	 * element&nbsp;{@code [1]} represents <u>lowest</u> possible displayed
	 * time&nbsp;value (is on the&nbsp;<u>right</u> side).<br>
	 * <i>Note.</i> Changing returned value <u>does&nbsp;not</u> affect to
	 * the&nbsp;stored one.
	 */
	public Time_unit_name[] get_time_value_edges()
	{
		try
		{
			time_value_edges_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			return time_value_edges.clone();
		}
		finally
		{
			time_value_edges_lock.unlock();
		}
	}

	
	/**
	 * Sets new time&nbsp;units displaying range settings.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized with:
	 * <ul><li>{@link #get_time_value_edges()};</li>
	 * <li>{@link #set_defaults()};</li>
	 * <li>{@link #write_to_file()}.</li></ul>
	 * 
	 * @param left_edge New value for <u>the&nbsp;leftmost</u> time&nbsp;value
	 * (i.e.&nbsp;<u>biggest</u> displaying time&nbsp;unit).
	 * 
	 * @param right_edge New value for <u>the&nbsp;rightmost</u> time&nbsp;value
	 * (i.e.&nbsp;<u>smallest</u> displaying time&nbsp;unit).
	 * 
	 * @exception IllegalArgumentException {@code left_edge} argument <u>must</u>
	 * contain <u>greater</u> time&nbsp;unit than {@code right_edge} argument,
	 * or be <u>equal</u> to it.<br>
	 * <i>Examples:</i>
	 * <ul><li>{@code left_edge} containing {@code Time_unit_name.TUN_months}
	 * value and {@code right_edge} containing
	 * {@code Time_unit_name.TUN_hours}&nbsp;<u>is&nbsp;right</u>;</li>
	 * <li>{@code left_edge} containing {@code Time_unit_name.TUN_days} value
	 * and {@code right_edge} with <u>the&nbsp;same</u>
	 * {@code Time_unit_name.TUN_days} value&nbsp; <u>is&nbsp;right</u>;</li>
	 * <li>{@code left_edge} containing {@code Time_unit_name.TUN_days} value
	 * and {@code right_edge} containing
	 * {@code Time_unit_name.TUN_years}&nbsp;<u>is&nbsp;wrong</u> (exception
	 * will be thrown).</li><ul>
	 * 
	 * @exception NullPointerException At&nbsp;least one of passed arguments
	 * is&nbsp;{@code null}.
	 */
	public void set_time_value_edges(
			final Time_unit_name left_edge, final Time_unit_name right_edge)
	{
		/* Если значение устанавливаемой крайней левой единицы времени
		 * оказалось больше правой */
		if (left_edge.compareTo(right_edge) > 0)
		{
			throw new IllegalArgumentException("left_edge argument value is"
					+ " grater than right_edge argument value");
		}
		
		try
		{
			time_value_edges_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_value_edges[0] = left_edge;
			time_value_edges[1] = right_edge;
		}
		finally
		{
			time_value_edges_lock.unlock();
		}
	}
	
	
	/**
	 * Sets default settings <u>except</u> locale setting.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #get_time_value_edges()};</li>
	 * <li>{@link #set_time_value_edges(Time_unit_name, Time_unit_name)};</li>
	 * <li>{@link #set_days_in_year(Days_in_year)};</li>
	 * <li>{@link #set_locale_setting(Locale_setting)};</li>
	 * <li>{@link #set_time_display_style_setting(Time_display_style)};</li>
	 * <li>{@link #set_time_unit_layout_setting(Time_unit_layout)};</li>
	 * <li>{@link #write_to_file()}.</li></ul>
	 */
	public void set_defaults()
	{
		try
		{
			common_semaphore.acquire(common_semaphore_permits);
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			days_count = Days_in_year.DIY_360;
			time_display_style_setting = Time_display_style.TDS_increase_able;
			time_unit_layout_setting = Time_unit_layout.TUL_value_sign;
			
			try
			{
				time_value_edges_lock.lockInterruptibly();
			}
			catch (final InterruptedException exc)
			{
				logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
				Thread.currentThread().interrupt();
			}
			
			try
			{
				time_value_edges = new Time_unit_name[2];
				
				time_value_edges[0] = Time_unit_name.TUN_hours;
				time_value_edges[1] = Time_unit_name.TUN_seconds;
			}
			finally
			{
				time_value_edges_lock.unlock();
			}
		}
		finally
		{
			common_semaphore.release(common_semaphore_permits);
		}
		
		/* There is a possible 'synchronization window' when outlier thread
		 * changes some settings field(-s) before "write_to_file()" method
		 * obtain mutexes and write settings to file. Consider it to be not
		 * a bug */
		write_to_file();
	}
	
	
	/**
	 * Writes settings to file.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #get_time_value_edges()};</li>
	 * <li>{@link #set_defaults()};</li>
	 * <li>{@link #set_days_in_year(Days_in_year)};</li>
	 * <li>{@link #set_locale_setting(Locale_setting)};</li>
	 * <li>{@link #set_time_display_style_setting(Time_display_style)};</li>
	 * <li>{@link #set_time_unit_layout_setting(Time_unit_layout)};</li>
	 * <li>{@link #set_time_value_edges(Time_unit_name, Time_unit_name)}.</li></ul>
	 */
	public void write_to_file()
	{
		try
		{
			common_semaphore.acquire(common_semaphore_permits);
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			try
			{
				time_value_edges_lock.lockInterruptibly();
			}
			catch (final InterruptedException exc)
			{
				logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
				Thread.currentThread().interrupt();
			}
			
			try
			{
				// Исходящий файловый поток
				FileOutputStream file_output = null;
				// Исходящий буферизированный поток
				BufferedOutputStream buffered_output = null;
				// Исходящий поток объектов
				ObjectOutputStream object_output = null;
				
				try
				{
					file_output = new FileOutputStream(file_name);
					buffered_output = new BufferedOutputStream(file_output);
					object_output = new ObjectOutputStream(buffered_output);
					object_output.writeObject(this);
				}
				catch (final FileNotFoundException exc)
				{
					logger.log(Level.WARNING, "Cannot find \"" + file_name
							+ "\" settings file. Exception stack trace:", exc);
					User_notification_dialog.notify_listener_and_wait(
							new User_notification_event(this),
							User_notification_type.UNT_IO_error,
							message_resources.getString("settings_file_write_error"));
				}
				// Ожидается от "object_output"
				catch (final IOException exc)
				{
					logger.log(Level.SEVERE, "Cannot write settings to \""
							+ file_name + "\" file. Exception stack trace:", exc);
					User_notification_dialog.notify_listener_and_wait(
							new User_notification_event(this),
							User_notification_type.UNT_IO_error,
							message_resources.getString("settings_file_write_error"));
				}
				finally
				{
					try
					{
						///// Попытки закрытия потоков вниз по цепочке /////
						// Если исходящий поток объектов был открыт
						if (object_output != null)
						{
							object_output.close();
						}
						/* Если программа успела открыть буферизированный
						 * исходящий поток */
						else if (buffered_output != null)
						{
							buffered_output.close();
						}
						/* Если программа успела открыть только исходящий
						 * файловый поток */
						else if (file_output != null)
						{
							file_output.close();
						}
					}
					/* В случае возникновения данного исключения никаких
					 * действий не планируется */
					catch (final IOException exc)
					{
						logger.log(Level.WARNING, "Cannot close \"" + file_name
								+ "\" settings file. Exception stack trace:", exc);
					}
				}
			}
			finally
			{
				time_value_edges_lock.unlock();
			}
		}
		finally
		{
			common_semaphore.release(common_semaphore_permits);
		}
	}
	
	
	/**
	 * Sets program locale.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #set_defaults()};</li>
	 * <li>{@link #write_to_file()}.</li></ul>
	 * 
	 * @param locale Locale to be set.
	 * 
	 * @exception NullPointerException {@code locale} argument is {@code null}.
	 */
	public void set_locale_setting(final Locale_setting locale)
	{
		// "locale" argument cannot be null
		if (locale == null)
		{
			throw new NullPointerException(
					Locale_setting.class.getName() + " argument is null");
		}
		
		// If new locale to be set is equal to current settings locale
		if (Settings.locale.equals(locale))
		{
			return;
		}
		
		// Locale to be passed when obtaining new resource bundle locale
		final Locale locale_to_pass;
		
		try
		{
			common_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			Settings.locale = locale;
			
			locale_to_pass = new Locale(Settings.locale.language_code,
					Settings.locale.country_code,
					Settings.locale.locale_representation);
			
			message_resources = ResourceBundle.getBundle(
					Resource.R_messages.file_path, locale_to_pass);
		}
		finally
		{
			common_semaphore.release();
		}

		/* TODO? Is it better to provide "through-setter-access" level instead
		 * of "default-access" for the following resource bundle changes? */
		Time_counter_control.message_resources = message_resources;
		
		// New resource bundle for time counters
		final ResourceBundle new_time_cunter_resources =
				ResourceBundle.getBundle(Resource.R_time_counter.file_path,
						locale_to_pass);
		
		Time_unit_full_name_resource_provider.time_counter_resources =
				new_time_cunter_resources;
		Time_unit_full_name_resource_provider.locale = locale;
		Time_counter.time_counter_resources = new_time_cunter_resources;
		Instance_counter.message_resources = message_resources;
		
		// Thread-safe snapshot of all existing time counters
		final CopyOnWriteArrayList<Time_counter> time_counters =
				new CopyOnWriteArrayList<>(
						Time_counter_control.get_instance().get_time_counters());
		final int time_counters_quantity = time_counters.size();
		
		// If there is no time counters to update locale on
		if (time_counters_quantity == 0)
		{
			return;
		}
		
		final int available_processors = Runtime.getRuntime().availableProcessors();
		// Will update all time counters text values with new locale
		ThreadPoolExecutor notifier = null;
		
		try
		{
			notifier = new ThreadPoolExecutor(
					available_processors > time_counters_quantity ?
							time_counters_quantity : available_processors,
					time_counters_quantity,
					0, TimeUnit.NANOSECONDS,
					new ArrayBlockingQueue<>(time_counters_quantity));
			notifier.prestartAllCoreThreads();

			// Update all time value strings to represent values with new locale
			for (final Time_counter i : time_counters)
			{
				notifier.execute(new Runnable()
				{
					@Override
					public void run()
					{
						i.build_time_string();
						i.notify_time_counter_text_listeners();
					}
				});
			}
		}
		finally
		{
			// Shutdown executor
			if (notifier != null)
			{
				notifier.setCorePoolSize(0);
			}
		}
	}
	
	
	/**
	 * @return Program locale.
	 */
	public Locale_setting get_locale_setting()
	{
		return locale;
	}
	
	
	/**
	 * @return Resource bundle representing <i>.properties</i> resource which
	 * contains resources for time&nbsp;counters such&nbsp;as time&nbsp;unit
	 * names.
	 */
	public ResourceBundle get_time_counter_resources()
	{
		return time_counter_resources;
	}
	
	
	///// Methods default-access of-instance ==============================/////
	/**
	 * @return Resource bundle representing <i>.properties</i> resource which
	 * contains dialog messages according to the&nbsp;program locale.
	 */
	ResourceBundle get_message_resources()
	{
		return message_resources;
	}
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * Метод сериализации для {@link ObjectOutputStream}.
	 * 
	 * @param object_output Исходящий поток объектов.
	 * 
	 * @throws IOException Ошибка ввода/вывода.
	 */
	private void writeObject(final ObjectOutputStream object_output)
			throws IOException
	{
		object_output.defaultWriteObject();
		object_output.writeFloat((float)1.0);  // Запись версии класса
		object_output.writeObject(days_count);
		object_output.writeObject(time_display_style_setting);
		object_output.writeObject(time_unit_layout_setting);
		object_output.writeObject(time_value_edges);
		object_output.writeObject(locale);
	}
	
	
	/**
	 * Метод десериализации для {@link ObjectInputStream}.
	 * 
	 * @param object_input Входящий поток объектов.
	 * 
	 * @throws IOException Ошибка ввода/вывода.
	 * 
	 * @throws ClassNotFoundException Класс сериализованного объекта
	 * не&nbsp;обнаружен.
	 */
	private void readObject(final ObjectInputStream object_input)
			throws IOException, ClassNotFoundException
	{
		object_input.defaultReadObject();
		object_input.skipBytes(4);  // Skip class version (float value)
		
		/* true - all deserialized fields are correct; false - at least on field
		 * is incorrect */
		boolean deserialization_status = true;
		
		days_count = (Days_in_year)object_input.readObject();
		
		// If value stored in the file is null
		if (days_count == null)
		{
			deserialization_status = false;
			days_count = Days_in_year.DIY_360;
		}
		
		time_display_style_setting =
				(Time_display_style)object_input.readObject();
		
		// If value stored in the file is null
		if (time_display_style_setting == null)
		{
			deserialization_status = false;
			time_display_style_setting = Time_display_style.TDS_increase_able;
		}
		
		time_unit_layout_setting = (Time_unit_layout)object_input.readObject();
		
		// If value stored in the file is null
		if (time_unit_layout_setting == null)
		{
			deserialization_status = false;
			time_unit_layout_setting = Time_unit_layout.TUL_value_sign;
		}
		
		/* Временное значение массива "time_value_edges" перед безопасным
		 * копированием */
		final Time_unit_name[] buffer =
				(Time_unit_name[])object_input.readObject();
		
		try
		{
			time_value_edges = buffer.clone();
			
			// If displayed time units range stored in file is incorrect
			if (time_value_edges.length != 2 ||
					time_value_edges[0].compareTo(time_value_edges[1]) > 0)
			{
				deserialization_status = false;
				time_value_edges[0] = Time_unit_name.TUN_hours;
				time_value_edges[1] = Time_unit_name.TUN_seconds;
			}
		}
		catch (final NullPointerException exc)
		{
			deserialization_status = false;
			time_value_edges = new Time_unit_name[2];
			
			time_value_edges[0] = Time_unit_name.TUN_hours;
			time_value_edges[1] = Time_unit_name.TUN_seconds;
		}
		
		locale = (Locale_setting)object_input.readObject();
		
		// If value stored in the file is null
		if (locale == null)
		{
			locale = Locale_setting.LS_default;
		}
		
		// Locale to be passed when obtaining resource bundles
		final Locale locale_to_pass = new Locale(locale.language_code,
				locale.country_code, locale.variant_code);
		
		message_resources = ResourceBundle.getBundle(
				Resource.R_messages.file_path, locale_to_pass);
		time_counter_resources = ResourceBundle.getBundle(
				Resource.R_time_counter.file_path, locale_to_pass);
		
		// If at least one deserialized field is incorrect
		if (!deserialization_status)
		{
			logger.log(Level.WARNING,
					"At least one deserialized " + Settings.class.getName()
							+ " class field is incorrect");
			User_notification_dialog.notify_listener_and_continue(
					new User_notification_event(this),
					User_notification_type.UNT_file_error,
					"incorrect_settings_file_content.1" + file_name
							+ "incorrect_settings_file_content.2");
		}
	}
	
	
	/**
	 * Prevents several instances creating of this <i>singleton</i> class while
	 * deserialization.
	 * 
	 * @return Reference to the&nbsp;single class instance.
	 * 
	 * @throws ObjectStreamException Any exception related to object input
	 * stream.
	 */
	private Object readResolve()
			throws ObjectStreamException
	{
		return this;
	}
}
