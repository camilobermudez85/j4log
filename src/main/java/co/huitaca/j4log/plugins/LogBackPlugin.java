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
package co.huitaca.j4log.plugins;

import java.security.ProtectionDomain;
import java.util.Map;

import co.huitaca.j4log.J4LogPlugin;

public class LogBackPlugin extends J4LogPlugin {

    @Override
    public int countLoggers() {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public Map<String, String> getLoggers() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public int countLoggersLike(String like) {
	// TODO Auto-generated method stub
	return 0;
    }

    @Override
    public Map<String, String> getLoggersLike(String like) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public void setLevel(String logger, String level) {
	// TODO Auto-generated method stub

    }

    @Override
    public String getLevel(String logger) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public boolean contains(String logger) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public String[] getObservedClasses() {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public byte[] onClassLoaded(String className, ClassLoader classLoader, ProtectionDomain protectionDomain,
	    byte[] classfileBuffer) {
	// TODO Auto-generated method stub
	return null;
    }

}
