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
package app.tar.graphical_shell;

import java.io.IOException;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.tar.graphical_shell.FXML_controllers.Basic_init_controller;
import app.tar.graphical_shell.FXML_controllers.Init_Instance_counter_controller;
import app.tar.graphical_shell.FXML_controllers.Init_Solo_counter_controller;
import app.tar.graphical_shell.FXML_controllers.Init_Time_counter_type_controller;
import app.tar.graphical_shell.FXML_controllers.Init_settings_controller;
import app.tar.graphical_shell.FXML_controllers.Init_Solo_counter_controller.Time_values;
import app.tar.graphical_shell.FXML_controllers.Init_settings_controller.Init_settings;
import app.tar.graphical_shell.dialog.Error_dialog;
import app.tar.graphical_shell.dialog.Error_dialog.Error_type;
import app.tar.graphical_shell.dialog.Error_dialog.Template_message;
import app.tar.graphical_shell.events.Locale_change_listener;
import app.tar.time_obj.Instance_counter;
import app.tar.time_obj.Mode;
import app.tar.time_obj.Solo_counter;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;


/**
 * Реализует окно создания счетчика времени. Экземпляр данного класса можно
 * использовать повторно.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
class New_time_counter_window
{
	///// Поля private статические ========================================/////
	/** Отвечает за логирование событий. */
	private static final Logger logger;
	
	/** Относительные директории изображений режимов подсчета времени.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера вызовет ошибку времени выполнения. */
	private static final Map<Mode, String> mode_img_directories;
	/** <i>Basic_init_layout.fxml</i> file's relative directory. */
	private static final String basic_init_layout_directory;
	/** <i>Init_Time_counter_type_layout.fxml</i> file's relative directory. */
	private static final String init_Time_counter_type_layout_directory;
	/** <i>Init_Solo_counter_layout.fxml</i> file's relative directory. */
	private static final String init_Solo_counter_layout_directory;
	/** <i>Init_Instance_counter_layout.fxml</i> file's relative directory. */
	private static final String init_Instance_counter_layout_directory;
	/** <i>Init_settings_layout.fxml</i> file's relative directory. */
	private static final String init_settings_layout_directory;
	
	/** Комбинация нажатия клавиш {@code Ctrl}&nbsp;{@code +}&nbsp;{@code Enter},
	 * которая должна приводить нажатию кнопки {@code Apply} на корневой панели
	 * компоновки. */
	private static final KeyCodeCombination shortcut_enter_key_combination;
	/** {@code Ctrl}&nbsp;{@code +}&nbsp;{@code Backspace} key&nbsp;combination
	 * which causes <i>Cancel</i> button fire on the&nbsp;{@link #root_pane}. */
	private static final KeyCodeCombination shortcut_backspace_key_combination;
	/** Комбинация нажатия клавиш {@code Ctrl}&nbsp;{@code +}&nbsp;{@code N},
	 * которая должна приводить к нажатию кнопки {@code Now} на панели
	 * компоновки, представленной объектом
	 * {@link Init_Instance_counter_controller}, в случае установки даты и
	 * времени для режима {@link Mode#M_elapsed_from}. */
	private static final KeyCodeCombination shortcut_n_key_combination;
	
	/** Текст всплывающей подсказки для клавиши, которая срабатывает при нажатии
	 * {@code Enter}. */
	private static final String enter_key_combination_text;
	/** Текст всплывающей подсказки для клавиши, которая срабатывает при нажатии
	 * {@code Esc}. */
	private static final String esc_key_combination_text;
	
	/** Graphic user interface settings. */
	private static final GUI_settings gui_settings;
	
	
	static
	{
		logger = Logger.getLogger(New_time_counter_window.class.getName());
		
		// Строки, которые должны содержаться в контейнере "mode_img_directories"
		final String[] mode_img_directories_strings = {
				"images/stopwatch.png",
				"images/countdown.png",
				"images/elapsed_from.png",
				"images/remains_till.png" };
		// Все элементы перечисления "Mode"
		final Mode[] mode_values = Mode.values();
		
		assert mode_img_directories_strings.length == mode_values.length :
			"Array size with values doesn\'t match with " + Mode.class.getName()
			+ " elements quantity";
		
		// Инициализатор контейнера "mode_img_directories"
		final Map<Mode, String> mode_img_directories_init =
				new EnumMap<>(Mode.class);
		
		// Инициализация контейнера "mode_img_directories_init"
		for (final Mode i : mode_values)
		{
			mode_img_directories_init.put(
					i, mode_img_directories_strings[i.ordinal()]);
		}
		
		mode_img_directories =
				Collections.unmodifiableMap(mode_img_directories_init);
		
		basic_init_layout_directory = "FXML_controllers/Basic_init_layout.fxml";
		init_Time_counter_type_layout_directory =
				"FXML_controllers/Init_Time_counter_type_layout.fxml";
		init_Solo_counter_layout_directory =
				"FXML_controllers/Init_Solo_counter_layout.fxml";
		init_Instance_counter_layout_directory =
				"FXML_controllers/Init_Instance_counter_layout.fxml";
		init_settings_layout_directory =
				"FXML_controllers/Init_settings_layout.fxml";
		
		shortcut_enter_key_combination =
				new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
		shortcut_backspace_key_combination =
				new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCodeCombination.SHORTCUT_DOWN);
		shortcut_n_key_combination = new KeyCodeCombination(
				KeyCode.N, KeyCombination.SHORTCUT_DOWN);
		enter_key_combination_text = "< " +
				new KeyCodeCombination(KeyCode.ENTER).getDisplayText() + " >";
		esc_key_combination_text = "< " +
				new KeyCodeCombination(KeyCode.ESCAPE).getDisplayText() + " >";
		
		gui_settings = GUI_settings.get_instance();
	}
	
	
	///// Поля private экземпляра =========================================/////
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * dialog messages. */
	private ResourceBundle messages_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * labels names. */
	private ResourceBundle labels_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * hints and tooltips texts. */
	private ResourceBundle hints_resources;
	
	/** {@code FXML}&#8209;загрузчик класса {@link Basic_init_controller},
	 * отвечающего за корневую панель компоновки. */
	private final FXMLLoader basic_init_controller_fxml_loader;
	/** Объект класса&#8209;контроллера, получаемый при помощи
	 * {@code FXML}&#8209;загрузчика {@link #basic_init_controller_fxml_loader}.
	 * Относится к панели компоновки {@link #root_pane}. */
	private final Basic_init_controller root_pane_controller;
	/** Корневая панель компоновки создаваемого окна. */
	private BorderPane root_pane;
	
	/** {@code FXML}&#8209;загрузчик класса
	 * {@link Init_Time_counter_type_controller}, отвечающего за компоновку
	 * панели выбора режима счетчика времени. */
	private final FXMLLoader init_Time_counter_type_controller_fxml_loader;
	/** Объект класса&#8209;контроллера, получаемый при помощи
	 * {@code FXML}&#8209;загрузчика
	 * {@link #init_Time_counter_type_controller_fxml_loader}. */
	private final Init_Time_counter_type_controller time_counter_type_set_pane_controller;
	/** Панель компоновки, в которой указывается тип (т.е.&nbsp;режим) счетчика
	 * времени. */
	private VBox time_counter_type_set_pane;
	
	/** Панель компоновки, основанная на {@link Init_Solo_counter_controller},
	 * для режима {@link Mode#M_stopwatch}. */
	private GridPane init_stopwatch_pane;
	/** Контроллер {@link #init_stopwatch_pane}. */
	private Init_Solo_counter_controller init_stopwatch_pane_controller;
	
	/** Панель компоновки, основанная на {@link Init_Solo_counter_controller},
	 * для режима {@link Mode#M_countdown}. */
	private GridPane init_countdown_pane;
	/** Контроллер {@link #init_countdown_pane}. */
	private Init_Solo_counter_controller init_countdown_pane_controller;
	
	/** Панель компоновки, основанная на {@link Init_Instance_counter_controller},
	 * для режима {@link Mode#M_elapsed_from}. */
	private GridPane init_elapsed_from_pane;
	/** Контроллер {@link #init_elapsed_from_pane}. */
	private Init_Instance_counter_controller init_elapsed_from_pane_controller;
	
	/** Layout pane based&nbsp;on {@link Init_Instance_counter_controller} for
	 * {@link Mode#M_remains_till} mode. */
	private GridPane init_remains_till_pane;
	/** {@link #init_remains_till_pane} layout&nbsp;pane controller. */
	private Init_Instance_counter_controller init_remains_till_pane_controller;
	
	/** Панель компоновки с настройками отображения времени, основанная на
	 * {@link Init_settings_controller}. */
	private GridPane init_settings_pane;
	/** Контроллер {@link #init_settings_pane}. */
	private Init_settings_controller init_settings_pane_controller;
	
	/** Сцена окна, создаваемого в этом классе. */
	private final Scene scene;
	/** Подмостки окна, создаваемого в этом классе. */
	private final Stage stage;
	
	/** Contains this object's window accelerators. */
	private final ObservableMap<KeyCombination, Runnable> accelerators;
	/** Предназначен для обработки нажатия комбинации клавиш
	 * {@code Ctrl}&nbsp;{@code +}&nbsp;{@code N}. */
	private final Runnable shortcut_n_accelerator;
	
	/** Handles moving from 1st to 2nd stage event of creating time counter. */
	private final EventHandler<ActionEvent> from_1_to_2_stage_event_handler;
	/** Handles moving from 2nd to 3rd stage event of creating time counter. */
	private final EventHandler<ActionEvent> from_2_to_3_stage_event_handler;
	/** Handles moving from 3rd to 2nd stage event of creating time counter. */
	private final EventHandler<ActionEvent> from_3_to_2_stage_event_handler;
	/** Handles moving from 2nd to 1st stage event of creating time counter. */
	private final EventHandler<ActionEvent> from_2_to_1_stage_event_handler;

	
	///// Нестатическая инициализация =====================================/////
	{
		messages_resources = gui_settings.get_messages_resources();
		labels_resources = gui_settings.get_labels_resources();
		hints_resources = gui_settings.get_hints_resources();
		
		basic_init_controller_fxml_loader = new FXMLLoader();
		basic_init_controller_fxml_loader.setLocation(
				New_time_counter_window.class.getResource(basic_init_layout_directory));
		basic_init_controller_fxml_loader.setResources(gui_settings.get_buttons_resources());
		root_pane = basic_init_controller_fxml_loader.load();
		root_pane_controller = basic_init_controller_fxml_loader.getController();
		
		init_Time_counter_type_controller_fxml_loader = new FXMLLoader();
		init_Time_counter_type_controller_fxml_loader.setLocation(
				New_time_counter_window.class.getResource(
						init_Time_counter_type_layout_directory));
		init_Time_counter_type_controller_fxml_loader.setResources(
				gui_settings.get_time_counter_resources());
		time_counter_type_set_pane =
				init_Time_counter_type_controller_fxml_loader.load();
		time_counter_type_set_pane_controller =
				init_Time_counter_type_controller_fxml_loader.getController();
		
		root_pane_controller.cancel_button.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				close_window();
				back_to_initial_state();
			}
		});

		init_stopwatch_pane = null;
		init_stopwatch_pane_controller = null;
		
		init_countdown_pane = null;
		init_countdown_pane_controller = null;
		
		init_elapsed_from_pane = null;
		init_elapsed_from_pane_controller = null;
		
		init_remains_till_pane = null;
		init_remains_till_pane_controller = null;
		
		init_settings_pane = null;
		init_settings_pane_controller = null;
		
		stage = new Stage();
		stage.addEventFilter(KeyEvent.ANY, Main_class.window_input_filter);
		stage.addEventFilter(
				MouseEvent.MOUSE_CLICKED, Main_class.window_input_filter);
		
		
		stage.setOnCloseRequest(new EventHandler<WindowEvent>()
		{
			@Override
			public void handle(final WindowEvent event)
			{
				close_window();
				back_to_initial_state();
			}
		});
		
		shortcut_n_accelerator = new Runnable()
		{
			@Override
			public void run()
			{
				init_elapsed_from_pane_controller.fire_now_button();
			}
		};
		
		from_1_to_2_stage_event_handler = new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* FXML-загрузчик панели компоновки второго этапа создания
				 * счетчика времени */
				final FXMLLoader stage_2_fxml_loader;
				// Выбранный пользователем режим счетчика времени
				final Mode time_counter_mode =
						time_counter_type_set_pane_controller.get_selected_mode();
				
				// Choosing a pane to show
				switch (time_counter_mode)
				{
				case M_stopwatch:
					/* Если панель компоновки для указания значений секундомера
					 * еще не инициализировалась */
					if (init_stopwatch_pane == null)
					{
						stage_2_fxml_loader = new FXMLLoader();
						stage_2_fxml_loader.setLocation(
								New_time_counter_window.class.getResource(
										init_Solo_counter_layout_directory));
						stage_2_fxml_loader.setResources(labels_resources);
						
						try
						{
							init_stopwatch_pane = stage_2_fxml_loader.load();
						}
						catch (final IOException exc)
						{
							logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
									+ " fxml layout. Exception stack trace:", exc);
							Error_dialog.show_message(
									Template_message.TM_layout_build);
							close_window();
							back_to_initial_state();
							
							return;
						}
						
						init_stopwatch_pane_controller =
								stage_2_fxml_loader.getController();
					}
					
					root_pane.setCenter(init_stopwatch_pane);
					
					root_pane_controller.apply_button.setOnAction(
							new EventHandler<ActionEvent>()
					{
						@Override
						public void handle(final ActionEvent event)
						{
							// Исходные значения времени для режима секундомера
							final Time_values init_time_values =
									init_stopwatch_pane_controller.get_time_values();
							
							// Если пользователь ввел некорректное значение
							if (init_time_values == null)
							{
								return;
							}
							
							/* If user hasn't moved to wizard's 3rd stage which
							 * contains time counter layout settings */
							if (init_settings_pane == null)
							{
								// Newly created time counter
								final Solo_counter new_stopwatch =
										new Solo_counter(
												Mode.M_stopwatch,
												init_time_values.period,
												init_time_values.duration,
												init_time_values.days_count);
								
								Time_counters_pane.add_time_counter_to_pane(
										new_stopwatch);
							}
							else
							{
								final Init_settings init_settings =
										init_settings_pane_controller.get_init_settings();
								// Newly created time counter
								final Solo_counter new_stopwatch =
										new Solo_counter(
												Mode.M_stopwatch,
												init_time_values.period,
												init_time_values.duration,
												init_time_values.days_count,
												init_settings.time_display_style,
												init_settings.left_displayed_edge,
												init_settings.right_displayed_edge,
												init_settings.time_unit_layout);
								
								Time_counters_pane.add_time_counter_to_pane(
										new_stopwatch);
								/* TODO: Set created time counter's value
								 * display on title if requested */
							}
							
							close_window();
							back_to_initial_state();
						}
					});
					
					break;
					
				case M_countdown:
					/* Если панель компоновки для указания значений таймера еще
					 * не инициализировалась */
					if (init_countdown_pane == null)
					{
						stage_2_fxml_loader = new FXMLLoader();
						stage_2_fxml_loader.setLocation(
								New_time_counter_window.class.getResource(
										init_Solo_counter_layout_directory));
						stage_2_fxml_loader.setResources(labels_resources);
						
						try
						{
							init_countdown_pane = stage_2_fxml_loader.load();
						}
						catch (final IOException exc)
						{
							logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
									+ " fxml layout. Exception stack trace:", exc);
							Error_dialog.show_message(
									Template_message.TM_layout_build);
							close_window();
							back_to_initial_state();
							
							return;
						}
						
						init_countdown_pane_controller =
								stage_2_fxml_loader.getController();
					}
					
					root_pane.setCenter(init_countdown_pane);
					
					root_pane_controller.apply_button.setOnAction(
							new EventHandler<ActionEvent>()
					{
						@Override
						public void handle(final ActionEvent event)
						{
							// Исходные значения времени для режима таймера
							final Time_values init_time_values =
									init_countdown_pane_controller.get_time_values();
							
							// Если пользователь ввел некорректное значение
							if (init_time_values == null)
							{
								return;
							}
							
							/* true - time value is 0 (is inappropriate for
							 * timer mode); false - initial timer value is more
							 * than 0 */
							boolean is_0 = false;
							
							// Initial timer value cannot be 0
							if (init_time_values.period == null &&
									init_time_values.duration == null)
							{
								is_0 = true;
							}
							else if (init_time_values.period == null &&
									init_time_values.duration != null &&
									init_time_values.duration.equals(LocalTime.of(0, 0, 0)))
							{
								is_0 = true;
							}
							else if (init_time_values.duration == null &&
									init_time_values.period != null &&
									init_time_values.period.isZero())
							{
								is_0 = true;
							}
							
							// If initial timer value is 0
							if (is_0)
							{
								init_countdown_pane_controller.set_warning_text(
										labels_resources.getString(
												"warning.incorrect_timer_value"));
								
								return;
							}
							
							/* If user hasn't moved to wizard's 3rd stage which
							 * contains time counter layout settings */
							if (init_settings_pane == null)
							{
								// Newly created time counter
								final Solo_counter new_timer = new Solo_counter(
										Mode.M_countdown,
										init_time_values.period,
										init_time_values.duration,
										init_time_values.days_count);
								
								Time_counters_pane.add_time_counter_to_pane(
										new_timer);
							}
							else
							{
								final Init_settings init_settings =
										init_settings_pane_controller.get_init_settings();
								
								// Newly created time counter
								final Solo_counter new_timer = new Solo_counter(
										Mode.M_countdown,
										init_time_values.period,
										init_time_values.duration,
										init_time_values.days_count,
										init_settings.time_display_style,
										init_settings.left_displayed_edge,
										init_settings.right_displayed_edge,
										init_settings.time_unit_layout);
								
								Time_counters_pane.add_time_counter_to_pane(
										new_timer);
								/* TODO: Set created time counter's value
								 * display on title if requested */
							}
							
							close_window();
							back_to_initial_state();
						}
					});
					
					break;
					
				case M_elapsed_from:
					/* Если панель компоновки для режима прошедшего времени с
					 * указанного момента еще не инициализировалась */
					if (init_elapsed_from_pane == null)
					{
						stage_2_fxml_loader = new FXMLLoader();
						stage_2_fxml_loader.setLocation(
								New_time_counter_window.class.getResource(
										init_Instance_counter_layout_directory));
						stage_2_fxml_loader.setResources(labels_resources);
						
						try
						{
							init_elapsed_from_pane = stage_2_fxml_loader.load();
						}
						catch (final IOException exc)
						{
							logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
									+ " fxml layout. Exception stack trace:", exc);
							Error_dialog.show_message(
									Template_message.TM_layout_build);
							close_window();
							back_to_initial_state();
							
							return;
						}
						
						init_elapsed_from_pane_controller =
								stage_2_fxml_loader.getController();
						
						init_elapsed_from_pane_controller.set_tooltip_to_now_button(
								"< " + shortcut_n_key_combination.getDisplayText() + " >");
					}
					
					accelerators.put(
							shortcut_n_key_combination, shortcut_n_accelerator);
					
					root_pane.setCenter(init_elapsed_from_pane);
					
					root_pane_controller.apply_button.setOnAction(
							new EventHandler<ActionEvent>()
					{
						@Override
						public void handle(final ActionEvent event)
						{
							/* Целевые значения даты и времени, введенные
							 * пользователем */
							final ZonedDateTime init_date_time =
									init_elapsed_from_pane_controller.get_date_time();
							
							// Если пользователь ввел некорректные данные
							if (init_date_time == null)
							{
								return;
							}
							
							/* If user hasn't moved to wizard's 3rd stage which
							 * contains time counter layout settings */
							if (init_settings_pane == null)
							{
								// Newly created time counter
								final Instance_counter new_elapsed_from =
										new Instance_counter(
												Mode.M_elapsed_from, init_date_time);
								
								Time_counters_pane.add_time_counter_to_pane(
										new_elapsed_from);
							}
							else
							{
								final Init_settings init_settings =
										init_settings_pane_controller.get_init_settings();
								// Newly created time counter
								final Instance_counter new_elapsed_from =
										new Instance_counter(
												Mode.M_elapsed_from,
												init_date_time,
												init_settings.time_display_style,
												init_settings.left_displayed_edge,
												init_settings.right_displayed_edge,
												init_settings.time_unit_layout);
								
								Time_counters_pane.add_time_counter_to_pane(
										new_elapsed_from);
								/* TODO: Set created time counter's value
								 * display on title if requested */
							}
							
							close_window();
							back_to_initial_state();
						}
					});
					
					break;
					
				case M_remains_till:
					/* Если панель компоновки для режима таймера обратного
					 * отсчета с привязкой к будущей дате еще не инициализировалась */
					if (init_remains_till_pane == null)
					{
						stage_2_fxml_loader = new FXMLLoader();
						stage_2_fxml_loader.setLocation(
								New_time_counter_window.class.getResource(
										init_Instance_counter_layout_directory));
						stage_2_fxml_loader.setResources(labels_resources);
						
						try
						{
							init_remains_till_pane = stage_2_fxml_loader.load();
						}
						catch (final IOException exc)
						{
							logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
									+ " fxml layout. Exception stack trace:", exc);
							Error_dialog.show_message(
									Template_message.TM_layout_build);
							close_window();
							back_to_initial_state();
							
							return;
						}
						
						init_remains_till_pane_controller =
								stage_2_fxml_loader.getController();
					}
					
					init_remains_till_pane_controller.make_now_button_disabled();
					root_pane.setCenter(init_remains_till_pane);
					
					root_pane_controller.apply_button.setOnAction(
							new EventHandler<ActionEvent>()
					{
						@Override
						public void handle(final ActionEvent event)
						{
							/* Целевые значения даты и времени, введенные
							 * пользователем */
							final ZonedDateTime init_date_time =
									init_remains_till_pane_controller.get_date_time();
							
							// Если пользователь ввел некорректные данные
							if (init_date_time == null)
							{
								return;
							}
							
							/* If user hasn't moved to wizard's 3rd stage which
							 * contains time counter layout settings */
							if (init_settings_pane == null)
							{
								// Newly created time counter
								final Instance_counter new_remains_till =
										new Instance_counter(
												Mode.M_remains_till, init_date_time);
								
								Time_counters_pane.add_time_counter_to_pane(
										new_remains_till);
							}
							else
							{
								final Init_settings init_settings =
										init_settings_pane_controller.get_init_settings();
								// Newly created time counter
								final Instance_counter new_remains_till =
										new Instance_counter(
												Mode.M_remains_till,
												init_date_time,
												init_settings.time_display_style,
												init_settings.left_displayed_edge,
												init_settings.right_displayed_edge,
												init_settings.time_unit_layout);
								
								Time_counters_pane.add_time_counter_to_pane(
										new_remains_till);
								/* TODO: Set created time counter's value
								 * display on title if requested */
							}
							
							close_window();
							back_to_initial_state();
						}
					});
					
					break;
					
				default:
					throw new EnumConstantNotPresentException(
							Mode.class, time_counter_mode.name());
				}
				
				root_pane_controller.previous_button.setOnAction(
						from_2_to_1_stage_event_handler);
				root_pane_controller.next_button.setOnAction(
						from_2_to_3_stage_event_handler);
				
				root_pane_controller.apply_button.setDisable(false);
				root_pane_controller.previous_button.setDisable(false);
				root_pane_controller.cancel_button.setCancelButton(false);
				root_pane_controller.previous_button.setCancelButton(true);
				
				accelerators.put(shortcut_enter_key_combination, new Runnable()
				{
					@Override
					public void run()
					{
						root_pane_controller.apply_button.fire();
					}
				});
				
				root_pane_controller.mode_image.setImage(new Image(
						New_time_counter_window.class.getResource(
								mode_img_directories.get(time_counter_mode)).toString()));
				root_pane_controller.layout_description.setText(
						labels_resources.getString("new_time_counter_wizard.stage.2"));
				
				root_pane_controller.previous_button.setTooltip(
						new Tooltip(esc_key_combination_text));
				root_pane_controller.apply_button.setTooltip(new Tooltip(
						"< " + shortcut_enter_key_combination.getDisplayText() + " >"));
				root_pane_controller.cancel_button.getTooltip().setText(
						"< " + shortcut_backspace_key_combination.getDisplayText() + " >");
			}
		};
		
		from_2_to_3_stage_event_handler = new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* Начальные значения счетчика времени в режиме секундомера или
				 * таймера */
				Time_values init_Solo_counter_values;
				/* Target date time value for time counter in "M_elapsed_from"
				 * and "M_remains_till" modes */
				ZonedDateTime init_Instance_counter_value;
				// Режим создаваемого счетчика времени
				final Mode selected_mode =
						time_counter_type_set_pane_controller.get_selected_mode();
				
				/* Проверка корректности введенных значений для счетчика времени
				 * перед переходом 3-му этапу создания счетчика времени */
				switch (selected_mode)
				{
				case M_stopwatch:
					init_Solo_counter_values =
							init_stopwatch_pane_controller.get_time_values();
					
					// Если пользователь ввел некорректное значение
					if (init_Solo_counter_values == null)
					{
						return;
					}
					
					break;
					
				case M_countdown:
					init_Solo_counter_values =
							init_countdown_pane_controller.get_time_values();
					
					// Если пользователь ввел некорректное значение
					if (init_Solo_counter_values == null)
					{
						return;
					}
					
					/* true - time value is 0 (is inappropriate for
					 * timer mode); false - initial timer value is more
					 * than 0 */
					boolean is_0 = false;
					
					// Initial timer value cannot be 0
					if (init_Solo_counter_values.period == null &&
							init_Solo_counter_values.duration == null)
					{
						is_0 = true;
					}
					else if (init_Solo_counter_values.period == null &&
							init_Solo_counter_values.duration != null &&
							init_Solo_counter_values.duration.equals(LocalTime.of(0, 0, 0)))
					{
						is_0 = true;
					}
					else if (init_Solo_counter_values.duration == null &&
							init_Solo_counter_values.period != null &&
							init_Solo_counter_values.period.isZero())
					{
						is_0 = true;
					}
					
					// If initial timer value is 0
					if (is_0)
					{
						init_countdown_pane_controller.set_warning_text(
								labels_resources.getString(
										"warning.incorrect_timer_value"));
						
						return;
					}
					
					break;
					
				case M_elapsed_from:
					init_Instance_counter_value =
							init_elapsed_from_pane_controller.get_date_time();
					
					// Если пользователь ввел некорректные значения
					if (init_Instance_counter_value == null)
					{
						return;
					}
					
					break;
					
				case M_remains_till:
					init_Instance_counter_value =
							init_remains_till_pane_controller.get_date_time();
					
					// Если пользователь ввел некорректные значения
					if (init_Instance_counter_value == null)
					{
						return;
					}
					
					break;
					
				default:
					throw new EnumConstantNotPresentException(
							Mode.class, selected_mode.name());
				}
				
				/* Если панель компоновки с настройками отображения времени еще
				 * не инициализировалась */
				if (init_settings_pane == null)
				{
					/* FXML-загрузчик панели компоновки третьего этапа создания
					 * счетчика времени */
					final FXMLLoader stage_3_fxml_loader = new FXMLLoader();
					stage_3_fxml_loader.setLocation(
							New_time_counter_window.class.getResource(
									init_settings_layout_directory));
					stage_3_fxml_loader.setResources(labels_resources);
					
					try
					{
						init_settings_pane = stage_3_fxml_loader.load();
					}
					catch (final IOException exc)
					{
						logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
								+ " fxml layout. Exception stack trace:", exc);
						Error_dialog.show_message(
								Error_type.ET_IO,
								messages_resources.getString(
										"error.cannot_show_settings_pane"));
						/* Окно создания счетчика времени умышленно
						 * не закрывается в этом случае, т.к. в крайнем случае
						 * счетчик времени можно создать и с дефолтными
						 * настройками отображения */
						
						return;
					}
					
					init_settings_pane_controller =
							stage_3_fxml_loader.getController();
				}
				
				root_pane.setCenter(init_settings_pane);
				accelerators.remove(shortcut_n_key_combination);
				
				root_pane_controller.next_button.setDefaultButton(false);
				root_pane_controller.next_button.setDisable(true);
				root_pane_controller.apply_button.setDefaultButton(true);
				
				root_pane_controller.apply_button.setOnAction(
						new EventHandler<ActionEvent>()
				{
					@Override
					public void handle(final ActionEvent event)
					{
						final Init_settings init_settings =
								init_settings_pane_controller.get_init_settings();
						
						switch (selected_mode)
						{
						case M_stopwatch:
							// Initial stopwatch mode values
							final Time_values init_stopwatch_values =
									init_stopwatch_pane_controller.get_time_values();
							
							// If user entered incorrect time values
							if (init_stopwatch_values == null)
							{
								return;
							}
							
							// Newly created time counter
							final Solo_counter new_stopwatch =
									new Solo_counter(
											Mode.M_stopwatch,
											init_stopwatch_values.period,
											init_stopwatch_values.duration,
											init_stopwatch_values.days_count,
											init_settings.time_display_style,
											init_settings.left_displayed_edge,
											init_settings.right_displayed_edge,
											init_settings.time_unit_layout);
							
							Time_counters_pane.add_time_counter_to_pane(
									new_stopwatch);
							/* TODO: Set created time counter's value display on
							 * title if requested */
							
							break;
							
						case M_countdown:
							// Initial timer mode values
							final Time_values init_timer_values =
									init_countdown_pane_controller.get_time_values();
							
							// If user entered incorrect time values
							if (init_timer_values == null)
							{
								return;
							}
							
							/* true - time value is 0 (is inappropriate for
							 * timer mode); false - initial timer value is more
							 * than 0 */
							boolean is_0 = false;
							
							// Initial timer value cannot be 0
							if (init_timer_values.period == null &&
									init_timer_values.duration == null)
							{
								is_0 = true;
							}
							else if (init_timer_values.period == null &&
									init_timer_values.duration != null &&
									init_timer_values.duration.equals(LocalTime.of(0, 0, 0)))
							{
								is_0 = true;
							}
							else if (init_timer_values.duration == null &&
									init_timer_values.period != null &&
									init_timer_values.period.isZero())
							{
								is_0 = true;
							}
							
							// If initial timer value is 0
							if (is_0)
							{
								init_countdown_pane_controller.set_warning_text(
										labels_resources.getString(
												"warning.incorrect_timer_value"));
								
								return;
							}
							
							// Newly created time counter
							final Solo_counter new_timer = new Solo_counter(
									Mode.M_countdown,
									init_timer_values.period,
									init_timer_values.duration,
									init_timer_values.days_count,
									init_settings.time_display_style,
									init_settings.left_displayed_edge,
									init_settings.right_displayed_edge,
									init_settings.time_unit_layout);
							
							Time_counters_pane.add_time_counter_to_pane(new_timer);
							/* TODO: Set created time counter's value display on
							 * title if requested */
							
							break;
							
						case M_elapsed_from:
							// Target date time values set by user
							final ZonedDateTime init_elapsed_from_time =
									init_elapsed_from_pane_controller.get_date_time();
							
							// If user entered incorrect date time value
							if (init_elapsed_from_time == null)
							{
								return;
							}
							
							// Newly created time counter
							final Instance_counter new_elapsed_from =
									new Instance_counter(
											Mode.M_elapsed_from,
											init_elapsed_from_time,
											init_settings.time_display_style,
											init_settings.left_displayed_edge,
											init_settings.right_displayed_edge,
											init_settings.time_unit_layout);
							
							Time_counters_pane.add_time_counter_to_pane(
									new_elapsed_from);
							/* TODO: Set created time counter's value display on
							 * title if requested */
							
							break;
							
						case M_remains_till:
							// Target date time values set by user
							final ZonedDateTime init_remains_till_time =
									init_remains_till_pane_controller.get_date_time();
							
							// If user entered incorrect date time value
							if (init_remains_till_time == null)
							{
								return;
							}
							
							// Newly created time counter
							final Instance_counter new_remains_till =
									new Instance_counter(
											Mode.M_remains_till,
											init_remains_till_time,
											init_settings.time_display_style,
											init_settings.left_displayed_edge,
											init_settings.right_displayed_edge,
											init_settings.time_unit_layout);
							
							Time_counters_pane.add_time_counter_to_pane(
									new_remains_till);
							/* TODO: Set created time counter's value display on
							 * title if requested */
							
							break;
							
						default:
							throw new EnumConstantNotPresentException(
									Mode.class, selected_mode.name());
						}
						
						close_window();
						back_to_initial_state();
					}
				});
				
				root_pane_controller.previous_button.setOnAction(
						from_3_to_2_stage_event_handler);
				
				root_pane_controller.layout_description.setText(
						labels_resources.getString("new_time_counter_wizard.stage.3"));
				
				root_pane_controller.next_button.setTooltip(null);
				root_pane_controller.apply_button.getTooltip().setText(
						"< " + shortcut_enter_key_combination.getDisplayText()
								+ " > "
								+ hints_resources.getString("accelerators.or")
								+ ' ' + enter_key_combination_text);
			}
		};
		
		from_3_to_2_stage_event_handler = new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				// Выбранный пользователем режим счетчика времени
				final Mode time_counter_mode =
						time_counter_type_set_pane_controller.get_selected_mode();
				
				// Выбор панели компоновки, к которой нужно откатиться
				switch (time_counter_mode)
				{
				case M_stopwatch:
					root_pane.setCenter(init_stopwatch_pane);
					
					break;
					
				case M_countdown:
					root_pane.setCenter(init_countdown_pane);
					
					break;
					
				case M_elapsed_from:
					root_pane.setCenter(init_elapsed_from_pane);
					accelerators.put(
							shortcut_n_key_combination, shortcut_n_accelerator);
					
					break;
					
				case M_remains_till:
					root_pane.setCenter(init_remains_till_pane);
					
					break;
					
				default:
					throw new EnumConstantNotPresentException(
							Mode.class, time_counter_mode.name());
				}
				
				root_pane_controller.previous_button.setOnAction(
						from_2_to_1_stage_event_handler);
				root_pane_controller.next_button.setOnAction(
						from_2_to_3_stage_event_handler);
				
				root_pane_controller.apply_button.setDefaultButton(false);
				root_pane_controller.next_button.setDefaultButton(true);
				root_pane_controller.next_button.setDisable(false);
				
				root_pane_controller.layout_description.setText(
						labels_resources.getString("new_time_counter_wizard.stage.2"));
				
				root_pane_controller.next_button.setTooltip(
						new Tooltip(enter_key_combination_text));
				root_pane_controller.apply_button.getTooltip().setText(
						"< " + shortcut_enter_key_combination + " >");
			}
		};
		
		from_2_to_1_stage_event_handler = new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				back_to_initial_state();
				root_pane.setCenter(time_counter_type_set_pane);
				
				root_pane_controller.layout_description.setText(
						labels_resources.getString("new_time_counter_wizard.stage.1"));
			}
		};
		
		gui_settings.add_Locale_change_listener(new Locale_change_listener()
		{
			@Override
			public void locale_changed()
			{
				messages_resources = gui_settings.get_messages_resources();
				labels_resources = gui_settings.get_labels_resources();
				hints_resources = gui_settings.get_hints_resources();
			}
		});
	}
	
	
	///// Конструкторы по умолчанию =======================================/////
	/**
	 * @param owner Owner window which will be blocked while this object's
	 * window is shown.
	 * 
	 * @throws IOException Не&nbsp;удалось загрузить панель компоновки из
	 * {@code FXML}&#8209;файла.
	 * 
	 * @exception NullPointerException Received argument is&nbsp;{@code null}.
	 */
	New_time_counter_window(final Window owner) throws IOException
	{
		// Аргумент метода не может быть null
		if (owner == null)
		{
			throw new NullPointerException(
					Window.class.getName() + " argument is null");
		}
		
		scene = new Scene(root_pane);
		accelerators = scene.getAccelerators();
		
		stage.setScene(scene);
		stage.initOwner(owner);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setResizable(false);
	}
	
	
	///// Методы по умолчанию экземпляра ==================================/////
	/**
	 * Отображает окно для создания нового счетчика времени и создает новый
	 * счетчик времени. Окно содержит три этапа создания счетчика времени:
	 * <ol><li>Выбор режима в котором будет работать счетчик времени, согласно
	 * именованным константам перечисления {@link Mode}. Является
	 * <u>обязательным</u>.</li>
	 * <li>Установка исходных значений для счетчика времени. Является
	 * <u>обязательным</u>.</li>
	 * <li>Установка настроек отображения счетчика времени. Является
	 * <u>необязательным</u>.</li></ol>
	 */
	void show_window()
	{
		set_root_pane_to_initial_state();
		root_pane.setCenter(time_counter_type_set_pane);
		stage.show();
	}
	
	
	///// Методы private экземпляра =======================================/////
	/**
	 * Закрывает окно этого класса и обнуляет ссылки на объекты, которые
	 * не&nbsp;будут использоваться при следующем вызове метода
	 * {@link #show_window()}.
	 */
	private void close_window()
	{
		stage.close();
		
		init_stopwatch_pane = null;
		init_stopwatch_pane_controller = null;
		
		init_countdown_pane = null;
		init_countdown_pane_controller = null;
		
		init_elapsed_from_pane = null;
		init_elapsed_from_pane_controller = null;
		
		init_remains_till_pane = null;
		init_remains_till_pane_controller = null;
		
		init_settings_pane = null;
		
		// Need to terminate its inner threads
		if (init_settings_pane_controller != null)
		{
			init_settings_pane_controller.shutdown();
			init_settings_pane_controller = null;
		}
	}
	
	
	/**
	 * Sets {@link #root_pane} to initial state.
	 */
	private void set_root_pane_to_initial_state()
	{
		root_pane_controller.next_button.setTooltip(
				new Tooltip(enter_key_combination_text));
		root_pane_controller.cancel_button.setTooltip(new Tooltip(
				"< " + shortcut_backspace_key_combination.getDisplayText() + " > "
						+ hints_resources.getString("accelerators.or") + ' '
						+ esc_key_combination_text));
		
		root_pane_controller.next_button.setOnAction(
				from_1_to_2_stage_event_handler);
		
		accelerators.put(shortcut_backspace_key_combination, new Runnable()
		{
			@Override
			public void run()
			{
				root_pane_controller.cancel_button.fire();
			}
		});
		
		root_pane_controller.layout_description.setText(
				labels_resources.getString("new_time_counter_wizard.stage.1"));
	}
	
	
	/**
	 * Sets {@link #root_pane} back to initial state at the&nbsp;end of
	 * executing {@link #show_window()}.
	 */
	private void back_to_initial_state()
	{
		set_root_pane_to_initial_state();
		root_pane_controller.mode_image.setImage(null);
		
		root_pane_controller.apply_button.setDefaultButton(false);
		root_pane_controller.next_button.setDefaultButton(true);
		root_pane_controller.previous_button.setCancelButton(false);
		root_pane_controller.cancel_button.setCancelButton(true);
		
		root_pane_controller.previous_button.setDisable(true);
		root_pane_controller.apply_button.setDisable(true);
		root_pane_controller.next_button.setDisable(false);
		
		accelerators.remove(shortcut_enter_key_combination);
		accelerators.remove(shortcut_n_key_combination);
		
		root_pane_controller.previous_button.setTooltip(null);
		root_pane_controller.apply_button.setTooltip(null);
	}
}
