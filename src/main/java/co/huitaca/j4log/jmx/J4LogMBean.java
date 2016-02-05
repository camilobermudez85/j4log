/*
 * Copyright 2016 Camilo Bermúdez
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package co.huitaca.j4log.jmx;

import java.util.Map;

import co.huitaca.j4log.LogLevel;

public interface J4LogMBean {

	public int countLoggers();

	public Map<String, LogLevel> getLoggers();

	public Map<String, LogLevel> getLoggers(int offset, int size);

	public int countLoggersLike(String like);

	public Map<String, LogLevel> getLoggersLike(String like);

	public Map<String, LogLevel> getLoggersLike(String like, int offset, int page);

	public void setLevel(String logger, LogLevel level);

	public LogLevel getLevel(String logger);

}
