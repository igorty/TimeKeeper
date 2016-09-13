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

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import app.tar.graphical_shell.events.Locale_change_listener;
import app.tar.time_obj.Instance_counter;
import app.tar.time_obj.Mode;
import app.tar.time_obj.Settings;
import app.tar.time_obj.Solo_counter;
import app.tar.time_obj.Time_counter;
import app.tar.time_obj.Time_counter_control;
import app.tar.time_obj.Settings.Locale_setting;
import app.tar.time_obj.Solo_counter.Initial_time_values;
import app.tar.time_obj.Time_counter.Time_counter_text_value;
import app.tar.time_obj.containers.Modified_ArrayList;
import app.tar.time_obj.events.Numeric_overflow_event;
import app.tar.time_obj.events.Numeric_overflow_listener;
import app.tar.time_obj.events.Time_counter_event;
import app.tar.time_obj.events.Time_counter_text_listener;
import app.tar.time_obj.events.Time_elapsed_listener;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.paint.Color;


/**
 * <p>Manages <i>time&nbsp;counters</i>.
 * Each time&nbsp;counter is stored in {@link HBox}, and all these {@code HBoxes}
 * are contained in a&nbsp;vertical {@link TilePane}. Each time&nbsp;counter is
 * provided with control elements ({@link TextField} to write
 * time&nbsp;counter's description and {@link Button}{@code s} to operate
 * the&nbsp;time&nbsp;counter).
 * <p>Time counters can be dragged with the&nbsp;aim to:
 * <ul><li>reorder them in user&#8209;convenient way;</li>
 * <li>copy time&nbsp;counter value to any text&nbsp;field.</li></ul>
 * <p><b>Important!</b> It is strictly necessary to perform operations on
 * {@link Time_counter}{@code s} using methods provided by this class (if
 * specified operation can be performed with implemented in this class methods).
 * Otherwise GUI&nbsp;will behave incorrectly.
 * <p><i>Note.</i> To provide full class's functionality
 * {@link #apply_scene(Scene)} need to be called.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
class Time_counters_pane
{
	///// Nested classes private ==========================================/////
	/**
	 * Represents cursor coordinates relatively&nbsp;to time&nbsp;counter's
	 * enclosing {@link HBox} container when dragging it.
	 * 
	 * @version 1.0
	 * @author Igor Taranenko
	 */
	private static class Drag_delta
	{
		/**
		 * Cursor X&nbsp;coordinate.
		 */
		double x_coord;
		/**
		 * Cursor Y&nbsp;coordinate.
		 */
		double y_coord;
	}
	
	
	///// Fields private static ===========================================/////
	/** Logs this class's events. */
	private static final Logger logger;
	
	/** Graphic user interface settings. */
	private static final GUI_settings gui_settings;
	/** Program settings. */
	private static final Settings program_settings;
	
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * text&nbsp;fields texts. */
	private static ResourceBundle text_fields_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * hints and tooltips texts. */
	private static ResourceBundle hints_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * menu&nbsp;items names. */
	private static ResourceBundle menu_items_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * specific time&nbsp;counter settings names. */
	private static ResourceBundle time_counter_resources;
	/** Resource bundle representing <i>.properties</i> which contains
	 * time&nbsp;unit names. */
	private static ResourceBundle time_unit_names_resources;
	
	/** Contains {@link HBox}{@code es}. Each {@code HBox} has single
	 * <i>time&nbsp;counter</i> with related controls. */
	private static final TilePane time_counters_pane;
	
	/** {@link #time_counters_pane} children nodes. */
	private static final ObservableList<Node> time_counters_pane_children;
	
	/** Contains each {@link Mode}{@code 's} {@link Time_counter} objects
	 * quantity.
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static final EnumMap<Mode, Integer> time_counters_quantity;
	/** {@link Time_counter} objects quantity which are working in
	 * {@link Mode#M_stopwatch} mode and currently running.
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static int running_stopwatches;
	/** {@link Time_counter} objects quantity which are working in
	 * {@link Mode#M_stopwatch} mode and currently paused.
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static int paused_stopwatches;
	/** {@link Time_counter} objects quantity which are working in
	 * {@link Mode#M_stopwatch} mode, has&nbsp;been&nbsp;restarted and
	 * are&nbsp;retaining their initial state, or even has&nbsp;not&nbsp;been
	 * started yet (i.e.&nbsp;{@link Solo_counter#counting_had_started()}
	 * returns false for these time&nbsp;counters).
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static int restarted_stopwatches;
	/** {@link Time_counter} objects quantity which are working in
	 * {@link Mode#M_stopwatch} mode and have overflowed state
	 * (i.e.&nbsp;{@link Solo_counter#numeric_overflow_status()} returns
	 * {@code true}).
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static int overflowed_stopwatches;
	/** {@link Time_counter} objects quantity which are working in
	 * {@link Mode#M_countdown} mode and currently running.
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static int running_timers;
	/** {@link Time_counter} objects quantity which are working in
	 * {@link Mode#M_countdown} mode and currently paused.
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static int paused_timers;
	/** {@link Time_counter} objects quantity which are working in
	 * {@link Mode#M_countdown} mode, has&nbsp;been&nbsp;restarted and
	 * are&nbsp;retaining their initial state, or even has&nbsp;not&nbsp;been
	 * started yet (i.e.&nbsp;{@link Solo_counter#counting_had_started()}
	 * returns false for these time&nbsp;counters).
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static int restarted_timers;
	/** {@link Time_counter} objects quantity which are working in
	 * {@link Mode#M_countdown} mode and have overflowed state
	 * (i.e.&nbsp;{@link Solo_counter#numeric_overflow_status()} returns
	 * {@code true}).
	 * <p>Implemented to disable/enable menu&nbsp;items from <i>Actions</i>
	 * submenu in menu&nbsp;bar. */
	private static int overflowed_timers;
	
	/** Synchronizes access to {@link #time_counters_pane_children} container. */
	private static final ReentrantLock time_counters_pane_children_lock;
	/** Synchronizes access to fields that control {@link Time_counter} objects
	 * types and states quantities with&nbsp;a&nbsp;view to manage
	 * main&nbsp;window's menu&nbsp;bar.
	 * <p>Synchronized fields list:
	 * <ul><li>{@link #time_counters_quantity};</li>
	 * <li>{@link #running_stopwatches};</li>
	 * <li>{@link #paused_stopwatches};</li>
	 * <li>{@link #restarted_stopwatches};</li>
	 * <li>{@link #overflowed_stopwatches};</li>
	 * <li>{@link #running_timers};</li>
	 * <li>{@link #paused_timers};</li>
	 * <li>{@link #restarted_timers};</li>
	 * <li>{@link #overflowed_timers}.</li></ul> */
	private static final ReentrantLock time_counters_quantity_lock;
	
	/** Images, which represent time&nbsp;counters modes.<br>
	 * <b>Warning!</b> The&nbsp;container <u>is&nbsp;immutable</u>.
	 * An&nbsp;attempt to change its content results in runtime&nbsp;exception. */
	private static final Map<Mode, Image> mode_images;
	
	/** <i>Start/Resume/Pause</i> {@link Button} image representing <i>start</i>
	 * function. */
	private static final Image start_image;
	/** <i>Start/Resume/Pause</i> {@link Button} image representing <i>resume</i>
	 * function. */
	private static final Image resume_image;
	/** <i>Start/Resume/Pause</i> {@link Button} image representing <i>pause</i>
	 * function. */
	private static final Image pause_image;
	/** <i>Restart</i> {@link Button} image. */
	private static final Image restart_image;
	/** <i>Close</i> {@link Button} image. */
	private static final Image close_image;
	/** <i>Time counter settings</i> {@link Button} image. */
	private static final Image setting_image;
	
	/** Time counters images size. */
	private static final double images_size;
	
	/** Time counter value color by default when time&nbsp;counter
	 * <u>is&nbsp;running</u> ({@link Solo_counter} objects may be in paused
	 * state). */
	private static final Color default_color;
	/** Time counter value color when time <u>has&nbsp;elapsed</u> or
	 * <u>numeric&nbsp;overflow occurred</u> (for {@link Solo_counter} objects
	 * only), and time&nbsp;counter is running (for {@link Solo_counter} objects
	 * only). */
	private static final Color time_elapsed_color;
	/** <i>Used by {@link Solo_counter} objects only.</i> Time&nbsp;counter
	 * value color when time&nbsp;counter is <u>in paused state</u> and time
	 * <u>has&nbsp;not elapsed</u> yet. */
	private static final Color default_disabled_color;
	/** <i>Used by {@link Solo_counter} objects only.</i> Time&nbsp;counter
	 * value color when time&nbsp;counter is <u>in paused state</u> and time
	 * <u>has&nbsp;elapsed already</u>. */
	private static final Color time_elapsed_disabled_color;
	/** {@link HBox} pane (which encloses single time&nbsp;counter controls)
	 * background&nbsp;color when user attention needed for
	 * the&nbsp;time&nbsp;counter. */
	private static final Background notify_background;
	/** {@link HBox} pane (which encloses single time&nbsp;counter controls)
	 * background&nbsp;color by default. */
	private static Background default_background;
	
	/** {@link HBox} pane (which encloses single time&nbsp;counter controls)
	 * identifier. */
	private static final String time_counter_unit_id;
	/** {@link Node} identifier for <i>Start/Resume/Pause</i> {@link Button}
	 * contained in {@link HBox}{@code es} representing {@link Solo_counter}
	 * time&nbsp;counters. */
	private static final String start_button_id;
	/** {@link Node} identifier for <i>Restart</i> {@link Button} contained in
	 * {@link HBox}{@code es} representing {@link Solo_counter}
	 * time&nbsp;counters. */
	private static final String restart_button_id;
	/** {@link Node} identifier for <i>Close</i> {@link Button} contained in
	 * {@link HBox}{@code es} representing {@link Time_counter} object. */
	private static final String close_button_id;
	
	/** Common listener for all {@link Time_counter}{@code s} represented in
	 * {@link #time_counters_pane_children}. */
	private static final Time_elapsed_listener time_elapsed_listener;
	/** Common listener for all {@link Solo_counter}{@code s} represented in
	 * {@link #time_counters_pane_children}. */
	private static final Numeric_overflow_listener numeric_overflow_listener;
	
	
	static
	{
		logger = Logger.getLogger(Time_counters_pane.class.getName());
		gui_settings = GUI_settings.get_instance();
		program_settings = Settings.get_instance();
		
		text_fields_resources = gui_settings.get_text_fields_resources();
		hints_resources = gui_settings.get_hints_resources();
		menu_items_resources = gui_settings.get_menu_items_resources();
		time_counter_resources = gui_settings.get_time_counter_resources();
		time_unit_names_resources = program_settings.get_time_counter_resources();
		
		time_counters_pane = new TilePane(Orientation.VERTICAL);
		time_counters_pane.setPrefRows(0);
		time_counters_pane_children_lock = new ReentrantLock();
		time_counters_quantity_lock = new ReentrantLock();
		time_counters_pane_children = time_counters_pane.getChildren();
		time_counters_quantity = new EnumMap<>(Mode.class);
		set_time_counters_quantity_to_0();
		disable_all_actions_menu_items();
		
		///// "mode_images" container initialization /////
		// "mode_images" container images
		final Image[] mode_images_values = {
				new Image(Time_counters_pane.class.getResource(
						"images/stopwatch.png").toString()),
				new Image(Time_counters_pane.class.getResource(
						"images/countdown.png").toString()),
				new Image(Time_counters_pane.class.getResource(
						"images/elapsed_from.png").toString()),
				new Image(Time_counters_pane.class.getResource(
						"images/remains_till.png").toString()) };
		// All "Mode" enumeration constants
		final Mode[] mode_values = Mode.values();
		
		assert mode_images_values.length == mode_values.length :
			"Array size doesn\'t match with " + Mode.class.getName()
				+ " enumeration constants quantity";
		
		// "mode_images" container initializer
		final Map<Mode, Image> mode_images_init =
				new EnumMap<>(Mode.class);
		
		// "mode_images_init" container initialization
		for (final Mode i : mode_values)
		{
			mode_images_init.put(i, mode_images_values[i.ordinal()]);
		}
		
		mode_images = Collections.unmodifiableMap(mode_images_init);
		//-------------------------------------------/////
		
		
		start_image = new Image(Time_counters_pane.class.getResource(
				"images/start.png").toString());
		resume_image = new Image(Time_counters_pane.class.getResource(
				"images/resume.png").toString());
		pause_image = new Image(Time_counters_pane.class.getResource(
				"images/pause.png").toString());
		restart_image = new Image(Time_counters_pane.class.getResource(
				"images/restart.png").toString());
		close_image = new Image(Time_counters_pane.class.getResource(
				"images/close.png").toString());
		setting_image = new Image(Time_counters_pane.class.getResource(
				"images/settings.png").toString());
		
		images_size = 32;
		
		default_color = Color.BLACK;
		time_elapsed_color = Color.RED;
		default_disabled_color = Color.GRAY;
		time_elapsed_disabled_color = Color.PALEVIOLETRED;
		notify_background =
				new Background(new BackgroundFill(Color.YELLOW, null, null));
		
		time_counter_unit_id = "time_counter";
		start_button_id = "start_button";
		restart_button_id = "restart_button";
		close_button_id = "close_button";
		
		time_elapsed_listener = new Time_elapsed_listener()
		{
			@Override
			public void time_counter_reached_zero(
					final Time_counter_event<?> event)
			{
				// The object which has generated event
				final Time_counter event_source =
						(Time_counter)event.getSource();
				
				try
				{
					time_counters_pane_children_lock.lockInterruptibly();
				}
				catch (final InterruptedException exc)
				{
					logger.log(Level.INFO,
							"Thread interrupts. Exception stack trace:", exc);
					Thread.currentThread().interrupt();
				}
				
				try
				{
					/* Search for HBox pane which represents time counter event
					 * object */
					for (final Node i : time_counters_pane_children)
					{
						// Time counter represented by this HBox pane
						final Time_counter represented =
								(Time_counter)i.getUserData();
						
						// If time counter representative is found
						if (represented.equals(event_source))
						{
							/* Search for label field to set specified color
							 * for its text */
							for (final Node j : ((HBox)i).getChildren())
							{
								if (j instanceof Label)
								{
									/* In case of counting time elapsed from
									 * specified instance time elapsed event is
									 * received when time value becomes POSITIVE.
									 * In this case time value text must be set
									 * to default color (not to the notification
									 * color) */
									if (represented.instance_mode.equals(
											Mode.M_elapsed_from))
									{
										Platform.runLater(new Runnable()
										{
											@Override
											public void run()
											{
												((Label)j).setTextFill(
														default_color);
											}
										});
									}
									else
									{
										Platform.runLater(new Runnable()
										{
											@Override
											public void run()
											{
												((Label)j).setTextFill(
														time_elapsed_color);
											}
										});
									}
									
									break;
								}
							}
							
							Platform.runLater(new Runnable()
							{
								@Override
								public void run()
								{
									((HBox)i).setBackground(notify_background);
								}
							});
							
							break;
						}
					}
				}
				finally
				{
					time_counters_pane_children_lock.unlock();
				}
				
				// TODO: Invoke alarm sound
			}
		};
		
		numeric_overflow_listener = new Numeric_overflow_listener()
		{
			@Override
			public void numeric_overflow_occurred(
					final Numeric_overflow_event event)
			{
				// The object which has generated event
				final Solo_counter event_source =
						(Solo_counter)event.getSource();
				
				try
				{
					time_counters_pane_children_lock.lockInterruptibly();
				}
				catch (final InterruptedException exc)
				{
					logger.log(Level.INFO,
							"Thread interrupts. Exception stack trace:", exc);
					Thread.currentThread().interrupt();
				}
				
				try
				{
					/* Search for HBox pane which represents time counter
					 * event object */
					for (final Node i : time_counters_pane_children)
					{
						// Time counter represented by this HBox pane
						final Time_counter represented =
								(Time_counter)i.getUserData();
						
						// If time counter representative is found
						if (represented.equals(event_source))
						{
							// Time counter representative children nodes
							final ObservableList<Node> representative_controls =
									((HBox)i).getChildren();
							/* There are 2 nodes need to be processed in
							 * the next loop, so 2 is one of loop's
							 * conditions */
							int actions_performed_counter = 0;
							
							/* Perform necessary actions over representative
							 * children nodes */
							for (int j = 0, end = representative_controls.size();
									j < end && actions_performed_counter < 2;
									++j)
							{
								final Node control =
										representative_controls.get(j);
								
								// Perform actions over Start/Resume/Pause button
								if (start_button_id.equals(control.getId()))
								{
									Platform.runLater(new Runnable()
									{
										@Override
										public void run()
										{
											control.setDisable(true);
											((Button)control).setGraphic(
													new ImageView(start_image));
										}
									});
									
									// If event object works in stopwatch mode
									if (event_source.instance_mode.equals(
											Mode.M_stopwatch))
									{
										///// Menu bar managing section ---/////
										time_counters_quantity_lock.lock();
										
										try
										{
											/* If the only running stopwatch
											 * has been paused */
											if (--running_stopwatches == 0)
											{
												Top_pane.set_disable_pause_stopwatches_menu_item(true);
												
												/* If there is no running
												 * stopwatches and timers to be
												 * paused after pausing the last
												 * stopwatch */
												if (running_timers == 0)
												{
													Top_pane.set_disable_pause_stopwatches_timers_menu_item(true);
												}
											}
											
											/* If paused stopwatch is
											 * the only one that can be restarted */
											if (running_stopwatches +
													(++paused_stopwatches) -
													restarted_stopwatches == 1)
											{
												Top_pane.set_disable_restart_stopwatches_menu_item(false);
												
												/* If paused stopwatch is
												 * the only one among all
												 * existing stopwatches and
												 * timers that can be restarted */
												if (running_timers +
														paused_timers -
														restarted_timers == 0)
												{
													Top_pane.set_disable_restart_stopwatches_timers_menu_item(false);
												}
											}
											
											++overflowed_stopwatches;
											
											assert running_stopwatches >= 0 :
												"Incorrect time counters quantity value";
											assert paused_stopwatches >= restarted_stopwatches :
												"Incorrect time counters quantity value";
											assert paused_stopwatches >= overflowed_stopwatches :
												"Incorrect time counters quantity value";
											assert paused_stopwatches + running_stopwatches ==
												time_counters_quantity.get(Mode.M_stopwatch) :
													"Incorrect time counters quantity value";
										}
										finally
										{
											time_counters_quantity_lock.unlock();
										}
										/////------------------------------/////
									}
									/* Event object works in timer mode
									 * ("Mode.M_countdown") */
									else
									{
										///// Menu bar managing section ---/////
										time_counters_quantity_lock.lock();
										
										try
										{
											/* If there are no running timers
											 * remaining after this one has been
											 * paused */
											if (--running_timers == 0)
											{
												Top_pane.set_disable_pause_timers_menu_item(true);
												
												/* If there are no running
												 * stopwatches and timers
												 * remaining after this one
												 * has been paused */
												if (running_stopwatches == 0)
												{
													Top_pane.set_disable_pause_stopwatches_timers_menu_item(true);
												}
											}
											
											/* If paused timer is the only one
											 * that can be restarted */
											if (running_timers + (++paused_timers) -
													restarted_timers == 1)
											{
												Top_pane.set_disable_restart_timers_menu_item(false);
												
												/* If paused timer is
												 * the only one among all
												 * existing stopwatches and
												 * timers that can be restarted */
												if (running_stopwatches +
														paused_stopwatches -
														restarted_stopwatches == 0)
												{
													Top_pane.set_disable_restart_stopwatches_timers_menu_item(false);
												}
											}
											
											++overflowed_timers;
											
											assert running_timers >= 0 :
												"Incorrect time counters quantity value";
											assert paused_timers >= restarted_timers :
												"Incorrect time counters quantity value";
											assert paused_timers >= overflowed_timers :
												"Incorrect time counters quantity value";
											assert paused_timers + running_timers ==
													time_counters_quantity.get(Mode.M_countdown) :
														"Incorrect time counters quantity value";
										}
										finally
										{
											time_counters_quantity_lock.unlock();
										}
										/////------------------------------/////
									}
									
									++actions_performed_counter;
								}
								/* Perform actions over label representing
								 * time counter value */
								else if (control instanceof Label)
								{
									Platform.runLater(new Runnable()
									{
										@Override
										public void run()
										{
											((Label)control).setTextFill(
													time_elapsed_color);
										}
									});
									
									++actions_performed_counter;
								}
							}
							
							Platform.runLater(new Runnable()
							{
								@Override
								public void run()
								{
									((HBox)i).setBackground(notify_background);
								}
							});
							
							break;
						}
					}
				}
				finally
				{
					time_counters_pane_children_lock.unlock();
				}
				
				// TODO: Invoke alarm sound
			}
		};
		
		final Time_counter_control time_counter_control =
				Time_counter_control.get_instance();
		
		time_counter_control.read_time_counters_from_file();
		
		gui_settings.add_Locale_change_listener(new Locale_change_listener()
		{
			@Override
			public void locale_changed()
			{
				try
				{
					time_counters_pane_children_lock.lockInterruptibly();
				}
				catch (final InterruptedException exc)
				{
					logger.log(Level.INFO,
							"Thread interrupts. Exception stack trace:", exc);
					Thread.currentThread().interrupt();
				}
				
				// Time counters contained in "Time_counter_control" singleton 
				final Modified_ArrayList time_counters;
				
				try
				{
					text_fields_resources =
							gui_settings.get_text_fields_resources();
					hints_resources = gui_settings.get_hints_resources();
					menu_items_resources = gui_settings.get_menu_items_resources();
					time_counter_resources =
							gui_settings.get_time_counter_resources();
					time_unit_names_resources =
							program_settings.get_time_counter_resources();
					
					/* Assign index to each "Time_counter" represented by HBox
					 * panes with a view to sort them */
					for (int i = 0, end = time_counters_pane_children.size();
							i < end; ++i)
					{
						((Time_counter)time_counters_pane_children.get(i).
								getUserData()).index_number = i;
					}
					
					time_counters = time_counter_control.get_time_counters();
					
					time_counters.sort(new Comparator<Time_counter>()
					{
						@Override
						public int compare(
								final Time_counter o1, final Time_counter o2)
						{
							return Integer.compare(
									o1.index_number, o2.index_number);
						}
					});
					
					Platform.runLater(new Runnable()
					{
						@Override
						public void run()
						{
							time_counters_pane_children.clear();
						}
					});
					
					
					///// Menu bar managing section -----------------------/////
					time_counters_quantity_lock.lock();
					
					try
					{
						set_time_counters_quantity_to_0();
					}
					finally
					{
						time_counters_quantity_lock.unlock();
					}
					/////--------------------------------------------------/////
				}
				finally
				{
					time_counters_pane_children_lock.unlock();
				}
				
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						/* Provide controls to all time counters again after
						 * clearing "time_counters_pane_children" */
						for (final Time_counter i : time_counters)
						{
							add_time_counter_to_pane(i);
						}
					}
				});
			}
		});
		
		// Time counters saved previously
		final Modified_ArrayList time_counters =
				time_counter_control.get_time_counters();
		
		/* Create controls for each time counter and place them into necessary
		 * nodes */
		for (final Time_counter i : time_counters)
		{
			add_time_counter_to_pane(i);
		}
	}
	
	
	///// Methods default-access static ===================================/////
	/**
	 * Provides controls for the&nbsp;given {@code time_counter} argument and
	 * places them into {@link TilePane} (can be obtained by calling
	 * {@link #get_time_counters_pane()} method).<br>
	 * <i>Performance note.</i> Contains synchronized sections.
	 * 
	 * @param time_counter Time&nbsp;counter to place into mentioned
	 * {@link TilePane}.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	static void add_time_counter_to_pane(final Time_counter time_counter)
	{
		// Represents time counter value
		final Label time_counter_value = new Label();
		
		///// Time counter enclosing container implementation /////
		// Encloses single time counter controls
		final HBox time_counter_unit = new HBox(5);
		
		time_counter_unit.setUserData(time_counter);
		time_counter_unit.setAlignment(Pos.CENTER);
		time_counter_unit.setPadding(new Insets(5));
		time_counter_unit.setId(time_counter_unit_id);
		
		/* Cursor when hovering over "time_counter_unit" to show user that
		 * it can be dragged */
		final Cursor time_counter_unit_cursor = Cursor.OPEN_HAND;
		
		time_counter_unit.setOnMouseEntered(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(final MouseEvent event)
			{
				time_counter_unit.getScene().setCursor(time_counter_unit_cursor);
			}
		});
		
		time_counter_unit.setOnMouseExited(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(final MouseEvent event)
			{
				time_counter_unit.getScene().setCursor(Cursor.DEFAULT);
			}
		});
		
		
		// If 'time_counter_unit' default background hasn't been initialized yet
		if (default_background == null)
		{
			default_background = time_counter_unit.getBackground();
		}
		
		
		time_counter_unit.addEventFilter(MouseEvent.MOUSE_CLICKED,
				new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(final MouseEvent event)
			{
				/* Restore default node background in case of it was changed to
				 * notify user and turn off alarm sound */
				time_counter_unit.setBackground(default_background);
				// TODO: Turn off alarm sound
			}
		});
		
		/* Cursor coordinates relatively to "time_counter_unit" node when
		 * dragging it */
		final Drag_delta drag_delta = new Drag_delta();
		
		time_counter_unit.setOnMousePressed(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(final MouseEvent event)
			{
				time_counter_unit.getScene().setCursor(Cursor.CLOSED_HAND);
				drag_delta.x_coord = event.getX();
				drag_delta.y_coord = event.getY();
			}
		});
		
		time_counter_unit.setOnMouseReleased(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(MouseEvent event)
			{
				time_counter_unit.getScene().setCursor(time_counter_unit_cursor);
			}
		});
		
		time_counter_unit.setOnDragDetected(new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(final MouseEvent event)
			{
				// Place "time_counter_unit" pane to drag-and-drop board
				final Dragboard dragboard =
						time_counter_unit.startDragAndDrop(TransferMode.COPY_OR_MOVE);
				/* Drag-and-drop board content to move with
				 * "time_counter_unit" pane */
				final ClipboardContent clipboard_content = new ClipboardContent();
				
				clipboard_content.putString(time_counter_value.getText());
				dragboard.setContent(clipboard_content);
				
				// "time_counter_unit" pane snapshot
				final WritableImage snapshot =
						time_counter_unit.snapshot(null, null);
				
				dragboard.setDragView(
						snapshot, drag_delta.x_coord, drag_delta.y_coord);
			}
		});
		
		time_counter_unit.setOnDragOver(new EventHandler<DragEvent>()
		{
			@Override
			public void handle(final DragEvent event)
			{
				final Object dragged_object = event.getGestureSource();
				
				/* If dragged object is time counter controls enclosing
				 * pane AND ... */
				if (dragged_object instanceof HBox &&
						// ... It's dragged over other than its initial place
						time_counters_pane_children.indexOf(dragged_object) !=
								time_counters_pane_children.indexOf(time_counter_unit))
				{
					event.acceptTransferModes(TransferMode.MOVE);
				}
			}
		});
		
		time_counter_unit.setOnDragDropped(new EventHandler<DragEvent>()
		{
			@Override
			public void handle(final DragEvent event)
			{
				// Dropped time counter controls pane
				final HBox dropped_object = (HBox)event.getGestureSource();
				final int dropped_object_index =
						time_counters_pane_children.indexOf(dropped_object);
				
				try
				{
					time_counters_pane_children_lock.lockInterruptibly();
				}
				catch (final InterruptedException exc)
				{
					logger.log(Level.INFO,
							"Thread interrupts. Exception stack trace:", exc);
					Thread.currentThread().interrupt();
				}
				
				try
				{
					time_counters_pane_children.remove(dropped_object);
					
					// Time counter controls pane on which other pane is dropped
					final int time_counter_unit_index =
							time_counters_pane_children.indexOf(time_counter_unit);
					
					assert dropped_object_index != -1 && time_counter_unit_index != -1 :
						"No\u00A0such time\u00A0counter controls pane(\u2011s) in "
						+ ObservableList.class.getName()
						+ " \"time_counters_pane\"";
					
					time_counters_pane_children.add(
							dropped_object_index <= time_counter_unit_index ?
									time_counter_unit_index + 1 : time_counter_unit_index,
									dropped_object);
				}
				finally
				{
					time_counters_pane_children_lock.unlock();
				}
			}
		});
		//----------------------------------------------------/////
		
		
		///// Time counter description text field implementation /////
		// User-defined time counter description
		final TextField description =
				new TextField(time_counter.description_text);
		
		HBox.setHgrow(description, Priority.ALWAYS);
		
		description.textProperty().addListener(new ChangeListener<String>()
		{
			@Override
			public void changed(
					final ObservableValue<? extends String> observable,
					final String old_value, final String new_value)
			{
				time_counter.description_text = description.getText();
			}
		});
		
		description.setPromptText(text_fields_resources.getString(
				"time_counter_description_prompt"));
		//-------------------------------------------------------/////
		
		
		///// Time counter mode image and time counter value implementation /////
		// Time counter mode image
		final ImageView mode_image =
				new ImageView(mode_images.get(time_counter.instance_mode));
		
		mode_image.setFitWidth(images_size);
		mode_image.setPreserveRatio(true);
		
		// ImageView tooltip text
		final StringBuilder image_tooltip = new StringBuilder(
				hints_resources.getString("mode_image_tooltip.1"));
		
		// Provide tooltip for "time_counter_value"
		switch (time_counter.instance_mode)
		{
		case M_stopwatch:
			image_tooltip.append(
					time_counter_resources.getString("modes.stopwatch"));
			
			break;
			
		case M_countdown:
			image_tooltip.append(
					time_counter_resources.getString("modes.timer"));
			
			/* Timer tooltip text which provides information about initial
			 * time values */
			final StringBuilder timer_tooltip = new StringBuilder(
					hints_resources.getString("initial_timer_time_tooltip"));
			final Solo_counter cast_to_Solo_counter = (Solo_counter)time_counter;
			final Initial_time_values initial_time_values =
					cast_to_Solo_counter.get_initial_time_values();
			
			/* "timer_tooltip" formatting is going from bigger time units to
			 * lower. Once time unit is nonzero, all following time units
			 * must be shown anyway.
			 * true - nonzero value is reached; false - otherwise */
			boolean value_reached = false;
			
			// If years value is nonzero
			if (initial_time_values.period_initial.getYears() != 0)
			{
				timer_tooltip.append(
						initial_time_values.period_initial.getYears());
				timer_tooltip.append(time_unit_names_resources.getString(
						"TUL_short_name.years"));
				value_reached = true;
			}
			
			// If nonzero time unit already reached OR ...
			if (value_reached ||
					// ... it's reached now
					initial_time_values.period_initial.getMonths() != 0)
			{
				timer_tooltip.append(
						initial_time_values.period_initial.getMonths());
				timer_tooltip.append(time_unit_names_resources.getString(
						"TUL_short_name.months"));
				value_reached = true;
			}
			
			// If nonzero time unit already reached OR ...
			if (value_reached ||
					// ... it's reached now
					initial_time_values.period_initial.getDays() != 0)
			{
				timer_tooltip.append(
						initial_time_values.period_initial.getDays());
				timer_tooltip.append(time_unit_names_resources.getString(
						"TUL_short_name.days"));
				value_reached = true;
			}
			
			// If nonzero time unit already reached OR ...
			if (value_reached ||
					// ... it's reached now
					initial_time_values.duration_initial.getHour() != 0)
			{
				timer_tooltip.append(
						initial_time_values.duration_initial.getHour());
				timer_tooltip.append(time_unit_names_resources.getString(
						"TUL_short_name.hours"));
				value_reached = true;
			}
			
			// If nonzero time unit already reached OR ...
			if (value_reached ||
					// ... it's reached now
					initial_time_values.duration_initial.getMinute() != 0)
			{
				timer_tooltip.append(
						initial_time_values.duration_initial.getMinute());
				timer_tooltip.append(time_unit_names_resources.getString(
						"TUL_short_name.minutes"));
			}
			
			timer_tooltip.append(
					initial_time_values.duration_initial.getSecond());
			timer_tooltip.append(time_unit_names_resources.getString(
					"TUL_short_name.seconds"));
			
			time_counter_value.setTooltip(new Tooltip(timer_tooltip.toString()));
			
			break;
			
		case M_elapsed_from:
			image_tooltip.append(
					time_counter_resources.getString("modes.elapsed_from"));
			
			/* Elapsed from time counter text which provides information
			 * about date time, difference from which is calculated */
			final StringBuilder elapsed_from_tooltip = new StringBuilder(
					time_counter_resources.getString("modes.elapsed_from") + ' ');
			final Instance_counter cast_to_Instance_counter1 =
					(Instance_counter)time_counter;
			
			// Program locale
			final Locale_setting program_locale1 =
					program_settings.get_locale_setting();
			// Locale to pass as methods argument
			final Locale locale_to_pass1 = new Locale(
					program_locale1.language_code,
					program_locale1.country_code,
					program_locale1.variant_code);
			
			elapsed_from_tooltip.append(
					cast_to_Instance_counter1.time_instance.getDayOfWeek().getDisplayName(
							TextStyle.SHORT, locale_to_pass1));
			elapsed_from_tooltip.append(' ');
			elapsed_from_tooltip.append(
					cast_to_Instance_counter1.time_instance.format(
							DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(
									locale_to_pass1)));
			
			time_counter_value.setTooltip(
					new Tooltip(elapsed_from_tooltip.toString()));
			
			break;
			
		case M_remains_till:
			image_tooltip.append(
					time_counter_resources.getString("modes.remains_till"));
			
			/* Remains till time counter text which provides information
			 * about date time, remaining value to which is calculated */
			final StringBuilder remains_till_tooltip = new StringBuilder(
					time_counter_resources.getString("modes.remains_till") + ' ');
			final Instance_counter cast_to_Instance_counter2 =
					(Instance_counter)time_counter;
			
			// Program locale
			final Locale_setting program_locale2 =
					program_settings.get_locale_setting();
			// Locale to pass as methods argument
			final Locale locale_to_pass2 = new Locale(
					program_locale2.language_code,
					program_locale2.country_code,
					program_locale2.variant_code);
			
			remains_till_tooltip.append(
					cast_to_Instance_counter2.time_instance.getDayOfWeek().getDisplayName(
							TextStyle.SHORT, locale_to_pass2));
			remains_till_tooltip.append(' ');
			remains_till_tooltip.append(
					cast_to_Instance_counter2.time_instance.format(
							DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(
									locale_to_pass2)));
			
			time_counter_value.setTooltip(
					new Tooltip(remains_till_tooltip.toString()));
			
			break;
			
		default:
			throw new EnumConstantNotPresentException(
					Mode.class, time_counter.instance_mode.name());
		}
		
		image_tooltip.append(hints_resources.getString("mode_image_tooltip.2"));
		Tooltip.install(mode_image, new Tooltip(image_tooltip.toString()));
		
		// Time counter value
		final Time_counter_text_value time_value =
				time_counter.get_time_counter_text_value();
		
		time_counter_value.setText(time_value.time_value_text);
		
		// If current time value is negative (i.e. has minus sign)
		if (!time_value.is_positive)
		{
			time_counter_value.setTextFill(time_elapsed_color);
		}
		
		// Copy to clipboard menu item for "time_counter_value" label
		final MenuItem copy_to_clipboard = new MenuItem(
				menu_items_resources.getString("copy_to_clipboard"));
		
		copy_to_clipboard.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				final ClipboardContent clipboard_content = new ClipboardContent();
				
				clipboard_content.putString(time_counter_value.getText());
				Clipboard.getSystemClipboard().setContent(clipboard_content);
			}
		});
		
		time_counter_value.setContextMenu(new ContextMenu(copy_to_clipboard));
		time_counter_value.setFocusTraversable(true);
		
		// Time counter value label default background
		final Background time_counter_value_default_background =
				time_counter_value.getBackground();
		
		time_counter_value.focusedProperty().addListener(
				new ChangeListener<Boolean>()
		{
			@Override
			public void changed(
					final ObservableValue<? extends Boolean> observable,
					final Boolean old_value, final Boolean new_value)
			{
				// If the label is focused now
				if (new_value)
				{
					time_counter_value.setBackground(new Background(
							new BackgroundFill(Color.ALICEBLUE, null, null)));
				}
				else
				{
					time_counter_value.setBackground(
							time_counter_value_default_background);
				}
			}
		});
		
		/* All time counter types except stopwatch must listen to
		 * the time elapsed event */
		if (!time_counter.instance_mode.equals(Mode.M_stopwatch))
		{
			time_counter.add_Time_elapsed_listener(time_elapsed_listener);
		}
		
		time_counter.add_Time_counter_text_listener(new Time_counter_text_listener()
		{
			@Override
			public void time_counter_text_changed(
					final Time_counter_event<?> event,
					final Time_counter_text_value new_value)
			{
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						time_counter_value.setText(new_value.time_value_text);
					}
				});
			}
		});
		//------------------------------------------------------------------/////
		
		
		// Cursor to appear when hovering over time counter buttons
		final Cursor buttons_cursor = Cursor.DEFAULT;
		
		///// "Close time counter" button implementation /////
		// Close time counter button
		final Button close_button = new Button(null, new ImageView(close_image));

		close_button.setId(close_button_id);
		close_button.setTooltip(
				new Tooltip(hints_resources.getString("close_button_tooltip")));
		close_button.setCursor(buttons_cursor);
		
		close_button.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				/* TODO: Provide confirming dialog after implementing custom
				 * DialogPane with "Disable this checking" CheckBox */
				
				/////-/ Menu bar managing section -------------------------/////
				time_counters_quantity_lock.lock();
				
				try
				{
					change_time_counters_quantity(time_counter.instance_mode, true);
					
					// If removed time counter worked in stopwatch mode
					if (time_counter.instance_mode.equals(Mode.M_stopwatch))
					{
						final Solo_counter cast_to_Solo_counter =
								(Solo_counter)time_counter;
						
						// If stopwatch was running when removing
						if (cast_to_Solo_counter.is_running())
						{
							// If the only running stopwatch was removed
							if (--running_stopwatches == 0)
							{
								Top_pane.set_disable_pause_stopwatches_menu_item(true);
								
								/* If there are no running stopwatches and
								 * timers after removing the last stopwatch */
								if (running_timers == 0)
								{
									Top_pane.set_disable_pause_stopwatches_timers_menu_item(true);
								}
							}
							
							assert running_stopwatches >= 0 :
								"Incorrect time counters quantity value";
							assert running_stopwatches + paused_stopwatches ==
									time_counters_quantity.get(Mode.M_stopwatch) :
										"Incorrect time counters quantity value";
						}
						else
						{
							--paused_stopwatches;
							
							// If removed stopwatch was in numeric overflow state
							if (cast_to_Solo_counter.numeric_overflow_status())
							{
								--overflowed_stopwatches;
								
								assert overflowed_stopwatches >= 0 :
									"Incorrect time counters quantity value";
							}
							else
							{
								/* If there are no paused stopwatches (with
								 * possibility to be started) remaining after
								 * removing this one */
								if (paused_stopwatches == 0)
								{
									Top_pane.set_disable_start_stopwatches_menu_item(true);
									
									/* If there are no paused stopwatches and
									 * timers (with possibility to be started)
									 * remaining after removing the last
									 * stopwatch */
									if (paused_timers == 0)
									{
										Top_pane.set_disable_start_stopwatches_timers_menu_item(true);
									}
								}
								
								// If removed stopwatch was in its initial state
								if (!cast_to_Solo_counter.counting_had_started())
								{
									--restarted_stopwatches;
									
									assert restarted_stopwatches >= 0 :
										"Incorrect time counters quantity value";
								}
							}
							
							assert paused_stopwatches >= 0 :
								"Incorrect time counters quantity value";
							assert paused_stopwatches >= restarted_stopwatches :
								"Incorrect time counters quantity value";
							assert paused_stopwatches >= overflowed_stopwatches :
								"Incorrect time counters quantity value";
							assert paused_stopwatches + running_stopwatches ==
									time_counters_quantity.get(Mode.M_stopwatch) :
										"Incorrect time counters quantity value";
						}
						
						/* If there are no stopwatches to restart after removing
						 * this one */
						if (running_stopwatches + paused_stopwatches -
								restarted_stopwatches == 0)
						{
							Top_pane.set_disable_restart_stopwatches_menu_item(true);
					
							/* If there are no stopwatches and timers to restart
							 * after removing this one */
							if (running_stopwatches + running_timers +
									paused_stopwatches + paused_timers -
									restarted_stopwatches - restarted_timers == 0)
							{
								Top_pane.set_disable_restart_stopwatches_timers_menu_item(true);
							}
						}
					}
					// If removed time counter worked in timer mode
					else if (time_counter.instance_mode.equals(Mode.M_countdown))
					{
						final Solo_counter cast_to_Solo_counter =
								(Solo_counter)time_counter;
						
						// If removed timer was running when removing
						if (cast_to_Solo_counter.is_running())
						{
							// If the only running timer was removed
							if (--running_timers == 0)
							{
								Top_pane.set_disable_pause_timers_menu_item(true);
								
								/* If there are no running stopwatches and
								 * timers after removing the last timer */
								if (running_stopwatches == 0)
								{
									Top_pane.set_disable_pause_stopwatches_timers_menu_item(true);
								}
							}
							
							assert running_timers >= 0 :
								"Incorrect time counters quantity value";
							assert running_timers + paused_timers ==
									time_counters_quantity.get(Mode.M_countdown) :
										"Incorrect time counters quantity value";
						}
						else
						{
							--paused_timers;
							
							// If removed timer was in numeric overflow state
							if (cast_to_Solo_counter.numeric_overflow_status())
							{
								--overflowed_timers;
								
								assert overflowed_timers >= 0 :
									"Incorrect time counters quantity value";
							}
							else
							{
								/* If there are no paused timers (with
								 * possibility to be started) remaining after
								 * removing this one */
								if (paused_timers == 0)
								{
									Top_pane.set_disable_start_timers_menu_item(true);
									
									/* If there are no paused stopwatches and
									 * timers (with possibility to be started)
									 * remaining after removing the last timer */
									if (paused_stopwatches == 0)
									{
										Top_pane.set_disable_start_stopwatches_timers_menu_item(true);
									}
								}
								
								// If removed timer was in its initial state
								if (!cast_to_Solo_counter.counting_had_started())
								{
									--restarted_timers;
									
									assert restarted_timers >= 0 :
										"Incorrect time counters quantity value";
								}
							}
							
							assert paused_timers >= 0 :
								"Incorrect time counters quantity value";
							assert paused_timers >= restarted_timers :
								"Incorrect time counters quantity value";
							assert paused_timers >= overflowed_stopwatches :
								"Incorrect time counters quantity value";
							assert paused_timers + running_timers ==
									time_counters_quantity.get(Mode.M_countdown) :
										"Incorrect time counters quantity value";
						}
						
						/* If there are no timers to restart after removing
						 * this one */
						if (running_timers + paused_timers - restarted_timers == 0)
						{
							Top_pane.set_disable_restart_timers_menu_item(true);
							
							/* If there are no stopwatches and timers to restart
							 * after removing this one */
							if (running_stopwatches + running_timers +
									paused_stopwatches + paused_timers -
									restarted_stopwatches - restarted_timers == 0)
							{
								Top_pane.set_disable_restart_stopwatches_timers_menu_item(true);
							}
						}
					}
				}
				finally
				{
					time_counters_quantity_lock.unlock();
				}
				/////-/----------------------------------------------------/////
				
				try
				{
					time_counters_pane_children_lock.lockInterruptibly();
				}
				catch (final InterruptedException exc)
				{
					logger.log(Level.INFO,
							"Thread interrupts. Exception stack trace:", exc);
					Thread.currentThread().interrupt();
				}
				
				try
				{
					time_counters_pane_children.remove(time_counter_unit);
					Time_counter_control.get_instance().get_time_counters().remove(
							time_counter);
				}
				finally
				{
					time_counters_pane_children_lock.unlock();
				}
				
				time_counters_pane.setPrefRows(
						time_counters_pane.getPrefRows() - 1);
			}
		});
		//-----------------------------------------------/////
		
		
		///// "Single time counter settings" button implementation /////
		// "setting_button" image
		final ImageView settings_button_image = new ImageView(setting_image);
		
		settings_button_image.setFitWidth(images_size);
		settings_button_image.setPreserveRatio(true);
		
		// Time counter settings button
		final Button setting_button = new Button(null, settings_button_image);
		
		setting_button.setTooltip(new Tooltip(hints_resources.getString(
				"time_counter_setting_button_tooltip")));
		setting_button.setCursor(buttons_cursor);
		
		setting_button.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				// TODO: Launch single time counter settings window
				
				/* If time counter value had positive sign after this
				 * time counter settings window closed */
				if (time_counter.get_time_counter_text_value().is_positive)
				{
					time_counter_value.setTextFill(default_color);
				}
				else
				{
					time_counter_value.setTextFill(time_elapsed_color);
				}
			}
		});
		//---------------------------------------------------------/////
		
		
		time_counters_quantity_lock.lock();
		
		try
		{
			change_time_counters_quantity(time_counter.instance_mode, false);
		}
		finally
		{
			time_counters_quantity_lock.unlock();
		}

		
		///// Add created controls to "time_counter_unit" pane /////
		/* For "Solo_counter" objects "Start/Resume/Pause" and "Restart" buttons
		 * are additionally provided */
		if (time_counter instanceof Solo_counter)
		{
			///// "Start/Resume/Pause" and "Restart" buttons implementation /////
			/* Start/pause/resume button. Available only for "Solo_counter"
			 * objects */
			final Button start_button = new Button();
			// Time counter restart. Available only for "Solo_counter" objects
			final Button restart_button = new Button();
			
			// "Solo_counter" methods are used frequently within this scope
			final Solo_counter cast_to_Solo_counter =
					(Solo_counter)time_counter;
			
			start_button.setId(start_button_id);
			restart_button.setId(restart_button_id);
			restart_button.setGraphic(new ImageView(restart_image));
			restart_button.setTooltip(new Tooltip(
					hints_resources.getString("restart_button_tooltip")));
			
			/* 'true' — need to check is there "Restart all stopwatches and
			 * timers" menu item need to be enabled after adding "Solo_counter"
			 * object */
			boolean check_total = false;
			
			time_counters_quantity_lock.lock();
			
			try
			{
				// If counting had started after creating or restarting
				if (cast_to_Solo_counter.counting_had_started())
				{
					// If time counter is now running
					if (cast_to_Solo_counter.is_running())
					{
						start_button.setGraphic(new ImageView(pause_image));
						start_button.setTooltip(new Tooltip(
								hints_resources.getString(
										"start_button_tooltips.pause")));
						
						
						/////-/ Menu bar managing section -----------------/////
						// If time counter works in stopwatch mode
						if (cast_to_Solo_counter.instance_mode.equals(
								Mode.M_stopwatch))
						{
							/* If added stopwatch is the first stopwatch that
							 * can be paused */
							if (++running_stopwatches == 1)
							{
								Top_pane.set_disable_pause_stopwatches_menu_item(false);
								
								/* If added stopwatch is the first among
								 * "Solo_counter" objects that can be paused */
								if (running_timers == 0)
								{
									Top_pane.set_disable_pause_stopwatches_timers_menu_item(false);
								}
								
								/* If added stopwatch is the first stopwatch
								 * that can be restarted */
								if (running_stopwatches + paused_stopwatches -
										restarted_stopwatches == 1)
								{
									Top_pane.set_disable_restart_stopwatches_menu_item(false);
									check_total = true;
								}
							}
							
							assert running_stopwatches + paused_stopwatches ==
									time_counters_quantity.get(Mode.M_stopwatch) :
										"Incorrect time counters quantity value";
						}
						// Time counter works in timer mode ("Mode.M_countdown")
						else
						{
							/* If added timer is the first timer that can be
							 * paused */
							if (++running_timers == 1)
							{
								Top_pane.set_disable_pause_timers_menu_item(false);
								
								/* If added timer is the first among
								 * "Solo_counter" objects that can be paused */
								if (running_stopwatches == 0)
								{
									Top_pane.set_disable_pause_stopwatches_timers_menu_item(false);
								}
								
								/* If added timer is the first timer that can be
								 * restarted */
								if (running_timers + paused_timers -
										restarted_timers == 1)
								{
									Top_pane.set_disable_restart_timers_menu_item(false);
									check_total = true;
								}
							}
							
							assert running_timers + paused_timers ==
									time_counters_quantity.get(Mode.M_countdown) :
										"Incorrect time counters quantity value";
						}
						/////-/--------------------------------------------/////
						
						
						// If time counter value is negative
						if (!time_value.is_positive)
						{
							time_counter_value.setTextFill(time_elapsed_color);
						}
					}
					else
					{
						// If time counter is in numeric overflow status
						if (cast_to_Solo_counter.numeric_overflow_status())
						{
							start_button.setDisable(true);
							start_button.setGraphic(new ImageView(start_image));
							time_counter_value.setTextFill(time_elapsed_color);
							
							
							/////-/ Menu bar managing section -------------/////
							// If time counter works in stopwatch mode
							if (cast_to_Solo_counter.instance_mode.equals(
									Mode.M_stopwatch))
							{
								++overflowed_stopwatches;
							}
							// Time counter works in timer mode ("Mode.M_countdown")
							else
							{
								++overflowed_timers;
							}
							/////-/----------------------------------------/////
						}
						else
						{
							start_button.setGraphic(new ImageView(resume_image));
							start_button.setTooltip(new Tooltip(
									hints_resources.getString(
											"start_button_tooltips.resume")));
						}
						
						
						/////-/ Menu bar managing section -----------------/////
						// If time counter works in stopwatch mode
						if (cast_to_Solo_counter.instance_mode.equals(
								Mode.M_stopwatch))
						{
							/* If added stopwatch is the first stopwatch that
							 * can be started/resumed */
							if (++paused_stopwatches - overflowed_stopwatches == 1)
							{
								Top_pane.set_disable_start_stopwatches_menu_item(false);
								check_total = true;

								/* If added stopwatch is the first among
								 * "Solo_counter" objects that can be
								 * started/resumed */
								if (paused_timers - overflowed_timers == 0)
								{
									Top_pane.set_disable_start_stopwatches_timers_menu_item(false);
								}
								
								/* If added stopwatch is the first that can be
								 * restarted */
								if (running_stopwatches + paused_stopwatches -
										restarted_stopwatches == 1)
								{
									Top_pane.set_disable_restart_stopwatches_menu_item(false);
								}
							}
							
							assert paused_stopwatches >= restarted_stopwatches :
								"Incorrect time counters quantity value";
							assert paused_stopwatches >= overflowed_stopwatches :
								"Incorrect time counters quantity value";
							assert paused_stopwatches + running_stopwatches ==
									time_counters_quantity.get(Mode.M_stopwatch) :
										"Incorrect time counters quantity value";
						}
						// Time counter works in timer mode ("Mode.M_countdown")
						else
						{
							/* If added timer is the first timer that can be
							 * started/resumed */
							if (++paused_timers - overflowed_timers == 1)
							{
								Top_pane.set_disable_start_timers_menu_item(false);
								check_total = true;
								
								/* If added timer is the first among
								 * "Solo_counter" objects that can be
								 * started/resumed */
								if (paused_stopwatches - overflowed_stopwatches == 0)
								{
									Top_pane.set_disable_start_stopwatches_timers_menu_item(false);
								}
								
								/* If added timer is the first that can be
								 * restarted */
								if (running_timers + paused_timers -
										restarted_timers == 1)
								{
									Top_pane.set_disable_restart_timers_menu_item(false);
								}
							}
							
							assert paused_timers >= restarted_timers :
								"Incorrect time counters quantity value";
							assert paused_timers >= overflowed_timers :
								"Incorrect time counters quantity value";
							assert paused_timers + running_timers ==
									time_counters_quantity.get(Mode.M_countdown) :
										"Incorrect time counters quantity value";
						}
						/////-/--------------------------------------------/////
						
						
						// If time counter value is positive
						if (time_value.is_positive)
						{
							time_counter_value.setTextFill(default_disabled_color);
						}
						else
						{
							time_counter_value.setTextFill(time_elapsed_disabled_color);
						}
					}
				}
				else
				{
					start_button.setGraphic(new ImageView(start_image));
					start_button.setTooltip(new Tooltip(
							hints_resources.getString(
									"start_button_tooltips.start")));
					restart_button.setDisable(true);
					time_counter_value.setTextFill(default_disabled_color);
					check_total = true;
					
					
					/////-/ Menu bar managing section ---------------------/////
					// If time counter works in stopwatch mode
					if (cast_to_Solo_counter.instance_mode.equals(Mode.M_stopwatch))
					{
						/* If added stopwatch is the first stopwatch, that can
						 * be started/resumed */
						if (++paused_stopwatches - overflowed_stopwatches == 1)
						{
							Top_pane.set_disable_start_stopwatches_menu_item(false);
							
							/* If added stopwatch is the first among
							 * "Solo_counter" objects that can be started/resumed */
							if (paused_timers - overflowed_timers == 0)
							{
								Top_pane.set_disable_start_stopwatches_timers_menu_item(false);
							}
						}
						
						++restarted_stopwatches;
						
						assert paused_stopwatches >= restarted_stopwatches :
							"Incorrect time counters quantity value";
						assert paused_stopwatches >= overflowed_stopwatches :
							"Incorrect time counters quantity value";
						assert paused_stopwatches + running_stopwatches ==
								time_counters_quantity.get(Mode.M_stopwatch) :
									"Incorrect time counters quantity value";
					}
					// Time counter works in timer mode ("Mode.M_countdown")
					else
					{
						// If this is the first timer, that can be started, added
						if (++paused_timers - overflowed_timers == 1)
						{
							Top_pane.set_disable_start_timers_menu_item(false);
							
							/* If there are no paused stopwatches that can be
							 * started */
							if (paused_stopwatches - overflowed_stopwatches == 0)
							{
								Top_pane.set_disable_start_stopwatches_timers_menu_item(false);
							}
						}
						
						++restarted_timers;
						
						assert paused_timers >= restarted_timers :
							"Incorrect time counters quantity value";
						assert paused_timers >= overflowed_timers :
							"Incorrect time counters quantity value";
						assert paused_timers + running_timers ==
								time_counters_quantity.get(Mode.M_countdown) :
									"Incorrect time counters quantity value";
					}
					/////-/------------------------------------------------/////
				}
				
				
				/////-/ Menu bar managing section -------------------------/////
				/* If need to check is there "Restart all stopwatches and timers"
				 * menu item need to be enabled AND ... */
				if (check_total &&
						/* ... mentioned menu item really needs to be enabled
						 * after adding first "Solo_counter" object that can be
						 * restarted */
						running_stopwatches + running_timers +
						paused_stopwatches + paused_timers -
						restarted_stopwatches - restarted_timers == 1)
				{
					Top_pane.set_disable_restart_stopwatches_timers_menu_item(false);
				}
				/////-/----------------------------------------------------/////
			}
			finally
			{
				time_counters_quantity_lock.unlock();
			}
			
			start_button.setOnAction(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(final ActionEvent event)
				{
					// If need to pause time counter
					if (cast_to_Solo_counter.is_running())
					{
						cast_to_Solo_counter.pause();
						start_button.setGraphic(new ImageView(resume_image));
						start_button.getTooltip().setText(
								hints_resources.getString(
										"start_button_tooltips.resume"));
						time_counter_value.setTextFill(
								cast_to_Solo_counter.get_time_counter_text_value().is_positive ?
										default_disabled_color :
											time_elapsed_disabled_color);
						
						
						/////-/ Menu bar managing section -----------------/////
						// If time counter works in stopwatch mode
						if (cast_to_Solo_counter.instance_mode.equals(Mode.M_stopwatch))
						{
							time_counters_quantity_lock.lock();
							
							try
							{
								/* If there are no stopwatches to pause after
								 * pausing this one */
								if (--running_stopwatches == 0)
								{
									Top_pane.set_disable_pause_stopwatches_menu_item(true);
									
									/* If paused stopwatch was the last
									 * "Solo_counter" object among running */
									if (running_timers == 0)
									{
										Top_pane.set_disable_pause_stopwatches_timers_menu_item(true);
									}
								}
								
								/* If paused stopwatch is the first that can be
								 * started/resumed */
								if ((++paused_stopwatches) -
										overflowed_stopwatches == 1)
								{
									Top_pane.set_disable_start_stopwatches_menu_item(false);
									
									/* If paused stopwatch is the first among
									 * all stopwatches and timers that can be
									 * started/resumed */
									if (paused_timers - overflowed_timers == 0)
									{
										Top_pane.set_disable_start_stopwatches_timers_menu_item(false);
									}
								}
								
								assert running_stopwatches >= 0 :
									"Incorrect time counters quantity value";
								assert paused_stopwatches >= restarted_stopwatches :
									"Incorrect time counters quantity value";
								assert paused_stopwatches >= overflowed_stopwatches :
									"Incorrect time counters quantity value";
								assert paused_stopwatches + running_stopwatches ==
										time_counters_quantity.get(Mode.M_stopwatch) :
											"Incorrect time counters quantity value";
							}
							finally
							{
								time_counters_quantity_lock.unlock();
							}
						}
						// Time counter works in timer mode ("Mode.M_countdown")
						else
						{
							time_counters_quantity_lock.lock();
							
							try
							{
								/* If there are no timers to pause after pausing
								 * this one */
								if (--running_timers == 0)
								{
									Top_pane.set_disable_pause_timers_menu_item(true);
									
									/* If paused timer was the last "Solo_counter"
									 * object among running */
									if (running_stopwatches == 0)
									{
										Top_pane.set_disable_pause_stopwatches_timers_menu_item(true);
									}
								}
								
								/* If paused timer is the first that can be
								 * started/resumed */
								if ((++paused_timers) - overflowed_timers == 1)
								{
									Top_pane.set_disable_start_timers_menu_item(false);
									
									/* If paused timer is the first among all
									 * stopwatches and timers that can be
									 * started/resumed */
									if (paused_stopwatches -
											overflowed_stopwatches == 0)
									{
										Top_pane.set_disable_start_stopwatches_timers_menu_item(false);
									}
								}
								
								assert running_timers >= 0 :
									"Incorrect time counters quantity value";
								assert paused_timers >= restarted_timers :
									"Incorrect time counters quantity value";
								assert paused_timers >= overflowed_timers :
									"Incorrect time counters quantity value";
								assert running_timers + paused_timers ==
										time_counters_quantity.get(Mode.M_countdown) :
											"Incorrect time counters quantity value";
							}
							finally
							{
								time_counters_quantity_lock.unlock();
							}
						}
						/////-/--------------------------------------------/////
					}
					// Need to start/resume time counter
					else
					{
						/////-/ Menu bar managing section -----------------/////
						// If time counter works in stopwatch mode
						if (cast_to_Solo_counter.instance_mode.equals(Mode.M_stopwatch))
						{
							time_counters_quantity_lock.lock();
							
							try
							{
								/* If stopwatch to start/resume is the last one
								 * that can be started/resumed */
								if (--paused_stopwatches == 0)
								{
									Top_pane.set_disable_start_stopwatches_menu_item(true);
									
									/* If stopwatch to start/resume is
									 * the last one among "Solo_counter" objects
									 * that can be started/resumed */
									if (paused_timers == 0)
									{
										Top_pane.set_disable_start_stopwatches_timers_menu_item(true);
									}
								}
								
								/* If this is the first stopwatch that can be
								 * paused */
								if (++running_stopwatches == 1)
								{
									Top_pane.set_disable_pause_stopwatches_menu_item(false);
									
									/* If stopwatch to start/resume is the first
									 * among "Solo_counter" objects that can be
									 * paused */
									if (running_timers == 0)
									{
										Top_pane.set_disable_pause_stopwatches_timers_menu_item(false);
									}
								}
								
								// If stopwatch is in its initial state
								if (!cast_to_Solo_counter.counting_had_started())
								{
									/* If stopwatch to start/resume is the first
									 * that can be restarted */
									if (running_stopwatches + paused_stopwatches -
											(--restarted_stopwatches) == 1)
									{
										Top_pane.set_disable_restart_stopwatches_menu_item(false);
										
										/* If stopwatch to start/resume is
										 * the first among "Solo_counter"
										 * objects that can be restarted */
										if (running_timers + paused_timers -
												restarted_timers == 0)
										{
											Top_pane.set_disable_restart_stopwatches_timers_menu_item(false);
										}
									}
									
									assert restarted_stopwatches >= 0 :
										"Incorrect time counters quantity value";
								}
								
								assert paused_stopwatches >= 0 :
									"Incorrect time counters quantity value";
								assert paused_stopwatches + running_stopwatches ==
										time_counters_quantity.get(Mode.M_stopwatch) :
											"Incorrect time counters quantity value";
								assert restarted_stopwatches <= paused_stopwatches :
									"Incorrect time counters quantity value";
							}
							finally
							{
								time_counters_quantity_lock.unlock();
							}
						}
						// Time counter works in timer mode ("Mode.M_countdown")
						else
						{
							time_counters_quantity_lock.lock();
							
							try
							{
								/* If timer to start/resume is the last on that
								 * can be started/resumed */
								if (--paused_timers == 0)
								{
									Top_pane.set_disable_start_timers_menu_item(true);
									
									/* If timer to start/resume is the last one
									 * among "Solo_counter" objects that can be
									 * started/resumed */
									if (paused_stopwatches == 0)
									{
										Top_pane.set_disable_start_stopwatches_timers_menu_item(true);
									}
								}
								
								// If this is the first timer that can be paused
								if (++running_timers == 1)
								{
									Top_pane.set_disable_pause_timers_menu_item(false);
									
									/* If timer to start/resume is the first
									 * among "Solo_counter" objects that can be
									 * paused */
									if (running_stopwatches == 0)
									{
										Top_pane.set_disable_pause_stopwatches_timers_menu_item(false);
									}
								}
								
								// If timer is in its initial state
								if (!cast_to_Solo_counter.counting_had_started())
								{
									/* If timer to start/resume is the first
									 * that can be restarted */
									if (running_timers + paused_timers -
											(--restarted_timers) == 1)
									{
										Top_pane.set_disable_restart_timers_menu_item(false);
										
										/* If timer to start/pause is the first
										 * among "Solo_counter" objects that can
										 * be restarted */
										if (running_stopwatches +
												paused_stopwatches -
												restarted_stopwatches == 0)
										{
											Top_pane.set_disable_restart_stopwatches_timers_menu_item(false);
										}
									}
									
									assert restarted_timers >= 0 :
										"Incorrect time counters quantity value";
								}
								
								assert paused_timers >= 0 :
									"Incorrect time counters quantity value";
								assert paused_timers + running_timers ==
										time_counters_quantity.get(Mode.M_countdown) :
											"Incorrect time counters quantity value";
								assert restarted_timers <= paused_timers :
									"Incorrect time counters quantity value";
							}
							finally
							{
								time_counters_quantity_lock.unlock();
							}
						}
						/////-/--------------------------------------------/////
						
						
						cast_to_Solo_counter.start();
						start_button.setGraphic(new ImageView(pause_image));
						start_button.getTooltip().setText(
								hints_resources.getString(
										"start_button_tooltips.pause"));
						restart_button.setDisable(false);
						time_counter_value.setTextFill(
								cast_to_Solo_counter.get_time_counter_text_value().is_positive ?
										default_color : time_elapsed_color);
					}
				}
			});
			
			restart_button.setOnAction(new EventHandler<ActionEvent>()
			{
				@Override
				public void handle(final ActionEvent event)
				{
					/* TODO: Provide confirming dialog after implementing custom
					 * DialogPane with "Disable this checking" CheckBox */
					
					// 'true' — time counter is in numeric overflow state
					final boolean numeric_overflow_status =
							cast_to_Solo_counter.numeric_overflow_status();
					
					cast_to_Solo_counter.restart();
					
					// If time counter is in paused state
					if (!cast_to_Solo_counter.is_running())
					{
						restart_button.setDisable(true);
						start_button.setGraphic(new ImageView(start_image));
						start_button.getTooltip().setText(
								hints_resources.getString(
										"start_button_tooltips.start"));
						
						
						/////-/ Menu bar managing section -----------------/////
						// If time counter works in stopwatch mode
						if (cast_to_Solo_counter.instance_mode.equals(Mode.M_stopwatch))
						{
							time_counters_quantity_lock.lock();
							
							try
							{
								/* If there are no stopwatches to restart after
								 * restarting this one */
								if (running_stopwatches + paused_stopwatches -
										(++restarted_stopwatches) == 0)
								{
									Top_pane.set_disable_restart_stopwatches_menu_item(true);
									
									/* If there are no stopwatches and timers to
									 * restart at all after restarting this
									 * stopwatch */
									if (running_timers + paused_timers -
											restarted_timers == 0)
									{
										Top_pane.set_disable_restart_stopwatches_timers_menu_item(true);
									}
								}
								
								assert restarted_stopwatches <= paused_stopwatches :
									"Incorrect time counters quantity value";
								
								/* If stopwatch had been in numeric overflow
								 * state before it was restarted */
								if (numeric_overflow_status)
								{
									/* If restarted stopwatch is the first one
									 * that can be started/resumed */
									if (paused_stopwatches - (--overflowed_stopwatches) == 1)
									{
										Top_pane.set_disable_start_stopwatches_menu_item(false);
										
										/* If restarted stopwatch is
										 * the first one among "Solo_counter"
										 * objects that can be started/resumed */
										if (paused_timers - overflowed_timers == 0)
										{
											Top_pane.set_disable_start_stopwatches_timers_menu_item(false);
										}
									}
									
									assert overflowed_stopwatches >= 0 :
										"Incorrect time counters quantity value";
								}
							}
							finally
							{
								time_counters_quantity_lock.unlock();
							}
						}
						// Time counter works in timer mode ("Mode.M_countdown")
						else
						{
							time_counters_quantity_lock.lock();
							
							try
							{
								/* If there are no timers to restart after
								 * restarting this one */
								if (running_timers + paused_timers -
										(++restarted_timers) == 0)
								{
									Top_pane.set_disable_restart_timers_menu_item(true);
									
									/* If there are no stopwatches and timers to
									 * restart at all after restarting this timer */
									if (running_stopwatches + paused_stopwatches -
											restarted_stopwatches == 0)
									{
										Top_pane.set_disable_restart_stopwatches_timers_menu_item(true);
									}
								}
								
								assert restarted_timers <= paused_timers :
									"Incorrect time counters quantity value";
								
								/* If timer had been in numeric overflow state
								 * before it was restarted */
								if (numeric_overflow_status)
								{
									/* If restarted timer is the first one that
									 * can be started/resumed */
									if (paused_timers - (--overflowed_timers) == 1)
									{
										Top_pane.set_disable_start_timers_menu_item(false);
										
										/* If restarted timer is the first one
										 * among "Solo_counter" objects that can
										 * be started/resumed */
										if (paused_stopwatches -
												overflowed_stopwatches == 0)
										{
											Top_pane.set_disable_start_stopwatches_timers_menu_item(false);
										}
									}
									
									assert overflowed_timers >= 0 :
										"Incorrect time counters quantity value";
								}
							}
							finally
							{
								time_counters_quantity_lock.unlock();
							}
						}
						/////-/--------------------------------------------/////
					}
					
					time_counter_value.setTextFill(default_color);
				}
			});
			
			cast_to_Solo_counter.add_Numeric_overflow_listener(
					numeric_overflow_listener);
			
			start_button.setCursor(buttons_cursor);
			restart_button.setCursor(buttons_cursor);
			//--------------------------------------------------------------/////
			
			
			time_counter_unit.getChildren().addAll(description, mode_image,
					time_counter_value, start_button, restart_button,
					close_button, setting_button);
		}
		else
		{
			time_counter_unit.getChildren().addAll(description, mode_image,
					time_counter_value, close_button, setting_button);
		}
		//-----------------------------------------------------/////
		
		
		time_counters_pane.setPrefRows(time_counters_pane.getPrefRows() + 1);
		time_counters_pane_children_lock.lock();
		
		try
		{
			time_counters_pane_children.add(time_counter_unit);
		}
		finally
		{
			time_counters_pane_children_lock.unlock();
		}
	}
	
	
	/**
	 * <b>Warning!</b> It&nbsp;is <u>strictly prohibited</u> to perform next
	 * actions on the&nbsp;returned {@link TilePane}:
	 * <ul><li>add nodes;</li>
	 * <li>call {@link Node#setUserData(Object)} on
	 * the&nbsp;{@link Node}{@code s} contained in the&nbsp;returned pane;</li>
	 * <li>call {@link Node#setId(String)} on the&nbsp;returned pane and its
	 * children.</li>
	 * <li>add or remove returned pane {@link Node}{@code s} children;</li>
	 * <li>set <i>Start/Resume/Pause</i>&nbsp;buttons tooltips to {@code null}.</li></ul>
	 * Otherwise it may result in <i>runtime exception</i>.
	 * 
	 * @return Pane containing time&nbsp;counters controls.
	 */
	static TilePane get_time_counters_pane()
	{
		return time_counters_pane;
	}
	

	/**
	 * This class subscribes to {@code scene's} <i>focus owner property</i> to
	 * determine {@link Node} that has&nbsp;obtained focus.
	 * <p>When time&nbsp;counter time is elapsed, its representative
	 * node's background is set to <i>notification</i> color. It is set back to
	 * default background after clicking on it (or its children). After calling
	 * this method a&nbsp;keyboard navigation is listened too with the&nbsp;aim
	 * to set default background color.
	 * <p><b>Warning!</b> It is <u>strictly prohibited</u> to set
	 * <i>"time_counter"</i>&nbsp;id to any {@code scene's} node. Otherwise
	 * {@link ClassCastException} may be thrown.
	 * 
	 * @param scene Scene which contains {@link TilePane} returned by
	 * {@link #get_time_counters_pane()} method.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	static void apply_scene(final Scene scene)
	{
		scene.focusOwnerProperty().addListener(new ChangeListener<Node>()
		{
			@Override
			public void changed(final ObservableValue<? extends Node> observable,
					final Node oldValue, final Node newValue)
			{
				if (newValue == null)
				{
					return;
				}
				
				// Focus owner's parent node
				final Parent parent = newValue.getParent();
				
				/* If focused Node stands on HBox pane which encloses single
				 * time counter controls identifier */
				if (parent != null && time_counter_unit_id.equals(parent.getId()))
				{
					((HBox)parent).setBackground(default_background);
				}
			}
		});
	}
	

	/**
	 * Sorts existing {@link Time_counter}{@code s} according to their controls
	 * position in {@link #time_counters_pane} and writes them to file.
	 */
	static void sort_and_save_time_counters()
	{
		// Thread-safe snapshot of "time_counters_pane" children
		final CopyOnWriteArrayList<Node> snapshot =
				new CopyOnWriteArrayList<>(time_counters_pane_children);
		
		/* Assign index to each "Time_counter" represented by HBox
		 * panes with a view to sort them */
		for (int i = 0, end = snapshot.size(); i < end; ++i)
		{
			((Time_counter)snapshot.get(i).getUserData()).index_number = i;
		}
		
		Time_counter_control.get_instance().save_time_counters();
	}
	
	
	/**
	 * Starts/resumes all {@link Solo_counter} objects which are working in
	 * passed {@code modes}.
	 * <p><b>Warning!</b> Since {@link Instance_counter}
	 * does&nbsp;not&nbsp;support starting/resuming its time&nbsp;counter, it is
	 * <u>prohibited</u> to pass {@link Mode#M_elapsed_from} and
	 * {@link Mode#M_remains_till} enumeration constants as arguments, and may
	 * result in runtime exception.
	 * <p><i>Note.</i> Task is performed
	 * <u>not&nbsp;in&nbsp;FX&nbsp;application thread</u>.
	 * 
	 * @param modes {@link Time_counter} objects types which are needed to be
	 * started/resumed.
	 * 
	 * @exception ClassCastException {@link Mode#M_elapsed_from} and/or
	 * {@link Mode#M_remains_till} are passed. {@link Time_counter}{@code s},
	 * operating in these modes, <u>do&nbsp;not</u> support required by method
	 * operation.
	 */
	static void start_all(final Mode... modes)
	{
		start_pause_all(true, modes);
	}
	
	
	/**
	 * Pauses all {@link Solo_counter} objects which are working in passed
	 * {@code modes}.
	 * <p><b>Warning!</b> Since {@link Instance_counter}
	 * does&nbsp;not&nbsp;support pausing its time&nbsp;counter, it is
	 * <u>prohibited</u> to pass {@link Mode#M_elapsed_from} and
	 * {@link Mode#M_remains_till} enumeration constants as arguments, and may
	 * result in runtime exception.
	 * <p><i>Note.</i> Task is performed
	 * <u>not&nbsp;in&nbsp;FX&nbsp;application thread</u>.
	 * 
	 * @param modes {@link Time_counter} objects types which are needed to be
	 * started/resumed.
	 * 
	 * @exception ClassCastException {@link Mode#M_elapsed_from} and/or
	 * {@link Mode#M_remains_till} are passed. {@link Time_counter}{@code s},
	 * operating in these modes, <u>do&nbsp;not</u> support required by method
	 * operation.
	 */
	static void pause_all(final Mode... modes)
	{
		start_pause_all(false, modes);
	}
	
	
	/* There are no circumstances in which ClassCastException may be thrown
	 * in contrast to "start_pause_all()" method */
	/**
	 * Restarts all {@link Solo_counter} objects which are working in passed
	 * {@code modes}.
	 * <p><i>Notes.</i>
	 * <ul><li>Since {@link Instance_counter} does&nbsp;not&nbsp;support
	 * restarting its time&nbsp;counter, the&nbsp;method <u>does&nbsp;nothing</u>
	 * for passed {@link Mode#M_elapsed_from} and {@link Mode#M_remains_till}
	 * arguments.</li>
	 * <li>Method is executed <u>not&nbsp;in&nbsp;FX&nbsp;application thread</u>.</li></ul>
	 * 
	 * @param modes {@link Time_counter} objects types which are needed to be
	 * restarted.
	 */
	static void restart_all(final Mode... modes)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				get_time_counters_by_mode(modes).forEach(new Consumer<Node>()
				{
					@Override
					public void accept(final Node t)
					{
						// Time counter representative pane children nodes
						final ObservableList<Node> representative_controls =
								((HBox)t).getChildren();
						
						// Searching for Restart button
						for (final Node control : representative_controls)
						{
							// If Restart button is found
							if (restart_button_id.equals(control.getId()))
							{
								Platform.runLater(new Runnable()
								{
									@Override
									public void run()
									{
										((Button)control).fire();
									}
								});
								
								break;
							}
						}
					}
				});
			}
		}).start();
	}
	
	
	/**
	 * Closes all {@link Time_counter} objects which are working in passed
	 * {@code modes}.
	 * <p><i>Note.</i> Method is executed
	 * <u>not&nbsp;in&nbsp;FX&nbsp;application thread</u>.
	 * 
	 * @param modes {@link Time_counter} objects types which are needed to be
	 * closed.
	 * 
	 * @exception NullPointerException There is {@code null} passed among
	 * arguments.
	 */
	static void close_all(final Mode... modes)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				// All "Mode" enumeration constants
				final Mode[] mode_values = Mode.values();
				
				// Check whether need to close all time counters
				if (modes.length == mode_values.length)
				{
					/* Not 'null' value for the specified mode means that
					 * related time counters need to be closed */
					final EnumMap<Mode, Boolean> required_modes =
							new EnumMap<>(Mode.class);
					
//					// Initialize "required_modes" container
//					for (final Mode i : mode_values)
//					{
//						required_modes.put(i, false);
//					}
					
					// Search for time counters types that need to be closed
					for (final Mode i : modes)
					{
						required_modes.put(i, true);
					}
					
					/* 'true' - all time counters need to be removed */
					boolean need_to_remove_all = true;
					
					/* Check whether need to close all time counters. This
					 * happens when all "required_modes" container values are
					 * not 'null' */
					for (final Mode i : mode_values)
					{
						if (required_modes.get(i) == null)
						{
							need_to_remove_all = false;
							
							break;
						}
					}
					
					// If all time counters need to be removed
					if (need_to_remove_all)
					{
						try
						{
							time_counters_pane_children_lock.lockInterruptibly();
						}
						catch (final InterruptedException exc)
						{
							logger.log(
									Level.INFO,
									"Thread interrupts. Exception stack trace:",
									exc);
							Thread.currentThread().interrupt();
						}
						
						try
						{
							Platform.runLater(new Runnable()
							{
								@Override
								public void run()
								{
									time_counters_pane_children.clear();
								}
							});
							
							Time_counter_control.get_instance().get_time_counters().clear();
							
							///// Menu bar managing section ---------------/////
							time_counters_quantity_lock.lock();
							
							try
							{
								set_time_counters_quantity_to_0();
								disable_all_actions_menu_items();
							}
							finally
							{
								time_counters_quantity_lock.unlock();
							}
							/////------------------------------------------/////
						}
						finally
						{
							time_counters_pane_children_lock.unlock();
						}
						
						return;
					}
				}
				
				get_time_counters_by_mode(modes).forEach(new Consumer<Node>()
				{
					@Override
					public void accept(final Node t)
					{
						// Time counter representative pane children nodes
						final ObservableList<Node> representative_controls =
								((HBox)t).getChildren();
						
						// Searching for Close button
						for (final Node control : representative_controls)
						{
							// If Restart button is found
							if (close_button_id.equals(control.getId()))
							{
								Platform.runLater(new Runnable()
								{
									@Override
									public void run()
									{
										((Button)control).fire();
									}
								});
								
								break;
							}
						}
					}
				});
			}
		}).start();
	}
	
	
	//**************************************************************************
	//                                                                         *
	// Methods private static                                                  *
	//                                                                         *
	//**************************************************************************
	/**
	 * Auxiliary for {@link #start_all(Mode...)} and {@link #pause_all(Mode...)}
	 * methods.
	 * <p>Starts or pauses all {@link Solo_counter} objects which are
	 * working in passed {@code modes}, depending on {@code start} argument value.
	 * <p><b>Warning!</b> Since {@link Instance_counter}
	 * does&nbsp;not&nbsp;support starting/pausing its time&nbsp;counter, it is
	 * <u>prohibited</u> to pass {@link Mode#M_elapsed_from} and
	 * {@link Mode#M_remains_till} enumeration constants as arguments, and may
	 * result in runtime exception.
	 * <p><i>Note.</i> The&nbsp;method is executed
	 * <u>not&nbsp;in&nbsp;FX&nbsp;application thread</u>.
	 * 
	 * @param start {@code true} &#0151; the&nbsp;method starts all
	 * {@link Solo_counter}{@code s}; {@code false}&nbsp;&#0151; pauses all.
	 * 
	 * @param modes {@link Time_counter} objects types which are needed to be
	 * started or paused.
	 * 
	 * @exception ClassCastException {@link Mode#M_elapsed_from} and/or
	 * {@link Mode#M_remains_till} are passed. {@link Time_counter}{@code s},
	 * operating in these modes, <u>do&nbsp;not</u> support required by method
	 * operation.
	 */
	private static void start_pause_all(
			final boolean start, final Mode... modes)
	{
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				get_time_counters_by_mode(modes).forEach(new Consumer<Node>()
				{
					@Override
					public void accept(final Node t)
					{
						// Time counter represented by "t" controls pane
						final Time_counter time_counter =
								(Time_counter)t.getUserData();
						
						/* true - time counter is currently running;
						 * false - is paused */
						final boolean is_runnging =
								((Solo_counter)time_counter).is_running();
						
						// If need to start all AND time counter is paused
						if (start && !is_runnging)
						{
							fire_start_button((HBox)t);
						}
						// If need to pause all AND time counter is running
						else if (!start && is_runnging)
						{
							fire_start_button((HBox)t);
						}
					}
				});
			}
		}).start();
	}
	
	
	/**
	 * Auxiliary for {@link #start_pause_all(boolean, Mode...)},
	 * {@link #restart_all(Mode...)} and {@link #close_all(Mode...)} methods.
	 * <p>Filters {@link #time_counters_pane_children} to find
	 * {@link HBox}{@code es} that represent {@link Time_counter}{@code s}
	 * working in specified {@code modes}.
	 * 
	 * @param modes {@link Time_counter} objects types which need to be found.
	 * 
	 * @return {@link Time_counter} representatives ({@link HBox} panes).
	 */
	private static Stream<Node> get_time_counters_by_mode(final Mode... modes)
	{
		// Thread-safe snapshot of "time_counters_pane" children
		final CopyOnWriteArrayList<Node> snapshot =
				new CopyOnWriteArrayList<>(time_counters_pane_children);
		
		return snapshot.stream().filter(new Predicate<Node>()
		{
			@Override
			public boolean test(final Node t)
			{
				// Time counter represented by "t" controls pane
				final Time_counter time_counter = (Time_counter)t.getUserData();
				
				// Compare "time_counter" object mode with required
				for (int i = 0; i < modes.length; ++i)
				{
					// If required mode is found
					if (time_counter.instance_mode.equals(modes[i]))
					{
						return true;
					}
				}
				
				return false;
			}
		});
	}
	
	
	/**
	 * {@link #start_pause_all(boolean, Mode...)} method auxiliary.
	 * <p>Searches and
	 * fires <i>Start/Resume/Pause</i> button on the&nbsp;given
	 * {@code time_counter_representative} pane.
	 * 
	 * @param time_counter_representative Pane to fire <i>Start/Resume/Pause</i>
	 * button on.
	 * 
	 * @exception NullPointerException Passed argument is {@code null}.
	 */
	private static void fire_start_button(final HBox time_counter_representative)
	{
		// Time counter representative pane children nodes
		final ObservableList<Node> representative_controls =
				((HBox)time_counter_representative).getChildren();
		
		// Searching for Start/Resume/Pause button
		for (final Node control : representative_controls)
		{
			// If Start/Resume/Pause button is found
			if (start_button_id.equals(control.getId()))
			{
				Platform.runLater(new Runnable()
				{
					@Override
					public void run()
					{
						((Button)control).fire();
					}
				});
				
				break;
			}
		}
	}
	
	
	/**
	 * Auxiliary for {@link #add_time_counter_to_pane(Time_counter)} method and
	 * {@link EventHandler} for <i>Close</i> button
	 * ({@link Button#setOnAction(EventHandler)}).
	 * <p>Adds/subtracts one time&nbsp;counter in {@link #time_counters_quantity}
	 * container using {@code mode} key argument for mapping. Performs
	 * <i>menu&nbsp;items</i> from <i>menu&nbsp;bar</i> managing depending on
	 * changed values.
	 * 
	 * @param mode Key for {@link #time_counters_quantity} container to change
	 * value in.
	 * 
	 * @param subtract {@code true} &#0151; time&nbsp;counters quantity will be
	 * subtracted; {@code false}&nbsp;&#0151; added.
	 * 
	 * @exception NullPointerException {@code mode} argument is {@code null}.
	 */
	private static void change_time_counters_quantity(
			final Mode mode, final boolean subtract)
	{
		// Old value in the specified mapping
		final int old_value = time_counters_quantity.get(mode);
		
		assert old_value >= 0 : "Incorrect time counters quantity value";
		
		time_counters_quantity.put(
				mode,
				(subtract ? old_value - 1 : old_value + 1));
		
		/* If (the resulting value became 0 after subtraction) OR (the resulting
		 * value became non zero after addition) */
		if ((subtract && old_value == 1) || (!subtract && old_value == 0))
		{
			/* 'true' — need to check sum of all existing time counters working
			 * in different modes to determine whether the removed/added
			 * time counter was the last/first existing at all */
			boolean check_total = false;
			
			// Disable/enable related menu bar submenus
			switch (mode)
			{
			case M_stopwatch:
				Top_pane.set_disable_stopwatches_submenu(subtract);
				
				// If there are no existing timers
				if (time_counters_quantity.get(Mode.M_countdown) == 0)
				{
					Top_pane.set_disable_stopwatches_timers_submenu(
							subtract ? true : false);
					check_total = true;
				}
				
				break;
				
				
			case M_countdown:
				Top_pane.set_disable_timers_submenu(subtract);
				
				// If there are no existing stopwatches
				if (time_counters_quantity.get(Mode.M_stopwatch) == 0)
				{
					Top_pane.set_disable_stopwatches_timers_submenu(
							subtract ? true : false);
					check_total = true;
				}
				
				break;
				
				
			case M_elapsed_from:
				Top_pane.set_disable_elapsed_from_submenu(subtract);
				
				// If there are no existing "Remains till" time counters
				if (time_counters_quantity.get(Mode.M_remains_till) == 0)
				{
					Top_pane.set_disable_elapsed_from_remains_till_submenu(
							subtract ? true : false);
					check_total = true;
				}
				
				break;
				
				
			case M_remains_till:
				Top_pane.set_disable_remains_till_submenu(subtract);
				
				// If there are no existing "Elapsed from" time counters
				if (time_counters_quantity.get(Mode.M_elapsed_from) == 0)
				{
					Top_pane.set_disable_elapsed_from_remains_till_submenu(
							subtract ? true : false);
					check_total = true;
				}
				
				break;
			
			
			default:
				throw new EnumConstantNotPresentException(Mode.class, mode.name());
			}
			
			
			/* If need to determine whether the removed/added time counter was
			 * the last/first existing at all */
			if (check_total)
			{
				// All existing time counters working in different modes sum
				int all_time_counters_quantity = 0;
				
				// Finding all time counters sum
				for (final Mode i : Mode.values())
				{
					all_time_counters_quantity += time_counters_quantity.get(i);
				}
				
				// If (the resulting value became 0 after subtraction) OR ...
				if ((subtract && all_time_counters_quantity == 0) ||
						// ... (the resulting value became non zero after addition)
						(!subtract && all_time_counters_quantity == 1))
				{
					Top_pane.set_disable_close_all_time_counters_menu_item(subtract);
					Top_pane.set_disable_save_menu_item(subtract);
				}
			}
		}
	}
	
	
	/**
	 * Sets {@code 0}&nbsp;value to all fields responsible for managing
	 * <i>Actions</i> menu&nbsp;items from menu&nbsp;bar:
	 * <ul><li>all {@link #time_counters_quantity} container values;</li>
	 * <li>{@link #running_stopwatches};</li>
	 * <li>{@link #paused_stopwatches};</li>
	 * <li>{@link #restarted_stopwatches};</li>
	 * <li>{@link #overflowed_stopwatches};</li>
	 * <li>{@link #running_timers};</li>
	 * <li>{@link #paused_timers};</li>
	 * <li>{@link #restarted_timers};</li>
	 * <li>{@link #overflowed_timers}.</li></ul>
	 */
	private static void set_time_counters_quantity_to_0()
	{
		for (final Mode i : Mode.values())
		{
			time_counters_quantity.put(i, 0);
		}
		
		running_stopwatches = 0;
		paused_stopwatches = 0;
		restarted_stopwatches = 0;
		overflowed_stopwatches = 0;
		running_timers = 0;
		paused_timers = 0;
		restarted_timers = 0;
		overflowed_timers = 0;
	}
	
	
	/**
	 * Disables all <i>Actions</i> menu&nbsp;items from menu&nbsp;bar and
	 * possibility to save time&nbsp;counters to file, using methods provided by
	 * {@link Top_pane} class.
	 */
	private static void disable_all_actions_menu_items()
	{
		Top_pane.set_disable_save_menu_item(true);
		
		Top_pane.set_disable_stopwatches_submenu(true);
		Top_pane.set_disable_start_stopwatches_menu_item(true);
		Top_pane.set_disable_pause_stopwatches_menu_item(true);
		Top_pane.set_disable_restart_stopwatches_menu_item(true);
		
		Top_pane.set_disable_timers_submenu(true);
		Top_pane.set_disable_start_timers_menu_item(true);
		Top_pane.set_disable_pause_timers_menu_item(true);
		Top_pane.set_disable_restart_timers_menu_item(true);
		
		Top_pane.set_disable_stopwatches_timers_submenu(true);
		Top_pane.set_disable_start_stopwatches_timers_menu_item(true);
		Top_pane.set_disable_pause_stopwatches_timers_menu_item(true);
		Top_pane.set_disable_restart_stopwatches_timers_menu_item(true);
		
		Top_pane.set_disable_elapsed_from_submenu(true);
		Top_pane.set_disable_remains_till_submenu(true);
		Top_pane.set_disable_elapsed_from_remains_till_submenu(true);
		
		Top_pane.set_disable_close_all_time_counters_menu_item(true);
	}
}
