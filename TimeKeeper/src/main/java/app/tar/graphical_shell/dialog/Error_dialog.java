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
package app.tar.graphical_shell.dialog;

import java.util.ResourceBundle;

import app.tar.graphical_shell.GUI_settings;
import app.tar.graphical_shell.events.Locale_change_listener;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


/**
 * Provides error dialog window.
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
	
	
	/**
	 * Error types, according to which title&nbsp;text will be provided.
	 * 
	 * @version 1.0
	 */
	public enum Error_type
	{
		/** Input-output error. */
		ET_IO,
		/** Deserialized file contains incorrect values. */
		ET_file_content_error,
		/** Error related to time&nbsp;zones. */
		ET_time_zone_error
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
	/**
	 * Выводит шаблонный текст сообщения об&nbsp;ошибке.
	 * 
	 * @param message Тип текста сообщения об&nbsp;ошибке.
	 * 
	 * @exception NullPointerException В&nbsp;качестве аргумента передан
	 * {@code null}.
	 */
	public static void show_message(final Template_message message)
	{
		switch (message)
		{
		case TM_layout_build:
			show_message(
					Error_type.ET_IO,
					messages_resources.getString("error.window_layout"));
			
			break;
			
		default:
			throw new EnumConstantNotPresentException(
					Template_message.class, message.name());
		}
	}
	
	
	/**
	 * Shows error dialog window.
	 * 
	 * @param error_type Error type, according to which title&nbsp;text
	 * will&nbsp;be displayed. <u>Can</u> be {@code null}. In this case
	 * no&nbsp;title will&nbsp;be displayed.
	 * 
	 * @param message_text Error message text.
	 */
	public static void show_message(
			final Error_type error_type, final String message_text)
	{
		// Диалоговое окно сообщения об ошибке
		final Alert error_dialog = new Alert(AlertType.ERROR);
		
		// If need to provide dialog window title text
		if (error_type != null)
		{
			switch (error_type)
			{
			case ET_IO:
				error_dialog.setTitle(
						messages_resources.getString("error.title.IO"));
				
				break;
				
			case ET_file_content_error:
				error_dialog.setTitle(
						messages_resources.getString("error.title.file_content"));
				
				break;
				
			case ET_time_zone_error:
				error_dialog.setTitle(
						messages_resources.getString("error.title.time_zone"));
				
				break;
			
			default:
				throw new EnumConstantNotPresentException(
						Error_type.class, error_type.name());
			}
		}
		// Dialog window title text is not needed
		else
		{
			error_dialog.setTitle(null);
		}
		
		error_dialog.setHeaderText(null);
		error_dialog.setContentText(message_text);
		error_dialog.showAndWait();
		/* TODO: Send turn off alarm sound command after implementing class
		 * responsible for sound notification */
	}
}
