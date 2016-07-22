package main_package;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import main_package.FXML_controllers.Basic_init_controller;
import main_package.FXML_controllers.Init_Instance_counter_controller;
import main_package.FXML_controllers.Init_Solo_counter_controller;
import main_package.FXML_controllers.Init_Time_counter_type_controller;
import main_package.FXML_controllers.Init_settings_controller;
import main_package.FXML_controllers.Init_settings_controller.Init_settings;
import main_package.FXML_controllers.Init_Solo_counter_controller.Time_values;
import main_package.dialog.Error_dialog;
import main_package.dialog.Error_dialog.Template_message;
import time_obj.Mode;


/* TODO: При создании нового счетчика времени перезаписывать файл настроек
 * новым стилем отображения */
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
	
	/** Комбинация нажатия клавиш {@code Ctrl}&nbsp;{@code +}&nbsp;{@code Enter},
	 * которая должна приводить нажатию кнопки {@code Apply} на корневой панели
	 * компоновки. */
	private static final KeyCodeCombination shortcut_enter_key_combination;
	/** Комбинация нажатия клавиш {@code Ctrl}&nbsp;{@code +}&nbsp;{@code Esc},
	 * которая должна приводить к нажатию кнопки {@code Cancel} на корневой
	 * панели компоновки. */
	private static final KeyCodeCombination shortcut_esc_key_combination;
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
	
	
	static
	{
		logger = Logger.getLogger(New_time_counter_window.class.getName());
		
		// Строки, которые должны содержаться в контейнере "mode_img_directories"
		final String[] mode_img_directories_strings = {
				"../images/stopwatch_large.png", "../images/countdown_large.png",
				"../images/elapsed_from_large.png", "../images/countdown_till_large.png" };
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
		shortcut_enter_key_combination =
				new KeyCodeCombination(KeyCode.ENTER, KeyCombination.SHORTCUT_DOWN);
		shortcut_esc_key_combination =
				new KeyCodeCombination(KeyCode.ESCAPE, KeyCodeCombination.SHORTCUT_DOWN);
		shortcut_n_key_combination = new KeyCodeCombination(
				KeyCode.N, KeyCombination.SHORTCUT_DOWN);
		enter_key_combination_text = "< " +
				new KeyCodeCombination(KeyCode.ENTER).getDisplayText() + " >";
		esc_key_combination_text = "< " +
				new KeyCodeCombination(KeyCode.ESCAPE).getDisplayText() + " >";
	}
	
	
	///// Поля private экземпляра =========================================/////
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
	
	/** Панель компоновки, основанная на {@link Init_Instance_counter_controller},
	 * для режима {@link Mode#M_countdown_till}. */
	private GridPane init_countdown_till_pane;
	/** Контроллер {@link #init_countdown_till_pane}. */
	private Init_Instance_counter_controller init_countdown_till_pane_controller;
	
	/** Панель компоновки с настройками отображения времени, основанная на
	 * {@link Init_settings_controller}. */
	private GridPane init_settings_pane;
	/** Контроллер {@link #init_settings_pane}. */
	private Init_settings_controller init_settings_pane_controller;
	
	/** Сцена окна, создаваемого в этом классе. */
	private final Scene new_time_counter_window_scene;
	/** Подмостки окна, создаваемого в этом классе. */
	private final Stage new_time_counter_window_stage;
	
	/** Предназначен для обработки нажатия комбинации клавиш
	 * {@code Ctrl}&nbsp;{@code +}&nbsp;{@code N}. */
	private final Runnable shortcut_n_accelerator;

	
	///// Нестатическая инициализация =====================================/////
	{
		basic_init_controller_fxml_loader = new FXMLLoader();
		basic_init_controller_fxml_loader.setLocation(
				New_time_counter_window.class.getResource("Basic_init_layout.fxml"));
		root_pane_controller = basic_init_controller_fxml_loader.getController();
		
		init_Time_counter_type_controller_fxml_loader = new FXMLLoader();
		init_Time_counter_type_controller_fxml_loader.setLocation(
				New_time_counter_window.class.getResource(
						"Init_Time_counter_type_layout.fxml"));
		time_counter_type_set_pane_controller =
				init_Time_counter_type_controller_fxml_loader.getController();

		init_stopwatch_pane = null;
		init_stopwatch_pane_controller = null;
		
		init_countdown_pane = null;
		init_countdown_pane_controller = null;
		
		init_elapsed_from_pane = null;
		init_elapsed_from_pane_controller = null;
		
		init_countdown_till_pane = null;
		init_countdown_till_pane_controller = null;
		
		init_settings_pane = null;
		init_settings_pane_controller = null;
		
		new_time_counter_window_stage = new Stage();
		
		shortcut_n_accelerator = new Runnable()
		{
			@Override
			public void run()
			{
				init_elapsed_from_pane_controller.fire_now_button();
			}
		};
	}
	
	
	///// Конструкторы по умолчанию =======================================/////
	/**
	 * @throws IOException Не&nbsp;удалось загрузить панель компоновки из
	 * {@code FXML}&#8209;файла.
	 */
	New_time_counter_window() throws IOException
	{
		root_pane = basic_init_controller_fxml_loader.load();
		time_counter_type_set_pane =
				init_Time_counter_type_controller_fxml_loader.load();
		new_time_counter_window_scene = new Scene(root_pane);
	}
	
	
	///// Методы по умолчанию экземпляра ==================================/////
	// TODO: Создать мнемоники для быстрых действий вместо нажатия клавиш
	/* TODO: Создать исходные всплывающие сообщения с указанием мнемоник над
	 * клавишами */
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
	 * 
	 * @param owner_window Окно вызывающей области видимости, доступ к которому
	 * будет блокирован на время отображения окна данного метода.
	 * 
	 * @throws IOException Не&nbsp;удалось получить панель компоновки из
	 * {@code fxml}&#8209;файла.
	 * 
	 * @exception NullPointerException В качестве аргумента передан {@code null}.
	 */
	void show_window(final Window owner_window) throws IOException
	{
		// Аргумент метода не может быть null
		if (owner_window == null)
		{
			throw new NullPointerException(
					Window.class.getName() + " argument is null");
		}
		
		root_pane.setCenter(time_counter_type_set_pane);
		
		// TODO: Переместить в глобальную область
		root_pane_controller.cancel_button.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				close_window();
				set_defaults();
			}
		});
		
		// TODO [next]: Переместить в глобальную область
		/* Обработчик события перехода с 1-го ко 2-му этапу создания счетчика
		 * времени */
		final EventHandler<ActionEvent> from_1_to_2_stage_event_handler =
				new EventHandler<ActionEvent>()
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
										"Init_Solo_counter_layout.fxml"));
						init_stopwatch_pane_controller =
								stage_2_fxml_loader.getController();
						
						try
						{
							init_stopwatch_pane = stage_2_fxml_loader.load();
						}
						catch (final IOException exc)
						{
							logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
									+ " fxml layout. Exception\'s stack trace:", exc);
							Error_dialog.show_IO_error_message(
									Template_message.TM_layout_build);
							close_window();
							set_defaults();
							
							return;
						}
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
							
							// TODO: Создать счетчик времени в режиме секундомера
							close_window();
							set_defaults();
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
										"Init_Solo_counter_layout.fxml"));
						init_countdown_pane_controller =
								stage_2_fxml_loader.getController();
						
						try
						{
							init_countdown_pane = stage_2_fxml_loader.load();
						}
						catch (final IOException exc)
						{
							logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
									+ " fxml layout. Exception\'s stack trace:", exc);
							Error_dialog.show_IO_error_message(
									Template_message.TM_layout_build);
							close_window();
							set_defaults();
							
							return;
						}
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
							
							// TODO: Создать счетчик времени в режиме таймера
							close_window();
							set_defaults();
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
										"Init_Instance_counter_layout.fxml"));
						init_elapsed_from_pane_controller =
								stage_2_fxml_loader.getController();
						
						try
						{
							init_elapsed_from_pane = stage_2_fxml_loader.load();
						}
						catch (final IOException exc)
						{
							logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
									+ " fxml layout. Exception\'s stack trace:", exc);
							Error_dialog.show_IO_error_message(
									Template_message.TM_layout_build);
							close_window();
							set_defaults();
							
							return;
						}
						
						init_elapsed_from_pane_controller.set_tooltip_to_now_button(
								"< " + shortcut_n_key_combination.getDisplayText() + " >");
					}
					
					new_time_counter_window_scene.getAccelerators().put(
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
							
							/* TODO: Создать счетчик времени в режиме прошедшего
							 * времени с указанного момента */
							close_window();
							set_defaults();
						}
					});
					
					break;
					
				case M_countdown_till:
					/* Если панель компоновки для режима таймера обратного
					 * отсчета с привязкой к будущей дате еще не инициализировалась */
					if (init_countdown_till_pane == null)
					{
						stage_2_fxml_loader = new FXMLLoader();
						stage_2_fxml_loader.setLocation(
								New_time_counter_window.class.getResource(
										"Init_Instance_counter_layout.fxml"));
						init_countdown_till_pane_controller =
								stage_2_fxml_loader.getController();
						
						try
						{
							init_countdown_till_pane = stage_2_fxml_loader.load();
						}
						catch (final IOException exc)
						{
							logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
									+ " fxml layout. Exception\'s stack trace:", exc);
							Error_dialog.show_IO_error_message(
									Template_message.TM_layout_build);
							close_window();
							set_defaults();
							
							return;
						}
					}
					
					init_countdown_till_pane_controller.make_now_button_disabled();
					root_pane.setCenter(init_countdown_till_pane);
					
					root_pane_controller.apply_button.setOnAction(
							new EventHandler<ActionEvent>()
					{
						@Override
						public void handle(final ActionEvent event)
						{
							/* Целевые значения даты и времени, введенные
							 * пользователем */
							final ZonedDateTime init_date_time =
									init_countdown_till_pane_controller.get_date_time();
							
							// Если пользователь ввел некорректные данные
							if (init_date_time == null)
							{
								return;
							}
							
							/* TODO: Создать счетчик времени в режиме таймера
							 * обратного отсчета с привязкой к будущей дате */
							close_window();
							set_defaults();
						}
					});
					
					break;
					
				default:
					throw new EnumConstantNotPresentException(
							Mode.class, time_counter_mode.name());
				}
				
				// TODO: Установить обработчик события для кнопки "Previous"
				root_pane_controller.apply_button.setDisable(false);
				root_pane_controller.previous_button.setDisable(false);
				root_pane_controller.mode_image.setImage(
						new Image(mode_img_directories.get(time_counter_mode)));
			}
		};
		
		// TODO: Переместить в глобальную область
		/* Обработчик события перехода с 2-го к 3-му этапу создания счетчика
		 * времени */
		final EventHandler<ActionEvent> from_2_to_3_stage_event_handler =
				new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* Начальные значения счетчика времени в режиме секундомера или
				 * таймера */
				Time_values init_Solo_counter_values;
				/* Целевое значение даты и времени для счетчика времени в режиме
				 * "M_elapsed_from" или "M_countdown_till" */
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
					
				case M_countdown_till:
					init_Instance_counter_value =
							init_countdown_till_pane_controller.get_date_time();
					
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
									"Init_settings_layout.fxml"));
					init_settings_pane_controller =
							stage_3_fxml_loader.getController();
					
					try
					{
						init_settings_pane = stage_3_fxml_loader.load();
					}
					catch (final IOException exc)
					{
						logger.log(Level.SEVERE, "Fatal error. Cannot obtain"
								+ " fxml layout. Exception\'s stack trace:", exc);
						Error_dialog.show_IO_error_message(
								"Cannot show time counter display settings.");
						/* Окно создания счетчика времени умышленно
						 * не закрывается в этом случае, т.к. в крайнем случае
						 * счетчик времени можно создать и с дефолтными
						 * настройками отображения */
						
						return;
					}
				}
				
				root_pane.setCenter(init_settings_pane);
				new_time_counter_window_scene.getAccelerators().remove(
						shortcut_n_key_combination);
				root_pane_controller.next_button.setDefaultButton(false);
				root_pane_controller.next_button.setDisable(true);
				root_pane_controller.apply_button.setDefaultButton(true);
				
				root_pane_controller.apply_button.setOnAction(
						new EventHandler<ActionEvent>()
				{
					@Override
					public void handle(final ActionEvent event)
					{
						// Настройки отображения счетчика времени
						final Init_settings init_settings =
								init_settings_pane_controller.get_init_settings();
						
						switch (selected_mode)
						{
						case M_stopwatch:
							// TODO: Создать счетчик времени в режиме секундомера
							
							break;
							
						case M_countdown:
							// TODO: Создать счетчик времени в режиме таймера
							
							break;
							
						case M_elapsed_from:
							/* TODO: Создать счетчик времени в режиме подсчета
							 * прошедшего времени с указанного момента */
							
							break;
							
						case M_countdown_till:
							/* TODO: Создать счетчик времени в режиме таймера
							 * обратного отсчета до указанного момента */
							
							break;
							
						default:
							throw new EnumConstantNotPresentException(
									Mode.class, selected_mode.name());
						}
						
						close_window();
						set_defaults();
					}
				});
				
				// TODO: Обработчик события для кнопки "Previous"
				
				root_pane_controller.next_button.setTooltip(null);
				root_pane_controller.apply_button.getTooltip().setText(
						"< " + shortcut_enter_key_combination.getDisplayText()
								+ " > or " + enter_key_combination_text);
			}
		};
		
		// TODO: Переместить в глобальную область
		/* Обработчик события перехода с 3-го к 2-му этапу создания счетчика
		 * времени */
		final EventHandler<ActionEvent> from_3_to_2_stage_event_handler =
				new EventHandler<ActionEvent>()
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
					new_time_counter_window_scene.getAccelerators().put(
							shortcut_n_key_combination, shortcut_n_accelerator);
					
					break;
					
				case M_countdown_till:
					root_pane.setCenter(init_countdown_till_pane);
					
					break;
					
				default:
					throw new EnumConstantNotPresentException(
							Mode.class, time_counter_mode.name());
				}
				
				// TODO: Обработчик события нажатия кнопки "Previous"
				
				root_pane_controller.next_button.setOnAction(
						from_2_to_3_stage_event_handler);
				root_pane_controller.apply_button.setDefaultButton(false);
				root_pane_controller.next_button.setDefaultButton(true);
				root_pane_controller.next_button.setDisable(false);
				root_pane_controller.apply_button.getTooltip().setText(
						"< " + shortcut_enter_key_combination + " >");
				root_pane_controller.next_button.setTooltip(
						new Tooltip(enter_key_combination_text));
			}
		};
		
		// TODO: Переместить в глобальную область
		/* Обработчик события перехода с 2-го к 1-му этапу создания счетчика
		 * времени */
		final EventHandler<ActionEvent> from_2_to_1_stage_event_handler =
				new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				set_defaults();
				root_pane.setCenter(time_counter_type_set_pane);
			}
		};
	}
	
	
	///// Методы private экземпляра =======================================/////
	/**
	 * Закрывает окно этого класса и обнуляет ссылки на объекты, которые
	 * не&nbsp;будут использоваться при следующем вызове метода
	 * {@link #show_window(Window)}.
	 */
	private void close_window()
	{
		new_time_counter_window_stage.close();
		
		init_stopwatch_pane = null;
		init_stopwatch_pane_controller = null;
		
		init_countdown_pane = null;
		init_countdown_pane_controller = null;
		
		init_elapsed_from_pane = null;
		init_elapsed_from_pane_controller = null;
		
		init_countdown_till_pane = null;
		init_countdown_till_pane_controller = null;
		
		init_settings_pane = null;
		
		// Need to terminate its inner threads
		if (init_settings_pane_controller != null)
		{
			init_settings_pane_controller.shutdown();
			init_settings_pane_controller = null;
		}
	}
	
	
	/**
	 * Приводит {@link #root_pane} к исходному состоянию.
	 */
	private void set_defaults()
	{
		root_pane_controller.mode_image.setImage(null);
		
		root_pane_controller.apply_button.setDefaultButton(false);
		root_pane_controller.next_button.setDefaultButton(true);
		root_pane_controller.previous_button.setDefaultButton(false);
		root_pane_controller.cancel_button.setDefaultButton(true);
		
		root_pane_controller.previous_button.setTooltip(null);
		root_pane_controller.next_button.setTooltip(
				new Tooltip(enter_key_combination_text));
		root_pane_controller.apply_button.setTooltip(null);
		root_pane_controller.cancel_button.getTooltip().setText(
				"< " + shortcut_esc_key_combination.getDisplayText() + " > or "
						+ esc_key_combination_text);
		
		// Мнемоники окна этого класса
		final ObservableMap<KeyCombination, Runnable> accelerators =
				new_time_counter_window_scene.getAccelerators();
		
		accelerators.remove(shortcut_enter_key_combination);
		accelerators.remove(shortcut_n_key_combination);
	}
}