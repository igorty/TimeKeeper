package main_package.FXML_controllers;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.PopOver.ArrowLocation;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import time_obj.Settings;
import time_obj.Time_display_style;
import time_obj.Time_unit_layout;
import time_obj.Time_unit_name;


/**
 * Контроллер компоновки окна создания счетчика времени с общими настройками.
 * Вызывается {@link FXMLLoader}'ом для файла <i>Init_settings_layout.fxml</i>.<br>
 * <b>Важно!</b> {@code FXML}&#8209;loader для данного класса (а соответственно
 * и инициализацию данного класса) необходимо запускать когда объект типа
 * {@link Stage}, на котором этот класс будет установлен, <u>уже отображается</u>
 * (для подмосток вызван метод {@link Stage#show()}). Иначе в данном классе
 * может быть сгенерировано {@link NullPointerException}. Причина описана
 * по&nbsp;ссылке внизу.<br>
 * <i>Примечание.</i> Корневой компоновкой для файла
 * <i>Init_settings_layout.fxml</i> является {@link GridPane}.
 * 
 * @version 1.0
 * @author Igor Taranenko
 * @see <a href="http://stackoverflow.com/questions/36969745/rangeslider-causes-nullpointerexception-when-pressing-tab-on-the-focused-slider">RangeSlider causes NullPointerException when pressing Tab on the focused slider</a>
 */
public class Init_settings_controller
{
	///// Вложенные классы public =========================================/////
	/**
	 * Выполняет роль контейнера для передачи всех выставленных настроек "одной
	 * кучей".
	 * 
	 * @version 1.0
	 * @author Igor Taranenko
	 */
	public class Init_settings
	{
		///// Поля public экземпляра ======================================/////
		/** Выбранное значение для отображаемых названий единиц времени. */
		public final Time_unit_layout time_unit_layout;
		
		/** Выбранный режим отображения счетчика времени целиком. */
		public final Time_display_style time_display_style;
		
		/** Крайняя левая отображаемая единица времени. */
		public final Time_unit_name left_displayed_edge;
		
		/** Крайняя правая отображаемая единица времени. */
		public final Time_unit_name right_displayed_edge;
		
		/** {@code true}&nbsp;&#0151; создаваемый счетчик времени должен
		 * отображаться на панели задач; {@code false}&nbsp;&#0151 нет. */
		public final boolean display_on_title;
		
		
		///// Конструкторы по умолчанию ===================================/////
		/**
		 * @param time_unit_layout Выбранное значение для отображаемых названий
		 * единиц времени.
		 * 
		 * @param time_display_style Выбранный режим отображения счетчика
		 * времени целиком.
		 * 
		 * @param left_displayed_edge Крайняя левая отображаемая единица времени.
		 * Может быть {@code null}, если {@code time_display_style} имеет
		 * значение {@link Time_display_style#TDS_if_reaches} или
		 * {@link Time_display_style#TDS_show_all}.
		 * 
		 * @param right_displayed_edge Крайняя правая отображаемая единица
		 * времени. Может быть {@code null}, если {@code time_display_style}
		 * имеет значение {@link Time_display_style#TDS_if_reaches} или
		 * {@link Time_display_style#TDS_show_all}.
		 * 
		 * @param display_on_title {@code true}&nbsp;&#0151; создаваемый счетчик
		 * времени должен отображаться на панели задач;
		 * {@code false}&nbsp;&#0151 нет.
		 * 
		 * @exception NullPointerException Если в качестве одного из аргументов
		 * передан {@code null}, за исключением указанных выше случаев.
		 */
		Init_settings(final Time_unit_layout time_unit_layout,
				final Time_display_style time_display_style,
				final Time_unit_name left_displayed_edge,
				final Time_unit_name right_displayed_edge,
				final boolean display_on_title)
		{
			/* Если в качестве названий единиц времени ИЛИ стиля отображения
			 * счетчика времени передан null */
			if (time_unit_layout == null || time_display_style == null)
			{
				throw new NullPointerException(Time_unit_layout.class.getName()
						+ " or " + Time_display_style.class.getName()
						+ " argument (or both) is null");
			}
			
			// Если в качестве крайних отображаемых единиц времени передан(ы) null
			if (left_displayed_edge == null || right_displayed_edge == null)
			{
				switch (time_display_style)
				{
				// В данных режимах это допустимо
				case TDS_if_reaches:
				case TDS_show_all:
					break;
					
				/* Данные режимы требуют указания отображаемого диапазона единиц
				 * времени */
				case TDS_custom_strict:
				case TDS_increase_able:
					throw new NullPointerException(Time_unit_name.class.getName()
							+ " argument(s) cannot be null in case of "
							+ Time_display_style.class.getName()
							+ " argument has " + time_display_style.name()
							+ " mode");
				}
			}
			
			this.time_unit_layout = time_unit_layout;
			this.time_display_style = time_display_style;
			this.left_displayed_edge = left_displayed_edge;
			this.right_displayed_edge = right_displayed_edge;
			this.display_on_title = display_on_title;
		}
	}
	
	
	///// Поля private статические ========================================/////
	/** Содержит возможные варианты выбора для {@link #time_unit_names_choicebox}.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	private static final Map<Time_unit_layout, String> time_unit_names_choicebox_values;
	
	/** Настройки программы. */
	private static Settings settings;
	
