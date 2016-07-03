package time_obj;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import time_obj.events.Numeric_overflow_event;
import time_obj.events.Numeric_overflow_listener;


/**
 * Реализует режимы секундомера и таймера обратного отсчета без привязки к дате
 * (согласно именованным константам {@link Mode#M_stopwatch} и
 * {@link Mode#M_countdown} соответственно).<br>
 * <i>Примечания.</i>
 * <ul><li>После создания объект добавляет себя в общий контейнер элементов типа
 * {@link Time_counter}, находящийся в singleton'е {@link Time_counter_control}.</li>
 * <li>При десериализации объект может сгенерировать следующие исключения:
 * <ul><li>{@link IOException}&nbsp;&#0151; ошибка в работе входящего потока;</li>
 * <li>{@link ClassNotFoundException}&nbsp;&#0151; класс сериализованного
 * объекта не&nbsp;определен;</li>
 * <li>{@link InvalidObjectException}&nbsp;&#0151; поле десериализованного
 * объекта не&nbsp;прошло валидацию.</li></ul></li></ul>
 *
 * @version 1.0
 * @author Igor Taranenko
 */
public class Solo_counter extends Time_counter implements Serializable
{
	///// Inner classes public ============================================/////
	/**
	 * Serves as container to return initial time&nbsp;counter's values. This
	 * class is used by {@link Solo_counter#get_initial_time_values()} method.
	 * 
	 * @version 1.0
	 * @author Igor Taranenko
	 */
	public class Initial_time_values
	{
		///// Fields public of-instance ===================================/////
		/** Initial years, months and days time values. */
		public final Period period_initial;
		/** Initial hours, minutes and seconds time values. */
		public final LocalTime duration_initial;
		
		
		///// Constructors default-access =================================/////
		/**
		 * <b>Warning!</b> Although passing {@code null} as any of arguments
		 * <u>is&nbsp;forbidden</u>, the&nbsp;constructor <u>does&nbsp;not</u>
		 * provide {@code null}&nbsp;argument checking.
		 * 
		 * @param period_initial Initial years, months and days time values.
		 * @param duration_initial Initial hours, minutes and seconds time
		 * values.
		 */
		Initial_time_values(
				final Period period_initial, final LocalTime duration_initial)
		{
			this.period_initial = period_initial;
			this.duration_initial = duration_initial;
		}
	}
	
	
	///// Поля private статические ========================================/////
	/** Логирует события в данном классе. */
	private static final Logger logger;
	
	/** Сообщение, отображаемое вместо счетчика времени в случае переполнения. */
	private static final String numeric_overflow_message;
	
	/* TODO: static final int time_counter_frequency = 100 (means time in
	 * milliseconds in which "thread_counter" is invoked) */
	
	
	static
	{
		logger = Logger.getLogger(Solo_counter.class.getName());
		numeric_overflow_message = "numeric overflow";
	}
	
	
	///// Поля private экземпляра =========================================/////
	/** <u>Начальные</u> значения в годах, месяцах и днях.
	 * 
	 * @serial Верифицируется после десериализации.<br>
	 * <i>Условия проверки.</i>
	 * <ul><li>Это поле <u>может</u> быть {@code null} при любом из следующих
	 * обстоятельств:
	 * <ul><li>режимом отсчета времени экземпляра класса является
	 * {@link time_obj.Mode#M_stopwatch};</li>
	 * <li>поле {@link #duration_init} <u>не&nbsp;ссылается</u> на
	 * {@code null}.</li></ul></li>
	 * <li>Метод {@link Period#isNegative()}, вызванный этим полем,
	 * <u>должен</u> возвращать {@code false}.</li>
	 * <li>Нормализация объекта <u>не&nbsp;должна</u> генерировать
	 * {@link ArithmeticException}.</li></ul>
	 * При несоответствии данным условиям генерируется исключение
	 * {@link InvalidObjectException}. */
	private Period period_init;
	/** <u>Текущие</u> значения в годах, месяцах и днях.
	 * 
	 * @serial Верифицируется после десериализации.<br>
	 * <i>Условия проверки.</i>
	 * <ul><li><u>Не&nbsp;должно</u> быть {@code null}.</li>
	 * <li>Метод {@link Period#isNegative()}, вызванный этим полем,
	 * <u>должен</u> возвращать {@code false}.</li>
	 * <li>Нормализация объекта <u>не&nbsp;должна</u> генерировать
	 * {@link ArithmeticException}.</li></ul>
	 * При несоответствии данным условиям генерируется исключение
	 * {@link InvalidObjectException}. */
	private Period period_passed;
	/** <u>Начальные</u> значения в часах, минутах и секундах.
	 * 
	 * @serial Верифицируется после десериализации.<br>
	 * <i>Условия проверки.</i>
	 * <ul><li>Это поле <u>может</u> быть {@code null} при любом из следующих
	 * обстоятельств:
	 * <ul><li>режимом отсчета времени экземпляра класса является
	 * {@link time_obj.Mode#M_stopwatch};</li>
	 * <li>поле {@link #period_init} <u>не&nbsp;ссылается</u> на
	 * {@code null}.</li></ul></li></ul>
	 * При несоответствии данным условиям генерируется исключение
	 * {@link InvalidObjectException}. */
	private LocalTime duration_init;
	/** <u>Текущие</u> значения в часах, минутах и секундах.
	 * 
	 * @serial Верифицируется после десериализации. Не&nbsp;должен быть
	 * {@code null}. При несоответствии условию генерируется исключение
	 * {@link InvalidObjectException}. */
	private LocalTime duration_passed;
	
	/** Режим подсчета времени согласно перечислению {@link Days_in_year} для
	 * экземпляра класса.
	 * 
	 * @serial Верифицируется после десериализации.<br>
	 * <i>Условие проверки:</i> <u>Не&nbsp;должен</u> быть {@code null}. */
	private final Days_in_year days_count;

