﻿package time_obj.containers;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import time_obj.Instance_counter;
import time_obj.Time_counter;
import time_obj.Time_counter_control;


// TODO: ? Объявить final
// TODO: ? Реализовать в виде singleton'а
/**
 * Модифицированный контейнер, в котором переопределяется ряд методов
 * с&nbsp;целью взаимодействия с классом {@link Time_counter_control}.<br>
 * <i>Примечания.</i>
 * <ul><li>Не&nbsp;может содержать {@code null} в&nbsp;качестве объекта.</li>
 * <li>Не&nbsp;может содержать одинаковые элементы</li>
 * <li>protected&#8209;метод {@link ArrayList#removeRange(int, int)}
 * не&nbsp;переопределяется.</li>
 * <li>Не&nbsp;поддерживает сериализацию.</li></ul>
 *
 * @version 1.0. Модифицированная версия {@link ArrayList}&nbsp;&#0151;&nbsp;1.8
 * @author Igor Taranenko
 */
public class Modified_ArrayList extends ArrayList<Time_counter>
{
	///// Перечисления private ===========================================/////
	/**
	 * Является одним из параметров метода
	 * {@link #transfer_collection_to_counter_control}.
	 */
	private enum Transfer_collection_mode
	{
		/** Добавить указанные элементы в список синхронно выполняемых объектов
		 * {@link #counter_control}. */
		TCM_add,
		/** Удалить указанные элементы из списка синхронно выполняемых объектов
		 * {@link #counter_control}. */
		TCM_remove,
		/** Оставить указанные элементы в списке синхронно выполняемых объектов
		 * {@link #counter_control}. */
		TCM_retain,
	}
	
	
	///// Поля private статические ========================================/////
	/** Логирует события данного класса. */
	private static final Logger logger;
	
	
	static
	{
		logger = Logger.getLogger(Modified_ArrayList.class.getName());
	}
	
	
	///// Поля private экземпляра ========================================/////
	/** Экземпляр синглтона, которому отдельно будут передаваться объекты типа
	 * {@link Instance_counter} для их синхронной работы. */
	private Time_counter_control counter_control;
	
	/** Синхронизирует работу всех переопределяемых методов базового класса. */
	private Semaphore modification_semaphore;
	