	/** Содержит возможные варианты выбора для
	 * {@link #time_display_style_choicebox}.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	private static final Map<Time_display_style, String>
			time_display_style_choicebox_values;
	
	
	static
	{
		// Инициализатор контейнера "time_unit_names_choicebox_values"
		final Map<Time_unit_layout, String> time_unit_names_choicebox_values_init =
				new EnumMap<>(Time_unit_layout.class);
		/* Строки значений, которые будут содержаться в контейнере
		 * "time_unit_names_choicebox_values" */
		final String[] time_unit_names_choicebox_strings =
			{ "Signs", "Short names", "Full names", "Digits only" };
		// Все именованные константы перечисления "Time_unit_layout"
		final Time_unit_layout[] time_unit_layout_values =
				Time_unit_layout.values();
		
		assert time_unit_names_choicebox_strings.length == time_unit_layout_values.length :
			"Array size doesn\'t match with " + Time_unit_layout.class.getName()
			+ " enum constants quantity";
		
		// Инициализация контейнера "time_unit_names_choicebox_values_init"
		for (final Time_unit_layout i : time_unit_layout_values)
		{
			time_unit_names_choicebox_values_init.put(
					i, time_unit_names_choicebox_strings[i.ordinal()]);
		}
		
		time_unit_names_choicebox_values =
				Collections.unmodifiableMap(time_unit_names_choicebox_values_init);
		settings = Settings.get_instance();
		
		// Инициализатор контейнера "time_display_style_choicebox_values"
		final Map<Time_display_style, String> time_display_style_choicebox_values_init =
				new EnumMap<>(Time_display_style.class);
		/* Строки значений, которые будут содержаться в контейнере
		 * "time_display_style_choicebox_values" */
		final String[] time_display_style_choicebox_strings =
			{ "If reaches", "Show all", "Custom strict", "Increase able" };
		// Все именованные константы перечисления "Time_display_style"
		final Time_display_style[] time_display_style_values =
				Time_display_style.values();
		
		assert time_display_style_choicebox_strings.length == time_display_style_values.length :
			"Array size doesn\'t match with " + Time_display_style.class.getName()
			+ " enum constants quantity";
		
		// Инициализация контейнера "time_display_style_choicebox_values"
		for (final Time_display_style i : time_display_style_values)
		{
			time_display_style_choicebox_values_init.put(
					i, time_display_style_choicebox_strings[i.ordinal()]);
		}
		
