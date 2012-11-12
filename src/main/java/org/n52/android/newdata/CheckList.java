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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckList<T> extends ArrayList<T> {

	public interface OnCheckedChangedListener<T> {
		void onCheckedChanged(T item, boolean newState);
	}

	private static final long serialVersionUID = 1L;

	private BitSet checkSet = new BitSet();
	private Set<OnCheckedChangedListener<T>> changeListeners = new HashSet<OnCheckedChangedListener<T>>();

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
		notifyCheckedChanged(get(index), state);
	}

	public void checkItem(T item, boolean state) {
		checkSet.set(this.indexOf(item), state);
		notifyCheckedChanged(item, state);
	}

	public void checkItem(T item) {
		checkItem(item, true);
	}

	private void notifyCheckedChanged(T item, boolean newState) {
		for (OnCheckedChangedListener<T> listener : changeListeners) {
			listener.onCheckedChanged(item, newState);
		}
	}

	public void addOnCheckedChangeListener(OnCheckedChangedListener<T> listener) {
		changeListeners.add(listener);
	}

	public void removeOnCheckedChangeListener(
			OnCheckedChangedListener<T> listener) {
		changeListeners.remove(listener);
	}

	public boolean isChecked(T item) {
		int index = indexOf(item);
		if (index >= 0) {
			return checkSet.get(index);
		} else {
			return false;
		}
	}

}
