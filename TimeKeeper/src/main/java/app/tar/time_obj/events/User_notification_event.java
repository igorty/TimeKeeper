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
package app.tar.time_obj.events;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EventObject;


/**
 * Event object representing notification event. {@link app.tar.time_obj} classes use
 * it to notify listener, which implements {@link User_notification_listener},
 * about event occurred. This event class is provided with a&nbsp;view to
 * implement user informing about important program events via GUI.<br>
 * <i>Note.</i> <u>Does&nbsp;not</u> support serialization.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
@SuppressWarnings("serial")
public class User_notification_event extends EventObject
{
	///// Constructors public =============================================/////
	/**
	 * @param source The&nbsp;object on which <i>user notification event</i>
	 * initially occurred.
	 * 
	 * @exception NullPointerException {@code source} argument is {@code null}.
	 */
	public User_notification_event(final Object source)
	{
		super(source);
	}
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * This class <u>does&nbsp;not</u> support serialization.
	 * 
	 * @exception NotSerializableException When this method called.
	 */
	@SuppressWarnings("javadoc")
	private void writeObject(final ObjectOutputStream output_stream)
			throws IOException
	{
		throw new NotSerializableException(User_notification_event.class.getName());
	}
	
	
	/**
	 * This class <u>does&nbsp;not</u> support serialization.
	 * 
	 * @exception NotSerializableException When this method called.
	 */
	@SuppressWarnings("javadoc")
	private void readObject(final ObjectInputStream input_stream)
			throws IOException, ClassNotFoundException
	{
		throw new NotSerializableException(User_notification_event.class.getName());
	}
}
