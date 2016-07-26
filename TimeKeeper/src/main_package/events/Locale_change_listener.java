package main_package.events;

import java.util.EventListener;
import java.util.ResourceBundle;

import main_package.GUI_settings;


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
