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
 * Application's main package, which launches graphical shell.
 * <p>{@link graphical_shell.Main_class} launches the&nbsp;program and
 * represents main application's window. This class binds together:
 * <ul><li><i>{@link graphical_shell.Time_counters_pane}</i>, which provides
 * graphical representation and controls to all {@link time_obj.Time_counter}
 * objects, and manipulates them;</li>
 * <li><i>{@link graphical_shell.Top_pane}</i>, which provides menu&nbsp;bar and
 * tool&nbsp;bar</li>.</ul>
 * {@link graphical_shell.New_time_counter_window} is a&nbsp;wizard providing
 * step&nbsp;by&nbsp;step new time&nbsp;counter creating. It can be launched
 * from main&nbsp;window.
 * <p>{@link graphical_shell.New_time_counter_window} uses
 * {@link graphical_shell.FXML_controllers} package classes and their
 * fxml&#8209;resources to build its layout.
 * [TODO: Provide further description with the project advancement]
 * 
 * @since 1.0
 * @author Igor Taranenko
 */
package graphical_shell;
