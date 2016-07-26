package main_package.FXML_controllers;

import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import main_package.GUI_settings;
import main_package.events.Locale_change_listener;
import time_obj.Mode;


/**
 * Root pane for <i>new&nbsp;time&nbsp;counter wizard</i> window. Called by
 * {@link FXMLLoader} for
 * <i>"main_package/FXML_controllers/Basic_init_layout.fxml"</i> file.<br>
 * <i>Notes.</i>
 * <ul><li>Root pane in <i>"Basic_init_layout.fxml"</i> is {@link BorderPane}.</li>
 * <li><i>"<i>Basic_init_layout.fxml</i>"</i> requires
 * <i>"main_package/resources/GUI_elements/buttons.properties"</i> resources to
 * be set.</li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Basic_init_controller
{
	///// Поля public экземпляра ==========================================/////
	/** Изображение режима счетчика времени согласно именованным константам
	 * перечисления {@link Mode}. */
	@FXML
	public ImageView mode_image;
	
	/** Описание назначения панели компоновки. */
	@FXML
	public Label layout_description;
	
	/** Кнопка перехода на предыдущую компоновку окна. */
	@FXML
	public Button previous_button;
	
	/** Кнопка перехода на следующую компоновку окна. */
	@FXML
	public Button next_button;
	
	/** Кнопка подтверждения создания счетчика времени. */
	@FXML
	public Button apply_button;
	
	/** Кнопка отмены создания счетчика времени. */
	@FXML
	public Button cancel_button;
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * Called by {@link FXMLLoader}.
	 */
	@FXML
	private void initialize()
	{
		assert mode_image != null : "mode_image field was not injected";
		assert layout_description != null : "layout_description field was not injected";
		assert previous_button != null : "previous_button field was not injected";
		assert next_button != null : "next_button field was not injected";
		assert apply_button != null : "apply_button field was not injected";
		assert cancel_button != null : "cancel_button field was not injected";
		
		// Graphic user interface settings
		final GUI_settings gui_settings = GUI_settings.get_instance();
		
		gui_settings.add_Locale_change_listener(new Locale_change_listener()
		{
			@Override
			public void locale_changed()
			{
				/* Resource bundle representing ".properties" resource which
				 * contains buttons names */
				final ResourceBundle resources =
						gui_settings.get_buttons_resources();
				
				previous_button.setText(resources.getString("previous"));
				next_button.setText(resources.getString("next"));
				apply_button.setText(resources.getString("apply"));
				cancel_button.setText(resources.getString("cancel"));
			}
		});
	}
}
