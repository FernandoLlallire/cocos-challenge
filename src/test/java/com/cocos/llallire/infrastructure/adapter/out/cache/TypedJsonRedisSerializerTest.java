package com.cocos.llallire.infrastructure.adapter.out.cache;

import static com.cocos.llallire.TestData.ARS;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.marketdata;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;

import com.cocos.llallire.application.dto.InstrumentPositionInformation;
import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.domain.model.Instrument;
import com.cocos.llallire.domain.model.Marketdata;
import com.cocos.llallire.infrastructure.config.CacheConfig;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

/**
 * Tests of {@link TypedJsonRedisSerializer}: every value type stored in Redis must survive a round trip
 * unchanged, including the scale of {@link BigDecimal} amounts.
 */
class TypedJsonRedisSerializerTest {

    private final JsonMapper jsonMapper = CacheConfig.cacheJsonMapper();
    private final TypeFactory types = jsonMapper.getTypeFactory();

    @Test
    void portfolioRoundTripKeepsAmountsAndScale() {
        PortfolioInformation portfolio = new PortfolioInformation(new BigDecimal("904784.00"),
                new BigDecimal("627500.00"), List.of(new InstrumentPositionInformation(47L, "PAMP",
                        "Pampa Holding S.A.", 40, new BigDecimal("37034.00"), new BigDecimal("-0.45"))));
        var serializer = new TypedJsonRedisSerializer<PortfolioInformation>(jsonMapper,
                types.constructType(PortfolioInformation.class));

        PortfolioInformation restored = serializer.deserialize(serializer.serialize(portfolio));

        assertThat(restored).isEqualTo(portfolio);
        assertThat(restored.availableCash().scale()).isEqualTo(2);
    }

    @Test
    void storesPlainJsonWithoutClassNames() {
        var serializer = new TypedJsonRedisSerializer<Instrument>(jsonMapper, types.constructType(Instrument.class));

        String json = new String(serializer.serialize(PAMP), StandardCharsets.UTF_8);

        assertThat(json).contains("\"ticker\":\"PAMP\"")
                .doesNotContain("@class")
                .as("derived isCash() is not data").doesNotContain("cash");
    }

    @Test
    void genericTypesRoundTrip() {
        var list = new TypedJsonRedisSerializer<List<Instrument>>(jsonMapper,
                types.constructCollectionType(List.class, Instrument.class));
        var cached = new TypedJsonRedisSerializer<CachedValue<Marketdata>>(jsonMapper,
                types.constructParametricType(CachedValue.class, Marketdata.class));
        CachedValue<Marketdata> present = CachedValue.of(marketdata(PAMP, "925.85", "921.80"));
        CachedValue<Marketdata> absent = CachedValue.of(null);

        assertThat(list.deserialize(list.serialize(List.of(PAMP, ARS)))).containsExactly(PAMP, ARS);
        assertThat(cached.deserialize(cached.serialize(present))).isEqualTo(present);
        assertThat(cached.deserialize(cached.serialize(absent))).isEqualTo(absent);
    }

    @Test
    void nullsAreKeptAsNullAndInvalidJsonFails() {
        var serializer = new TypedJsonRedisSerializer<Instrument>(jsonMapper, types.constructType(Instrument.class));

        assertThat(serializer.serialize(null)).isNull();
        assertThat(serializer.deserialize(null)).isNull();
        assertThatThrownBy(() -> serializer.deserialize("not json".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(SerializationException.class);
    }
}
