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
 * Time counter work mode.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public enum Mode
{
	/** Stopwatch mode. */
	M_stopwatch,
	/** Timer mode. */
	M_countdown,
	/** Counting time elapsed from specified instant. */
	M_elapsed_from,
	/** Counting time remaining to specified instant. */
	M_remains_till
}
