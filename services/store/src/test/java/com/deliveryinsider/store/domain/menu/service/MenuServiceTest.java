package com.deliveryinsider.store.domain.menu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.deliveryinsider.store.domain.menu.entity.Menu;
import com.deliveryinsider.store.domain.menu.enums.MenuStatus;
import com.deliveryinsider.store.domain.menu.mapper.MenuMapper;
import com.deliveryinsider.store.domain.menu.request.MenuCreateRequest;
import com.deliveryinsider.store.domain.menu.request.MenuUpdateRequest;
import com.deliveryinsider.store.domain.menu.response.MenuResponse;
import com.deliveryinsider.store.domain.store.entity.Store;
import com.deliveryinsider.store.domain.store.mapper.StoreMapper;
import com.deliveryinsider.store.global.error.BusinessException;
import com.deliveryinsider.store.global.error.MenuErrorCode;
import com.deliveryinsider.store.global.error.StoreErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    private MenuMapper menuMapper;

    @Mock
    private StoreMapper storeMapper;
    @Mock
    private com.deliveryinsider.store.domain.catalog.CatalogEventWriter catalogEvents;

    @InjectMocks
    private MenuService menuService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // ==========================================
    // 1. Create (메뉴 등록) 테스트
    // ==========================================

    @Test
    @DisplayName("정상 케이스: Menu 등록에 성공한다")
    void Menu_Registration_Success() {
        Long userId = 1L;
        Store store = Store.builder().id(10L).userId(userId).build();
        MenuCreateRequest request = new MenuCreateRequest("불고기피자", 20000, 10000, 1000, 20);

        Menu savedMenu = Menu.builder()
                .id(100L)
                .storeId(10L)
                .menuName("불고기피자")
                .menuPrice(20000)
                .menuStatus(MenuStatus.ACTIVE)
                .build();

        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.save(any(Menu.class))).thenReturn(1);
        when(menuMapper.findByIdAndStoreId(any(), eq(10L))).thenReturn(savedMenu);

        MenuResponse response = menuService.create(userId, request);

        assertEquals("불고기피자", response.menuName());
        assertEquals(MenuStatus.ACTIVE, response.menuStatus());
    }

    @Test
    @DisplayName("실패 케이스: DB에 메뉴 저장이 실패하면 예외가 발생한다")
    void Menu_Registration_FAILURE() {

        // 1. 테스트에 필요한 기본 가짜(Mock) 데이터를 선언합니다.
        Long userId = 1L; // 요청을 보낼 점주의 ID를 1번으로 가정합니다.

        // 유저 ID가 1번인 점주가 소유한 10번 가게(Store) 객체를 생성합니다.
        Store store = Store.builder().id(10L).userId(userId).build();

        // 클라이언트(Postman 등)가 보냈다고 가정할 메뉴 생성 요청서(DTO)를 만듭니다.
        MenuCreateRequest request = new MenuCreateRequest("불고기피자", 20000, 10000, 1000, 20);

        // 2. 가짜 객체(Mock)들의 행동을 조작(Stubbing)합니다.
        // "만약 storeMapper에서 findByUserId(1L)을 호출하면, 방금 만든 store 객체를 줘라"
        when(storeMapper.findByUserId(userId)).thenReturn(store);

        // 핵심 실패 조작: "만약 menuMapper가 save(메뉴)를 호출하면, 실패(0)했다고 거짓말 쳐라"
        // (실제 성공 시에는 1이 반환되어야 하지만 고의로 0을 반환하게 만듭니다)
        when(menuMapper.save(any(Menu.class))).thenReturn(0);

        // 3. 실행 및 예외 발생 검증 (assertThrows)
        // assertThrows는 람다식 "() -> 실행로직" 을 실행했을 때 지정한 예외(BusinessException)가 터지는지 감시합니다.
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> menuService.create(userId, request) // 실제 서비스 로직 실행
        );

        // 4. 터져나온 예외가 정확히 우리가 의도한 에러 코드인지 최종 확인합니다.
        assertEquals(MenuErrorCode.MENU_REGIST_ERROR, exception.errorCode());
    }

    @Test
    @DisplayName("실패 케이스: 필수값 누락 시 Validation 에러가 발생한다")
    void Menu_noValue_Validation_FAILURE() {
        // 실패 테스트를 하려면 109 라인의 주석을 해제하고, 110번 라인을 주석 으로..
        //MenuCreateRequest request = new MenuCreateRequest("불고기피자", 20000, 10000, 1000, 20, 5);
        MenuCreateRequest request = new MenuCreateRequest(null, null, null, null, null);
        Set<ConstraintViolation<MenuCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    // ==========================================
    // 2. Read (메뉴 조회) 테스트
    // ==========================================

    @Test
    @DisplayName("정상 케이스: Menu 목록 조회에 성공한다")
    void FindMenuList_Success() {
        Long userId = 1L;
        Store store = Store.builder().id(10L).userId(userId).build();
        Menu menu = Menu.builder().id(100L).menuName("불고기피자").build();

        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.findAllByStoreId(10L)).thenReturn(Collections.singletonList(menu));

        List<MenuResponse> responses = menuService.findAll(userId);

        assertEquals(1, responses.size());
        assertEquals("불고기피자", responses.get(0).menuName());
    }

    @Test
    @DisplayName("정상 케이스: Menu 단건 조회에 성공한다")
    void FindOneMenu_Success() {
        Long userId = 1L;
        Long menuId = 100L;
        Store store = Store.builder().id(10L).userId(userId).build();
        Menu menu = Menu.builder().id(menuId).menuName("치즈피자").build();

        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(menu);

        MenuResponse response = menuService.findOne(userId, menuId);

        assertEquals("치즈피자", response.menuName());
    }

    @Test
    @DisplayName("실패 케이스: 없는 menuId를 조회하거나 내 Store 소유가 아니면 예외가 발생한다")
    void findInvalid_MenuAndStore() {
        Long userId = 1L;
        Long menuId = 999L;
        Store store = Store.builder().id(10L).userId(userId).build();

        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> menuService.findOne(userId, menuId)
        );

        // Store는 존재하지만 해당 Store의 메뉴가 없으므로 MENU_NOT_FOUND가 현재 계약이다.
        assertEquals(MenuErrorCode.MENU_NOT_FOUND, exception.errorCode());
    }

    // ==========================================
    // 3. Update (메뉴 수정) 테스트
    // ==========================================

    @Test
    @DisplayName("정상 케이스: Menu 일반 정보 수정에 성공한다")
    void PatchMenu_Success() {
        Long userId = 1L;
        Long menuId = 100L;
        Store store = Store.builder().id(10L).userId(userId).build();

        Menu currentMenu = Menu.builder().id(menuId).menuName("치즈피자").menuStatus(MenuStatus.ACTIVE).build();
        MenuUpdateRequest request = new MenuUpdateRequest("슈퍼치즈피자", 25000, 12000, 1000, 20, MenuStatus.ACTIVE);
        Menu updatedMenu = Menu.builder().id(menuId).menuName("슈퍼치즈피자").menuStatus(MenuStatus.ACTIVE).build();

        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(currentMenu, updatedMenu);
        when(menuMapper.update(any(Menu.class))).thenReturn(1);

        MenuResponse response = menuService.update(userId, menuId, request);

        assertEquals("슈퍼치즈피자", response.menuName());
    }

    @Test
    @DisplayName("정상 케이스: status 필드만 변경(ACTIVE -> DISABLED) 성공한다")
    void PatchMenuStatus_Success() {
        Long userId = 1L;
        Long menuId = 100L;
        Store store = Store.builder().id(10L).userId(userId).build();

        Menu currentMenu = Menu.builder().id(menuId).menuName("치즈피자").menuStatus(MenuStatus.ACTIVE).build();
        MenuUpdateRequest request = new MenuUpdateRequest("치즈피자", null, null, null, null, MenuStatus.DISABLED);
        Menu updatedMenu = Menu.builder().id(menuId).menuName("치즈피자").menuStatus(MenuStatus.DISABLED).build();

        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(currentMenu, updatedMenu);
        when(menuMapper.update(any(Menu.class))).thenReturn(1);

        MenuResponse response = menuService.update(userId, menuId, request);

        assertEquals(MenuStatus.DISABLED, response.menuStatus());
    }

    @Test
    @DisplayName("실패 케이스: 수정하려는 메뉴가 DB에 제대로 반영되지 않으면 예외가 발생한다")
    void PatchMenu_FAILURE() {
        Long userId = 1L;
        Long menuId = 100L;
        Store store = Store.builder().id(10L).userId(userId).build();
        Menu currentMenu = Menu.builder().id(menuId).menuName("치즈피자").build();
        MenuUpdateRequest request = new MenuUpdateRequest("슈퍼치즈피자", null, null, null, null, null);

        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.findByIdAndStoreId(menuId, 10L)).thenReturn(currentMenu);

        // 가짜 DB에게 "업데이트된 줄(row)이 0개야" 라고 조작. 1으로 설정하면 성공처리이기 때문에, 테스트실패 처리
        //when(menuMapper.update(any(Menu.class))).thenReturn(1);

        when(menuMapper.update(any(Menu.class))).thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> menuService.update(userId, menuId, request)
        );
        assertEquals(MenuErrorCode.MENU_NOT_FOUND, exception.errorCode());
    }

    // ==========================================
    // 4. Delete (소프트 삭제) 테스트
    // ==========================================

    @Test
    @DisplayName("정상 케이스: Menu Soft Delete(삭제)에 성공한다")
    void Menu_Soft_Delete_Success() {
        Long userId = 1L;
        Long menuId = 100L;
        Store store = Store.builder().id(10L).userId(userId).build();

        when(storeMapper.findByUserId(userId)).thenReturn(store);
        when(menuMapper.softDelete(menuId, 10L)).thenReturn(1);

        menuService.delete(userId, menuId);
    }

    @Test
    @DisplayName("실패 케이스: 삭제하려는 메뉴가 없거나 이미 삭제되어 있으면 예외가 발생한다")
    void Menu_Soft_Delete_FAILURE() {
        Long userId = 1L;
        Long menuId = 100L;
        Store store = Store.builder().id(10L).userId(userId).build();

        when(storeMapper.findByUserId(userId)).thenReturn(store);

        // 가짜 DB에게 "삭제할 데이터가 없어서 0개 지웠어" 라고 조작
        //  thenReturn(1)로 바꾸면, 서비스 코드가 "오 삭제 성공했네?" 하고 에러를 터뜨리지 않아서 테스트 실패
        when(menuMapper.softDelete(menuId, 10L)).thenReturn(0);



        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> menuService.delete(userId, menuId)
        );


        // MENU_NOT_FOUND를 엉뚱한 MENU_REGIST_ERROR 같은 걸로 바꾸면, 예상한 에러와 달라서 테스트실패
        //assertEquals(MenuErrorCode.MENU_REGIST_ERROR, exception.errorCode());

        assertEquals(MenuErrorCode.MENU_NOT_FOUND, exception.errorCode());
    }
}
