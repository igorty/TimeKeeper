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


/**
 * Possible time&nbsp;units names.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public enum Time_unit_layout
{
	/** Signs (like&nbsp;<i>"</i> for seconds) are represented with
	 * time&nbsp;units' numeric&nbsp;values. Is the&nbsp;default
	 * time&nbsp;units names scheme. */
	TUL_value_sign,
	/** Short names are represented with time&nbsp;units' numeric&nbsp;values. */
	TUL_short_name,
	/** Full names are represented with time&nbsp;units' numeric&nbsp;values. */
	TUL_full_name,
	/** Only time&nbsp;units' numeric&nbsp;values are shown. */
	TUL_digits_only
}
