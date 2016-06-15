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
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;


// TODO: ? Объявить абстрактные методы
/**
 * Инкапсулирует основные свойства счетчика времени.<br>
 * <b>Важно!</b> Содержит элементы JavaFX. Инициализация экземпляров перед
 * запуском компонентов JavaFX приведет к ошибке времени выполнения.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public abstract class Time_counter implements Serializable
{
	///// Перечисления protected ==========================================/////
	/**
	 * Содержит именованные константы общих кнопок для всех классов,
	 * 	наследующих класс {@link Time_counter}.
	 */
	protected enum Button_key_common
	{
		/** Кнопка "Закрыть". */
		BKC_close,
		/** Кнопка настройки экземпляра отсчета времени. */
		BKC_setting
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
	
	/** Содержит адреса изображений для режимов счетчика времени<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения контейнера приведет к ошибке времени выполнения. */
	private static final Map<Mode, String> mode_img_directories;
	
	/** Содержит форматирующие последовательности (например, "%2d") для
	 * форматирования строки.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	private static final Map<Time_unit_name, String> format_conversions;
	
	/** Относительные директории изображений кнопок {@link #button_close} и
	 * {@link #button_setting} согласно перечислению
	 * {@link Button_key_common}.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	private static final Map<Button_key_common, String> button_images;
	
	/** Шаблонный текст подсказки для поля {@link #description} с описанием
	 * отсчета времени. */
	private static final String description_prompt_template;
	
	/** Тексты всплывающих подсказок согласно перечислению
	 * {@link Button_key_common}. */
	private static final Map<Button_key_common, String> button_tooltips;
	
	/** Кол&#8209;во разрешений для семафора {@link #layout_fields_semaphore}. */
	private static final int layout_fields_semaphore_permits;
	
	
	static
	{
		logger = Logger.getLogger(Time_counter.class.getName());
		strict_display_mode_text = "CUT  ";
		
		// Строки для инициализации контейнера "mode_img_directories"
		final String[] mode_img_directories_strings = {
				"images/stopwatch_middle.png", "images/countdown_middle.png",
				"images/elapsed_from_middle.png", "images/countdown_till_middle.png" };
		// Инициализатор контейнера "mode_img_directories"
		final Map<Mode, String> mode_img_directories_init =
				new EnumMap<>(Mode.class);
		// Все элементы перечисления "Mode"
		final Mode[] mode_values = Mode.values();
		
		assert mode_img_directories_strings.length == mode_values.length :
			"Array size with values doesn\'t match with " + Mode.class.getName()
			+ " elements quantity";
		
		// Инициализация контейнера "mode_img_directories_init"
		for (final Mode i : mode_values)
		{
			mode_img_directories_init.put(
					i, mode_img_directories_strings[i.ordinal()]);
		}
		
		mode_img_directories =
				Collections.unmodifiableMap(mode_img_directories_init);
		
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
		
		// TODO: ? Правильность указания URL
		/* Строки с относительными директориями изображений для инициализации
		 * "button_images" */
		final String[] button_image_text = { "../Images/close.gif",
				"../Images/setting.gif" };
		// Все элементы перечисления "Button_key_common"
		final Button_key_common[] button_key_common_values =
				Button_key_common.values();
		
		assert button_image_text.length == button_key_common_values.length :
			"Array size with values doesn\'t match with "
				+ Button_key_common.class.getName() + " elements quantity";
		
		// Инициализатор "button_images"
		final Map<Button_key_common, String> button_image_init =
				new EnumMap<>(Button_key_common.class);
		
		/* Инициализация "button_image_init" относительными директориями
		 * изображений */
		for (final Button_key_common i : button_key_common_values)
		{
			button_image_init.put(i, button_image_text[i.ordinal()]);
		}
		
		button_images = Collections.unmodifiableMap(button_image_init);
		month_sizes = Collections.unmodifiableList(Arrays.asList(
				31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31));
		description_prompt_template = "You may add description here";
		
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
		
		// Значения для контейнера "button_tooltips"
		final String[] button_tooltip_values = { "Close", "Setting" };
		
		assert button_tooltip_values.length == button_key_common_values.length :
			"Array size with values doesn\'t match with "
				+ Button_key_common.class.getName() + " elements quantity";
		
		// Инициализатор контейнера "button_tooltips"
		final Map<Button_key_common, String> button_tooltip_init =
				new EnumMap<>(Button_key_common.class);
		
		// Присваивание значений контейнеру "button_tooltip_init"
		for (final Button_key_common i : button_key_common_values)
		{
			button_tooltip_init.put(i, button_tooltip_values[i.ordinal()]);
		}
		
		button_tooltips = Collections.unmodifiableMap(button_tooltip_init);
		layout_fields_semaphore_permits = 3;
		settings = Settings.get_instance();
	}
	
	
	///// Поля public экземпляра ==========================================/////
	/** Текст с описанием отсчета времени. Задается пользователем для удобства */
	public transient TextField description;
	
	/** Изображение режима отсчета времени. */
	public transient ImageView mode_image;	
	
	// TODO: Вычислить фиксированную ширину после построения ГПИ
	/** Показывает счетчик времени */
	public transient Label time_counter;
	
	/** Закрытие отсчета времени. */
	public transient Button button_close;
	/** Настройка экземпляра отсчета времени. */
	public transient Button button_setting;
	
	// TODO: Изменение значения при перемещении в ГПИ
	/** Порядковый номер, обозначающий положение данного счетчика времени при
	 * компоновке всех счетчиков времени в ГПИ. Должен изменяться при
	 * перемещении объекта {@link Time_counter} в другое место относительно
	 * остальных объектов {@link Time_counter}. Изменение порядкового номера
	 * необходимо для сортировки контейнера, содержащего все объекты
	 * {@link Time_counter}, перед записью в файл. Таким образом при запуске
	 * программы достигается сохранение порядка расположения счетчиков времени
	 * в ГПИ программы. */
	public transient int index_number;
	
	
	///// Поля protected экземпляра =======================================/////
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
	protected final Mode instance_mode;
	
	/** Содержит значения единиц времени согласно перечислению
	 * {@link time_obj.Time_unit_name}. */
	protected transient EnumMap<Time_unit_name, Long> time_unit_values;
	
	/** {@code true}&nbsp;&#0151; значения счетчика времени должны отображаться
	 * с положительными значениями единиц времени; {@code false}&nbsp;&#0151; с
	 * отрицательными.<br>
	 * Использование расширяющими классами зависит от режима подсчета времени.<br>
	 * <ul><li>{@link Mode#M_stopwatch}. Значения единиц времени в этом режиме
	 * не&nbsp;могут быть отрицательными (всегда имеет значение {@code true}).</li>
	 * <li>{@link Mode#M_countdown}. Поле имеет значение {@code true} пока
	 * таймер не&nbsp;достиг нуля. При пересечении нулевого времени единицы
	 * времени должны быть отрицательными (полю присваивается значение
	 * {@code false}).</li>
	 * <li>{@link Mode#M_countdown_till}. Поле имеет значение {@code true} пока
	 * текущее время не&nbsp;достигло указанных даты и времени. При пересечении
	 * указанной временной точки единицы времени должны быть отрицательными
	 * (полю присваивается значение {@code false}).</li>
	 * <li>{@link Mode#M_elapsed_from}. Поле имеет значение {@code false} пока
	 * текущее время не&nbsp;достигло указанных даты и времени. При пересечении
	 * указанной временной точки единицы времени должны быть положительными
	 * (полю присваивается значение {@code true}).</li></ul>
	 * 
	 * @serial Десериализованное значение не&nbsp;проверяется. Если режимом
	 * подсчета времени является {@link Mode#M_stopwatch}, полю присваивается
	 * {@code true} в расширяющем классе. */
	protected boolean is_positive_value;
	
	/** Флаг десериализации объекта. {@code true}&nbsp;&#0151; десериализация
	 * объекта прошла успешно; {@code false}&nbsp;&#0151; некоторые
	 * (<u>относительно некритические</u>) поля объекта не&nbsp;прошли
	 * валидацию и инициализированы значениями по&nbsp;умолчанию. */
	protected transient boolean deserialization_status;
	
	/** Строка со&nbsp;значением счетчика времени для {@link #time_counter}. */
	protected transient String time_counter_text;
	
	
	///// Поля private экземпляра =========================================/////
	/** Стиль отображения счетчика времени целиком для экземпляра этого класса.
	 * 
	 * @serial После десериализации <u>не&nbsp;должен</u> быть {@code null}.
	 * Если условие не&nbsp;соблюдается&nbsp;&#0151; присваивается значение
	 * по&nbsp;умолчанию. */
	private Time_display_style object_time_display_style;
	
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
	private Time_unit_name[] object_time_value_edges;
	
	/** Содержит стиль отображения названий единиц времени для экземпляра
	 * класса.
	 * 
	 * @serial После десериализации <u>не&nbsp;должен</u> быть {@code null}.
	 * Если условие не&nbsp;соблюдается&nbsp;&#0151; присваивается значение
	 * по&nbsp;умолчанию. */
	private Time_unit_layout time_unit_layout_value;
	
	/** Используется для форматирования строки с еденицами времени, которая
	 * выводится через {@link #time_counter}. */
	private transient Formatter formatter;
	
	// TODO: Изменять значение в обработчике для "description"
	/** Содержит текст из transient&#8209;поля {@link #description} для его
	 * восстановления после десериализации.
	 * @serial */
	private String description_text;
	
	/** Синхронизирует доступ к полям {@link #object_time_display_style},
	 * {@link #object_time_value_edges}, {@link #time_unit_layout_value}. */
	private transient Semaphore layout_fields_semaphore;


	///// Нестатический блок инициализации ================================/////
	{
		time_counter = new Label();
		description = new TextField();  // TODO: SetOnAction()
		description.setPrefColumnCount(40);
		description.setPromptText(description_prompt_template);
		description_text = null;
		time_unit_values = new EnumMap<>(Time_unit_name.class);
		deserialization_status = true;
		object_time_display_style = settings.get_time_display_style_setting();
		object_time_value_edges = settings.get_time_value_edges();
		time_unit_layout_value = settings.get_time_unit_layout_setting();
		layout_fields_semaphore =
				new Semaphore(layout_fields_semaphore_permits);
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
		mode_image = new ImageView(mode_img_directories.get(instance_mode));
		// TODO: Обработчики событий нажатия на кнопки
		button_close = new Button("",
				new ImageView(button_images.get(Button_key_common.BKC_close)));
		button_close.setTooltip(
				new Tooltip(button_tooltips.get(Button_key_common.BKC_close)));
		button_setting = new Button("",
				new ImageView(button_images.get(Button_key_common.BKC_setting)));
		button_setting.setTooltip(
				new Tooltip(button_tooltips.get(Button_key_common.BKC_setting)));
	}

	
	///// Методы public экземпляра ========================================/////
	/**
	 * Возвращает режим работы счетчика времени этого объекта.
	 * 
	 * @return Режим работы счетчика времени в&nbsp;виде именованной константы
	 * из перечисления {@link Mode}. 
	 */
	public Mode get_instance_mode()
	{
		return instance_mode;
	}
	
	
	/**
	 * Установка стиля отображения счетчика времени целиком для экземпляра
	 * класса согласно именованным константам перечисления
	 * {@link Time_display_style}.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param new_value Новый стиль отображения счетчика времени.
	 * 
	 * @exception NullPointerException Передача {@code null} в качестве
	 * аргумента.
	 */
	public void set_object_time_display_style(Time_display_style new_value)
	{
		// Если параметром функции является null
		if (new_value == null)
		{
			throw new NullPointerException(
					Time_display_style.class.getName() + " argument is null");
		}
		
		try
		{
			layout_fields_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		object_time_display_style = new_value;
		layout_fields_semaphore.release();
	}
	
	
	/**
	 * Устанавливает новые значения крайних отображаемых единиц времени согласно
	 * перечислению {@link Time_unit_name}.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param left_edge Новое значение для крайней левой отображаемой единицы
	 * времени (т.е.&nbsp;наибольшая отображаемая единица времени). Индекс
	 * массива&nbsp;&#0151;&nbsp;{@code [0]}.
	 * 
	 * @param right_edge Новое значение для крайней правой отображаемой единицы
	 * времени (т.е.&nbsp;наименьшая отображаемая единица времени). Индекс
	 * массива&nbsp;&#0151;&nbsp;{@code [1]}.
	 * 
	 * @exception IllegalArgumentException Параметр {@code left_edge} должен
	 * содержать единицу времени <u>больше</u> чем параметр {@code right_edge},
	 * или быть <u>равным</u> ему.<br>
	 * <i>Примеры:</i>
	 * <ul><li>{@code left_edge} со значением
	 * {@code Time_unit_name.TUN_months} и {@code right_edge} со&nbsp;значением
	 * {@code Time_unit_name.TUN_hours}&nbsp;&#0151; правильно;</li>
	 * <li>{@code left_edge} со&nbsp;значением {@code Time_unit_name.TUN_days}
	 * и {@code right_edge} с этим&nbsp;же значением
	 * {@code Time_unit_name.TUN_days}&nbsp;&#0151; правильно;</li>
	 * <li>{@code left_edge} со&nbsp;значением {@code Time_unit_name.TUN_days}
	 * и {@code right_edge} со&nbsp;значением
	 * {@code Time_unit_name.TUN_years}&nbsp;&#0151;
	 * <u>неправильно</u>.</li></ul>
	 * 
	 * @exception NullPointerException Если хотя&nbsp;бы один из переданных
	 * аргументов&nbsp;&#0151; {@code null}.
	 */
	public void set_object_time_value_edges(
			Time_unit_name left_edge, Time_unit_name right_edge)
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
			layout_fields_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		object_time_value_edges[0] = left_edge;
		object_time_value_edges[1] = right_edge;
		layout_fields_semaphore.release();
	}
	
	
	/**
	 * Устанавливает стиль отображения единиц времени для экземпляра класса
	 * согласно именованным константам перечисления {@link Time_unit_layout}.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param layout_set Новый стиль отображения названий единиц времени.
	 * 
	 * @exception NullPointerException Передача {@code null} в качестве
	 * аргумента.
	 */
	public void set_time_unit_layout_value(Time_unit_layout layout_set)
	{
		// Если параметром метода является null
		if (layout_set == null)
		{
			throw new NullPointerException(
					Time_unit_layout.class.getName() + " argument is null");
		}
		
		try
		{
			layout_fields_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		time_unit_layout_value = layout_set;
		layout_fields_semaphore.release();
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
	
	
	///// Методы protected экземпляра =====================================/////
	/**
	 * Формирует строку счетчика времени {@link #time_counter_text}, которая
	 * будет отображаться посредством {@link #time_counter}.<br>
	 * <b>Важно!</b> Данный метод открывает и закрывает {@link #formatter}.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 */
	protected void build_time_string()
	{
		formatter = new Formatter();
		
		try
		{
			layout_fields_semaphore.acquire(layout_fields_semaphore_permits);
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		// Если выбран строгий диапазон отображаемых единиц времени
		if (object_time_display_style.equals(Time_display_style.TDS_custom_strict))
		{
			formatter.format(strict_display_mode_text);
		}
		
		// Если значение отрицательное - в начале ставится знак "минус"
		if (!is_positive_value)
		{
			formatter.format("\u0150");
		}
		
		/* Если в настройках установлено отображение единиц времени, которые
		 * "имеют вес" (т.е. значение которых достигнуто) */
		if (object_time_display_style.equals(Time_display_style.TDS_if_reaches))
		{
			/* Формирование строки для форматирования, содержащей отображаемые
			 * единицы времени, происходит от больших единиц времени к меньшим.
			 * Как только значение одной из единиц времени оказывается значащим
			 * (т.е. отличным от нуля), - все меньшие единицы времени должны
			 * отображаться в любом случае.
			 * true - значащая единица времени достигнута; false - нет */
			boolean value_reached = false;
			
			/* Формирование строки для форматирования, содержащей отображаемые
			 * единицы времени */
			for (final Time_unit_name i : Time_unit_name.values())
			{
				/* Если значащая единица времени уже достигнута в предыдущих
				 * итерациях ИЛИ достигнута в этой итарации ... */
				if (value_reached || time_unit_values.get(i) != 0 ||
						/* ... ИЛИ это секунды (должны отображаться в любом
						 * случае) как наименьшая отображаемая единица времени */
						i.equals(Time_unit_name.TUN_seconds))
				{
					value_reached = true;
					format(i);
				}
			}
		}
		// Если в настройках установлено отображение всех единиц времени
		else if (object_time_display_style.equals(Time_display_style.TDS_show_all))
		{
			/* Формирование строки для форматирования, содержащей отображаемые
			 * единицы времени */
			for (final Time_unit_name i : Time_unit_name.values())
			{
				format(i);
			}
		}
		/* Если в настройках установлен конкретный диапазон отображаемых единиц
		 * времени (строгий ИЛИ нестрогий) */
		else if (object_time_display_style.equals(Time_display_style.TDS_custom_strict) ||
				object_time_display_style.equals(Time_display_style.TDS_increase_able))
		{
			/* true - если установлен НЕСТРОГИЙ диапазон отображаемых единиц
			 * времени; false - установлен СТРОГИЙ диапазон */
			final boolean increase_able_is_set = object_time_display_style.equals(
						Time_display_style.TDS_increase_able);
			/* Формирование строки для форматирования, содержащей отображаемые
			 * единицы времени, происходит от больших единиц времени к меньшим.
			 * Соответственно, если большая единица времени имеет значение
			 * (т.е. не равна 0), - меньшие единицы должны отображаться в любом
			 * случае. Это касается режима НЕСТРОГОГО отображения диапазона
			 * отображаемых единиц времени
			 * (Time_display_style.TDS_increase_able). true - обнаружено
			 * ненулевое значение единицы времени, превышающее установленный
			 * диапазон для отображения; false - нет */
			boolean out_of_set_range = false;
			
			/* Формирование строки для форматирования, содержащей отображаемые
			 * единицы времени */
			for (final Time_unit_name i : Time_unit_name.values())
			{
				/* Результат сравнения именованной константы текущей итерации и
				 * крайней правой отображаемой единицы (наименьшей отображаемой
				 * единицы) времени */
				final int compare_to_1 = i.compareTo(object_time_value_edges[1]);
				
				/* Если (единица времени входит в выставленный диапазон
				 * отображения) ... */
				if ((i.compareTo(object_time_value_edges[0]) >= 0 && compare_to_1 <= 0) ||
						/* ... ИЛИ (установлен нестрогий диапазон отображаемых
						 * единиц времени И (в предыдущих итерациях обнаружено
						 * ненулевое значение единицы времени, превышающее
						 * установленный диапазон для отображения ... */
						(increase_able_is_set && (out_of_set_range ||
								// ... ИЛИ (это обнаружилось в этой итерации)))
								(compare_to_1 <= 0 && time_unit_values.get(i) != 0))))
				{
					out_of_set_range = true;
					format(i);
				}
			}
		}
		
		layout_fields_semaphore.release(layout_fields_semaphore_permits);
		time_counter_text = formatter.toString();
		formatter.close();
	}
	
	
	/**
	 * Вызывается расширяющим классом для инициализации
	 * {@code transient}&#8209;полей, требующих предварительной проверки
	 * расширяющим классом, после десериализации.
	 */
	protected final void deserialization_restore()
	{
		mode_image = new ImageView(mode_img_directories.get(instance_mode));
	}
	
	
	/**
	 * Устанавливает строку {@link #time_counter_text} в {@link #time_counter}.
	 */
	protected void set_time_counter_text()
	{
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				time_counter.setText(time_counter_text);
			}
		});
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
		if (time_unit_layout_value.equals(Time_unit_layout.TUL_full_name))
		{
			// Числовое значение единицы времени текущей итерации
			final long time_unit_value_num = time_unit_values.get(time_unit);
			// Длина названия единицы времени
			final long value_name_length = time_unit_texts.
					get(time_unit_layout_value).get(time_unit).length() + 1;
			/* Название единицы времени, передаваемое в качестве
			 * аргумента для форматирования строки */
			final String value_name_text =
					time_unit_texts.get(time_unit_layout_value).get(time_unit) +
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
					time_unit_texts.get(time_unit_layout_value).get(time_unit),
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
			object_time_value_edges = object_time_value_edges.clone();
			
			// Если размер массива не равен 2-м элементам ИЛИ ...
			if (object_time_value_edges.length != 2 ||
					/* ... значение крайней левой отображаемой единицы времени
					 * больше правой */
					object_time_value_edges[0].compareTo(object_time_value_edges[1]) > 0)
			{
				object_time_value_edges = settings.get_time_value_edges();
				deserialization_status = false;
			}
		}
		catch (final NullPointerException exc)
		{
			object_time_value_edges = settings.get_time_value_edges();
			logger.log(Level.WARNING, "Deserialized object\'s noncritical field"
					+ " is null. Deserialized object can be used. Excepion\'s stack trace:", exc);
			deserialization_status = false;
		}
		
		// Если поле оказалось пустой ссылкой
		if (object_time_display_style == null)
		{
			object_time_display_style =
					settings.get_time_display_style_setting();
			logger.log(Level.WARNING, "Deserialized object\'s noncritical field of "
					+ Time_display_style.class.getName() + " type is null."
							+ " Deserialized object can be used.");
			deserialization_status = false;
		}
		
		// Если поле оказалось пустой ссылкой
		if (time_unit_layout_value == null)
		{
			time_unit_layout_value = settings.get_time_unit_layout_setting();
			logger.log(Level.WARNING, "Deserialized object\'s noncritical field of "
					+ Time_unit_layout.class.getName() + " type is null."
							+ " Deserialized object can be used.");
			deserialization_status = false;
		}
		
		layout_fields_semaphore =
				new Semaphore(layout_fields_semaphore_permits);
		description = new TextField();
		description.setPrefColumnCount(40);
		
		// Если поле "description" содержало текст перед сериализацией
		if (description_text != null)
		{
			description.setText(description_text);
		}
		else
		{
			description.setPromptText(description_prompt_template);
		}
		
		time_counter = new Label();
		button_close = new Button("",
				new ImageView(button_images.get(Button_key_common.BKC_close)));
		button_close.setTooltip(
				new Tooltip(button_tooltips.get(Button_key_common.BKC_close)));
		button_setting = new Button("",
				new ImageView(button_images.get(Button_key_common.BKC_setting)));
		button_setting.setTooltip(
				new Tooltip(button_tooltips.get(Button_key_common.BKC_setting)));
		time_unit_values = new EnumMap<>(Time_unit_name.class);
	}
}