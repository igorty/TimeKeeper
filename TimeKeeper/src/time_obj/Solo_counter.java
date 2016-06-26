package time_obj;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.Period;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;


/**
 * Реализует режимы секундомера и таймера обратного отсчета без привязки к дате
 * (согласно именованным константам {@link Mode#M_stopwatch} и
 * {@link Mode#M_countdown} соответственно).<br>
 * <b>Важно!</b> Содержит элементы JavaFX. Инициализация экземпляров перед
 * запуском компонентов JavaFX приведет к ошибке времени выполнения.<br>
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
	///// Перечисления private ============================================/////
	/**
	 * Именованные константы кнопок класса {@link Solo_counter}.
	 * 
	 * @author Cryptor
	 */
	private enum Button_key_solo
	{
		/** Кропка Старт со значением "Старт". */
		BKS_START_start,
		/** Кнопка Старт со значением "Пауза". */
		BKS_START_pause,
		/** Кнопка Старт со значением "Возобновить". */
		BKS_START_resume,
		/** Кнопка "Перезапустить". */
		BKS_restart
	}
	
	
	///// Поля private статические ========================================/////
	/** Логирует события в данном классе. */
	private static final Logger logger;
	
	/** Относительные директории изображений кнопок {@link #button_start} и
	 * {@link #button_restart} согласно перечислению {@link Button_key_solo}.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	private static final Map<Button_key_solo, String> button_images;
	
	/** Тексты всплывающих подсказок согласоно перечислению
	 * {@link Button_key_solo}.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	private static final Map<Button_key_solo, String> button_tooltips;
	
	/** Сообщение, отображаемое вместо счетчика времени в случае переполнения. */
	private static final String numeric_overflow_message;
	
	static
	{
		logger = Logger.getLogger(Solo_counter.class.getName());
		
		/* Строки с относительными директориями изображений для инициализации
		 * контейнера "button_images" */
		final String[] button_image_urls = { "images/start.gif",
				"images/pause.gif", "images/resume.gif",
				"images/restart.gif" };
		// Все элементы перечисления "Button_key_solo"
		final Button_key_solo[] button_key_solo_values = Button_key_solo.values();
		
		assert button_image_urls.length == button_key_solo_values.length :
			"Array size with values doesn\'t match with quantity of "
				+ Button_key_solo.class.getName() + " elements";
		
		// Инициализатор контейнера "button_images"
		final Map<Button_key_solo, String> button_images_init =
				new EnumMap<>(Button_key_solo.class);
		
		/* Инициализация контейнера "button_images_init" относительными
		 * директориями изображений */
		for (final Button_key_solo i : button_key_solo_values)
		{
			button_images_init.put(i, button_image_urls[i.ordinal()]);
		}
		
		button_images = Collections.unmodifiableMap(button_images_init);
		
		// Значения для контейнера "button_tooltips"
		final String[] button_tooltips_values = { "Start", "Pause", "Resume",
				"Restart" };
		
		assert button_image_urls.length == button_key_solo_values.length :
			"Array size with values doesn\'t match with quantity of "
				+ Button_key_solo.class.getName() + " elements";
		
		// Инициализатор контейнера "button_tooltips"
		final Map<Button_key_solo, String> button_tooltips_init =
				new EnumMap<>(Button_key_solo.class);
		
		// Присваивание значений контейнеру "button_tooltips_init"
		for (final Button_key_solo i : button_key_solo_values)
		{
			button_tooltips_init.put(i, button_tooltips_values[i.ordinal()]);
		}
		
		button_tooltips = Collections.unmodifiableMap(button_images_init);
		numeric_overflow_message = "numeric overflow";
	}
	
	
	///// Поля public экземпляра ==========================================/////
	/** Запуск/приостановка/возобновление отсчета времени. */
	public transient Button button_start;
	/** Перезапуск отсчета времени. */
	public transient Button button_restart;

	
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

	/** От данного поля зависит изображение кнопки {@link #button_start}.
	 * {@code true}&nbsp;&#0151; отсчет уже начинался (кнопка
	 * {@link #button_start} была нажата). В этом случае, после приостановки
	 * отсчета времени, указанная кнопка будет с рисунком согласно именованной
	 * константе {@link Button_key_solo#BKS_START_resume}.
	 * {@code false}&nbsp;&#0151; отсчет еще не&nbsp;начинался. В этом случае
	 * рисунок кнопки будет согласно именованной константе
	 * {@link Button_key_solo#BKS_START_start}.
	 * 
	 * @serial Десериализованное значение не&nbsp;проверяется. */
	private boolean counting_have_started;
	
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
	
	/** Регулирует доступ к изменению значения счетчика времени.<br>
	 * <i>Поля, доступ к которым синхронизируется данным семафором (<u>список
	 * может быть неполным</u>):</i>
	 * <ul><li>{@link #period_passed};</li>
	 * <li>{@link #duration_passed};</li>
	 * <li>{@link #counting_have_started};</li>
	 * <li>{@link Time_counter#time_unit_values};</li>
	 * <li>{@link Time_counter#is_positive_value}.</li></ul> */
	private transient ReentrantLock lock;
	
	
	// Нестатическая инициализация ========================================/////
	{
		/* TODO: Обработчики событий для всех четырех кнопок. Реализовать:
		 * наведение, убирание курсора, нажатие */
		button_start = new Button("",
				new ImageView(button_images.get(Button_key_solo.BKS_START_start)));
		button_start.setTooltip(
				new Tooltip(button_tooltips.get(Button_key_solo.BKS_START_start)));
		button_restart = new Button("",
				new ImageView(button_images.get(Button_key_solo.BKS_restart)));
		button_restart.setTooltip(
				new Tooltip(button_tooltips.get(Button_key_solo.BKS_restart)));
		button_restart.setDisable(true);
		counting_have_started = false;
		is_positive_value = true;
		thread_counter_init();
		numeric_overflow = false;
		lock = new ReentrantLock();
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
					+ " behaviour when calling from class\'s constructor."
					+ " Exception\'s stack trace:", exc);
			throw new RuntimeException("Incorrect method behaviour when calling from "
					+ Solo_counter.class.getName() + " class\'s constructor");
		}
		
		period_passed = period_init;
		duration_passed = duration_init;
		set_time_unit_values();
		build_time_string();
		set_time_counter_text();
		set_initial_time_tooltip();
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
	/* TODO: Make synchronized access for 'start' action after dividing
	 * on two separated methods */
	/**
	 * Отвечает за запуск и приостановку отсчета времени.<br>
	 * <i>Примечания.</i>
	 * <ul><li>При повторном запуске уже запущенного (либо&nbsp;повторной
	 * приостановке уже приостановленного) отсчета времени ничего
	 * не&nbsp;происходит.</li>
	 * <li>Если счетчик времени экземпляра класса достиг максимального значения,
	 * данный метод не&nbsp;будет выполняться.</li></ul>
	 * 
	 * @param start {@code true}&nbsp;&#0151; запуск отсчета времени;
	 * {@code false}&nbsp;&#0151; приостановка отсчета времени.
	 */
	public void thread_counter_manager(boolean start)
	{
		// Если необходимо запустить отсчет времени
		if (start)
		{
			// Если (отсчет времени уже хотя бы раз запускался И ...
			if ((thread_counter_executor != null &&
					/* ... отсчет времени сейчас выполняется) ИЛИ счетчик
					 * времени достиг максимально возможного значения */
					!thread_counter_executor.isShutdown()) || numeric_overflow)
			{
				return;
			}
			
			thread_counter_executor =
					Executors.newSingleThreadScheduledExecutor();
			thread_counter_executor.scheduleAtFixedRate(
					thread_counter, 1, 1, TimeUnit.SECONDS);
			
			// Изменение изображения кнопки "Старт"
			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{
					button_start.setGraphic(new ImageView(button_images.get(
							Button_key_solo.BKS_START_pause)));
					button_start.setTooltip(new Tooltip(button_tooltips.get(
							Button_key_solo.BKS_START_pause)));
					button_restart.setDisable(false);
				}
			});
		}
		else
		{
			// Если отсчет времени ни разу не запускался
			if (thread_counter_executor == null)
			{
				return;
			}
			
			thread_counter_executor.shutdown();
			
			try
			{
				thread_counter_executor.awaitTermination(1, TimeUnit.SECONDS);
			}
			catch (final InterruptedException exc)
			{
				logger.log(Level.INFO, "Tread interrupts");
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
			
			// Изменение изображения кнопки "Старт"
			Platform.runLater(new Runnable()
			{
				@Override
				public void run()
				{
					button_start.setGraphic(new ImageView(button_images.get(
							Button_key_solo.BKS_START_resume)));
					// TODO: Сделать шрифт серым
					button_start.setTooltip(new Tooltip(button_tooltips.get(
							Button_key_solo.BKS_START_resume)));
				}
			});
		}
	}
	
	
	/**
	 * Перезапуск счетчика времени начиная с первоначально заданного времени.<br>
	 * <i>Примечания.</i>
	 * <ul><li>Если отсчет времени еще не&nbsp;начинался, данная функция
	 * не&nbsp;выполняет никаких действий.</li>
	 * <li>Корректировка времени вперед и/или назад не&nbsp;сказывается на
	 * значении первоначально заданного времени.</li></ul>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 */
	public void restart()
	{
		try
		{
			lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		try
		{
			// Если отсчет времени еще не начинался
			if (!counting_have_started)
			{
				return;
			}
			
			period_passed = period_init;
			duration_passed = duration_init;
			
			// Если ход счетчика времени сейчас приостановлен
			if (thread_counter_executor.isShutdown())
			{
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						button_start.setGraphic(new ImageView(
								button_images.get(Button_key_solo.BKS_START_start)));
						button_start.setTooltip(new Tooltip(
								button_tooltips.get(Button_key_solo.BKS_START_start)));
						button_restart.setDisable(true);
					}
				});
				
				counting_have_started = false;
				numeric_overflow = false;
			}

			is_positive_value = true;
			set_time_unit_values();
			build_time_string();
			set_time_counter_text();
		}
		finally
		{
			lock.unlock();
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
	 * @exception IllegalArgumentException Параметр "seconds_amount" меньше нуля.
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
			lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Tread interrupts.");
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
				if (instance_mode.equals(Mode.M_stopwatch) || !is_positive_value)
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
						is_positive_value = false;
						seconds_corrected = Math.abs(seconds_corrected);
					}
				}
			}
			else
			{
				/* Если экземпляр класса работает в режиме секундомера ИЛИ это
				 * режим таймера, и нулевое время уже было достигнуто */
				if (instance_mode.equals(Mode.M_stopwatch) || !is_positive_value)
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
						set_time_counter_text();
						
						return true;
					}
					/* Если в результате корректировки времени было пересечено
					 * нулевое время И экземпляр класса работает в режиме таймера */
					else if (seconds_passed < 0 &&
							instance_mode.equals(Mode.M_countdown))
					{
						is_positive_value = true;
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
			
			duration_passed = LocalTime.of(hours, minutes, day_seconds % minutes_in_hour);
			set_time_unit_values();
			build_time_string();
			set_time_counter_text();
			
			return true;
		}
		finally
		{
			lock.unlock();
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
					+ " Exception\'s stack trace:", exc);
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

			time_counter.setText(numeric_overflow_message);
		}
		
		// В режиме секундомера поле "is_positive_value" может быть только true
		if (instance_mode.equals(Mode.M_stopwatch))
		{
			is_positive_value = true;
		}
		
		set_time_unit_values();
		thread_counter_init();
		set_initial_time_tooltip();
		button_restart = new Button("",
				new ImageView(button_images.get(Button_key_solo.BKS_restart)));
		button_restart.setTooltip(
				new Tooltip(button_tooltips.get(Button_key_solo.BKS_restart)));
		
		// Если отсчет начался перед сериализацией
		if (counting_have_started)
		{
			button_start = new Button("", new ImageView(
					button_images.get(Button_key_solo.BKS_START_resume)));
			button_start.setTooltip(new Tooltip(
					button_tooltips.get(Button_key_solo.BKS_START_resume)));
		}
		else
		{
			button_start = new Button("", new ImageView(
					button_images.get(Button_key_solo.BKS_START_start)));
			button_start.setTooltip(new Tooltip(
					button_tooltips.get(Button_key_solo.BKS_START_start)));
			button_restart.setDisable(true);
		}
		
		lock = new ReentrantLock();
		deserialization_restore();
	}
	
	
	/**
	 * Вспомогательный метод для {@link #Solo_counter(Mode, Period, LocalTime, Days_in_year)}
	 * и {@link #readObject(ObjectInputStream)}, выполняющий проверку полей
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
						+ " class\'s constructor is null. Exception\'s stack trace:", exc);
				throw exc;
			}
			else
			{
				logger.log(Level.SEVERE, "Deserialized object\'s field "
						+ Mode.class.getName() + " is null. This deserialized"
						+ " object cannot be used. Exception\'s stack trace:", exc);
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
							+ " class\'s constructor. Exception\'s stack trace:", exc);
					throw exc;
				}
				else
				{
					logger.log(Level.SEVERE, "Decerialized object\'s field of type "
							+ Period.class.getName() + " is incorrect. This"
							+ " deserizlized object cannot be used. Exception\'s"
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
					lock.lockInterruptibly();
				}
				catch (final InterruptedException exc)
				{
					logger.log(Level.INFO, "Thread interrupts.");
					Thread.currentThread().interrupt();
				}
				
				try
				{
					counting_have_started = true;
					
					/* Если экземпляр класса работает в режиме секундомера ИЛИ
					 * это режим таймера, и нулевое время уже было достигнуто */
					if (instance_mode.equals(Mode.M_stopwatch) || !is_positive_value)
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
								
								Platform.runLater(new Runnable()
								{
									@Override
									public void run()
									{
										time_counter.setText(
												numeric_overflow_message);
										/* TODO: Установить красный фон или красный
										 * текст */
										button_start.setGraphic(new ImageView(
												button_images.get(
														Button_key_solo.BKS_START_start)));
										button_start.setDisable(true);
									}
								});
								
								thread_counter_manager(false);
								
								return;
							}
						}
					}
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
								is_positive_value = false;
							}
						}
					}
					
					set_time_unit_values();
					build_time_string();
					set_time_counter_text();
				}
				finally
				{
					lock.unlock();
				}
			}
		};
	}
	
	
	/**
	 * Если {@link Time_counter#instance_mode} имеет значение
	 * {@link time_obj.Mode#M_countdown}, метод устанавливает всплывающее
	 * сообщение над {@link Time_counter#time_counter} и
	 * {@link Time_counter#mode_image}, содержащее {@link #period_init} и
	 * {@link #duration_init}. В&nbsp;ином случае не&nbsp;делает ничего.
	 */
	private void set_initial_time_tooltip()
	{
		// Если экземпляр класса работает в режиме секундомера
		if (instance_mode != Mode.M_countdown)
		{
			return;
		}
		
		// Текст всплывающего сообщения
		final StringBuilder tooltip_text =
				new StringBuilder("Initial timer\'s time: ");
		/* Формирование текста всплывающей подсказки, содержащей отображаемые
		 * единицы времени, происходит от больших единиц времени к меньшим.
		 * Как только значение одной из единиц времени оказывается значащим
		 * (т.е. отличным от нуля), - все меньшие единицы времени должны
		 * отображаться в любом случае.
		 * true - значащая единица времени достигнута; false - нет */
		boolean value_reached = false;
		
		// Если год имеет значащее число
		if (period_init.getYears() != 0)
		{
			tooltip_text.append(period_init.getYears());
			tooltip_text.append("yr ");
			value_reached = true;
		}
		
		// Если значащая единица времени уже достигнута ИЛИ достигнута сейчас
		if (value_reached || period_init.getMonths() != 0)
		{
			tooltip_text.append(period_init.getMonths());
			tooltip_text.append("mn ");
			value_reached = true;
		}
		
		// Если значащая единица времени уже достигнута ИЛИ достигнута сейчас
		if (value_reached || period_init.getDays() != 0)
		{
			tooltip_text.append(period_init.getDays());
			tooltip_text.append("d ");
			value_reached = true;
		}
		
		// Если значащая единица времени уже достигнута ИЛИ достигнута сейчас
		if (value_reached || duration_init.getHour() != 0)
		{
			tooltip_text.append(duration_init.getHour());
			tooltip_text.append("hr ");
			value_reached = true;
		}
		
		// Если значащая единица времени уже достигнута ИЛИ достигнута сейчас
		if (value_reached || duration_init.getMinute() != 0)
		{
			tooltip_text.append(duration_init.getMinute());
			tooltip_text.append("min ");
		}
		
		tooltip_text.append(duration_init.getSecond());
		tooltip_text.append("sec");
		
		// Всплывающее сообщение
		final Tooltip tooltip = new Tooltip(tooltip_text.toString());
		
		time_counter.setTooltip(tooltip);
		Tooltip.install(mode_image, tooltip);
	}
}
