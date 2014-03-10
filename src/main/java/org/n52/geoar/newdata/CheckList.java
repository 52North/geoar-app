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
package org.n52.geoar.newdata;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckList<T> extends ArrayList<T> {

	public interface OnCheckedChangedListener<T> {
		void onCheckedChanged(T item, boolean newState);
	}

	public interface OnItemChangedListener<T> {
		void onItemAdded(T item);

		void onItemRemoved(T item);
	}

	public static abstract class OnItemChangedListenerWrapper<T> implements
			OnItemChangedListener<T> {
		public void onItemAdded(T item) {
			onItemChanged();
		};

		public void onItemRemoved(T item) {
			onItemChanged();
		};

		public void onItemChanged() {
		};
	}

	class Checker {
		private T item;

		private Checker(T item) {
			this.item = item;
		}

		void setChecked(boolean state) {
			checkItem(item, state);
		}

		boolean isChecked() {
			return CheckList.this.isChecked(item);
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface CheckManager {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public @interface CheckedChangedListener {
	}

	private static final long serialVersionUID = 1L;
	private Method checkedChangedMethod;

	private BitSet checkSet = new BitSet();
	private Set<OnCheckedChangedListener<T>> checkedChangeListeners = new HashSet<OnCheckedChangedListener<T>>();
	private Set<OnItemChangedListener<T>> itemChangeListeners = new HashSet<OnItemChangedListener<T>>();

	private Field checkManagerField;

	public CheckList(Class<?> itemClass) {
		for (Field field : itemClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(CheckManager.class)) {
				field.setAccessible(true);
				checkManagerField = field;
			}
		}
		for (Method method : itemClass.getDeclaredMethods()) {
			if (method.isAnnotationPresent(CheckedChangedListener.class)) {
				method.setAccessible(true);
				checkedChangedMethod = method;
			}
		}
	}

	public CheckList() {

	}

	@SuppressWarnings("unchecked")
	public <E> List<E> ofType(Class<E> clazz) {
		List<E> resultList = new ArrayList<E>();

		for (T item : this) {
			if (clazz.isAssignableFrom(item.getClass())) {
				resultList.add((E) item);
			}
		}

		return resultList;
	}

	public List<T> getCheckedItems() {
		List<T> resultList = new ArrayList<T>();
		for (int i = 0; i < size(); i++) {
			if (checkSet.get(i))
				resultList.add(get(i));
		}
		return resultList;
	}
	

	public List<T> getUncheckedItems() {
		List<T> resultList = new ArrayList<T>();
		for (int i = 0; i < size(); i++) {
			if (!checkSet.get(i))
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
		boolean changed = checkSet.get(index) != state;
		T item = get(index);
		checkSet.set(index, state);
		if (changed) {
			try {
				if (checkedChangedMethod != null) {
					checkedChangedMethod.invoke(item, state);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			notifyCheckedChanged(item, state);	
		}
	}

	@Override
	public void add(int index, T object) {
		injectFields(object);
		super.add(index, object);
		notifyItemAdded(object);
	};

	@Override
	public boolean add(T object) {
		injectFields(object);
		boolean changed = super.add(object);
		notifyItemAdded(object);
		return changed;
	};

	public void add(T object, boolean state) {
		add(object);
		checkItem(object, state);
	}

	private void injectFields(T object) {
		try {
			if (checkManagerField != null) {
				if (checkManagerField.get(object) != null) {
					throw new RuntimeException(
							"This item is already element of a CheckList!");
				}
				checkManagerField.set(object, new Checker(object));
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void checkItem(T item, boolean state) {
		if (contains(item)) {
			checkItem(this.indexOf(item), state);
		}
	}

	public void checkItem(T item) {
		checkItem(item, true);
	}

	private void notifyCheckedChanged(T item, boolean newState) {
		for (OnCheckedChangedListener<T> listener : checkedChangeListeners) {
			listener.onCheckedChanged(item, newState);
		}
	}

	private void notifyItemAdded(T item) {
		for (OnItemChangedListener<T> listener : itemChangeListeners) {
			listener.onItemAdded(item);
		}
	}

	private void notifyItemRemoved(T item) {
		for (OnItemChangedListener<T> listener : itemChangeListeners) {
			listener.onItemRemoved(item);
		}
	}

	public void addOnCheckedChangeListener(OnCheckedChangedListener<T> listener) {
		checkedChangeListeners.add(listener);
	}

	public void removeOnCheckedChangeListener(
			OnCheckedChangedListener<T> listener) {
		checkedChangeListeners.remove(listener);
	}

	public void addOnItemChangeListener(OnItemChangedListener<T> listener) {
		itemChangeListeners.add(listener);
	}

	public void removeOnItemChangeListener(OnItemChangedListener<T> listener) {
		itemChangeListeners.remove(listener);
	}

	public boolean isChecked(T item) {
		int index = indexOf(item);
		return isChecked(index);
	}

	public boolean isChecked(int index) {
		if (index >= 0) {
			return checkSet.get(index);
		} else {
			return false;
		}
	}

	@Override
	public T remove(int index) {
		for (int i = index, len = size() - 1; i < len; i++) {
			checkSet.set(index, checkSet.get(i + 1));
		}
		checkSet.set(size() - 1, false);
		T item = super.remove(index);
		try {
			if (checkManagerField != null) {
				checkManagerField.set(item, null);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		notifyItemRemoved(item);
		return item;
	}

	@Override
	public boolean remove(Object object) {
		int index = indexOf(object);
		if (index >= 0) {
			return remove(index) != null;
		} else {
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		checkSet.clear();
		super.clear();
		notifyItemRemoved(null);
	}

	public void checkAll(boolean state) {
		for (int i = 0, len = size(); i < len; i++) {
			checkItem(i, state);
		}
	}

	public boolean hasChecked() {
		return checkSet.cardinality() > 0;
	}
	
	public boolean allChecked() {
		return checkSet.cardinality() == size();
	}
}
