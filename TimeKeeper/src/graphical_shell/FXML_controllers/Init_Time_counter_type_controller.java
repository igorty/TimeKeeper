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
 * <i>"graphical_shell/resources/languages/time_counter.properties"</i>
 * resources to be set.</li></ul>
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
	
	/** Means choosing {@link Mode#M_remains_till} mode. */
	@FXML
	private RadioButton remains_till_radio_button;

	/** Toggle group for {@link #stopwatch_radio_button},
	 * {@link #countdown_radio_button}, {@link #elapsed_from_radio_button} and
	 * {@link #remains_till_radio_button} radio&nbsp;buttons. */
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
		assert remains_till_radio_button != null : "remains_till_radio_button was not injected";
		assert toggle_group != null : "toggle_group was not injected";
		
		stopwatch_radio_button.setUserData(Mode.M_stopwatch);
		countdown_radio_button.setUserData(Mode.M_countdown);
		elapsed_from_radio_button.setUserData(Mode.M_elapsed_from);
		remains_till_radio_button.setUserData(Mode.M_remains_till);

		// Radio buttons images size
		final double images_size = 32;
		// "stopwatch_radio_button" image
		final ImageView stopwatch_image = new ImageView(
				Init_Time_counter_type_controller.class.getResource(
						"/graphical_shell/resources/images/stopwatch.png").toString());
		
		stopwatch_image.setFitWidth(images_size);
		stopwatch_image.setPreserveRatio(true);
		stopwatch_radio_button.setGraphic(stopwatch_image);
		
		// "countdown_radio_button" image
		final ImageView countdown_image = new ImageView(
				Init_Time_counter_type_controller.class.getResource(
						"/graphical_shell/resources/images/countdown.png").toString());
		
		countdown_image.setFitWidth(images_size);
		countdown_image.setPreserveRatio(true);
		countdown_radio_button.setGraphic(countdown_image);
		
		// "elapsed_from_radio_button" image
		final ImageView elapsed_from_image = new ImageView(
				Init_Time_counter_type_controller.class.getResource(
						"/graphical_shell/resources/images/elapsed_from.png").toString());
		
		elapsed_from_image.setFitWidth(images_size);
		elapsed_from_image.setPreserveRatio(true);
		elapsed_from_radio_button.setGraphic(elapsed_from_image);
		
		// "remains_till_radio_button" image
		final ImageView remains_till_image = new ImageView(
				Init_Time_counter_type_controller.class.getResource(
						"/graphical_shell/resources/images/remains_till.png").toString());
		
		remains_till_image.setFitWidth(images_size);
		remains_till_image.setPreserveRatio(true);
		remains_till_radio_button.setGraphic(remains_till_image);
		
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
				
				stopwatch_radio_button.setText(
						resources.getString("modes.stopwatch"));
				countdown_radio_button.setText(
						resources.getString("modes.timer"));
				elapsed_from_radio_button.setText(
						resources.getString("modes.elapsed_from"));
				remains_till_radio_button.setText(
						resources.getString("modes.remains_till"));
			}
		});
	}
}
