package main_package.FXML_controllers;

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.time.LocalTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.layout.GridPane;
import main_package.GUI_settings;
import time_obj.Days_in_year;
import time_obj.Settings;
import time_obj.Solo_counter;


/**
 * {@link Solo_counter} initial&nbsp;values settings&nbsp;pane controller.
 * Called by {@link FXMLLoader} for
 * <i>"main_package/FXML_controllers/Init_Solo_counter_layout.fxml"</i> file.<br>
 * <i>Notes.</i>
 * <ul><li>Root pane in <i>"Init_Solo_counter_layout.fxml"</i> is
 * {@link GridPane}.</li>
 * <li><i>"Init_Solo_counter_layout.fxml"</i> requires
 * <i>"main_package/resources/GUI_elements/labels.properties"</i> resources to
 * be set.</li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Init_Solo_counter_controller
{
	///// Вложенные классы public =========================================/////
	/**
	 * Выполняет роль контейнера для передачи начальных значений лет, месяцев,
	 * дней, часов, минут и секунд, а&nbsp;также типа подсчета дней в месяце
	 * (кол&#8209;ва&nbsp;дней в году) согласно именованным константам
	 * перечисления {@link Days_in_year}. Эти данные необходимо указать
	 * конструктору
	 * {@link Solo_counter#Solo_counter(time_obj.Mode, java.time.Period, java.time.LocalTime, Days_in_year)}.
	 *
	 * @version 1.0
	 * @author Igor Taranenko
	 */
	public class Time_values
	{
		///// Поля public экземпляра ======================================/////
		/** Значения лет, месяцев и дней. */
		public final Period period;
		
		/** Значения часов, минут и секунд. */
		public final LocalTime duration;
		
		/** Тип подсчета дней в месяце (кол&#8209;во&nbsp;дней в году). */
		public final Days_in_year days_count;
		
		
		///// Конструкторы по умолчанию ===================================/////
		/**
		 * Contains time&nbsp;values and
		 * days&#8209;in&#8209;year/days&#8209;in&#8209;month counting type to
		 * pass&nbsp;on
		 * {@link Solo_counter#Solo_counter(time_obj.Mode, Period, LocalTime, Days_in_year)}
		 * constructor.
		 * 
		 * @param period Years, months and days time&nbsp;values. <u>Can</u> be
		 * {@code null}.
		 * 
		 * @param duration Hours, minutes and seconds time&nbsp;values.
		 * <u>Can</u> be {@code null}.
		 * 
		 * @param days_count Days&#8209;in&#8209;year/days&#8209;in&#8209;month
		 * counting type.
		 * 
		 * @exception NullPointerException {@link Days_in_year} argument is
		 * {@code null}.
		 */
		Time_values(final Period period, final LocalTime duration,
				final Days_in_year days_count)
		{
			// Если "days_count" - null
			if (days_count == null)
			{
				throw new NullPointerException(
						Days_in_year.class.getName() + " argument is null");
			}
			
			this.period = period;
			this.duration = duration;
			this.days_count = days_count;
		}
	}
	
	
	///// Fields private static ===========================================/////
	/** Graphic user interface settings. */
	private static final GUI_settings gui_settings;
	
	
	static
	{
		gui_settings = GUI_settings.get_instance();
	}
	
	
	///// Поля private экземпляра =========================================/////
	/** Поле со&nbsp;значением кол&#8209;ва лет. */
	@FXML
	private TextField years_field;
	
	/** Поле со&nbsp;значением кол&#8209;ва месяцев. */
	@FXML
	private TextField months_field;
	
	/** Поле со&nbsp;значением кол&#8209;ва дней. */
	@FXML
	private TextField days_field;
	
	/** Поле со&nbsp;значением кол&#8209;ва часов. */
	@FXML
	private TextField hours_field;
	
	/** Поле со&nbsp;значением кол&#8209;ва минут. */
	@FXML
	private TextField minutes_field;
	
	/** Поле со&nbsp;значением кол&#8209;ва секунд. */
	@FXML
	private TextField seconds_field;

	/** Текстовая метка, отображающая текст предупреждения в&nbsp;случае ввода
	 * некорректных значений даты и времени. */
	@FXML
	private Label warning_label;

	/** Панель выбора типа подсчета времени согласно именованным константам
	 * перечисления {@link Days_in_year}. */
	@FXML
	private ChoiceBox<String> days_count_choice;
	
	/** Содержит выбранное в {@link #days_count_choice} значение. */
	private Days_in_year chosen_days_count;
	
	/** Contains string items for {@link #days_count_choice}.<br>
	 * <b>Warning!</b> The&nbsp;container is <u>immutable</u>. An&nbsp;attempt
	 * to change its content results in runtime exception. */
	private final Map<Days_in_year, String> days_count_choice_values;
	
	/** Выводит справку об ограничениях для вводимых чисел в текстовые поля
	 * {@link #years_field}, {@link #months_field}, {@link #days_field},
	 * {@link #hours_field}, {@link #minutes_field}, {@link #seconds_field}. */
	@FXML
	private Button date_time_hint_button;
	
	/** Выводит справку о принципах подсчета времени согласно именованным
	 * константам перечисления {@link Days_in_year}. */
	@FXML
	private Button days_count_hint_button;
	
	/** Содержит {@link TextFormatter}'ы для текстовых полей ввода даты и
	 * времени ({@link #years_field}, {@link #months_field},
	 * {@link #days_field}, {@link #hours_field}, {@link #minutes_field} и
	 * {@link #seconds_field}). */
	private final ArrayList<TextFormatter<Integer>> text_formatters;
	
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * specific time counter settings names. */
	private final ResourceBundle time_counter_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * hints and tooltips texts. */
	private final ResourceBundle hints_resources;
	/** Resource bundle representing <i>.properties</i> resource which contains
	 * labels names. */
	private final ResourceBundle labels_resources;
	
	
	///// Нестатическая инициализация =====================================/////
	{
		time_counter_resources = gui_settings.get_time_counter_resources();
		hints_resources = gui_settings.get_hints_resources();
		labels_resources = gui_settings.get_labels_resources();
		
		///// "days_count_choice_values" container initialization /////
		// "days_count_choice_values" container initializer
		final Map<Days_in_year, String> days_count_choice_values_init =
				new EnumMap<>(Days_in_year.class);
		// All "Days_in_year" enumeration constants
		final Days_in_year[] days_in_year_values = Days_in_year.values();
		// String items to be stored in "days_count_choice_values" container
		final String[] days_count_choice_strings = {
				time_counter_resources.getString("days_count.360"),
				time_counter_resources.getString("days_count.365") };
		
		assert days_count_choice_strings.length == days_in_year_values.length :
			"Array size doesn\'t match with " + Days_in_year.class.getName()
				+ " enum constants quantity";
		
		// "days_count_choice_values_init" container initialization
		for (final Days_in_year i : days_in_year_values)
		{
			days_count_choice_values_init.put(
					i, days_count_choice_strings[i.ordinal()]);
		}
		
		days_count_choice_values =
				Collections.unmodifiableMap(days_count_choice_values_init);
		
		
		// Кол-во TextFormatter'ов для добавления к текстовым полям
		final int text_formatters_quantity = 6;
		
		text_formatters = new ArrayList<>(text_formatters_quantity);
		
		// Заполнение контейнера "text_formatters_init" элементами
		for (int i = 1; i <= text_formatters_quantity; ++i)
		{
			text_formatters.add(
					new TextFormatter<>(new UnaryOperator<TextFormatter.Change>()
			{
				@Override
				public Change apply(final Change change)
				{
					// Содержимое текстового поля
					final String change_string = change.getControlNewText();
					// Кол-во символов содержимого текстового поля
					final int change_string_length = change_string.length();
					
					// Текстовое поле не должно содержать более 10-ти символов
					if (change_string_length > 10)
					{
						return null;
					}
					
					// Если текстовое поле не содержит символов
					if (change_string_length == 0)
					{
						return change;
					}
					
					// Разрешенный шаблон для текстовой строки
					final DecimalFormat text_field_format = new DecimalFormat("0");
					// Содержит положение в строке во время парсинга этой строки
					final ParsePosition parse_position = new ParsePosition(0);
					/* Число, получившееся в результате парсинга строки
					 * "change_string" */
					 Number number = text_field_format.
							 parse(change_string, parse_position);
					
					 // Если в начале строки для парсинга находится нечисловой символ
					 if (number == null)
					 {
						 return null;
					 }
					 
					 // Числовое значение, содержащееся в поле для ввода
					 final long field_number = number.longValue();
					 
					// Если в текстовом поле содержатся нецифровые символы ИЛИ ...
					if (parse_position.getIndex() < change_string_length ||
							/* ... получившееся число отрицательное ИЛИ больше
							 * чем может поместиться в int ИЛИ ... */
							field_number < 0 || field_number > Integer.MAX_VALUE ||
							/* ... (первым числом текстового поля является 0 И
							 * при этом пользователь пытается ввести другие
							 * символы (числа)) */
							(change_string.charAt(0) == '0' && change_string_length > 1))
					{
						return null;
					}
					
					warning_label.setText(null);
					return change;
				}
			}));
		}
		
		chosen_days_count = Settings.get_instance().get_days_in_year();
	}
	
	
	///// Методы public экземпляра ========================================/////
	/**
	 * Возвращает объект, сформированный согласно указанным пользователем
	 * значениям. Может вернуть {@code null}, если во&nbsp;время
	 * упрощения&nbsp;(нормализации) значений времени получившиеся значения
	 * не&nbsp;будут помещаться в содержащие их объекты {@link Period} и
	 * {@link LocalTime}.
	 * 
	 * @return Значения времени и типа подсчета месяцев (кол&#8209;ва&nbsp;дней
	 * в году), завернутых в класс. {@code null}&nbsp;&#0151; если во&nbsp;время
	 * нормализации значений времени было брошено {@link ArithmeticException}.<br>
	 * <i>Примечание.</i> При возврате {@code null} на панели компоновки, за
	 * которую отвечает данный контроллер, выводится предупреждающий текст.
	 */
	public Time_values get_time_values()
	{
		// Шаблон парсинга текстовых строк
		final DecimalFormat text_field_format = new DecimalFormat("0");
		// Аргумент для парсинга строки
		final ParsePosition parse_position = new ParsePosition(0);
		// Число получающееся в результате парсинга строки
		Number parsed_number =
				text_field_format.parse(years_field.getText(), parse_position);
		// Кол-во лет
		final int years;
		
		// Если текстовое поле содержит значение
		if (parsed_number != null)
		{
			years = parsed_number.intValue();
			parse_position.setIndex(0);
		}
		else
		{
			years = 0;
		}
		
		// Кол-во месяцев
		final int months;
		
		parsed_number =
				text_field_format.parse(months_field.getText(), parse_position);
		
		// Если текстовое поле содержит значение
		if (parsed_number != null)
		{
			months = parsed_number.intValue();
			parse_position.setIndex(0);
		}
		else
		{
			months = 0;
		}
		
		// Кол-во дней
		final int days;
		
		parsed_number =
				text_field_format.parse(days_field.getText(), parse_position);
		
		// Если текстовое поле содержит значение
		if (parsed_number != null)
		{
			days = parsed_number.intValue();
			parse_position.setIndex(0);
		}
		else
		{
			days = 0;
		}
		
		/* Resource bundle representing ".properties" resource which contains
		 * labels names */
		final ResourceBundle labels_resources = gui_settings.get_labels_resources();
		// Значения лет, месяцев и дней
		Period period = null;

		// Если хотя бы одно из полей лет/месяцев/дней имеет ненулевое значение
		if (years != 0 || months != 0 || days != 0)
		{
			try
			{
				period = Solo_counter.normalize_period_obj(
						Period.of(years, months, days), chosen_days_count);
			}
			catch (final ArithmeticException exc)
			{
				warning_label.setText(
						labels_resources.getString("warning.too_big_time_value"));
				
				return null;
			}
		}
		
		// Кол-во часов
		long hours = 0;
		
		parsed_number =
				text_field_format.parse(hours_field.getText(), parse_position);
		
		// Если текстовое поле содержит значение
		if (parsed_number != null)
		{
			hours = parsed_number.longValue();
			parse_position.setIndex(0);
		}
		
		// Кол-во минут
		long minutes = 0;
		
		parsed_number =
				text_field_format.parse(minutes_field.getText(), parse_position);
		
		// Если текстовое поле содержит значение
		if (parsed_number != null)
		{
			minutes = parsed_number.longValue();
			parse_position.setIndex(0);
		}
		
		parsed_number =
				text_field_format.parse(seconds_field.getText(), parse_position);
		
		// Кол-во секунд
		long seconds =
				(parsed_number != null ? parsed_number.longValue() : 0);
		// Кол-во секунд в минуте
		final long seconds_in_minute = TimeUnit.MINUTES.toSeconds(1);
		/* Кол-во полных дней, формирующееся из значений часов, минут и секунд,
		 * которое необходимо будет добавить в объект типа Period */
		final long whole_days = (hours * TimeUnit.HOURS.toSeconds(1) +
				minutes * seconds_in_minute + seconds) / TimeUnit.DAYS.toSeconds(1);

		// Если есть кол-во дней, которое необходимо добавить в объект типа Period
		if (whole_days != 0)
		{
			// Если значения лет/месяцев/дней не равны нулю
			if (period != null)
			{
				try
				{
					period = Solo_counter.normalize_period_obj(
							period.plusDays(whole_days), chosen_days_count);
				}
				catch (final ArithmeticException exc)
				{
					warning_label.setText(labels_resources.getString(
							"warning.too_big_time_value"));
					
					return null;
				}
			}
			else
			{
				period = Solo_counter.normalize_period_obj(
						Period.of(0, 0, (int)whole_days), chosen_days_count);
			}
		}
		
		hours %= TimeUnit.DAYS.toHours(1);
		minutes %= TimeUnit.HOURS.toMinutes(1);
		seconds %= seconds_in_minute;
		
		// Значения часов, минут и секунд
		LocalTime duration = null;
		
		// Если хотя бы одно из полей часов/минут/секунд имеет ненулевое значение
		if (hours != 0 || minutes != 0 || seconds != 0)
		{
			duration = LocalTime.of((int)hours, (int)minutes, (int)seconds);
		}
		
		return new Time_values(period, duration, chosen_days_count);
	}
	
	
	/**
	 * Выводит указанное предупреждение в отведенном участке панели компоновки.
	 * 
	 * @param warning_text Текст предупреждения.
	 */
	public void set_warning_text(final String warning_text)
	{
		warning_label.setText(warning_text);
	}
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * Called by {@link FXMLLoader}.
	 */
	@FXML
	private void initialize()
	{
		assert years_field != null : "years_field field was not injected";
		assert months_field != null : "months_field field was not injected";
		assert days_field != null : "days_field field was not injected";
		assert hours_field != null : "hours_field field was not injected";
		assert minutes_field != null : "minutes_field field was not injected";
		assert seconds_field != null : "seconds_field field was not injected";
		assert warning_label != null : "warning_label field was not injected";
		assert days_count_choice != null : "days_count_choice field was not injected";
		assert date_time_hint_button != null : "date_time_hint_button field was not injected";
		assert days_count_hint_button != null : "days_count_hint_button field was not injected";
		
		years_field.setTextFormatter(text_formatters.get(0));
		months_field.setTextFormatter(text_formatters.get(1));
		days_field.setTextFormatter(text_formatters.get(2));
		hours_field.setTextFormatter(text_formatters.get(3));
		minutes_field.setTextFormatter(text_formatters.get(4));
		seconds_field.setTextFormatter(text_formatters.get(5));
		days_count_choice.setItems(FXCollections.observableArrayList(
				days_count_choice_values.values()));
		days_count_choice.setValue(days_count_choice_values.get(chosen_days_count));
		
		days_count_choice.setOnAction(new EventHandler<ActionEvent>()
		{
			@Override
			public void handle(final ActionEvent event)
			{
				// Выбранный пользователем вариант
				final String chosen_variant = days_count_choice.getValue();
				
				// Если выбранным типом подсчета является 360 дней в году
				if (chosen_variant.equals(
						days_count_choice_values.get(Days_in_year.DIY_360)))
				{
					chosen_days_count = Days_in_year.DIY_360;
				}
				// Выбранным типом подсчета является 365 дней в году
				else
				{
					chosen_days_count = Days_in_year.DIY_365;
				}
				
				warning_label.setText(null);
			}
		});
	}
	
	
	/**
	 * Обработчик события нажатия кнопки {@link #date_time_hint_button}. Выводит
	 * поясняющее сообщение над {@link #months_field} (т.е.&nbsp;посередине
	 * между всеми полями для ввода даты и времени).
	 */
	@FXML
	private void date_time_hint_button_on_action()
	{
		/* Explanation text. Length is reserved according to minimal strings
		 * length that will be contained in */
		final StringBuilder explanation_text =
				new StringBuilder(306 + 9 + 48 + 10 + 45);
		
		// 306 signs in default resource
		explanation_text.append(hints_resources.getString(
						"Init_Solo_counter_controller.date_time_hint.1"));
		// 9 signs
		explanation_text.append(Integer.MAX_VALUE);
		// 48 signs in default resource
		explanation_text.append(hints_resources.getString(
						"Init_Solo_counter_controller.date_time_hint.2"));
		// 10 signs in default resource
		explanation_text.append(
				labels_resources.getString("time_counter_settings.days_count"));
		// 45 signs in default resource
		explanation_text.append(hints_resources.getString(
						"Init_Solo_counter_controller.date_time_hint.3"));
		
		// Node to contain explanation text
		final Label label = new Label(explanation_text.toString());
		// Окно всплывающего сообщения
		final PopOver hint = new PopOver(label);
		
		hint.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		hint.setDetachable(false);
		hint.setCornerRadius(0);
		hint.setArrowSize(0);
		hint.show(months_field);
	}
	
	
	/**
	 * Обработчик события нажатия кнопки {@link #days_count_hint_button}.
	 * Выводит поясняющее сообщение над {@link #days_count_choice}.
	 */
	@FXML
	private void days_count_hint_button_on_action()
	{
		/* Explanation text. Length is reserved according to minimal strings
		 * length that will be contained in */
		final StringBuilder explanation_text =
				new StringBuilder(67 + 8 + 60 + 8 + 162);
		
		// 67 signs in default resource
		explanation_text.append(hints_resources.getString(
						"Init_Solo_counter_controller.days_count_hint.1"));
		// 8 signs in default resource
		explanation_text.append(time_counter_resources.getString("days_count.360"));
		// 60 signs in default resource
		explanation_text.append(hints_resources.getString(
						"Init_Solo_counter_controller.days_count_hint.2"));
		// 8 signs in default resource
		explanation_text.append(time_counter_resources.getString("days_count.365"));
		// 162 signs in default resource
		explanation_text.append(hints_resources.getString(
						"Init_Solo_counter_controller.days_count_hint.3"));
		
		// Node to contain explanation text
		final Label label = new Label(explanation_text.toString());
		// Окно всплывающего сообщения
		final PopOver hint = new PopOver(label);
		
		hint.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		hint.setDetachable(false);
		hint.setCornerRadius(0);
		hint.show(days_count_choice);
	}
}
