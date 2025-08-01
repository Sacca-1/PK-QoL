package com.wildyqol;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Text;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Singleton
public class FcCacheService
{
	private final WildyQoLConfig config;
	private final Map<String, Instant> cache = new ConcurrentHashMap<>();

	@Inject
	FcCacheService(WildyQoLConfig config)
	{
		this.config = config;
	}

	/**
	 * Cache a player name as a friends chat member
	 * @param name The player name to cache
	 */
	public void cache(String name)
	{
		if (name == null || name.trim().isEmpty())
		{
			return;
		}
		String normalizedName = Text.standardize(name);
		cache.put(normalizedName, Instant.now());
		log.debug("Cached FC member: {}", normalizedName);
	}

	/**
	 * Check if a player name is cached as a friends chat member
	 * @param name The player name to check
	 * @return true if the player is cached and not expired
	 */
	public boolean isCached(String name)
	{
		if (name == null || name.trim().isEmpty())
		{
			return false;
		}
		String normalizedName = Text.standardize(name);
		Instant timestamp = cache.get(normalizedName);
		if (timestamp == null)
		{
			return false;
		}

		long minutes = Duration.between(timestamp, Instant.now()).toMinutes();
		if (minutes >= config.fcCacheMinutes())
		{
			cache.remove(normalizedName);
			log.debug("Expired FC cache entry: {}", normalizedName);
			return false;
		}
		return true;
	}

	/**
	 * Remove expired entries from the cache
	 */
	public void purgeExpired()
	{
		long ttl = config.fcCacheMinutes();
		Instant now = Instant.now();
		int removed = 0;
		
		for (Map.Entry<String, Instant> entry : cache.entrySet())
		{
			if (Duration.between(entry.getValue(), now).toMinutes() >= ttl)
			{
				cache.remove(entry.getKey());
				removed++;
			}
		}
		
		if (removed > 0)
		{
			log.debug("Purged {} expired FC cache entries", removed);
		}
	}

	/**
	 * Get the number of cached entries
	 * @return The number of cached entries
	 */
	public int getCacheSize()
	{
		return cache.size();
	}

	/**
	 * Clear all cached entries
	 */
	public void clear()
	{
		cache.clear();
		log.debug("Cleared FC cache");
	}
} 