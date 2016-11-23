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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.tar.time_obj.containers.Modified_ArrayList;
import app.tar.time_obj.events.Time_counter_event;
import app.tar.time_obj.events.Time_counter_text_listener;
import app.tar.time_obj.events.Time_elapsed_listener;


/**
 * Инкапсулирует основные свойства счетчика времени.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public abstract class Time_counter implements Serializable
{
	/** Serializable class version number. */
	private static final long serialVersionUID = 710648485265535381L;


	///// Inner classes public ============================================/////
	/**
	 * Serves as container representing time&nbsp;counter text value and
	 * providing information about value sign (positive or negative).
	 * 
	 * @version 1.0
	 * @author Igor Taranenko
	 */
	public class Time_counter_text_value
	{
		///// Fields public of-instance ===================================/////
		/** Contains text&nbsp;string representing time&nbsp;counter value. */
		public final String time_value_text;
		/** Time&nbsp;value sign. {@code true}&nbsp;&#0151;
		 * {@link #time_value_text} represents <u>positive</u> value;
		 * {@code false}&nbsp;&#0151; <u>negative</u> value. */
		public final boolean is_positive;
		
		
		///// Constructors default-access =================================/////
		/**
		 * @param time_value_text Time&nbsp;counter text value.
		 * 
		 * @param is_positive_value Time&nbsp;value sign.
		 * {@code true}&nbsp;&#0151; {@code time_value_text}&nbsp;argument
		 * represents <u>positive</u> value; {@code false}&nbsp;&#0151;
		 * <u>negative</u> value.
		 * 
		 * @exception NullPointerException {@code time_value_text}&nbsp;argument
		 * is&nbsp;{@code null}.
		 */
		private Time_counter_text_value(
				final String time_value_text, final boolean is_positive_value)
		{
			// String argument cannot be null
			if (time_value_text == null)
			{
				throw new NullPointerException(
						String.class.getName() + " argument is null");
			}
			
			this.time_value_text = time_value_text;
			this.is_positive = is_positive_value;
		}
	}
	
	
	///// Поля protected статические ======================================/////
	/** Кол-во дней в каждом из 12&#8209;ти месяцев.<br>
	 * <i>Примечания.</i>
	 * <ul><li>Счет начинается с нуля (т.е.&nbsp;индекс января&nbsp;-&nbsp;0).</li>
	 * <li>Кол&#8209;во дней в феврале&nbsp;&#0151; 28.</li></ul>
	 * <b>Важно!</b> Ссылается на
	 * {@link Collections#unmodifiableList(java.util.List)}. Попытка изменения
	 * контейнера приведет к исключению времени выполнения. */
	protected static final List<Integer> month_sizes;
	
	/** Настройки программы. */
	protected static final Settings settings;
	
	
	///// Fields default-access static ====================================/////
	/** Resource bundle representing <i>.properties</i> file which contains
	 * resources for time&nbsp;counters. */
	static ResourceBundle time_counter_resources;
	

	///// Поля private статические ========================================/////
	/** Логирует события данного класса. */
	private static final Logger logger;
	
	/** Содержит форматирующие последовательности (например, "%2d") для
	 * форматирования строки.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	private static final Map<Time_unit_name, String> format_conversions;
	
	/** Represents <i>app/tar/time_obj/time_counter_resources.properties</i>
	 * file's <i>"TUL_value_sign"</i>&nbsp;key&nbsp;group.<br>
	 * <b>Warning!</b> The&nbsp;container is <u>immutable</u>. An&nbsp;attempt
	 * to change its content results in runtime exception. */
	private static final Map<Time_unit_name, String> time_unit_signs;
	/** Represents <i>app/tar/time_obj/time_counter_resources.properties</i>
	 * file's <i>"TUL_short_name"</i>&nbsp;key&nbsp;group.<br>
	 * <b>Warning!</b> The&nbsp;container is <u>immutable</u>. An&nbsp;attempt
	 * to change its content results in runtime exception. */
	private static final Map<Time_unit_name, String> time_unit_short_names;
	/** Represents <i>app/tar/time_obj/time_counter_resources.properties</i>
	 * file's <i>"TUL_digits_only"</i>&nbsp;key&nbsp;group.<br>
	 * <b>Warning!</b> The&nbsp;container is <u>immutable</u>. An&nbsp;attempt
	 * to change its content results in runtime exception. */
	private static final Map<Time_unit_name, String> time_unit_digits_only;
	
	/** Кол&#8209;во разрешений для семафора {@link #semaphore}. */
	private static final int semaphore_permits;
	
	
	static
	{
		logger = Logger.getLogger(Time_counter.class.getName());
		settings = Settings.get_instance();
		time_counter_resources = settings.get_time_counter_resources();
		
		// Строки для инициализации контейнера "format_conversions"
		final String[] format_conversion_text = { "%,10d", "%2d", "%3d", "%2d",
				"%02d", "%02d" };
		/* Keys from "app/tar/time_obj/time_counter_resources.properties" file
		 * for "time_unit_signs" container */ 
		final String[] time_unit_sings_keys = { "TUL_value_sign.years",
				"TUL_value_sign.months", "TUL_value_sign.days",
				"TUL_value_sign.hours", "TUL_value_sign.minutes",
				"TUL_value_sign.seconds" };
		/* Keys from "app/tar/time_obj/time_counter_resources.properties" file
		 * for "time_unit_short_names" container */
		final String[] time_unit_short_names_keys = { "TUL_short_name.years",
				"TUL_short_name.months", "TUL_short_name.days",
				"TUL_short_name.hours", "TUL_short_name.minutes",
				"TUL_short_name.seconds" };
		/* Keys from "app/tar/time_obj/time_counter_resources.properties" file
		 * for "time_unit_digits_only" container */
		final String[] time_unit_digits_only_keys = { "TUL_digits_only.years",
				"TUL_digits_only.months", "TUL_digits_only.days",
				"TUL_digits_only.hours", "TUL_digits_only.minutes",
				"TUL_digits_only.seconds" };
		
		// Все элементы перечисления "Time_unit_name"
		final Time_unit_name[] time_unit_name_values = Time_unit_name.values();
		
		assert format_conversion_text.length == time_unit_name_values.length :
			"Array size with values doesn\'t match with "
				+ Time_unit_name.class.getName() + " enumeration constants quantity";
		assert time_unit_name_values.length == time_unit_sings_keys.length :
			"Array size with values doesn\'t match with "
				+ Time_unit_name.class.getName() + " enumeration constants quantity";
		assert time_unit_name_values.length == time_unit_short_names_keys.length :
			"Array size with values doesn\'t match with "
				+ Time_unit_name.class.getName() + " enumeration constants quantity";
		assert time_unit_name_values.length == time_unit_digits_only_keys.length :
			"Array size with values doesn\'t match with "
				+ Time_unit_name.class.getName() + " enumeration constants quantity";
		
		// Инициализатор "format_conversions"
		final Map<Time_unit_name, String> format_conversion_init =
				new EnumMap<>(Time_unit_name.class);
		// "time_unit_signs" container initializer
		final Map<Time_unit_name, String> time_unit_signs_init =
				new EnumMap<>(Time_unit_name.class);
		// "time_unit_short_names" container initializer
		final Map<Time_unit_name, String> time_unit_short_names_init =
				new EnumMap<>(Time_unit_name.class);
		// "time_unit_digits_only" container initializer
		final Map<Time_unit_name, String> time_unit_digits_only_init =
				new EnumMap<>(Time_unit_name.class);
		
		// Map containers initialization
		for (final Time_unit_name i : time_unit_name_values)
		{
			// Current enumeration constant ordinal value
			final int ordinal = i.ordinal();
			
			format_conversion_init.put(i, format_conversion_text[ordinal]);
			time_unit_signs_init.put(i, time_unit_sings_keys[ordinal]);
			time_unit_short_names_init.put(i, time_unit_short_names_keys[ordinal]);
			time_unit_digits_only_init.put(i, time_unit_digits_only_keys[ordinal]);
		}
		
		format_conversions = Collections.unmodifiableMap(format_conversion_init);
		time_unit_signs = Collections.unmodifiableMap(time_unit_signs_init);
		time_unit_short_names =
				Collections.unmodifiableMap(time_unit_short_names_init);
		time_unit_digits_only =
				Collections.unmodifiableMap(time_unit_digits_only_init);
		
		month_sizes = Collections.unmodifiableList(Arrays.asList(
				31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31));
		
		semaphore_permits = 4;
	}
	
	
	///// Поля public экземпляра ==========================================/////
	/** Порядковый номер, обозначающий положение данного счетчика времени при
	 * компоновке всех счетчиков времени в ГПИ. Должен изменяться при
	 * перемещении объекта {@link Time_counter} в другое место относительно
	 * остальных объектов {@link Time_counter}. Изменение порядкового номера
	 * необходимо для сортировки контейнера, содержащего все объекты
	 * {@link Time_counter}, перед записью в файл. Таким образом при запуске
	 * программы достигается сохранение порядка расположения счетчиков времени
	 * в ГПИ программы. */
	public transient int index_number;
	
	/** Режим работы экземпляра счетчика времени согласно перечислению
	 * {@link Mode}.
	 * 
	 * @serial Верифицируется расширяющим классом после десериализации.<br>
	 * <i>Условия проверки.</i>
	 * <ul><li><u>Не&nbsp;должен</u> быть {@code null}.</li>
	 * <li><u>Должен</u> соответствовать одному из возможных режимов для
	 * расширяющего класса.</li></ul>
	 * При несоответствии условиям проверки генерируется исключение
	 * {@link InvalidObjectException}. */
	public final Mode instance_mode;
	
	/** Designed for storing time&nbsp;counter explanation text, which user
	 * inputs in specialized GUI&nbsp;text&nbsp;field.
	 * @serial */
	public volatile String description_text;
	
	
	///// Поля protected экземпляра =======================================/////
	/** Contains time&nbsp;unit values according to {@link Time_unit_name}
	 * enumeration. */
	protected transient EnumMap<Time_unit_name, Long> time_unit_values;
	
	/** Флаг десериализации объекта. {@code true}&nbsp;&#0151; десериализация
	 * объекта прошла успешно; {@code false}&nbsp;&#0151; некоторые
	 * (<u>относительно некритические</u>) поля объекта не&nbsp;прошли
	 * валидацию и инициализированы значениями по&nbsp;умолчанию. */
	protected transient boolean deserialization_status;
	
	
	///// Поля private экземпляра =========================================/////
	/** Стиль отображения счетчика времени целиком для экземпляра этого класса.
	 * 
	 * @serial После десериализации <u>не&nbsp;должен</u> быть {@code null}.
	 * Если условие не&nbsp;соблюдается&nbsp;&#0151; присваивается значение
	 * по&nbsp;умолчанию. */
	private Time_display_style time_display_style;
	
	/** Class instance time&nbsp;units displaying&nbsp;range setting. Object
	 * represents array which contains <u>2&nbsp;elements</u>, where
	 * element&nbsp;{@code [0]} represents <u>biggest</u> possible displayed
	 * time&nbsp;value (is on the&nbsp;<u>left</u> side), and
	 * element&nbsp;{@code [1]} represents <u>lowest</u> possible displayed
	 * time&nbsp;value (is on the&nbsp;<u>right</u> side). These scopes are
	 * related to {@link Time_display_style#TDS_custom_strict} and
	 * {@link Time_display_style#TDS_increase_able} time&nbsp;counter's display
	 * styles.
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
	private Time_unit_name[] time_value_edges;
	
	/** Содержит стиль отображения названий единиц времени для экземпляра
	 * класса.
	 * 
	 * @serial После десериализации <u>не&nbsp;должен</u> быть {@code null}.
	 * Если условие не&nbsp;соблюдается&nbsp;&#0151; присваивается значение
	 * по&nbsp;умолчанию. */
	private Time_unit_layout time_unit_layout;
	
	/** Used for formatting {@link #time_counter_text} string. */
	private transient Formatter formatter;
	
	/** Synchronizes access to {@link #time_display_style},
	 * {@link #time_unit_layout}, {@link #time_counter_text} and
	 * {@link #is_positive} fields. */
	private transient Semaphore semaphore;
	/** Synchronizes access to {@link #time_value_edges} field. */
	private transient ReentrantLock time_value_edges_lock;
	/** Synchronizes access to {@link #time_value_listeners} and
	 * {@link #time_value_listeners_notifier}. */
	private transient ReentrantLock time_value_listeners_lock;
	/** Synchronizes access to {@link #time_elapsed_listeners}. */
	private transient ReentrantLock time_elapsed_listeners_lock;
	
	/** Contains text&nbsp;string representing time&nbsp;counter value. */
	private transient String time_counter_text;
	
	/** Time&nbsp;counter value sign (time&nbsp;value is represented by
	 * {@link #time_counter_text}). {@code true}&nbsp;&#0151; time&nbsp;value
	 * <u>is&nbsp;positive</u>; {@code false}&nbsp;&#0151;
	 * <u>is&nbsp;negative</u>.<br>
	 * <i>Note.</i> This field value depends on {@link Mode} in which extending
	 * class operates (see&nbsp;{@link #set_time_counter_value_sign(boolean)}
	 * method description).
	 * 
	 * @serial Deserialized value <u>is&nbsp;not</u> checked. If
	 * time&nbsp;counter mode is&nbsp;{@link Mode#M_stopwatch}, field
	 * <u>should&nbsp;be&nbsp;set</u> to {@code true} in extending class. */
	private boolean is_positive;
	
	/** Contains listeners subscribed for {@link #time_counter_text} changing
	 * event. */
	private transient ArrayList<Time_counter_text_listener> time_value_listeners;
	/** Contains listeners subscribed for <i>time elapsed event</i>. */
	private transient ArrayList<Time_elapsed_listener> time_elapsed_listeners;
	
	/** Notifies subscribed listeners contained in {@link #time_value_listeners}
	 * using separate thread for each notification to speed&nbsp;up performance. */
	private transient ThreadPoolExecutor time_value_listeners_notifier;
	

	///// Нестатический блок инициализации ================================/////
	{
		description_text = null;
		time_unit_values = new EnumMap<>(Time_unit_name.class);
		deserialization_status = true;
		semaphore = new Semaphore(semaphore_permits);
		time_value_edges_lock = new ReentrantLock();
		time_value_listeners_lock = new ReentrantLock();
		time_elapsed_listeners_lock = new ReentrantLock();
		is_positive = true;
		time_value_listeners = new ArrayList<>();
		time_elapsed_listeners = new ArrayList<>();
		time_value_listeners_notifier = new ThreadPoolExecutor(
				0, Integer.MAX_VALUE,
				0, TimeUnit.NANOSECONDS,
				new LinkedTransferQueue<>());
	}
	
	
	///// Конструкторы protected ==========================================/////
	/**
	 * This constructor takes time&nbsp;counter layout parameters from
	 * {@link Settings}&nbsp;object.
	 * 
	 * @param mode Mode in which this time&nbsp;counter runs.
	 * 
	 * @exception NullPointerException {@code mode} argument is
	 * {@code null}.
	 */
	protected Time_counter(final Mode mode)
	{
		// Argument cannot be null
		if (mode == null)
		{
			throw new NullPointerException(
					Mode.class.getName() + " argument is null");
		}
		
		this.instance_mode = mode;
		time_display_style = settings.get_time_display_style_setting();
		time_value_edges = settings.get_time_value_edges();
		time_unit_layout = settings.get_time_unit_layout_setting();
	}
	
	
	/**
	 * This constructor takes time&nbsp;counter layout settings from its
	 * parameters.<br>
	 * {@code leftmost_displayed_time_unit} and
	 * {@code rightmost_displayed_time_unit} arguments <u>can</u> be {@code null}
	 * <u>if {@link Time_display_style#TDS_if_reaches} or
	 * {@link Time_display_style#TDS_show_all} is passed as
	 * {@code time_display_style} argument</u>. In this case time&nbsp;unit
	 * display range is taken from {@link Settings} object.
	 * 
	 * @param mode Mode in which this time&nbsp;counter runs.
	 * 
	 * @param time_display_style Time&nbsp;counter display style.
	 * 
	 * @param leftmost_displayed_time_unit The&nbsp;leftmost time&nbsp;unit
	 * which will be displayed.
	 * 
	 * @param rightmost_displayed_time_unit The&nbsp;rightmost time&nbsp;unit
	 * which will be displayed.
	 * 
	 * @param time_unit_layout The&nbsp;way in which time&nbsp;units names will
	 * be displayed.
	 * 
	 * @exception NullPointerException If any passed argument (<u>except case
	 * described for {@code leftmost_displayed_time_unit} and
	 * {@code rightmost_displayed_time_unit}</u>) is {@code null}.
	 * 
	 * @exception IllegalArgumentException {@code leftmost_displayed_time_unit}
	 * argument <u>must contain greater</u> time&nbsp;unit than
	 * {@code rightmost_displayed_time_unit} argument, or be <u>equal</u> to it.<br>
	 * <i>Examples:</i>
	 * <ul><li>{@code leftmost_displayed_time_unit} containing
	 * {@code Time_unit_name.TUN_months} value and
	 * {@code rightmost_displayed_time_unit} containing
	 * {@code Time_unit_name.TUN_hours}&nbsp;<u>is&nbsp;right</u>;</li>
	 * <li>{@code leftmost_displayed_time_unit} containing
	 * {@code Time_unit_name.TUN_days} value and
	 * {@code rightmost_displayed_time_unit} with <u>the&nbsp;same</u>
	 * {@code Time_unit_name.TUN_days} value&nbsp; <u>is&nbsp;right</u>;</li>
	 * <li>{@code leftmost_displayed_time_unit} containing
	 * {@code Time_unit_name.TUN_days} value and
	 * {@code rightmost_displayed_time_unit} containing
	 * {@code Time_unit_name.TUN_years}&nbsp;<u>is&nbsp;wrong</u> (exception
	 * will be thrown).</li><ul>
	 */
	protected Time_counter(final Mode mode,
			final Time_display_style time_display_style,
			final Time_unit_name leftmost_displayed_time_unit,
			final Time_unit_name rightmost_displayed_time_unit,
			final Time_unit_layout time_unit_layout)
	{
		// This arguments cannot be null anyway
		if (mode == null || time_display_style == null || time_unit_layout == null)
		{
			throw new NullPointerException(
					"At least one of important arguments is null");
		}
		
		// If at least one of edge values is null
		if (leftmost_displayed_time_unit == null || rightmost_displayed_time_unit == null)
		{
			/* If time counter display style requires displayed time units range
			 * to be set while it is not */
			if (!time_display_style.equals(Time_display_style.TDS_if_reaches) &&
				!time_display_style.equals(Time_display_style.TDS_show_all))
			{
				throw new IllegalArgumentException(Time_unit_name.class.getName()
						+ " arguments cannot be null in this case");
			}
			
			time_value_edges = settings.get_time_value_edges();
		}
		else
		{
			/* If new displayed time unit range is incorrect as noted in this
			 * constructor's "IllegalArgumentException" description */
			if (leftmost_displayed_time_unit.compareTo(rightmost_displayed_time_unit) > 0)
			{
				throw new IllegalArgumentException("leftmost_displayed_time_unit"
						+ " argument enum value is grater than"
						+ " rightmost_displayed_time_unit argument enum value");
			}
			
			time_value_edges = new Time_unit_name[2];
			time_value_edges[0] = leftmost_displayed_time_unit;
			time_value_edges[1] = rightmost_displayed_time_unit;
		}
		
		this.instance_mode = mode;
		this.time_display_style = time_display_style;
		this.time_unit_layout = time_unit_layout;
	}

	
	///// Методы public экземпляра ========================================/////
	/**
	 * Sets display style for this time&nbsp;counter.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #build_time_string()};</li>
	 * <li>{@link #build_time_string(String)};</li>
	 * <li>{@link #get_time_counter_text_value()};</li>
	 * <li>{@link #set_time_counter_value_sign(boolean)};</li>
	 * <li>{@link #set_time_unit_layout(Time_unit_layout)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()}.</li></ul>
	 * 
	 * @param new_value New time&nbsp;counter display style.
	 * 
	 * @exception NullPointerException Passed argument is&nbsp;{@code null}.
	 */
	public void set_time_display_style(final Time_display_style new_value)
	{
		// Если параметром функции является null
		if (new_value == null)
		{
			throw new NullPointerException(
					Time_display_style.class.getName() + " argument is null");
		}
		
		try
		{
			semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_display_style = new_value;
		}
		finally
		{
			semaphore.release();
		}
	}
	
	
	/**
	 * @return This time&nbsp;counter display style.
	 */
	public Time_display_style get_time_display_style()
	{
		return time_display_style;
	}
	
	
	
	/**
	 * Sets new time&nbsp;units displaying range for time&nbsp;counter.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized with:
	 * <ul><li>{@link #build_time_string()};</li>
	 * <li>{@link #set_time_value_edges(Time_unit_name, Time_unit_name)}.</li></ul>
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
		/* If new displayed time unit range is incorrect as noted in this
		 * method's "IllegalArgumentException" description */
		if (left_edge.compareTo(right_edge) > 0)
		{
			throw new IllegalArgumentException("left_edge argument enum value is"
					+ " grater than right_edge argument enum value");
		}
		
		try
		{
			time_value_edges_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
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
	 * <i>Performance note.</i> Method contains synchronized sections.
	 * Synchronized with:
	 * <ul><li>{@link #build_time_string()};</li>
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
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
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
	 * Sets time&nbsp;units layout style for this time&nbsp;counter.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #build_time_string()};</li>
	 * <li>{@link #build_time_string(String)}</li>
	 * <li>{@link #build_time_string(String)};</li>
	 * <li>{@link #get_time_counter_text_value()};</li>
	 * <li>{@link #set_time_counter_value_sign(boolean)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()}.</li></ul>
	 * 
	 * @param layout_set New time&nbsp;units layout style.
	 * 
	 * @exception NullPointerException Passed argument is&nbsp;{@code null}.
	 */
	public void set_time_unit_layout(final Time_unit_layout layout_set)
	{
		// Если параметром метода является null
		if (layout_set == null)
		{
			throw new NullPointerException(
					Time_unit_layout.class.getName() + " argument is null");
		}
		
		try
		{
			semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_unit_layout = layout_set;
		}
		finally
		{
			semaphore.release();
		}
	}
	
	
	/**
	 * @return Time&nbsp;units layout style.
	 */
	public Time_unit_layout get_time_unit_layout()
	{
		return time_unit_layout;
	}
	
	
	/**
	 * Возвращает флаг десериализации объекта.
	 * 
	 * @return {@code true}&nbsp;&#0151; объект десериализован
	 * без&nbsp;некорректных значений полей. Также {@code true} будет возвращено
	 * объектом, который создан при&nbsp;использовании конструктора,
	 * а&nbsp;не&nbsp;путем десериализации. {@code false} будет возвращено
	 * в&nbsp;случае если некоторые (<u>относительно некритические</u>) поля
	 * объекта не&nbsp;прошли валидацию и инициализированы значениями
	 * по&nbsp;умолчанию.
	 */
	public boolean get_deserialization_status()
	{
		return deserialization_status;
	}
	
	
	/**
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #build_time_string()};</li>
	 * <li>{@link #build_time_string(String)};</li>
	 * <li>{@link #set_time_counter_value_sign(boolean)};</li>
	 * <li>{@link #set_time_display_style(Time_display_style)};</li>
	 * <li>{@link #set_time_unit_layout(Time_unit_layout)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()}.</li></ul>
	 * 
	 * @return Object representing time&nbsp;counter text&nbsp;value and its
	 * sign (positive or negative).
	 */
	public Time_counter_text_value get_time_counter_text_value()
	{
		try
		{
			semaphore.acquire(semaphore_permits);
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			return new Time_counter_text_value(time_counter_text, is_positive);
		}
		finally
		{
			semaphore.release(semaphore_permits);
		}
	}
	
	
	/**
	 * Adds specified {@code listener} to receive time&nbsp;counter's
	 * time&nbsp;value <i>text change event</i>. Same {@code listener} <u>can</u>
	 * be&nbsp;added multiple times.<br>
	 * <i>Notes.</i>
	 * <ul><li>It is recommended to unsubscribe listener (if
	 * it&nbsp;is&nbsp;referenced anywhere else) using
	 * {@link #remove_Time_counter_text_listener(Time_counter_text_listener)}
	 * when there&nbsp;is no&nbsp;need to receive such event. Such action
	 * reduces resource usage and prevents resource leaks.</li>
	 * <li>However when removing the&nbsp;time&nbsp;counter object from
	 * {@link Modified_ArrayList}, obtained by
	 * {@link Time_counter_control#get_time_counters()} method, <u>all</u> its
	 * listeners removed automatically.</li></ul>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #remove_Time_counter_text_listener(Time_counter_text_listener)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()};</li>
	 * <li>{@link #shutdown()}.</li></ul>
	 * 
	 * @param listener Listener to be subscribed on event.
	 * 
	 * @exception NullPointerException {@code listener} argument is {@code null}.
	 */
	public void add_Time_counter_text_listener(
			final Time_counter_text_listener listener)
	{
		// Argument cannot be null
		if (listener == null)
		{
			throw new NullPointerException(
					Time_counter_text_listener.class.getName() + " argument is null");
		}
		
		try
		{
			time_value_listeners_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_value_listeners.add(listener);
			time_value_listeners_notifier.setCorePoolSize(
					time_value_listeners_notifier.getCorePoolSize() + 1);
		}
		finally
		{
			time_value_listeners_lock.unlock();
		}
	}
	
	
	/**
	 * Removes <u>first occurrence</u> of specified {@code listener} argument
	 * <u>if such present</u>.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #add_Time_counter_text_listener(Time_counter_text_listener)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()};</li>
	 * <li>{@link #shutdown()}.</li></ul>
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
	public boolean remove_Time_counter_text_listener(
			final Time_counter_text_listener listener)
	{
		// Argument cannot be null
		if (listener == null)
		{
			throw new NullPointerException(
					Time_counter_text_listener.class.getName() + " argument is null");
		}
		
		try
		{
			time_value_listeners_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			// true - given "listener" is successfully removed; false - otherwise
			final boolean is_removed = time_value_listeners.remove(listener);
			
			// If given "listener" was unsubscribed from event notifying
			if (is_removed)
			{
				time_value_listeners_notifier.setCorePoolSize(
						time_value_listeners_notifier.getCorePoolSize() - 1);
			}
			
			return is_removed;
		}
		finally
		{
			time_value_listeners_lock.unlock();
		}
	}
	
	
	/**
	 * Adds specified {@code listener} to receive <i>time elapsed event</i>.
	 * Same {@code listener} <u>can</u> be&nbsp;added multiple times.<br>
	 * <i>Notes.</i>
	 * <ul><li>It is recommended to unsubscribe listener (if
	 * it&nbsp;is&nbsp;referenced anywhere else) using
	 * {@link #remove_Time_elapsed_listener(Time_elapsed_listener)} when
	 * there&nbsp;is no&nbsp;need to receive such event. Such action reduces
	 * resource usage and prevents resource leaks.</li>
	 * <li>However when removing the&nbsp;time&nbsp;counter object from
	 * {@link Modified_ArrayList}, obtained by
	 * {@link Time_counter_control#get_time_counters()} method, <u>all</u> its
	 * listeners removed automatically.</li></ul>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #remove_Time_elapsed_listener(Time_elapsed_listener)};</li>
	 * <li>{@link #notify_time_elapsed_listeners()};</li>
	 * <li>{@link #shutdown()}.</li></ul>
	 * 
	 * @param listener Listener to be subscribed on event.
	 * 
	 * @exception NullPointerException {@code listener} argument is {@code null}.
	 */
	public void add_Time_elapsed_listener(final Time_elapsed_listener listener)
	{
		// The argument cannot be null
		if (listener == null)
		{
			throw new NullPointerException(
					Time_elapsed_listener.class.getName() + " argument is null");
		}
		
		try
		{
			time_elapsed_listeners_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stach trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_elapsed_listeners.add(listener);
		}
		finally
		{
			time_elapsed_listeners_lock.unlock();
		}
	}
	
	
	/**
	 * Removes <u>first occurrence</u> of specified {@code listener} argument
	 * <u>if such present</u>.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #add_Time_elapsed_listener(Time_elapsed_listener)};</li>
	 * <li>{@link #notify_time_elapsed_listeners()};</li>
	 * <li>{@link #shutdown()}.</li></ul>
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
	public boolean remove_Time_elapsed_listener(final Time_elapsed_listener listener)
	{
		// The argument cannot be null
		if (listener == null)
		{
			throw new NullPointerException(
					Time_elapsed_listener.class.getName() + " argument is null");
		}
		
		try
		{
			time_elapsed_listeners_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			return time_elapsed_listeners.remove(listener);
		}
		finally
		{
			time_elapsed_listeners_lock.unlock();
		}
	}
	
	
	/**
	 * Releases all resources (threads, listeners). Is&nbsp;necessary when this
	 * instance is&nbsp;not&nbsp;needed anymore.
	 * This method is invoked in {@link Modified_ArrayList}
	 * container&nbsp;methods related&nbsp;to removing object(&#8209;s).
	 * Such&nbsp;container exists in {@link Time_counter_control} object and can
	 * be obtained by calling {@link Time_counter_control#get_time_counters()}.
	 * So there&nbsp;is <u>no&nbsp;need</u> to call this method explicitly
	 * before removing the&nbsp;instance from mentioned container. However,
	 * calling this method more than once <u>is&nbsp;not</u> harmful.<br>
	 * <b>Warning!</b> Object using after calling this method
	 * <u>is&nbsp;prohibited</u>. Its methods behavior <u>is&nbsp;undefined</u>
	 * except next ones:
	 * <ul><li><i>this</i> method;</li>
	 * <li>{@link #build_time_string()};</li>
	 * <li>{@link #notify_time_counter_text_listeners()}.</li></ul>
	 * Two last listed methods are used in
	 * {@link Settings#set_locale_setting(app.tar.time_obj.Settings.Locale_setting)} and
	 * <u>are&nbsp;guaranteed</u> not&nbsp;to cause any exceptions when invoked
	 * after this method calling.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #add_Time_counter_text_listener(Time_counter_text_listener)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()};</li>
	 * <li>{@link #remove_Time_counter_text_listener(Time_counter_text_listener)};</li>
	 * <li>{@link #add_Time_elapsed_listener(Time_elapsed_listener)};</li>
	 * <li>{@link #notify_time_elapsed_listeners()};</li>
	 * <li>{@link #remove_Time_elapsed_listener(Time_elapsed_listener)}.</li></ul>
	 */
	public void shutdown()
	{
		time_value_listeners_lock.lock();
		
		try
		{
			time_value_listeners.clear();
			time_value_listeners_notifier.shutdown();
			
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						time_value_listeners_notifier.awaitTermination(
								5, TimeUnit.SECONDS);
					}
					catch (final InterruptedException exc)
					{
						logger.log(Level.INFO,
								"Thread interrupts. Exception stack trace:", exc);
						Thread.currentThread().interrupt();
					}
					finally
					{
						/* If ThreadPoolExecutor hasn't shutdown during waiting
						 * time */
						if (!time_value_listeners_notifier.isTerminated())
						{
							logger.log(Level.WARNING,
									"Forcible " + ThreadPoolExecutor.class.getName()
											+ " termination due\u00A0to long waiting");
							time_value_listeners_notifier.shutdownNow();
						}
					}
				}
			}).start();
		}
		finally
		{
			time_value_listeners_lock.unlock();
		}
		
		time_elapsed_listeners_lock.lock();
		
		try
		{
			time_elapsed_listeners.clear();
		}
		finally
		{
			time_elapsed_listeners_lock.unlock();
		}
	}

	
	///// Методы protected экземпляра =====================================/////
	/**
	 * Formats text&nbsp;string with time&nbsp;counter value (can be obtained
	 * via {@link #get_time_counter_text_value()}).<br>
	 * <i>Performance note.</i> Contains synchronized code sections. Synchronized
	 * with:
	 * <ul><li>{@link #get_time_value_edges()};</li>
	 * <li>{@link #set_time_value_edges(Time_unit_name, Time_unit_name)};</li>
	 * <li>{@link #build_time_string(String)};</li>
	 * <li>{@link #get_time_counter_text_value()};</li>
	 * <li>{@link #set_time_counter_value_sign(boolean)};</li>
	 * <li>{@link #set_time_display_style(Time_display_style)};</li>
	 * <li>{@link #set_time_unit_layout(Time_unit_layout)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()}.</li></ul>
	 */
	protected final void build_time_string()
	{
		try
		{
			semaphore.acquire(semaphore_permits);
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
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
				logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
				Thread.currentThread().interrupt();
			}
			
			try
			{
				formatter = new Formatter();
				
				// Если выбран строгий диапазон отображаемых единиц времени
				if (time_display_style.equals(Time_display_style.TDS_custom_strict))
				{
					formatter.format(time_counter_resources.getString(
							"strict_display_mode_mark"));
				}
				
				// Если значение отрицательное - в начале ставится знак "минус"
				if (!is_positive)
				{
					formatter.format("\u2012");
				}
				
				/* Если в настройках установлено отображение единиц времени,
				 * которые "имеют вес" (т.е. значение которых достигнуто) */
				if (time_display_style.equals(Time_display_style.TDS_if_reaches))
				{
					/* Формирование строки для форматирования, содержащей
					 * отображаемые единицы времени, происходит от больших
					 * единиц времени к меньшим. Как только значение одной из
					 * единиц времени оказывается значащим (т.е. отличным от
					 * нуля), - все меньшие единицы времени должны отображаться
					 * в любом случае. true - значащая единица времени
					 * достигнута; false - нет */
					boolean value_reached = false;
					
					/* Формирование строки для форматирования, содержащей
					 * отображаемые единицы времени */
					for (final Time_unit_name i : Time_unit_name.values())
					{
						/* If meaningful time value is already reached in
						 * previous iterations OR it's reached in this
						 * iteration OR ... */
						if (value_reached || time_unit_values.get(i) != 0 ||
								/* this is seconds time unit (must be shown
								 * anyway as the smallest time unit) */
								i.equals(Time_unit_name.TUN_seconds))
						{
							value_reached = true;
							format(i);
						}
					}
				}
				// Если в настройках установлено отображение всех единиц времени
				else if (time_display_style.equals(Time_display_style.TDS_show_all))
				{
					/* Формирование строки для форматирования, содержащей
					 * отображаемые единицы времени */
					for (final Time_unit_name i : Time_unit_name.values())
					{
						format(i);
					}
				}
				/* Если в настройках установлен конкретный диапазон отображаемых
				 * единиц времени (строгий ИЛИ нестрогий) */
				else if (time_display_style.equals(Time_display_style.TDS_custom_strict) ||
						time_display_style.equals(Time_display_style.TDS_increase_able))
				{
					/* true - если установлен НЕСТРОГИЙ диапазон отображаемых
					 * единиц времени; false - установлен СТРОГИЙ диапазон */
					final boolean increase_able_is_set = time_display_style.equals(
								Time_display_style.TDS_increase_able);
					/* Time string formatting is performed in direction from
					 * the biggest time unit to the smallest one. So if bigger
					 * time unit has value (i.e. is not 0), smaller ones must be
					 * shown anyway. It concerns only the NON STRICT time units
					 * display style ("Time_display_style.TDS_increase_able").
					 * true - non zero time unit value is found (even if it's
					 * bigger than set time units displaying range); false -
					 * otherwise */
					boolean must_be_shown = false;
					
					/* Формирование строки для форматирования, содержащей
					 * отображаемые единицы времени */
					for (final Time_unit_name i : Time_unit_name.values())
					{
						/* Результат сравнения именованной константы текущей
						 * итерации и крайней правой отображаемой единицы
						 * (наименьшей отображаемой единицы) времени */
						final int compare_to_rightmost =
								i.compareTo(time_value_edges[1]);
						
						// If (time value is in set displaying range) OR ...
						if ((i.compareTo(time_value_edges[0]) >= 0 && compare_to_rightmost <= 0) ||
								/* ... (bigger than set displaying range time values
								 * can be shown, if time value is greater than
								 * set displaying range AND current iteration
								 * time unit is bigger or equals to
								 * the rightmost possible displayed time unit
								 * AND ... */
								(increase_able_is_set && compare_to_rightmost <= 0 &&
								/* ... (in previous iterations nonzero
								 * time value occurred OR it's occurred in this
								 * iteration)) */
								(must_be_shown || time_unit_values.get(i) != 0)))
						{
							must_be_shown = true;
							format(i);
						}
					}
				}
				
				time_counter_text = formatter.toString();
				formatter.close();
			}
			finally
			{
				time_value_edges_lock.unlock();
			}
		}
		finally
		{
			semaphore.release(semaphore_permits);
		}
	}
	
	
	/**
	 * Sets specified {@code message} instead of time&nbsp;counter value.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #build_time_string()};</li>
	 * <li>{@link #get_time_counter_text_value()};</li>
	 * <li>{@link #set_time_counter_value_sign(boolean)};</li>
	 * <li>{@link #set_time_display_style(Time_display_style)};</li>
	 * <li>{@link #set_time_unit_layout(Time_unit_layout)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()}.</li></ul>
	 * 
	 * @param message Text to set instead of time&nbsp;counter value.
	 */
	protected final void build_time_string(final String message)
	{
		try
		{
			semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_counter_text = message;
		}
		finally
		{
			semaphore.release();
		}
	}
	
	
	/**
	 * Notifies listeners which implement {@link Time_counter_text_listener}
	 * interface about {@link #time_counter_text} value changed.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #build_time_string()};</li>
	 * <li>{@link #build_time_string(String)};</li>
	 * <li>{@link #get_time_counter_text_value()};</li>
	 * <li>{@link #set_time_counter_value_sign(boolean)};</li>
	 * <li>{@link #set_time_display_style(Time_display_style)};</li>
	 * <li>{@link #set_time_unit_layout(Time_unit_layout)};</li>
	 * <li>{@link #add_Time_counter_text_listener(Time_counter_text_listener)};</li>
	 * <li>{@link #remove_Time_counter_text_listener(Time_counter_text_listener)};</li>
	 * <li>{@link #shutdown()}.</li></ul>
	 */
	protected final void notify_time_counter_text_listeners()
	{
		try
		{
			semaphore.acquire(semaphore_permits);
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
				time_value_listeners_lock.lockInterruptibly();
			}
			catch (final InterruptedException exc)
			{
				logger.log(Level.INFO,
						"Thread interrupts. Exception stack trace:", exc);
				Thread.currentThread().interrupt();
			}
			
			try
			{
				// This object's reference to be passed with event
				final Time_counter instance = this;
				
				// Listeners notification
				for (final Time_counter_text_listener i : time_value_listeners)
				{
					time_value_listeners_notifier.execute(new Runnable()
					{
						@Override
						public void run()
						{
							i.time_counter_text_changed(
									new Time_counter_event<>(instance),
									new Time_counter_text_value(
											time_counter_text, is_positive));
						}
					});
				}
			}
			finally
			{
				time_value_listeners_lock.unlock();
			}
		}
		finally
		{
			semaphore.release(semaphore_permits);
		}
	}
	
	
	/**
	 * Sets time&nbsp;counter value sign (positive or negative). Extending
	 * classes must use value sign setting according to {@link Mode} in which
	 * they are running.
	 * <ul><li><i>{@link Mode#M_stopwatch}.</i> Time&nbsp;values <u>cannot be
	 * negative</u> ({@code true} always need to be passed).</li>
	 * <li><i>{@link Mode#M_countdown}.</i> {@code true} need to be passed
	 * before time&nbsp;value reaches zero (i.e.&nbsp;while time&nbsp;value
	 * <u>is&nbsp;positive</u>). After reaching zero {@code false} need to be
	 * passed (i.e.&nbsp;when time&nbsp;value <u>is&nbsp;negative</u>).</li>
	 * <li><i>{@link Mode#M_elapsed_from}.</i> {@code false} need to be passed
	 * while current time&nbsp;value <u>is&nbsp;before</u> designated
	 * time&nbsp;point (i.e.time&nbsp;value <u>is&nbsp;negative</u> because it
	 * <u>is&nbsp;before</u> the&nbsp;designated time&nbsp;point, elapsed time
	 * from which need to be calculated). {@code true} need to be passed when
	 * current time&nbsp;value <u>is&nbsp;after</u> designated time&nbsp;point
	 * (i.e.&nbsp;time&nbsp;value <u>is&nbsp;positive</u>).</li>
	 * <li><i>{@link Mode#M_remains_till}.</i> {@code true} need to be passed
	 * while current time&nbsp;value <u>has&nbsp;not</u> yet reached
	 * the&nbsp;designated time&nbsp;point (i.e.&nbsp;time&nbsp;value
	 * <u>is&nbsp;positive</u> because it actually shows time remaining to
	 * the&nbsp;designated time&nbsp;point). {@code false} need to be passed
	 * when current time&nbsp;value <u>is&nbsp;after</u> designated
	 * time&nbsp;point (i.e.&nbsp;time&nbsp;value <u>is&nbsp;negative</u>
	 * because designated time&nbsp;point <u>is&nbsp;passed</u>).</li></ul>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #build_time_string()};</li>
	 * <li>{@link #build_time_string(String)};</li>
	 * <li>{@link #get_time_counter_text_value()};</li>
	 * <li>{@link #set_time_display_style(Time_display_style)};</li>
	 * <li>{@link #set_time_unit_layout(Time_unit_layout)};</li>
	 * <li>{@link #notify_time_counter_text_listeners()}.</li></ul>
	 * 
	 * @param is_positive {@code true}&nbsp;&#0151; time&nbsp;value
	 * <u>is&nbsp;positive</u>; {@code false}&nbsp;&#0151;
	 * <u>is&nbsp;negative</u>.
	 * 
	 * @return Previous time&nbsp;counter value sign: {@code true}&nbsp;&#0151;
	 * <u>positive</u>; {@code false}&nbsp;&#0151; <u>negative</u>.
	 */
	protected final boolean set_time_counter_value_sign(
			final boolean is_positive)
	{
		try
		{
			semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			// Previous time value sign to return
			final boolean previous_value = this.is_positive;
			
			this.is_positive = is_positive;
			
			return previous_value;
		}
		finally
		{
			semaphore.release();
		}
	}
	
	
	/**
	 * @return Time&nbsp;counter value&nbsp;sign. {@code true}&nbsp;&#0151;
	 * time&nbsp;value <u>is&nbsp;positive</u>; {@code false}&nbsp;&#0151;
	 * <u>is&nbsp;negative</u>. Returning&nbsp;value depends
	 * on {@link Mode} in which time&nbsp;counter is running. <u>Extending
	 * classes must set value&nbsp;sign correctly</u>
	 * (see&nbsp;{@link #set_time_counter_value_sign(boolean)} method
	 * description).
	 * <ul><li><i>{@link Mode#M_stopwatch}.</i> Time&nbsp;value
	 * <u>is&nbsp;always</u> positive, so {@code true} is&nbsp;always returned.</li>
	 * <li><i>{@link Mode#M_countdown}.</i> {@code true} is&nbsp;returned before
	 * time&nbsp;value reaches zero (i.e.&nbsp;time&nbsp;value
	 * <u>is&nbsp;positive</u>). After reaching zero {@code false}
	 * is&nbsp;returned (i.e.&nbsp;time&nbsp;value <u>is&nbsp;negative</u>).</li>
	 * <li><i>{@link Mode#M_elapsed_from}.</i> {@code false} is&nbsp;returned
	 * while current time&nbsp;value <u>is&nbsp;before</u> designated
	 * time&nbsp;point (i.e.&nbsp;time&nbsp;value <u>is&nbsp;negative</u>
	 * because it <u>is&nbsp;before</u> the&nbsp;designated time&nbsp;point,
	 * elapsed time from which need to be calculated). {@code true}
	 * is&nbsp;returned when current time&nbsp;value <u>is&nbsp;after</u>
	 * designated time&nbsp;point (i.e.&nbsp;time&nbsp;value
	 * <u>is&nbsp;positive</u>).</li>
	 * <li><i>{@link Mode#M_remains_till}.</i> {@code true} is&nbsp;returned
	 * while current time&nbsp;value <u>has&nbsp;not</u> yet reached
	 * the&nbsp;designated time&nbsp;point (i.e.&nbsp;time&nbsp;value
	 * <u>is&nbsp;positive</u> because it actually shows time remaining to
	 * the&nbsp;designated time&nbsp;point). {@code false} is&nbsp;returned when
	 * current time&nbsp;value <u>is&nbsp;after</u> designated time&nbsp;point
	 * (i.e.&nbsp;time&nbsp;value <u>is&nbsp;negative</u> because designated
	 * time&nbsp;point <u>is&nbsp;passed</u>).</li></ul>
	 */
	protected final boolean is_positive_value()
	{
		return is_positive;
	}
	
	
	/**
	 * Notifies listeners which implement {@link Time_elapsed_listener}
	 * interface about <i>time elapsed event</i>.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #add_Time_elapsed_listener(Time_elapsed_listener)};</li>
	 * <li>{@link #remove_Time_elapsed_listener(Time_elapsed_listener)};</li>
	 * <li>{@link #shutdown()}.</li></ul>
	 */
	protected void notify_time_elapsed_listeners()
	{
		try
		{
			time_elapsed_listeners_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		/* Time elapsed listeners notifier to notify each listener in a separate
		 * thread */
		ThreadPoolExecutor notifier = null;
		
		try
		{
			final int time_elapsed_listeners_quantity =
					time_elapsed_listeners.size();
			
			// If there is no listeners to notify
			if (time_elapsed_listeners_quantity == 0)
			{
				return;
			}
			
			notifier = new ThreadPoolExecutor(
					time_elapsed_listeners_quantity,
					time_elapsed_listeners_quantity,
					0, TimeUnit.NANOSECONDS,
					new ArrayBlockingQueue<>(time_elapsed_listeners_quantity));
			notifier.prestartAllCoreThreads();
			
			// Reference to THIS object to send as event object
			final Time_counter instance = this;
			
			// Time elapsed listeners notifying (each in separate thread)
			for (final Time_elapsed_listener i : time_elapsed_listeners)
			{
				notifier.execute(new Runnable()
				{
					@Override
					public void run()
					{
						i.time_counter_reached_zero(
								new Time_counter_event<Time_counter>(instance));
					}
				});
			}
		}
		finally
		{
			time_elapsed_listeners_lock.unlock();
			
			// Shutdown executor
			if (notifier != null)
			{
				notifier.setCorePoolSize(0);
			}
		}
	}
	
	
	///// Методы private экземпляра =======================================/////
	/**
	 * Auxiliary for {@link #build_time_string()} method. Formats string
	 * according to passed {@code time_unit} argument and appends it to
	 * {@link #formatter}.
	 * 
	 * @param time_unit Time&nbsp;unit which has to be displayed.
	 */
	private void format(final Time_unit_name time_unit)
	{
		formatter.format(format_conversions.get(time_unit),
				time_unit_values.get(time_unit));
		
		switch (time_unit_layout)
		{
		case TUL_value_sign:
			formatter.format(time_counter_resources.getString(
					time_unit_signs.get(time_unit)));
			
			break;
			
		case TUL_short_name:
			formatter.format(time_counter_resources.getString(
					time_unit_short_names.get(time_unit)));
			
			break;
			
		case TUL_full_name:
			formatter.format(Time_unit_full_name_resource_provider.get_name(
					time_unit, time_unit_values.get(time_unit)));
			
			break;
			
		case TUL_digits_only:
			formatter.format(time_counter_resources.getString(
					time_unit_digits_only.get(time_unit)));
			
			break;
		
		default:
			throw new EnumConstantNotPresentException(
					Time_unit_layout.class, time_unit_layout.name());
		}
	}
	
	
	/**
	 * Отвечает за десериализацию, создание безопасных копий критических полей
	 * и их проверку.
	 * 
	 * @param input_stream Входной поток, из которого извлекается экземпляр
	 * класса.
	 * 
	 * @throws IOException Ошибка в работе входящего потока.
	 * 
	 * @throws ClassNotFoundException Класс сериализованного объекта
	 * не&nbsp;определен.
	 */
	private void readObject(final ObjectInputStream input_stream)
			throws IOException, ClassNotFoundException
	{
		input_stream.defaultReadObject();
		deserialization_status = true;
		
		try
		{
			time_value_edges = time_value_edges.clone();
			
			// Если размер массива не равен 2-м элементам ИЛИ ...
			if (time_value_edges.length != 2 ||
					/* ... значение крайней левой отображаемой единицы времени
					 * больше правой */
					time_value_edges[0].compareTo(time_value_edges[1]) > 0)
			{
				time_value_edges = settings.get_time_value_edges();
				deserialization_status = false;
			}
		}
		catch (final NullPointerException exc)
		{
			time_value_edges = settings.get_time_value_edges();
			logger.log(Level.WARNING, "Deserialized object\'s noncritical field"
					+ " is null. Deserialized object can be used. Exception stack trace:", exc);
			deserialization_status = false;
		}
		
		// Если поле оказалось пустой ссылкой
		if (time_display_style == null)
		{
			time_display_style = settings.get_time_display_style_setting();
			logger.log(Level.WARNING, "Deserialized object\'s noncritical field of "
					+ Time_display_style.class.getName() + " type is null."
							+ " Deserialized object can be used.");
			deserialization_status = false;
		}
		
		// Если поле оказалось пустой ссылкой
		if (time_unit_layout == null)
		{
			time_unit_layout = settings.get_time_unit_layout_setting();
			logger.log(Level.WARNING, "Deserialized object\'s noncritical field of "
					+ Time_unit_layout.class.getName() + " type is null."
							+ " Deserialized object can be used.");
			deserialization_status = false;
		}
		
		semaphore = new Semaphore(semaphore_permits);
		time_value_edges_lock = new ReentrantLock();
		time_value_listeners_lock = new ReentrantLock();
		time_elapsed_listeners_lock = new ReentrantLock();
		time_counter_text = "";
		time_unit_values = new EnumMap<>(Time_unit_name.class);
		time_value_listeners = new ArrayList<>();
		time_elapsed_listeners = new ArrayList<>();
		time_value_listeners_notifier = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0,
				TimeUnit.NANOSECONDS, new LinkedTransferQueue<>());
	}
}
