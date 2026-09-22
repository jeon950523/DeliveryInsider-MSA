package com.deliveryinsider.report.domain.report.service;

import com.deliveryinsider.report.domain.report.mapper.ReportReadMapper;
import com.deliveryinsider.report.domain.report.projection.ReportSummaryProjection;
import com.deliveryinsider.report.domain.report.request.ReportSummaryRequest;
import com.deliveryinsider.report.integration.store.CurrentStoreClient;
import com.deliveryinsider.report.integration.store.CurrentStoreResponse;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportEntitlementTest {

    @Test
    void subscription이_없어도_기본리포트는_매장소유권만_확인한다() {
        CurrentStoreClient currentStoreClient =
            mock(CurrentStoreClient.class);

        ReportReadMapper reportReadMapper =
            mock(ReportReadMapper.class);

        when(
            currentStoreClient.findByUserId(8L)
        ).thenReturn(
            new CurrentStoreResponse(
                3L,
                "fixture-store"
            )
        );

        ReportSummaryProjection summary =
            mock(ReportSummaryProjection.class);

        when(
            reportReadMapper.findSummary(
                3L,
                null,
                null,
                null
            )
        ).thenReturn(summary);

        when(
            reportReadMapper.findFinancialDataStatuses(
                3L,
                null,
                null,
                null
            )
        ).thenReturn(java.util.List.of());

        ReportReadService service =
            new ReportReadService(
                currentStoreClient,
                reportReadMapper,
                mock(com.deliveryinsider.report.integration.platform.PlatformFinancialClient.class),
                new ReportMenuProfitCalculator()
            );

        service.getSummary(
            8L,
            new ReportSummaryRequest(
                null,
                null,
                null
            )
        );

        verify(
            reportReadMapper
        ).findSummary(
            3L,
            null,
            null,
            null
        );
    }

}
