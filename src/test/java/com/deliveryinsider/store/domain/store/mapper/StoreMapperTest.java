package com.deliveryinsider.store.domain.store.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.deliveryinsider.store.domain.store.entity.Store;

@SpringBootTest
@Transactional // 테스트가 끝나면 DB를 원래 상태로 롤백(되돌림)하여 데이터를 망가뜨리지 않음!
class StoreMapperTest {

    @Autowired
    private StoreMapper storeMapper;

    @Test
    @DisplayName("진짜 DB 연동: findByUserId 쿼리가 정상 작동한다")
    void findByUserId_IntegrationTest() {

        // 1. [Given] 테스트 환경 준비
        // 테스트를 위해 로컬 DB에 userId=1인 가게가 최소 1개는 있어야 합니다.
        Long userId = 1L;

        // 2. [When] 실제 기능 실행
        // Mapper(MyBatis)를 통해 실제 DB에 SELECT 쿼리를 날려 데이터를 가져옵니다.
        Store store = storeMapper.findByUserId(userId);

        // 3. [Then] 결과 검증
        // 가져온 데이터가 null이 아닌지 (즉, DB에 데이터가 존재하는지) 가장 먼저 확인합니다.
        assertNotNull(store, "테스트를 위해 로컬 DB에 userId=1 인 가게 데이터가 필요합니다!");

        // 가져온 가게 데이터의 주인이 정말로 1번 사장님(userId = 1L)이 맞는지 검증합니다.
        assertEquals(userId, store.getUserId(), "조회된 가게의 주인이 1번 사장님이어야 합니다.");
    }

    @Test
    @DisplayName("진짜 DB 연동: update 쿼리가 정상 작동한다")
    void update_IntegrationTest() {

        // 1. [Given] 로컬 DB에 이미 존재하는 1번 사장님을 타겟으로 지정합니다.
        Long userId = 1L;

        // 진짜 DB에 쿼리를 날려서 1번 사장님의 가게 데이터를 통째로 퍼옵니다.
        Store store = storeMapper.findByUserId(userId);

        // 퍼온 데이터가 null이 아닌지(DB에 존재하는지) 1차로 안전망 검증을 합니다.
        assertNotNull(store, "테스트를 위해 로컬 DB에 userId=1 인 가게 데이터가 필요합니다!");

        // 업데이트할 껍데기(DTO)를 만듭니다.
        String newStoreName = "로컬 통합테스트 가게이름 변경"; // 바꿀 이름

        // 기존 DB에서 퍼온 데이터(store)를 바탕으로, 이름만 쏙 바꿔서 새로운 Store 객체를 조립합니다.
        Store updatedStore = Store.builder()
                .id(store.getId())           // 기존 PK 유지
                .userId(store.getUserId())   // 기존 사장님 ID 유지
                .storeName(newStoreName)     // 💥 오직 이것만 새로운 이름으로 덮어씌움!
                .phone(store.getPhone())     // 나머지는 기존 데이터 그대로 복사...
                .businessRegistrationNumber(store.getBusinessRegistrationNumber())
                .businessVerificationId(store.getBusinessVerificationId())
                .address(store.getAddress())
                .addressDetail(store.getAddressDetail())
                .industryType(store.getIndustryType())
                .minimumOrderAmount(store.getMinimumOrderAmount())
                .openTime(store.getOpenTime())
                .closeTime(store.getCloseTime())
                .operationStatus(store.getOperationStatus())
                .build();

        // 2. [When] 조립된 객체를 매퍼에 던져 진짜 UPDATE 쿼리를 실행합니다.
        // result에는 업데이트가 적용된 행(row)의 개수가 담깁니다.
        int result = storeMapper.update(updatedStore);

        // 3. [Then] 검증
        // 업데이트가 정상적으로 수행되었다면 1줄이 바뀌었어야 하므로(1) 이를 검증합니다.
        assertEquals(1, result, "업데이트된 행의 개수는 1개여야 합니다.");

        // 4. (선택/확인 사살) DB에 쿼리가 잘 먹혔는지 다시 한번 조회해서 이름을 대조해봅니다.
        Store checkStore = storeMapper.findByUserId(userId);

        // 아까 지정했던 "로컬 통합테스트 가게이름 변경"이 진짜 DB에서 조회한 이름과 똑같은지 확인합니다.
        assertEquals(newStoreName, checkStore.getStoreName(), "DB에 변경된 이름이 정확히 반영되어야 합니다.");

        // 💡 팁: 클래스 상단의 @Transactional 덕분에 테스트 종료 시 변경된 이름은 즉시 원래대로 자동 롤백됩니다.
    }
}