	/** {@code true}&nbsp;&#0151; time&nbsp;counting <u>was&nbsp;started</u>
	 * after instance creation (or after restart while
	 * the&nbsp;time&nbsp;counter was stopped); {@code false}&nbsp;&#0151
	 * time&nbsp;counting <u>was&nbsp;not</u> started yet.
	 * 
	 * @serial Deserialized value <u>does&nbsp;not</u>&nbsp;checked. */
	private boolean counting_has_started;
	
	/** Содержит поток, выполняющий ход времени. */
	private transient Runnable thread_counter;
	
	/** Периодически выполняет поток {@link #thread_counter}. */
	private transient ScheduledExecutorService thread_counter_executor;
	
	/** Флаг числового переполнения счетчика времени. {@code false}&nbsp;&#0151;
	 * числового переполнения нет; счетчик времени работает в штатном режиме.
	 * {@code true}&nbsp;&#0151; счетчик времени достиг максимально возможного
	 * значения; дальнейший инкремент времени невозможен.
	 * 
	 * @serial Верифицируется после десериализации, если имеет значение
	 * {@code true}. В этом случае значения полей {@link #period_passed} и
	 * {@link #duration_passed} должны иметь значения даты, времени
	 * "{@link Integer#MAX_VALUE}.11.31/32" (31/32 в&nbsp;зависимости от
	 * {@link Time_counter#instance_mode}) и "0:00:00". <i>Наступление
	 * <u>этих</u> даты и времени должно было вызвать переполнение</i>.
	 * Если&nbsp;же дата и время имеют другое значение&nbsp;&#0151; генерируется
	 * {@link InvalidObjectException}. */
	private volatile boolean numeric_overflow;
	
	/** Regulates time&nbsp;counter value modify access.<br>
	 * <i>Fields and methods access to which is synchronized by this lock
	 * (<u>list can be incomplete</u>):</i>
	 * <ul><li>{@link #period_passed};</li>
	 * <li>{@link #duration_passed};</li>
	 * <li>{@link #counting_has_started};</li>
	 * <li>{@link Time_counter#time_unit_values};</li>
	 * <li>{@link #numeric_overflow} (except in
	 * {@link #numeric_overflow_status()} method);</li>
	 * <li>{@link #set_time_counter_value_sign(boolean)};</li>
	 * <li>{@link Time_counter#is_positive_value()};</li>
	 * <li>{@link #start()}.</li></ul> */
	private transient ReentrantLock modify_lock;
	/** Synchronizes relative to the&nbsp;listeners fields access.<br>
	 * <i>Fields access to which is synchronized by this lock:</i>
	 * <ul><li>{@link #numeric_overflow_listeners};</li>
	 * <li>{@link #listeners_notifier}.</li></ul> */
	private transient ReentrantLock event_lock;
	
	/** Contains listeners subscribed for numeric overflow event. */
	private transient ArrayList<Numeric_overflow_listener> numeric_overflow_listeners;
	
