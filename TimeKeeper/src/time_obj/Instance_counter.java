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
	 * метода {@link #difference_calculation()}. Устанавливается из
	 * {@link Time_counter_control}. */
	static CyclicBarrier difference_calculation_barrier;
	
	
	///// Fields private static ===========================================/////
	/** Отвечает за логирование событий. */
	private static final Logger logger;
	
	/** Текущие дата и время. */
	private static ZonedDateTime time_current;
	
	/** Часовой пояс с локальными настройками сезонного перевода времени. */
	private static ZoneId zone_rules;
	
	
	static
	{
		logger = Logger.getLogger(Instance_counter.class.getName());
		
		try
		{
			zone_rules = ZoneId.systemDefault();
		}
		catch(final DateTimeException exc)
		{
			logger.log(Level.WARNING,
					"Cannot obtain system ZoneId. Exception stack trace:", exc);
			zone_rules = null;
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
	 * @param mode_set Режим работы счетчика времени согласно перечислению
	 * {@link time_obj.Mode}.<br>
	 * <b>Важно!</b> Т.к.&nbsp;данный класс реализует <u>исключительно</u>
	 * режим подсчета прошедшего времени с заданного момента и режим таймера
	 * обратного отсчета с привязкой к будущей дате, конструктор этого класса
	 * принимает <u>только</u> {@link time_obj.Mode#M_elapsed_from} или
	 * {@link time_obj.Mode#M_countdown_till} в качестве параметра.
	 * 
	 * @param time_instance_init Значение даты и времени, относительно которого
	 * будет вестись подсчет прошедшего/оставшегося времени.<br>
	 * <b>Важно!</b> В качестве аргумента <u>не&nbsp;может</u> передаваться
	 * {@code null}.
	 * 
	 * @exception IllegalArgumentException В случае передачи неподходящего
	 * параметра для {@code mode_set}.
	 * 
	 * @exception NullPointerException Если в качестве одного из аргументов
	 * конструктора передан {@code null}.
	 */
	public Instance_counter(
			final Mode mode_set, final ZonedDateTime time_instance_init)
	{
		super(mode_set);
		
		// Если передан неподходящий параметр для "mode_set"
		if (!mode_set.equals(Mode.M_elapsed_from) &&
				!mode_set.equals(Mode.M_countdown_till))
		{
			throw new IllegalArgumentException("Incompatible "
					+ Mode.class.getName() + " value passed to constructor");
		}
		
		// Если передан null для "time_instance_init"
		if (time_instance_init == null)
		{
			throw new NullPointerException(ZonedDateTime.class.getName()
					+ " object passed to constructor is null");
		}

		time_instance = time_instance_init;
		time_instance_offset = time_instance.getOffset().getTotalSeconds();
		
		/* Если не удалось получить часовой пояс с локальными настройками
		 * сезонного перевода времени во время статической инициализации */
		if (zone_rules == null)
		{
			zone_rules = time_instance_init.getZone();
		}
		else
		{
			/* Если статическое поле часового пояса не совпадает с новым
			 * значением даты и времени, относительно которого будет вестись
			 * подсчет прошедшего/оставшегося времени */
			if (!zone_rules.equals(time_instance_init.getZone()));
			{
				// TODO
			}
		}
		
		difference_calculation();
	}
	

	///// Методы public статические =======================================/////
	/**
	 * Для экземпляров данного класса существует общее статическое значение
	 * {@link ZoneId}, переодически сверяемое с системными настройками. Также
	 * это значение сверяется со&nbsp;значением {@link ZoneId}, полученным из
	 * аргумента {@link ZonedDateTime} конструктора при создании нового
	 * экземпляра. При несоответствии с уже установленными правилами вновь
	 * полученное значение правил устанавливается как корректное. Если&nbsp;же
	 * в области видимости, создающей экземпляр данного класса не&nbsp;удалось
	 * получить {@link ZoneId} для создания объекта
	 * {@link ZonedDateTime}&nbsp;&#0151; рекомендуется воспользоваться данным
	 * методом с&nbsp;целью избежания лишних корректировок в экземплярах данного
	 * класса.
	 * 
	 * @return Значение правил временной зоны, используемое во&nbsp;всех
	 * экземплярах данного класса.<br>
	 * <i>Примечание.</i> Если еще не&nbsp;существует ни&nbsp;одного экземпляра
	 * данного класса, и при этом во&nbsp;время статической инициализации
	 * не&nbsp;удалось получить {@link ZoneId}&nbsp;&#0151; будет возвращен
	 * {@code null}.
	 */
	public static ZoneId get_Instance_counter_zone_rules()
	{
		return zone_rules;
	}
	
	
	/* TODO: Метод, обновляющий локальные настройки временной зоны "ZoneId".
	 * Синхронизировать с "difference_calculation()" */
	
	
	///// Methods default-access of-instance ==============================/////
	/**
	 * Вычисляет разницу во времени между текущей датой и целевой датой
	 * {@link #time_instance}.
	 */
	final void difference_calculation()
	{
		time_current = ZonedDateTime.now(zone_rules);
		
		/* Разница в смещениях относительно гринвичского времени между текущим
		 * и целевым временем в секундах */
		int offset_difference =
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
		
		try
		{
			difference_calculation_barrier.await();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts after encountering "
					+ InterruptedException.class.getName() + " while waiting for"
					+ " all same threads to reach this point. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		// При возникновении данного исключения поток доведет выполнение до конца
		catch (final BrokenBarrierException exc)
		{
			logger.log(Level.INFO, "Thread encountered broken common barrier"
					+ " while waiting for all same threads to reach this point."
					+ " It will continue executing. Exception stack trace:", exc);
		}
		
		time_counter_text_listeners_notification();
	}
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * Auxiliary method for {@link #difference_calculation()}.
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
		
		/* Если экземпляр работает в режиме таймера обратного отсчета до
		 * определенной временной точки */
		if (calculation_till)
		{
			/* В режиме "Mode.M_countdown_till" положительное кол-во секунд
			 * означает положительные значения таймера обратного отсчета */
			set_time_counter_value_sign(seconds >= 0 ? true : false);
		}
		else
		{
			/* В режиме "Mode.M_elapsed_from" отрицательное кол-во секунд
			 * означает положительные значения времени, прошедшего с
			 * определенного времени */
			set_time_counter_value_sign(seconds < 0 ? true : false);
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
					/* Месяц целевой ИЛИ текущей даты (в зависимсоти от
					 * дополнительных условий) */
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

						// Если предпоследним месяцем является февраль И ...
						if (month_before == 2 &&
								// ... это высокосный год
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
						
						/* Если предпоследним месяцем является февраль И это
						 * высокосный год */
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
	private void readObject(ObjectInputStream input_stream)
			throws IOException, ClassNotFoundException
	{
		input_stream.defaultReadObject();

		try
		{
			/* Если не удалось получить часовой пояс с локальными настройками
			 * сезонного перевода времени во время статической инициализации */
			if (zone_rules == null)
			{
				zone_rules = time_instance.getZone();
			}
			/* Если статическое поле часового пояса не совпадает с целевым
			 * значением даты и времени, относительно которого будет вестись
			 * подсчет прошедшего/оставшегося времени */
			else if (!zone_rules.equals(time_instance.getZone()))
			{
				// TODO
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
		difference_calculation();
	}
}
