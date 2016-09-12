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
/**
 * Provides <i>time&nbsp;counters</i> which may work in different
 * {@link time_obj.Mode}{@code s}.
 * <p>The basic package class is abstract {@link time_obj.Time_counter} class.
 * It encapsulates primary functionality. The&nbsp;class contains calculated
 * time&nbsp;difference and is responsible for its representing in
 * a&nbsp;text&nbsp;string value. Time representation depends on
 * {@link time_obj.Time_display_style}, {@link time_obj.Time_unit_layout} and
 * displayed time&nbsp;units range setting. Each {@link time_obj.Time_counter}
 * may have its own time layout settings. These settings may be changed
 * on&nbsp;the&nbsp;fly after time&nbsp;counter creation.
 * <p>For the&nbsp;case of {@link time_obj.Time_unit_layout#TUL_full_name}
 * layout there is auxiliary
 * {@link time_obj.Time_unit_full_name_resource_provider} class implemented (see
 * its description for clarification).
 * <p>The&nbsp;class may store time&nbsp;counter user&#8209;defined description,
 * so GUI&nbsp;implementation may easily implement writing and retrieving it
 * without necessity to create additional storage.
 * <p>{@link time_obj.Solo_counter} and
 * {@link time_obj.Instance_counter} extend {@link time_obj.Time_counter} class.
 * <p>{@link time_obj.Solo_counter} implements <i>stopwatch</i>
 * ({@link time_obj.Mode#M_stopwatch}) and <i>countdown</i>
 * ({@link time_obj.Mode#M_countdown}) modes. <i>Stopwatch</i> may be
 * initialized not&nbsp;only with {@code 0}&nbsp;time&nbsp;unit&nbsp;values:
 * initial value may&nbsp;be any. Of&nbsp;course <i>timer</i> may have any
 * initial value too. When <i>timer</i> reaches {@code 0}, it continues counting
 * showing negative time&nbsp;value.
 * <p>Time&nbsp;value may be edited after {@link time_obj.Solo_counter} object
 * creation, no&nbsp;matter it is running or paused. Time&nbsp;value editing has
 * form of adding/subtracting specified time. <i>Timer</i> time&nbsp;value may
 * be even turned to elapsed&nbsp;(negative), while <i>stopwatch</i>
 * may&nbsp;not: minimal value for it is&nbsp;just&nbsp;{@code 0}.
 * The&nbsp;class provides time&nbsp;counter restart possibility too.
 * <p>{@link time_obj.Instance_counter} implements so&nbsp;called
 * <i>elapsed&nbsp;from</i> ({@link time_obj.Mode#M_elapsed_from}) and
 * <i>remains&nbsp;till</i> ({@link time_obj.Mode#M_remains_till}) modes.
 * The&nbsp;class is strongly based on {@link java.time}&nbsp;API. It calculates
 * time elapsed&nbsp;from/remaining&nbsp;till specified instant. The&nbsp;class
 * stores common {@link java.time.ZoneId} for all its instances. Time&nbsp;zone
 * rules are <u>taken into account</u> when calculating
 * the&nbsp;time&nbsp;difference, so there will be no&nbsp;time&nbsp;"jumping"
 * when clocks are moved from "winter" to "summer" time and vice&nbsp;versa.
 * <p>The feature of both supported {@link time_obj.Mode}{@code s}
 * initialization, that they may be initialized with "elapsed" values.
 * Here&nbsp;is the&nbsp;explanation. Let's&nbsp;say today is the
 * 1st&nbsp;of&nbsp;January,&nbsp;<u>2016</u>. We&nbsp;want time&nbsp;counter to
 * count time elapsed from 1st&nbsp;of&nbsp;January,&nbsp;<u>2010</u>. In this
 * case it simply shows <u>positive</u> elapsed&nbsp;time. But we can set (for
 * some convenience) target date&nbsp;time that <u>has&nbsp;not</u>
 * come&nbsp;yet. In case of {@link time_obj.Mode#M_elapsed_from}
 * time&nbsp;counter initially will show <u>negative</u> difference (target
 * date&nbsp;time will be in future, so <u>there&nbsp;no&nbsp;positive
 * "elapsed&nbsp;from" difference</u>!), and after reaching the&nbsp;target
 * time&nbsp;point value became <u>positive</u>.
 * {@link time_obj.Mode#M_remains_till} has fully <u>opposite</u> behavior.
 * 
 * <h2>How time&nbsp;difference is represented to the&nbsp;user</h2>
 * The feature of time&nbsp;representing is that it will be displayed as
 * <i>3&nbsp;months&nbsp;15&nbsp;days&nbsp;5&nbsp;hours</i> instead of
 * <i>105&nbsp;days&nbsp;5&nbsp;hours</i> or <i>2,525&nbsp;hours</i>
 * for&nbsp;example. There&nbsp;is user&#8209;convenient conversion performed.
 * <p>{@link time_obj.Solo_counter} and {@link time_obj.Instance_counter} has
 * some differences.
 * <p>{@link time_obj.Solo_counter} may provide counting in <u>two</u>&nbsp;ways.
 * They are represented by {@link time_obj.Days_in_year} enumeration.
 * The&nbsp;key difference between them is only in days&#8209;in&#8209;month
 * quantity. {@link time_obj.Days_in_year#DIY_360} has {@code 30}&nbsp;days in
 * each month, while {@link time_obj.Days_in_year#DIY_365} has months quantity
 * to form {@code 365}&#8209;days year. Each {@link time_obj.Solo_counter} is
 * set with {@link time_obj.Days_in_year} value at creation time, and this value
 * <u>cannot</u> be changed later.
 * <p>{@link time_obj.Instance_counter} counts time&nbsp;difference in
 * the&nbsp;following manner. Let's&nbsp;say today is
 * <i>the&nbsp;1st&nbsp;of&nbsp;May,&nbsp;2016</i> and the&nbsp;target
 * date&nbsp;time is <i>the&nbsp;14th&nbsp;of&nbsp;July&nbsp;the&nbsp;same
 * year</i>. The&nbsp;time&nbsp;difference will be represented as
 * <i>2&nbsp;months&nbsp;13&nbsp;days</i>.
 * 
 * <h2>Auxiliary classes</h2>
 * All {@link time_obj.Time_counter} objects are controlled by
 * {@link time_obj.Time_counter_control} class, which is implemented as
 * a&nbsp;singleton. Especially it concerns to {@link time_obj.Instance_counter}
 * objects. {@link time_obj.Time_counter_control} provides synchronous
 * per&#8209;second execution to them (because current&nbsp;time is mutual for
 * all!). But {@link time_obj.Solo_counter} objects have individual
 * per&#8209;1/10th&nbsp;second task execution (however user&#8209;provided
 * accuracy is only {@code 1}&nbsp;second).
 * <p>Each {@link time_obj.Time_counter} puts itself to
 * {@link time_obj.containers.Modified_ArrayList} container at
 * the&nbsp;initialization time, which is stored in mentioned
 * {@link time_obj.Time_counter_control} class.
 * <p>The last meaningful class that deserves attention is
 * {@link time_obj.Settings}. It stores {@link time_obj} package necessary
 * settings such as {@link time_obj.Time_counter}'s values default layout and
 * resource&nbsp;bundle <i>.properties</i> objects according to the&nbsp;current
 * application's language (there are 3&nbsp;implemented languages for now:
 * <i>English</i>, <i>Russian</i> and <i>Ukrainian</i>).
 * 
 * @since 1.0
 * @author Igor Taranenko
 */
package time_obj;
