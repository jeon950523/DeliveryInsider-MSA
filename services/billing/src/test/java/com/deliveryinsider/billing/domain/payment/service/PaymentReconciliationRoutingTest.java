package com.deliveryinsider.billing.domain.payment.service;
import com.deliveryinsider.billing.domain.payment.entity.PaymentEntity;
import com.deliveryinsider.billing.domain.payment.mapper.PaymentMapper;
import com.deliveryinsider.billing.integration.payment.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.List;
import static org.mockito.Mockito.*;
class PaymentReconciliationRoutingTest {
    @ParameterizedTest @EnumSource(PaymentProviderResultStatus.class)
    void mockCandidatesUseMockOnlyAndUnknownDoesNotActivate(PaymentProviderResultStatus status) {
        var mapper=mock(PaymentMapper.class); var tx=mock(InitialPaymentTransactionService.class);
        var toss=mock(PaymentProviderQueryClient.class); var mock=mock(PaymentProviderQueryClient.class);
        when(toss.provider()).thenReturn("TOSS"); when(mock.provider()).thenReturn("MOCK");
        var payment=new PaymentEntity(); payment.setId(1L); payment.setProvider("MOCK"); payment.setPaymentOrderId("fixture-order"); payment.setAmount(9900);
        when(mapper.findReconciliationCandidates(20)).thenReturn(List.of(payment));
        var result=new PaymentProviderResult(status,null,null,null,null,null,null);
        when(mock.findPayment("fixture-order",9900)).thenReturn(result);
        new PaymentReconciliationService(mapper,List.of(toss,mock),tx).reconcile(20);
        verify(toss,never()).findPayment(anyString(),anyLong());
        if(status==PaymentProviderResultStatus.SUCCEEDED) verify(tx).succeed(1L,result);
        else if(status==PaymentProviderResultStatus.FAILED) verify(tx).fail(1L,result);
        else verifyNoInteractions(tx);
    }
}
