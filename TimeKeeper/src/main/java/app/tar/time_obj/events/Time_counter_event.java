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

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EventObject;

import time_obj.Time_counter;


/**
 * Event object representing {@link Time_counter} event.<br>
 * <i>Note.</i> <u>Does&nbsp;not</u> support serialization.
 * 
 * @param <Type1> {@link Time_counter} extending type.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
@SuppressWarnings("serial")
public class Time_counter_event<Type1 extends Time_counter> extends EventObject
{
	///// Constructors public =============================================/////
	/**
	 * @param source The&nbsp;{@link Time_counter} (or its extending class)
	 * object on which the&nbsp;Event initially occurred.
	 * 
	 * @exception IllegalArgumentException {@code source} argument is {@code null}.
	 */
	public Time_counter_event(final Type1 source)
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
		throw new NotSerializableException(Time_counter_event.class.getName());
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
		throw new NotSerializableException(Time_counter_event.class.getName());
	}
}
