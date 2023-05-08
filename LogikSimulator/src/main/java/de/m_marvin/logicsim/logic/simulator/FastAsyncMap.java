package de.m_marvin.logicsim.logic.simulator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class FastAsyncMap<K, V> implements Map<K, V> {
	
	protected final Function<FastAsyncMap<K, V>, Integer> allocationRegulator;
	
	protected int entryCount;
	protected K[] keyArr;
	protected V[] valueArr;
	
	public FastAsyncMap() {
		this(16, m -> 16);
	}
	
	@SuppressWarnings("unchecked")
	public FastAsyncMap(int initialCapacity, Function<FastAsyncMap<K, V>, Integer> allocationRegulator) {
		this.keyArr = (K[]) new Object[initialCapacity];
		this.valueArr = (V[]) new Object[initialCapacity];
		this.allocationRegulator = allocationRegulator;
	}
	
	protected int indexSearchKey(K key) {
		for (int i = 0; i < capacity(); i++) {
			if (Objects.equals(keyArr[i], key)) return i;
		}
		return -1;
	}

	protected int indexSearchValue(V value) {
		for (int i = 0; i < capacity(); i++) {
			if (Objects.equals(valueArr[i], value)) return i;
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	protected void increaseCapacity() {
		int finalSize = this.capacity() + this.allocationRegulator.apply(this);
		K[] nKeyArr = (K[]) new Object[finalSize];
		V[] nValueArr =  (V[]) new Object[finalSize];
		for (int i = 0; i < this.capacity(); i++) {
			nKeyArr[i] = this.keyArr[i];
			nValueArr[i] = this.valueArr[i];
		}
		this.keyArr = nKeyArr;
		this.valueArr = nValueArr;
	}
	
	public int capacity() {
		return keyArr.length;
	}
	
	@Override
	public int size() {
		return this.entryCount;
	}

	@Override
	public boolean isEmpty() {
		return this.entryCount == 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsKey(Object key) {
		return indexSearchKey((K) key) >= 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean containsValue(Object value) {
		return indexSearchValue((V) value) >= 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V get(Object key) {
		int index = indexSearchKey((K) key);
		if (index >= 0) {
			return this.valueArr[index];
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public V getOrDefault(Object key, V defaultValue) {
		int index = indexSearchKey((K) key);
		if (index >= 0) {
			return this.valueArr[index];
		}
		return defaultValue;
	}
	
	@Override
	public V put(K key, V value) {
		int index = indexSearchKey(key);
		if (index == -1) {
			index = indexSearchKey(null);
			entryCount++;
		}
		if (index == -1) {
			index = capacity();
			increaseCapacity();
		}
		V old = this.valueArr[index];
		this.keyArr[index] = key;
		this.valueArr[index] = value;
		return old;
	}

	@Override
	public V remove(Object key) {
		@SuppressWarnings("unchecked")
		int index = indexSearchKey((K) key);
		if (index >= 0) {
			this.keyArr[index] = null;
			V old = this.valueArr[index];
			this.valueArr[index] = null;
			this.entryCount--;
			return old;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (K key : m.keySet()) {
			put(key, m.get(key));
		}
	}

	@Override
	public void clear() {
		for (int i = 0; i < capacity(); i++) {
			this.keyArr[i] = null;
			this.valueArr[i] = null;
			this.entryCount = 0;
		}
	}

	@Override
	public Set<K> keySet() {
		return keySet;
	}

	@Override
	public Collection<V> values() {
		return this.valueCollection;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		return this.entrySet;
	}

	protected Set<K> keySet =  new Set<K>() {

		@Override
		public int size() {
			return FastAsyncMap.this.size();
		}

		@Override
		public Iterator<K> iterator() {
			return new Iterator<K>() {
				int index = -1;
				int nextIndex = -1;
				
				protected void findNext() {
					for (int i = index + 1; i < capacity(); i++) {
						if (FastAsyncMap.this.keyArr[i] != null) {
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
				public K next() {
					if (nextIndex == index) findNext();
					index = nextIndex;
					return FastAsyncMap.this.keyArr[index];
				}
			};
		}
		
		@Override
		public boolean isEmpty() {
			return FastAsyncMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return FastAsyncMap.this.containsKey(o);
		}

		@Override
		public Object[] toArray() {
			Object[] keys = new Object[FastAsyncMap.this.size()];
			int i = 0;
			for (K key : this) keys[i++] = key;
			return keys;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			int i = 0;
			for (K key : this) a[i++] = (T) key;
			return a;
		}
		
		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) if (!FastAsyncMap.this.containsKey(o)) return false;
			return true;
		}

		@Override
		public boolean add(K e) {
			return false;
		}

		@Override
		public boolean addAll(Collection<? extends K> c) {
			return false;
		}

		@Override
		public boolean remove(Object o) {
			return false;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return false;
		}
		
		@Override
		public void clear() {}
		
	};
	
	protected Collection<V> valueCollection = new Collection<V>() {

		@Override
		public int size() {
			return FastAsyncMap.this.size();
		}

		@Override
		public Iterator<V> iterator() {
			return new Iterator<V>() {
				int index = -1;
				int nextIndex = -1;
				
				protected void findNext() {
					for (int i = index + 1; i < capacity(); i++) {
						if (FastAsyncMap.this.keyArr[i] != null) {
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
				public V next() {
					if (nextIndex == index) findNext();
					index = nextIndex;
					return FastAsyncMap.this.valueArr[index];
				}
			};
		}
		
		@Override
		public boolean isEmpty() {
			return FastAsyncMap.this.isEmpty();
		}

		@Override
		public boolean contains(Object o) {
			return FastAsyncMap.this.containsValue(o);
		}

		@Override
		public Object[] toArray() {
			Object[] values = new Object[FastAsyncMap.this.size()];
			int i = 0;
			for (V value : this) values[i++] = value;
			return values;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			int i = 0;
			for (V values : this) a[i++] = (T) values;
			return a;
		}
		
		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) if (!FastAsyncMap.this.containsValue(o)) return false;
			return true;
		}
		
		@Override
		public boolean remove(Object o) {
			return false;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return false;
		}
		
		@Override
		public void clear() {}

		@Override
		public boolean add(V e) {
			return false;
		}

		@Override
		public boolean addAll(Collection<? extends V> c) {
			return false;
		}
		
	};
	
	protected Set<Entry<K, V>> entrySet = new Set<Map.Entry<K,V>>() {

		@Override
		public int size() {
			return FastAsyncMap.this.size();
		}
		
		@Override
		public boolean isEmpty() {
			return FastAsyncMap.this.isEmpty();
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean contains(Object o) {
			if (o instanceof Entry entry) {
				return FastAsyncMap.this.containsKey(entry.getKey());
			}
			return false;
		}

		@Override
		public boolean containsAll(Collection<?> c) {
			for (Object o : c) if (!this.contains(o)) return false;
			return true;
		}

		@Override
		public Iterator<Entry<K, V>> iterator() {
			return new Iterator<Map.Entry<K,V>>() {
				int index = -1;
				int nextIndex = -1;
				
				protected void findNext() {
					for (int i = index + 1; i < capacity(); i++) {
						if (FastAsyncMap.this.keyArr[i] != null) {
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
				public Entry<K, V> next() {
					if (nextIndex == index) findNext();
					index = nextIndex;
					return new Entry<K, V>() {

						@Override
						public K getKey() {
							return FastAsyncMap.this.keyArr[index];
						}

						@Override
						public V getValue() {
							return FastAsyncMap.this.valueArr[index];
						}

						@Override
						public V setValue(V value) {
							V old = FastAsyncMap.this.valueArr[index];
							FastAsyncMap.this.valueArr[index] = value;
							return old;
						}
						
					};
				}
				
			};
		}

		@Override
		public Object[] toArray() {
			Object[] values = new Object[FastAsyncMap.this.size()];
			int i = 0;
			for (Entry<K, V> value : this) values[i++] = value;
			return values;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T[] toArray(T[] a) {
			int i = 0;
			for (Entry<K, V> values : this) a[i++] = (T) values;
			return a;
		}
		
		@Override
		public boolean add(Entry<K, V> e) {
			return false;
		}

		@Override
		public boolean remove(Object o) {
			return false;
		}

		@Override
		public boolean addAll(Collection<? extends Entry<K, V>> c) {
			return false;
		}

		@Override
		public boolean retainAll(Collection<?> c) {
			return false;
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			return false;
		}

		@Override
		public void clear() {}
		
	};
	
	@Override
	public String toString() {
		Iterator<Entry<K,V>> i = entrySet().iterator();
		if (! i.hasNext())
			return "{}";

		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (;;) {
			Entry<K,V> e = i.next();
			K key = e.getKey();
			V value = e.getValue();
			sb.append(key   == this ? "(this Map)" : key);
			sb.append('=');
			sb.append(value == this ? "(this Map)" : value);
			if (! i.hasNext())
				return sb.append('}').toString();
			sb.append(',').append(' ');
        }
	}
	
}
