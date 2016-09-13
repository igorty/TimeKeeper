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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;

import app.tar.time_obj.Settings.Locale_setting;


/**
 * Provides full time&nbsp;unit names according to current <i>program locale</i>
 * which stored in {@link Settings} object.<br>
 * The&nbsp;problem of full time&nbsp;unit names is that they <u>depend on</u>
 * the&nbsp;time&nbsp;unit value. For&nbsp;example in English, <i>year</i>
 * time&nbsp;unit is "years" when it comes to plural form (i.e.&nbsp;when its
 * value ending is from&nbsp;2&nbsp;to&nbsp;9). But "years" is used in values
 * ending in&nbsp;11 and&nbsp;0 too. Other languages may have their own rules.<br>
 * <b>Important!</b>
 * <ul><li>This class is closely related to
 * the&nbsp;<i>app/tar/time_obj/time_counter_resources[locale].properties</i>
 * files. Full time&nbsp;unit names in these files are represented by common
 * key&nbsp;<i>TUL_full_name</i>. Its sub&nbsp;keys have <i>numeric value keys</i>
 * which are used by supported locales. So some languages may&nbsp;not specify
 * all <i>TUL_full_name</i> keys listed in <i>default .properties</i>, while
 * other languages use all of them. It may happen that it would&nbsp;not be
 * enough for some new languages to represent all time&nbsp;unit name numeric
 * forms with existing keys. In this case additional keys <u>must</u> be added
 * in <i>the&nbsp;default .properties file</i> as&nbsp;well as in
 * the&nbsp;specified new&nbsp;one with the&nbsp;aim to prevent possible
 * exceptions.</li>
 * <li>When additional locale is added to {@link Settings.Locale_setting}, this
 * class <u>must</u> implement additional case in
 * {@link #get_name(Time_unit_name, long)} method according to the&nbsp;newly
 * created locale. Otherwise an&nbsp;exception can be thrown when newly created
 * locale is switched&nbsp;on.</li></ul>
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
class Time_unit_full_name_resource_provider
{
	///// Fields default-access static ====================================/////
	/** Program locale. */
	static Locale_setting locale;
	
	/** Resource bundle representing <i>.properties</i> file which contains
	 * resources for time&nbsp;counters. */
	static ResourceBundle time_counter_resources;
	
	
	///// Fields private static ===========================================/////
	/** Contains full&nbsp;name keys from {@link #time_counter_resources}
	 * resource bundle.<br>
	 * <b>Warning!</b> The&nbsp;container is <u>immutable</u>. An&nbsp;attempt
	 * to modify its content causes runtime exception. */
	private final static Map<Time_unit_name, String> full_name_keys;
	
	
	static
	{
		// Program settings
		Settings settings = Settings.get_instance();
		
		locale = settings.get_locale_setting();
		time_counter_resources = settings.get_time_counter_resources();
		
		// Key names to be initialized in "full_name_keys" container
		final String[] key_names = { "TUL_full_name.years",
				"TUL_full_name.months", "TUL_full_name.days",
				"TUL_full_name.hours", "TUL_full_name.minutes",
				"TUL_full_name.seconds" };
		// All "Time_unit_name" enumerated constants
		final Time_unit_name[] time_unit_names_values = Time_unit_name.values();
		
		assert key_names.length == time_unit_names_values.length :
			"Array size with values doesn\'t match with "
				+ Time_unit_name.class.getName() + " enumerated constants quantity";
		
		// "full_name_keys" container initializer
		final Map<Time_unit_name, String> full_name_keys_init =
				new EnumMap<>(Time_unit_name.class);
		
		// "full_name_keys_init" container initialization
		for (final Time_unit_name i : time_unit_names_values)
		{
			full_name_keys_init.put(i, key_names[i.ordinal()]);
		}
		
		full_name_keys = Collections.unmodifiableMap(full_name_keys_init);
	}
	
	
	
	///// Methods default-access static ===================================/////
	/**
	 * Returns appropriate name for given {@code time_unit} based on
	 * the&nbsp;{@code time_unit_value}.
	 * 
	 * @param time_unit Time&nbsp;unit which appropriate name need to be
	 * returned.
	 * 
	 * @param time_unit_value Time&nbsp;unit value to determine proper name for.
	 * 
	 * @return Appropriate name based on given arguments.
	 * 
	 * @exception IllegalArgumentException {@code time_unit_value} argument has
	 * <u>negative value</u>.
	 * 
	 * @exception NullPointerException {@code time_unit} argument is
	 * {@code null}.
	 */
	static String get_name(
			final Time_unit_name time_unit, final long time_unit_value)
	{
		if (time_unit == null)
		{
			throw new NullPointerException(
					Time_unit_name.class.getName() + " argument is null");
		}
		
		// The argument cannot be negative
		if (time_unit_value < 0)
		{
			throw new IllegalArgumentException(
					"time_unit_value argument has negative value");
		}
		
		// Choosing appropriate time unit name
		switch (locale)
		{
		case LS_default:
		case LS_English:
			// If time unit value ends in 11 OR ends from 2 to 0
			if (time_unit_value % 100 == 11 || time_unit_value % 10 != 1)
			{
				return time_counter_resources.getString(
						full_name_keys.get(time_unit) + ".2");
			}
			
			return time_counter_resources.getString(full_name_keys.get(time_unit) + ".1");
			
			
		case LS_Russian:
		case LS_Ukrainian:
			// Remainder of the division to determine time unit value ending
			int division_remainder = (int)(time_unit_value % 100);
			
			switch (division_remainder)
			{
			case 11:
			case 12:
			case 13:
			case 14:
				return time_counter_resources.getString(
						full_name_keys.get(time_unit) + ".5");
			}
			
			division_remainder = (int)(time_unit_value % 10);
			
			switch (division_remainder)
			{
			case 1:
				return time_counter_resources.getString(
						full_name_keys.get(time_unit) + ".1");
				
			case 2:
			case 3:
			case 4:
				return time_counter_resources.getString(
						full_name_keys.get(time_unit) + ".2");
				
			// Other values from 5 to 0
			default:
				return time_counter_resources.getString(
						full_name_keys.get(time_unit) + ".5");
			}
			
		
		default:
			throw new EnumConstantNotPresentException(
					Locale_setting.class, locale.name());
		}
	}
}
