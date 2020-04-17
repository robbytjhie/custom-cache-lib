package com.custom.cache.lib.service;

import java.util.ArrayList;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.map.LRUMap;


public class CustomInMemoryCache <K, T> {

   private long timeToLiveInMillis;

   private LRUMap cacheMap;

   protected class CachedObject {
      public long lastAccessed = System.currentTimeMillis();
      public T value;

      protected CachedObject(T value) {
         this.value = value;
      }
   }
   /*
      Here are the characteristic of the program:
	  - Items will expire based on a time to live period.
      - Cache will keep most recently used items.
      - If you try to add more items then max specified, apache common collections has a LRUMap, which, removes the least used entries 
        from a fixed sized map.
      - For the expiration of items we can timestamp the last access and in a separate thread remove the items when the time to live 
        limit is reached. This is nice for reducing memory pressure for applications that have long idle time in between accessing the cached objects.
      - This custom in memory cache is thread safe.
    */

   public CustomInMemoryCache(long timeToLiveInSeconds, final long timerIntervalInSeconds, 
     int maxItems) {
      this.timeToLiveInMillis = timeToLiveInSeconds * 1000;

      cacheMap = new LRUMap(maxItems);

      if (timeToLiveInMillis > 0 && timerIntervalInSeconds > 0) {

         Thread t = new Thread(new Runnable() {
            public void run() {
               while (true) {
                  try {
                     Thread.sleep(timerIntervalInSeconds * 1000);
                  }
                  catch (InterruptedException ex) {
                  }

                  cleanup();
               }
            }
         });

         t.setDaemon(true);
         t.start();
      }
   }

   //Added a record into the cache
   public void put(K key, T value) {
      synchronized (cacheMap) {
         cacheMap.put(key, new CachedObject(value));
      }
   }

   //fetch a record from the cache
   public T get(K key) {
      synchronized (cacheMap) {
         CachedObject c = (CachedObject) cacheMap.get(key);

         if (c == null)
            return null;
         else {
            c.lastAccessed = System.currentTimeMillis();
            return c.value;
         }
      }
   }

   //remove a record from the cache
   public void remove(K key) {
      synchronized (cacheMap) {
         cacheMap.remove(key);
      }
   }

   //check number of records inside the cache
   public int size() {
      synchronized (cacheMap) {
         return cacheMap.size();
      }
   }

   //performs clean up based on least used record and record beyond it's time to live
   //this method is useful to reduce the number of cache which growing overtime
   @SuppressWarnings("unchecked")
   public void cleanup() {

      long now = System.currentTimeMillis();
      ArrayList<K> keysToDelete = null;

      synchronized (cacheMap) {
         MapIterator itr = cacheMap.mapIterator();

         keysToDelete = new ArrayList<K>((cacheMap.size() / 2) + 1);
         K key = null;
         CachedObject c = null;

         while (itr.hasNext()) {
            key = (K) itr.next();
            c = (CachedObject) itr.getValue();

            if (c != null && (now > (timeToLiveInMillis + c.lastAccessed))) {
               keysToDelete.add(key);
            }
         }
      }

      for (K key : keysToDelete) {
         synchronized (cacheMap) {
            cacheMap.remove(key);
         }

         Thread.yield();
      }
   }
}