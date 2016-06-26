package time_obj;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;


// TODO: ? Объявить абстрактные методы
/**
 * Инкапсулирует основные свойства счетчика времени.<br>
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
	
	
	///// Поля pulic статические ==========================================/////
	/** Шаблонные названия единиц времени (символьные, сокращенные, полные и
	 * пустые), распределенные по группам, сокласно именованным константам
	 * {@link Time_unit_layout#TUL_value_sign},
	 * {@link Time_unit_layout#TUL_short_name},
	 * {@link Time_unit_layout#TUL_full_name} и
	 * {@link Time_unit_layout#TUL_digits_only} из перечисления
	 * {@link Time_unit_layout}.<br>
	 * Метод {@link EnumMap#get(Object)} возвращает контейнер
	 * {@code Map<}{@link Time_unit_name}{@code , }{@link String}{@code >},
	 * содержащий строки с названиями единиц времени для конкретной группы.<br>
	 * <b>Важно!</b> Как <u>данный контейнер</u>, так <u>и все вложенные</u> в
	 * него контейнеры
	 * {@code Map<}{@link Time_unit_name}{@code ,}{@link String}{@code >}
	 * являются <u>неизменяемыми (immutable)</u>. Попытка изменить содержимое
	 * данных контейнеров приведет к ошибке времени выполнения. */
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
	

	///// Нестатический блок инициализации ================================/////
	{
		description_text = null;
		time_unit_values = new EnumMap<>(Time_unit_name.class);
		deserialization_status = true;
		time_display_style = settings.get_time_display_style_setting();
		time_value_edges = settings.get_time_value_edges();
		time_unit_layout = settings.get_time_unit_layout_setting();
		semaphore = new Semaphore(semaphore_permits);
		time_value_edges_lock = new ReentrantLock();
		is_positive = true;
	}
	
	
	///// Конструкторы protected ==========================================/////
	/**
	 * Данному конструктору делегируется инициализация полей с общей для
	 * наследуемых классов информацией.
	 * 
	 * @param instance_mode_set Режим работы счетчика времени экземпляра класса.
	 */
	protected Time_counter(final Mode instance_mode_set)
	{
		instance_mode = instance_mode_set;
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
	 * <i>Perfomance note.</i> Contains synchronized sections. Synchronized with:
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
	 * and {@code right_edge} contatining
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
	 * <i>Perfomance note.</i> Method contains synchronized sections.
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
	 * <i>Perfomance note.</i> Contains synchronized sections. Synchronized
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
	
	
	///// Методы protected экземпляра =====================================/////
	/**
	 * Formats text&nbsp;string with time&nbsp;counter value (can be obtained
	 * via {@link #get_time_counter_text_value()}).<br>
	 * <i>Perfomance note.</i> Contains synchronized code sections. Synchronized
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
					formatter.format("\u0150");
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
						/* Если значащая единица времени уже достигнута в
						 * предыдущих итерациях ИЛИ достигнута в этой итарации ... */
						if (value_reached || time_unit_values.get(i) != 0 ||
								/* ... ИЛИ это секунды (должны отображаться в
								 * любом случае) как наименьшая отображаемая
								 * единица времени */
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
	 * Notifies listeners subscribed via [TODO: listener interface name] about
	 * {@link #time_counter_text} value changed.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #build_time_string()};</li>
	 * <li>{@link #build_time_string(String)};</li>
	 * <li>{@link #get_time_counter_text_value()};</li>
	 * <li>{@link #set_time_counter_value_sign(boolean)};</li>
	 * <li>{@link #set_time_display_style(Time_display_style)};</li>
	 * <li>{@link #set_time_unit_layout(Time_unit_layout)}.</li></ul>
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
			/* TODO: Notify listeners about changed "time_counter_text" field
			 * value and provide "is_positive_value" in addition */
		}
		finally
		{
			semaphore.release(semaphore_permits);
		}
	}
	
	
	/**
	 * Sets time&nbsp;counter value sign (positive or negative). Extenging
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
	 * <i>Perfomance note.</i> Contains synchronized sections. Synchronized
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
	 * Вспомогательный метод для {@link #build_time_string()}. Форматирует
	 * участок строки, отвечающий за отображение, единицы времени, переданной
	 * в&nbsp;качестве аргумента, и конкантенирует полученную строку с
	 * {@link #formatter}.<br>
	 * <b>Важно!</b> Данный метод <u>не&nbsp;создает</u> и
	 * <u>не&nbsp;закрывает</u> поле типа {@link Formatter} экземпляра класса.
	 * 
	 * @param time_unit Единица времени согласно перечислению
	 * {@link Time_unit_name}, которую необходимо отобразить.
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
	private void readObject(ObjectInputStream input_stream)
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
					+ " is null. Deserialized object can be used. Excepion\'s stack trace:", exc);
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
		time_unit_values = new EnumMap<>(Time_unit_name.class);
	}
}
