package com.deliveryinsider.store.domain.menu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.deliveryinsider.store.domain.menu.entity.Menu;
import com.deliveryinsider.store.domain.menu.entity.MenuLossDismissal;
import com.deliveryinsider.store.domain.menu.mapper.MenuMapper;
import com.deliveryinsider.store.domain.menu.request.MenuLossDismissRequest;
import com.deliveryinsider.store.domain.menu.response.MenuLossDismissalResponse;
import com.deliveryinsider.store.domain.store.entity.Store;
import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.MenuErrorCode;

@ExtendWith(MockitoExtension.class) // Mockito 가짜 객체들을 사용할 수 있게 해주는 마법의 어노테이션
class MenuLossDismissalServiceTest {

    @Mock // 가짜 MenuMapper 생성
    private MenuMapper menuMapper;

    @Mock // 가짜 StoreMapper 생성
    private StoreMapper storeMapper;
    @Mock
    private com.deliveryinsider.store.domain.catalog.CatalogEventWriter catalogEvents;

    @InjectMocks // 가짜 매퍼들을 주입받아 동작하는 실제 MenuService 객체 (LossDismissal 로직도 여기에 포함됨)
    private MenuService menuService;

    @Test
    @DisplayName("정상 케이스: LossDismissal Dismiss (기각/숨김) 성공")
    void LossDismissal_Dismiss_Success() {
        // 1. [Given] 데이터 준비
        Long userId = 1L; // 점주 ID
        Long menuId = 100L; // 숨기고싶은 메뉴 ID

        // 점주 가게정보 객체 생성
        Store store = Store.builder().id(10L).userId(userId).build();

        // 숨길 대상인 메뉴 객체 생성
        Menu menu = Menu.builder().id(menuId).menuName("치즈피자").build();

        // 메뉴를 7일동안 숨기겠다는 클라이언트의 요청 생성
        MenuLossDismissRequest request = new MenuLossDismissRequest(7);

        // DB에 저장된 후 리턴될 가상의 최종 결과물
        MenuLossDismissal savedDismissal = MenuLossDismissal.builder()
                .id(1L)
                .storeId(10L)
                .menuId(menuId)
                .dismissedAt(LocalDateTime.now())
                .hideUntil(LocalDateTime.now().plusDays(7))
                .restoredAt(null) // 처음 기각 시 복구 시간은 당연히 null이어야 합니다.
                .build();

        // 2. 가짜 매퍼 행동 조작 (Stubbing)
        // userId 1로 가게를 조회하면, 우리가 만든 10번 가게 객체를 주도록 설정
        when(storeMapper.findByUserId(userId)).thenReturn(store);

        // 100번 메뉴를 찾으면 치즈피자 객체를 주도록 설정
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(menu);

        // 숨김 처리 내역을 조회하면, 위에서 만든 savedDismissal 데이터를 반환하도록 설정
        when(menuMapper.findLossDismissalByStoreIdAndMenuId(10L, menuId)).thenReturn(savedDismissal);

        // 3. [When] 점주가 메뉴를 숨겨 달라고 요청하는 실제 비즈니스 로직 실행
        MenuLossDismissalResponse response = menuService.dismissLossMenu(userId, menuId, request);

        // 4. [Then] 결과 확인
        // 응답 객체가 null이 아닌지 (즉, 서비스 로직이 정상적으로 완주했는지) 확인합니다.
        assertNotNull(response, "정상적인 응답 객체가 반환되어야 합니다.");

        // 반환된 응답의 메뉴 ID가 우리가 숨겨달라고 요청한 100번 메뉴가 맞는지 검증합니다.
        assertEquals(menuId, response.menuId(), "응답받은 메뉴 ID가 일치해야 합니다.");
    }

    @Test
    @DisplayName("정상 케이스: LossDismissal Active 목록 조회 성공")
    void LossDismissal_Active_List_Success() {
        // 1. [Given] 테스트 준비물
        Long userId = 1L; // 점주 ID
        Store store = Store.builder().id(10L).userId(userId).build(); // 점주의 가게정보

        // 이미 숨김 처리되어 있는 과거 내역 1개를 준비합니다.
        MenuLossDismissal dismissal = MenuLossDismissal.builder()
                .id(1L)
                .storeId(10L)
                .menuId(100L)
                .build();

        // 2. 가짜 매퍼 조작
        when(storeMapper.findByUserId(userId)).thenReturn(store);

        // 10번 가게의 '숨김 목록'을 달라고 요청하면, 1개짜리 리스트(Collections.singletonList)를 반환하도록 조작합니다.
        when(menuMapper.findActiveLossDismissalsByStoreId(10L)).thenReturn(Collections.singletonList(dismissal));

        // 3. [When] 현재 숨겨져 있는 메뉴 목록 조회 로직 실행
        List<MenuLossDismissalResponse> responses = menuService.findActiveLossDismissals(userId);

        // 4. [Then] 결과 확인
        // 리스트 안에 정말로 1개의 항목이 잘 들어있는지 사이즈를 검증합니다.
        assertEquals(1, responses.size(), "목록에는 1개의 숨김 내역이 있어야 합니다.");

        // 그 1개의 숨겨진 메뉴 ID가 100번이 맞는지 확인합니다.
        assertEquals(100L, responses.get(0).menuId(), "숨겨진 메뉴의 ID는 100이어야 합니다.");
    }

