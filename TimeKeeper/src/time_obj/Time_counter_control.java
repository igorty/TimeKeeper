package time_obj;

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

import time_obj.containers.Modified_ArrayList;
import time_obj.dialog.IO_error_type;
import time_obj.dialog.Read_write_dialog;
import time_obj.events.IO_error_event;


/**
 * Stores all {@link Time_counter} objects. List of all objects can be obtained
 * using {@link #get_time_counters()} method.<br>
 * The&nbsp;main task of this class is to provide synchronous execution of all
 * {@link Instance_counter} objects.<br>
 * <i>Note.</i> This class <u>is implemented as singleton</u>. Its instance can
 * be obtained usnig {@link #get_instance()} method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Time_counter_control
{
	///// Поля статические private ========================================/////
	/** Логирует события, происходящие в этом классе. */
	private static final Logger logger;
	
	/** Единственный экземпляр класса */
	private static Time_counter_control instance;
	
	
	static
	{
		logger = Logger.getLogger(Time_counter_control.class.getName());
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
	 * {@link Instance_counter#difference_calculation()} каждого
	 * экземпляра класса {@link Instance_counter}.<br>
	 * <b>Важно!</b> Доступ к полю должен синхронизироваться. */
	private ArrayList<Callable<Void>> instance_counters_tasks;
	
	/** Запускает потоки {@link #instance_counters_tasks}. */
	private ExecutorService instance_counters_executor;
	
	/** Основной поток класса, реализующий синхронную работу экземпляров класса
	 * {@link Instance_counter}. */
	private Runnable synchronous_task;
	
	/** Executor, переодически запускающий {@link #synchronous_task}. */
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
	
	
	///// Нестатическая инициализация =====================================/////
	{
		instance_counters_executor = Executors.newCachedThreadPool();
		
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
		
		FileInputStream file_input = null;  // Входящий файловый поток
		// Буферизированный входящий поток
		BufferedInputStream buffered_input = null;
		ObjectInputStream object_input = null;  // Входящий поток объектов
		// Кол-во объектов "Time_counter", содержащееся в файле
		int objects_quantity = -1;
		/* Кол-во объектов, прочитанных из файла. Также выступает счетчиком
		 * цикла for */
		int objects_read = 0;
		/* Статус десериализации объектов. true - поля десериализованных
		 * объектов являются корректными; false - обнаружена как минимум одна
		 * (некритическая) ошибка при десериализации полей объектов */
		boolean deserialization_status = true;
		
		try
		{
			file_input = new FileInputStream(file_name);
			buffered_input = new BufferedInputStream(file_input);
			object_input = new ObjectInputStream(buffered_input);
			object_input.skipBytes(4);  // Пропуск версии этого класса
			objects_quantity = object_input.readInt();
			
			time_counters.ensureCapacity(objects_quantity);
			
			// Чтение объектов "Time_counter" из файла в контейнер
			for (; objects_read < objects_quantity; ++objects_read)
			{
				// Временный объект для записи в контейнеры
				Time_counter temp = null;
				
				try
				{
					temp = (Time_counter)object_input.readObject();
				}
				catch (ClassNotFoundException | InvalidObjectException exc)
				{
					--objects_read;
					
					continue;
				}
				
				/* Проверка статуса десериализации некритических полей объекта,
				 * ЕСЛИ до этого десериализированные поля были корректными */
				if (deserialization_status)
				{
					deserialization_status = temp.get_deserialization_status();
				}
				
				time_counters.add(temp);
				temp.index_number = objects_read;
				
				// Если прочитанный объект относится к типу "Instance_counter"
				if (temp instanceof Instance_counter)
				{
					instance_counters.add((Instance_counter)temp);
				}
			}
		}
		catch (final FileNotFoundException exc)
		{
			logger.log(Level.WARNING, "Cannot find " + file_name + " file to load "
					+ Time_counter.class.getName() + " objects from it."
							+ " Excepton stack trace:", exc);
			Read_write_dialog.notify_listener(new IO_error_event(this),
					IO_error_type.IOET_read_error, file_name + " file not found."
							+ " Program cannot load time counters saved previously.");
		}
		catch (final IOException exc)
		{
			logger.log(Level.SEVERE, "Cannot read from " + file_name +
					" file. Exception stack trace:", exc);
			Read_write_dialog.notify_listener(new IO_error_event(this),
					IO_error_type.IOET_read_error, "Error occurred while reading "
							+ file_name + " file with saved time counters."
							+ " Time counters cannot be loaded to program.");
		}
		finally
		{
			/* Если файл был открыт для чтения И при этом не удалось прочитать
			 * все объекты */
			if (objects_quantity != -1 && objects_read != objects_quantity)
			{
				// Строка для сообщения об ошибке
				final StringBuilder message = new StringBuilder(
						"Error occurred while reading saved time counters from the file.");
				
				// Если частично удалось прочитать объекты "Time_counter" из файла
				if (objects_read != 0)
				{
					message.append(" Not all instances have been read. Quantity of files read is ");
					message.append(objects_read);
					message.append(" of ");
					message.append(objects_quantity);
					message.append(" available.");
					
					/* Если десериализированные объекты содержат некритические
					 * ошибки */
					if (!deserialization_status)
					{
						message.append("\nSome of read instances have noncritical"
								+ " errors (such instances will function properly).");
					}
				}
				else
				{
					message.append(" None time counters have been read.");
				}
				
				Read_write_dialog.notify_listener(new IO_error_event(this),
						IO_error_type.IOET_read_error, message.toString());
			}
			else if (!deserialization_status)
			{
				Read_write_dialog.notify_listener(new IO_error_event(this),
						IO_error_type.IOET_read_error,
						"Some of read instances have noncritical errors (such"
								+ " instances will function properly).");
			}
			
			try
			{
				///// Попытка закрытия потоков вниз по цепочке /////
				// Если входящий поток объектов был открыт
				if (object_input != null)
				{
					object_input.close();
				}
				// Если программа успела открыть буферизированный входящий поток
				else if (buffered_input != null)
				{
					buffered_input.close();
				}
				// Если программа успела открыть только входящий файловый поток
				else if (file_input != null)
				{
					file_input.close();
				}
			}
			// При возникновении данного исключения никаких действий не предпринимается
			catch (final IOException exc)
			{
				logger.log(Level.WARNING, "Cannot close " + file_name +
						" file. Exception stack trace:", exc);
			}
			finally
			{
				instance_counters_tasks =
						new ArrayList<>(instance_counters.size());
				reset_instance_counters_tasks();
			}
		}
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
					instance_counter_obj.difference_calculation();
					
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
						to_callable.difference_calculation();
						
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
						"to_remove object doesen\'t exist in the list");
			}
			
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
	
	
	// TODO: ? Возможно вызывает исключение UnsupportedOperationException
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
		ObjectOutputStream object_output = null;  // Искходящий поток объектов
		
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
			Read_write_dialog.notify_listener(new IO_error_event(this),
					IO_error_type.IOET_write_error,
					"Error occurred while accessing " + file_name + " file."
							+ " Current time counters state cannot be saved.");
		}
		catch (final IOException exc)
		{
			logger.log(Level.SEVERE, "Cannot perform writing "
					+ Time_counter.class.getName() + " objects to " + file_name
					+ " file. Exception stack trace:", exc);
			Read_write_dialog.notify_listener(new IO_error_event(this),
					IO_error_type.IOET_write_error, "Error occurred while saving"
							+ " time counters in file. Current time counters"
							+ " state probably haven\'t been saved properly.");
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

	
	///// Методы private экземпляра =======================================/////
	/**
	 * Отвечает за запуск/приостановку executor'а
	 * {@link #synchronous_task_executor}. При вызове определяет необходимость
	 * запуска либо остановки executor'а.
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
			
			try
			{
				synchronous_task_executor.awaitTermination(1, TimeUnit.SECONDS);
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
			
			if (!synchronous_task_executor.isTerminated())
			{
				synchronous_task_executor.shutdownNow();
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
					to_callable.difference_calculation();
					
					return null;
				}
			});
			
			assert result :
				"Unexpected error occurred while adding Callable object to list";
		}
	}
}
