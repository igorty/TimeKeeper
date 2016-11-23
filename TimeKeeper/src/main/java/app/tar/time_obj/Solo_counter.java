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
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.tar.time_obj.containers.Modified_ArrayList;
import app.tar.time_obj.events.Numeric_overflow_event;
import app.tar.time_obj.events.Numeric_overflow_listener;


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
	/** Serializable class version number. */
	private static final long serialVersionUID = 6927588936635159313L;


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
	
	/** Time in milliseconds representing frequency of executing
	 * {@link #thread_counter} runnable. */
	private static final int clock_frequency;
	
	
	static
	{
		logger = Logger.getLogger(Solo_counter.class.getName());
		clock_frequency = 100;
	}
	
	
	///// Поля private экземпляра =========================================/////
	/** Initial years, months and days values.
	 * 
	 * @serial Verified after deserialization.
	 * <p><i>Checking conditions.</i>
	 * <ul><li>The field <u>can</u> be {@code null} if at&nbsp;least one of
	 * the&nbsp;following statements are correct:
	 * <ul><li>class instance works in {@link Mode#M_stopwatch} mode;</li>
	 * <li>{@link #duration_init} field <u>is&nbsp;not</u>
	 * {@code null}.</li></ul></li>
	 * <li>This field's {@link Period#isNegative()} method <u>must</u> return
	 * {@code false}.</li>
	 * <li>This field's normalizing ({@link Period#normalized()})
	 * <u>must&nbsp;not</u> throw {@link ArithmeticException}.</li></ul>
	 * If field does&nbsp;not correspond to listed conditions
	 * {@link InvalidObjectException} is thrown. */
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
	/** Initial hours, minutes and seconds values.
	 * 
	 * @serial Verified after deserialization.
	 * <p><i>Checking conditions.</i>
	 * <ul><li>The field <u>can</u> be {@code null} if at&nbsp;least one of
	 * the&nbsp;following statements are correct:
	 * <ul><li>class instance works in {@link Mode#M_stopwatch} mode;</li>
	 * <li>{@link #period_init} field <u>is&nbsp;not</u>
	 * {@code null}.</li></ul></li></ul>
	 * If field does&nbsp;not correspond to listed conditions
	 * {@link InvalidObjectException} is thrown. */
	private LocalTime duration_init;
	/** <u>Текущие</u> значения в часах, минутах и секундах.
	 * 
	 * @serial Верифицируется после десериализации. Не&nbsp;должен быть
	 * {@code null}. При несоответствии условию генерируется исключение
	 * {@link InvalidObjectException}. */
	private LocalTime duration_passed;
	
	/** Represents tenth&#8209;of&#8209;second part for inner purposes. When
	 * this field equals&nbsp;{@code 10}, {@link #thread_counter} runnable ticks
	 * one&nbsp;second.
	 * 
	 * @serial Must be in range <u>from&nbsp;{@code 0}&nbsp;to&nbsp;{@code 9}</u>
	 * after deserialization. If condition <u>is&nbsp;not met</u>, field is set
	 * to&nbsp;{@code 0} and {@link Time_counter#deserialization_status} field
	 * is set to {@code false}. */
	private volatile int tenth_of_second;
	
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
	/** Synchronizes access to {@link #numeric_overflow_listeners}. */
	private transient ReentrantLock event_lock;
	
	/** Contains listeners subscribed for numeric overflow event. */
	private transient ArrayList<Numeric_overflow_listener> numeric_overflow_listeners;
	
	/** {@code true} &#0151; {@link #shutdown()} method
	 * has&nbsp;been&nbsp;invoked; {@code false}&nbsp;&#0151; otherwise. */
	private transient volatile boolean is_shutdown;
	
	
	// Нестатическая инициализация ========================================/////
	{
		tenth_of_second = 0;
		counting_has_started = false;
		set_time_counter_value_sign(true);
		thread_counter_init();
		numeric_overflow = false;
		modify_lock = new ReentrantLock();
		event_lock = new ReentrantLock();
		numeric_overflow_listeners = new ArrayList<>();
		is_shutdown = false;
	}


	///// Конструкторы public =============================================/////
	/**
	 * This constructor takes time&nbsp;counter layout parameters from
	 * {@link Settings}&nbsp;object.
	 * 
	 * @param mode Mode in which this time&nbsp;counter runs.<br>
	 * <b>Important!</b> Since this class implements <i>stopwatch</i> and
	 * <i>timer</i> modes, this argument can be {@link Mode#M_stopwatch} or
	 * {@link Mode#M_countdown} <u>only</u>.
	 * 
	 * @param initial_period Initial years, months and days values. Passing
	 * {@code null} means these time&nbsp;values are set to&nbsp;{@code 0}.
	 * 
	 * @param initial_duration Initial hours, minutes and seconds values.
	 * Passing {@code null} means these time&nbsp;values are set
	 * to&nbsp;{@code 0}.
	 * 
	 * @param days_count Days&#8209;in&#8209;year/days&#8209;in&#8209;month
	 * counting type.
	 * 
	 * @throws ArithmeticException Numeric overflow occurred while
	 * {@link Period} object normalization.
	 * 
	 * @exception IllegalArgumentException If:
	 * <ul><li>inappropriate argument passed for {@code mode};</li>
	 * <li>in case of <u>timer mode</u> {@code null} is passed for
	 * {@code initial_period} and {@code initial_duration} at&nbsp;once, or
	 * initial timer time&nbsp;value <u>is&nbsp;less</u> than one&nbsp;second;</li>
	 * <li>{@code initial_period} contains negative time&nbsp;value
	 * (i.e.&nbsp;{@link Period#isNegative()} for this argument returns
	 * {@code true}).</li></ul>
	 * 
	 * @exception NullPointerException At least one of {@code mode} or
	 * {@code days_count} arguments is {@code null}.
	 * 
	 * @exception RuntimeException Logic error occurred in constructor behavior.
	 * Such exception <u>is&nbsp;not expected</u> during normal operation.
	 */
	public Solo_counter(final Mode mode, final Period initial_period,
			final LocalTime initial_duration, final Days_in_year days_count)
			throws ArithmeticException
	{
		super(mode);
		
		period_init = initial_period;
		duration_init = initial_duration;
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
	 * @param initial_period Initial years, months and days values. Passing
	 * {@code null} means these time&nbsp;values are set to&nbsp;{@code 0}.
	 * 
	 * @param initial_duration Initial hours, minutes and seconds values.
	 * Passing {@code null} means these time&nbsp;values are set
	 * to&nbsp;{@code 0}.
	 * 
	 * @param days_count Days&#8209;in&#8209;year/days&#8209;in&#8209;month
	 * counting type.
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
	public Solo_counter(final Mode mode, final Period initial_period,
			final LocalTime initial_duration, final Days_in_year days_count,
			final Time_display_style time_display_style,
			final Time_unit_name leftmost_displayed_time_unit,
			final Time_unit_name rightmost_displayed_time_unit,
			final Time_unit_layout time_unit_layout)
	{
		super(mode, time_display_style, leftmost_displayed_time_unit,
				rightmost_displayed_time_unit, time_unit_layout);
		
		period_init = initial_period;
		duration_init = initial_duration;
		this.days_count = days_count;
		
		try
		{
			parameters_verifying(true);
		}
		/* This exception is not expected in case of calling
		 * "parameters_verifying(boolean)" method with true argument */
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
		Time_counter_control.get_instance().get_time_counters().add(this);
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
	 * <li>numeric overflow occurred;</li>
	 * <li>{@link #shutdown()} method has&nbsp;been&nbsp;invoked.</li></ul>
	 */
	public boolean start()
	{
		// If "shutdown()" method has been invoked already
		if (is_shutdown)
		{
			return false;
		}
		
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
			thread_counter_executor.scheduleAtFixedRate(thread_counter,
					clock_frequency, clock_frequency, TimeUnit.MILLISECONDS);
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
	 * time&nbsp;counting <u>was&nbsp;not</u>&nbsp;paused. This may occur
	 * due&nbsp;to following reasons:
	 * <ul><li>time&nbsp;counter is already in paused state because of:
	 * <ul><li>the method has&nbsp;been&nbsp;invoked previously and
	 * {@link #start()} method has&nbsp;not&nbsp;been called after that;</li>
	 * <li>numeric overflow;</li>
	 * <li>time counter simply has&nbsp;not&nbsp;been&nbsp;started yet;</li></ul></li>
	 * <li>{@link #shutdown()} method has&nbsp;been&nbsp;invoked.</li></ul>
	 */
	public boolean pause()
	{
		/* I didn't forget to check "is_shutdown" field value here. In this
		 * implementation it's not needed */
		
		// Если отсчет времени ни разу не запускался
		if (thread_counter_executor == null)
		{
			return false;
		}
		
		thread_counter_executor.shutdown();
		
		try
		{
			thread_counter_executor.awaitTermination(
					clock_frequency, TimeUnit.MILLISECONDS);
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
	 * <ul><li>This method <u>does&nbsp;nothing</u> in next cases:
	 * <ul><li>time&nbsp;counter is already (or yet) in its initial state;</li>
	 * <li>{@link #shutdown()} method has&nbsp;been&nbsp;invoked.</li></ul></li>
	 * <li>Subsequent time correction
	 * (using {@link #time_values_correction(long, boolean)}) after instance
	 * creation <u>does&nbsp;not</u> affect to its initial time&nbsp;set.</li></ul>
	 * <i>Performance note.</i> Contains synchronized sections.
	 */
	public void restart()
	{
		// If "shutdown()" method has been invoked already
		if (is_shutdown)
		{
			return;
		}
		
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
			tenth_of_second = 0;
			
			// Если ход счетчика времени сейчас приостановлен
			if (thread_counter_executor == null ||
					thread_counter_executor.isShutdown())
			{
				counting_has_started = false;
				numeric_overflow = false;
			}

			set_time_counter_value_sign(true);
			set_time_unit_values();
			build_time_string();
			notify_time_counter_text_listeners();
		}
		finally
		{
			modify_lock.unlock();
		}
	}
	
	
	/**
	 * Time counter's value adjustment. Adds or subtracts (depending on
	 * {@code add} argument) passed {@code seconds_amount} time&nbsp;amount.
	 * <p>In general, time <u>subtraction</u> results&nbsp;in time&nbsp;counter
	 * value shifting <u>back</u> on the&nbsp;time&nbsp;axis; and
	 * vice&nbsp;versa: <u>addition</u> results&nbsp;in time&nbsp;counter value
	 * shifting <u>forward</u> on the&nbsp;time&nbsp;axis.
	 * <p><u>Bigger</u> than time&nbsp;counter's time&nbsp;value subtraction in
	 * {@link Mode#M_stopwatch} mode results&nbsp;in its <u>zeroing</u>
	 * (as&nbsp;it starts from beginning).
	 * <p>Time <u>subtraction</u> in {@link Mode#M_countdown} mode
	 * results&nbsp;in remaining time <u>increasing</u> (hence if timer
	 * <u>reached zero</u> already, time subtraction results&nbsp;in passed time
	 * <u>decreasing</u>). Time <u>addition</u> results&nbsp;in remaining time
	 * <u>decreasing</u> (hence if timer <u>reached zero</u> already, time
	 * addition results&nbsp;in passed time <u>increasing</u>).
	 * <p><i>Performance note.</i> Contains synchronized sections.
	 * 
	 * @param seconds_amount Correction time&nbsp;amount in seconds.
	 * <u>Cannot</u> be negative.
	 * 
	 * @param add {@code true} &#0151; time&nbsp;amount addition;
	 * {@code false}&nbsp;&#0151; subtraction.
	 * 
	 * @return {@code true} &#0151; operation succeed; {@code false}&nbsp;&#0151;
	 * addition/subtraction result is too&nbsp;big (former time&nbsp;value
	 * remains in this case).
	 * 
	 * @exception IllegalArgumentException {@code seconds_amount} argument is
	 * negative.
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
						notify_time_counter_text_listeners();
						
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
			// Adjusted hours quantity
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
			notify_time_counter_text_listeners();
			
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
	
	
	/**
	 * Adds specified {@code listener} to receive numeric overflow event. Same
	 * {@code listener} <u>can</u> be&nbsp;added multiple times.<br>
	 * <i>Notes.</i>
	 * <ul><li>It is recommended to unsubscribe listener (if
	 * it&nbsp;is&nbsp;referenced anywhere else) using
	 * {@link #remove_Numeric_overflow_listener(Numeric_overflow_listener)} when
	 * there&nbsp;is no&nbsp;need to receive such event. Such action reduces
	 * resource usage and prevents resource leaks.</li>
	 * <li>However when removing the&nbsp;time&nbsp;counter object from
	 * {@link Modified_ArrayList}, obtained by
	 * {@link Time_counter_control#get_time_counters()} method, <u>all</u> its
	 * listeners removed automatically.</li></ul>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with (private methods <u>are&nbsp;not</u> listed):
	 * <ul><li>{@link #remove_Numeric_overflow_listener(Numeric_overflow_listener)};</li>
	 * <li>{@link #shutdown()}.</li></ul>
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
	 * <ul><li>{@link #add_Numeric_overflow_listener(Numeric_overflow_listener)};</li>]
	 * <li>{@link #shutdown()}.</li></ul>
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
			return numeric_overflow_listeners.remove(listener);
		}
		finally
		{
			event_lock.unlock();
		}
	}
	
	
	/**
	 * <i>Note.</i> Next methods are guaranteed <u>not&nbsp;to&nbsp;cause
	 * exceptions</u> after this method calling:
	 * <ul><li>{@link #start()};</li>
	 * <li>{@link #pause()};</li>
	 * <li>{@link #restart()}.</li></ul>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with (private methods <u>are&nbsp;not</u> listed):
	 * <ul><li>{@link #add_Numeric_overflow_listener(Numeric_overflow_listener)}</li>
	 * <li>{@link #remove_Numeric_overflow_listener(Numeric_overflow_listener)}.</li></ul>
	 */
	@Override
	public void shutdown()
	{
		pause();
		event_lock.lock();
		
		try
		{
			numeric_overflow_listeners.clear();
		}
		finally
		{
			event_lock.unlock();
		}
		
		super.shutdown();
	}
	
	
	/**
	 * Informs whether time&nbsp;counter has&nbsp;been&nbsp;shutdown by calling
	 * {@link #shutdown()} method.
	 * 
	 * @return {@code true} &#0151; time&nbsp;counter is&nbsp;shutdown;
	 * {@code false}&nbsp;&#0151; is working in normal state.
	 */
	public boolean is_shutdown()
	{
		return is_shutdown;
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
	private void readObject(final ObjectInputStream input_stream)
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
		
		// Stopwatch mode time value can be only positive
		if (instance_mode.equals(Mode.M_stopwatch))
		{
			set_time_counter_value_sign(true);
		}
		
		// Field representing split second, must have value within second
		if (tenth_of_second < 0 || tenth_of_second > 9)
		{
			tenth_of_second = 0;
			deserialization_status = false;
		}
		
		thread_counter_init();
		modify_lock = new ReentrantLock();
		event_lock = new ReentrantLock();
		numeric_overflow_listeners = new ArrayList<>();
		is_shutdown = false;
		
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
			
			build_time_string(
					time_counter_resources.getString("numeric_overflow_mark"));
		}
		else
		{
			set_time_unit_values();
			build_time_string();
		}
	}
	
	
	/**
	 * {@link #Solo_counter(Mode, Period, LocalTime, Days_in_year)} and
	 * {@link #readObject(ObjectInputStream)} auxiliary performing
	 * {@link Time_counter#instance_mode}, {@link #period_init} and
	 * {@link #duration_init} fields checking.
	 * 
	 * @param called_from_constructor Determines exception types which can be
	 * thrown during checking. {@code true}&nbsp;&#0151; the&nbsp;method is
	 * called from constructor; {@link ArithmeticException},
	 * {@link IllegalArgumentException} and {@link NullPointerException} can be
	 * thrown. {@code false}&nbsp;&#0151; the&nbsp;method is called from
	 * {@link #readObject(ObjectInputStream)} method;
	 * {@link InvalidObjectException} can be thrown.
	 * 
	 * @throws InvalidObjectException Deserialized object's field did&nbsp;not
	 * pass validation.
	 * 
	 * @exception ArithmeticException Numeric overflow occurred during
	 * {@link Period} normalization.
	 * 
	 * @exception IllegalArgumentException If:
	 * <ul><li>{@link Time_counter#instance_mode} is inappropriate for this
	 * class;</li>
	 * <li>Initial time&nbsp;amount is less than one&nbsp;second in
	 * {@link Mode#M_countdown} mode or {@link #period_init} and
	 * {@link #duration_init} are both {@code null} (regarding&nbsp;to
	 * {@link Mode#M_countdown} mode too);</li>
	 * <li>{@link #period_init} field contains negative time&nbsp;values
	 * ({@link Period#isNegative()} returns {@code true}).</li></ul>
	 * 
	 * @exception NullPointerException {@link Time_counter#instance_mode} is
	 * {@code null}.
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
			duration_init = LocalTime.of(0, 0, 0);
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
				// If there is no full second cycle passed yet
				if (++tenth_of_second != 10)
				{
					return;
				}
				
				tenth_of_second = 0;
				
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
								build_time_string(
										time_counter_resources.getString(
												"numeric_overflow_mark"));
								pause();
								notify_numeric_overflow_listeners();
								
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
							
							/* If days quantity became negative in the result of
							 * decrement */
							if (period_passed.getDays() == -1)
							{
								// If months OR years quantity is still positive
								if (months_remain > 0 || years_remain > 0)
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
								else
								{
									duration_passed = duration_passed.plusSeconds(2);
									period_passed = period_passed.plusDays(1);
									set_time_counter_value_sign(false);
									notify_time_elapsed_listeners();
								}
							}
						}
					}
					
					set_time_unit_values();
					build_time_string();
					notify_time_counter_text_listeners();
				}
				finally
				{
					modify_lock.unlock();
				}
			}
		};
	}
	
	
	/**
	 * Notifies listeners subscribed via {@link Numeric_overflow_listener}
	 * interface about numeric overflow occurred.<br>
	 * <i>Performance note.</i> Contains synchronized sections. Synchronized
	 * with:
	 * <ul><li>{@link #add_Numeric_overflow_listener(Numeric_overflow_listener)};</li>
	 * <li>{@link #remove_Numeric_overflow_listener(Numeric_overflow_listener)}.</li></ul>
	 */
	private void notify_numeric_overflow_listeners()
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
		
		/* Numeric overflow listeners notifier to notify each listener in
		 * a separate thread */
		ThreadPoolExecutor notifier = null;
		
		try
		{
			final int numeric_overflow_listeners_quantity =
					numeric_overflow_listeners.size();
			
			notifier = new ThreadPoolExecutor(
					numeric_overflow_listeners_quantity,
					numeric_overflow_listeners_quantity,
					0, TimeUnit.NANOSECONDS,
					new ArrayBlockingQueue<>(numeric_overflow_listeners_quantity));
			notifier.prestartAllCoreThreads();
			
			// This object's reference to be passed with event
			final Solo_counter instance = this;
			
			// Listeners notification
			for (final Numeric_overflow_listener i : numeric_overflow_listeners)
			{
				notifier.execute(new Runnable()
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
			
			// Shutdown executor
			if (notifier != null)
			{
				notifier.setCorePoolSize(0);
			}
		}
	}
}
