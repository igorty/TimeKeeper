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
package time_obj.dialog;

import time_obj.events.User_notification_listener;


/**
 * Possible notification types which can be passed with
 * {@link User_notification_listener#user_notification_occurred(time_obj.events.User_notification_event, User_notification_type, String)}
 * method.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public enum User_notification_type
{
	/** Error occurred during IO&nbsp;operation performing. */
	UNT_IO_error,
	/** Deserialized file contains incorrect values. */
	UNT_file_error,
	/** Cannot obtain system time&nbsp;zone rules. */
	UNT_time_zone_error,
	/** Informing notification. */
	UNT_informing
}
