/**
 * Copyright 2012 52°North Initiative for Geospatial Open Source Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.android.newdata;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class CheckList<T> extends ArrayList<T> {
	private static final long serialVersionUID = 1L;

	BitSet checkSet = new BitSet();

	public List<T> getCheckedItems() {
		List<T> resultList = new ArrayList<T>();
		for (int i = 0; i < size(); i++) {
			if (checkSet.get(i))
				resultList.add(get(i));
		}
		return resultList;
	}

	@SuppressWarnings("unchecked")
	public <E> List<E> getCheckedItems(Class<E> clazz) {
		List<E> resultList = new ArrayList<E>();
		for (int i = 0; i < size(); i++) {
			if (checkSet.get(i)) {
				T item = get(i);
				if (clazz.isAssignableFrom(item.getClass())) {
					resultList.add((E) item);
				}
			}
		}
		return resultList;
	}

	public void checkItem(int index, boolean state) {
		checkSet.set(index, state);
	}

	public void checkItem(T item, boolean state) {
		checkSet.set(this.indexOf(item), state);
	}

}
