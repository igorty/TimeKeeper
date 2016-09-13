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
package app.tar.graphical_shell.events;

import java.util.EventListener;
import java.util.ResourceBundle;

import app.tar.graphical_shell.GUI_settings;


/**
 * The&nbsp;listener interface for receiving <i>program locale change event</i>.<br>
 * The&nbsp;class that is interested in processing the&nbsp;event implements
 * this interface, and the&nbsp;object created with that class is subscribed for
 * the&nbsp;event using
 * {@link GUI_settings#add_Locale_change_listener(Locale_change_listener)}
 * method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public interface Locale_change_listener extends EventListener
{
	/**
	 * Invoked when program locale has&nbsp;changed.<br>
	 * Listener should&nbsp;call one of
	 * get&#8209;specified&#8209;{@link ResourceBundle} methods in
	 * {@link GUI_settings} class to receive new resources.
	 */
	void locale_changed();
}
