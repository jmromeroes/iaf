/*
   Copyright 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.statistics;

import lombok.Getter;

/**
 * Counter value that is maintained with statistics.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class ScalarStatistic extends GaugeBase {

	private @Getter long value;

	public synchronized void increase() {
		++value;
	}
	public synchronized void decrease() {
		--value;
	}

}
