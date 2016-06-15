package main_package;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import time_obj.Mode;
import time_obj.Time_counter;
import time_obj.Time_counter_control;
import time_obj.containers.Modified_ArrayList;


/**
 * Исходный класс программы.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Main_class extends Application
{
	///// Поля public статические =========================================/////
	// TODO: Переместить
	// TODO: Initialize
	/** Панель компоновки, содержащая созданные счетчики времени. */
	public static GridPane time_counters_pane;
	
	/** Содержит названия режимов счетчика времени для ГПИ.<br>
	 * <b>Важно!</b> Ссылается на {@link Collections#unmodifiableMap(Map)}.
	 * Попытка изменения содержимого контейнера приведет к ошибке времени
	 * выполнения. */
	public static final Map<Mode, String> mode_names;
	
	
	static
	{
		// Строки, содержащиеся в контейнере "mode_names"
		final String[] mode_names_strings = {
				"Stopwatch", "Countdown", "Elapsed from", "Remains till" };
		// Все именованные константы перечисления
		final Mode[] mode_values = Mode.values();
		
		assert mode_names_strings.length == mode_values.length :
			"Strings array size doesn\'t match with " + Mode.class.getName()
				+ " elements quantity";
		
		// Инициализатор контейнера "mode_names"
		final Map<Mode, String> mode_names_init = new EnumMap<>(Mode.class);
		
		// Инициализация контейнера "mode_names_init"
		for (Mode i : mode_values)
		{
			mode_names_init.put(i, mode_names_strings[i.ordinal()]);
		}
		
		mode_names = Collections.unmodifiableMap(mode_names_init);
	}
	
	
	///// Методы public статические =======================================/////
	/**
	 * Точка входа программы.
	 * 
	 * @param args Не&nbsp;используется.
	 */
	public static void main(final String[] args)
	{
		launch((String[])null);
	}
	
	
	///// Методы public экземпляра ========================================/////
	/**
	 * @param primary_stage Основная сцена. Задается исполняющей средой.
	 */
	@Override
	public void start(final Stage primary_stage)
	{
		// TODO
	}
	
	
	/////  Методы private экземпляра ======================================/////
	// TODO: ? No need
	/**
	 * Выводит окно&nbsp;ГПИ для создания объекта {@link Time_counter}.
	 * Созданный объект добавляется в контейнер типа {@link Modified_ArrayList},
	 * который можно получить вызовом метода
	 * {@link Time_counter_control#get_time_counters()}, а&nbsp;также помещается
	 * на панель компоновки {@link #time_counters_pane}.
	 * 
	 * @param owner Вызвавшее окно, которое будет деактивировано на&nbsp;время
	 * существования окна для создания объекта {@link Time_counter}.
	 * 
	 * @param time_counter_mode Режим работы созданного счетчика времени.
	 */
	private void create_time_counter(
			final Window owner, final Mode time_counter_mode)
	{
		// TODO: Initizlize whith children
		final VBox root_node = null;  // Корневая панель компоновки
		// TODO: Подогнать размер
		final Scene scene = new Scene(root_node, 400, 600);  // Сцена окна
		final Stage stage = new Stage();  // Подмостки окна
		
		stage.setScene(scene);
		stage.initOwner(owner);
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setResizable(false);
		stage.show();
	}
}