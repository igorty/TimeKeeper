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
 * Стиль отображения счетчика времени целиком.
 * 
 * @version 1.0
 * @author Igor Taranenko
 */
public enum Time_display_style
{
	/** Отображаются <u>только</u> те единицы времени, которые "имеют вес"
	 * (т.е.&nbsp;значение которых достигнуто). Так если текущим значением в
	 * режиме секундомера является 10&nbsp;минут 0&nbsp;секунд&nbsp;&#0151;
	 * отображаться будут только минуты и секунды. Аналогично для других
	 * режимов подсчета времени. */
	TDS_if_reaches,
	/** Отображаются все единицы времени, даже если значение времени
	 * является меньше. */
	TDS_show_all,
	/** Выбирается <u>строгий</u> диапазон отображаемых единиц времени. Другие
	 * единицы времени не&nbsp;будут отображаться даже в случае выхода значения
	 * времени за указанный диапазон единиц. */
	TDS_custom_strict,
	/** Выбирается <u>нестрогий</u> диапазон отображаемых единиц времени.
	 * Единицы большего значения, не&nbsp;входящие в диапазон, будут
	 * отображаться только в случае выхода значения времени за указанный
	 * диапазон единиц. Является стилем отображения счетчика времени
	 * по&nbsp;умолчанию. */
	TDS_increase_able
}
