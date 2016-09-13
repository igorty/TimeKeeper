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
package app.tar.time_obj;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import app.tar.time_obj.containers.Modified_ArrayList;
import app.tar.time_obj.dialog.User_notification_dialog;
import app.tar.time_obj.dialog.User_notification_type;
import app.tar.time_obj.events.User_notification_event;


/**
 * Stores all {@link Time_counter} objects. List of all objects can be obtained
 * using {@link #get_time_counters()} method.<br>
 * The&nbsp;main task of this class is to provide synchronous execution of all
 * {@link Instance_counter} objects.<br>
 * <i>Note.</i> This class <u>is implemented as singleton</u>. Its instance can
 * be obtained using {@link #get_instance()} method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Time_counter_control
{
	///// Fields default-access static ====================================/////
	/** Resource bundle <i>.properties</i> file containing dialog messages
	 * according to program locale. */
	static ResourceBundle message_resources;
	
	
	///// Поля статические private ========================================/////
	/** Логирует события, происходящие в этом классе. */
	private static final Logger logger;
	
	/** Единственный экземпляр класса */
	private static Time_counter_control instance;
	
	
	static
	{
		logger = Logger.getLogger(Time_counter_control.class.getName());
		message_resources = Settings.get_instance().get_message_resources();
		instance = new Time_counter_control();
	}
	
	
	///// Поля экземпляра private =========================================/////
	/** Название файла, содержащего элементы {@link #time_counters}. */
	private final String file_name;

	/** Содержит объекты, которые должны выполняться синхронно.<br>
	 * <b>Важно!</b> Доступ к полю должен синхронизироваться. */
	private ArrayList<Instance_counter> instance_counters;
	
	/** Содержит все объекты типа {@link Time_counter}. */
	private Modified_ArrayList time_counters;
	
	/** Потоки, вызывающие метод
	 * {@link Instance_counter#difference_calculation(boolean)} каждого
	 * экземпляра класса {@link Instance_counter}.<br>
	 * <b>Важно!</b> Доступ к полю должен синхронизироваться. */
	private ArrayList<Callable<Void>> instance_counters_tasks;
	
	/** Запускает потоки {@link #instance_counters_tasks}. */
	private ExecutorService instance_counters_executor;
	
	/** Основной поток класса, реализующий синхронную работу экземпляров класса
	 * {@link Instance_counter}. */
	private Runnable synchronous_task;
	
	/** Periodically executes {@link #synchronous_task}. */
	private ScheduledExecutorService synchronous_task_executor;
	
	/** Присваивается статическому полю
	 * {@link Instance_counter#difference_calculation_barrier}.<br>
	 * <b>Важно!</b> Кол&#8209;во&nbsp;барьеров <u>должно соответствовать</u>
	 * кол&#8209;ву&nbsp; элементов в {@link #instance_counters} и
	 * {@link #instance_counters_tasks}. */
	private CyclicBarrier instance_counters_barrier;
	
	/** Обеспечивает синхронизированный доступ к полям
	 * {@link #instance_counters} и {@link #instance_counters_tasks}. */
	private ReentrantLock lock;
	
	/** {@code true}&nbsp;&#0151; {@link #read_time_counters_from_file()} method
	 * has&nbsp;been&nbsp;invoked already; {@code false}&nbsp;&#0151; otherwise. */
	private boolean time_counters_are_read_from_file;
	
	
	///// Нестатическая инициализация =====================================/////
	{
		synchronous_task = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					lock.lockInterruptibly();
				}
				catch (final InterruptedException exc)
				{
					logger.log(Level.INFO,
							"Thread interrupts. Exception stack trace:", exc);
					Thread.currentThread().interrupt();
				}
				
				try
				{
					/* Результаты выполнения синхронно выполняющихся методов,
					 * использующиеся как флаг окончания работы каждого
					 * синхронного метода */
					List<Future<Void>> futures = null;
					
					try
					{
						futures = instance_counters_executor.invokeAll(
								instance_counters_tasks);
					}
					catch (final InterruptedException exc)
					{
						instance_counters_barrier.reset();
						logger.log(Level.INFO,
								"Thread interrupts. Exception stack trace", exc);
						Thread.currentThread().interrupt();
					}
					
					// Флаг завершения всех синхронно выполняющихся потоков
					boolean is_done = true;
					
					// Опрос синхронных потоков на предмет их завершения
					do
					{
						for (Future<Void> i : futures)
						{
							is_done = i.isDone();
							
							// Если хотя бы один синхронный поток еще не завершен
							if (!is_done)
							{
								break;
							}
						}
					} while (!is_done);					
					
					instance_counters_barrier.reset();
				}
				finally
				{
					lock.unlock();
				}
			}
		};
		
		lock = new ReentrantLock();
		file_name = "time counters.tk";
		time_counters = new Modified_ArrayList();
		instance_counters = new ArrayList<>();
		instance_counters_tasks = new ArrayList<>();
		time_counters_are_read_from_file = false;
	}
	
	
	///// Конструкторы private ============================================/////
	/**
	 * Единственный конструктор класса.
	 */
	private Time_counter_control() {}
	
	
	///// Методы public статические =======================================/////
	/**
	 * Возвращает единственный экземпляр класса.
	 * 
	 * @return Единственный экземпляр класса.
	 */
	public static Time_counter_control get_instance()
	{
		return instance;
	}
	
	
	///// Методы public экземпляра ========================================/////
	/**
	 * Returns container, with all created {@link Time_counter} objects. Each
	 * {@link Instance_counter} and {@link Solo_counter} object, when created,
	 * adds itself to this container.
	 * 
	 * @return All created {@link Time_counter} objects.
	 */
	public Modified_ArrayList get_time_counters()
	{
		return time_counters;
	}
	
	
	/**
	 * Добавляет объект в группу синхронного выполняющихся объектов типа
	 * {@link Instance_counter}.<br>
	 * <b>Важно!</b> Данный метод объявлен для взаимодействия с внутренним
	 * контейнером типа {@link Modified_ArrayList}. При вызове не&nbsp;из
	 * указанного контейнера генерирует исключение.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param instance_counter_obj Объект, который необходимо добавить.
	 * 
	 * @exception IllegalStateException Метод вызван не&nbsp;из&nbsp;контейнера
	 * {@link Modified_ArrayList}.
	 * 
	 * @exception NullPointerException В&nbsp;качестве аргумента передан
	 * {@code null}.
	 */
	public void add_instance_counter(
			final Instance_counter instance_counter_obj)
	{
		try
		{
			lock.lockInterruptibly();
		}
		/* Данное исключение не ожидается. Даже в случае его возникновения метод
		 * должен выполниться до конца */
		catch (final InterruptedException exc)
		{
			logger.log(Level.WARNING, "Thread encountered unexpected "
					+ InterruptedException.class.getName() + " while acquiring"
					+ " semaphore permit to continue executing. Therefore"
					+ " thread must be executed entirely. Exception stack trace:", exc);
		}
		
		try
		{
			// Кол-во элементов в контейнере "instance_counters"
			final int instance_counters_size = instance_counters.size();
			
			// Если метод вызван не из контейнера "time_counters"
			if (time_counters.get_instance_counters_quantity() -
					instance_counters_size != 1)
			{
				throw new IllegalStateException(
						"Method have been called in inappropriate way");
			}
			
			assert !instance_counters.contains(instance_counter_obj) :
					"Unexpected error occurred while adding Instance_counter element"
						+ " to list. Additional object already exists";
			
			// Объявлен для assert'ов
			boolean result = instance_counters.add(instance_counter_obj);
			
			assert result : "Unexpected error occurred while adding"
					+ " Instance_counter element to list";
			
			result = instance_counters_tasks.add(new Callable<Void>()
			{
				@Override
				public Void call() throws Exception
				{
					instance_counter_obj.difference_calculation(true);
					
					return null;
				}
			});
			
			assert result :
					"Unexpected error occurred while adding Callable object to list";
			
			instance_counters_barrier =
					new CyclicBarrier(instance_counters_size + 1);
			synchronous_task_executor_manager();
		}
		finally
		{
			lock.unlock();
		}
	}
	

	/**
	 * Добавляет группу объектов в общий список для обеспечения синхронного
	 * выполнения всех объектов типа {@link Instance_counter}.<br>
	 * <b>Важно!</b> Данный метод объявлен для взаимодействия с внутренним
	 * контейнером типа {@link Modified_ArrayList}. При вызове не&nbsp;из
	 * указанного контейнера генерирует исключение.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param group Группа объектов, которые необходимо добавить.
	 * 
	 * @exception IllegalStateException Метод вызван не&nbsp;из&nbsp;контейнера
	 * {@link Modified_ArrayList}.
	 * 
	 * @exception NullPointerException В&nbsp;качестве аргумента передан
	 * {@code null}.
	 */
	public void add_instance_counters_group(
			final ArrayList<Instance_counter> group)
	{
		try
		{
			lock.lockInterruptibly();
		}
		/* Данное исключение не ожидается. Даже в случае его возникновения метод
		 * должен выполниться до конца */
		catch (final InterruptedException exc)
		{
			logger.log(Level.WARNING, "Thread encountered unexpected "
					+ InterruptedException.class.getName() + " while acquiring"
					+ " semaphore permit to continue executing. Therefore"
					+ " thread must be executed entirely. Exception stack trace:", exc);
		}
		
		try
		{
			// Кол-во элементов в контейнере "instance_counters"
			final int instance_counters_size = instance_counters.size();
			// Кол-во элементов в полученной коллекции
			final int group_size = group.size();
			
			// Если метод вызван не из контейнера "time_counters"
			if (time_counters.get_instance_counters_quantity() -
					instance_counters_size != group_size)
			{
				throw new IllegalStateException(
						"Method have been called in inappropriate way");
			}
			
			// Объявлен для assert'ов
			boolean result = instance_counters.addAll(group);
			
			assert result : "Unexpected error occurred while adding collection"
					+ " of Instance_counter objects to list";
			
			// Добавление Callable'ов из элементов полученной коллекции
			for (int i = 0; i < group_size; ++i)
			{
				/* Объект, метод которого необходимо поместить в
				 * "instance_counters_tasks" */
				final Instance_counter to_callable = group.get(i);
				
				result = instance_counters_tasks.add(new Callable<Void>()
				{
					@Override
					public Void call() throws Exception
					{
						to_callable.difference_calculation(true);
						
						return null;
					}
				});
				
				assert result :
						"Unexpected error occurred while adding Callable object to list";
			}
			
			instance_counters_barrier =
					new CyclicBarrier(instance_counters_size + group_size);
			synchronous_task_executor_manager();			
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/**
	 * Очищает список синхронно выполняющихся объектов типа
	 * {@link Instance_counter}.<br>
	 * <b>Важно!</b> Данный метод объявлен для взаимодействия с внутренним
	 * контейнером типа {@link Modified_ArrayList}. При вызове не&nbsp;из
	 * указанного контейнера генерирует исключение.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @exception IllegalStateException Метод вызван не&nbsp;из&nbsp;контейнера
	 * {@link Modified_ArrayList}.
	 */
	public void clear_instance_counters_list()
	{
		// Если метод вызван не из контейнера "time_counters"
		if (time_counters.get_instance_counters_quantity() != 0)
		{
			throw new IllegalStateException(
					"Method have been called in inappropriate way");
		}
		
		try
		{
			lock.lockInterruptibly();
		}
		/* Данное исключение не ожидается. Даже в случае его возникновения метод
		 * должен выполниться до конца */
		catch (final InterruptedException exc)
		{
			logger.log(Level.WARNING, "Thread encountered unexpected "
					+ InterruptedException.class.getName() + " while acquiring"
					+ " semaphore permit to continue executing. Therefore"
					+ " thread must be executed entirely. Exception stack trace:", exc);
		}
		
		try
		{
			instance_counters_tasks.clear();
			instance_counters.clear();
			synchronous_task_executor_manager();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/**
	 * Удаляет указанный элемент из списка синхронно выполняющихся объектов.<br>
	 * <b>Важно!</b> Данный метод объявлен для взаимодействия с внутренним
	 * контейнером типа {@link Modified_ArrayList}. При вызове не&nbsp;из
	 * указанного контейнера генерирует исключение.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param to_remove Элемент, который необходимо удалить из списка.
	 * 
	 * @exception IllegalArgumentException Указанный элемент не&nbsp;найден в
	 * списке синхронно выполняющихся объектов.
	 * 
	 * @exception IllegalStateException Метод вызван не&nbsp;из&nbsp;контейнера
	 * {@link Modified_ArrayList}.
	 */
	public void remove_instance_counter(final Instance_counter to_remove)
	{
		try
		{
			lock.lockInterruptibly();
		}
		/* Данное исключение не ожидается. Даже в случае его возникновения метод
		 * должен выполниться до конца */
		catch (final InterruptedException exc)
		{
			logger.log(Level.WARNING, "Thread encountered unexpected "
					+ InterruptedException.class.getName() + " while acquiring"
					+ " semaphore permit to continue executing. Therefore"
					+ " thread must be executed entirely. Exception stack trace:", exc);
		}
		
		try
		{
			// Если метод вызван не из контейнера "time_counters"
			if (instance_counters.size() -
					time_counters.get_instance_counters_quantity() != 1)
			{
				throw new IllegalStateException(
						"Method have been called in inappropriate way");
			}
			
			// Если указанного в параметре объекта не существует
			if (!instance_counters.remove(to_remove))
			{
				throw new IllegalArgumentException(
						"to_remove object doesn\'t exist in the list");
			}
			
			reset_instance_counters_tasks();
			
			// Synchronously executed objects quantity
			final int instance_counters_quantity = instance_counters.size();
			
			// Cyclic barriers quantity cannot be 0
			if (instance_counters_quantity != 0)
			{
				instance_counters_barrier = new CyclicBarrier(instance_counters_quantity);
			}
			
			synchronous_task_executor_manager();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/**
	 * Удаляет группу объектов из общего списка синхронно выполняющихся
	 * объектов.<br>
	 * <b>Важно!</b> Данный метод объявлен для взаимодействия с внутренним
	 * контейнером типа {@link Modified_ArrayList}. При вызове не&nbsp;из
	 * указанного контейнера генерирует исключение.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param group Группа объектов, которые необходимо удалить.
	 * 
	 * @exception IllegalStateException Метод вызван не&nbsp;из&nbsp;контейнера
	 * {@link Modified_ArrayList}.
	 * 
	 * @exception NullPointerException В&nbsp;качестве аргумента передан
	 * {@code null}.
	 */
	public void remove_instance_counter_group(
			final ArrayList<Instance_counter> group)
	{
		try
		{
			lock.lockInterruptibly();
		}
		/* Данное исключение не ожидается. Даже в случае его возникновения метод
		 * должен выполниться до конца */
		catch (final InterruptedException exc)
		{
			logger.log(Level.WARNING, "Thread encountered unexpected "
					+ InterruptedException.class.getName() + " while acquiring"
					+ " semaphore permit to continue executing. Therefore"
					+ " thread must be executed entirely. Exception stack trace:", exc);
		}
		
		try
		{
			// Если метод вызван не из контейнера "time_counters"
			if (instance_counters.size() -
					time_counters.get_instance_counters_quantity() != group.size())
			{
				throw new IllegalStateException(
						"Method have been called in inappropriate way");
			}
			
			// Объявлен для assert'а
			final boolean result = instance_counters.removeAll(group);
			
			assert result :
					"Unexpected error occurred while removing elements from list";
			
			reset_instance_counters_tasks();
			instance_counters_barrier = new CyclicBarrier(instance_counters.size());
			synchronous_task_executor_manager();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/**
	 * Удаляет все элементы из общего списка синхронно выполняющихся объектов
	 * за&nbsp;исключением объектов из указанного контейнера.<br>
	 * <b>Важно!</b> Данный метод объявлен для взаимодействия с внутренним
	 * контейнером типа {@link Modified_ArrayList}. При вызове не&nbsp;из
	 * указанного контейнера генерирует исключение.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param group Группа объектов, которые необходимо оставить.
	 * 
	 * @exception IllegalStateException Метод вызван не&nbsp;из&nbsp;контейнера
	 * {@link Modified_ArrayList}.
	 * 
	 * @exception NullPointerException В&nbsp;качестве аргумента передан
	 * {@code null}.
	 */
	public void retain_instance_counter_group(
			final ArrayList<Instance_counter> group)
	{
		try
		{
			lock.lockInterruptibly();
		}
		/* Данное исключение не ожидается. Даже в случае его возникновения метод
		 * должен выполниться до конца */
		catch (final InterruptedException exc)
		{
			logger.log(Level.WARNING, "Thread encountered unexpected "
					+ InterruptedException.class.getName() + " while acquiring"
					+ " semaphore permit to continue executing. Therefore"
					+ " thread must be executed entirely. Exception stack trace:", exc);
		}
		
		try
		{
			// Если метод вызван не из контейнера "time_counters"
			if (instance_counters.size() -
					time_counters.get_instance_counters_quantity() != group.size())
			{
				throw new IllegalStateException(
						"Method have been called in inappropriate way");
			}
			
			// Объявлен для assert'а
			final boolean result = instance_counters.retainAll(group);
			
			assert result : "Unexpected exception occurred while calling"
					+ " retain_instance_counter_group(ArrayList<Instance_counter>)."
					+ " Method shouldn\'t return false";
			
			reset_instance_counters_tasks();
			instance_counters_barrier = new CyclicBarrier(instance_counters.size());
			synchronous_task_executor_manager();
		}
		finally
		{
			lock.unlock();
		}
	}
	
	
	/**
	 * Записывает все содержащиеся элементы {@link Time_counter} в файл.
	 */
	public void save_time_counters()
	{
		// Comparator, сортирующий элементы контейнера "time_counters"
		final Comparator<Time_counter> comparator =
				new Comparator<Time_counter>()
		{
			@Override
			public int compare(Time_counter object1, Time_counter object2)
			{
				return Integer.compare(object1.index_number, object2.index_number);
			}
		};
		
		time_counters.sort(comparator);
		
		FileOutputStream file_output = null;  // Исходящий файловый поток
		// Буферизированный исходящий поток
		BufferedOutputStream buffered_output = null;
		ObjectOutputStream object_output = null;
		
		try
		{
			file_output = new FileOutputStream(file_name);
			buffered_output = new BufferedOutputStream(file_output);
			object_output = new ObjectOutputStream(buffered_output);
			
			// Кол-во элементов в контейнере "time_counters"
			final int time_counters_size = time_counters.size();
			
			object_output.writeFloat((float)1.0);  // Запись версии этого класса
			object_output.writeInt(time_counters_size);
			
			// Запись элементов контейнера "time_counters" в файл
			for (int i = 0; i < time_counters_size; ++i)
			{
				object_output.writeObject(time_counters.get(i));
			}
		}
		catch (final FileNotFoundException exc)
		{
			logger.log(Level.SEVERE, "Cannot obtain " + file_name
					+ " file to write " + Time_counter.class.getName()
					+ " objects. Exception stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(
					new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					message_resources.getString("time_counters_file_write_error1.1")
							+ file_name
							+ message_resources.getString("time_counters_file_write_error1.2"));
		}
		catch (final IOException exc)
		{
			logger.log(Level.SEVERE, "Cannot perform writing "
					+ Time_counter.class.getName() + " objects to " + file_name
					+ " file. Exception stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(
					new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					message_resources.getString("time_counters_file_write_error2"));
		}
		finally
		{
			try
			{
				///// Попытка закрытия потоков вниз по цепочке /////
				// Если исходящий поток объектов был открыт
				if (object_output != null)
				{
					object_output.close();
				}
				/* Если программа успела открыть буферизированный исходящий
				 * поток */
				else if (buffered_output != null)
				{
					buffered_output.close();
				}
				// Если программа успела открыть только исходящий файловый поток
				else if (file_output != null)
				{
					file_output.close();
				}
			}
			/* В случае возникновения данного исключения никаких действий
			 * не предпринимается */
			catch (final IOException exc)
			{
				logger.log(Level.WARNING, "Cannot close " + file_name
						+ " file after writing. Exception stack trace:", exc);
			}
		}
	}
	
	
	/**
	 * Reads {@link Time_counter} objects from file and adds them to inner
	 * time&nbsp;counters list (can be obtained using
	 * {@link #get_time_counters()} method).<br>
	 * <i>Note.</i> The&nbsp;method <u>does&nbsp;nothing</u> if it
	 * has&nbsp;been&nbsp;called already and succeed.
	 * 
	 * @return {@code true}&nbsp;&#0151; {@link Time_counter}{@code s} are read
	 * from file. {@code false}&nbsp;&#0151; failed to read file or method
	 * is&nbsp;called not&nbsp;the&nbsp;first time.
	 */
	public boolean read_time_counters_from_file()
	{
		// If time counters are already read from file
		if (time_counters_are_read_from_file)
		{
			return false;
		}
		
		FileInputStream file_input_stream = null;
		BufferedInputStream buffered_input_stream = null;
		ObjectInputStream object_input_stream = null;
		// "Time_counter" objects quantity which are stored in file
		int objects_quantity = -1;
		// Read from file objects quantity. Is the for-loop counter too
		int objects_read = 0;
		/* Deserialized objects status. true - all deserialized objects and
		 * their fields are correct; false - at least one (non critical) error
		 * occurred while objects fields deserialization */
		boolean deserialization_status = true;
		
		try
		{
			file_input_stream = new FileInputStream(file_name);
			buffered_input_stream = new BufferedInputStream(file_input_stream);
			object_input_stream = new ObjectInputStream(buffered_input_stream);
			// Skip this class version (float value)
			object_input_stream.skipBytes(4);
			objects_quantity = object_input_stream.readInt();
			
			time_counters.ensureCapacity(objects_quantity);
			
			// Read "Time_counter" objects from file to container
			for (; objects_read < objects_quantity; ++objects_read)
			{
				// Temporary object to write into container
				Time_counter temp = null;
				
				try
				{
					temp = (Time_counter)object_input_stream.readObject();
				}
				catch (final ClassNotFoundException | InvalidObjectException exc)
				{
					--objects_read;
					
					continue;
				}
				
				/* Check object's fields deserialization status,
				 * IF deserialized fields were correct before */
				if (deserialization_status)
				{
					deserialization_status = temp.get_deserialization_status();
				}
				
				time_counters.add(temp);
				temp.index_number = objects_read;
			}
		}
		catch (final FileNotFoundException exc)
		{
			logger.log(Level.WARNING, "Cannot find " + file_name + " file to load "
					+ Time_counter.class.getName() + " objects from it."
							+ " Exception stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(
					new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					file_name + message_resources.getString("time_counters_file_not_found"));
			
			return false;
		}
		catch (final IOException exc)
		{
			logger.log(Level.SEVERE, "Cannot read from " + file_name +
					" file. Exception stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(
					new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					message_resources.getString("time_counters_file_read_error.1")
							+ file_name
							+ message_resources.getString("time_counters_file_read_error.2"));
			
			return false;
		}
		finally
		{
			try
			{
				///// Attempt to close streams down the chain /////
				// If object input stream was successfully opened
				if (object_input_stream != null)
				{
					object_input_stream.close();
				}
				// If buffered input stream was opened
				else if (buffered_input_stream != null)
				{
					buffered_input_stream.close();
				}
				// If file input stream was opened only
				else if (file_input_stream != null)
				{
					file_input_stream.close();
				}
				//-------------------------------------------/////
			}
			// There is no actions undertaken when such exception occurred
			catch (final IOException exc)
			{
				logger.log(Level.WARNING, "Cannot close " + file_name +
						" file. Exception stack trace:", exc);
			}
			
			/* If file was successfully opened for reading AND failed to read all
			 * objects */
			if (objects_quantity != -1 && objects_read != objects_quantity)
			{
				// Error message string
				final StringBuilder message =
						new StringBuilder(message_resources.getString(
								"incorrect_time_counters_file_content.1.1"));
				
				// If "Time_counter" objects were read partially
				if (objects_read != 0)
				{
					message.append(message_resources.getString(
							"incorrect_time_counters_file_content.1.2.1.1"));
					message.append(objects_read);
					message.append(message_resources.getString(
							"incorrect_time_counters_file_content.1.2.1.2"));
					message.append(objects_quantity);
					message.append(message_resources.getString(
							"incorrect_time_counters_file_content.1.2.1.3"));
					
					// If deserialized objects contain non critical errors
					if (!deserialization_status)
					{
						message.append(message_resources.getString(
								"incorrect_time_counters_file_content.1.2.2"));
					}
				}
				else
				{
					message.append(message_resources.getString(
							"incorrect_time_counters_file_content.1.3"));
				}
				
				User_notification_dialog.notify_listener_and_wait(new User_notification_event(this),
						User_notification_type.UNT_file_error, message.toString());
			}
			else if (!deserialization_status)
			{
				User_notification_dialog.notify_listener_and_wait(new User_notification_event(this),
						User_notification_type.UNT_file_error,
						message_resources.getString(
								"incorrect_time_counters_file_content.2"));
			}
		}
		
		time_counters_are_read_from_file = true;
		
		return true;
	}

	
	///// Методы private экземпляра =======================================/////
	/**
	 * Is in&nbsp;charge for {@link #synchronous_task_executor} and
	 * {@link #instance_counters_executor} launching/terminating. Determines
	 * the&nbsp;necessary action when called.
	 */
	private void synchronous_task_executor_manager()
	{
		/* Если есть объекты, выполнять которые необходимо синхронно -
		 * выполняется запуск executor'а */
		if (instance_counters.size() > 0)
		{
			// Если executor хотя бы раз запускался И ...
			if (synchronous_task_executor != null &&
					// ... executor сейчас выполняется
					!synchronous_task_executor.isShutdown())
			{
				return;
			}
			
			instance_counters_executor = Executors.newCachedThreadPool();
			Instance_counter.difference_calculation_barrier =
					instance_counters_barrier;
			synchronous_task_executor =
					Executors.newSingleThreadScheduledExecutor();
			synchronous_task_executor.scheduleAtFixedRate(
					synchronous_task, 0, 1, TimeUnit.SECONDS);
		}
		// Остановка executor'а
		else
		{
			// Если executor еще ни разу не запускался
			if (synchronous_task_executor == null)
			{
				return;
			}
			
			synchronous_task_executor.shutdown();
			instance_counters_executor.shutdown();
			
			try
			{
				synchronous_task_executor.awaitTermination(1, TimeUnit.SECONDS);
				instance_counters_executor.awaitTermination(1, TimeUnit.SECONDS);
			}
			/* Данное исключение не ожидается. Даже в случае его возникновения
			 * необходимо полностью выполнить метод с целью принудительного
			 * завершения выполнения потока */
			catch (final InterruptedException exc)
			{
				logger.log(Level.WARNING, "Thread encountered "
						+ InterruptedException.class.getName()
						+ " exception while waiting for thread termination. This"
						+ " thread will continue to execute. Exception stack trace:", exc);
			}
			
			// If this executor wasn't terminated while waiting
			if (!synchronous_task_executor.isTerminated())
			{
				logger.log(Level.WARNING,
						"Forcible " + ScheduledExecutorService.class.getName()
								+ " termination due\u00A0to long waiting");
				synchronous_task_executor.shutdownNow();
			}
			
			// If this executor wasn't terminated while waiting
			if (!instance_counters_executor.isTerminated())
			{
				logger.log(Level.WARNING,
						"Forcible " + ExecutorService.class.getName()
								+ " termination due\u00A0to long waiting");
				instance_counters_executor.shutdownNow();
			}
		}
	}
	
	
	/**
	 * Очистка {@link #instance_counters_tasks} и заполнение его Callable'ами
	 * заново.
	 */
	private void reset_instance_counters_tasks()
	{
		instance_counters_tasks.clear();
		
		for (int i = 0, end = instance_counters.size(); i < end; ++i)
		{
			/* Объект, метод которого необходимо поместить в
			 * "instance_counters_tasks" */
			final Instance_counter to_callable = instance_counters.get(i);
			// Объявлен для assert'а
			final boolean result =
					instance_counters_tasks.add(new Callable<Void>()
			{
				@Override
				public Void call() throws Exception
				{
					to_callable.difference_calculation(true);
					
					return null;
				}
			});
			
			assert result :
					"Unexpected error occurred while adding Callable object to list";
		}
	}
}
