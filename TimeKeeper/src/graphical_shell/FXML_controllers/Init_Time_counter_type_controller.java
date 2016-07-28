package graphical_shell.FXML_controllers;

import java.util.ResourceBundle;

import graphical_shell.GUI_settings;
import graphical_shell.events.Locale_change_listener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import time_obj.Mode;


/**
 * Time counter type choosing layout&nbsp;pane controller. Called by
 * {@link FXMLLoader} for
 * <i>"graphical_shell/FXML_controllers/Init_Time_counter_type_layout.fxml"</i>
 * file.<br>
 * <i>Notes.</i>
 * <ul><li>Root pane in <i>"Init_Time_counter_type_layout.fxml"</i> is
 * {@link VBox}.</li>
 * <li><i>"Init_Time_counter_type_layout.fxml"</i> requires
 * <i>"graphical_shell/resources/time_counter.properties"</i> resources to be set.</li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Init_Time_counter_type_controller
{
	///// Поля private экземпляра =========================================/////
	/** Обозначает выбор режима {@link Mode#M_stopwatch}. */
	@FXML
	private RadioButton stopwatch_radio_button;
	
	/** Обозначает выбор режима {@link Mode#M_countdown}. */
	@FXML
	private RadioButton countdown_radio_button;
	
	/** Обозначает выбор режима {@link Mode#M_elapsed_from}. */
	@FXML
	private RadioButton elapsed_from_radio_button;
	
	/** Обозначает выбор режима {@link Mode#M_countdown_till}. */
	@FXML
	private RadioButton countdown_till_radio_button;

	/** Переключатель группы радиокнопок {@link #stopwatch_radio_button},
	 * {@link #countdown_radio_button}, {@link #elapsed_from_radio_button} и
	 * {@link #countdown_till_radio_button}. */
	@FXML
	private ToggleGroup toggle_group;
	
	/** Выбранный на текущий момент режим, определяющийся выбранной радиокнопкой. */
	private Mode selected_mode;
	
	
	///// Нестатическая инициализация =====================================/////
	{
		selected_mode = Mode.M_stopwatch;
	}
	
	
	///// Методы public экземпляра ========================================/////
	/**
	 * @return Выбранный на&nbsp;текущий момент режим работы счетчика времени
	 * в&nbsp;соответствии с активной радиокнопкой.
	 */
	public Mode get_selected_mode()
	{
		return selected_mode;
	}
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * Called by {@link FXMLLoader}.
	 */
	@FXML
	private void initialize()
	{
		assert stopwatch_radio_button != null : "stopwatch_radio_button was not injected";
		assert countdown_radio_button != null : "countdown_radio_button was not injected";
		assert elapsed_from_radio_button != null : "elapsed_from_radio_button was not injected";
		assert countdown_till_radio_button != null : "countdown_till_radio_button was not injected";
		assert toggle_group != null : "toggle_group was not injected";
		
		stopwatch_radio_button.setUserData(Mode.M_stopwatch);
		countdown_radio_button.setUserData(Mode.M_countdown);
		elapsed_from_radio_button.setUserData(Mode.M_elapsed_from);
		countdown_till_radio_button.setUserData(Mode.M_countdown_till);
		
		stopwatch_radio_button.setGraphic(new ImageView("images/stopwatch_middle.png"));
		countdown_radio_button.setGraphic(new ImageView("images/countdown_middle.png"));
		elapsed_from_radio_button.setGraphic(new ImageView("images/elapsed_from_middle.png"));
		countdown_till_radio_button.setGraphic(new ImageView("images/countdown_till_middle.png"));
		
		toggle_group.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
		{
			@Override
			public void changed(final ObservableValue<? extends Toggle> observable,
					final Toggle old_value, final Toggle new_value)
			{
				selected_mode = (Mode)new_value.getUserData();
			}
		});
		
		// Graphic user interface settings
		final GUI_settings gui_settings = GUI_settings.get_instance();
		
		gui_settings.add_Locale_change_listener(new Locale_change_listener()
		{
			@Override
			public void locale_changed()
			{
				/* Resource bundle representing ".properties" resource which
				 * contains specific time counter settings names */
				final ResourceBundle resources =
						gui_settings.get_time_counter_resources();
				
				stopwatch_radio_button.setText(resources.getString("modes.stopwatch"));
				countdown_radio_button.setText("modes.timer");
				elapsed_from_radio_button.setText("modes.elapsed_from");
				countdown_till_radio_button.setText("modes.remains_till");
			}
		});
	}
}
