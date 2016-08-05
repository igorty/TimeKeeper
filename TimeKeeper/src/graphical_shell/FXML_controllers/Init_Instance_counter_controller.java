package graphical_shell.FXML_controllers;

import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import graphical_shell.GUI_settings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import time_obj.Instance_counter;
import time_obj.Mode;
import time_obj.Settings;
import time_obj.Settings.Locale_setting;


/**
 * {@link Instance_counter} initial&nbsp;values settings&nbsp;pane controller.
 * Called by {@link FXMLLoader} for
 * <i>"graphical_shell/FXML_controllers/Init_Instance_counter_layout.fxml"</i>
 * file.<br>
 * <i>Notes.</i>
 * <ul><li>Root pane in <i>"Init_Instance_counter_layout.fxml"</i> is
 * {@link GridPane}.</li>
 * <li><i>"Init_Instance_counter_layout.fxml"</i> requires
 * <i>"graphical_shell/resources/GUI_elements/labels.properties"</i> resources to
 * be set.</li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public class Init_Instance_counter_controller
{
	///// Поля private статические ========================================/////
	/** Отвечает за логирование событий. */
	private static final Logger logger;
	
	/** Graphic user interface settings. */
	private static final GUI_settings gui_settings;
	
	
	static
	{
		logger = Logger.getLogger(Init_Instance_counter_controller.class.getName());
		gui_settings = GUI_settings.get_instance();
	}
	
	
	///// Поля private экземпляра =========================================/////
	/** Поле для ввода года. */
	@FXML
	private TextField year_field;
	
	/** Поле для ввода месяца. */
	@FXML
	private TextField month_field;
	
	/** Поле для ввода дня. */
	@FXML
	private TextField day_field;
	
	/** Поля для ввода часа. */
	@FXML
	private TextField hour_field;
	
	/** Поле для ввода минуты. */
	@FXML
	private TextField minute_field;
	
	/** Поле для ввода секунды. */
	@FXML
	private TextField second_field;
	
	/** Поле выбора даты. */
	@FXML
	private DatePicker date_picker;
	
	/** Текстовое поле, содержащееся в {@link #date_picker}. */
	private TextField date_picker_text_field;
	
	/** Устанавливает текущие дату и время в соответствующие текстовые поля. */
	@FXML
	private Button now_button;
	
	/** Данное поле инициализируется при нажатии кнопки {@link #now_button} в
	 * методе {@link #now_button_on_action()}. При последующем изменении даты
	 * или времени в ГПИ данному полю присваивается {@code null}. Если при
	 * обращении к методу {@link #get_date_time()} данное поле
	 * не&nbsp;равно&nbsp;{@code null}&nbsp;&#0151; указанный метод вернет это
	 * поле. */
	private ZonedDateTime date_time_now;
	
	/** Выводит справку для элементов этой компоновки. */
	@FXML
	private Button hint_button;
	
	/** Текстовая метка, отображающая текст предупреждения в&nbsp;случае ввода
	 * некорректных значений даты и времени. */
	@FXML
	private Label warning_label;
	
	/** Содержит текстовые форматировщики для полей {@link #minute_field} и
	 * {@link #second_field}. */
	private final ArrayList<TextFormatter<String>> text_formatters;
	
	/** Числовое значение года, полученное из {@link #year_field}. Если
	 * {@link #year_field} не&nbsp;содержит значения&nbsp;&#0151; данному полю
	 * присваивается значение&nbsp;{@link Integer#MIN_VALUE}. */
	private int year_value;
	/* TODO: ? Вероятность исключения при нахождении поля "month_field" в фокусе
	 * и нажатии кнопки подтверждения создания указанных даты и времени, либо
	 * нажатия кнопки Next */
	/** Числовое значение месяца, полученное из {@link #month_field}. Если
	 * {@link #month_field} не&nbsp;содержит значения&nbsp;&#0151; данному полю
	 * присваивается значение&nbsp;{@link Integer#MIN_VALUE}. */
	private int month_value;
	/** Числовое значение дня, полученное из {@link #day_field}. Если
	 * {@link #day_field} не&nbsp;содержит значения&nbsp;&#0151; данному полю
	 * присваивается значение&nbsp;{@link Integer#MIN_VALUE}. */
	private int day_value;
	/** Числовое значение часа, полученное из {@link #hour_field}. Если
	 * {@link #hour_field} не&nbsp;содержит значения&nbsp;&#0151; данному полю
	 * присваивается значение&nbsp;{@code 0}. */
	private int hour_value;
	/** Числовое значение минуты, полученное из {@link #minute_field}. Если
	 * {@link #minute_field} не&nbsp;содержит значения&nbsp;&#0151; данному полю
	 * присваивается значение&nbsp;{@code 0}. */
	private int minute_value;
	/** Числовое значение секунды, полученное из {@link #second_field}. Если
	 * {@link #second_field} не&nbsp;содержит значения&nbsp;&#0151; данному полю
	 * присваивается значение&nbsp;{@code 0}. */
	private int second_value;
	
	
	///// Instance initializer ============================================/////
	{
		// Program locale
		final Locale_setting program_locale =
				Settings.get_instance().get_locale_setting();
		
		Locale.setDefault(new Locale(program_locale.language_code,
				program_locale.country_code, program_locale.variant_code));
		
		date_time_now = null;
		
		// Размер контейнера "text_formatters_init"
		final int text_formatters_size = 2;

		text_formatters = new ArrayList<>(text_formatters_size);
		
		// Инициализация элементов контейнера "text_formatters_init"
		for (int i = 1; i <= text_formatters_size; ++i)
		{
			text_formatters.add(
					new TextFormatter<>(new UnaryOperator<TextFormatter.Change>()
			{
				@Override
				public Change apply(final Change change)
				{
					// Содержимое текстового поля
					final String field_string = change.getControlNewText();
					// Кол-во символов содержимого текстового поля
					final int field_string_length = field_string.length();
					
					// Значение минут и секунд не может содержать более двух цифр
					if (field_string_length > 2)
					{
						return null;
					}
					
					/* true - данный TextFormatter принадлежит текстовому полю
					 * "minute_field"; false - текстовому полю "second_field".
					 * Это необходимо чтобы определить какому из полей
					 * ("minute_value" или "second_value") необходимо
					 * присваивать значение */
					final boolean is_minute_field =
							change.getControl().equals(minute_field);
					
					// Если текстовое поле не содержит символов
					if (field_string_length == 0)
					{
						warning_label.setText(null);
						
						/* Если данный TextFormatter принадлежит текстовому полю
						 * "minute_field" И уже установленное значение отличное
						 * от нуля */
						if (is_minute_field && minute_value != 0)
						{
							minute_value = 0;
							date_time_now = null;
						}
						/* If TextFormatter belongs to "second_field" text field
						 * AND already set value differs from 0 */
						else if (!is_minute_field && second_value != 0)
						{
							second_value = 0;
							date_time_now = null;
						}
						
						return change;
					}
					
					// Шаблон для парсинга текстовых строк
					final DecimalFormat text_field_format = new DecimalFormat("0");
					// Содержит положение в строке во время парсинга этой строки
					final ParsePosition parse_position = new ParsePosition(0);
					/* Число получившееся в результате парсинга строки
					 * "field_string" */
					final Number number =
							text_field_format.parse(field_string, parse_position);
					
					// Если не удалось прочитать число
					if (number == null)
					{
						return null;
					}
					
					// Числовое значение, содержащееся в поле для ввода
					final int field_value = number.intValue();
					
					// Если в поле для ввода содержатся нецифровые символы ИЛИ ...
					if (parse_position.getIndex() < field_string_length ||
							/* ... введенное значение времени не находится в
							 * диапазоне возможных значений */
							field_value > 59 || field_value < 0)
					{
						return null;
					}
					
					warning_label.setText(null);
					
					/* Если данный TextFormatter принадлежит текстовому полю
					 * "minute_field" И введенное значение отличается от уже
					 * существующего */
					if (is_minute_field && minute_value != field_value)
					{
						minute_value = field_value;
						date_time_now = null;
					}
					/* Condition: If TextFormatter belongs to "second_field"
					 * text field AND newly entered value differs from already
					 * existing */
					else if (!is_minute_field && second_value != field_value)
					{
						second_value = field_value;
						date_time_now = null;
					}
					
					return change;
				}
			}));
		}
		
		year_value = Integer.MIN_VALUE;
		month_value = Integer.MIN_VALUE;
		day_value = Integer.MIN_VALUE;
		hour_value = 0;
		minute_value = 0;
		second_value = 0;
	}
	
	
	///// Методы public экземпляра ========================================/////
	/**
	 * Возвращает введенные в панели компоновки дату и время. Если необходимые
	 * для заполнения поля даты не&nbsp;содержат полной информации, либо любое
	 * из полей содержит некорректные данные&nbsp;&#0151;
	 * возвращается&nbsp;{@code null}.<br>
	 * Предусмотрена вероятность ввода неоднозначных даты и времени, например
	 * когда они указывают на момент сезонного перевода времени. В этом случае
	 * метод создает диалоговое окно. Диалоговое окно может быть одного из двух
	 * видов в зависимости от ситуации:<br>
	 * <ul><li>Ситуация невалидного времени (например указание времени между
	 * {@code 2:00}&nbsp;и&nbsp;{@code 3:00} при переводе времени
	 * вперед)&nbsp;&#0151; выводится окно с сообщением о невалидном времени.
	 * После закрытия диалогового окна возвращается {@code null}.</li>
	 * <li>Ситуация "наложенного" времени (например указание времени между
	 * {@code 3:00}&nbsp;и&nbsp;{@code 4:00} при переводе времени
	 * назад)&nbsp;&#0151; выводится окно с сообщением о двух (если что и более)
	 * валидных значениях времени, различающихся часовым поясом.
	 * Во&nbsp;всплывающем окне предлагается выбор подходящего часового пояса, и
	 * после выбора метод возвращает сформированную дату и время. Если
	 * пользователь нажимает кнопку отмены/закрытия диалогового окна&nbsp;&#0151;
	 * возвращается&nbsp;{@code null}.</li></ul>
	 * 
	 * @return Введенные дату и время; {@code null}&nbsp;в&nbsp;указанных выше
	 * случаях.
	 */
	public ZonedDateTime get_date_time()
	{
		// Если были выставлены текущие дата и время
		if (date_time_now != null)
		{
			return date_time_now;
		}
		
		/* Resource bundle representing ".properties" resource which contains
		 * labels names */
		final ResourceBundle labels_resources = gui_settings.get_labels_resources();
		
		// Если не были введены необходимые значения либо введены неверные значения
		if (year_value == Integer.MIN_VALUE || month_value == Integer.MIN_VALUE ||
				day_value == Integer.MIN_VALUE)
		{
			warning_label.setText(
					labels_resources.getString("warning.incorrect_date_time"));
			
			return null;
		}
		
		// Устанавливаемое значение даты и времени
		final LocalDateTime date_time;
		
		try
		{
			date_time = LocalDateTime.of(year_value, month_value, day_value,
					hour_value, minute_value, second_value);
		}
		catch (final DateTimeException exc)
		{
			warning_label.setText(
					labels_resources.getString("warning.incorrect_date_time"));
			
			return null;
		}
		
		/* Resource bundle representing ".properties" resource which contains
		 * dialog messages */
		final ResourceBundle messages_resources =
				gui_settings.get_messages_resources();
		// Временная зона с локальными настройками сезонного перевода времени
		ZoneId zone_id;
		
		try
		{
			zone_id = ZoneId.systemDefault();
		}
		catch (final DateTimeException exc)
		{
			logger.log(Level.WARNING, "Cannot obtain system default ZoneId."
					+ " Exception stack trace:", exc);
			zone_id = ZoneId.ofOffset("UTC", ZoneOffset.UTC);
			
			// Диалоговое окно с сообщением об ошибке
			final Alert error = new Alert(AlertType.ERROR);
			
			error.setTitle(null);
			error.setContentText(messages_resources.getString("error.time_zone"));
			error.showAndWait();
		}
		
		// Список валидных временных зон для устанавливаемых даты и времени
		final List<ZoneOffset> valid_offsets =
				zone_id.getRules().getValidOffsets(date_time);
		// Кол-во валидных временных зон для устанавливаемых даты и времени
		final int valid_offsets_quantity = valid_offsets.size();
		
		// If date time value set cannot exist in specified time zone
		if (valid_offsets_quantity == 0)
		{
			// Диалоговое окно с сообщением
			final Alert notification = new Alert(AlertType.INFORMATION);
			
			notification.setTitle(null);
			notification.setHeaderText(messages_resources.getString(
					"notification.time_zone.nonexistent.header"));
			notification.setContentText(messages_resources.getString(
					"notification.time_zone.nonexistent.content"));
			notification.showAndWait();
			warning_label.setText(
					labels_resources.getString("warning.incorrect_date_time"));
			
			return null;
		}
		// If date time value set can exist in more than one time zone
		if (valid_offsets_quantity > 1)
		{
			// Уточняющее диалоговое окно
			final ChoiceDialog<ZoneOffset> clarification_dialog =
					new ChoiceDialog<ZoneOffset>(valid_offsets.get(0), valid_offsets);
			
			clarification_dialog.setTitle(null);
			clarification_dialog.setHeaderText(messages_resources.getString(
					"notification.time_zone.more_than_one.header"));
			
			/* Dialog content text. Length is reserved according to minimal
			 * strings length that will be contained in */
			final StringBuilder context_text = new StringBuilder(39 + 1 + 182);
			
			// 39 signs in default resource
			context_text.append(messages_resources.getString(
							"notification.time_zone.more_than_one.content.1"));
			// At least 1 symbol
			context_text.append(valid_offsets_quantity);
			// 182 signs in default resource
			context_text.append(messages_resources.getString(
							"notification.time_zone.more_than_one.content.2"));
			
			clarification_dialog.setContentText(context_text.toString());
			
			// Выбранный пользователем часовой пояс (если выбран)
			final Optional<ZoneOffset> choice = clarification_dialog.showAndWait();
			
			// Если пользователь сделал выбор часового пояса
			if (choice.isPresent())
			{
				return ZonedDateTime.ofStrict(date_time, choice.get(), zone_id);
			}
			
			warning_label.setText(
					labels_resources.getString("warning.incorrect_date_time"));
			
			return null;
		}
		
		return ZonedDateTime.of(date_time, zone_id);
	}
	
	
	/**
	 * Применяет метод {@link Button#fire()} к кнопке {@code Now} на панели
	 * компоновки.
	 */
	public void fire_now_button()
	{
		now_button.fire();
	}
	
	
	/**
	 * Makes current date&nbsp;time set button (<i>"Now"</i> button) disabled.
	 * The&nbsp;method is useful when creating time&nbsp;counter in
	 * {@link Mode#M_remains_till} mode, because there&nbsp;is no&nbsp;sense to
	 * set current date and time.
	 */
	public void make_now_button_disabled()
	{
		now_button.setDisable(true);
	}
	
	
	/**
	 * Устанавливает текст для всплывающей подсказки над кнопкой <i>Now</i>.
	 * 
	 * @param tooltip_text Текст всплывающей подсказки. Передача {@code null}
	 * убирает всплывающую подсказку.
	 */
	public void set_tooltip_to_now_button(final String tooltip_text)
	{
		// Если всплывающую подсказку необходимо убрать
		if (tooltip_text == null)
		{
			now_button.setTooltip(null);
			
			return;
		}
		
		// Всплывающая подсказка кнопки "Now"
		final Tooltip now_button_tooltip = now_button.getTooltip();
		
		// Если всплывающая подсказка не установлена
		if (now_button_tooltip == null)
		{
			now_button.setTooltip(new Tooltip(tooltip_text));
			
			return;
		}
		
		now_button_tooltip.setText(tooltip_text);
	}
	
	
	///// Methods private of-instance =====================================/////
	/**
	 * Called by {@link FXMLLoader}.
	 */
	@FXML
	private void initialize()
	{
		assert year_field != null : "year_field field was not injected";
		assert month_field != null : "month_field field was not injected";
		assert day_field != null : "day_field field was not injected";
		assert hour_field != null : "hour_field field was not injected";
		assert minute_field != null : "minute_field field was not injected";
		assert second_field != null : "second_field field was not injected";
		assert date_picker != null : "date_picker field was not injected";
		assert now_button != null : "now_button field was not injected";
		assert hint_button != null : "hint_button field was not injected";
		assert warning_label != null : "warning_label field was not injected";
		
		now_button.setText(gui_settings.get_buttons_resources().getString("now"));
		
		year_field.setTextFormatter(
				new TextFormatter<>(new UnaryOperator<TextFormatter.Change>()
		{
			@Override
			public Change apply(final Change change)
			{
				// Содержимое текстового поля
				final String field_string = change.getControlNewText();
				// Кол-во символов содержимого текстового поля
				final int field_string_length = field_string.length();
				
				// Значение года не должно содержать более 10-ти символов
				if (field_string_length > 10)
				{
					return null;
				}
				
				// Если текстовое поле не содержит символов
				if (field_string_length == 0)
				{
					warning_label.setText(null);
					year_value = Integer.MIN_VALUE;
					date_time_now = null;
					
					return change;
				}
				
				// Шаблон для парсинга текстовой строки
				final DecimalFormat text_field_format = new DecimalFormat("0");
				// Содержит положение в строке во время парсинга этой строки
				final ParsePosition parse_position = new ParsePosition(0);
				/* Число, получившееся в результате парсинга строки
				 * "change_string" */
				final Number number =
						text_field_format.parse(field_string, parse_position);
				
				/* Если первым символом введен знак "минус" И это единственный
				 * введенный символ */
				if (number == null && field_string_length == 1 &&
						field_string.charAt(0) == '-')
				{
					year_value = Integer.MIN_VALUE;
					date_time_now = null;
					
					return change;
				}
				
				// Если введен некорректный символ
				if (number == null)
				{
					return null;
				}
				
				// Числовое значение, содержащееся в поле для ввода
				final int field_value = number.intValue();
				
				// Если в текстовом поле содержатся нецифровые символы ИЛИ ...
				if (parse_position.getIndex() < field_string_length ||
						// ... значение года меньше минимально возможного ИЛИ ...
						field_value < Year.MIN_VALUE ||
						// ... значение года больше максимально возможного ИЛИ ...
						field_value > Year.MAX_VALUE ||
						/* ... (первой цифрой текстового поля является 0 И при
						 * этом пользователь пытается ввести другие символы
						 * (цифры)) ИЛИ ... */
						(field_string.charAt(0) == '0' && field_string_length > 1) ||
						/* ... (первым символом текстового поля является знак
						 * "минус" И при этом пользователь пытается ввести
						 * первой цифрой 0) */
						(field_string.charAt(0) == '-' && field_string.charAt(1) == '0'))
				{
					return null;
				}
				
				warning_label.setText(null);

				// Если уже существующее значение не совпадает с введенным
				if (year_value != field_value)
				{
					year_value = field_value;
					date_time_now = null;
				}
				
				return change;
			}
		}));
		
		month_field.focusedProperty().addListener(new ChangeListener<Boolean>()
		{
			@Override
			public void changed(final ObservableValue<? extends Boolean> observable,
					final Boolean old_value, final Boolean new_value)
			{
				// Если поле для ввода месяца потеряло фокус
				if (!new_value)
				{
					// Содержимое поля для ввода месяца
					String text = month_field.getText();
					
					// Если поле для ввода месяца пустое
					if (text == null || text.length() == 0)
					{
						month_value = Integer.MIN_VALUE;
						date_time_now = null;
						
						return;
					}
					
					text = text.trim();
					
					/* true - месяц прочитан, дальнейшее выполнение цикла
					 * не нужно; false - парсинг месяца необходимо продолжать */
					boolean month_parsed = false;
					// Кол-во используемых шаблонов для парсинга месяца
					final int parse_patterns = 4;
					
					/* Парсинг месяца. Выполняется попытка парсинга по шаблонах
					 * в указанном порядке: "LLLL" и "MMMM"; "LLL" и "MMM";
					 * "LL" и "MM"; "L" и "M" */
					for (int parse_attempt = 1; parse_attempt <= parse_patterns;
							++parse_attempt)
					{
						// Если месяц прочитан
						if (month_parsed)
						{
							return;
						}
						
						// Шаблон парсинга специфических представлений месяца
						final StringBuilder l_pattern = new StringBuilder(4);
						// Шаблон парсинга базовых представлений месяца
						final StringBuilder m_pattern = new StringBuilder(4);
						
						/* Приведение шаблонов парсинга к виду согласно текущей
						 * попытке */
						for (int i = 0, end = parse_patterns - parse_attempt;
								i <= end; ++i)
						{
							l_pattern.append('L');
							m_pattern.append('M');
						}
						
						// Форматировщик специфических представлений месяца
						DateTimeFormatter l_date_formatter =
								DateTimeFormatter.ofPattern(
										l_pattern.toString(), Locale.ENGLISH);
						
						month_parsed = parse(text, l_date_formatter);
						
						// Форматировщик базовых представлений месяца
						DateTimeFormatter m_date_formatter = null;
						
						// Если месяц еще не прочитан
						if (!month_parsed)
						{
							m_date_formatter = DateTimeFormatter.ofPattern(
									m_pattern.toString(), Locale.ENGLISH);
							month_parsed = parse(text, m_date_formatter);
						}
						
						// Если месяц еще не прочитан
						if (!month_parsed)
						{
							l_date_formatter =
									l_date_formatter.withLocale(Locale.getDefault());
							month_parsed = parse(text, l_date_formatter);
						}
						
						// Если месяц еще не прочитан
						if (!month_parsed)
						{
							m_date_formatter =
									m_date_formatter.withLocale(Locale.getDefault());
							month_parsed = parse(text, m_date_formatter);
						}
					}
					
					// Если месяц так и не был прочитан
					if (!month_parsed)
					{
						month_field.setStyle("-fx-text-fill: red");
						month_field.setText(
								gui_settings.get_text_fields_resources().getString(
										"month_field_incorrect"));
						month_value = Integer.MIN_VALUE;
						date_time_now = null;
					}
				}
				// Поле для ввода получило фокус
				else
				{
					warning_label.setText(null);

					/* Если числовое значение месяца не установлено - возможно
					 * в поле было выведено сообщение о некорректном вводе месяца */
					if (month_value == Integer.MIN_VALUE)
					{
						month_field.setText(null);
					}
					
					month_field.setStyle("-fx-text-fill: black");
				}
			}
			
			
			/**
			 * Парсит полученную строку указанным форматировщиком. При успешном
			 * парсинге устанавливает номер месяца в текстовое поле
			 * {@link #month_field}.
			 * 
			 * @param text Текст, который необходимо парсить.
			 * 
			 * @param date_formatter Форматировщик, применяемый к тексту.
			 * 
			 * @return {@code true}&nbsp;&#0151; месяц успешно обнаружен;
			 * {@code false}&nbsp;&#0151; парсинг завершился исключением.
			 * 
			 * @exception NullPointerException В&nbsp;качестве одного из
			 * аргументов передан {@code null}.
			 */
			boolean parse(final String text, final DateTimeFormatter date_formatter)
			{
				try
				{
					// Получившееся в результате парсинга число
					final int parsed_value =
							date_formatter.parse(text).get(ChronoField.MONTH_OF_YEAR);
					
					month_field.setText(Integer.toString(parsed_value));

					// Если уже существующее значение отличается от введенного
					if (month_value != parsed_value)
					{
						month_value = parsed_value;
						date_time_now = null;
					}
					
					return true;
				}
				catch (final DateTimeException exc)
				{
					return false;
				}
			}
		});
		
		day_field.setTextFormatter(
				new TextFormatter<>(new UnaryOperator<TextFormatter.Change>()
		{
			@Override
			public Change apply(final Change change)
			{
				// Содержимое текстового поля
				final String field_string = change.getControlNewText();
				// Кол-во символов содержимого текстового поля
				final int field_string_length = field_string.length();
				
				// Значение дня месяца не может содержать более двух цифр
				if (field_string_length > 2)
				{
					return null;
				}
				
				// Если текстовое поле не содержит символов
				if (field_string_length == 0)
				{
					warning_label.setText(null);
					day_value = Integer.MIN_VALUE;
					date_time_now = null;
					
					return change;
				}
				
				// Шаблон для парсинга текстовой строки
				final DecimalFormat text_field_format = new DecimalFormat("0");
				// Содержит положение в строке во время парсинга этой строки
				final ParsePosition parse_position = new ParsePosition(0);
				/* Число, получившееся в результате парсинга строки
				 * "change_string" */
				final Number number =
						text_field_format.parse(field_string, parse_position);
				
				// Если не удалось прочитать число
				if (number == null)
				{
					return null;
				}
				
				// Числовое значение, содержащееся в поле для ввода
				final int field_value = number.intValue();
				
				// Если в поле для ввода содержатся нецифровые символы ИЛИ ...
				if (parse_position.getIndex() < field_string_length ||
						/* ... введенный день месяца не находится в диапазоне
						 * возможных значений ИЛИ ... */
						field_value > 31 || field_value < 1 ||
						// ... пользователь пытается ввести первой цифрой 0
						field_string.charAt(0) == '0')
				{
					return null;
				}
				
				warning_label.setText(null);

				// Если уже существующее значение не совпадает с введенным
				if (day_value != field_value)
				{
					day_value = field_value;
					date_time_now = null;
				}
				
				return change;
			}
		}));
		
		hour_field.setTextFormatter(
				new TextFormatter<>(new UnaryOperator<TextFormatter.Change>()
		{
			@Override
			public Change apply(final Change change)
			{
				// Содержимое текстового поля
				final String field_string = change.getControlNewText();
				// Кол-во символов содержимого текстового поля
				final int field_string_length = field_string.length();
				
				// Значение часа не может содержать более двух цифр
				if (field_string_length > 2)
				{
					return null;
				}
				
				// Если текстовое поле не содержит символов
				if (field_string_length == 0)
				{
					warning_label.setText(null);

					// Если уже установленное значение отличное от нуля 
					if (hour_value != 0)
					{
						hour_value = 0;
						date_time_now = null;
					}
					
					return change;
				}
				
				// Шаблон для парсинга текстовой строки
				final DecimalFormat text_field_format = new DecimalFormat("0");
				// Содержит положение в строке во время парсинга этой строки
				final ParsePosition parse_position = new ParsePosition(0);
				/* Число, получившееся в результате парсинга строки
				 * "field_string" */
				final Number number =
						text_field_format.parse(field_string, parse_position);
				
				// Если не удалось прочитать число
				if (number == null)
				{
					return null;
				}
				
				// Числовое значение, содержащееся в поле для ввода
				final int field_value = number.intValue();
				
				// Если в поле для ввода содержатся нецифровые символы ИЛИ ...
				if (parse_position.getIndex() < field_string_length ||
						/* ... введенное значение часа не находится в диапазоне
						 * возможных значений */
						field_value > 23 || field_value < 0)
				{
					return null;
				}
				
				warning_label.setText(null);

				// Если уже установленное значение отличается от введенного
				if (hour_value != field_value)
				{
					hour_value = field_value;
					date_time_now = null;
				}
				
				return change;
			}
		}));
		
		minute_field.setTextFormatter(text_formatters.get(0));
		second_field.setTextFormatter(text_formatters.get(1));
		date_picker_text_field = date_picker.getEditor();
		date_picker_text_field.setDisable(true);
	}
	
	
	/**
	 * Обработчик события выбора даты для {@link #date_picker}. Устанавливает
	 * выбранную дату в текстовые поля {@link #year_field}, {@link #month_field},
	 * {@link #day_field}.
	 */
	@FXML
	private void date_picker_on_action()
	{
		// Если поле выбора даты находится в фокусе
		if (date_picker.isFocused())
		{
			warning_label.setText(null);
			
			// Выбранная дата
			final LocalDate date_picked = date_picker.getValue();
			
			date_picker_text_field.setText(null);
			
			/* If date picker has been used already it returns null because of
			 * its text is set to null by this method so need to check
			 * IF date picker returned null OR ( current date time were set AND... */
			if (date_picked == null || (date_time_now != null &&
					// ... date picker has same date)
					date_picked.equals(date_time_now.toLocalDate())))
			{
				return;
			}
			
			year_field.setText(Integer.toString(date_picked.getYear()));
			
			// Picked month value
			final int picked_month = date_picked.getMonthValue();
			
			month_field.setText(Integer.toString(picked_month));
			month_value = picked_month;
			day_field.setText(Integer.toString(date_picked.getDayOfMonth()));
			date_time_now = null;
		}
	}
	
	
	/**
	 * Обработчик события раскрытия поля для выбора даты {@link #date_picker}.
	 */
	@FXML
	private void date_picker_on_showing()
	{
		// Если значения для года, месяца и числа присвоены
		if (year_value != Integer.MIN_VALUE && month_value != Integer.MIN_VALUE &&
				day_value != Integer.MIN_VALUE)
		{
			date_picker.setValue(LocalDate.of(year_value, month_value, day_value));
		}
	}
	
	
	/**
	 * Обработчик события нажатия на кнопку {@link #now_button}. Устанавливает
	 * текущие дату и время в соответствующие текстовые поля.
	 */
	@FXML
	private void now_button_on_action()
	{
		/* TODO: !!! Возможно ли исключение "ZoneRulesException" или
		 * "DateTimeException"? */
		date_time_now = ZonedDateTime.now();
		year_value = date_time_now.getYear();
		month_value = date_time_now.getMonthValue();
		day_value = date_time_now.getDayOfMonth();
		hour_value = date_time_now.getHour();
		minute_value = date_time_now.getMinute();
		second_value = date_time_now.getSecond();
		
		year_field.setText(Integer.toString(year_value));
		month_field.setText(Integer.toString(month_value));
		day_field.setText(Integer.toString(day_value));
		hour_field.setText(Integer.toString(hour_value));
		minute_field.setText(Integer.toString(minute_value));
		second_field.setText(Integer.toString(second_value));
	}
	
	
	/**
	 * Обработчик события нажатия на кнопку {@link #hint_button}. Выводит
	 * всплывающее сообщение к текстовым полям этой компоновки.
	 */
	@FXML
	private void hint_button_on_action()
	{
		/* Resource bundle representing ".properties" resource which contains
		 * hints and tooltips texts */
		final ResourceBundle hints_resources = gui_settings.get_hints_resources();
		/* Resource bundle representing ".properties" resource which contains
		 * labels names */
		final ResourceBundle labels_resources = gui_settings.get_labels_resources();
		
		/* Explanation text. Length is reserved according to minimal strings
		 * length that will be contained in */
		final StringBuilder explanation_text = new StringBuilder(
				78 + 9 + 4 + 10 + 177 + 5 + 2 + 7 + 5 + 7 + 172 + 3 + 58 + 13 + 8);
		
		// 78 sings in default resource
		explanation_text.append(hints_resources.getString(
				"Init_Instance_counter_controller.hint.1"));
		// 10 signs
		explanation_text.append(Year.MIN_VALUE);
		// 4 signs in default resource
		explanation_text.append(hints_resources.getString(
				"Init_Instance_counter_controller.hint.2"));
		// 9 signs
		explanation_text.append(Year.MAX_VALUE);
		// 177 signs in default resource
		explanation_text.append(hints_resources.getString(
				"Init_Instance_counter_controller.hint.3"));
		// 5 signs in default resource
		explanation_text.append(labels_resources.getString(
				"Instance_counter_time_fields.hour"));
		// 2 signs in default resource
		explanation_text.append(hints_resources.getString(
				"Init_Instance_counter_controller.hint.4"));
		// 7 signs in default resource
		explanation_text.append(labels_resources.getString(
				"Instance_counter_time_fields.minute"));
		// 5 signs in default resource
		explanation_text.append(hints_resources.getString(
				"Init_Instance_counter_controller.hint.5"));
		// 7 signs in default resource
		explanation_text.append(labels_resources.getString(
				"Instance_counter_time_fields.second"));
		// 172 signs in default resource
		explanation_text.append(hints_resources.getString(
				"Init_Instance_counter_controller.hint.6"));
		// 3 signs in default resource
		explanation_text.append(now_button.getText());
		// 58 signs in default resource
		explanation_text.append(hints_resources.getString(
				"Init_Instance_counter_controller.hint.7"));
		// 13 signs in default resource
		explanation_text.append(
				gui_settings.get_time_counter_resources().getString("modes.elapsed_from"));
		// 8 signs in default resource
		explanation_text.append(hints_resources.getString(
				"Init_Instance_counter_controller.hint.8"));
		
		// Node to contain explanation text
		final Label label = new Label(explanation_text.toString());
		// Окно всплывающего сообщения
		final PopOver hint = new PopOver(label);
		
		hint.setArrowLocation(ArrowLocation.BOTTOM_CENTER);
		hint.setDetachable(false);
		hint.setCornerRadius(0);
		hint.setArrowSize(0);
		hint.show(hint_button);
	}
}
