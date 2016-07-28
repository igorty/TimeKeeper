package graphical_shell.FXML_controllers;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.PopOver.ArrowLocation;

import graphical_shell.GUI_settings;
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
 * Time counter settings pane controller. Called by {@link FXMLLoader} for
 * <i>"graphical_shell/FXML_controllers/Init_settings_layout.fxml"</i>.<br>
 * <b>Important!</b> {@code FXML}&#8209;loader for this class and its
 * initialization <u>must</u> be invoked only when {@link Stage} object, on
 * which the&nbsp;class's pane is located, <u>is&nbsp;already&nbsp;shown</u>
 * (i.e.&nbsp;{@link Stage#show()} is already called). Otherwise
 * {@link NullPointerException} can be thrown in the&nbsp;class. The&nbsp;reason
 * is described in the&nbsp;link below.<br>
 * <i>Notes.</i>
 * <ul><li>Root pane in <i>"Init_settings_layout.fxml"</i> is {@link GridPane}.</li>
 * <li><i>"Init_settings_layout.fxml"</i> requires
 * <i>"graphical_shell/resources/GUI_elements/labels.properties"</i> resources to
 * be set.</li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 * 
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
		
		/** Leftmost displayed time&nbsp;unit. <u>Can</u> be {@code null} if
		 * {@link #time_display_style} is
		 * {@link Time_display_style#TDS_if_reaches} or
		 * {@link Time_display_style#TDS_show_all}. */
		public final Time_unit_name left_displayed_edge;
		
		/** Rightmost displayed time&nbsp;unit. <u>Can</u> be {@code null} if
		 * {@link #time_display_style} is
		 * {@link Time_display_style#TDS_if_reaches} or
		 * {@link Time_display_style#TDS_show_all}. */
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
	/** Program settings. */
	private static final Settings program_settings;
	/** Graphic user interface settings. */
	private static final GUI_settings gui_settings;
	
	
	static
	{
		program_settings = Settings.get_instance();
		gui_settings = GUI_settings.get_instance();
	}
	

	///// Поля private экземпляра =========================================/////
	/** Панель выбора названий единиц времени согласно именованным константам
	 * перечисления {@link Time_unit_layout}. */
	@FXML
	private ChoiceBox<String> time_unit_names_choicebox;
	
	/** Содержит выбранное на текущий момент значение в
	 * {@link #time_unit_names_choicebox}. */
	private Time_unit_layout chosen_time_unit_layout;
	
	/** Contains string items for {@link #time_unit_names_choicebox}.<br>
	 * <b>Warning!</b> The&nbsp;container is <u>immutable</u>. An&nbsp;attempt
	 * to change its content results in runtime exception. */
	private final Map<Time_unit_layout, String> time_unit_names_choicebox_values;
	
	/** Панель выбора отображения значений счетчика времени согласно именованным
	 * константам перечисления {@link Time_display_style}. */
	@FXML
	private ChoiceBox<String> time_display_style_choicebox;
	
	/** Содержит выбранное на текущий момент значение в
	 * {@link #time_display_style_choicebox}. */
	private Time_display_style chosen_time_display_style;
	
	/** Contains string items for {@link #time_display_style_choicebox}.<br>
	 * <b>Warning!</b> The&nbsp;container is <u>immutable</u>. An&nbsp;attempt
	 * to change its content results in runtime exception. */
	private final Map<Time_display_style, String> time_display_style_choicebox_values;

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
	
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * hints and tooltips texts. */
	private final ResourceBundle hints_resources;
	
	
	///// Нестатическая инициализация =====================================/////
	{
		chosen_time_unit_layout = program_settings.get_time_unit_layout_setting();
		chosen_time_display_style = program_settings.get_time_display_style_setting();
		
		// Значения крайних отображаемых единиц времени
		final Time_unit_name[] display_edges = program_settings.get_time_value_edges();
		
		left_chosen_displayed_edge = display_edges[0];
		right_chosen_displayed_edge = display_edges[1];
		
		time_unit_name_resources = program_settings.get_time_counter_resources();
		
		/* Resource bundle representing ".properties" resource which contains
		 * specific time counter settings names */
		final ResourceBundle time_counter_resources =
				gui_settings.get_time_counter_resources();
		
		///// "time_unit_names_choicebox_values" container initialization /////
		// "time_unit_names_choicebox_values" container initializer
		final Map<Time_unit_layout, String> time_unit_names_choicebox_values_init =
				new EnumMap<>(Time_unit_layout.class);
		// String items to be stored in "time_unit_names_choicebox_values"
		final String[] time_unit_names_choicebox_strings = {
				time_counter_resources.getString("time_units_layout.signs"),
				time_counter_resources.getString("time_units_layout.short_names"),
				time_counter_resources.getString("time_units_layout.full_names"),
				time_counter_resources.getString("time_units_layout.digits_only") };
		// All "Time_unit_layout" enumeration constants
		final Time_unit_layout[] time_unit_layout_values = Time_unit_layout.values();
		
		assert time_unit_names_choicebox_strings.length == time_unit_layout_values.length :
			"Array size doesn\'t match with " + Time_unit_layout.class.getName()
				+ " enum constants quantity";
		
		// "time_unit_names_choicebox_values_init" container initialization
		for (final Time_unit_layout i : time_unit_layout_values)
		{
			time_unit_names_choicebox_values_init.put(
					i, time_unit_names_choicebox_strings[i.ordinal()]);
		}
		
		time_unit_names_choicebox_values =
				Collections.unmodifiableMap(time_unit_names_choicebox_values_init);
		
		///// "time_display_style_choicebox_values" container initialization /////
		// "time_display_style_choicebox_values" container initializer
		final Map<Time_display_style, String> time_display_style_choicebox_values_init =
				new EnumMap<>(Time_display_style.class);
		// String items to be stored in "time_display_style_choicebox_values"
		final String[] time_display_style_choicebox_strings = {
				time_counter_resources.getString("time_display_style.if_reaches"),
				time_counter_resources.getString("time_display_style.show_all"),
				time_counter_resources.getString("time_display_style.custom_strict"),
				time_counter_resources.getString("time_display_style.increase_able") };
		// All "Time_display_style" enumeration constants
		final Time_display_style[] time_display_style_values =
				Time_display_style.values();
		
		assert time_display_style_choicebox_strings.length == time_display_style_values.length :
			"Array size doesn\'t match with " + Time_display_style.class.getName()
				+ " enum constants quantity";
		
		// "time_display_style_choicebox_values" container initialization
		for (final Time_display_style i : time_display_style_values)
		{
			time_display_style_choicebox_values_init.put(
					i, time_display_style_choicebox_strings[i.ordinal()]);
		}
		
		time_display_style_choicebox_values =
				Collections.unmodifiableMap(time_display_style_choicebox_values_init);
		
		///// "time_unit_names" container initialization /////
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
		hints_resources = gui_settings.get_hints_resources();
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
	 * delays if the&nbsp;whole program need to be shutdown immediately.<br>
	 * <b>Warning!</b> Using class instance after this method called may result
	 * in exception.
	 */
	public void shutdown()
	{
		executor.shutdown();
	}
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * Called by {@link FXMLLoader}.
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
				/* Resource bundle representing ".properties" resource which
				 * contains labels names */
				final ResourceBundle label_resources =
						gui_settings.get_labels_resources();
				
				// Преобразование значения со шкалы
				switch (object.intValue())
				{
				case 0:
					return label_resources.getString(
							"layout_settings.displayed_range.years");
					
				case 1:
					return label_resources.getString(
							"layout_settings.displayed_range.months");
					
				case 2:
					return label_resources.getString(
							"layout_settings.displayed_range.days");
					
				case 3:
					return label_resources.getString(
							"layout_settings.displayed_range.hours");
					
				case 4:
					return label_resources.getString(
							"layout_settings.displayed_range.minutes");
					
				case 5:
					return label_resources.getString(
							"layout_settings.displayed_range.seconds");
					
				default:
					throw new IllegalArgumentException(
							"Unsupported value (" + object.intValue()
									+ ") has been passed as "
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
				hints_resources.getString("layout_settings.display_on_title_hint")));
		update_rangeslider_enabling_state();
		executor.execute(example_text_modifier);
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
				hints_resources.getString("layout_settings.time_unit_names_hint"));
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
		/* Resource bundle representing ".properties" resource which contains
		 * labels names */
		final ResourceBundle labels_resources =
				gui_settings.get_labels_resources();
		
		/* Explanation text. Length is reserved according to minimal strings
		 * length that will be contained in */
		final StringBuilder explanation_text_builder = new StringBuilder(
				56 + 10 + 176 + 8 + 65 + 13 + 30 + 26 + 164 + 5 + 11 + 13 + 25 + 26 + 87);
		
		// 56 signs in default resource
		explanation_text_builder.append(hints_resources.getString(
				"layout_settings.time_display_style_hint.1"));
		// 10 signs in default resource
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_if_reaches));
		// 176 signs in default resource
		explanation_text_builder.append(hints_resources.getString(
				"layout_settings.time_display_style_hint.2"));
		// 8 signs in default resource
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_show_all));
		// 65 signs in default resource
		explanation_text_builder.append(hints_resources.getString(
				"layout_settings.time_display_style_hint.3"));
		// 13 signs in default resource
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_custom_strict));
		// 30 signs in default resource
		explanation_text_builder.append(hints_resources.getString(
				"layout_settings.time_display_style_hint.4"));
		// 26 signs in default resource
		explanation_text_builder.append(
				labels_resources.getString("layout_settings.displayed_range"));
		// 164 signs in default resource
		explanation_text_builder.append(hints_resources.getString(
				"layout_settings.time_display_style_hint.5"));
		// 5 signs in default resource
		explanation_text_builder.append(
				time_unit_name_resources.getString("strict_display_mode_mark"));
		// 11 signs in default resource
		explanation_text_builder.append(hints_resources.getString(
				"layout_settings.time_display_style_hint.6"));
		// 13 signs in default resource
		explanation_text_builder.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_increase_able));
		// 25 signs in default resource
		explanation_text_builder.append(hints_resources.getString(
				"layout_settings.time_display_style_hint.7"));
		// 26 signs in default resource
		explanation_text_builder.append(labels_resources.getString(
				"layout_settings.displayed_range"));
		// 87 signs in default resource
		explanation_text_builder.append(hints_resources.getString(
				"layout_settings.time_display_style_hint.8"));
		
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
		/* Explanation text. Length is reserved according to minimal strings
		 * length that will be contained in */
		final StringBuilder explanation_text =
				new StringBuilder(57 + 13 + 7 + 13 + 27);
		
		// 57 signs in default resource
		explanation_text.append(
				hints_resources.getString("layout_settings.displayed_range_hint.1"));
		// 13 signs in default resource
		explanation_text.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_custom_strict));
		// 7 signs in default resource
		explanation_text.append(
				hints_resources.getString("layout_settings.displayed_range_hint.2"));
		// 13 signs in default resource
		explanation_text.append(time_display_style_choicebox_values.get(
				Time_display_style.TDS_increase_able));
		// 27 signs in default resource
		explanation_text.append(
				hints_resources.getString("layout_settings.displayed_range_hint.3"));
		
		// Node to contain explanation text
		final Label label = new Label(explanation_text.toString());
		// Окно всплывающего сообщения
		final PopOver hint = new PopOver(label);
		
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
