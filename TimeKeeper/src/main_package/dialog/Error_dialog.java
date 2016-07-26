package main_package.dialog;

import java.util.ResourceBundle;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import main_package.GUI_settings;
import main_package.events.Locale_change_listener;


/**
 * Содержит статические методы вывода диалогового окна с сообщением
 * об&nbsp;ошибке ввода&#8209;вывода.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Error_dialog
{
	///// Перечисления public =============================================/////
	/**
	 * Шаблонные тексты сообщений диалоговых окон об&nbsp;ошибке.
	 * 
	 * @version 1.0
	 * @author Igor Taranenko
	 */
	public enum Template_message
	{
		/** Window layout component&nbsp;build error&nbsp;message is shown. */
		TM_layout_build
	}
	
	
	///// Fields private static ===========================================/////
	/** Graphic user interface settings. */
	private static final GUI_settings gui_settings;
	
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * dialog messages. */
	private static ResourceBundle messages_resources;
	
	
	static
	{
		gui_settings = GUI_settings.get_instance();
		messages_resources = gui_settings.get_messages_resources();
		
		gui_settings.add_Locale_change_listener(new Locale_change_listener()
		{
			@Override
			public void locale_changed()
			{
				messages_resources = gui_settings.get_messages_resources();
			}
		});
	}
	
	
	///// Методы public статические =======================================/////
	// TODO? Optimize
	/**
	 * Выводит шаблонный текст сообщения об&nbsp;ошибке.
	 * 
	 * @param message Тип текста сообщения об&nbsp;ошибке.
	 * 
	 * @exception NullPointerException В&nbsp;качестве аргумента передан
	 * {@code null}.
	 */
	public static void show_IO_error_message(final Template_message message)
	{
		// Аргумент не должен быть null
		if (message == null)
		{
			throw new NullPointerException(
					Template_message.class.getName() + " argument is null");
		}
		
		switch (message)
		{
		case TM_layout_build:
			show_IO_error_message(
					messages_resources.getString("error.window_layout"));
			
			break;
			
		default:
			throw new EnumConstantNotPresentException(
					Template_message.class, message.name());
		}
	}
	
	
	/**
	 * Выводит диалоговое окно с сообщением об&nbsp;ошибке ввода&#8209;вывода.
	 * 
	 * @param message_text Текст сообщения об&nbsp;ошибке.
	 */
	public static void show_IO_error_message(final String message_text)
	{
		// Диалоговое окно сообщения об ошибке
		final Alert error_dialog = new Alert(AlertType.ERROR);
		
		error_dialog.setTitle(messages_resources.getString("error.title.IO"));
		error_dialog.setHeaderText(null);
		error_dialog.setContentText(message_text);
		error_dialog.showAndWait();
	}
}
