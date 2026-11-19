package com.cocos.llallire.infrastructure.config;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.application.port.out.PortfolioCachePort;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.domain.model.User;
import com.cocos.llallire.infrastructure.adapter.out.cache.CacheCircuitBreaker;
import com.cocos.llallire.infrastructure.adapter.out.cache.CacheMetrics;
import com.cocos.llallire.infrastructure.adapter.out.cache.CacheNames;
import com.cocos.llallire.infrastructure.adapter.out.cache.CacheRegion;
import com.cocos.llallire.infrastructure.adapter.out.cache.CachedValue;
import com.cocos.llallire.infrastructure.adapter.out.cache.CachingInstrumentPort;
import com.cocos.llallire.infrastructure.adapter.out.cache.CachingMarketdataPort;
import com.cocos.llallire.infrastructure.adapter.out.cache.CachingUserPort;
import com.cocos.llallire.infrastructure.adapter.out.cache.PortfolioCacheAdapter;
import com.cocos.llallire.infrastructure.adapter.out.cache.TypedJsonRedisSerializer;
import com.cocos.llallire.infrastructure.adapter.out.persistence.InstrumentPersistenceAdapter;
import com.cocos.llallire.infrastructure.adapter.out.persistence.MarketdataPersistenceAdapter;
import com.cocos.llallire.infrastructure.adapter.out.persistence.UserPersistenceAdapter;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

/**
 * Cache wiring.
 *
 * <p>The application layer only knows the ports. Here the JPA adapters are wrapped with cache-aside decorators,
 * which are registered as the {@link Primary} implementation of each port, so the use cases get the cached
 * version without any change.</p>
 */
@Configuration
@EnableConfigurationProperties(AppCacheProperties.class)
public class CacheConfig {

    /** Prefix of every Redis key written by this application. */
    static final String REDIS_KEY_PREFIX = "cocos:";

    /**
     * Cache manager selected by {@code app.cache.type}.
     *
     * @param properties        cache settings
     * @param connectionFactory Redis connection factory; only used when the type is REDIS
     * @return Redis, in-memory or no-op cache manager
     */
    @Bean
    public CacheManager cacheManager(AppCacheProperties properties,
            ObjectProvider<RedisConnectionFactory> connectionFactory) {
        return switch (properties.type()) {
            case REDIS -> redisCacheManager(properties.ttl(), connectionFactory.getObject());
            case MEMORY -> new ConcurrentMapCacheManager(CacheNames.ALL.toArray(String[]::new));
            case NONE -> new NoOpCacheManager();
        };
    }

    /**
     * Circuit breaker shared by all cache regions.
     *
     * @param properties cache settings
     * @param clock      system clock
     * @param metrics    cache metrics
     * @return breaker
     */
    @Bean
    public CacheCircuitBreaker cacheCircuitBreaker(AppCacheProperties properties, Clock clock, CacheMetrics metrics) {
        return new CacheCircuitBreaker(properties.failureCooldown(), clock, metrics);
    }

    /**
     * Cached users port.
     *
     * @param persistence    JPA adapter
     * @param cacheManager   cache manager
     * @param circuitBreaker shared cache circuit breaker
     * @param metrics        cache metrics
     * @return decorator used by the use cases
     */
    @Bean
    @Primary
    public LoadUserPort cachingUserPort(UserPersistenceAdapter persistence, CacheManager cacheManager,
            CacheCircuitBreaker circuitBreaker,
            CacheMetrics metrics) {
        return new CachingUserPort(persistence, new CacheRegion<>(cacheManager, CacheNames.USERS, circuitBreaker, metrics));
    }

    /**
     * Cached instruments port.
     *
     * @param persistence    JPA adapter
     * @param cacheManager   cache manager
     * @param circuitBreaker shared cache circuit breaker
     * @param metrics        cache metrics
     * @return decorator used by the use cases
     */
    @Bean
    @Primary
    public LoadInstrumentPort cachingInstrumentPort(InstrumentPersistenceAdapter persistence,
            CacheManager cacheManager, CacheCircuitBreaker circuitBreaker,
            CacheMetrics metrics) {
        return new CachingInstrumentPort(persistence,
                new CacheRegion<>(cacheManager, CacheNames.INSTRUMENTS, circuitBreaker, metrics),
                new CacheRegion<>(cacheManager, CacheNames.INSTRUMENT_SEARCH, circuitBreaker, metrics));
    }

