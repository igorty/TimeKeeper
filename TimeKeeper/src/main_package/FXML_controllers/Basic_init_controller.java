package main_package.FXML_controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import time_obj.Mode;


/**
 * Контроллер базовой компоновки для окна создания счетчика времени. Вызывается
 * {@link FXMLLoader}'ом для файла <i>Basic_init_layout.fxml</i>.<br>
 * <i>Примечание.</i> Корневой компоновкой для файла <i>Basic_init_layout.fxml</i>
 * является {@link BorderPane}.
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
	
	
	///// Методы private экземпляра =======================================/////
	/**
	 * Вызывается {@link FXMLLoader}'ом.
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
	}
}
