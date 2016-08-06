package graphical_shell;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import graphical_shell.events.Locale_change_listener;
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
import time_obj.Instance_counter;
import time_obj.Mode;
import time_obj.Settings;
import time_obj.Settings.Locale_setting;
import time_obj.Solo_counter;
import time_obj.Time_counter;
import time_obj.Time_counter.Time_counter_text_value;
import time_obj.Time_counter_control;
import time_obj.Solo_counter.Initial_time_values;
import time_obj.containers.Modified_ArrayList;
import time_obj.events.Numeric_overflow_event;
import time_obj.events.Numeric_overflow_listener;
import time_obj.events.Time_counter_event;
import time_obj.events.Time_counter_text_listener;
import time_obj.events.Time_elapsed_listener;


/**
 * Manages <i>time&nbsp;counters</i>.<br>
 * Each time&nbsp;counter is stored in {@link HBox}, and all these {@code HBoxes}
 * are contained in a&nbsp;vertical {@link TilePane}. Each time&nbsp;counter is
 * provided with control elements ({@link TextField} to write
 * time&nbsp;counter's description and {@link Button}{@code s} to operate
 * the&nbsp;time&nbsp;counter).<br>
 * Time counters can be dragged with the&nbsp;aim to:
 * <ul><li>reorder them in user&#8209;convenient way;</li>
 * <li>copy time&nbsp;counter value to any text&nbsp;field.</li></ul>
 * 
 * The class is also provides time&nbsp;counters reordering by dragging them
 * into the&nbsp;convenient place within the&nbsp;{@code TilePane}.
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
	
	/* TODO: Provide resize when parent node width changed (after placing to
	 * parent node) */
	/* TODO: Provide the pane's scene listener to set default background for
	 * the pane */
	/** Contains {@link HBox}{@code es}. Each {@code HBox} has single
	 * <i>time&nbsp;counter</i> with related controls. */
	private static final TilePane time_counters_pane;
	
	/** {@link #time_counters_pane} children nodes. */
	private static final ObservableList<Node> time_counters_pane_children;
	
	/** Synchronizes access to {@link #time_counters_pane_children} container. */
	private static final ReentrantLock lock;
	
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
	
	/** {@link Node} identifier for <i>Start/Resume/Pause</i> {@link Button}
	 * contained in {@link HBox}{@code es} representing {@link Solo_counter}
	 * time&nbsp;counters. */
	private static final String start_button_id;
	
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
		lock = new ReentrantLock();
		time_counters_pane_children = time_counters_pane.getChildren();
		
		///// "mode_images" container initialization /////
		// "mode_images" container images
		final Image[] mode_images_values = {
				new Image(Time_counters_pane.class.getResource(
						"resources/images/stopwatch.png").toString()),
				new Image(Time_counters_pane.class.getResource(
						"resources/images/countdown.png").toString()),
				new Image(Time_counters_pane.class.getResource(
						"resources/images/elapsed_from.png").toString()),
				new Image(Time_counters_pane.class.getResource(
						"resources/images/remains_till.png").toString()) };
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
				"resources/images/start.png").toString());
		resume_image = new Image(Time_counters_pane.class.getResource(
				"resources/images/resume.png").toString());
		pause_image = new Image(Time_counters_pane.class.getResource(
				"resources/images/pause.png").toString());
		restart_image = new Image(Time_counters_pane.class.getResource(
				"resources/images/restart.png").toString());
		close_image = new Image(Time_counters_pane.class.getResource(
				"resources/images/close.png").toString());
		setting_image = new Image(Time_counters_pane.class.getResource(
				"resources/images/settings.png").toString());
		
		images_size = 32;
		
		default_color = Color.BLACK;
		time_elapsed_color = Color.RED;
		default_disabled_color = Color.GRAY;
		time_elapsed_disabled_color = Color.PALEVIOLETRED;
		notify_background =
				new Background(new BackgroundFill(Color.YELLOW, null, null));
		
		start_button_id = "start_button";
		
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
					lock.lockInterruptibly();
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
					lock.unlock();
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
					lock.lockInterruptibly();
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
								final String id = control.getId();
								
								if (id != null && id.equals(start_button_id))
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
									
									++actions_performed_counter;
								}
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
					lock.unlock();
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
					lock.lockInterruptibly();
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
				}
				finally
				{
					lock.unlock();
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
		
		/* "time_counter_unit" pane background color in normal state */
		final Background time_counter_unit_default_background =
				time_counter_unit.getBackground();
		
		time_counter_unit.addEventFilter(MouseEvent.MOUSE_CLICKED,
				new EventHandler<MouseEvent>()
		{
			@Override
			public void handle(final MouseEvent event)
			{
				/* Restore default node background in case of it was changed to
				 * notify user and turn off alarm sound */
				time_counter_unit.setBackground(
						time_counter_unit_default_background);
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
				
				time_counters_pane_children.remove(dropped_object);
				
				// Time counter controls pane on which other pane is dropped
				final int time_counter_unit_index =
						time_counters_pane_children.indexOf(time_counter_unit);
				
				assert dropped_object_index != -1 || time_counter_unit_index != -1 :
					"No\u00A0such time\u00A0counter controls pane(\u2011s) in "
						+ ObservableList.class.getName()
						+ " \"time_counters_pane\"";
				
				time_counters_pane_children.add(
						dropped_object_index <= time_counter_unit_index ?
								time_counter_unit_index + 1 : time_counter_unit_index,
						dropped_object);
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
		final Cursor buttons_cursor = Cursor.HAND;
		
		///// "Close time counter" button implementation /////
		// Close time counter button
		final Button close_button = new Button(null, new ImageView(close_image));

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
				
				time_counters_pane_children.remove(time_counter_unit);
				Time_counter_control.get_instance().get_time_counters().remove(time_counter);
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
			restart_button.setGraphic(new ImageView(restart_image));
			restart_button.setTooltip(new Tooltip(
					hints_resources.getString("restart_button_tooltip")));
			
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
					
					// If time counter value is negative
					if (!time_value.is_positive)
					{
						time_counter_value.setTextFill(time_elapsed_color);
					}
				}
				else
				{
					start_button.setGraphic(new ImageView(resume_image));
					start_button.setTooltip(new Tooltip(
							hints_resources.getString(
									"start_button_tooltips.resume")));
					
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
					}
					// Need to start time counter
					else
					{
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
					
					cast_to_Solo_counter.restart();
					
					// If time counter is in paused state
					if (!cast_to_Solo_counter.is_running())
					{
						restart_button.setDisable(true);
						start_button.setGraphic(new ImageView(start_image));
						start_button.getTooltip().setText(
								hints_resources.getString(
										"start_button_tooltips.start"));
					}
					
					time_counter_value.setTextFill(default_color);
				}
			});
			
			// If time counter is in numeric overflow status
			if (cast_to_Solo_counter.numeric_overflow_status())
			{
				start_button.setDisable(true);
				start_button.setGraphic(new ImageView(start_image));
				time_counter_value.setTextFill(time_elapsed_color);
			}
			
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
		
		
		try
		{
			lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_counters_pane_children.add(time_counter_unit);
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/**
	 * <b>Warning!</b> It&nbsp;is <u>strictly prohibited</u> to perform next
	 * actions on the&nbsp;returned {@link TilePane}:
	 * <ul><li>add nodes;</li>
	 * <li>call {@link Node#setUserData(Object)} on
	 * the&nbsp;{@link Node}{@code s} contained in the&nbsp;returned pane;</li>
	 * <li>call {@link Node#setId(String)} on the&nbsp;returned pane
	 * {@link Node}{@code s} children.</li>
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
}
