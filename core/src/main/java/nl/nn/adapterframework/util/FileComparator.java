/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.io.File;
import java.util.Comparator;

/**
 * @author Jaco de Groot
 */
public class FileComparator implements Comparator<File> {

	@Override
	public int compare(File file1, File file2) {
		long l = file1.lastModified() - file2.lastModified();
		if (l < 0) {
			return -1;
		} if (l > 0) {
			return 1;
		} else {
			return 0;
		}
	}

}
