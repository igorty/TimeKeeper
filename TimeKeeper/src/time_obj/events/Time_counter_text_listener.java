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
package time_obj.events;

import java.util.EventListener;

import time_obj.Time_counter;
import time_obj.Time_counter.Time_counter_text_value;


/**
 * The&nbsp;listener interface for receiving time&nbsp;counter's time&nbsp;value
 * text change event. The&nbsp;class that is interested in processing
 * the&nbsp;event implements this interface, and the&nbsp;object created with
 * that class is subscribed for the&nbsp;event using
 * {@link Time_counter#add_Time_counter_text_listener(Time_counter_text_listener)}
 * method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public interface Time_counter_text_listener extends EventListener
{
	/**
	 * Invoked when time&nbsp;counter's time&nbsp;value text changes.
	 * 
	 * @param event {@link Time_counter} object which has generated
	 * the&nbsp;event.
	 * 
	 * @param new_value Represents new time&nbsp;value.
	 */
	void time_counter_text_changed(
			Time_counter_event<?> event, Time_counter_text_value new_value);
}