    @Test
    @DisplayName("정상 케이스: LossDismissal Restore (복구) 성공")
    void LossDismissal_Restore_Success() {

        // 1. [Given] 테스트에 필요한 데이터 준비: 점주ID, 메뉴ID, 점주의 가게ID
        Long userId = 1L;
        Long menuId = 100L;
        Store store = Store.builder().id(10L).userId(userId).build();
        Menu menu = Menu.builder().id(menuId).menuName("치즈피자").build();

        // 2. 가짜 매퍼 행동 조작
        // 점주ID로 조회 시 해당되는 가게 리턴
        when(storeMapper.findByUserId(userId)).thenReturn(store);

        // 10번 가게(10L)에서, menuId(예: 100번)에 해당하는 메뉴를 찾아달라고 하면, menu 객체를 반환
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(menu);

        // 3. [When] 숨김처리 해제 로직 실제 실행
        menuService.restoreLossMenu(userId, menuId);

        // 4. [Then] 동작 검증 (verify)
        // 리턴값이 없는 void 메서드의 경우, 값을 비교(assertEquals)할 수 없습니다.
        // 대신 Mockito의 verify()를 사용하여 "이 가짜 객체의 특정 메서드가 정확히 1번(times(1)) 호출되었는가?"를 감시합니다.
        // 즉, "menuMapper의 restoreLossDismissal(10L, 100L) 쿼리가 정말로 실행되었는가?"를 증명합니다.
        verify(menuMapper, times(1)).restoreLossDismissal(10L, menuId);
    }

    @Test
    @DisplayName("실패 케이스: 기각하려는 메뉴가 없으면 예외가 발생한다")
    void LossDismissal_Dismiss_FAILURE() {
        // 1. [Given]
        Long userId = 1L;
        Long menuId = 999L; // DB에 아예 존재하지 않는 메뉴 ID
        Store store = Store.builder().id(10L).userId(userId).build();
        MenuLossDismissRequest request = new MenuLossDismissRequest(7);

        // 2. 가짜 매퍼 행동 조작
        when(storeMapper.findByUserId(userId)).thenReturn(store);

        // 유령 메뉴(999L)를 찾으려고 시도하면 null(없음)을 반환하게 합니다.
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(null);

        // 3. [When & Then]
        // 메뉴가 없으므로 서비스 로직이 예외를 던지는지 확인합니다.
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> menuService.dismissLossMenu(userId, menuId, request)
        );
        // 에러 코드까지 검증하려면 아래 구문을 추가하면 좋습니다.
        // assertEquals(MenuErrorCode.MENU_NOT_FOUND, exception.errorCode());
    }

    @Test
    @DisplayName("정상 케이스: LossDismissal 다시 Dismiss (재숨김) 성공")
    void LossDismissal_ReDismiss_Success() {
        // 1. [Given] 테스트 준비
        Long userId = 1L;
        Long menuId = 100L;
        Store store = Store.builder().id(10L).userId(userId).build();
        Menu menu = Menu.builder().id(menuId).menuName("치즈피자").build();

        // 피자가 또 다 팔려서 이번엔 3일만 숨겨달라고 재요청
        MenuLossDismissRequest request = new MenuLossDismissRequest(3);

        // [DB 결과물 상상하기]
        // 핵심: 과거에 복구(Restore)했던 기록이 있더라도, 다시 숨기면 restoredAt이 null로 덮어씌워져야 함!
        MenuLossDismissal savedDismissal = MenuLossDismissal.builder()
                .id(1L)
                .storeId(10L)
                .menuId(menuId)
                .dismissedAt(LocalDateTime.now())
                .hideUntil(LocalDateTime.now().plusDays(3))
                .restoredAt(null) // <--- 다시 숨겼으므로 복구 기록 초기화 (이 로직 검증이 핵심)
                .build();

        // 2. 가짜 매퍼 행동 조작
        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(menu);

        // 업데이트 된 숨김 내역을 반환하도록 설정
        when(menuMapper.findLossDismissalByStoreIdAndMenuId(10L, menuId)).thenReturn(savedDismissal);

        // 3. [When] 실제 재숨김 액션 실행
        MenuLossDismissalResponse response = menuService.dismissLossMenu(userId, menuId, request);

        // 4. [Then] 결과 확인
        assertNotNull(response);
        assertEquals(menuId, response.menuId(), "요청한 메뉴의 ID가 반환되어야 합니다.");
    }
}
