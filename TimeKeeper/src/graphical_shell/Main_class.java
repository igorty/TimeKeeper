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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import graphical_shell.dialog.Error_dialog;
import graphical_shell.dialog.Error_dialog.Error_type;
import graphical_shell.dialog.Notification_dialog;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import time_obj.Time_counter_control;
import time_obj.dialog.User_notification_dialog;
import time_obj.dialog.User_notification_type;
import time_obj.events.User_notification_event;
import time_obj.events.User_notification_listener;


/**
 * Program initial class.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Main_class extends Application
{
	//**************************************************************************
	//                                                                         *
	// Fields default-access static                                            *
	//                                                                         *
	//**************************************************************************
	/** Filters {@link Window}s input events with&nbsp;a&nbsp;view to turn off
	 * alarm sound after any button typed. */
	static final EventHandler<InputEvent> window_input_filter;
	
	
	//**************************************************************************
	//                                                                         *
	// Fields private static                                                   *
	//                                                                         *
	//**************************************************************************
	/** Logs this class's events. */
	private static final Logger logger;
	
	
	static
	{
		logger = Logger.getLogger(Main_class.class.getName());
		
		
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler()
		{
			@Override
			public void uncaughtException(final Thread t, final Throwable e)
			{
				logger.log(Level.SEVERE,
						"Unhandled exception. Exception stack trace:", e);
			}
		});
		
		
		try
		{
			LogManager.getLogManager().readConfiguration(
					Main_class.class.getResourceAsStream(
							"resources/logging/logging.properties"));
		}
		catch (final SecurityException | IOException exc)
		{
			logger.log(
					Level.SEVERE,
					"Cannot access logging configuration file. Exception stack"
							+ " trace:",
					exc);
		}
		
		
		window_input_filter = new EventHandler<InputEvent>()
		{
			@Override
			public void handle(final InputEvent event)
			{
				/* TODO: Send turn off alarm sound command after implementing
				 * class responsible for sound notification */
			}
		};
	}
	
	
	//**************************************************************************
	//                                                                         *
	// Methods public static                                                   *
	//                                                                         *
	//**************************************************************************
	/**
	 * Program entry point
	 * 
	 * @param args Not used.
	 */
	public static void main(final String[] args)
	{
		launch((String[])null);
	}
	
	
	//**************************************************************************
	//                                                                         *
	// Methods public of-instance                                              *
	//                                                                         *
	//**************************************************************************
	@Override
	public void start(final Stage primary_stage)
	{
		User_notification_dialog.set_User_notification_listener(
				new User_notification_listener()
		{
			@Override
			public void user_notification_occurred(
					final User_notification_event event,
					final User_notification_type user_notification_type,
					final String message)
			{
				switch (user_notification_type)
				{
				case UNT_IO_error:
					Error_dialog.show_message(Error_type.ET_IO, message);
					
					break;
					
				case UNT_file_error:
					Error_dialog.show_message(
							Error_type.ET_file_content_error, message);
					
					break;
					
				case UNT_time_zone_error:
					Error_dialog.show_message(
							Error_type.ET_time_zone_error, message);
					
					break;
					
				case UNT_informing:
					Notification_dialog.show_message(message);
					
					break;
				
				default:
					throw new EnumConstantNotPresentException(
							User_notification_type.class,
							user_notification_type.name());
				}
			}
		});
		
		
		primary_stage.addEventFilter(KeyEvent.ANY, window_input_filter);
		primary_stage.addEventFilter(MouseEvent.MOUSE_CLICKED, window_input_filter);
		
		
		// Pane representing time counters
		final TilePane time_counters_pane =
				Time_counters_pane.get_time_counters_pane();
		// Main window's root pane
		final BorderPane root_pane =
				new BorderPane(new ScrollPane(time_counters_pane));
		
		
		root_pane.widthProperty().addListener(new ChangeListener<Number>()
		{

			@Override
			public void changed(
					final ObservableValue<? extends Number> observable,
					final Number oldValue, final Number newValue)
			{
				time_counters_pane.setPrefTileWidth((double)newValue);
			}
		});
		
		
		Top_pane.apply_primary_stage(primary_stage);
		root_pane.setTop(Top_pane.get_top_pane());
		
		final Scene scene = new Scene(root_pane, 600, 500);
		
		Time_counters_pane.apply_scene(scene);
		primary_stage.setScene(scene);
		
		
		primary_stage.setOnCloseRequest(new EventHandler<WindowEvent>()
		{
			@Override
			public void handle(final WindowEvent event)
			{
				Time_counters_pane.sort_and_save_time_counters();
				Time_counter_control.get_instance().get_time_counters().clear();
			}
		});
		
		
		primary_stage.show();
	}
}