    /**
     * Cached marketdata port.
     *
     * @param persistence    JPA adapter
     * @param cacheManager   cache manager
     * @param circuitBreaker shared cache circuit breaker
     * @param metrics        cache metrics
     * @return decorator used by the use cases
     */
    @Bean
    @Primary
    public LoadMarketdataPort cachingMarketdataPort(MarketdataPersistenceAdapter persistence,
            CacheManager cacheManager, CacheCircuitBreaker circuitBreaker,
            CacheMetrics metrics) {
        return new CachingMarketdataPort(persistence,
                new CacheRegion<>(cacheManager, CacheNames.MARKETDATA, circuitBreaker, metrics));
    }

    /**
     * Portfolio cache.
     *
     * @param cacheManager   cache manager
     * @param circuitBreaker shared cache circuit breaker
     * @param metrics        cache metrics
     * @return portfolio cache adapter
     */
    @Bean
    public PortfolioCachePort portfolioCachePort(CacheManager cacheManager, CacheCircuitBreaker circuitBreaker,
            CacheMetrics metrics) {
        return new PortfolioCacheAdapter(
                new CacheRegion<>(cacheManager, CacheNames.PORTFOLIO, circuitBreaker, metrics),
                new CacheRegion<>(cacheManager, CacheNames.PORTFOLIO_GENERATION, circuitBreaker, metrics));
    }

    /**
     * Builds the Redis cache manager: one configuration per region with its TTL and typed JSON serializer.
     * Regions not listed here are not created.
     *
     * @param ttl               TTL per region
     * @param connectionFactory Redis connection factory
     * @return Redis cache manager
     */
    private RedisCacheManager redisCacheManager(AppCacheProperties.Ttl ttl,
            RedisConnectionFactory connectionFactory) {
        JsonMapper jsonMapper = cacheJsonMapper();
        TypeFactory types = jsonMapper.getTypeFactory();
        Map<String, RedisCacheConfiguration> regions = Map.of(
                CacheNames.USERS, region(jsonMapper, types.constructType(User.class), ttl.users()),
                CacheNames.INSTRUMENTS, region(jsonMapper, types.constructType(Instrument.class), ttl.instruments()),
                CacheNames.INSTRUMENT_SEARCH, region(jsonMapper,
                        types.constructCollectionType(List.class, Instrument.class), ttl.instrumentSearch()),
                CacheNames.MARKETDATA, region(jsonMapper,
                        types.constructParametricType(CachedValue.class, Marketdata.class), ttl.marketdata()),
                CacheNames.PORTFOLIO, region(jsonMapper, types.constructType(PortfolioInformation.class),
                        ttl.portfolio()),
                CacheNames.PORTFOLIO_GENERATION, region(jsonMapper, types.constructType(String.class),
                        ttl.portfolioGeneration()));
        return RedisCacheManager.builder(connectionFactory)
                .withInitialCacheConfigurations(regions)
                .disableCreateOnMissingCache()
                .build();
    }

    /**
     * JSON mapper for cache values. Derived {@code isX()} methods of the domain records (for example,
     * {@code Instrument.isCash()}) are not data, so they are not written.
     *
     * @return mapper used by every region
     */
    public static JsonMapper cacheJsonMapper() {
        return JsonMapper.builder()
                .changeDefaultVisibility(visibility -> visibility.withIsGetterVisibility(Visibility.NONE))
                .build();
    }

    /**
     * Builds the Redis configuration of one region.
     *
     * @param jsonMapper JSON mapper
     * @param type       value type of the region
     * @param ttl        time to live
     * @return region configuration: string keys prefixed with {@value #REDIS_KEY_PREFIX}, typed JSON values,
     *         no {@code null} values
     */
    private RedisCacheConfiguration region(JsonMapper jsonMapper, JavaType type, Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .prefixCacheNameWith(REDIS_KEY_PREFIX)
                .serializeKeysWith(SerializationPair.fromSerializer(RedisSerializer.string()))
                .serializeValuesWith(SerializationPair.fromSerializer(new TypedJsonRedisSerializer<>(jsonMapper, type)));
    }
}
