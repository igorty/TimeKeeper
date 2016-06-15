package time_obj.dialog;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


/**
 * Содержит статический метод вывода диалогового окна с сообщением
 * об&nbsp;ошибке записи/чтения файла.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Read_write_dialog
{
	///// Методы public статические =======================================/////
	/**
	 * Вывод диалогового окна с сообщением об&nbsp;ошибке записи/чтения.<br>
	 * <b>Важно!</b> Содержит элементы JavaFX. Вызов перед запуском компонентов
	 * JavaFX приведет к ошибке времени выполнения.
	 * 
	 * @param is_read_error {@code true}&nbsp;&#0151; в заголовке диалогового
	 * окна будет указано, что произошла ошибка чтения;
	 * {@code false}&nbsp;&#0151; ошибка записи.
	 * 
	 * @param message Текст сообщения об&nbsp;ошибке.
	 */
	public static void show_error_message(
			final boolean is_read_error, final String message)
	{
		// Текст заголовка сообщения
		final String title_text = (is_read_error ? "Read error" : "Write error");
		
		Platform.runLater(new Runnable()
		{
			@Override
			public void run()
			{
				// Диалоговое окно сообщения об ошибке
				final Alert error_dialog = new Alert(AlertType.ERROR);
				
				error_dialog.setTitle(title_text);
				error_dialog.setHeaderText(null);
				error_dialog.setContentText(message);
				error_dialog.showAndWait();
			}
		});
	}
}
