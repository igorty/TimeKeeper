package time_obj;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.logging.Level;
import java.util.logging.Logger;

import time_obj.dialog.User_notification_dialog;
import time_obj.dialog.User_notification_type;
import time_obj.events.User_notification_event;


/**
 * Реализует режимы подсчета прошедшего времени с заданного момента и таймера
 * обратного отсчета с привязкой к будущей дате (согласно именованным
 * константам {@link Mode#M_elapsed_from} и {@link Mode#M_countdown_till}
 * соответственно).<br>
 * <i>Примечания.</i>
 * <ul><li>После создания объект добавляет себя в общий контейнер
 * элементов типа {@link Time_counter}, находящийся в singleton'е
 * {@link Time_counter_control}.</li>
 * <li>При десериализации объект может бросить следующие исключения:
 * <ul><li>{@link IOException}&nbsp;&#0151; ошибка в работе входящего потока;</li>
 * <li>{@link ClassNotFoundException}&nbsp;&#0151; класс сериализованного
 * объекта не&nbsp;определен;</li>
 * <li>{@link InvalidObjectException}&nbsp;&#0151; если хотя&#8209;бы один
 * инвариант десериализованного объекта не&nbsp;прошел валидацию.</li></ul></li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Instance_counter extends Time_counter implements Serializable
{
	///// Fields default-access static ====================================/////
	/** Барьер всех экземпляров данного класса для синхронного выполнения
	 * метода {@link #difference_calculation(boolean)}. Устанавливается из
	 * {@link Time_counter_control}. */
	static CyclicBarrier difference_calculation_barrier;
	
	
	///// Fields private static ===========================================/////
	/** Отвечает за логирование событий. */
	private static final Logger logger;
	
	/** Текущие дата и время. */
	private static ZonedDateTime time_current;
	
	/** Часовой пояс с локальными настройками сезонного перевода времени. */
	private static ZoneId zone_id;
	
	
	static
	{
		logger = Logger.getLogger(Instance_counter.class.getName());
		
		try
		{
			zone_id = ZoneId.systemDefault();
		}
		catch(final DateTimeException exc)
		{
			logger.log(Level.WARNING,
					"Cannot obtain system ZoneId. Exception stack trace:", exc);
			zone_id = null;
		}
	}
	
	
	///// Fields public of-instance =======================================/////
	/** Значение даты и времени, относительно которого будет вестись подсчет
	 * оставшегося/прошедшего времени (еще называемое здесь&nbsp;&#0151;
	 * <i>"целевое" время</i>).
	 * 
	 * @serial После десериализации не&nbsp;должен быть {@code null}. При
	 * несоответствии условию проверки генерируется исключение
	 * {@link InvalidObjectException}. */
	public final ZonedDateTime time_instance;
	
	
	///// Поля private экземпляра =========================================/////
	/** Смещение целевого времени {@link #time_instance} в секундах
	 * относительно гринвичского времени. */
	private transient int time_instance_offset;
	
	
	///// Constructors public =============================================/////
	/**
	 * This constructor takes time&nbsp;counter layout parameters from
	 * {@link Settings}&nbsp;object.
	 * 
	 * @param mode Mode in which this time&nbsp;counter runs.<br>
	 * <b>Important!</b> Since this class implements <i>counting time elapsed
	 * from specified instant</i> and <i>counting time remaining to specified
	 * instant</i> modes, this argument can be {@link Mode#M_elapsed_from} or
	 * {@link Mode#M_countdown_till} <u>only</u>.
	 * 
	 * @param time_instance Date and time values relatively to which
	 * elapsed/remaining time will be counted. <u>Cannot</u> be {@code null}.<br>
	 * <i>Note.</i> The&nbsp;argument's {@link ZoneId} is compared with
	 * {@code Instance_counter}'s stored value (can be obtained using
	 * {@link #get_time_counter_text_value()}). <u>If values do&nbsp;not
	 * match</u>, {@code Instance_counter} object will try to ensure system
	 * zone&nbsp;rules are same as its own stored value (using
	 * {@link ZoneId#systemDefault()} method). <u>If such checking fails</u>,
	 * the&nbsp;argument's zone&nbsp;rules will be set as default. <u>If
	 * mentioned checking succeed</u>, {@code Instance_counter}'s stored
	 * zone&nbsp;rules will be updated from obtained value.
	 * 
	 * @exception IllegalArgumentException Inappropriate {@code mode} argument
	 * passed.
	 * 
	 * @exception NullPointerException At least one of passed arguments
	 * is&nbsp;{@code null}.
	 */
	public Instance_counter(
			final Mode mode, final ZonedDateTime time_instance)
	{
		super(mode);
		
		common_constructors_method(mode, time_instance);
		this.time_instance = time_instance;
		difference_calculation(false);
		Time_counter_control.get_instance().get_time_counters().add(this);
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
	 * @param time_instance Date and time values relatively to which
	 * elapsed/remaining time will be counted. <u>Cannot</u> be {@code null}.<br>
	 * <i>Note.</i> The&nbsp;argument's {@link ZoneId} is compared with
	 * {@code Instance_counter}'s stored value (can be obtained using
	 * {@link #get_time_counter_text_value()}). <u>If values do&nbsp;not
	 * match</u>, {@code Instance_counter} object will try to ensure system
	 * zone&nbsp;rules are same as its own stored value (using
	 * {@link ZoneId#systemDefault()} method). <u>If such checking fails</u>,
	 * the&nbsp;argument's zone&nbsp;rules will be set as default. <u>If
	 * mentioned checking succeed</u>, {@code Instance_counter}'s stored
	 * zone&nbsp;rules will be updated from obtained value.
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
	public Instance_counter(final Mode mode, final ZonedDateTime time_instance,
			final Time_display_style time_display_style,
			final Time_unit_name leftmost_displayed_time_unit,
			final Time_unit_name rightmost_displayed_time_unit,
			final Time_unit_layout time_unit_layout)
	{
		super(mode, time_display_style, leftmost_displayed_time_unit,
				rightmost_displayed_time_unit, time_unit_layout);
		
		common_constructors_method(mode, time_instance);
		this.time_instance = time_instance;
		difference_calculation(false);
		Time_counter_control.get_instance().get_time_counters().add(this);
	}
	

	///// Methods default-access of-instance ==============================/////
	/**
	 * Вычисляет разницу во времени между текущей датой и целевой датой
	 * {@link #time_instance}.
	 * @param is_synchronous {@code true}&nbsp;&#0151; all instances of
	 * {@code Instance_counter} class are executed synchronously using
	 * {@link #difference_calculation_barrier}; {@code false}&nbsp;&#0151;
	 * the&nbsp;method need to be executed singly (commonly when instance
	 * initialization).
	 */
	final void difference_calculation(boolean is_synchronous)
	{
		time_current = ZonedDateTime.now(zone_id);
		
		/* Разница в смещениях относительно гринвичского времени между текущим
		 * и целевым временем в секундах */
		final int offset_difference =
				time_current.getOffset().getTotalSeconds() - time_instance_offset;

		// Если часовые пояса текущего и целевого времени совпадают
		if (offset_difference == 0)
		{
			difference_calculation_sub_method(time_current,
					instance_mode.equals(Mode.M_countdown_till) ? true : false);
		}
		else
		{
			// Текущие дата и время "time_current", без привязки к часовому поясу
			final LocalDateTime time_current_local;
			
			/* Если разница в смещениях относительно гринвичского времени между
			 * текущим и целевым временим в секундах имеет положительное значение */
			if (offset_difference > 0)
			{
				time_current_local = time_current.
						minusSeconds(offset_difference).toLocalDateTime();
			}
			else
			{
				time_current_local = time_current.toLocalDateTime().
						minusSeconds(offset_difference);
			}
			
			difference_calculation_sub_method(time_current_local,
					instance_mode.equals(Mode.M_countdown_till) ? true : false);
		}
		
		build_time_string();
		
		// If method is executing synchronously with same class instances
		if (is_synchronous)
		{
			try
			{
				difference_calculation_barrier.await();
			}
			catch (final InterruptedException exc)
			{
				logger.log(Level.INFO, "Thread interrupts after encountering "
						+ InterruptedException.class.getName() + " while waiting"
								+ " for all same threads to reach this point."
								+ " Exception stack trace:", exc);
				Thread.currentThread().interrupt();
			}
			// При возникновении данного исключения поток доведет выполнение до конца
			catch (final BrokenBarrierException exc)
			{
				logger.log(Level.INFO, "Thread encountered broken common barrier"
						+ " while waiting for all same threads to reach this"
								+ " point. It will continue executing. Exception"
								+ " stack trace:", exc);
			}
		}
		
		notify_time_counter_text_listeners();
	}
	
	
	// TODO: Метод, обновляющий локальные настройки временной зоны "ZoneId"
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * Auxiliary method for {@link #difference_calculation(boolean)}.
	 * Is&nbsp;in&nbsp;charge for converting time&nbsp;units, obtained from
	 * {@link Temporal#until(Temporal, java.time.temporal.TemporalUnit)}, into
	 * values for {@link Time_counter#time_unit_values}.
	 * 
	 * @param <Type1> This generic type is common for {@link ZonedDateTime} and
	 * {@link LocalDateTime}, which are expected as {@code date_time_now}
	 * argument.
	 * 
	 * @param date_time_now Current date and time.
	 * 
	 * @param calculation_till {@code true} means calculating time in
	 * {@link Mode#M_countdown_till}&nbsp;mode. In this case
	 * time&nbsp;difference <u>will be positive if
	 * target&nbsp;time&nbsp;({@link #time_instance}) has&nbsp;not yet come</u>
	 * (in&nbsp;other words, when current time&nbsp;value <u>is&nbsp;less</u>
	 * than target&nbsp;time). If current time&nbsp;value <u>is&nbsp;greater
	 * than target&nbsp;time</u>, time&nbsp;difference <u>will be negative</u>.<br>
	 * When passing {@code false} as the&nbsp;argument (means calculating time
	 * in {@link Mode#M_elapsed_from}), the&nbsp;reverse situation occurs.
	 * In&nbsp;this case time&nbsp;difference <u>will be positive if current
	 * time&nbsp;value is&nbsp;greater than
	 * target&nbsp;time&nbsp;({@link #time_instance})</u> (in&nbsp;other words,
	 * when target&nbsp;time <u>has&nbsp;already&nbsp;come</u>). If
	 * target&nbsp;time&nbsp;value <u>is&nbsp;greater than current&nbsp;time</u>,
	 * time&nbsp;difference <u>will be negative</u>.
	 */
	private <Type1 extends Temporal>
	void difference_calculation_sub_method(final Type1 date_time_now,
			boolean calculation_till)
	{
		// Кол-во полных месяцев между текущей и целевой датой и временем
		long months = date_time_now.until(time_instance, ChronoUnit.MONTHS);
		/* Кол-во полных (не во всех случаях) дней между текущей и целевой
		 * датой и временем */
		long days = date_time_now.until(time_instance, ChronoUnit.DAYS);
		// Кол-во полных секунд между текущей и целевой датой и временем
		long seconds = date_time_now.until(time_instance, ChronoUnit.SECONDS);
		// Current time value sign to compare with previous one
		final boolean current_value_sign;
		// Previous time value sign to compare with current one
		final boolean previous_value_sign;
		
		/* Если экземпляр работает в режиме таймера обратного отсчета до
		 * определенной временной точки */
		if (calculation_till)
		{
			/* В режиме "Mode.M_countdown_till" положительное кол-во секунд
			 * означает положительные значения таймера обратного отсчета */
			current_value_sign = seconds >= 0 ? true : false;
			previous_value_sign = set_time_counter_value_sign(current_value_sign);
		}
		else
		{
			/* В режиме "Mode.M_elapsed_from" отрицательное кол-во секунд
			 * означает положительные значения времени, прошедшего с
			 * определенного времени */
			current_value_sign = seconds <= 0 ? true : false;
			previous_value_sign = set_time_counter_value_sign(current_value_sign);
		}

		// If time value reached zero
		if (current_value_sign != previous_value_sign)
		{
			notify_time_elapsed_listeners();
		}
		
		/* Если разница во времени между текущей и целевой датами насчитывает
		 * хотя бы один полный месяц */
		if (months != 0)
		{
			final LocalDate date_now;  // Чистая текущая дата без времени
			
			// Если параметром метода является класс "ZonedDateTime"
			if (date_time_now instanceof ZonedDateTime)
			{
				// Буферный объект для приведения "date_time_now" к ZonedDateTime
				final ZonedDateTime temp = (ZonedDateTime)date_time_now;
				
				date_now = temp.toLocalDate();
			}
			// Если параметром метода является класс "LocalDateTime"
			else if (date_time_now instanceof LocalDateTime)
			{
				// Буферный объект для приведения "date_time_now" к LocalDateTime
				final LocalDateTime temp = (LocalDateTime)date_time_now;
				
				date_now = temp.toLocalDate();
			}
			else
			{
				throw new IllegalArgumentException("Internal class error. Illegal "
						+ Temporal.class.getName()
						+ " argument passed to template method");
			}
			
			/* TODO: ? Правильно ли использовать "time_instance" вместо чистой
			 * даты (без временной зоны) этого поля? */
			/* Кол-во полных дней между текущей и целевой датой без учета
			 * часов, минут, секунд */
			final long days_strict = date_now.until(time_instance, ChronoUnit.DAYS);
			// Число месяца текущей даты
			final int date_now_month_day =
					date_time_now.get(ChronoField.DAY_OF_MONTH);
			// Число месяца целевой даты
			final int date_instance_month_day = time_instance.getDayOfMonth();
			
			/* Определение кол-ва оставшихся/прошедших дней.
			 * Постановка условия зависит от знака кол-ва полных секунд между
			 * текущей и целевой датой и временем. Если кол-во полных секунд
			 * имеет ПОЛОЖИТЕЛЬНЫЙ знак, условие сработает когда день целевого
			 * месяца БОЛЬШЕ дня текущего месяца. Если же кол-во полных секунд
			 * имеет ОТРИЦАТЕЛЬНЫЙ знак, условие сработает когда день целевого
			 * месяца МЕНЬШЕ дня текущего месяца */
			if (seconds > 0 ? date_instance_month_day > date_now_month_day :
				date_instance_month_day < date_now_month_day)
			{
				/* Если кол-во полных дней между текущей и целевой датой с
				 * учетом часов, минут, секунд совпадает с кол-вом полных дней
				 * без учета часов, минут, секунд */
				if (days == days_strict)
				{
					days = date_instance_month_day - date_now_month_day;
				}
				else
				{
					days = date_instance_month_day - date_now_month_day - 1;
				}
			}
			else
			{
				// Если числа месяцев текущей и целевой даты совпадают И ...
				if (date_now_month_day == date_instance_month_day &&
						/* ... кол-во полных дней между текущей и целевой датой
						 * с учетом часов, минут, секунд совпадает с кол-вом
						 * полных дней без учета часов, минут, секунд */
						days == days_strict)
				{
					days = 0;
				}
				else
				{
					/* Target time OR current time month (depends on additional
					 * conditions) */
					final int current_month;
					/* Месяц, идущий перед целевым/текущим (в зависимости от
					 * месяца "current_month") */
					final int month_before;
					/* Кол-во дней в месяце "month_before" (в зависимости от
					 * дополнительных условий может быть как положительным так
					 * и отрицательным) */
					final int days_in_month_before;
					
					/* Если кол-во полных секунд между текущей и целевой датой
					 * и временем имеет положительный знак.
					 * Переменной "current_month" присваивается месяц ЦЕЛЕВОЙ
					 * даты; значение "days_in_month_before" будет
					 * ПОЛОЖИТЕЛЬНЫМ */
					if (seconds > 0)
					{
						// Присваивается месяц ЦЕЛЕВОЙ даты
						current_month = time_instance.getMonthValue();
						month_before =
								(current_month > 1 ? current_month - 1 : 12);

						// If penultimate month is February AND ...
						if (month_before == 2 &&
								// ... this is a leap year
								time_instance.toLocalDate().isLeapYear())
						{
							days_in_month_before =
									month_sizes.get(month_before - 1) + 1;
						}
						else
						{
							days_in_month_before =
									month_sizes.get(month_before - 1);
						}
					}
					/* Переменной "current_month" присваивается месяц ТЕКУЩЕЙ
					 * даты; значение "days_in_month_before" будет
					 * ОТРИЦАТЕЛЬНЫМ */
					else
					{
						current_month = date_now.getMonthValue();
						month_before =
								(current_month > 1 ? current_month - 1 : 12);
						
						/* If penultimate month is February AND this is a leap
						 * year */
						if (month_before == 2 && date_now.isLeapYear())
						{
							days_in_month_before =
									-(month_sizes.get(month_before - 1) + 1);
						}
						else
						{
							days_in_month_before =
									-(month_sizes.get(month_before - 1));
						}
					}
					
					/* Если кол-во полных дней между текущей и целевой датой с
					 * учетом часов, минут, секунд совпадает с кол-вом полных
					 * дней без учета часов, минут, секунд */
					if (days == days_strict)
					{
						days = days_in_month_before - date_now_month_day +
								date_instance_month_day;
					}
					else
					{
						days = days_in_month_before - date_now_month_day +
								date_instance_month_day - 1;
					}
				}
			}
		}

		// Кол-во полных лет между текущей и целевой датой и временем
		final long years = date_time_now.until(time_instance, ChronoUnit.YEARS);
		
		months = months - years * 12;
		
		// Кол-во полных часов между текущей и целевой датой и временем
		long hours = date_time_now.until(time_instance, ChronoUnit.HOURS);
		// Кол-во полных минут между текущей и целевой датой и временем
		long minutes = date_time_now.until(time_instance, ChronoUnit.MINUTES);
		
		seconds = seconds - minutes * 60;
		minutes = minutes - hours * 60;
		hours %= 24;
		time_unit_values.put(Time_unit_name.TUN_years, Math.abs(years));
		time_unit_values.put(Time_unit_name.TUN_months, Math.abs(months));
		time_unit_values.put(Time_unit_name.TUN_days, Math.abs(days));
		time_unit_values.put(Time_unit_name.TUN_hours, Math.abs(hours));
		time_unit_values.put(Time_unit_name.TUN_minutes, Math.abs(minutes));
		time_unit_values.put(Time_unit_name.TUN_seconds, Math.abs(seconds));
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
	 * 
	 * @exception InvalidObjectException Если хотя&#8209;бы один инвариант
	 * десериализованного объекта не&nbsp;прошел валидацию.
	 */
	private void readObject(final ObjectInputStream input_stream)
			throws IOException, ClassNotFoundException
	{
		input_stream.defaultReadObject();

		try
		{
			/* Если не удалось получить часовой пояс с локальными настройками
			 * сезонного перевода времени во время статической инициализации */
			if (zone_id == null)
			{
				zone_id = time_instance.getZone();
			}
			
			/* Если режим работы экземпляра счетчика времени является
			 * неподходящим для данного класса */
			if (!instance_mode.equals(Mode.M_elapsed_from) &&
					!instance_mode.equals(Mode.M_countdown_till))
			{
				throw new InvalidObjectException(
						"Incompatible time mode for Instance_counter object");
			}
		}
		catch (final NullPointerException exc)
		{
			logger.log(Level.SEVERE, "Deserialized object has at least one"
					+ " important field whith value null. This object cannot be"
					+ " used. Exception stack trace:", exc);
			throw new InvalidObjectException(
					"At least one of deserialized fields is null");
		}
		
		time_instance_offset = time_instance.getOffset().getTotalSeconds();
		difference_calculation(false);
	}
	
	
	/**
	 * This method consists of common procedures for both constructors
	 * {@link #Instance_counter(Mode, ZonedDateTime)} and
	 * {@link #Instance_counter(Mode, ZonedDateTime, Time_display_style, Time_unit_name, Time_unit_name, Time_unit_layout)}.
	 * 
	 * @param mode_init Mode in which this time&nbsp;counter runs.
	 * {@link Mode#M_elapsed_from} and {@link Mode#M_countdown_till} <u>are
	 * only</u> permitted.
	 * 
	 * @param time_instance_init Initial date and time values relatively to
	 * which elapsed/remaining time will be counted.
	 * 
	 * @exception IllegalArgumentException Inappropriate {@code mode_init}
	 * argument passed.
	 * 
	 * @exception NullPointerException At least one of passed arguments
	 * is&nbsp;{@code null}.
	 */
	private void common_constructors_method(final Mode mode_init,
			final ZonedDateTime time_instance_init)
	{
		// If time counter mode is inappropriate for this class
		if (!mode_init.equals(Mode.M_elapsed_from) &&
				!mode_init.equals(Mode.M_countdown_till))
		{
			throw new IllegalArgumentException("Incompatible "
					+ Mode.class.getName() + " value passed to constructor");
		}
		
		time_instance_offset = time_instance_init.getOffset().getTotalSeconds();
		
		// If failed to obtain zone rules while static initialization
		if (zone_id == null)
		{
			zone_id = time_instance_init.getZone();
		}
		/* If saved zone rules doesn't match with "time_instance_init"
		 * zone rules */
		else if (!zone_id.equals(time_instance_init.getZone()))
		{
			// System zone rules to compare with rules stored in class
			final ZoneId system_zone_id;
			
			try
			{
				system_zone_id = ZoneId.systemDefault();
			}
			catch(final DateTimeException exc)
			{
				// New time zone id to set
				final ZoneId new_zone_id = time_instance_init.getZone();
				
				User_notification_dialog.notify_listener_and_continue(
						new User_notification_event(this),
						User_notification_type.UNT_time_zone_error,
						"New time counter time zone rules doesn't match with"
								+ " already stored. Program have tried to check"
								+ " system time zone rules but failed. Program"
								+ " time zone rules are changed from "
								+ zone_id.getRules().toString() + " to "
								+ new_zone_id.getRules().toString());
				/* TODO: Consider synchronizing with "difference_calculation()"
				 * method execution */
				zone_id = new_zone_id;
				
				return;
			}
			
			// If system zone id doesn't match with one stored in class
			if (!system_zone_id.equals(zone_id))
			{
				User_notification_dialog.notify_listener_and_continue(
						new User_notification_event(this),
						User_notification_type.UNT_informing,
						"Program time zone rules updated with system value. Was "
								+ zone_id.getRules().toString() + " became "
								+ system_zone_id.getRules().toString() + '.');
				/* TODO: Consider synchronizing with "difference_calculation()"
				 * method execution */
				zone_id = system_zone_id;
			}
		}
	}
}
