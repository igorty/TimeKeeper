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
package graphical_shell.dialog;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;


/**
 * Provides notification dialog window.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Notification_dialog
{
	//**************************************************************************
	//                                                                         *
	// Methods public static                                                   *
	//                                                                         *
	//**************************************************************************
	/**
	 * Shows information dialog window.
	 * 
	 * @param message_text Information message text.
	 */
	public static void show_message(final String message_text)
	{
		// Information dialog window
		final Alert info_dialog = new Alert(AlertType.INFORMATION);
		
		info_dialog.setTitle(null);
		info_dialog.setHeaderText(null);
		info_dialog.setContentText(message_text);
		info_dialog.showAndWait();
		/* TODO: Send turn off alarm sound command after implementing class
		 * responsible for sound notification */
	}
}
