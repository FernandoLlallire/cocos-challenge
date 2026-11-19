package com.cocos.llallire.application.service;

import static com.cocos.llallire.TestData.ARS;
import static com.cocos.llallire.TestData.PAMP;
import static com.cocos.llallire.TestData.USER_ID;
import static com.cocos.llallire.TestData.cashIn;
import static com.cocos.llallire.TestData.marketdata;
import static com.cocos.llallire.TestData.order;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.out.LoadInstrumentPort;
import com.cocos.llallire.application.port.out.LoadMarketdataPort;
import com.cocos.llallire.application.port.out.LoadOrderPort;
import com.cocos.llallire.application.port.out.LoadUserPort;
import com.cocos.llallire.domain.exception.UserNotFoundException;
import com.cocos.llallire.domain.model.OrderSide;
import com.cocos.llallire.domain.model.OrderStatus;
import com.cocos.llallire.domain.model.User;
import com.cocos.llallire.domain.service.PortfolioCalculator;

/**
 * Tests of {@link GetPortfolio}. Uses the real {@link PortfolioCalculator} and mocks the outbound ports.
 */
@ExtendWith(MockitoExtension.class)
class GetPortfolioTest {

    @Mock
    private LoadUserPort userPort;
    @Mock
    private LoadOrderPort orderPort;
    @Mock
    private LoadInstrumentPort instrumentPort;
    @Mock
    private LoadMarketdataPort marketdataPort;

    private final RecordingPortfolioCache portfolioCache = new RecordingPortfolioCache();
    private GetPortfolio getPortfolio;

    @BeforeEach
    void setUp() {
        getPortfolio = new GetPortfolio(userPort, orderPort, instrumentPort, marketdataPort,
                new PortfolioCalculator(), portfolioCache);
    }

    @Test
    void goesThroughThePortfolioCacheAndDoesNotInvalidateIt() {
        when(userPort.loadUserById(USER_ID)).thenReturn(Optional.of(new User(USER_ID, "a@b.com", "1")));
        when(orderPort.loadOrdersByUserIdAndStatuses(any(), any())).thenReturn(List.of());

        getPortfolio.execute(USER_ID);

        assertThat(portfolioCache.loaderCalls).isEqualTo(1);
        assertThat(portfolioCache.invalidatedUsers).isEmpty();
    }

    @Test
    void loadsFilledAndPendingOrdersAndBuildsThePortfolio() {
        when(userPort.loadUserById(USER_ID)).thenReturn(Optional.of(new User(USER_ID, "a@b.com", "1")));
        when(orderPort.loadOrdersByUserIdAndStatuses(USER_ID, OrderStatus.affectingHoldings())).thenReturn(List.of(
                cashIn(10_000),
                order(PAMP, OrderSide.BUY, OrderStatus.FILLED, 5, "900", 1)));
        when(instrumentPort.loadInstrumentsByIds(List.of(ARS.id(), PAMP.id()))).thenReturn(List.of(ARS, PAMP));
        when(marketdataPort.loadLastMarketdataByInstrumentIds(List.of(ARS.id(), PAMP.id())))
                .thenReturn(Map.of(PAMP.id(), marketdata(PAMP, "1000", "990")));

        PortfolioInformation portfolio = getPortfolio.execute(USER_ID);

        assertThat(portfolio.availableCash()).isEqualByComparingTo("5500");
        assertThat(portfolio.totalAccountValue()).isEqualByComparingTo("10500");
        assertThat(portfolio.positions()).singleElement().satisfies(position -> {
            assertThat(position.ticker()).isEqualTo("PAMP");
            assertThat(position.quantity()).isEqualTo(5);
            assertThat(position.marketValue()).isEqualByComparingTo("5000");
            assertThat(position.totalReturnPct()).isEqualByComparingTo("11.11");
        });
    }

    @Test
    void userWithoutOrdersHasAnEmptyPortfolioAndNoExtraQueries() {
        when(userPort.loadUserById(USER_ID)).thenReturn(Optional.of(new User(USER_ID, "a@b.com", "1")));
        when(orderPort.loadOrdersByUserIdAndStatuses(any(), any())).thenReturn(List.of());

        PortfolioInformation portfolio = getPortfolio.execute(USER_ID);

        assertThat(portfolio.positions()).isEmpty();
        assertThat(portfolio.totalAccountValue()).isEqualByComparingTo("0");
        verify(instrumentPort, never()).loadInstrumentsByIds(any());
        verify(marketdataPort, never()).loadLastMarketdataByInstrumentIds(any());
    }

    @Test
    void failsWhenUserDoesNotExist() {
        when(userPort.loadUserById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getPortfolio.execute(99L)).isInstanceOf(UserNotFoundException.class);
        verify(orderPort, never()).loadOrdersByUserIdAndStatuses(any(), any());
    }
}
