package main_package.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


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
		/**
		 * Выводится сообщение об&nbsp;ошибке построения компонента окна
		 * создания нового счетчика времени.
		 */
		TM_layout_build
	}
	
	
	///// Методы public статические =======================================/////
	// TODO: ? Опримизировать
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
			show_IO_error_message("Error occurred during creating new time"
					+ " counter window layout component.");
			
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
		
		error_dialog.setTitle("IO Error");
		error_dialog.setHeaderText(null);
		error_dialog.setContentText(message_text);
		error_dialog.showAndWait();
	}
}