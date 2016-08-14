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

import time_obj.Solo_counter;


/**
 * The&nbsp;listener interface for receiving numeric overflow event from
 * {@link Solo_counter} object. The&nbsp;class that is interested in processing
 * the&nbsp;event implements this interface, and the&nbsp;object created with
 * that class is subscribed for the&nbsp;event using
 * {@link Solo_counter#add_Numeric_overflow_listener(Numeric_overflow_listener)}
 * method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public interface Numeric_overflow_listener extends EventListener
{
	/**
	 * Invoked when numeric overflow occurs.
	 * 
	 * @param event {@link Solo_counter} object which has generated
	 * the&nbsp;event.
	 */
	void numeric_overflow_occurred(Numeric_overflow_event event);
}