	/** Notifies subscribed listeners contained in
	 * {@link #numeric_overflow_listeners} using separate thread for each
	 * notification to speed&nbsp;up perfomance. */
	private transient ThreadPoolExecutor listeners_notifier;
	
	
	// Нестатическая инициализация ========================================/////
	{
		counting_has_started = false;
		set_time_counter_value_sign(true);
		thread_counter_init();
		numeric_overflow = false;
		modify_lock = new ReentrantLock();
		event_lock = new ReentrantLock();
		numeric_overflow_listeners = new ArrayList<>();
		listeners_notifier = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0,
				TimeUnit.NANOSECONDS, new LinkedTransferQueue<>());
	}


	///// Конструкторы public =============================================/////
	/**
	 * @param mode_set Режим работы счетчика времени согласно перечислению
	 * {@link time_obj.Mode}.<br>
	 * <b>Важно!</b> Т.к.&nbsp;данный класс реализует <u>исключительно</u>
	 * режим секундомера и режим таймера обратного отсчета без привязки к
	 * будущей дате, конструктор этого класса принимает <u>только</u>
	 * {@link time_obj.Mode#M_stopwatch} или {@link time_obj.Mode#M_countdown} в
	 * качестве параметра.
	 * 
	 * @param period_set Начальные значения лет, месяцев и дней.<br>
	 * Если в качестве режима работы указывается секундомер, данный параметр
	 * будет начальным значением в годах, месяцах и днях. Передача {@code null}
	 * обозначает нулевые значения указанных временных отрезков.<br>
	 * Если в качестве режима работы указывается таймер, данные значения будут
	 * точкой обратного отсчета в годах, месяцах и днях. Передача {@code null}
	 * обозначает нулевые значения указанных временных отрезков.<br>
	 * 
	 * @param duration_set Начальные значения часов, минут, секунд.<br>
	 * Если в качестве режима работы указывается секундомер, данный параметр
	 * будет начальным значением в часах, минутах и секундах. Передача
	 * {@code null} обозначает нулевые значения указанных временных
	 * отрезков.<br>
	 * Если в качестве режима работы указывается таймер, данные значения будут
	 * точкой обратного отсчета в часах, минутах и секундах. Передача
	 * {@code null} обозначает нулевые значения указанных временных
	 * отрезков.<br>
	 * 
	 * @param days_count Режим подсчета времени создаваемого экземпляра класса.
	 * 
	 * @throws ArithmeticException Если во время нормализации объекта типа
	 * {@link Period} произошел выход числа за пределы содержащего его
	 * примитивного типа.
	 * 
	 * @exception IllegalArgumentException Если:
	 * <ul><li>передан неподходящий аргумент для {@code mode_set};</li>
	 * <li>время для установки таймера меньше секунды или передан {@code null}
	 * для обоих аргументов {@code period_set} и {@code duration_set} сразу
	 * (касается установки значения для режима таймера);</li>
	 * <li>{@code period_set} содержит отрицательное значение в своих полях
	 * (метод {@link Period#isNegative()} возвращает {@code true}).</li></ul>
	 * 
	 * @exception NullPointerException Если в качестве аргумента для
	 * {@code mode_set} либо {@code days_count} передан {@code null}.
	 * 
	 * @exception RuntimeException Возникла логическая ошибка в работе
	 * конструктора. Данное исключение не&nbsp;ожидается при нормальной работе.
	 */
	public Solo_counter(final Mode mode_set, final Period period_set,
			final LocalTime duration_set, final Days_in_year days_count)
			throws ArithmeticException
	{
		super(mode_set);
		
		period_init = period_set;
		duration_init = duration_set;
		this.days_count = days_count;
		
		try
		{
			parameters_verifying(true);
		}
		/* При вызове метода "void parameters_verifying(boolean)" с аргументом
		 * true данное исключение не ожидается */
		catch (final InvalidObjectException exc)
		{
			logger.log(Level.SEVERE, "Fatal error.\nReason: Incorrect method"
					+ " behaviour when calling from class constructor."
					+ " Exception stack trace:", exc);
			throw new RuntimeException("Incorrect method behaviour when calling from "
					+ Solo_counter.class.getName() + " class\'s constructor");
		}
		
		period_passed = period_init;
		duration_passed = duration_init;
		set_time_unit_values();
		build_time_string();
	}
	
	
	///// Методы public статические =======================================/////
	/**
	 * Проверяет на корректность и нормализует поле типа {@link Period},
	 * т.е.&nbsp;значения в годах, месяцах и днях.
	 * 
	 * @param period Значение, которое необходимо нормализовать.
	 * 
	 * @param count_mode Режим подсчета дней в месяце, опираясь на который
	 * будет происходить нормализация.
	 * 
	 * @return Нормализованный объект.
	 * 
	 * @throws ArithmeticException Получившаяся в&nbsp;результате нормализации
	 * дата не&nbsp;помещается в объекте типа {@link Period}.
	 * 
	 * @exception NullPointerException В&nbsp;качестве одного из параметров
	 * передан {@code null}.
	 * 
	 * @exception IllegalArgumentException {@code period} содержит хотя&nbsp;бы
	 * одно поле с отрицательным значением ({@link Period#isNegative()}
	 * возвращает {@code true} применительно к переданному аргументу).
	 */
	public static Period normalize_period_obj(
			Period period, final Days_in_year count_mode)
					throws ArithmeticException
	{
		if (period.isNegative())
		{
			throw new IllegalArgumentException(
					"Period object argument has negative date value");
		}
		
		// Кол-во дней в объекте, который нормализуется
		final int days = period.getDays();
		
		period = period.normalized();
		
		// Кол-во дней в месяце, где год состоит из 360-и дней
		final int days_in_month = 30;
		
		/* Если подсчет даты основывается на 360-и днях в году И кол-во дней в
		 * дате отсчета времени больше 30 */
		if (count_mode.equals(Days_in_year.DIY_360) && days > days_in_month)
		{
			period = Period.of(period.getYears(),
					period.plusMonths(days / days_in_month).getMonths(),
					days % days_in_month);
			period = period.normalized();
		}
		// Если подсчет даты основывается на 365-и днях в году И ...
		else if (count_mode.equals(Days_in_year.DIY_365) &&
				/* ... кол-во дней в дате отсчета времени больше чем
				 * дней в определенном (т.е. текущем) месяце */
				days > month_sizes.get(period.getMonths()))
		{
			final int days_in_year = 365;  // Кол-во дней в году
			
			/* Если кол-во дней в дате отсчета больше, чем кол-во дней
			 * в году */
			if (days > days_in_year)
			{
				period = period.plusMonths(days / days_in_year).
						minusDays(days % days_in_year).normalized();
			}
			
			/* Пока кол-во дней в дате отсчета превышает кол-во дней в
			 * определенном (т.е. текущем) месяце - будет происходить
			 * корректировка даты (дни будут вычитаться в пользу месяцев) */
			while (period.getDays() > month_sizes.get(period.getMonths()))
			{
				period = period.minusDays(month_sizes.get(period.getMonths()));
				period = period.plusMonths(1);
				period = period.normalized();
			}
		}

		return period;
	}

	
	///// Методы public экземпляра ========================================/////
	/**
	 * Starts (resumes) time&nbsp;counting.
	 * 
	 * @return {@code true}&nbsp;&#0151; time&nbsp;counting
	 * <u>was</u>&nbsp;successfully started (resumed). {@code false}&nbsp;&#0151;
	 * time&nbsp;counting <u>was&nbsp;not</u>&nbsp;started (resumed). This may
	 * occur due&nbsp;to following reasons:
	 * <ul><li>time&nbsp;counter is already running;</li>
	 * <li>numeric overflow occurred.</li></ul>
	 */
	public boolean start()
	{
		try
		{
			modify_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			// Если (отсчет времени уже хотя бы раз запускался И ...
			if ((thread_counter_executor != null &&
					/* ... отсчет времени сейчас выполняется) ИЛИ счетчик
					 * времени достиг максимально возможного значения */
					!thread_counter_executor.isShutdown()) || numeric_overflow)
			{
				return false;
			}
			
			thread_counter_executor =
					Executors.newSingleThreadScheduledExecutor();
			thread_counter_executor.scheduleAtFixedRate(
					thread_counter, 1, 1, TimeUnit.SECONDS);
			counting_has_started = true;
			
			return true;
		}
		finally
		{
			modify_lock.unlock();
		}
	}
	
	
	/**
	 * Pauses time&nbsp;counting.
	 * 
	 * @return {@code true}&nbsp;&#0151; time&nbsp;counting
	 * <u>was</u>&nbsp;successfully paused. {@code false}&nbsp;&#0151;
	 * time&nbsp;counting <u>was&nbsp;not</u>&nbsp;paused. This may occur if
	 * time&nbsp;counter is already in paused state (whether after calling this
	 * method, due&nbsp;to numeric overflow or if it was&nbsp;not&nbsp;started
	 * yet).
	 */
	public boolean pause()
	{
		// Если отсчет времени ни разу не запускался
		if (thread_counter_executor == null)
		{
			return false;
		}
		
		thread_counter_executor.shutdown();
		
		try
		{
			// TODO: Change waiting to 1/10 second using special static field
			thread_counter_executor.awaitTermination(1, TimeUnit.SECONDS);
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		finally
		{
			// Если поток не был остановлен в течении времени ожидания
			if (!thread_counter_executor.isTerminated())
			{
				logger.log(Level.WARNING,
						"Forcible thread termination due to long waiting");
				thread_counter_executor.shutdownNow();
			}
		}
		
		return true;
	}
	
	
	/**
	 * Sets time&nbsp;counter to its initial state (sets time&nbsp;value with
	 * which it was&nbsp;created). This method <u>does&nbsp;not</u> resume
	 * counting if time&nbsp;counter was&nbsp;paused (whether by calling
	 * {@link #pause()} or due&nbsp;to numeric overflow).<br>
	 * <i>Notes.</i>
	 * <ul><li>If time&nbsp;counter is already (or yet) in its intital state,
	 * this method <u>does&nbsp;nothing.</u></li>
	 * <li>Subsequent time correction
	 * (using {@link #time_values_correction(long, boolean)}) after instance
	 * creation <u>does&nbsp;not</u> affect to its initial time&nbsp;set.</li></ul>
	 * <i>Perfomance note.</i> Contains synchronized sections.
	 */
	public void restart()
	{
		try
		{
			modify_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			// Если отсчет времени еще не начинался
			if (!counting_has_started)
			{
				return;
			}
			
			period_passed = period_init;
			duration_passed = duration_init;
			
			// Если ход счетчика времени сейчас приостановлен
			if (thread_counter_executor.isShutdown())
			{
				counting_has_started = false;
				numeric_overflow = false;
			}

			set_time_counter_value_sign(true);
			set_time_unit_values();
			build_time_string();
			time_counter_text_listeners_notification();
		}
		finally
		{
			modify_lock.unlock();
		}
	}
	
	
	/**
	 * Корректировка значений счетчика времени. В зависимости от переданного
	 * аргумента добавляет либо вычитает переданное кол&#8209;во времени.<br>
	 * <i>В&nbsp;общем случае <u>вычитание</u> времени приводит к смещению
	 * счетчика времени <u>назад</u> по временной оси; и наоборот:
	 * <u>добавление</u> времени приводит к смещению счетчика времени
	 * <u>вперед</u> по временной оси.</i><br>
	 * В режиме {@link time_obj.Mode#M_stopwatch} вычитание времени, <u>большего
	 * чем значение счетчика времени</u>, приведет к его <u>обнулению</u> (все
	 * равно что секундомер запущен заново).<br>
	 * В режиме {@link time_obj.Mode#M_countdown} <u>вычитание</u> времени
	 * приводит к <u>увеличению</u> оставшегося времени (в&nbsp;случае уже
	 * <u>наступившего нулевого значения</u>&nbsp;&#0151; приводит к
	 * <u>уменьшению</u> прошедшего времени после нулевого значения).
	 * <u>Добавление</u> времени приводит к <u>уменьшению</u> оставшегося
	 * времени (в&nbsp;случае уже <u>наступившего нулевого
	 * значения</u>&nbsp;&#0151; приводит к <u>увеличению</u> прошедшего
	 * времени после нулевого значения).<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param seconds_amount Корректирующее значение в секундах.
	 * <u>Не&nbsp;может иметь отрицательное значение</u>.
	 * 
	 * @param add {@code true}&nbsp;&#0151; переданное значение времени
	 * добавляется; {@code false}&nbsp;&#0151; вычитается.
	 * 
	 * @return {@code true}&nbsp;&#0151; операция выполнена успешно;
	 * {@code false}&nbsp;&#0151; значение счетчика времени, получившееся
	 * в&nbsp;результате корректировки слишком большое (при&nbsp;этом остается
	 * прежнее значение счетчика времени).
	 * 
	 * @exception IllegalArgumentException Параметр {@code seconds_amount}
	 * меньше нуля.
	 */
	public boolean time_values_correction(
			final long seconds_amount, final boolean add)
	{
		// Если параметр значения секунд отрицательный
		if (seconds_amount < 0)
		{
			throw new IllegalArgumentException(
					"Correction seconds value is negative");
		}
		
		/* Кол-во секунд, содержащихся в полях "period_passed" и
		 * "duration_passed" */
		long seconds_passed;
		// Кол-во секунд в сутках
		final long seconds_in_day = TimeUnit.DAYS.toSeconds(1);
		
		try
		{
			modify_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			// Если режимом подсчета установлено 30 дней в каждом месяце
			if (days_count.equals(Days_in_year.DIY_360))
			{
				// Кол-во дней в месяце где год состоит из 360-и дней
				final int days_in_month = 30;
				
				seconds_passed = (period_passed.toTotalMonths() * days_in_month +
						period_passed.getDays()) * seconds_in_day;
			}
			/* Если режимом подсчета установлено кол-во дней в месяце,
			 * соответствующее месяцу */
			else
			{
				final int days_in_year = 365;  // Кол-во дней в году
				
				seconds_passed =
						period_passed.getYears() * days_in_year * seconds_in_day;
				
				/* Добавление кол-ва секунд из каждого наступившего месяца поля
				 * "period_passed" */
				for (int i = 0, months = period_passed.getMonths(); i < months; i++)
				{
					seconds_passed += month_sizes.get(i) * seconds_in_day;
				}
			}
			
			seconds_passed += period_passed.getDays() * seconds_in_day +
					duration_passed.toSecondOfDay();
			
			/* Скорректированное кол-во секунд, которое будет содержаться в полях
			 * "period_passed" и "duration_passed" */
			long seconds_corrected;
			
			// Если необходимо добавить время
			if (add)
			{
				/* Если экземпляр класса работает в режиме секундомера ИЛИ это
				 * режим таймера, и нулевое время уже было достигнуто */
				if (instance_mode.equals(Mode.M_stopwatch) || !is_positive_value())
				{
					try
					{
						seconds_corrected =
								Math.addExact(seconds_passed, seconds_amount);
					}
					catch (final ArithmeticException exc)
					{
						return false;
					}
				}
				/* Фактически время отнимается, т.к. это режим таймера и нулевое
				 * время еще не было достигнуто */
				else
				{
					seconds_corrected = seconds_passed - seconds_amount;
					
					/* Если в результате корректировки времени была пересечена
					 * нулевая точка времени */
					if (seconds_corrected < 0)
					{
						set_time_counter_value_sign(false);
						seconds_corrected = Math.abs(seconds_corrected);
					}
				}
			}
			else
			{
				/* Если экземпляр класса работает в режиме секундомера ИЛИ это
				 * режим таймера, и нулевое время уже было достигнуто */
				if (instance_mode.equals(Mode.M_stopwatch) || !is_positive_value())
				{
					seconds_corrected = seconds_passed - seconds_amount;
					
					/* Если в результате корректировки времени было пересечено
					 * нулевое время И экземпляр класса работает в режиме
					 * секундомера */
					if (seconds_corrected <= 0 &&
							instance_mode.equals(Mode.M_stopwatch))
					{
						period_passed = Period.of(0, 0, 0);
						duration_passed = LocalTime.of(0, 0, 0);
						set_time_unit_values();
						build_time_string();
						time_counter_text_listeners_notification();
						
						return true;
					}
					/* Если в результате корректировки времени было пересечено
					 * нулевое время И экземпляр класса работает в режиме таймера */
					else if (seconds_passed < 0 &&
							instance_mode.equals(Mode.M_countdown))
					{
						set_time_counter_value_sign(true);
						seconds_corrected = Math.abs(seconds_corrected);
					}
				}
				/* Фактически время добавляется, т.к. это режим таймера и нулевое
				 * время еще не было достигнуто */
				else
				{
					try
					{
						seconds_corrected =
								Math.addExact(seconds_passed, seconds_amount);
					}
					catch (final ArithmeticException exc)
					{
						return false;
					}
				}
			}
			
			// Кол-во полных дней, получившееся из скорректированного кол-ва секунд
			final long total_days = seconds_corrected / seconds_in_day;
			/* Сохранение старого значения лет/месяцев/дней перед нормализацией
			 * на случай исключения */
			final Period buffer = period_passed;
			
			/* Если получившееся кол-во полных дней больше макс. числа,
			 * помещающегося в int */
			if (total_days > Integer.MAX_VALUE)
			{
				// Кол-во дней в году согласно режиму подсчета времени экземпляра
				final int days_in_year =
						(days_count.equals(Days_in_year.DIY_360) ? 360 : 365);
				// Кол-во полных лет, получившееся из кол-ва полных дней "total_days"
				final long total_years = total_days / days_in_year;
				
				/* Если получившееся кол-во полных лет больше макс. числа
				 * помещающегося в int */
				if (total_years > Integer.MAX_VALUE)
				{
					return false;
				}
				
				period_passed = Period.of(
						(int)total_years, 0, (int)(total_days % days_in_year));
			}
			else
			{
				period_passed = Period.of(0, 0, (int)total_days);
			}
			
			try
			{
				period_passed = normalize_period_obj(period_passed, days_count);
			}
			catch (final ArithmeticException exc)
			{
				period_passed = buffer;
				
				return false;
			}
			
			// Кол-во секунд в оставшихся неполных сутках
			int day_seconds = (int)(seconds_corrected % seconds_in_day);
			// Секунд в одном часе
			final int seconds_in_hour = (int)TimeUnit.HOURS.toSeconds(1);
			// Сокрректированное кол-во часов
			final int hours = day_seconds / seconds_in_hour;
			
			day_seconds %= seconds_in_hour;
			
			// Кол-во минут в часе
			final int minutes_in_hour = (int)TimeUnit.HOURS.toMinutes(1);
			// Скорректированное кол-во минут
			final int minutes = day_seconds / minutes_in_hour;
			
			duration_passed =
					LocalTime.of(hours, minutes, day_seconds % minutes_in_hour);
			set_time_unit_values();
			build_time_string();
			time_counter_text_listeners_notification();
			
			return true;
		}
		finally
		{
			modify_lock.unlock();
		}
	}
	
	
	/**
	 * Lets know whether counting had started after creation (or after restart
	 * while the&nbsp;time&nbsp;counter was stopped).
	 * 
	 * @return {@code true}&nbsp;&#0151; time&nbsp;counter had started;
	 * {@code false}&nbsp;&#0151; had&nbsp;not.
	 */
	public boolean counting_had_started()
	{
		return counting_has_started;
	}
	
	
	/**
	 * Lets know whether time&nbsp;counter is currently running.<br>
	 * <i>Note.</i> Time&nbsp;counter can be stopped not&nbsp;only after
	 * {@link #pause()} method calling. It also stops if numeric overflow occurs
	 * (method {@link #numeric_overflow_status()} returns {@code true}).
	 * 
	 * @return {@code true}&nbsp;&#0151; time&nbsp;counter currently running;
	 * {@code false}&nbsp;&#0151; is&nbsp;paused.
	 */
	public boolean is_running()
	{
		// If time counter is running
		if (thread_counter_executor != null && !thread_counter_executor.isShutdown())
		{
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * When time&nbsp;counter reaches its maximum possible value it stops
	 * counting and sets numeric overflow status to {@code true}. In this case
	 * calling {@link #start()} <u>does&nbsp;nothing</u>. But time&nbsp;counter
	 * can be restarted by calling {@link #restart()}.<br>
	 * <i>Note.</i> Interested in numeric overflow event object can subscribe
	 * for the&nbsp;event using
	 * {@link #add_Numeric_overflow_listener(Numeric_overflow_listener)}.
	 * 
	 * @return {@code true}&nbsp;&#0151; numeric overflow occurred;
	 * {@code false}&nbsp;&#0151; time&nbsp;counter is&nbsp;in&nbsp;normal state.
	 */
	public boolean numeric_overflow_status()
	{
		return numeric_overflow;
	}
	
	
	/**
	 * @return Initial time&nbsp;counter's values.
	 */
	public Initial_time_values get_initial_time_values()
	{
		return new Initial_time_values(period_init, duration_init);
	}
	
	
	/* TODO: ? Is there possible resource leak if object has not unsubscribed
	 * from this event notifying, but is not referred anymore else? If so - need
	 * to mention this in javadoc */
	/**
	 * Adds specified {@code listener} to receive numeric overflow event. Same
	 * {@code listener} <u>can</u> be&nbsp;added multiple times.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with (private methods <u>are&nbsp;not</u> listed):
	 * <ul><li>{@link #remove_Numeric_overflow_listener(Numeric_overflow_listener)}.</li></ul>
	 * 
	 * @param listener Listener to be subscribed on event.
	 * 
	 * @exception NullPointerException {@code listener} argument is {@code null}.
	 */
	public void add_Numeric_overflow_listener(
			final Numeric_overflow_listener listener)
	{
		// Argument cannot be null
		if (listener == null)
		{
			throw new NullPointerException(
					Numeric_overflow_listener.class.getName() + " argument is null");
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
			numeric_overflow_listeners.add(listener);
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
	 * with (private methods <u>are&nbsp;not</u> listed):
	 * <ul><li>{@link #add_Numeric_overflow_listener(Numeric_overflow_listener)}.</li></ul>
	 * 
	 * @param listener Listener to be unsubscribed from event.
	 * 
	 * @return {@code true}&nbsp;&#0151; <u>first occurrence</u> of
	 * {@code listener} argument <u>successfully removed</u> (unsubscribed) from
	 * event notifying. {@code false}&nbsp;&#0151; <u>there&nbsp;is no</u> such
	 * {@code listener} (i.e.&nbsp;nothing to remove).
	 * 
	 * @exception NullPointerException {@code listener} argumetn is {@code null}.
	 */
	public boolean remove_Numeric_overflow_listener(
			final Numeric_overflow_listener listener)
	{
		// Argument cannot be null
		if (listener == null)
		{
			throw new NullPointerException(
					Numeric_overflow_listener.class.getName() + " argument is null");
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
			final boolean is_removed =
					numeric_overflow_listeners.remove(listener);
			
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
	
	
	///// Методы private экземпляра =======================================/////
	/**
	 * Устанавливает значения в контейнер {@link Time_counter#time_unit_values}.<br>
	 * 
	 * @exception NullPointerException Если {@link #period_passed} или
	 * {@link #duration_passed} ссылается на {@code null}.
	 */
	private void set_time_unit_values()
	{
		time_unit_values.put(
				Time_unit_name.TUN_years, (long)period_passed.getYears());
		time_unit_values.put(
				Time_unit_name.TUN_months, (long)period_passed.getMonths());
		time_unit_values.put(
				Time_unit_name.TUN_days, (long)period_passed.getDays());
		time_unit_values.put(
				Time_unit_name.TUN_hours, (long)duration_passed.getHour());
		time_unit_values.put(
				Time_unit_name.TUN_minutes, (long)duration_passed.getMinute());
		time_unit_values.put(
				Time_unit_name.TUN_seconds, (long)duration_passed.getSecond());
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
	 * @exception InvalidObjectException Поле десериализованного объекта
	 * не&nbsp;прошло валидацию.
	 */
	private void readObject(ObjectInputStream input_stream)
			throws IOException, ClassNotFoundException
	{
		input_stream.defaultReadObject();
		
		// Если данные поля после десериализации ссылаются на null
		if (days_count == null || period_passed == null || duration_passed == null)
		{
			throw new InvalidObjectException(
					"At least one of deserialized fields is null");
		}

		parameters_verifying(false);
		
		try
		{
			period_passed = normalize_period_obj(period_passed, days_count);
		}
		catch (final ArithmeticException exc)
		{
			logger.log(Level.SEVERE, "Deserialized object has incorrect "
					+ Period.class.getName() + " field which causes"
					+ " numeric overflow. Deserialized object cannot be used."
					+ " Exception stack trace:", exc);
			throw new InvalidObjectException("Numeric overflow occurred while"
					+ " normalizing deserialized Period type object");
		}
		
		/* Если счетчик времени переполнен - выясняется действительно ли это
		 * правда а не подделка сериализованного объекта */
		if (numeric_overflow)
		{
			/* Кол-во дней в последнем (12-м) месяце, вызывающее переполнение.
			 * Зависит от режима подсчета дней экземпляра */
			final int days_overload =
					(instance_mode.equals(Days_in_year.DIY_360) ? 31 : 32);

			/* Если значение счетчика времени не соответствует максимально
			 * возможному значению (значению которое привело к числовому
			 * переполнению счетчика времени) */
			if (!period_passed.equals(
						Period.of(Integer.MAX_VALUE, 11, days_overload)) ||
					!duration_passed.equals(LocalTime.of(0, 0, 0)))
			{
				throw new InvalidObjectException("Discrepancy between numeric"
						+ " overflow flag and time counter value");
			}
			
			build_time_string(numeric_overflow_message);
		}
		
		/* В режиме секундомера значение счетчика времени может быть только
		 * положительным */
		if (instance_mode.equals(Mode.M_stopwatch))
		{
			set_time_counter_value_sign(true);
		}
		
		set_time_unit_values();
		thread_counter_init();
		modify_lock = new ReentrantLock();
		event_lock = new ReentrantLock();
		numeric_overflow_listeners = new ArrayList<>();
		listeners_notifier = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0,
				TimeUnit.NANOSECONDS, new LinkedTransferQueue<>());
	}
	
	
	/**
	 * Вспомогательный метод для
	 * {@link #Solo_counter(Mode, Period, LocalTime, Days_in_year)} и
	 * {@link #readObject(ObjectInputStream)}, выполняющий проверку полей
	 * {@link Time_counter#instance_mode}, {@link #period_init} и
	 * {@link #duration_init} на соответствие требованиям класса.
	 * 
	 * @param called_from_constructor Определяет типы исключений, которые будут
	 * генерироваться при проверке. {@code true}&nbsp;&#0151; данный метод был
	 * вызван из конструктора, генерируются {@link ArithmeticException},
	 * {@link IllegalArgumentException} и {@link NullPointerException}.
	 * {@code false}&nbsp;&#0151; данный метод был вызван из
	 * {@link #readObject(ObjectInputStream)}, генерируется
	 * {@link InvalidObjectException}.
	 * 
	 * @throws InvalidObjectException Поле десериализованного объекта
	 * не&nbsp;прошло валидацию.
	 * 
	 * @exception ArithmeticException Если во время нормализации объекта типа
	 * {@link Period} произошел выход числа за пределы содержащего его
	 * примитивного типа.
	 * 
	 * @exception IllegalArgumentException Если:
	 * <ul><li>значение {@link Time_counter#instance_mode} не&nbsp;подходит для
	 * этого класса;</li>
	 * <li>время для установки в режиме
	 * {@link time_obj.Mode#M_countdown} меньше секунды или {@link #period_init}
	 * и {@link #duration_init} ссылаются на {@code null} одновременно (касается
	 * режима {@link time_obj.Mode#M_countdown});</li>
	 * <li>{@link #period_init} содержит отрицательное значение в своих полях
	 * (метод {@link Period#isNegative()} возвращает true).</li></ul>
	 * 
	 * @exception NullPointerException Если {@link Time_counter#instance_mode}
	 * ссылается на {@code null}.
	 */
	private void parameters_verifying(final boolean called_from_constructor)
			throws InvalidObjectException
	{
		try
		{
			// Если передан недопустимый параметр согласно перечислению "Mode"
			if (!instance_mode.equals(Mode.M_stopwatch) &&
					!instance_mode.equals(Mode.M_countdown))
			{
				// Если метод был вызван конструктором
				if (called_from_constructor)
				{
					throw new IllegalArgumentException(
							"Unsupported mode for this object");					
				}
				else
				{
					throw new InvalidObjectException(
							"Incompatible time mode for Solo_counter object");
				}
			}			
		}
		catch (final NullPointerException exc)
		{
			// Если метод был вызван конструктором
			if (called_from_constructor)
			{
				logger.log(Level.SEVERE, "Fatal error.\nReason: "
						+ Mode.class.getName() + " object passed to this"
						+ " class\'s constructor is null. Exception stack trace:", exc);
				throw exc;
			}
			else
			{
				logger.log(Level.SEVERE, "Deserialized object field "
						+ Mode.class.getName() + " is null. This deserialized"
						+ " object cannot be used. Exception stack trace:", exc);
				throw new InvalidObjectException(
						"Deserialized instance_mode field is null");
			}
		}
		
		// Если выбран режим таймера обратного отсчета без привязки к дате
		if (instance_mode.equals(Mode.M_countdown))
		{
			/* true - ссылка хотя бы одного из параметров ("period_init" или
			 * "duration_init") не равна null; false - оба параметра ссылаются
			 * на null */
			boolean time_exists = false;
			/* true - в параметре с кол-вом лет, месяцев и дней все указанные
			 * значения равны 0 (останется ИСТИНОЙ, если указанный параметр
			 * конструктора будет null); false - хотя бы одно значение из
			 * указанных не равно нулю */
			boolean period_init_is_zero = true;
			
			// Если параметр с кол-вом лет, месяцев и дней не ссылается на null
			if (period_init != null)
			{
				period_init_is_zero = period_init.isZero();
				time_exists = true;
			}
			
			// Если параметр с кол-вом часов, минут и секунд не ссылается на null
			if (duration_init != null)
			{
				/* Если кол-во лет, месяцев и дней равно 0 И значение
				 * часов/минут/секунд меньше одной секунды */
				if (period_init_is_zero &&
						duration_init.compareTo(LocalTime.of(0, 0, 1)) < 0)
				{
					// Если метод вызван конструктором
					if (called_from_constructor)
					{
						throw new IllegalArgumentException(
								"Wrong time set for countdown mode");
					}
					else
					{
						throw new InvalidObjectException(
								"Initial time for countdown mode is set to 0");
					}
				}
				
				time_exists = true;
			}
			
			/* Если оба параметра, отвечающих за значение времени
			 * ("period_init" и "duration_init"), ссылаются на null */
			if (!time_exists)
			{
				// Если метод был вызван конструктором
				if (called_from_constructor)
				{
					throw new IllegalArgumentException(
							"Wrong time set for countdown mode");
				}
				else
				{
					throw new InvalidObjectException(
							"Initial time for countdown mode is set to 0");
				}
			}
		}
		
		// Если параметр лет/месяцев/дней задан
		if (period_init != null)
		{
			try
			{
				period_init = normalize_period_obj(period_init, days_count);
			}
			catch (final ArithmeticException | IllegalArgumentException exc)
			{
				// Если метод был вызван конструктором
				if (called_from_constructor)
				{
					logger.log(Level.SEVERE, "Fatal error.\nReason: Incorrect "
							+ Period.class.getName() + " object passed to this"
							+ " class\'s constructor. Exception stack trace:", exc);
					throw exc;
				}
				else
				{
					logger.log(Level.SEVERE, "Decerialized object\'s field of type "
							+ Period.class.getName() + " is incorrect. This"
							+ " deserizlized object cannot be used. Exception"
							+ " stack trace:", exc);
					throw new InvalidObjectException("Deserialized "
							+ Period.class.getName() + " type object is incorrect");
				}
			}
		}
		else
		{
			period_init = Period.of(0, 0, 0);
		}

		
		// Если параметр часов/минут/секунд не задан
		if (duration_init == null)
		{
			duration_passed = LocalTime.of(0, 0, 0);
		}
	}
	
	
	/**
	 * Инициализирует поле {@link #thread_counter}.
	 */
	private void thread_counter_init()
	{
		thread_counter = new Runnable()
		{
			@Override
			public void run()
			{
				/* TODO: Реализовать обращение к данному потоку раз в 1/10
				 * секунды */
				
				try
				{
					modify_lock.lockInterruptibly();
				}
				catch (final InterruptedException exc)
				{
					logger.log(Level.INFO, "Tread interrupts. Exception stack trace:", exc);
					Thread.currentThread().interrupt();
				}
				
				try
				{
					/* Если экземпляр класса работает в режиме секундомера ИЛИ
					 * это режим таймера, и нулевое время уже было достигнуто */
					if (instance_mode.equals(Mode.M_stopwatch) || !is_positive_value())
					{
						duration_passed = duration_passed.plusSeconds(1);
						
						// Если суточное время обнулилось (наступило 0:00:00)
						if (duration_passed.toSecondOfDay() == 0)
						{
							period_passed = period_passed.plusDays(1);
							
							try
							{
								period_passed = normalize_period_obj(
										period_passed, days_count);
							}
							catch(final ArithmeticException exc)
							{
								numeric_overflow = true;
								build_time_string(numeric_overflow_message);
								pause();
								numeric_overflow_listeners_notification();
								
								return;
							}
						}
					}
					// Instance works in timer mode and zero time isn't reached yet
					else
					{
						duration_passed = duration_passed.minusSeconds(1);
						
						/* Время в секундах, соответствующее 23:59:59 в суточном
						 * эквиваленте (24часа * 60минут * 60секунд - 1секунда) */
						final long one_second_less = TimeUnit.DAYS.toSeconds(1) - 1;
						
						// Если суточное время - 23:59:59
						if (duration_passed.toSecondOfDay() == one_second_less)
						{
							period_passed = period_passed.minusDays(1);
							
							/* Кол-во оставшихся месяцев в значении таймера
							 * обратного отсчета */
							final int months_remain = period_passed.getMonths();
							/* Кол-во оставшихся лет в значении таймера
							 * обратного отсчета */
							final int years_remain = period_passed.getYears();
							
							/* Если в результате декремента дня кол-во дней
							 * стало отрицательным И ... */
							if (period_passed.getDays() == -1 &&
									/* ... (кол-во месяцев ИЛИ лет все еще
									 * положительное) */
									(months_remain > 0 || years_remain > 0))
							{
								/* Если подсчет даты основывается на 360-и днях
								 * в году */
								if (days_count.equals(Days_in_year.DIY_360))
								{
									period_passed = period_passed.plusDays(31);
								}
								else
								{
									period_passed = period_passed.plusDays(
											month_sizes.get(months_remain - 1) + 1);
								}
								
								period_passed = period_passed.minusMonths(1);
								period_passed = period_passed.normalized();
							}
							// Если кол-во оставшихся месяцев И лет равно нулю
							else if (months_remain == 0 && years_remain == 0)
							{
								duration_passed = duration_passed.plusSeconds(2);
								set_time_counter_value_sign(false);
							}
						}
					}
					
					set_time_unit_values();
					build_time_string();
					time_counter_text_listeners_notification();
				}
				finally
				{
					modify_lock.unlock();
				}
			}
		};
	}
	
	
	/**
	 * Nofifies listeners subscribed via {@link Numeric_overflow_listener}
	 * interface about numeric overflow occurred.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #add_Numeric_overflow_listener(Numeric_overflow_listener)};</li>
	 * <li>{@link #remove_Numeric_overflow_listener(Numeric_overflow_listener)}.</li></ul>
	 */
	private void numeric_overflow_listeners_notification()
	{
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
			// This object's reference to be passed with event
			final Solo_counter instance = this;
			
			// Listeners notification
			for (final Numeric_overflow_listener i : numeric_overflow_listeners)
			{
				listeners_notifier.execute(new Runnable()
				{
					@Override
					public void run()
					{
						i.numeric_overflow_occurred(
								new Numeric_overflow_event(instance));
					}
				});
			}
		}
		finally
		{
			event_lock.unlock();
		}
	}
}
