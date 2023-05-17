  package de.m_marvin.logicsim.logic.simulator;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Function;

public class AsyncArrayList<E> extends AbstractCollection<E> implements List<E> {

	protected final Function<AsyncArrayList<E>, Integer> allocationRegulator;
	
	protected int entryCount;
	protected E[] elementArr;

	public AsyncArrayList() {
		this(16, a -> 16);
	}
	
	@SuppressWarnings("unchecked")
	public AsyncArrayList(int initialCapacity, Function<AsyncArrayList<E>, Integer> allocationRegulator) {
		this.elementArr = (E[]) new Object[initialCapacity];
		this.allocationRegulator = allocationRegulator;
	}
	
	public AsyncArrayList(E[] array) {
		this(array, a -> 16);
	}
	
	public AsyncArrayList(E[] array, Function<AsyncArrayList<E>, Integer> allocationRegulator) {
		this.elementArr = array;
		this.allocationRegulator = allocationRegulator;
	}

	@SuppressWarnings("unchecked")
	protected void increaseCapacity() {
		int finalSize = this.capacity() + this.allocationRegulator.apply(this);
		E[] nArr = (E[]) new Object[finalSize];
		for (int i = 0; i < this.capacity(); i++) {
			nArr[i] = this.elementArr[i];
		}
		this.elementArr = nArr;
	}

	public int capacity() {
		return elementArr.length;
	}
	
	@Override
	public int size() {
		return this.entryCount;
	}
	
	@Override
	public boolean add(E e) {
		add(size(), e);
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		c.forEach(e -> add(size(), e));
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		for (E entry : c) add(index++, entry);
		return true;
	}

	@Override
	public E get(int index) {
		if (index >= this.entryCount) throw new IndexOutOfBoundsException(index);
		return this.elementArr[index];
	}
	
	@Override
	public E set(int index, E element) {
		if (index >= this.entryCount) throw new IndexOutOfBoundsException(index);
		E old = this.elementArr[index];
		this.elementArr[index] = element;
		return old;
	}

	@Override
	public void add(int index, E element) {
		if (index > this.entryCount) throw new IndexOutOfBoundsException(index);
		if (this.elementArr.length <= this.entryCount + 1) increaseCapacity();
		if (index != this.entryCount) {
			for (int i = index; i < this.entryCount; i++) this.elementArr[i + 1] = this.elementArr[i];
		}
		this.elementArr[index] = element;
		this.entryCount++;
	}
	
	@Override
	public boolean remove(Object o) {
		for (int i = 0; i < this.entryCount; i++) {
			if (Objects.equals(this.elementArr[i], o)) {
				remove(i);
				return true;
			}
		}
		return false;
	}

	@Override
	public E remove(int index) {
		if (index >= this.entryCount) throw new IndexOutOfBoundsException(index);
		E old = this.elementArr[index];
		for (int i = index; i < this.entryCount - 1; i++) this.elementArr[i] = this.elementArr[i + 1];
		this.entryCount--;
		return old;
	}

	@Override
	public int indexOf(Object o) {
		for (int i = 0; i < this.entryCount; i++) {
			if (Objects.equals(this.elementArr[i], o)) return i;
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		for (int i = this.entryCount - 1; i >= 0; i--) {
			if (Objects.equals(this.elementArr[i], o)) return i;
		}
		return -1;
	}
	
	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		if (fromIndex < 0) throw new IndexOutOfBoundsException(fromIndex);
		if (toIndex >= size()) throw new IndexOutOfBoundsException(toIndex);
		@SuppressWarnings("unchecked")
		E[] subArr = (E[]) new Object[toIndex - fromIndex];
		for (int i = fromIndex; i <= toIndex; i++) {
			subArr[i - fromIndex] = get(i);
		}
		return new AsyncArrayList<>(subArr, this.allocationRegulator);
	}
	
	@Override
	public void clear() {
		this.entryCount = 0;
	}

	@Override
	public Iterator<E> iterator() {
		return new Iterator<E>() {
			int index = -1;
			int nextIndex = -1;
			
			protected void findNext() {
				for (int i = index + 1; i < size(); i++) {
					if (AsyncArrayList.this.elementArr[i] != null) {
						nextIndex = i;
						break;
					}
				}
			}
			
			@Override
			public boolean hasNext() {
				if (nextIndex == index) findNext();
				return nextIndex != index;
			}
			
			@Override
			public E next() {
				if (nextIndex == index) findNext();
				index = nextIndex;
				return AsyncArrayList.this.elementArr[index];
			}
		};
	}

	@Override
	public ListIterator<E> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
