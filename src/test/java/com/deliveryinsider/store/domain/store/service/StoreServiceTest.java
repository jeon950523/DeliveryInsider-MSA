package com.deliveryinsider.store.domain.store.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.deliveryinsider.store.domain.store.entity.Store;
import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.domain.store.request.StoreUpdateRequest;
import com.deliveryinsider.store.domain.store.response.StoreResponse;
import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.StoreErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) // Mockito 가짜 객체들을 사용할 수 있게 해주는 마법의 어노테이션
class StoreServiceTest {

    @Mock // DB와 통신하는 척하는 가짜 매퍼 객체 생성
    private StoreMapper storeMapper;
    @Mock
    private com.deliveryinsider.store.domain.catalog.CatalogEventWriter catalogEvents;

    @InjectMocks // 가짜 매퍼 객체를 주입받아서 동작하는 진짜 서비스 객체 생성
    private StoreService storeService;

    // ==========================================
    // 1. 조회 (Read) 테스트
    // ==========================================

    @Test
    @DisplayName("정상 케이스: 내 Store 조회에 성공한다")
    void FindMyStore_SUCCESS() {
        // 1. [Given] 테스트 환경 준비
        Long userId = 1L; // 조회를 요청하는 사장님의 ID

        // DB에서 꺼내올 가짜 데이터 객체를 만듭니다.
        Store testStore = Store.builder()
                .id(10L)
                .userId(userId)
                .storeName("맛있는 피자집")
                .build();

        // 행동 조작 (Stubbing): 가짜 매퍼에게 "userId 1로 검색하면 testStore를 반환해!"라고 대본을 줍니다.
        when(storeMapper.findByUserId(userId)).thenReturn(testStore);

        // 2. [When] 실제 기능 실행
        StoreResponse result = storeService.findMyStore(userId);

        // 3. [Then] 결과 검증
        // 반환된 응답(DTO)의 데이터가 방금 가짜 DB에서 꺼내준 데이터와 일치하는지 확인합니다.
        assertEquals(10L, result.id(), "가게 ID가 10번이어야 합니다.");
        assertEquals("맛있는 피자집", result.storeName(), "가게 이름이 정확해야 합니다.");
    }

    @Test
    @DisplayName("실패 케이스: Store가 없는 userId로 조회하면 STORE_NOT_FOUND 예외가 발생한다")
    void NoStore_userId_Exception() {
        // 1. [Given] 테스트 환경 준비
        Long wrongUserId = 999L; // 존재하지 않는 가짜 유저 ID

        // 행동 조작: DB에 해당 유저의 상점이 없으므로 null을 반환하게 설정
        when(storeMapper.findByUserId(wrongUserId)).thenReturn(null);

        // 2. [When] & [Then] 실행과 동시에 예외 발생 검증
        // 람다식 내의 로직을 실행했을 때 BusinessException이 터지는지 감시합니다.
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> storeService.findMyStore(wrongUserId)
        );

        // 3. 발생한 예외가 정확히 우리가 의도한 "가게 없음" 에러인지 확인합니다.
        assertEquals(StoreErrorCode.STORE_NOT_FOUND, exception.errorCode());
    }

    // ==========================================
    // 2. 수정 (Update / PATCH) 테스트
    // ==========================================

    @Test
    @DisplayName("정상 케이스: Store 부분 PATCH(수정)에 성공한다")
    void Store_PART_PATCH_SUCCESS() {
        // 1. [Given]
        Long userId = 1L;

        // 원본 Store 데이터
        Store currentStore = Store.builder()
                .id(10L)
                .userId(userId)
                .storeName("기존 피자집")
                .build();

        // 클라이언트의 변경 요청 (이름만 변경)
        StoreUpdateRequest request = new StoreUpdateRequest(
                "변경된 피자집", null, null, null, null, null, null, null, null
        );

        // 변경 후 예상되는 Store 데이터
        Store updatedStore = Store.builder()
                .id(10L)
                .userId(userId)
                .storeName("변경된 피자집")
                .build();

        // 행동 조작: 서비스 코드 내부에서 findByUserId가 수정 전/후로 2번 호출되므로,
        // 첫 번째 호출 시에는 기존 데이터를, 두 번째 호출 시에는 업데이트된 데이터를 주도록 연달아 대본을 줍니다.
        when(storeMapper.findByUserId(userId)).thenReturn(currentStore, updatedStore);

        // 행동 조작: 업데이트 쿼리가 성공적으로 1(1개 행 수정됨)을 반환한다고 설정합니다.
        when(storeMapper.update(any(Store.class))).thenReturn(1);

        // 2. [When] 실제 기능 실행
        StoreResponse result = storeService.update(userId, request);

        // 3. [Then] 결과 검증
        // 결과로 나온 객체의 이름이 클라이언트가 요청한 이름으로 덮어씌워졌는지 확인합니다.
        assertEquals("변경된 피자집", result.storeName());
    }

    @Test
    @DisplayName("실패 케이스: Store 수정 시 DB에 업데이트가 적용되지 않으면 예외가 발생한다")
    void Store_PATCH_UPDATE_FAILURE() {

        // 1. [Given] 테스트 환경 준비
        Long userId = 1L;

        // 유저 ID 1번을 가진 10번 가게 객체를 생성합니다.
        Store currentStore = Store.builder().id(10L).userId(userId).build();

        // 이름만 변경하겠다는 클라이언트의 요청(DTO)을 생성합니다.
        StoreUpdateRequest request = new StoreUpdateRequest(
                "변경된 피자집", null, null, null, null, null, null, null, null
        );

        // 2. 가짜 객체 행동 조작 (Stubbing)
        // 유저 검증 로직에서는 정상적으로 기존 가게가 찾아지도록 설정합니다.
        when(storeMapper.findByUserId(userId)).thenReturn(currentStore);

        // 핵심 실패 조작: 진짜라면 1이 반환되어야 하지만, DB 오류 등으로 인해 0행이 수정되었다고 거짓말합니다.
        when(storeMapper.update(any(Store.class))).thenReturn(0);

        // 3. [When] & [Then] 실행 및 예외 검증
        // 0이 반환되었으므로 서비스 로직 내부에서 IN_EDIT_STORE_ERROR 예외를 던져야 합니다.
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> storeService.update(userId, request) // 실제 서비스 로직 실행
        );

        // 4. 예외 코드 검증
        // 터진 예외가 정확히 우리가 의도한 IN_EDIT_STORE_ERROR 인지 확인합니다.
        assertEquals(StoreErrorCode.IN_EDIT_STORE_ERROR, exception.errorCode());
    }

    @Test
    @DisplayName("실패 케이스: 빈 PATCH (수정할 필드가 하나도 없으면 Validation 검증에 실패한다)")
    void EMPTY_PATCH_Validation_FAILURE() {
        // 1. [Given] 클라이언트가 아무런 데이터도 보내지 않은 최악의 상황 (모든 필드가 null)
        StoreUpdateRequest emptyRequest = new StoreUpdateRequest(
                null, null, null, null, null, null, null, null, null
        );

        // 2. [When] & [Then]
        // 컨트롤러 단의 @Valid에서 잡아내기 위한 유효성 검사 메서드(isUpdateFieldPresent)를 직접 호출해 봅니다.
        boolean hasUpdateField = emptyRequest.isUpdateFieldPresent();

        // 결과가 false여야 검증에 실패(에러 발생 대상)한 것이므로 assertFalse로 검증합니다.
        assertFalse(hasUpdateField, "빈 PATCH 요청이므로 isUpdateFieldPresent()는 false를 반환해야 합니다.");
    }
}
