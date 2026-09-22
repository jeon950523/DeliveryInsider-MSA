package com.deliveryinsider.store.integration.nts;

import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.StoreErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class NtsBusinessClient {

    private static final String VALID_CODE = "01";

    private final RestClient restClient;
    private final String serviceKey;

    public NtsBusinessClient(
            @Qualifier("ntsRestClient") RestClient restClient,
            @Value("${integration.nts.service-key:}") String serviceKey
    ) {
        this.restClient = restClient;
        this.serviceKey = serviceKey;
    }

    public NtsBusinessVerificationResult verify(
            String businessRegistrationNumber,
            String representativeName,
            String openingDate
    ) {
        validateConfiguration();

        ValidationResponse validationResponse = validateBusiness(
                businessRegistrationNumber,
                representativeName,
                openingDate
        );

        ValidationData validationData = firstValidationData(validationResponse);

        if (!VALID_CODE.equals(validationData.valid())) {
            return new NtsBusinessVerificationResult(
                    false,
                    validationData.valid_msg(),
                    null,
                    null
            );
        }

        StatusResponse statusResponse = findStatus(
                businessRegistrationNumber
        );

        StatusData statusData = firstStatusData(statusResponse);

        return new NtsBusinessVerificationResult(
                true,
                validationData.valid_msg(),
                statusData.b_stt_cd(),
                statusData.b_stt()
        );
    }

    private ValidationResponse validateBusiness(
            String businessRegistrationNumber,
            String representativeName,
            String openingDate
    ) {
        try {
            var request = new ValidationRequest(
                    List.of(
                            new ValidationBusiness(
                                    businessRegistrationNumber,
                                    openingDate,
                                    representativeName,
                                    "",
                                    "",
                                    "",
                                    "",
                                    "",
                                    ""
                            )
                    )
            );

            return restClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/nts-businessman/v1/validate")
                            .queryParam("serviceKey", serviceKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ValidationResponse.class);

        } catch (RestClientException e) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_PROVIDER_ERROR,
                    e
            );
        }
    }

    private StatusResponse findStatus(
            String businessRegistrationNumber
    ) {
        try {
            return restClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/nts-businessman/v1/status")
                            .queryParam("serviceKey", serviceKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                            new StatusRequest(
                                    List.of(
                                            businessRegistrationNumber
                                    )
                            )
                    )
                    .retrieve()
                    .body(StatusResponse.class);

        } catch (RestClientException e) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_PROVIDER_ERROR,
                    e
            );
        }
    }

    private ValidationData firstValidationData(
            ValidationResponse response
    ) {
        if (response == null
                || response.data() == null
                || response.data().isEmpty()) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_PROVIDER_ERROR
            );
        }

        return response.data().getFirst();
    }

    private StatusData firstStatusData(
            StatusResponse response
    ) {
        if (response == null
                || response.data() == null
                || response.data().isEmpty()) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_PROVIDER_ERROR
            );
        }

        return response.data().getFirst();
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(serviceKey)) {
            throw new BusinessException(
                    StoreErrorCode.BUSINESS_VERIFICATION_PROVIDER_ERROR
            );
        }
    }

    private record ValidationRequest(
            List<ValidationBusiness> businesses
    ) {
    }

    private record ValidationBusiness(
            String b_no,
            String start_dt,
            String p_nm,
            String p_nm2,
            String b_nm,
            String corp_no,
            String b_sector,
            String b_type,
            String b_adr
    ) {
    }

    private record ValidationResponse(
            String status_code,
            Integer request_cnt,
            Integer valid_cnt,
            List<ValidationData> data
    ) {
    }

    private record ValidationData(
            String b_no,
            String valid,
            String valid_msg
    ) {
    }

    private record StatusRequest(
            List<String> b_no
    ) {
    }

    private record StatusResponse(
            String status_code,
            Integer request_cnt,
            Integer match_cnt,
            List<StatusData> data
    ) {
    }

    private record StatusData(
            String b_no,
            String b_stt,
            String b_stt_cd,
            String tax_type,
            String tax_type_cd,
            String end_dt
    ) {
    }
}
