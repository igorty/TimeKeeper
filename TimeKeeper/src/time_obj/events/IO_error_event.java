package time_obj.events;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EventObject;


/**
 * Event object representing IO&nbsp;error event. {@link time_obj} classes use
 * it to notify listener, which implements {@link IO_error_listener}, about
 * read/write error.<br>
 * <i>Note.</i> <u>Does&nbsp;not</u> support serialization.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
@SuppressWarnings("serial")
public class IO_error_event extends EventObject
{
	///// Constructors public =============================================/////
	/**
	 * @param source The&nbsp;object on which IO&nbsp;error initially occurred.
	 * 
	 * @exception NullPointerException {@code source} argument is {@code null}.
	 */
	public IO_error_event(final Object source)
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
		throw new NotSerializableException(IO_error_event.class.getName());
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
		throw new NotSerializableException(IO_error_event.class.getName());
	}
}
