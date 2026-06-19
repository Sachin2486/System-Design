import java.util.*;

public class CacheDemo {

	// Cache Entry

	static class CacheEntry <K,V> {
		private final K key;
		private V value;
		private long expiryTime;

		public CacheEntry(K key, V value,long expiryTime ) {
			this.key = key;
			this.value = value;
			this.expiryTime = expiryTime;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public long getExpiryTime() {
			return expiryTime;
		}

		public void setValue(V value) {
			this.value = value;
		}

		public void setExpiryTime(long expiryTime) {
			this.expiryTime = expiryTime;
		}

		public boolean isExpired() {
			return System.currentTimeMillis() >= expiryTime;
		}

	}

	// Eviction Policy
	interface EvictionPolicy<K> {

		void keyInserted(K key);

		void keyAccessed(K key);

		void removeKey(K key);

		K evictKey();
	}

	// FiFo EvictionPolicy

	static class FIFOEvictionPolicy<K> implements EvictionPolicy<K> {
		private final LinkedHashSet<K> keys = new LinkedHashSet<>();

		@Override
		public void keyInserted(K key) {
			keys.add(key);
		}

		@Override
		public void keyAccessed(K key) {
			//No-op
		}

		@Override
		public void removeKey(K key) {
			keys.remove(key);
		}

		@Override
		public K evictKey() {

			if(keys.isEmpty()) {
				return null;
			}

			Iterator<K> iterator = keys.iterator();
			K victim = iterator.next();

			iterator.remove();

			return victim;
		}
	}

	/// LRU ///

	static class LRUEvictionPolicy<K> implements EvictionPolicy<K> {

		private final LinkedHashSet<K> keys = new LinkedHashSet<>();

		@Override
		public void keyInserted(K key) {
			keys.add(key);
		}

		@Override
		public void keyAccessed(K key) {

			if (!keys.contains(key)) {
				return;
			}

			keys.remove(key);
			keys.add(key);
		}

		@Override
		public void removeKey(K key) {
			keys.remove(key);
		}

		@Override
		public K evictKey() {

			if (keys.isEmpty()) {
				return null;
			}

			Iterator<K> iterator = keys.iterator();

			K victim = iterator.next();

			iterator.remove();

			return victim;
		}
	}

	static class LFUEvictionPolicy<K> implements EvictionPolicy<K> {

		private final Map<K, Integer> keyFrequency = new HashMap<>();

		private final Map<Integer, LinkedHashSet<K>> frequencyMap =
		    new HashMap<>();

		private int minFrequency = 1;

		@Override
		public void keyInserted(K key) {

			keyFrequency.put(key, 1);

			frequencyMap
			.computeIfAbsent(1, k -> new LinkedHashSet<>())
			.add(key);

			minFrequency = 1;
		}

		@Override
		public void keyAccessed(K key) {

			Integer freq = keyFrequency.get(key);

			if (freq == null) {
				return;
			}

			LinkedHashSet<K> currentKeys =
			    frequencyMap.get(freq);

			currentKeys.remove(key);

			if (currentKeys.isEmpty()) {

				frequencyMap.remove(freq);

				if (freq == minFrequency) {
					minFrequency++;
				}
			}

			int newFreq = freq + 1;

			keyFrequency.put(key, newFreq);

			frequencyMap
			.computeIfAbsent(newFreq,
			                 k -> new LinkedHashSet<>())
			.add(key);
		}

		@Override
		public void removeKey(K key) {

			Integer freq = keyFrequency.remove(key);

			if (freq == null) {
				return;
			}

			LinkedHashSet<K> keys =
			    frequencyMap.get(freq);

			if (keys == null) {
				return;
			}

			keys.remove(key);

			if (keys.isEmpty()) {
				frequencyMap.remove(freq);
			}
		}

		@Override
		public K evictKey() {

			LinkedHashSet<K> keys =
			    frequencyMap.get(minFrequency);

			if (keys == null || keys.isEmpty()) {
				return null;
			}

			Iterator<K> iterator = keys.iterator();

			K victim = iterator.next();

			iterator.remove();

			keyFrequency.remove(victim);

			if (keys.isEmpty()) {
				frequencyMap.remove(minFrequency);
			}

			return victim;
		}
	}

	// Cache

	static class Cache<K, V> {

		private final int capacity;

		private final Map<K, CacheEntry<K, V>> storage;

		private final EvictionPolicy<K> evictionPolicy;

		public Cache(int capacity,
		             EvictionPolicy<K> evictionPolicy) {

			this.capacity = capacity;
			this.evictionPolicy = evictionPolicy;
			this.storage = new HashMap<>();
		}

		public void put(K key,
		                V value,
		                long ttlMillis) {

			cleanupExpiredKeys();

			long expiryTime =
			    System.currentTimeMillis() + ttlMillis;

			if (storage.containsKey(key)) {

				CacheEntry<K, V> existing =
				    storage.get(key);

				existing.setValue(value);
				existing.setExpiryTime(expiryTime);

				evictionPolicy.keyAccessed(key);

				return;
			}

			if (storage.size() >= capacity) {

				K victim =
				    evictionPolicy.evictKey();

				if (victim != null) {
					storage.remove(victim);
				}
			}

			CacheEntry<K, V> entry =
			    new CacheEntry<>(
			    key,
			    value,
			    expiryTime
			);

			storage.put(key, entry);

			evictionPolicy.keyInserted(key);
		}

		public Optional<V> get(K key) {

			CacheEntry<K, V> entry =
			    storage.get(key);

			if (entry == null) {
				return Optional.empty();
			}

			if (entry.isExpired()) {

				delete(key);

				return Optional.empty();
			}

			evictionPolicy.keyAccessed(key);

			return Optional.of(entry.getValue());
		}

		public void delete(K key) {

			storage.remove(key);

			evictionPolicy.removeKey(key);
		}

		private void cleanupExpiredKeys() {

			List<K> expiredKeys =
			    new ArrayList<>();

			for (Map.Entry<K, CacheEntry<K, V>> entry :
			        storage.entrySet()) {

				if (entry.getValue().isExpired()) {
					expiredKeys.add(entry.getKey());
				}
			}

			for (K key : expiredKeys) {
				delete(key);
			}
		}

		public void printCache() {

			System.out.println("Current Cache:");

			for (Map.Entry<K, CacheEntry<K, V>> entry :
			        storage.entrySet()) {

				System.out.println(
				    entry.getKey()
				    + " -> "
				    + entry.getValue().getValue()
				);
			}

			System.out.println();
		}
	}


	// Driver

	public static void main(String[] args) throws Exception {

		Cache<Integer, String> cache =
		    new Cache<>(
		    3,
		    new LRUEvictionPolicy<>()
		);

		cache.put(1, "A", 10000);
		cache.put(2, "B", 10000);
		cache.put(3, "C", 10000);

		cache.get(1);

		cache.put(4, "D", 10000);

		System.out.println(cache.get(1)); // A
		System.out.println(cache.get(2)); // Empty

		cache.printCache();

		System.out.println("Testing TTL");

		cache.put(5, "TEMP", 2000);

		Thread.sleep(3000);

		System.out.println(cache.get(5)); // Empty
	}
}


