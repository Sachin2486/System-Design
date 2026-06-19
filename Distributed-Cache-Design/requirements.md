Requirements
Our cache should support:

Basic operations:
Put(key, value, ttl): Add a key-value pair with a time-to-live (TTL).
Get(key): Retrieve the value for a key if it exists and hasn't expired.
Delete(key): Remove a key manually.

Eviction Policies:
LRU (Least Recently Used)
LFU (Least Frequently Used)
FIFO (First In First Out)
Evict entries when the cache reaches its maximum capacity.

TTL Expiry:
Automatically remove keys that have expired based on their TTL.
Code should be extensible and adhere best practices.

