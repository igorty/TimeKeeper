package time_obj.events;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EventObject;

import time_obj.Solo_counter;


/**
 * Event object representing {@link Solo_counter} numeric overflow event.<br>
 * <i>Note.</i> <u>Does&nbsp;not</u> support serialization.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
@SuppressWarnings("serial")
public class Numeric_overflow_event extends EventObject
{
	///// Constructors public =============================================/////
	/**
	 * @param source The&nbsp;{@link Solo_counter} object on which numeric
	 * overflow initially occurred.
	 * 
	 * @exception IllegalArgumentException {@code source} argument is {@code null}.
	 */
	public Numeric_overflow_event(final Solo_counter source)
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
	private void writeObject(ObjectOutputStream output_stream)
			throws IOException
	{
		throw new NotSerializableException(Numeric_overflow_event.class.getName());
	}
	
	
	/**
	 * This class <u>does&nbsp;not</u> support serialization.
	 * 
	 * @exception NotSerializableException When this method called.
	 */	
	@SuppressWarnings("javadoc")
	private void readObject(ObjectInputStream input_stream)
			throws IOException, ClassNotFoundException
	{
		throw new NotSerializableException(Numeric_overflow_event.class.getName());
	}
}
