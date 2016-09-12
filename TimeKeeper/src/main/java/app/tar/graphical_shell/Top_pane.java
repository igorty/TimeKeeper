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
package graphical_shell;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import graphical_shell.dialog.Error_dialog;
import graphical_shell.dialog.Error_dialog.Template_message;
import graphical_shell.events.Locale_change_listener;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import time_obj.Mode;
import time_obj.Time_counter;
import time_obj.Time_counter_control;
import time_obj.containers.Modified_ArrayList;


/**
 * Implements {@link MenuBar} and {@link ToolBar} of the&nbsp;main&nbsp;window.
 * <p><b>Important!</b> It is critically&nbsp;needed to call
 * {@link #apply_primary_stage(Stage)} method <u>before providing menu&nbsp;bar
 * to user</u>. Otherwise next menu&nbsp;items will&nbsp;throw
 * {@link NullPointerException} when using them:
 * <ul><li><i>File&rArr;Exit</i>;</li>
 * <li><i>File&rArr;New time&nbsp;counter</i>.</li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
class Top_pane
{
	//**************************************************************************
	//                                                                         *
	// Fields private static                                                   *
	//                                                                         *
	//**************************************************************************
	/** Logs this class's events. */
	private static final Logger logger;

	/** Graphic user interface settings. */
	private static final GUI_settings gui_settings;
	
	/** Contains all existing {@link Time_counter} objects. */
	private static final Modified_ArrayList time_counters;
	
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * hints and tooltips texts. */
	private static ResourceBundle hints_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * menu&nbsp;items names. */
	private static ResourceBundle menu_items_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * dialog messages. */
	private static ResourceBundle messages_resources;
	
	/** Contains {@link MenuBar} and {@link ToolBar}. */
	private static final VBox top_pane;
	
	/** New time&nbsp;counter wizard. */
	private static New_time_counter_window new_time_counter_window;

	/** Saves {@link Time_counter} objects to file.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>File&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem save_menu_item;
	/** Shuts down the program.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>File&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem exit_menu_item;
	/** Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;this submenu</i>. */
	private static final Menu stopwatches_submenu;
	/** Starts/resumes all {@link Time_counter} objects working in
	 * {@link Mode#M_stopwatch} mode.
	 * <p>Place in menu&nbsp;bar hierarchy:
	 * <i>Actions&rArr;Stopwatches&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem start_stopwatches_menu_item;
	/** Pauses all {@link Time_counter} objects working in
	 * {@link Mode#M_stopwatch} mode.
	 * <p>Place in menu&nbsp;bar hierarchy:
	 * <i>Actions&rArr;Stopwatches&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem pause_stopwatches_menu_item;
	/** Restarts all {@link Time_counter} objects working in
	 * {@link Mode#M_stopwatch} mode.
	 * <p>Place in menu&nbsp;bar hierarchy:
	 * <i>Actions&rArr;Stopwatches&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem restart_stopwatches_menu_item;
	/** Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;this submenu</i>. */
	private static final Menu timers_submenu;
	/** Starts/resumes all {@link Time_counter} objects working in
	 * {@link Mode#M_countdown} mode.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;Timers&rArr;this
	 * menu&nbsp;item</i>. */
	private static final MenuItem start_timers_menu_item;
	/** Pauses all {@link Time_counter} objects working in
	 * {@link Mode#M_countdown} mode.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;Timers&rArr;this
	 * menu&nbsp;item</i>. */
	private static final MenuItem pause_timers_menu_item;
	/** Restarts all {@link Time_counter} objects working in
	 * {@link Mode#M_countdown} mode.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;Timers&rArr;this
	 * menu&nbsp;item</i>. */
	private static final MenuItem restart_timers_menu_item;
	/** Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;this submenu</i>. */
	private static final Menu stopwatches_timers_submenu;
	/** Starts/resumes all {@link Time_counter} objects working in
	 * {@link Mode#M_stopwatch} and {@link Mode#M_countdown} modes.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;Both Stopwatches and
	 * Timers&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem start_stopwatches_timers_menu_item;
	/** Pauses all {@link Time_counter} objects working in
	 * {@link Mode#M_stopwatch} and {@link Mode#M_countdown} modes.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;Both Stopwatches and
	 * Timers&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem pause_stopwatches_timers_menu_item;
	/** Restarts all {@link Time_counter} objects working in
	 * {@link Mode#M_stopwatch} and {@link Mode#M_countdown} modes.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;Both Stopwatches and
	 * Timers&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem restart_stopwatches_timers_menu_item;
	/** Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;this submenu</i>. */
	private static final Menu elapsed_from_submenu;
	/** Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;this submenu</i>. */
	private static final Menu remains_till_submenu;
	/** Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;this submenu</i>. */
	private static final Menu elapsed_from_remains_till_submenu;
	/** Closes all {@link Time_counter} objects.
	 * <p>Place in menu&nbsp;bar hierarchy: <i>Actions&rArr;this menu&nbsp;item</i>. */
	private static final MenuItem close_all_time_counters_menu_item;
	
	/** Launches new time&nbsp;counter wizard window. */
	private static final Button new_time_counter_button;
	
	
	static
	{
		logger = Logger.getLogger(Top_pane.class.getName());
		gui_settings = GUI_settings.get_instance();
		time_counters = Time_counter_control.get_instance().get_time_counters();
		new_time_counter_window = null;
		
		hints_resources = gui_settings.get_hints_resources();
		menu_items_resources = gui_settings.get_menu_items_resources();
		messages_resources = gui_settings.get_messages_resources();
		
		// Image for controls that create new time counter
		final Image new_time_counter_image = new Image(
				Top_pane.class.getResource("resources/images/new.png").toString());
		

		///// Menu bar implementation -------------------------------------/////
		final double menu_items_image_size = 16;
		
		
		/////-/ File menu from menu bar implementation --------------------/////
		/////-// New time counter menu item implementation ----------------/////
		// "new_time_counter_menu_item" image
		final ImageView new_time_counter_menu_item_image =
				new ImageView(new_time_counter_image);
		
		new_time_counter_menu_item_image.setFitWidth(menu_items_image_size);
		new_time_counter_menu_item_image.setPreserveRatio(true);
		
		/* Launches "new_time_counter_button".
		 * Place in menu bar hierarchy: 'File→this menu item' */
		final MenuItem new_time_counter_menu_item = new MenuItem(
				menu_items_resources.getString("new_time_counter"),
				new_time_counter_menu_item_image);
		
		new_time_counter_menu_item.setAccelerator(
				new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
		
		new_time_counter_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				new_time_counter_button.fire();
			}
		});
		/////-//-----------------------------------------------------------/////
		

		/////-// Save menu item implementation ----------------------------/////
		save_menu_item = new MenuItem(
				menu_items_resources.getString("save"),
				new ImageView(Top_pane.class.getResource(
						"resources/images/save.png").toString()));
		
		save_menu_item.setAccelerator(
				new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
		
		save_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						Time_counters_pane.sort_and_save_time_counters();
						
						final PopOver save_succeed = new PopOver(new Label(
								messages_resources.getString(
										"notification.save_succeed")));
						
						save_succeed.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
						save_succeed.setDetachable(false);
						save_succeed.setCornerRadius(0);
						save_succeed.setArrowSize(0);
						
						Platform.runLater(new Runnable()
						{
							@Override
							public void run()
							{
								/* TODO! Unexpected behavior. Exception can be
								 * thrown when PopOver object is still showing
								 * and window is closing */
								save_succeed.show(
										Time_counters_pane.get_time_counters_pane());
							}
						});
						
						try
						{
							Thread.sleep(2_000);
						}
						// Exception isn't expected
						catch (final InterruptedException exc)
						{
							logger.log(
									Level.WARNING,
									"Thread got interrupted flag, although it"
											+ " is\u00A0not expected here."
											+ " Exception stack trace:",
									exc);
						}
						
						save_succeed.hide(new Duration(1_000));
					}
				}).start();
			}
		});
		/////-//-----------------------------------------------------------/////
		
		
		/////-// Exit menu item implementation ----------------------------/////
		exit_menu_item = new MenuItem(menu_items_resources.getString("exit"));
		exit_menu_item.setAccelerator(
				new KeyCodeCombination(KeyCode.F4, KeyCombination.ALT_DOWN));
		/////-//-----------------------------------------------------------/////
		
		
		// Top-level menu
		final Menu file_menu = new Menu(
				menu_items_resources.getString("file"),
				null,
				new_time_counter_menu_item, save_menu_item,
				new SeparatorMenuItem(), exit_menu_item);
		/////-/------------------------------------------------------------/////
		
		
		/////-/ Options menu from menu bar implementation -----------------/////
		// "settings_menu_item" image
		final ImageView settings_menu_item_image = new ImageView(
				Top_pane.class.getResource("resources/images/settings.png").toString());
		
		settings_menu_item_image.setFitWidth(menu_items_image_size);
		settings_menu_item_image.setPreserveRatio(true);
		
		/* Opens settings window.
		 * Place in menu bar hierarchy: 'Options→this menu item' */
		final MenuItem settings_menu_item = new MenuItem(
				menu_items_resources.getString("settings"),
				settings_menu_item_image);
		
		settings_menu_item.setAccelerator(
				new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
		
		settings_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				// TODO: Launch settings window
			}
		});
		
		// Top-level menu
		final Menu options_menu = new Menu(
				menu_items_resources.getString("options"), null, settings_menu_item);
		/////-/------------------------------------------------------------/////
		
		
		/////-/ Actions menu from menu bar implementation -----------------/////
		// Represents start image
		final Image start_image = new Image(Top_pane.class.getResource(
				"resources/images/start.png").toString());
		// Represents pause image
		final Image pause_image = new Image(Top_pane.class.getResource(
				"resources/images/pause.png").toString());
		// Represents restart image
		final Image restart_image = new Image(Top_pane.class.getResource(
				"resources/images/restart.png").toString());
		// Represents close image
		final Image close_image = new Image(Top_pane.class.getResource(
				"resources/images/close.png").toString());
		
		
		/////-// Stopwatches submenu implementation -----------------------/////
		/////-/// Start all stopwatches menu item implementation ----------/////
		// "start_stopwatches_menu_item" image
		final ImageView start_stopwatches_menu_item_image =
				new ImageView(start_image);
		
		start_stopwatches_menu_item_image.setFitWidth(menu_items_image_size);
		start_stopwatches_menu_item_image.setPreserveRatio(true);

		start_stopwatches_menu_item = new MenuItem(
				menu_items_resources.getString("start_all"),
				start_stopwatches_menu_item_image);
		
		start_stopwatches_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				Time_counters_pane.start_all(Mode.M_stopwatch);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Pause all stopwatches menu item implementation ----------/////
		// "pause_stopwatches_menu_item" image
		final ImageView pause_stopwatches_menu_item_image =
				new ImageView(pause_image);
		
		pause_stopwatches_menu_item_image.setFitWidth(menu_items_image_size);
		pause_stopwatches_menu_item_image.setPreserveRatio(true);
		
		pause_stopwatches_menu_item = new MenuItem(
				menu_items_resources.getString("pause_all"),
				pause_stopwatches_menu_item_image);
		
		pause_stopwatches_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				Time_counters_pane.pause_all(Mode.M_stopwatch);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Restart all stopwatches menu item implementation --------/////
		// "restart_stopwatches_menu_item" image
		final ImageView restart_stopwatches_menu_item_image =
				new ImageView(restart_image);
		
		restart_stopwatches_menu_item_image.setFitWidth(menu_items_image_size);
		restart_stopwatches_menu_item_image.setPreserveRatio(true);
		
		restart_stopwatches_menu_item = new MenuItem(
				menu_items_resources.getString("restart_all"),
				restart_stopwatches_menu_item_image);
		
		restart_stopwatches_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.restart_all(Mode.M_stopwatch);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Close all stopwatches menu item implementation ----------/////
		// "close_stopwatches_menu_item" image
		final ImageView close_stopwatches_menu_item_image =
				new ImageView(close_image);
		
		close_stopwatches_menu_item_image.setFitWidth(menu_items_image_size);
		close_stopwatches_menu_item_image.setPreserveRatio(true);
		
		/* Closes all "Time_counter" objects working in "Mode.M_stopwatch" mode.
		 * Place in menu bar hierarchy: 'Actions→Stopwatches→this menu item' */
		final MenuItem close_stopwatches_menu_item = new MenuItem(
				menu_items_resources.getString("close_all"),
				close_stopwatches_menu_item_image);
		
		close_stopwatches_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.close_all(Mode.M_stopwatch);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		// "stopwatches_submenu" image
		final ImageView stopwatch_image = new ImageView(
				Top_pane.class.getResource("resources/images/stopwatch.png").toString());
		
		stopwatch_image.setFitWidth(menu_items_image_size);
		stopwatch_image.setPreserveRatio(true);
		
		stopwatches_submenu = new Menu(
				menu_items_resources.getString("stopwatches"),
				stopwatch_image,
				start_stopwatches_menu_item, pause_stopwatches_menu_item,
				restart_stopwatches_menu_item, close_stopwatches_menu_item);
		/////-//-----------------------------------------------------------/////
		
		
		/////-// Timers submenu implementation ----------------------------/////
		/////-/// Start all timers menu item implementation ---------------/////
		// "start_timers_menu_item" image
		final ImageView start_timers_menu_item_image = new ImageView(start_image);
		
		start_timers_menu_item_image.setFitWidth(menu_items_image_size);
		start_timers_menu_item_image.setPreserveRatio(true);
		
		start_timers_menu_item = new MenuItem(
				menu_items_resources.getString("start_all"),
				start_timers_menu_item_image);
		
		start_timers_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				Time_counters_pane.start_all(Mode.M_countdown);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Pause all timers menu item implementation ---------------/////
		// "pause_timers_menu_item" image
		final ImageView pause_timers_menu_item_image = new ImageView(pause_image);
		
		pause_timers_menu_item_image.setFitWidth(menu_items_image_size);
		pause_timers_menu_item_image.setPreserveRatio(true);
		
		pause_timers_menu_item = new MenuItem(
				menu_items_resources.getString("pause_all"),
				pause_timers_menu_item_image);
		
		pause_timers_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				Time_counters_pane.pause_all(Mode.M_countdown);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Restart all timers menu item implementation -------------/////
		// "restart_timers_menu_item" image
		final ImageView restart_timers_menu_item_image =
				new ImageView(restart_image);
		
		restart_timers_menu_item_image.setFitWidth(menu_items_image_size);
		restart_timers_menu_item_image.setPreserveRatio(true);
		
		restart_timers_menu_item = new MenuItem(
				menu_items_resources.getString("restart_all"),
				restart_timers_menu_item_image);
		
		restart_timers_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.restart_all(Mode.M_countdown);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Close all timers menu item implementation ---------------/////
		// "close_timers_menu_item" image
		final ImageView close_timers_menu_item_image = new ImageView(close_image);
		
		close_timers_menu_item_image.setFitWidth(menu_items_image_size);
		close_timers_menu_item_image.setPreserveRatio(true);
		
		/* Closes all "Time_counter" objects working in "Mode.M_countdown" mode.
		 * Place in menu bar hierarchy: 'Actions→Timers→this menu item' */
		final MenuItem close_timers_menu_item = new MenuItem(
				menu_items_resources.getString("close_all"),
				close_timers_menu_item_image);
		
		close_timers_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.close_all(Mode.M_countdown);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		// "timers_submenu" image
		final ImageView timer_image = new ImageView(
				Top_pane.class.getResource("resources/images/countdown.png").toString());
		
		timer_image.setFitWidth(menu_items_image_size);
		timer_image.setPreserveRatio(true);
		
		timers_submenu = new Menu(
				menu_items_resources.getString("timers"),
				timer_image,
				start_timers_menu_item, pause_timers_menu_item,
				restart_timers_menu_item, close_timers_menu_item);
		/////-//-----------------------------------------------------------/////
		
		
		/////-// Both (Stopwatches and Timers) submenu implementation -----/////
		/////-/// Start all stopwatches and timers menu item implementation /////
		// "start_stopwatches_timers_menu_item" image
		final ImageView start_stopwatches_timers_menu_item_image =
				new ImageView(start_image);
		
		start_stopwatches_timers_menu_item_image.setFitWidth(
				menu_items_image_size);
		start_stopwatches_timers_menu_item_image.setPreserveRatio(true);
		
		start_stopwatches_timers_menu_item = new MenuItem(
				menu_items_resources.getString("start_all"),
				start_stopwatches_timers_menu_item_image);
		start_stopwatches_timers_menu_item.setAccelerator(
				new KeyCodeCombination(KeyCode.S, KeyCombination.ALT_DOWN));
		
		start_stopwatches_timers_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				Time_counters_pane.start_all(Mode.M_stopwatch, Mode.M_countdown);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Pause all stopwatches and timers menu item implementation /////
		// "pause_stopwatches_timers_menu_item" image
		final ImageView pause_stopwatches_timers_menu_item_image =
				new ImageView(pause_image);
		
		pause_stopwatches_timers_menu_item_image.setFitWidth(
				menu_items_image_size);
		pause_stopwatches_timers_menu_item_image.setPreserveRatio(true);
		
		pause_stopwatches_timers_menu_item = new MenuItem(
				menu_items_resources.getString("pause_all"),
				pause_stopwatches_timers_menu_item_image);
		pause_stopwatches_timers_menu_item.setAccelerator(
				new KeyCodeCombination(KeyCode.P, KeyCombination.ALT_DOWN));
		
		pause_stopwatches_timers_menu_item.setOnAction(
				new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				Time_counters_pane.pause_all(Mode.M_stopwatch, Mode.M_countdown);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Restart all stopwatches and timers menu item implementation /////
		// "restart_stopwatches_timers_menu_item" image
		final ImageView restart_stopwatches_timers_menu_item_image =
				new ImageView(restart_image);
		
		restart_stopwatches_timers_menu_item_image.setFitWidth(
				menu_items_image_size);
		restart_stopwatches_timers_menu_item_image.setPreserveRatio(true);
		
		restart_stopwatches_timers_menu_item = new MenuItem(
				menu_items_resources.getString("restart_all"),
				restart_stopwatches_timers_menu_item_image);
		restart_stopwatches_timers_menu_item.setAccelerator(
				new KeyCodeCombination(KeyCode.R, KeyCombination.ALT_DOWN));
		
		restart_stopwatches_timers_menu_item.setOnAction(
				new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.restart_all(Mode.M_stopwatch, Mode.M_countdown);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		/////-/// Close all stopwatches and timers menu item implementation /////
		// "close_stopwatches_timers_menu_item" image
		final ImageView close_stopwatches_timers_menu_item_image =
				new ImageView(close_image);
		
		close_stopwatches_timers_menu_item_image.setFitWidth(
				menu_items_image_size);
		close_stopwatches_timers_menu_item_image.setPreserveRatio(true);
		
		/* Closes all "Time_counter" objects working in "Mode.M_stopwatch" and
		 * "Mode.M_countdown" modes.
		 * Place in menu bar hierarchy: 'Actions→Both Stopwatches and Timers→
		 * this menu&nbsp;item' */
		final MenuItem close_stopwatches_timers_menu_item = new MenuItem(
				menu_items_resources.getString("close_all"),
				close_stopwatches_timers_menu_item_image);
		
		close_stopwatches_timers_menu_item.setOnAction(
				new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.close_all(Mode.M_stopwatch, Mode.M_countdown);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		stopwatches_timers_submenu = new Menu(
				menu_items_resources.getString("stopwatches_and_timers"),
				null,
				start_stopwatches_timers_menu_item,
				pause_stopwatches_timers_menu_item,
				restart_stopwatches_timers_menu_item,
				close_stopwatches_timers_menu_item);
		/////-//-----------------------------------------------------------/////
		
		
		/////-// Elapsed from submenu implementation ----------------------/////
		/////-/// Close all Elapsed from time counters menu item implementation /////
		// "close_elapsed_from_menu_item" image
		final ImageView close_elapsed_from_menu_item_image =
				new ImageView(close_image);
		
		close_elapsed_from_menu_item_image.setFitWidth(menu_items_image_size);
		close_elapsed_from_menu_item_image.setPreserveRatio(true);
		
		/* Closes all "Time_counter" objects working in "Mode.M_elapsed_from"
		 * mode.
		 * Place in menu bar hierarchy: 'Actions→"Elapsed from" time counters→
		 * this menu item' */
		final MenuItem close_elapsed_from_menu_item = new MenuItem(
				menu_items_resources.getString("close_all"),
				close_elapsed_from_menu_item_image);
		
		close_elapsed_from_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.close_all(Mode.M_elapsed_from);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		// "elapsed_from_submenu" image
		final ImageView elapsed_from_submenu_image = new ImageView(
				Top_pane.class.getResource("resources/images/elapsed_from.png").toString());
		
		elapsed_from_submenu_image.setFitWidth(menu_items_image_size);
		elapsed_from_submenu_image.setPreserveRatio(true);
		
		elapsed_from_submenu = new Menu(
				menu_items_resources.getString("elapsed_from_time_counters"),
				elapsed_from_submenu_image,
				close_elapsed_from_menu_item);
		/////-//-----------------------------------------------------------/////
		
		
		/////-// Remains till submenu implementation ----------------------/////
		/////-/// Close all Remains till time counters menu item implementation /////
		// "close_remains_till_menu_item" image
		final ImageView close_remains_till_menu_item_image =
				new ImageView(close_image);
		
		close_remains_till_menu_item_image.setFitWidth(menu_items_image_size);
		close_remains_till_menu_item_image.setPreserveRatio(true);
		
		/* Closes all "Time_counter" objects working in "Mode.M_remains_till"
		 * mode.
		 * Place in menu bar hierarchy: 'Actions→"Remains till" time counters→
		 * this menu item' */
		final MenuItem close_remains_till_menu_item = new MenuItem(
				menu_items_resources.getString("close_all"),
				close_remains_till_menu_item_image);
		
		close_remains_till_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.close_all(Mode.M_remains_till);
			}
		});
		/////-///----------------------------------------------------------/////
		
		
		// "remains_till_submenu" image
		final ImageView remains_till_submenu_image = new ImageView(
				Top_pane.class.getResource("resources/images/remains_till.png").toString());
		
		remains_till_submenu_image.setFitWidth(menu_items_image_size);
		remains_till_submenu_image.setPreserveRatio(true);
		
		remains_till_submenu = new Menu(
				menu_items_resources.getString("remains_till_time_counters"),
				remains_till_submenu_image,
				close_remains_till_menu_item);
		/////-//-----------------------------------------------------------/////
		
		
		/////-// Both (Elapsed from and Remains till) submenu implementation /////
		/*///-/// Close all Elapsed from and Remains till menu item
		 * implementation ///*/
		// "close_elapsed_from_remains_till_menu_item" image
		final ImageView close_elapsed_from_remains_till_menu_item_image =
				new ImageView(close_image);
		
		close_elapsed_from_remains_till_menu_item_image.setFitWidth(
				menu_items_image_size);
		close_elapsed_from_remains_till_menu_item_image.setPreserveRatio(true);
		
		/* Closes all "Time_counter" objects working in "Mode.M_elapsed_from"
		 * and "Mode.M_remains_till" modes.
		 * Place in menu bar hierarchy: 'Actions→Both "Elapsed from" and
		 * "Remains till" time counters→this menu item' */
		final MenuItem close_elapsed_from_remains_till_menu_item = new MenuItem(
				menu_items_resources.getString("close_all"),
				close_elapsed_from_remains_till_menu_item_image);
		
		close_elapsed_from_remains_till_menu_item.setOnAction(
				new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.close_all(
						Mode.M_elapsed_from, Mode.M_remains_till);
			}
		});
		/*///-///----------------------------------------------------------///*/
		
		
		elapsed_from_remains_till_submenu = new Menu(
				menu_items_resources.getString("elapsed_from_and_remains_till"),
				null,
				close_elapsed_from_remains_till_menu_item);
		/////-//-----------------------------------------------------------/////
		
		
		/////-// Close all menu item implementation -----------------------/////
		// "close_all_time_counters_menu_item" image
		final ImageView close_all_time_counters_menu_item_image =
				new ImageView(close_image);
		
		close_all_time_counters_menu_item_image.setFitWidth(menu_items_image_size);
		close_all_time_counters_menu_item_image.setPreserveRatio(true);
		
		close_all_time_counters_menu_item = new MenuItem(
				menu_items_resources.getString("close_all_time_counters"),
				close_all_time_counters_menu_item_image);

		close_all_time_counters_menu_item.setOnAction(
				new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				Time_counters_pane.close_all(Mode.M_stopwatch, Mode.M_countdown,
						Mode.M_elapsed_from, Mode.M_remains_till);
			}
		});
		/////-//-----------------------------------------------------------/////
		
		
		// Top-level menu
		final Menu actions_menu = new Menu(
				menu_items_resources.getString("actions"),
				null,
				stopwatches_submenu, timers_submenu, stopwatches_timers_submenu,
				new SeparatorMenuItem(), elapsed_from_submenu,
				remains_till_submenu, elapsed_from_remains_till_submenu,
				new SeparatorMenuItem(), close_all_time_counters_menu_item);
		/////-/------------------------------------------------------------/////
		
		
		/////-/ Help menu from menu bar implementation --------------------/////
		/////-// Helpful information menu item implementation -------------/////
		/* Provides help window.
		 * Place in menu bar hierarchy: 'Help→this menu item' */
		final MenuItem helpful_info_menu_item = new MenuItem(
				menu_items_resources.getString("helpful_info"),
				new ImageView(Top_pane.class.getResource(
						"resources/images/info.png").toString()));
		
		helpful_info_menu_item.setAccelerator(new KeyCodeCombination(KeyCode.F1));
		
		helpful_info_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				// TODO: Launch help window
			}
		});
		/////-//-----------------------------------------------------------/////
		
		
		/////-// About menu item implementation ---------------------------/////
		/* Provides about-program window.
		 * Place in menu bar hierarchy: 'Help→this menu item' */
		final MenuItem about_menu_item =
				new MenuItem(menu_items_resources.getString("about"));
		
		about_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(ActionEvent event)
			{
				// TODO: Launch about-program window
			}
		});
		/////-//-----------------------------------------------------------/////
		
		
		// Top-level menu
		final Menu help_menu = new Menu(
				menu_items_resources.getString("help"),
				null,
				helpful_info_menu_item, new SeparatorMenuItem(), about_menu_item);
		/////-/------------------------------------------------------------/////
		
		
		// Menu bar for the main window
		final MenuBar menu_bar =
				new MenuBar(file_menu, options_menu, actions_menu, help_menu);
		/////--------------------------------------------------------------/////
		
		
		///// Tool bar implementation -------------------------------------/////
		new_time_counter_button =
				new Button(null, new ImageView(new_time_counter_image));
		
		// New time counter toolbar button tooltip
		final Tooltip new_time_counter_button_tooltip = new Tooltip(
				hints_resources.getString("new_time_counter_button_tooltip"));
		
		new_time_counter_button.setTooltip(new_time_counter_button_tooltip);
		
		/* TODO: Provide possibility to add additional buttons to the tool bar
		 * from program settings window: implement methods to make an easy node
		 * addition and removing from the "tool_bar" field */	
		// Tool bar for the main window
		final ToolBar tool_bar = new ToolBar(new_time_counter_button);
		/////--------------------------------------------------------------/////
		
		
		top_pane = new VBox(menu_bar, tool_bar);
		
		
		gui_settings.add_Locale_change_listener(new Locale_change_listener()
		{
			@Override
			public void locale_changed()
			{
				hints_resources = gui_settings.get_hints_resources();
				menu_items_resources = gui_settings.get_menu_items_resources();
				messages_resources = gui_settings.get_messages_resources();
				
				file_menu.setText(menu_items_resources.getString("file"));
				new_time_counter_menu_item.setText(
						menu_items_resources.getString("new_time_counter"));
				save_menu_item.setText(menu_items_resources.getString("save"));
				exit_menu_item.setText(menu_items_resources.getString("exit"));
				
				options_menu.setText(menu_items_resources.getString("options"));
				settings_menu_item.setText(
						menu_items_resources.getString("settings"));
				
				actions_menu.setText(menu_items_resources.getString("actions"));
				stopwatches_submenu.setText(
						menu_items_resources.getString("stopwatches"));
				start_stopwatches_menu_item.setText(
						menu_items_resources.getString("start_all"));
				pause_stopwatches_menu_item.setText(
						menu_items_resources.getString("pause_all"));
				restart_stopwatches_menu_item.setText(
						menu_items_resources.getString("restart_all"));
				close_stopwatches_menu_item.setText(
						menu_items_resources.getString("close_all"));
				
				timers_submenu.setText(menu_items_resources.getString("timers"));
				start_timers_menu_item.setText(
						menu_items_resources.getString("start_all"));
				pause_timers_menu_item.setText(
						menu_items_resources.getString("pause_all"));
				restart_timers_menu_item.setText(
						menu_items_resources.getString("restart_all"));
				close_timers_menu_item.setText(
						menu_items_resources.getString("close_all"));
				
				stopwatches_timers_submenu.setText(
						menu_items_resources.getString("stopwatches_and_timers"));
				start_stopwatches_timers_menu_item.setText(
						menu_items_resources.getString("start_all"));
				pause_stopwatches_timers_menu_item.setText(
						menu_items_resources.getString("pause_all"));
				restart_stopwatches_timers_menu_item.setText(
						menu_items_resources.getString("restart_all"));
				close_stopwatches_timers_menu_item.setText(
						menu_items_resources.getString("close_all"));
				
				elapsed_from_submenu.setText(menu_items_resources.getString(
						"elapsed_from_time_counters"));
				close_elapsed_from_menu_item.setText(
						menu_items_resources.getString("close_all"));
				
				remains_till_submenu.setText(menu_items_resources.getString(
						"remains_till_time_counters"));
				close_remains_till_menu_item.setText(
						menu_items_resources.getString("close_all"));
				
				elapsed_from_remains_till_submenu.setText(
						menu_items_resources.getString(
								"elapsed_from_and_remains_till"));
				close_elapsed_from_remains_till_menu_item.setText(
						menu_items_resources.getString("close_all"));
				
				close_all_time_counters_menu_item.setText(
						menu_items_resources.getString("close_all_time_counters"));
				
				help_menu.setText(menu_items_resources.getString("help"));
				helpful_info_menu_item.setText(
						menu_items_resources.getString("helpful_info"));
				about_menu_item.setText(menu_items_resources.getString("about"));
				
				new_time_counter_button_tooltip.setText(
						hints_resources.getString(
								"new_time_counter_button_tooltip"));
			}
		});
	}
	
	
	//**************************************************************************
	//                                                                         *
	// Methods default-access static                                           *
	//                                                                         *
	//**************************************************************************
	/**
	 * Calling this method is critically&nbsp;needed <u>before providing
	 * menu&nbsp;bar to user</u>. Otherwise next menu&nbsp;items will&nbsp;throw
	 * {@link NullPointerException} when using them:
	 * <ul><li><i>File&rArr;Exit</i>;</li>
	 * <li><i>File&rArr;New time&nbsp;counter</i>.</li></ul>
	 * 
	 * @param primary_stage Program's primary stage.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	static void apply_primary_stage(final Stage primary_stage)
	{
		try
		{
			new_time_counter_window = new New_time_counter_window(primary_stage);
		}
		catch (final IOException exc)
		{
			logger.log(
					Level.SEVERE,
					"Cannot create " + New_time_counter_window.class.getName()
							+ " instance. Exception stack trace:",
					exc);
		}
		
		exit_menu_item.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				Time_counters_pane.sort_and_save_time_counters();
				time_counters.clear();
				primary_stage.close();
			}
		});
		
		new_time_counter_button.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* If failed to create "New_time_counter_window" instance
				 * previously */
				if (new_time_counter_window == null)
				{
					try
					{
						new_time_counter_window =
								new New_time_counter_window(primary_stage);
					}
					catch (final IOException exc)
					{
						logger.log(
								Level.SEVERE,
								"Cannot create "
										+ New_time_counter_window.class.getName()
										+ " instance. Exception stack trace:",
								exc);
						Error_dialog.show_message(
								Template_message.TM_layout_build);
						
						return;
					}
				}
				
				new_time_counter_window.show_window();
			}
		});
	}
	
	
	/**
	 * @return Pane containing {@link MenuBar} and {@link ToolBar}.
	 */
	static VBox get_top_pane()
	{
		return top_pane;
	}
	
	
	/**
	 * Sets <i>File&rArr;Save time&nbsp;counters</i> menu&nbsp;item <i>disable
	 * property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_save_menu_item(final boolean value)
	{
		save_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Stopwatches</i> submenu <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_stopwatches_submenu(final boolean value)
	{
		stopwatches_submenu.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Stopwatches&rArr;Start/Resume all</i> menu&nbsp;item
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_start_stopwatches_menu_item(final boolean value)
	{
		start_stopwatches_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Stopwatches&rArr;Pause&nbsp;all</i> menu&nbsp;item
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_pause_stopwatches_menu_item(final boolean value)
	{
		pause_stopwatches_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Stopwatches&rArr;Restart&nbsp;all</i> menu&nbsp;item
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_restart_stopwatches_menu_item(final boolean value)
	{
		restart_stopwatches_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Timers</i> submenu <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_timers_submenu(final boolean value)
	{
		timers_submenu.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Timers&rArr;Start/Resume&nbsp;all</i> menu&nbsp;item
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_start_timers_menu_item(final boolean value)
	{
		start_timers_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Timers&rArr;Pause&nbsp;all</i> menu&nbsp;item
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_pause_timers_menu_item(final boolean value)
	{
		pause_timers_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Timers&rArr;Restart&nbsp;all</i> menu&nbsp;item
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_restart_timers_menu_item(final boolean value)
	{
		restart_timers_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Both Stopwatches and Timers&rArr;</i> submenu
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_stopwatches_timers_submenu(final boolean value)
	{
		stopwatches_timers_submenu.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Both Stopwatches and
	 * Timers&rArr;Start/Resume&nbsp;all</i> menu&nbsp;item <i>disable
	 * property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_start_stopwatches_timers_menu_item(
			final boolean value)
	{
		start_stopwatches_timers_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Both Stopwatches and Timers&rArr;Pause&nbsp;all</i>
	 * menu&nbsp;item <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_pause_stopwatches_timers_menu_item(
			final boolean value)
	{
		pause_stopwatches_timers_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Both Stopwatches and Timers&rArr;Restart&nbsp;all</i>
	 * menu&nbsp;item <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_restart_stopwatches_timers_menu_item(
			final boolean value)
	{
		restart_stopwatches_timers_menu_item.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;"Elapsed&nbsp;from" time&nbsp;counters</i> submenu
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_elapsed_from_submenu(final boolean value)
	{
		elapsed_from_submenu.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;"Remains&nbsp;till" time&nbsp;counters</i> submenu
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_remains_till_submenu(final boolean value)
	{
		remains_till_submenu.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Both "Elapsed&nbsp;from" and "Remains&nbsp;till"
	 * time&nbsp;counters</i> submenu <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_elapsed_from_remains_till_submenu(
			final boolean value)
	{
		elapsed_from_remains_till_submenu.setDisable(value);
	}
	
	
	/**
	 * Sets <i>Actions&rArr;Close all time&nbsp;counters</i> menu&nbsp;item
	 * <i>disable property</i> value.
	 * 
	 * @param value {@code true} &#0151; sets menu&nbsp;item to disabled state;
	 * {@code false}&nbsp;&#0151; menu&nbsp;item becomes enabled.
	 */
	static void set_disable_close_all_time_counters_menu_item(
			final boolean value)
	{
		close_all_time_counters_menu_item.setDisable(value);
	}
}
