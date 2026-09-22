package com.deliveryinsider.store.global.response;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class GlobalResponseTest {
    @Test void successResponseKeepsCommonContract() {
        GlobalResponse<String> r=GlobalResponse.success("ok","data");
        assertThat(r.code()).isEqualTo("00"); assertThat(r.data()).isEqualTo("data");
    }
}
