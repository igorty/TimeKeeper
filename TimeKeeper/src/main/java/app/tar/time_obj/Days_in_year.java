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
 * Days&#8209;in&#8209;year/days&#8209;in&#8209;month counting type.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public enum Days_in_year
{
	/** Total days amount in year is 360 (each month consists of 30&nbsp;days). */
	DIY_360,
	/** Total days amount in year is 365 (days amount in each month is as in
	 * normal year). */
	DIY_365
}
