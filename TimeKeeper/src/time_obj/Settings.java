package time_obj;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import time_obj.dialog.User_notification_type;
import time_obj.dialog.User_notification_dialog;
import time_obj.events.User_notification_event;


/* TODO: Change description after removing "serialize()" method calling from
 * some instance methods */
/**
 * Содержит настройки программы. После каждого успешного обращения к методам,
 * устанавливающим настройки, поля класса перезаписываются в файл.<br>
 * <i>Примечание.</i> Реализован в&nbsp;виде singleton'а.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public final class Settings implements Serializable
{
	///// Поля private статические =======================================/////
	/** Отвечает за логирование событий. */
	private static final Logger logger;
	
	/** Единственный экземпляр класса. */
	private static final Settings instance;
	
	/** Режим подсчета дней согласно перечислению {@link Days_in_year}.
	 * По&nbsp;умолчанию установлено ({@link time_obj.Days_in_year#DIY_360}). */
	private static Days_in_year days_count;
	
	/**
	 * Текущий установленный стиль отображения счетчика времени для программы
	 * согласно перечислению {@link time_obj.Time_display_style}.
	 * Значением по&nbsp;умолчанию является
	 * {@link time_obj.Time_display_style#TDS_increase_able}.
	 */
	private static Time_display_style time_display_style_setting;
	
	/** Содержит настройку отображения названий единиц времени для программы в
	 * целом. Значением по&nbsp;умолчанию является отображение только символов
	 * единиц времени&nbsp;&#0151;
	 * {@link time_obj.Time_unit_layout#TUL_value_sign}. */
	private static Time_unit_layout time_unit_layout_setting;
	
	/** Содержит значения крайних отображаемых единиц времени согласно
	 * перечислению {@link time_obj.Time_unit_name}. [0]&nbsp;&#0151; крайняя
	 * левая отображаемая единица (наибольшая отображаемая единица времени);
	 * [1]&nbsp;&#0151; крайняя правая отображаемая единица (наименьшая
	 * отображаемая единица времени).<br>
	 * Данные рамки имеют отношение к стилю отображения счетчика времени
	 * целиком согласно именованным константам
	 * {@link time_obj.Time_display_style#TDS_custom_strict} и
	 * {@link time_obj.Time_display_style#TDS_increase_able} в
	 * перечислении {@link time_obj.Time_display_style}. */
	private static Time_unit_name[] time_value_edges;
	
	/** Название файла с настройками */
	private static final String file_name;
	
	/** Кол&#8209;во разрешений для семафора {@link #common_semaphore}. */
	private static final int common_semaphore_permits;
	
	
	static
	{
		logger = Logger.getLogger(Settings.class.getName());
		file_name = "settings.tk";  // TODO: Указать правильную директорию
		common_semaphore_permits = 3;
		instance = new Settings();
	}
	
	
	///// Поля private экземпляра ========================================/////
	/** Синхронизирует доступ к полю {@link #time_value_edges}. */
	private transient final ReentrantLock time_value_edges_lock;
	/** Синхронизирует доступ записи в поля {@link #days_count},
	 * {@link #time_display_style_setting}, {@link #time_unit_layout_setting}. */
	private transient final Semaphore common_semaphore;
	
	
	///// Нестатическая инициализация ====================================/////
	{
		time_value_edges_lock = new ReentrantLock();
		common_semaphore = new Semaphore(common_semaphore_permits);
		
		FileInputStream file_input = null;  // Входящий файловый поток
		// Буферизированный входящий поток
		BufferedInputStream buffered_input = null;
		ObjectInputStream object_input = null;  // Входящий поток объектов
		
		try
		{
			file_input = new FileInputStream(file_name);
			buffered_input = new BufferedInputStream(file_input);
			object_input = new ObjectInputStream(buffered_input);
			object_input.readObject();
		}
		catch (final FileNotFoundException exc)
		{
			logger.log(Level.WARNING, "Cannot find \"" + file_name
					+ "\" settings file. Exception\'s stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					file_name + " file not found. Program settings will be set to default.");
			set_defaults();
		}
		// Ожидается от "object_input"
		catch (final IOException exc)
		{
			logger.log(Level.SEVERE, "Cannot read file \"" + file_name
					+ "\" settings file. Exception\'s stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					"Error occurred while reading settings file. Program"
							+ " settings will be set to default.");
			set_defaults();
		}
		catch (final ClassNotFoundException exc)
		{
			logger.log(Level.SEVERE, '\"' + file_name + "\" settings file"
					+ " contains incompatible class type. Exception\'s stack trace:", exc);
			User_notification_dialog.notify_listener_and_wait(new User_notification_event(this),
					User_notification_type.UNT_IO_error,
					"Error occurred while reading settings file. Program"
							+ " settings will be set to default.");
			set_defaults();
		}
		finally
		{
			try
			{
				///// Попытки закрытия потоков вниз по цепочке /////
				/* TODO: ? Не вызовет ли конфликт закрытие потока объектов
				 * (а соответственно и файлового потока) после выполнения
				 * "public void set_defaults()", в котором открываются файловый
				 * и объектный потоки для записи в тот же файл при возникновении
				 * исключения? */
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
				// Если программа успела открыть только файловый входящий поток
				else if (file_input != null)
				{
					file_input.close();
				}
			}
			// При неудачной попытке закрытия потока никаких действий не планируется
			catch (final IOException exc)
			{
				logger.log(Level.WARNING, "Cannot close \"" + file_name
						+ "\" settings file. Exception\'s stack trace:", exc);
			}
		}
	}
	
	
	///// Конструкторы private ===========================================/////
	/**
	 * Private&#8209;конструктор для реализации singleton'а.
	 */
	private Settings() {}
	
	
	///// Методы public статические ======================================/////
	/**
	 * Получение единственного экземпляра данного класса.
	 * 
	 * @return Единственный экземпляр класса.
	 */
	public static Settings get_instance()
	{
		return instance;
	}
	
	
	///// Методы public экземпляра========================================/////	
	/**
	 * Возвращает тип подсчета дней в месяце (кол&#8209;во дней в году).
	 * 
	 * @return Именованная константа согласно перечислению
	 * {@link time_obj.Days_in_year}.
	 */
	public Days_in_year get_days_in_year()
	{
		return days_count;
	}

	
	/**
	 * Установка типа подсчета дней в месяце (кол&#8209;во дней в году).<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param days_count_set Именованная константа согласно перечислению
	 * {@link time_obj.Days_in_year}.
	 * 
	 * @exception NullPointerException Передача {@code null} в качестве
	 * аргумента.
	 */
	public void set_days_in_year(final Days_in_year days_count_set)
	{
		// Если параметром метода является null
		if (days_count_set == null)
		{
			throw new NullPointerException(
					Days_in_year.class.getName() + " argument is null");
		}
		
		try
		{
			common_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			days_count = days_count_set;
		}
		finally
		{
			common_semaphore.release();
		}
		
		serialize();  // TODO: Remove
	}

	
	/**
	 * Возвращает текущий установленный стиль отображения счетчика времени для
	 * программы согласно перечислению
	 * {@link time_obj.Time_display_style}.
	 * 
	 * @return Текущий установленный стиль отображения счетчика времени для
	 * программы согласно перечислению
	 * {@link time_obj.Time_display_style}.
	 */
	public Time_display_style get_time_display_style_setting()
	{
		return time_display_style_setting;
	}

	
	/**
	 * Устанавливает новое значение для текущего стиля отображения счетчика
	 * времени в программе согласно перечислению
	 * {@link time_obj.Time_display_style}.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param new_value Новое значение согласно перечислению
	 * {@link time_obj.Time_display_style}.
	 * 
	 * @exception NullPointerException Передача {@code null} в качестве
	 * аргумента.
	 */
	public void set_time_display_style_setting(final Time_display_style new_value)
	{
		// Если параметром метода является null
		if (new_value == null)
		{
			throw new NullPointerException(
					Time_display_style.class.getName() + " argument is null");
		}
		
		try
		{
			common_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_display_style_setting = new_value;
		}
		finally
		{
			common_semaphore.release();
		}
		
		serialize();  // TODO: Remove
	}

	
	/**
	 * Возвращает программную настройку отображения названий единиц времени для
	 * программы в целом согласно перечислению {@link time_obj.Time_unit_layout}.
	 * 
	 * @return Программная настройка отображения названий единиц времени для
	 * программы в целом согласно перечислению {@link time_obj.Time_unit_layout}. 
	 */
	public Time_unit_layout get_time_unit_layout_setting()
	{
		return time_unit_layout_setting;
	}
	

	/**
	 * Устанавливает новое значение программной настройки отображения названий
	 * единиц времени согласно перечислению {@link time_obj.Time_unit_layout}.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param time_unit_layout_set Новое значение программной настройки
	 * отображения названий единиц времени согласно перечислению
	 * {@link time_obj.Time_unit_layout}.
	 * 
	 * @exception NullPointerException Передача {@code null} в качестве
	 * аргумента.
	 */
	public void set_time_unit_layout_setting(
			final Time_unit_layout time_unit_layout_set)
	{
		// Если параметром метода является null
		if (time_unit_layout_set == null)
		{
			throw new NullPointerException(
					Time_unit_layout.class.getName() + " argument is null");
		}
		
		try
		{
			common_semaphore.acquire();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_unit_layout_setting = time_unit_layout_set;
		}
		finally
		{
			common_semaphore.release();
		}
		
		serialize();  // TODO: Remove
	}
	

	/**
	 * Возвращает массив именнованных констант единиц времени со значениями
	 * крайних (наибольшей и наименьшей) единиц времени для отображения.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @return Массив размерностью в два элемента, в котором
	 * элемент&nbsp;{@code [0]} обозначает левую крайнюю единицу времени
	 * (наибольшую отображаемую единицу времени), а элемент&nbsp;{@code [1]}
	 * обозначает правую крайнюю единицу времени (наименьшую отображаемую
	 * единицу времени) согласно перечислению {@link time_obj.Time_unit_name}.<br>
	 * <i>Примечание.</i> Возврат выполняется <u>по значению</u>.
	 */
	public Time_unit_name[] get_time_value_edges()
	{
		try
		{
			time_value_edges_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			return time_value_edges.clone();
		}
		finally
		{
			time_value_edges_lock.unlock();
		}
	}

	
	/**
	 * Устанавливает крайние отображаемые единицы времени согласно перечислению
	 * {@link time_obj.Time_unit_name}.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param left_edge Новое значение для крайней левой отображаемой единицы
	 * времени (т.е.&nbsp;наибольшая отображаемая единица времени). Индекс
	 * массива&nbsp;&#0151;&nbsp;{@code [0]}.
	 * 
	 * @param right_edge Новое значение для крайней правой отображаемой единицы
	 * времени (т.е.&nbsp;наименьшая отображаемая единица времени). Индекс
	 * массива&nbsp;&#0151;&nbsp;{@code [1]}.
	 * 
	 * @exception IllegalArgumentException Параметр {@code left_edge} должен
	 * содержать единицу времени <u>больше</u> чем параметр {@code right_edge},
	 * или быть <u>равным</u> ему.<br>
	 * <i>Примеры:</i>
	 * <ul><li>{@code left_edge} со значением
	 * {@code Time_unit_name.TUN_months} и {@code right_edge} со&nbsp;значением
	 * {@code Time_unit_name.TUN_hours}&nbsp;&#0151; правильно;</li>
	 * <li>{@code left_edge} со&nbsp;значением {@code Time_unit_name.TUN_days}
	 * и {@code right_edge} с этим&nbsp;же значением
	 * {@code Time_unit_name.TUN_days}&nbsp;&#0151; правильно;</li>
	 * <li>{@code left_edge} со&nbsp;значением {@code Time_unit_name.TUN_days}
	 * и {@code right_edge} со&nbsp;значением
	 * {@code Time_unit_name.TUN_years}&nbsp;&#0151;
	 * <u>неправильно</u>.</li></ul>
	 * 
	 * @exception NullPointerException Если хотя&nbsp;бы один из переданных
	 * аргументов&nbsp;&#0151; {@code null}.
	 */
	public void set_time_value_edges(
			final Time_unit_name left_edge, final Time_unit_name right_edge)
	{
		/* Если значение устанавливаемой крайней левой единицы времени
		 * оказалось больше правой */
		if (left_edge.compareTo(right_edge) > 0)
		{
			throw new IllegalArgumentException("left_edge argument value is"
					+ " grater than right_edge argument value");
		}
		
		try
		{
			time_value_edges_lock.lockInterruptibly();
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			time_value_edges[0] = left_edge;
			time_value_edges[1] = right_edge;
		}
		finally
		{
			time_value_edges_lock.unlock();
		}
		
		serialize();  // TODO: Remove
	}
	
	
	/**
	 * Устанавливает настройки программы по&nbsp;умолчанию (за&nbsp;исключением
	 * [TODO: возобновления показа всех предупреждающих всплывающих окон]).<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 */
	public void set_defaults()
	{
		try
		{
			common_semaphore.acquire(common_semaphore_permits);
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			days_count = Days_in_year.DIY_360;
			time_display_style_setting = Time_display_style.TDS_increase_able;
			time_unit_layout_setting = Time_unit_layout.TUL_value_sign;
			
			try
			{
				time_value_edges_lock.lockInterruptibly();
			}
			catch (final InterruptedException exc)
			{
				logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
				Thread.currentThread().interrupt();
			}
			
			try
			{
				time_value_edges = new Time_unit_name[2];
				
				time_value_edges[0] = Time_unit_name.TUN_hours;
				time_value_edges[1] = Time_unit_name.TUN_seconds;
			}
			finally
			{
				time_value_edges_lock.unlock();
			}
		}
		finally
		{
			common_semaphore.release(common_semaphore_permits);
		}
		
		/* There is a possible 'synchronization window' when outlier thread
		 * changes some settings field(-s) before "serialize()" method obtain
		 * mutexes and write settings to file. Consider it to be not a bug */
		serialize();
	}
	
	
	// TODO: Метод возобновления показа всех предупреждающих всплывающих окон
	
	
	///// Методы private экземпляра ======================================/////
	/**
	 * Сериализует поля класса и сохряняет в файл {@link #file_name}.
	 */
	private void serialize()
	{
		try
		{
			common_semaphore.acquire(common_semaphore_permits);
		}
		catch (final InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
			Thread.currentThread().interrupt();
		}
		
		try
		{
			try
			{
				time_value_edges_lock.lockInterruptibly();
			}
			catch (final InterruptedException exc)
			{
				logger.log(Level.INFO, "Thread interrupts. Exception stack trace:", exc);
				Thread.currentThread().interrupt();
			}
			
			try
			{
				// Исходящий файловый поток
				FileOutputStream file_output = null;
				// Исходящий буферизированный поток
				BufferedOutputStream buffered_output = null;
				// Исходящий поток объектов
				ObjectOutputStream object_output = null;
				
				try
				{
					file_output = new FileOutputStream(file_name);
					buffered_output = new BufferedOutputStream(file_output);
					object_output = new ObjectOutputStream(buffered_output);
					object_output.writeObject(this);
				}
				catch (final FileNotFoundException exc)
				{
					logger.log(Level.WARNING, "Cannot find \"" + file_name
							+ "\" settings file. Exception\'s stack trace:", exc);
					User_notification_dialog.notify_listener_and_wait(new User_notification_event(this),
							User_notification_type.UNT_IO_error,
							"Error occurred while accessing settings file."
									+ " Program settings cannot be saved.");
				}
				// Ожидается от "object_output"
				catch (final IOException exc)
				{
					logger.log(Level.SEVERE, "Cannot write settings to \""
							+ file_name + "\" file. Exception\'s stack trace:", exc);
					User_notification_dialog.notify_listener_and_wait(new User_notification_event(this),
							User_notification_type.UNT_IO_error,
							"Error occurred while accessing settings file."
									+ " Program settings cannot be saved.");
				}
				finally
				{
					try
					{
						///// Попытки закрытия потоков вниз по цепочке /////
						// Если исходящий поток объектов был открыт
						if (object_output != null)
						{
							object_output.close();
						}
						/* Если программа успела открыть буферизированный
						 * исходящий поток */
						else if (buffered_output != null)
						{
							buffered_output.close();
						}
						/* Если программа успела открыть только исходящий
						 * файловый поток */
						else if (file_output != null)
						{
							file_output.close();
						}
					}
					/* В случае возникновения данного исключения никаких
					 * действий не планируется */
					catch (final IOException exc)
					{
						logger.log(Level.WARNING, "Cannot close \"" + file_name
								+ "\" settings file. Exception\'s stack trace:", exc);
					}
				}
			}
			finally
			{
				time_value_edges_lock.unlock();
			}
		}
		finally
		{
			common_semaphore.release(common_semaphore_permits);
		}
	}
	
	
	/**
	 * Метод сериализации для {@link ObjectOutputStream}.
	 * 
	 * @param object_output Исходящий поток объектов.
	 * 
	 * @throws IOException Ошибка ввода/вывода.
	 */
	private void writeObject(final ObjectOutputStream object_output)
			throws IOException
	{
		object_output.defaultWriteObject();
		object_output.writeFloat((float)1.0);  // Запись версии класса
		object_output.writeObject(days_count);
		object_output.writeObject(time_display_style_setting);
		object_output.writeObject(time_unit_layout_setting);
		object_output.writeObject(time_value_edges);
	}
	
	
	/**
	 * Метод десериализации для {@link ObjectInputStream}.
	 * 
	 * @param object_input Входящий поток объектов.
	 * 
	 * @throws IOException Ошибка ввода/вывода.
	 * 
	 * @throws ClassNotFoundException Класс сериализованного объекта
	 * не&nbsp;обнаружен.
	 */
	private void readObject(final ObjectInputStream object_input)
			throws IOException, ClassNotFoundException
	{
		object_input.defaultReadObject();
		object_input.skipBytes(4);  // Пропуск версии класса
		days_count = (Days_in_year)object_input.readObject();
		time_display_style_setting =
				(Time_display_style)object_input.readObject();
		time_unit_layout_setting = (Time_unit_layout)object_input.readObject();
		
		/* Временное значение массива "time_value_edges" перед безопасным
		 * копированием */
		final Time_unit_name[] buffer =
				(Time_unit_name[])object_input.readObject();
		
		time_value_edges = buffer.clone();
	}
	
	
	/**
	 * Метод для реализации singleton'а, предотвращающий создание нескольких
	 * экезпляров данного класса путем десериализации.
	 * 
	 * @return Ссылка на единственный экземпляр класса.
	 * 
	 * @throws ObjectStreamException Любая ошибка связянная с входящим потоком
	 * объектов.
	 */
	private Object readResolve()
			throws ObjectStreamException
	{
		return this;
	}
}