	/** Кол&#8209;во элементов типа {@link Instance_counter}, содержащихся в
	 * контейнере. */
	private int instance_counters_quantity;
	
	
	///// Нестатическая инициализация ====================================/////
	{
		counter_control = Time_counter_control.get_instance();
		modification_semaphore = new Semaphore(1);
		instance_counters_quantity = 0;
	}
	
	
	///// Конструкторы public ============================================/////
	/**
	 * Создает пустой список размерностью в десять&nbsp;элементов.
	 */
	public Modified_ArrayList()
	{
		super();
	}
	
	
	/**
	 * Создает список, содержащий элементы полученной коллекции,
	 * в&nbsp;том&nbsp;порядке, в котором они возвращаются итератором
	 * коллекции.<br>
	 * 
	 * @param collection_init Коллекция, элементы которой должны быть помещены
	 * в этот контейнер.
	 * 
	 * @exception NullPointerException
	 * <ul><li>В качестве аргумента передан {@code null};</li>
	 * <li>хотя&nbsp;бы один из элементов
	 * коллекции&nbsp;&#0151;&nbsp;{@code null}.</li></ul>
	 */
	public Modified_ArrayList(
			final Collection<? extends Time_counter> collection_init)
	{
		super(collection_init);
		
		// Если в коллекции обнаружен элемент null
		if (collection_init.contains(null))
		{
			throw new NullPointerException(Collection.class.getName()
					+ " argument contains null element");
		}
		
		transfer_collection_to_counter_control(
				collection_init, Transfer_collection_mode.TCM_add);
	}
	
	
	/**
	 * Создает пустой список с указанной размерностью.
	 * 
	 * @param initial_capacity Изначальная размерность списка.
	 * 
	 * @exception IllegalArgumentException В качестве аргумента передано
	 * отрицательное значение.
	 */
	public Modified_ArrayList(final int initial_capacity)
	{
		super(initial_capacity);
	}
	
	
	///// Методы public экземпляра =======================================/////
	/**
	 * Добавляет указанный элемент в контейнер.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит синхронизированные
	 * участки.
	 * 
	 * @param element Элемент, который нужно добавить в контейнер.
	 * 
	 * @return {@code true}.
	 * 
	 * @exception NullPointerException В&nbsp;качестве аргумента передан
	 * {@code null}.
	 * 
	 * @exception IllegalArgumentException Контейнер уже содержит указанный
	 * элемент.
	 */
	@Override
	public boolean add(final Time_counter element)
	{
		// Если параметром метода является null
		if (element == null)
		{
			throw new NullPointerException(
					Time_counter.class.getName() + " argument is null");
		}
		
		// Если контейнер уже содержит добавляемый элемент
		if (this.contains(element))
		{
			throw new IllegalArgumentException(Time_counter.class.getName() +
					" argument already exists in container");
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		// Результат работы метода базового класса
		final boolean to_return = super.add(element);
		
		assert to_return :
			"Unexpected error occurred while adding element to the list";
		
		/* Если объектом для добавления является экземпляром класса
		 * "Instance_counter" */
		if (element instanceof Instance_counter)
		{
			++instance_counters_quantity;
			counter_control.add_instance_counter((Instance_counter)element);
		}
		
		modification_semaphore.release();
		
		return to_return;
	}
	
	
	/**
	 * Вставляет полученный элемент по указанной позиции в контейнер.
	 * Существующий элемент с данным индексом, и все последующие, сдвигаются на
	 * один индекс вправо.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит
	 * синхронизированные участки.
	 * 
	 * @param index Индекс, по которому должен быть размещен полученный элемент.
	 * 
	 * @param element Элемент, который необходимо вставить в контейнер.
	 * 
	 * @exception IndexOutOfBoundsException Если переданный индекс выходит за
	 * возможный диапазон {@code (index < 0 || index > size())}.
	 * 
	 * @exception NullPointerException {@code element == null}.
	 * 
	 * @exception IllegalArgumentException Контейнер уже содержит указанный
	 * элемент.
	 */
	@Override
	public void add(final int index, final Time_counter element)
	{
		// Если полученная ссылка на элемент равна null
		if (element == null)
		{
			throw new NullPointerException(
					Time_counter.class.getName() + " argument is null");
		}
		
		// Если контейнер уже содержит добавляемый элемент
		if (this.contains(element))
		{
			throw new IllegalArgumentException(Time_counter.class.getName() +
					" argument already exists in container");
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		try
		{
			super.add(index, element);
		}
		catch (IndexOutOfBoundsException exc)
		{
			modification_semaphore.release();
			
			logger.log(Level.SEVERE, "Fatal error.\nReason: Passed index"
					+ " argument is out of bounds. Exception\'s stack trace:", exc);
			throw exc;
		}
		
		/* Если объектом для добавления является экземпляром класса
		 * "Instance_counter" */
		if (element instanceof Instance_counter)
		{
			++instance_counters_quantity;
			counter_control.add_instance_counter((Instance_counter)element);
		}
		
		modification_semaphore.release();
	}
	
	
	/**
	 * Добавляет элементы полученной коллекции в конец контейнера
	 * в&nbsp;том&nbsp;порядке, в котором они возвращаются итератором
	 * коллекции.<br>
	 * <b>Важно!</b> Поведение данного метода не&nbsp;определено если полученная
	 * коллекция модифицируется во&nbsp;время выполнения данного метода.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Данный метод содержит
	 * синхронизированные участки.
	 * 
	 * @param collection Коллекция, элементы которой необходимо добавить в
	 * контейнер.
	 * 
	 * @return {@code true}.
	 * 
	 * @exception NullPointerException
	 * <ul><li>В качестве аргумента передан {@code null};</li>
	 * <li>хотя&nbsp;бы один из элементов
	 * коллекции&nbsp;&#0151;&nbsp;{@code null}.</li></ul>
	 * 
	 * @exception IllegalArgumentException Контейнер уже содержит хотя&nbsp;бы
	 * один элемент из полученной коллекции.
	 */
	@Override
	public boolean addAll(final Collection<? extends Time_counter> collection)
	{
		// Если в коллекции обнаружен элемент null
		if (collection.contains(null))
		{
			throw new NullPointerException(
					Collection.class.getName() + " argument contains null");
		}
		
		/* Проверка элементов полученного контейнера на наличие их в этом
		 * контейнере */
		for (Time_counter i : collection)
		{
			if (this.contains(i))
			{
				throw new IllegalArgumentException("At least one element of "
						+ Collection.class.getName()
						+ " argument already exists in this container");
			}
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		// Результат работы переопределенного метода базового класса
		final boolean result = super.addAll(collection);
		
		assert result :
			"Unexpected exception occurred while adding elements to the list";
		
		transfer_collection_to_counter_control(
				collection, Transfer_collection_mode.TCM_add);
		modification_semaphore.release();
		
		return result;
	}
	
	
	/**
	 * Добавляет элементы полученной коллекции в контейнер, начиная с
	 * указанного индекса, в&nbsp;том&nbsp;порядке, в котором они возвращаются
	 * итератором коллекции. Существующий элемент контейнера с указанным
	 * индексом, и все последующие, сдвигаются на один индекс вправо.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит
	 * синхронизированные участки.
	 * 
	 * @param index Индекс, по&nbsp;которому вставляется первый элемент
	 * полученной коллекции.
	 * 
	 * @param collection Коллекция, элементы которой должны быть добавлены в
	 * контейнер.
	 * 
	 * @return {@code true}.
	 * 
	 * @exception IndexOutOfBoundsException Если переданный индекс выходит за
	 * возможный диапазон {@code (index < 0 || index > size())}.
	 * 
	 * @exception NullPointerException
	 * <ul><li>Передан {@code null} в&nbsp;качестве коллекции;</li>
	 * <li>хотя&nbsp;бы один из элементов
	 * коллекции&nbsp;&#0151;&nbsp;{@code null}.</li></ul>
	 * 
	 * @exception IllegalArgumentException Контейнер уже содержит хотя&nbsp;бы
	 * один элемент из полученной коллекции.
	 */
	@Override
	public boolean addAll(
			final int index, final Collection<? extends Time_counter> collection)
	{
		// Если в коллекции обнаружен элемент null
		if (collection.contains(null))
		{
			throw new NullPointerException(
					Collection.class.getName() + " argument contains null");
		}
		
		/* Проверка элементов полученного контейнера на наличие их в этом
		 * контейнере */
		for (Time_counter i : collection)
		{
			if (this.contains(i))
			{
				throw new IllegalArgumentException("At least one element of "
						+ Collection.class.getName()
						+ " argument already exists in this container");
			}
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		// Результат работы переопределенного метода базового класса
		final boolean result;
		
		try
		{
			result = super.addAll(index, collection);
		}
		catch (IndexOutOfBoundsException exc)
		{
			modification_semaphore.release();

			logger.log(Level.SEVERE, "Fatal error.\n Reason: Passed index"
					+ " argument is out of bounds. Exception\'s stack trace:", exc);
			throw exc;
		}
		
		assert result :
			"Unexpected error occurred while adding elements to the list";
		
		transfer_collection_to_counter_control(
				collection, Transfer_collection_mode.TCM_add);
		modification_semaphore.release();
		
		return true;
	}
	
	
	/**
	 * Удаляет все элементы контейнера.
	 */
	@Override
	public void clear()
	{
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		super.clear();
		
		instance_counters_quantity = 0;
		counter_control.clear_instance_counters_list();
		modification_semaphore.release();
	}
	
	
	/**
	 * Удаляет элемент контейнера по&nbsp;указанному индексу. Сдвигает все
	 * последующие элементы влево (вычитает единицу с ихних индексов).<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит
	 * синхронизированные участки.
	 * 
	 * @param index Индекс элемента, который необходимо удалить.
	 * 
	 * @return <u>Метод возвращает {@code null}</u>.
	 * 
	 * @exception IndexOutOfBoundsException Если переданное значение индекса
	 * выходит за диапазон возможных значений
	 * {@code (index < 0 || index >= size())}.
	 */
	@Override
	public Time_counter remove(final int index)
	{
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		final Time_counter removed_element;  // Элемент по удаленному индексу
		
		try
		{
			removed_element = super.remove(index);
		}
		catch (IndexOutOfBoundsException exc)
		{
			modification_semaphore.release();
			
			logger.log(Level.SEVERE, "Fatal error.\nReason: Passed index"
					+ " argument is out of bounds. Exception\'s stack trace:", exc);
			throw exc;
		}
		
		/* Если удаленный элемент является экземпляром класса "Instance_counter",
		 * экземпляры которого должны выполняться синхронно */
		if (removed_element instanceof Instance_counter)
		{
			--instance_counters_quantity;
			counter_control.remove_instance_counter(
					(Instance_counter)removed_element);
		}
		
		modification_semaphore.release();
		
		return null;
	}
	
	
	/**
	 * Удаляет указанный элемент из контейнера, если таковой существует. Если
	 * контейнер не&nbsp;содержит указанного элемента&nbsp;&#0151; контейнер
	 * не&nbsp;изменяется.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит
	 * синхронизированные участки.
	 * 
	 * @param to_remove Элемент, который необходимо удалить из контейнера.
	 * 
	 * @return {@code true}&nbsp;&#0151; элемент удален;
	 * {@code false}&nbsp;&#0151; указанный элемент не&nbsp;найден.
	 * 
	 * @exception NullPointerException В&nbsp;качестве аргумента передан
	 * {@code null}.
	 */
	@Override
	public boolean remove(final Object to_remove)
	{
		// Если параметром метода является null
		if (to_remove == null)
		{
			throw new NullPointerException(
					Object.class.getName() + " argument is null");
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		// Если контейнер не содержит указанного объекта
		if (!super.remove(to_remove))
		{
			modification_semaphore.release();
			
			return false;
		}
		
		--instance_counters_quantity;
		counter_control.remove_instance_counter((Instance_counter)to_remove);
		modification_semaphore.release();
		
		return true;
	}
	
	
	/**
	 * Удаляет из контейнера все элементы, содержащиеся в полученной коллекции.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит
	 * синхронизированные участки.
	 * 
	 * @param collection Коллекция, содержащая элементы которые необходимо
	 * удалить.
	 * 
	 * @return {@code true} если данный контейнер был изменен.
	 * 
	 * @exception ClassCastException Тип элементов полученной коллекции
	 * отличный от {@link Time_counter} или его подклассов.
	 * 
	 * @exception NullPointerException
	 * <ul><li>В качестве аргумента передан {@code null};</li>
	 * <li>хотя&nbsp;бы один из элементов
	 * коллекции&nbsp;&#0151;&nbsp;{@code null}.</li></ul>
	 */
	@Override
	public boolean removeAll(final Collection<?> collection)
	{
		// Если в коллекции обнаружен элемент null
		if (collection.contains(null))
		{
			throw new NullPointerException(
					Collection.class.getName() + " argument contains null");
		}
		
		// Если в коллекции есть элементы И ...
		if (!collection.isEmpty() &&
				/* ... тип этих элементов отличный от "Time_counter" или его
				 * подклассов */
				!(collection.iterator().next() instanceof Time_counter))
		{
			throw new ClassCastException(Collection.class.getName()
					+ " parameter objects have incorrect type. "
					+ Time_counter.class.getName() + " type expected");
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		// Если в результате работы метода базового класса было возвращено false
		if (!super.removeAll(collection))
		{
			modification_semaphore.release();
			
			return false;
		}
		
		transfer_collection_to_counter_control(
				(Collection<? extends Time_counter>)collection,
				Transfer_collection_mode.TCM_remove);
		modification_semaphore.release();
		
		return true;
	}
	
	
	// TODO: ? Правильно ли описание "<? super Time_counter>"?
	/**
	 * Удаляет все элементы контейнера, которые удовлетворяют указанному
	 * условию. Ошибки времени выполнения, возникающие в процессе итерации,
	 * передаются вызывающей области видимости.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит
	 * синхронизированные участки.
	 * 
	 * @param filter Условие, возвращающее {@code true} если элемент
	 * удовлетворяет этому условию.
	 * 
	 * @return {@code true} если хотя&nbsp;бы один элемент был удален.
	 * 
	 * @exception NullPointerException Передан {@code null} в&nbsp;качестве
	 * аргумента.
	 */
	@Override
	public boolean removeIf(final Predicate<? super Time_counter> filter)
	{
		// Если получен null в качестве параметра
		if (filter == null)
		{
			throw new NullPointerException(
					Predicate.class.getName() + " argument is null");
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		// Список удаляемых элементов контейнера
		final ArrayList<Time_counter> to_remove = new ArrayList<>();
		
		/* Отбор удаляемых элементов контейнера для последующего поиска в них
		 * экземпляров типа "Instance_counter" */
		for (Time_counter i : this)
		{
			// Если данный элемент контейнера необходимо удалить
			if(filter.test(i))
			{
				to_remove.add(i);
			}
		}
		
		// Если ни один элемент контейнера не был удален
		if (!super.removeIf(filter))
		{
			modification_semaphore.release();
			
			return false;
		}
		
		transfer_collection_to_counter_control(
				to_remove, Transfer_collection_mode.TCM_remove);
		modification_semaphore.release();
		
		return true;
	}
	
	
	/**
	 * Метод не&nbsp;поддерживается.
	 * 
	 * @exception UnsupportedOperationException При вызове данного метода.
	 */
	@Override
	public void replaceAll(UnaryOperator<Time_counter> operator)
	{
		throw new UnsupportedOperationException(
				"Method is unsupported by this implementation");
	}
	
	
	// TODO: ? Возможно вызывает исключение UnsupportedOperationException
	/**
	 * Удаляет все элементы контейнера, кроме тех которые содержатся в
	 * полученной коллекции.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит
	 * синхронизированные участки.
	 * 
	 * @param collection Коллекция, содержащая элементы которые необходимо
	 * оставить.
	 * 
	 * @return {@code true} если данный контейнер был изменен.
	 * 
	 * @exception ClassCastException Тип элементов полученной коллекции
	 * отличный от {@link Time_counter} или его подклассов.
	 * 
	 * @exception NullPointerException
	 * <ul><li>В качестве аргумента передан {@code null};</li>
	 * <li>хотя&nbsp;бы один из элементов
	 * коллекции&nbsp;&#0151;&nbsp;{@code null}.</li></ul>
	 */
	@Override
	public boolean retainAll(final Collection<?> collection)
	{
		// Если в коллекции обнаружен элемент null
		if (collection.contains(null))
		{
			throw new NullPointerException(
					Collection.class.getName() + " argument contains null");
		}
		
		// Если в коллекции есть элементы И ...
		if (!collection.isEmpty() &&
				/* ... тип этих элементов отличный от "Time_counter" или его
				 * подклассов */
				!(collection.iterator().next() instanceof Time_counter))
		{
			throw new ClassCastException(Collection.class.getName()
					+ " argument objects have incorrect type. "
					+ Time_counter.class.getName() + " type expected");
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		// Результат работы переопределяемого метода
		final boolean result = super.retainAll(collection);
		
		/* Если контейнер не был изменен в результате вызова метода базового
		 * класса */
		if (!result)
		{
			modification_semaphore.release();
			
			return result;
		}
		
		transfer_collection_to_counter_control(
				(Collection<? extends Time_counter>)collection,
				Transfer_collection_mode.TCM_retain);
		modification_semaphore.release();
		
		return result;
	}
	
	
	/**
	 * Заменяет элемент по&nbsp;указанному индексу полученным элементом.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Содержит
	 * синхронизированные участки.
	 * 
	 * @param index Индекс элемента, который нужно заменить.
	 * 
	 * @param element Элемент, который нужно сохранить по&nbsp;указанному
	 * индексу.
	 * 
	 * @return <u>Метод возвращает {@code null}</u>.
	 * 
	 * @exception IndexOutOfBoundsException Если полученное значение индекса
	 * выходит за возможный диапазон значений
	 * {@code (index < 0 || index >= size())}.
	 * 
	 * @exception NullPointerException В качестве элемента передан {@code null}.
	 * 
	 * @exception IllegalArgumentException Контейнер уже содержит указанный
	 * элемент.
	 */
	@Override
	public Time_counter set(final int index, final Time_counter element)
	{
		// Если в качестве элемента передан null
		if (element == null)
		{
			throw new NullPointerException(
					Time_counter.class.getName() + " argument is null");
		}
		
		// Если контейнер уже содержит добавляемый элемент
		if (this.contains(element))
		{
			throw new IllegalArgumentException(Time_counter.class.getName() +
					" argument already exists in container");
		}
		
		try
		{
			modification_semaphore.acquire();
		}
		catch (InterruptedException exc)
		{
			logger.log(Level.INFO, "Thread interrupts.");
			Thread.currentThread().interrupt();
		}
		
		final Time_counter to_replace;  // Элемент, который необходимо заменить
		
		try
		{
			to_replace = super.set(index, element);
		}
		catch (IndexOutOfBoundsException exc)
		{
			modification_semaphore.release();
			
			logger.log(Level.SEVERE, "Passed index argument is out of bounds."
					+ " Exception\'s stack trace:", exc);
			throw exc;
		}
		
		/* Если элемент, который необходимо заменить, имеет тип
		 * "Instance_counter" */
		if (to_replace instanceof Instance_counter)
		{
			--instance_counters_quantity;
			counter_control.remove_instance_counter(
					(Instance_counter)to_replace);
		}
		
		/* Если элемент, который необходимо сохранить, имеет тип
		 * "Instance_counter" */
		if (element instanceof Instance_counter)
		{
			++instance_counters_quantity;
			counter_control.add_instance_counter((Instance_counter)element);
		}
		
		modification_semaphore.release();
		
		return null;
	}
	
	
	/**
	 * Данный метод не&nbsp;поддерживается и генерирует исключение.
	 * 
	 * @exception UnsupportedOperationException Генерируется при вызове метода.
	 */
	@Override
	public Object[] toArray()
	{
		throw new UnsupportedOperationException(
				"Method is unsupported by this implementation");
	}
	
	
	/**
	 * Данный метод не&nbsp;поддерживается и генерирует исключение.
	 * 
	 * @exception UnsupportedOperationException Генерируется при вызове метода.
	 */
	@Override
	public <Type1> Type1[] toArray(Type1[] arr)
	{
		throw new UnsupportedOperationException(
				"Method is unsupported by this implementation");
	}
	
	
	/**
	 * Данный метод объявлен для взаимодействия с синглтоном
	 * {@link Time_counter_control}, и с высокой долей вероятности сгенерирует
	 * исключение при вызове не&nbsp;из указанного объекта.
	 * 
	 * @return Кол&#8209;во элементов типа {@link Instance_counter},
	 * содержащихся в контейнере.
	 * 
	 * @exception IllegalStateException Метод вызван
	 * <u>не&nbsp;во&nbsp;время&nbsp;выполнения</u> переопределенного метода
	 * класса. Скорее всего это произошло при вызове не&nbsp;из
	 * {@link Time_counter_control}.
	 */
	public int get_instance_counters_quantity()
	{
		/* Если метод вызван не во время выполнения синхронизированного участка
		 * класса */
		if (modification_semaphore.availablePermits() != 0)
		{
			throw new IllegalStateException("Method invoked in inappropriate way");
		}
		
		return instance_counters_quantity;
	}
	
	
	///// Методы private экземпляра ======================================/////
	/**
	 * Ищет в полученном контейнере экземпляры {@link Instance_counter} и
	 * передает их синглтону {@link #counter_control} для синхронного
	 * выполнения.<br>
	 * <i>Примечание по&nbsp;производительности.</i> Вызывает синхронизированные
	 * методы.
	 * 
	 * @param collection Контейнер, в котором будет производиться поиск
	 * экземпляров {@link Instance_counter}.
	 * 
	 * @param action Необходимое действие над списком синхронно выполняющихся
	 * объектов, содержащихся в {@link #counter_control}.
	 */
	private void transfer_collection_to_counter_control(
			final Collection<? extends Time_counter> collection,
			final Transfer_collection_mode action)
	{
		/* Временный контейнер для передачи синглтону "counter_control" всех
		 * объектов "Instance_counter", обнаруженных в "collection_init", */
		final ArrayList<Instance_counter> buffer = new ArrayList<>();
		
		/* Если необходимо оставить указанные элементы в списке синхронно
		 * выполняемых объектов ИЛИ необходимо заменить все синхронно
		 * выполняемые объекты новыми */
		if (action.equals(Transfer_collection_mode.TCM_retain))
		{
			instance_counters_quantity = 0;
		}

		/* Поиск объектов типа "Instance_counter" в коллекции "collection_init"
		 * для их передачи синглтону "counter_control" */
		for (Time_counter i : collection)
		{
			// Если объект является экземпляром класса "Instance_counter"
			if (i instanceof Instance_counter)
			{
				buffer.add((Instance_counter)i);
				
				/* Если необходимо удалить указанные элементы из списка
				 * синхронно выполняемых объектов */
				if (action.equals(Transfer_collection_mode.TCM_remove))
				{
					--instance_counters_quantity;
				}
				else
				{
					++instance_counters_quantity;
				}
			}
		}
		
		// Вызов метода из "counter_control" в зависимости от требуемой операции
		switch (action)
		{
		case TCM_add:
			counter_control.add_instance_counters_group(buffer);
			break;
			
		case TCM_remove:
			counter_control.remove_instance_counter_group(buffer);
			break;
			
		case TCM_retain:
			counter_control.retain_instance_counter_group(buffer);
			break;
		}
	}
	
	
	/**
	 * Сериализация не&nbsp;поддерживается. Генерирует исключение.
	 * 
	 * @exception NotSerializableException При вызове данного метода.
	 */
	private void writeObject(ObjectOutputStream out_stream)
			throws IOException
	{
		throw new NotSerializableException(Modified_ArrayList.class.getName() +
				" class doesn\'t support serialization");
	}
	
	
	/**
	 * Десериализация не&nbsp;поддерживается. Генерирует исключение.
	 * 
	 * @exception NotSerializableException При вызове данного метода.
	 */
	private void readObject(ObjectInputStream in_stream)
			throws IOException, ClassNotFoundException
	{
		throw new NotSerializableException(Modified_ArrayList.class.getName() +
				" class doesn\'t support deserialization");
	}
}