		time_display_style_choicebox_values =
				Collections.unmodifiableMap(time_display_style_choicebox_values_init);
	}
	

	///// Поля private экземпляра =========================================/////
	/** Панель выбора названий единиц времени согласно именованным константам
	 * перечисления {@link Time_unit_layout}. */
	@FXML
	private ChoiceBox<String> time_unit_names_choicebox;
	
	/** Содержит выбранное на текущий момент значение в
	 * {@link #time_unit_names_choicebox}. */
	private Time_unit_layout chosen_time_unit_layout;
	
	/** Панель выбора отображения значений счетчика времени согласно именованным
	 * константам перечисления {@link Time_display_style}. */
	@FXML
	private ChoiceBox<String> time_display_style_choicebox;
	
	/** Содержит выбранное на текущий момент значение в
	 * {@link #time_display_style_choicebox}. */
	private Time_display_style chosen_time_display_style;

	/** Текстовая метка, относящаяся к {@link #time_units_displayed_rangeslider}. */
	@FXML
	private Label time_units_displayed_range_label;
	
	/** Диапазонный слайдер для установки отображаемых единиц времени согласно
	 * именованным константам перечисления {@link Time_unit_name}. */
	@FXML
	private RangeSlider time_units_displayed_rangeslider;

	/** Содержит выбранное на текущий момент значение левого слайдера в
	 * {@link #time_units_displayed_rangeslider}. */
	private Time_unit_name left_chosen_displayed_edge;
	
	/** Содержит выбранное на текущий момент значение правого слайдера в
	 * {@link #time_units_displayed_rangeslider}. */
	private Time_unit_name right_chosen_displayed_edge;
	
	/** Показывает участок примера с единицами времени, которые могут
	 * отображаться когда будут иметь ненулевые значения. */
	@FXML
	private Label disabled_label;
	
	/** Показывает участок примера с единицами времени, которые должны
	 * отображаться всегда. */
	@FXML
	private Label enabled_label;
	
	/** Checkbox, указывающий нужно&nbsp;ли отображать создаваемое значение
	 * счетчика времени в поле заголовка программы (в&nbsp;этом&nbsp;случае
	 * значение будет также отображено на панели задач). */
	@FXML
	private CheckBox display_on_title_check_box;
	
	/** Кнопка справки, касающаяся {@link #time_unit_names_choicebox}. */
	@FXML
	private Button time_unit_names_hint_button;
	
	/** Кнопка справки, касающаяся {@link #time_display_style_choicebox}. */
	@FXML
	private Button time_display_style_hint_button;
	
	/** Кнопка справки, касающаяся {@link #time_units_displayed_rangeslider}. */
	@FXML
	private Button time_units_displayed_range_hint_button;
	
	/** Resource bundle representing <i>.properties</i> file which contains
	 * resources for time&nbsp;unit names. */
	private final ResourceBundle time_unit_name_resources;
	
	/** Time&nbsp;unit template names (symbolic, short, full and empty),
	 * distributed to groups according to enumeration constants
	 * {@link Time_unit_layout#TUL_value_sign},
	 * {@link Time_unit_layout#TUL_short_name},
	 * {@link Time_unit_layout#TUL_full_name} and
	 * {@link Time_unit_layout#TUL_digits_only} from
	 * {@link Time_unit_layout} enumeration.<br>
	 * {@link EnumMap#get(Object)} method returns
	 * {@code Map<}{@link Time_unit_name}{@code , }{@link String}{@code >}
	 * container containing time&nbsp;units keys for
	 * <i>time_obj.resources.time_counter_resources.properties</i> file
	 * according to specified group.<br>
	 * <b>Important!</b> <u>This container</u> and all <u>nested</u> in it
	 * {@code Map<}{@link Time_unit_name}{@code ,}{@link String}{@code >}
	 * containers are <u>immutable</u>. An&nbsp;attempt to change containers
	 * content results in <u>runtime exception</u>. */
	private final Map<Time_unit_layout, Map<Time_unit_name, String>> time_unit_names;
	
	/** Обновляет текст примера отображения счетчика времени в
	 * {@link #disabled_label} и {@link #enabled_label}. */
	private final Runnable example_text_modifier;
	
	/** Пул потоков, созданный для выполнения {@link #example_text_modifier}. */
	private final ExecutorService executor;
	
	
	///// Нестатическая инициализация =====================================/////
	{
		chosen_time_unit_layout = settings.get_time_unit_layout_setting();
		chosen_time_display_style = settings.get_time_display_style_setting();
		update_rangeslider_enabling_state();
		
		// Значения крайних отображаемых единиц времени
		final Time_unit_name[] display_edges = settings.get_time_value_edges();
		
		left_chosen_displayed_edge = display_edges[0];
		right_chosen_displayed_edge = display_edges[1];
		
		time_unit_name_resources = settings.get_time_counter_resources();
		
		// Time unit names to be stored in "time_unit_name_resources" container
		final String[][] time_unit_names_values = {
				{ time_unit_name_resources.getString("TUL_value_sign.years"),
					time_unit_name_resources.getString("TUL_value_sign.months"),
					time_unit_name_resources.getString("TUL_value_sign.days"),
					time_unit_name_resources.getString("TUL_value_sign.hours"),
					time_unit_name_resources.getString("TUL_value_sign.minutes"),
					time_unit_name_resources.getString("TUL_value_sign.seconds") },
				{ time_unit_name_resources.getString("TUL_short_name.years"),
					time_unit_name_resources.getString("TUL_short_name.months"),
					time_unit_name_resources.getString("TUL_short_name.days"),
					time_unit_name_resources.getString("TUL_short_name.hours"),
					time_unit_name_resources.getString("TUL_short_name.minutes"),
					time_unit_name_resources.getString("TUL_short_name.seconds") },
				/* TODO! These full time unit names are all match with value 10
				 * in all currently existing locales (English, Russian and
				 * Ukrainian). Some new locale may not fit the code provided for
				 * "example_text_modifier" Runnable. In this case the code need
				 * to be edited */
				{ time_unit_name_resources.getString("TUL_full_name.years.5"),
					time_unit_name_resources.getString("TUL_full_name.months.5"),
					time_unit_name_resources.getString("TUL_full_name.days.5"),
					time_unit_name_resources.getString("TUL_full_name.hours.5"),
					time_unit_name_resources.getString("TUL_full_name.minutes.5"),
					time_unit_name_resources.getString("TUL_full_name.seconds.5") },
				{ time_unit_name_resources.getString("TUL_digits_only.years"),
					time_unit_name_resources.getString("TUL_digits_only.months"),
					time_unit_name_resources.getString("TUL_digits_only.days"),
					time_unit_name_resources.getString("TUL_digits_only.hours"),
					time_unit_name_resources.getString("TUL_digits_only.minutes"),
					time_unit_name_resources.getString("TUL_digits_only.seconds") } };
		
		// All "Time_unit_name" enumeration constants
		final Time_unit_name[] time_unit_name_constants = Time_unit_name.values();
		
		assert time_unit_names_values[0].length == time_unit_name_constants.length &&
				time_unit_names_values[1].length == time_unit_name_constants.length &&
				time_unit_names_values[2].length == time_unit_name_constants.length &&
				time_unit_names_values[3].length == time_unit_name_constants.length :
					" Some subarrays\'s length doesn\'t match with "
						+ Time_unit_name.class.getName() + " enumeration constants quantity";
		
		// Four "time_unit_names_init" container initializers
		final Map<Time_unit_name, String> time_unit_names_init0 =
				new EnumMap<>(Time_unit_name.class),
				time_unit_names_init1 = new EnumMap<>(Time_unit_name.class),
				time_unit_names_init2 = new EnumMap<>(Time_unit_name.class),
				time_unit_names_init3 = new EnumMap<>(Time_unit_name.class);
		
		/* "time_unit_names_init" container initializers values assignment */
		for (final Time_unit_name i : time_unit_name_constants)
		{
			// Current enumeration constant ordinal value
			final int ordinal = i.ordinal();
			
			time_unit_names_init0.put(i,
					time_unit_names_values[Time_unit_layout.TUL_value_sign.ordinal()][ordinal]);
			time_unit_names_init1.put(i,
					time_unit_names_values[Time_unit_layout.TUL_short_name.ordinal()][ordinal]);
			time_unit_names_init2.put(i,
					time_unit_names_values[Time_unit_layout.TUL_full_name.ordinal()][ordinal]);
			time_unit_names_init3.put(i,
					time_unit_names_values[Time_unit_layout.TUL_digits_only.ordinal()][ordinal]);
		}
		
		// "time_unit_names" container initializer
		final Map<Time_unit_layout, Map<Time_unit_name, String>> time_unit_names_init =
				new EnumMap<>(Time_unit_layout.class);
		
		time_unit_names_init.put(Time_unit_layout.TUL_value_sign,
				Collections.unmodifiableMap(time_unit_names_init0));
		time_unit_names_init.put(Time_unit_layout.TUL_short_name,
				Collections.unmodifiableMap(time_unit_names_init1));
		time_unit_names_init.put(Time_unit_layout.TUL_full_name,
				Collections.unmodifiableMap(time_unit_names_init2));
		time_unit_names_init.put(Time_unit_layout.TUL_digits_only,
				Collections.unmodifiableMap(time_unit_names_init3));
		time_unit_names = Collections.unmodifiableMap(time_unit_names_init);
		
		example_text_modifier = new Runnable()
		{
			@Override
			public void run()
			{
				// Компоновщик строки примера для "disabled_label"
				final StringBuilder disabled_text = new StringBuilder();
				// Компоновщик строки примера для "enabled_label"
				final StringBuilder enabled_text = new StringBuilder();
				
				switch (chosen_time_display_style)
				{
				case TDS_if_reaches:
					disabled_text.append('0');
					disabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_years));
					disabled_text.append('0');
					disabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_months));
					disabled_text.append('0');
					disabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_days));
					
					enabled_text.append("10");
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_hours));
					enabled_text.append("00");
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_minutes));
					enabled_text.append("10");
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_seconds));
					
					break;
					
					
				case TDS_show_all:
					enabled_text.append('0');
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_years));
					enabled_text.append('0');
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_months));
					enabled_text.append('0');
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_days));
					enabled_text.append("10");
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_hours));
					enabled_text.append("00");
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_minutes));
					enabled_text.append("10");
					enabled_text.append(
							time_unit_names.get(chosen_time_unit_layout).
									get(Time_unit_name.TUN_seconds));
					
					break;
					
					
				case TDS_custom_strict:
					enabled_text.append(time_unit_name_resources.getString(
							"strict_display_mode_mark"));
					
					// Построение строк с примерами
					for (final Time_unit_name i : time_unit_name_constants)
					{
						/* Если данная единица времени вообще не может
						 * отображаться (находится за крайней правой
						 * (наименьшей) отображаемой единицей времени) */
						if (i.compareTo(right_chosen_displayed_edge) > 0)
						{
							break;
						}
						
						// Если данная единица времени не должна отображаться
						if (i.compareTo(left_chosen_displayed_edge) < 0)
						{
							disabled_text.append("10"
									+ time_unit_names.get(chosen_time_unit_layout).get(i));
						}
						// Данная единица времени должна отображаться
						else
						{
							// Если выводятся минуты ИЛИ секунды
							if (i.equals(Time_unit_name.TUN_minutes) ||
									i.equals(Time_unit_name.TUN_seconds))
							{
								enabled_text.append("00");
							}
							// Выводятся все остальные единицы времени
							else
							{
								enabled_text.append('0');
							}
							
							enabled_text.append(
									time_unit_names.get(chosen_time_unit_layout).get(i));
						}
					}
					
					break;
					
					
				case TDS_increase_able:
					// Построение строк с примерами
					for (final Time_unit_name i : time_unit_name_constants)
					{
						/* Если данная единица времени вообще не может
						 * отображаться (находится за крайней правой
						 * (наименьшей) отображаемой единицей времени) */
						if (i.compareTo(right_chosen_displayed_edge) > 0)
						{
							break;
						}
						
						/* Результат сравнения единицы времени текущей итерации
						 * с крайней левой (наибольшей) отображаемой единицей
						 * времени */
						final int compare_to_left_edge =
								i.compareTo(left_chosen_displayed_edge);
						
						/* Если данная единица времени может отображаться только
						 * имея ненулевое значение */
						if (compare_to_left_edge < 0)
						{
							// Если выводятся минуты
							if (i.equals(Time_unit_name.TUN_minutes))
							{
								disabled_text.append("00");
							}
							// Выводятся все остальные единицы времени
							else
							{
								disabled_text.append('0');
							}
							
							disabled_text.append(
									time_unit_names.get(chosen_time_unit_layout).get(i));
						}
						// Данная единица времени должна отображаться
						else
						{
							// Если выводятся минуты ИЛИ секунды
							if (i.equals(Time_unit_name.TUN_minutes) ||
									i.equals(Time_unit_name.TUN_seconds))
							{
								/* Условие тернарного оператора: Первая
								 * отображаемая единица времени должна быть 0 */
								enabled_text.append(
										compare_to_left_edge == 0 ? "00" : "10");
							}
							// Выводятся все остальные единицы времени
							else
							{
								/* Условие тернарного оператора: Первая
								 * отображаемая единица времени должна быть 0 */
								enabled_text.append(
										compare_to_left_edge == 0 ? "0" : "10");
							}
							
							enabled_text.append(
									time_unit_names.get(chosen_time_unit_layout).get(i));
						}
					}
					
					break;
					
					
				default:
					throw new EnumConstantNotPresentException(
							Time_display_style.class, chosen_time_display_style.name());
				}
				
				// Строка, которую нужно вывести в "disabled_label"
				final String disabled_label_string = disabled_text.toString();
				// Строка, которую нужно вывести в "enabled_label"
				final String enabled_label_string = enabled_text.toString();
				
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						disabled_label.setText(disabled_label_string);
						enabled_label.setText(enabled_label_string);
					}
				});
			}
		};
		
		executor = Executors.newCachedThreadPool();
	}
	
	
	///// Методы public экземпляра ========================================/////
	/**
	 * Возвращает выставленные настройки отображения создаваемого счетчика
	 * времени.
	 * 
	 * @return Настройки отображения счетчика времени.
	 */
	public Init_settings get_init_settings()
	{
		switch (chosen_time_display_style)
		{
		// В этих режимах диапазон отображаемых значений не нужен
		case TDS_if_reaches:
		case TDS_show_all:
			return new Init_settings(chosen_time_unit_layout,
					chosen_time_display_style, null, null,
					display_on_title_check_box.isSelected());
			
		// В этих режимах необходимо указывать диапазон отображаемых значений
		case TDS_custom_strict:
		case TDS_increase_able:
			return new Init_settings(chosen_time_unit_layout,
					chosen_time_display_style, left_chosen_displayed_edge,
					right_chosen_displayed_edge,
					display_on_title_check_box.isSelected());
			
		default:
			throw new EnumConstantNotPresentException(
					Time_display_style.class, chosen_time_display_style.name());
		}
	}
	
	
	/**
	 * Shuts&nbsp;down this controller threads.<br>
	 * This class has one {@link ExecutorService} which is kept alive for
	 * 1&nbsp;minute from last runnable executing, after which becomes dormant.
	 * However it&nbsp;is recommended to call this method to prevent possible
	 * delays if the&nbsp;whole program need to be shutdown immediately.
	 */
	public void shutdown()
	{
		executor.shutdown();
	}
	
	
	///// Методы private экземпляра =======================================/////
	/**
	 * Вызывается {@link FXMLLoader}'ом.
	 */
	@FXML
	private void initialize()
	{
		assert time_unit_names_choicebox != null :
			"time_unit_names_choicebox was not injected";
		assert time_display_style_choicebox != null :
			"time_display_style_choicebox was not injected";
		assert time_units_displayed_range_label != null :
			"time_units_displayed_range_label was not injected";
		assert time_units_displayed_rangeslider != null :
			"time_units_displayed_rangeslider was not injected";
		assert disabled_label != null : "disabled_label was not injected";
		assert enabled_label != null : "enabled_label was not injected";
		assert display_on_title_check_box != null :
			"display_on_title_check_box was not injected";
		assert time_unit_names_hint_button != null :
			"time_unit_names_hint_button was not injected";
		assert time_display_style_hint_button != null :
			"time_display_style_hint_button was not injected";
		assert time_units_displayed_range_hint_button != null :
			"time_units_displayed_range_hint_button was not injected";
		
		time_unit_names_choicebox.setItems(FXCollections.observableArrayList(
				time_unit_names_choicebox_values.values()));
		time_unit_names_choicebox.setValue(
				time_unit_names_choicebox_values.get(chosen_time_unit_layout));
		
		time_unit_names_choicebox.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				// Выбранный пользователем вариант
				final String chosen_variant = time_unit_names_choicebox.getValue();
				
				// Определение выбранного значения
				for (final Time_unit_layout i : Time_unit_layout.values())
				{
					// Если выбранное значение определено
					if (chosen_variant.equals(time_unit_names_choicebox_values.get(i)))
					{
						chosen_time_unit_layout = i;
						
						break;
					}
				}
				
				executor.execute(example_text_modifier);
			}
		});
		
		time_display_style_choicebox.setItems(FXCollections.observableArrayList(
				time_display_style_choicebox_values.values()));
		time_display_style_choicebox.setValue(
				time_display_style_choicebox_values.get(chosen_time_display_style));
		
		time_display_style_choicebox.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				// Выбранный пользователем вариант
				final String chosen_variant =
						time_display_style_choicebox.getValue();
				
				// Определение выбранного значения
				for (final Time_display_style i : Time_display_style.values())
				{
					if (chosen_variant.equals(time_display_style_choicebox_values.get(i)))
					{
						chosen_time_display_style = i;
						
						break;
					}
				}
				
				update_rangeslider_enabling_state();
				executor.execute(example_text_modifier);
			}
		});
		
		time_units_displayed_rangeslider.setLowValue(
				left_chosen_displayed_edge.ordinal());
		time_units_displayed_rangeslider.setHighValue(
				right_chosen_displayed_edge.ordinal());
		
		time_units_displayed_rangeslider.setLabelFormatter(new StringConverter<Number>()
		{
			@Override
			public String toString(final Number object)
			{
				// Преобразование значения со шкалы
				switch (object.intValue())
				{
				case 0:
					return "Years";
					
				case 1:
					return "Months";
					
				case 2:
					return "Days";
					
				case 3:
					return "Hours";
					
				case 4:
					return "Minutes";
					
				case 5:
					return "Seconds";
					
				default:
					throw new IllegalArgumentException("Unsupported value ("
							+ object.intValue() + ") has been passed as "
							+ Number.class.getName() + " argument");
				}
			}

			// Не переопределяется, т.к. использование не ожидается
			@Override
			public Number fromString(final String string)
			{
				throw new UnsupportedOperationException("fromString(String) method"
						+ " isn\'t overriden in StringConverter<Number> class");
			}
		});
		
		time_units_displayed_rangeslider.lowValueProperty().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(final ObservableValue<? extends Number> observable,
					final Number old_value, final Number new_value)
			{
				// Предыдущее округленное значение слайдера
				final long value_before = Math.round(old_value.doubleValue());
				// Теперешнее округленное значение слайдера
				final long value_now = Math.round(new_value.doubleValue());
				
				// Если целочисленный эквивалент значения изменился
				if (value_before != value_now)
				{
					/* Определение значения, которое должно быть присвоено
					 * "left_chosen_displayed_edge" */
					for (final Time_unit_name i : Time_unit_name.values())
					{
						// Если значение определено
						if (i.ordinal() == value_now)
						{
							left_chosen_displayed_edge = i;
							
							break;
						}
					}
					
					executor.execute(example_text_modifier);
				}
			}
		});
		
		time_units_displayed_rangeslider.highValueProperty().addListener(new ChangeListener<Number>()
		{
			@Override
			public void changed(final ObservableValue<? extends Number> observable,
					final Number old_value, final Number new_value)
			{
				// Предыдущее округленное значение слайдера
				final long value_before = Math.round(old_value.doubleValue());
				// Теперешнее округленное значение слайдера
				final long value_now = Math.round(new_value.doubleValue());
				
				// Если целочисленный эквивалент значения изменился
				if (value_before != value_now)
				{
					/* Определение значения, которое должно быть присвоено
					 * "right_chosen_displayed_edge" */
					for (final Time_unit_name i : Time_unit_name.values())
					{
						// Если значение определено
						if (i.ordinal() == value_now)
						{
							right_chosen_displayed_edge = i;
							
							break;
						}
					}
					
					executor.execute(example_text_modifier);
				}
			}
		});
		
		/* Перемещает фокус на соседние Node'ы при необходимости. Данный фильтр
		 * необходим из-за возможного "NullPointerException", причины которого
		 * указаны в описании класса */
		time_units_displayed_rangeslider.addEventFilter(
				KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>()
		{
			@Override
			public void handle(final KeyEvent event)
			{
				// Если нажат TAB
				if (event.getCode().equals(KeyCode.TAB))
				{
					// Зажат ли Shift
					final boolean shift_is_down = event.isShiftDown();
					
					// Если левый слайдер находится в фокусе И Shift зажат
					if (time_units_displayed_rangeslider.lookup(".low-thumb").isFocused() &&
							shift_is_down)
					{
						time_display_style_choicebox.requestFocus();
						event.consume();
					}
					// Если правый слайдер находится в фокусе И Shift не зажат
					else if (time_units_displayed_rangeslider.lookup(".high-thumb").isFocused() &&
							!shift_is_down)
					{
						display_on_title_check_box.requestFocus();
						event.consume();
					}
				}
			}
		});
		
		display_on_title_check_box.setTooltip(new Tooltip(
				"The time value is being displayed\nin task bar when this box is checked"));
	}
	
	
	/**
	 * Обработчик события нажатия кнопки {@link #time_unit_names_hint_button}.
	 * Выводит поясняющее сообщение над {@link #time_unit_names_choicebox}.
	 */
	@FXML
	private void time_unit_names_hint_button_on_action()
	{
		// Текст поясняющего сообщения
		final Label explanation_text = new Label(
				"The way in which time unit names will be represented.");
		// Окно всплывающего сообщения
		final PopOver hint = new PopOver(explanation_text);
		
		hint.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		hint.setDetachable(false);
		hint.setCornerRadius(0);
		hint.show(time_unit_names_choicebox);
	}
	
	
	/**
	 * Обработчик события нажатия кнопки {@link #time_display_style_hint_button}.
	 * Выводит поясняющее сообщение над {@link #time_display_style_choicebox}.
	 */
	@FXML
	private void time_display_style_hint_button_on_action()
	{
		/* Кол-во символов, память под которые будет выделяться для построения
		 * текста. Данное кол-во основано на размере добавляемых строк в
		 * "explanation_text_builder" */
		final int explanation_text_builder_capacity =
				55 + 10 + 162 + 8 + 60 + 13 + 206 + 5 + 10 + 13 + 121;  // Сумма - 663
		// Компоновщик текста для поясняющего сообщения
		final StringBuilder explanation_text_builder =
				new StringBuilder(explanation_text_builder_capacity);
		
		// Кол-во добавляемых символов - 55
		explanation_text_builder.append(
				"Determines mode in which time values are being shown.\n\"");
		// Кол-во добавляемых символов - 10
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_if_reaches));
		// Кол-во добавляемых символов - 162
		explanation_text_builder.append("\" \u2014 time values showing starts"
				+ " from biggest non zero value (e.g. current value\n\tof time"
				+ " counter is 10 minutes and 0 seconds, only these values are"
				+ " being shown).\n\"");
		// Кол-во добавляемых символов - 8
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_show_all));
		// Кол-во добавляемых символов - 60
		explanation_text_builder.append(
				"\" \u2014 all time values from years to seconds are being shown.\n\"");
		// Кол-во добавляемых символов - 13
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_custom_strict));
		// Кол-во добавляемых символов - 206
		explanation_text_builder.append("\" \u2014 only time values from \"Time"
				+ " units displayed range\" are being shown.\n\tOther time"
				+ " values won\'t be shown even in case of bigger than chosen"
				+ " range time value.\n\tNote: in this mode time value is marked with \"");
		// Кол-во добавляемых символов - 5
		explanation_text_builder.append(
				time_unit_name_resources.getString("strict_display_mode_mark"));
		// Кол-во добавляемых символов - 10
		explanation_text_builder.append("\" label.\n\"");
		// Кол-во добавляемых символов - 13
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_increase_able));
		// Кол-во добавляемых символов - 121
		explanation_text_builder.append("\" \u2014 Time values from \"Time units"
				+ " displayed range\" are always shown.\n\tBigger values only"
				+ " shown if time value reaches them.");
		
		// Текст поясняющего сообщения
		final Label explanation_text =
				new Label(explanation_text_builder.toString());
		// Окно всплывающего сообщения
		final PopOver hint = new PopOver(explanation_text);
		
		hint.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		hint.setDetachable(false);
		hint.setCornerRadius(0);
		hint.show(time_display_style_choicebox);
	}
	
	
	/**
	 * Обработчик события нажатия кнопки
	 * {@link #time_units_displayed_range_hint_button}. Выводит поясняющее
	 * сообщение над {@link #time_units_displayed_rangeslider}.
	 */
	@FXML
	private void time_units_displayed_range_hint_button_on_action()
	{
		/* Кол-во символов, память под которые будет выделяться для построения
		 * текста. Данное кол-во основано на размере добавляемых строк в
		 * "explanation_text_builder" */
		final int explanation_text_builder_capacity = 56 + 13 + 7 + 13 + 27;  // Сумма - 116
		// Компоновщик текста для поясняющего сообщения
		final StringBuilder explanation_text_builder =
				new StringBuilder(explanation_text_builder_capacity);
		
		// Кол-во добавляемых символов - 56
		explanation_text_builder.append(
				"Determines time values to be shown.\nCan be set only in \"");
		// Кол-во добавляемых символов - 13
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_custom_strict));
		// Кол-во добавляемых символов - 7
		explanation_text_builder.append("\" and \"");
		// Кол-во добавляемых символов - 13
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_increase_able));
		// Кол-во добавляемых символов - 27
		explanation_text_builder.append("\" time display style modes.");
		
		// Текст поясняющего сообщения
		final Label explanation_text =
				new Label(explanation_text_builder.toString());
		// Окно всплывающего сообщения
		final PopOver hint = new PopOver(explanation_text);
		
		hint.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		hint.setDetachable(false);
		hint.setCornerRadius(0);
		hint.show(time_units_displayed_rangeslider);
	}
	
	
	/**
	 * Активирует/деактивирует диапазонный слайдер и примыкающую к нему надпись
	 * в зависимости от установленного в {@link #chosen_time_display_style}
	 * значения.
	 */
	private void update_rangeslider_enabling_state()
	{
		/* true - необходимо деактивировать диапазонный слайдер; false -
		 * наоборот */
		final boolean disable_rangeslider;
		
		switch (chosen_time_display_style)
		{
		/* Данные режимы не требуют настройки диапазона отображения
		 * единиц времени */
		case TDS_if_reaches:
		case TDS_show_all:
			disable_rangeslider = true;
			
			break;
			
		/* Данным режимам необходима настройка диапазона отображения
		 * единиц времени */
		case TDS_custom_strict:
		case TDS_increase_able:
			disable_rangeslider = false;
			
			break;
			
		default:
			throw new EnumConstantNotPresentException(
					Time_display_style.class, chosen_time_display_style.name());
		}
		
		time_units_displayed_range_label.setDisable(disable_rangeslider);
		time_units_displayed_rangeslider.setDisable(disable_rangeslider);
	}
}