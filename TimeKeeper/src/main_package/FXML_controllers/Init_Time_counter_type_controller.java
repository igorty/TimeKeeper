package main_package.FXML_controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import main_package.Main_class;
import time_obj.Mode;


/**
 * Контроллер участка компоновки для выбора типа создаваемого счетчика времени
 * на основе именованных констант перечисления {@link Mode}. Вызывается
 * {@link FXMLLoader}'ом для файла <i>Init_Time_counter_type_layout.fxml</i>.<br>
 * <i>Примечание.</i> Корневой компоновкой для файла
 * <i>Init_Time_counter_type_layout.fxml</i> является {@link VBox}.
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
	
	
	///// Методы private экземпляра =======================================/////
	/** Вызывается {@link FXMLLoader}'ом. */
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
		
		stopwatch_radio_button.setText(Main_class.mode_names.get(Mode.M_stopwatch));
		countdown_radio_button.setText(Main_class.mode_names.get(Mode.M_countdown));
		elapsed_from_radio_button.setText(Main_class.mode_names.get(Mode.M_elapsed_from));
		countdown_till_radio_button.setText(Main_class.mode_names.get(Mode.M_countdown_till));
		
		toggle_group.selectedToggleProperty().addListener(new ChangeListener<Toggle>()
		{
			@Override
			public void changed(final ObservableValue<? extends Toggle> observable,
					final Toggle old_value, final Toggle new_value)
			{
				selected_mode = (Mode)new_value.getUserData();
			}
		});
	}
}