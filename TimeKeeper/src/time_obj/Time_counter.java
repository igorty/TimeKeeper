package time_obj;

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
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import time_obj.events.Time_counter_event;
import time_obj.events.Time_counter_text_listener;


// TODO: ? Объявить абстрактные методы
/**
 * Инкапсулирует основные свойства счетчика времени.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public abstract class Time_counter implements Serializable
{
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
	
	
	///// Поля public статические =========================================/////
	/** Time&nbsp;units template names (symbolic, short, full and empty),
	 * distributed to groups according to named&nbsp;constants
	 * {@link Time_unit_layout#TUL_value_sign},
	 * {@link Time_unit_layout#TUL_short_name},
	 * {@link Time_unit_layout#TUL_full_name} and
	 * {@link Time_unit_layout#TUL_digits_only} from
	 * {@link Time_unit_layout} enumeration.<br>
	 * {@link EnumMap#get(Object)} method returns
	 * {@code Map<}{@link Time_unit_name}{@code , }{@link String}{@code >}
	 * container containing time&nbsp;units names for specified group.<br>
	 * <b>Important!</b> <u>This container</u> and all <u>nested</u> in it
	 * {@code Map<}{@link Time_unit_name}{@code ,}{@link String}{@code >}
	 * containers are <u>immutable</u>. An&nbsp;attempt to change containers
	 * content results in runtime exception. */
	public static final Map<Time_unit_layout, Map<Time_unit_name, String>>
			time_unit_texts;
	
	/** Шаблонный текст режима отображения
	 * {@link Time_display_style#TDS_custom_strict}, указываемый перед счетчиком
	 * времени. */
	public static final String strict_display_mode_text;
	
	
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
	

	///// Поля private статические ========================================/////
	/** Логирует события данного класса. */
	private static final Logger logger;
	
	/** Содержит форматирующие последовательности (например, "%2d") для
	 * форматирования строки.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	private static final Map<Time_unit_name, String> format_conversions;
	
	/** Кол&#8209;во разрешений для семафора {@link #semaphore}. */
	private static final int semaphore_permits;
	
	
	static
	{
		logger = Logger.getLogger(Time_counter.class.getName());
		strict_display_mode_text = "CUT  ";
		
		// TODO: Заполнить нулями пустующие места в минутах и секундах 
		// Строки для инициализации контейнера "format_conversions"
		final String[] format_conversion_text = { "%,10d", "%2d", "%3d", "%2d",
				"%2d", "%2d" };
		// Все элементы перечисления "Time_unit_name"
		final Time_unit_name[] time_unit_name_values = Time_unit_name.values();
		
		assert format_conversion_text.length == time_unit_name_values.length :
			"Array size with values doesn\'t match with "
				+ Time_unit_name.class.getName() + " elements quantity";
		
		// Инициализатор "format_conversions"
		final Map<Time_unit_name, String> format_conversion_init =
				new EnumMap<>(Time_unit_name.class);
		
		/* Инициализация контейнера "format_conversion_init" форматирующими
		 * последовательностями для поля типа Formatter */
		for (final Time_unit_name i : time_unit_name_values)
		{
			format_conversion_init.put(i, format_conversion_text[i.ordinal()]);
		}
		
		format_conversions = Collections.unmodifiableMap(format_conversion_init);
		month_sizes = Collections.unmodifiableList(Arrays.asList(
				31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31));
		
		// Значения для контейнера "time_unit_texts"
		final String[][] time_unit_text_values = {
				{ "y  ", "m  ", "d  ", ":", "\'", "\"  " },
				{ " yr  ", " mn  ", " d  ", " hr  ", " min  ", " sec  " },
				{ " year", " month", " day", " hour", " minute", " second" },
				{ "  ", "  ", "   ", " ", " ", "  " } };

		assert time_unit_text_values[0].length == time_unit_name_values.length &&
				time_unit_text_values[1].length == time_unit_name_values.length &&
				time_unit_text_values[2].length == time_unit_name_values.length &&
				time_unit_text_values[3].length == time_unit_name_values.length :
					"Some subarray\'s length doesn\'t match with "
						+ Time_unit_name.class.getName() + " elements quantity";
		
		/* Четыре инициализатора для "time_unit_text_init". Каждый из них
		 * отвечает за стиль названия единиц времени согласно перечислению
		 * "Time_unit_layout" */
		final Map<Time_unit_name, String> time_unit_text_init0 =
				new EnumMap<>(Time_unit_name.class),
				time_unit_text_init1 = new EnumMap<>(Time_unit_name.class),
				time_unit_text_init2 = new EnumMap<>(Time_unit_name.class),
				time_unit_text_init3 = new EnumMap<>(Time_unit_name.class);
		
		// Присваивание значений time_unit_text_init'ам
		for (final Time_unit_name i : time_unit_name_values)
		{
			time_unit_text_init0.put(i,
					time_unit_text_values[Time_unit_layout.TUL_value_sign.ordinal()][i.ordinal()]);
			time_unit_text_init1.put(i,
					time_unit_text_values[Time_unit_layout.TUL_short_name.ordinal()][i.ordinal()]);
			time_unit_text_init2.put(i,
					time_unit_text_values[Time_unit_layout.TUL_full_name.ordinal()][i.ordinal()]);
			time_unit_text_init3.put(i,
					time_unit_text_values[Time_unit_layout.TUL_digits_only.ordinal()][i.ordinal()]);
		}
		
		// Инициализатор для "time_unit_texts"
		final Map<Time_unit_layout, Map<Time_unit_name, String>> time_unit_text_init =
				new EnumMap<>(Time_unit_layout.class);
		
		time_unit_text_init.put(Time_unit_layout.TUL_value_sign,
				Collections.unmodifiableMap(time_unit_text_init0));
		time_unit_text_init.put(Time_unit_layout.TUL_short_name,
				Collections.unmodifiableMap(time_unit_text_init1));
		time_unit_text_init.put(Time_unit_layout.TUL_full_name,
				Collections.unmodifiableMap(time_unit_text_init2));
		time_unit_text_init.put(Time_unit_layout.TUL_digits_only,
				Collections.unmodifiableMap(time_unit_text_init3));
		time_unit_texts = Collections.unmodifiableMap(time_unit_text_init);
		
		semaphore_permits = 4;
		settings = Settings.get_instance();
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
	/** Содержит значения единиц времени согласно перечислению
	 * {@link time_obj.Time_unit_name}. */
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
	
	/** Содержит названия крайних отображаемых единиц времени для экземпляра
	 * этого класса согласно перечислению
	 * {@link time_obj.Time_unit_name}. {@code [0]}&nbsp;&#0151; крайняя левая
	 * отображаемая единица (наибольшая отображаемая единица времени);
	 * {@code [1]}&nbsp;&#0151; крайняя правая отображаемая единица (наименьшая
	 * отображаемая единица времени).<br>
	 * Данные рамки имеют отношение к стилю отображения счетчика времени
	 * целиком согласно именованным константам
	 * {@link time_obj.Time_display_style#TDS_custom_strict} и
	 * {@link time_obj.Time_display_style#TDS_increase_able} в
	 * перечислении {@link time_obj.Time_display_style}.
	 * 
	 * @serial После десериализации переприсваивается копия и осуществляется
	 * верификация.<br>
	 * <i>Условия верификации.</i>
	 * <ul><li><u>Не&nbsp;должен</u> быть {@code null}.</li>
	 * <li><u>Не&nbsp;должен</u> содержать {@code null}.</li>
	 * <li>Кол&#8209;во элементов массива <u>должно</u> быть&nbsp;2.</li>
	 * <li>Метод {@link Enum#compareTo(Enum)}, вызванный элементом с
	 * индексом&nbsp;{@code [0]} <u>должен</u> возвращать <u>отрицательное или
	 * нулевое значение</u>.</li></ul>
	 * При несоответствии требованиям устанавливаются значения
	 * по&nbsp;умолчанию. */
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
	 * {@link #listeners_notifier}. */
	private transient ReentrantLock event_lock;
	
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
	
	/** Notifies subscribed listeners contained in {@link #time_value_listeners}
	 * using separate thread for each notification to speed&nbsp;up perfomance. */
	private transient ThreadPoolExecutor listeners_notifier;
	

	///// Нестатический блок инициализации ================================/////
	{
		description_text = null;
		time_unit_values = new EnumMap<>(Time_unit_name.class);
		deserialization_status = true;
		semaphore = new Semaphore(semaphore_permits);
		time_value_edges_lock = new ReentrantLock();
		event_lock = new ReentrantLock();
		is_positive = true;
		time_value_listeners = new ArrayList<>();
		listeners_notifier = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0,
				TimeUnit.NANOSECONDS, new LinkedTransferQueue<>());
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
	 * <li>{@link #time_counter_text_listeners_notification()}.</li></ul>
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
	 * time&nbsp;value (is on the&nbsp;<u>right</u> side).
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
	 * <li>{@link #time_counter_text_listeners_notification()}.</li></ul>
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
	 * <li>{@link #time_counter_text_listeners_notification()}.</li></ul>
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
	
	
	/* TODO: ? Is there possible resource leak if object has not unsubscribed
	 * from this event notifying, but is not referred anymore else? If so - need
	 * to mention this in javadoc */
	/**
	 * Adds specified {@code listener} to receive time&nbsp;counter's
	 * time&nbsp;value text change event. Same {@code listener} <u>can</u>
	 * be&nbsp;added multiple times.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #remove_Time_counter_text_listener(Time_counter_text_listener)};</li>
	 * <li>{@link #time_counter_text_listeners_notification()}.</li></ul>
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
			event_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
		}
		
		try
		{
			time_value_listeners.add(listener);
			listeners_notifier.setCorePoolSize(
					listeners_notifier.getCorePoolSize() + 1);
		}
		finally
		{
			event_lock.unlock();
		}
	}
	
	
	/**
	 * Remove <u>first occurrence</u> of specified {@code listener} argument
	 * <u>if such present</u>.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #add_Time_counter_text_listener(Time_counter_text_listener)};</li>
	 * <li>{@link #time_counter_text_listeners_notification()}.</li></ul>
	 * 
	 * @param listener Listener to be unsubscribed from event.
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
			event_lock.lockInterruptibly();
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
				listeners_notifier.setCorePoolSize(
						listeners_notifier.getCorePoolSize() - 1);
			}
			
			return is_removed;
		}
		finally
		{
			event_lock.unlock();
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
	 * <li>{@link #time_counter_text_listeners_notification()}.</li></ul>
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
					formatter.format(strict_display_mode_text);
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
					/* Формирование строки для форматирования, содержащей
					 * отображаемые единицы времени, происходит от больших
					 * единиц времени к меньшим. Соответственно, если большая
					 * единица времени имеет значение (т.е. не равна 0), -
					 * меньшие единицы должны отображаться в любом случае. Это
					 * касается режима НЕСТРОГОГО отображения диапазона
					 * отображаемых единиц времени
					 * (Time_display_style.TDS_increase_able). true - обнаружено
					 * ненулевое значение единицы времени, превышающее
					 * установленный диапазон для отображения; false - нет */
					boolean out_of_set_range = false;
					
					/* Формирование строки для форматирования, содержащей
					 * отображаемые единицы времени */
					for (final Time_unit_name i : Time_unit_name.values())
					{
						/* Результат сравнения именованной константы текущей
						 * итерации и крайней правой отображаемой единицы
						 * (наименьшей отображаемой единицы) времени */
						final int compare_to_1 =
								i.compareTo(time_value_edges[1]);
						
						/* Если (единица времени входит в выставленный диапазон
						 * отображения) ... */
						if ((i.compareTo(time_value_edges[0]) >= 0 && compare_to_1 <= 0) ||
								/* ... ИЛИ (установлен нестрогий диапазон
								 * отображаемых единиц времени И (в предыдущих
								 * итерациях обнаружено ненулевое значение
								 * единицы времени, превышающее установленный
								 * диапазон для отображения ... */
								(increase_able_is_set && (out_of_set_range ||
										// ... ИЛИ (это обнаружилось в этой итерации)))
										(compare_to_1 <= 0 && time_unit_values.get(i) != 0))))
						{
							out_of_set_range = true;
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
	 * <li>{@link #time_counter_text_listeners_notification()}.</li></ul>
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
	 * Notifies listeners subscribed via {@link Time_counter_text_listener}
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
	 * <li>{@link #remove_Time_counter_text_listener(Time_counter_text_listener)}.</li></ul>
	 */
	protected final void time_counter_text_listeners_notification()
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
				event_lock.lockInterruptibly();
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
					listeners_notifier.execute(new Runnable()
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
				event_lock.unlock();
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
	 * <li><i>{@link Mode#M_countdown_till}.</i> {@code true} need to be passed
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
	 * <li>{@link #time_counter_text_listeners_notification()}.</li></ul>
	 * 
	 * @param is_positive {@code true}&nbsp;&#0151; time&nbsp;value
	 * <u>is&nbsp;positive</u>; {@code false}&nbsp;&#0151;
	 * <u>is&nbsp;negative</u>.
	 */
	protected final void set_time_counter_value_sign(
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
			this.is_positive = is_positive;
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
	 * <li><i>{@link Mode#M_countdown_till}.</i> {@code true} is&nbsp;returned
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
		/* Если вместе с числовыми значениями единиц времени будут
		 * выводиться их полные названия */
		if (time_unit_layout.equals(Time_unit_layout.TUL_full_name))
		{
			// Числовое значение единицы времени текущей итерации
			final long time_unit_value_num = time_unit_values.get(time_unit);
			// Длина названия единицы времени
			final long value_name_length = time_unit_texts.
					get(time_unit_layout).get(time_unit).length() + 1;
			/* Название единицы времени, передаваемое в качестве
			 * аргумента для форматирования строки */
			final String value_name_text =
					time_unit_texts.get(time_unit_layout).get(time_unit) +
					// TODO: Учесть вывод чисел, заканчивающихся на ...11
					/* Условие тернарного оператора: Если числовое
					 * значение единицы времени текущей итерации
					 * заканчивается на ...1 - значит название этой
					 * единицы времени будет выводиться в
					 * единственном роде */
					(time_unit_value_num % 10 == 1 ? "" : "s");
			
			formatter.format(format_conversions.get(time_unit) + "%" +
					value_name_length + "s  ", time_unit_value_num,
					value_name_text);
		}
		else
		{
			formatter.format(format_conversions.get(time_unit) +
					time_unit_texts.get(time_unit_layout).get(time_unit),
					time_unit_values.get(time_unit));
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
		event_lock = new ReentrantLock();
		time_unit_values = new EnumMap<>(Time_unit_name.class);
		time_value_listeners = new ArrayList<>();
		listeners_notifier = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0,
				TimeUnit.NANOSECONDS, new LinkedTransferQueue<>());
	}
}
