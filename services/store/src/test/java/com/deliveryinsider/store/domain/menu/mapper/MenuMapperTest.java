package com.deliveryinsider.store.domain.menu.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.deliveryinsider.store.domain.menu.entity.Menu;
import com.deliveryinsider.store.domain.menu.entity.MenuLossDismissal;
import com.deliveryinsider.store.domain.menu.enums.MenuStatus;

@SpringBootTest
@Transactional // DB 원상복구(롤백) 마법
class MenuMapperTest {

    @Autowired
    private MenuMapper menuMapper;

    @Test
    @DisplayName("진짜 DB 연동: Menu의 생성, 조회, 수정, 삭제(CRUD)가 물결처럼 잘 흘러간다")
    void Menu_CRUD_IntegrationTest() {
        // [주의] 테스트를 위해 로컬 DB에 id=1인 가게가 반드시 존재해야 합니다! (StoreMapperTest와 동일)
        Long storeId = 1L;

        // 1. [Create] 새로운 메뉴를 DB에 찔러넣기 (save)
        Menu newMenu = Menu.builder()
                .storeId(storeId)
                .menuName("리얼 테스트 마라탕")
                .menuPrice(15000)
                .menuCost(5000)
                .packagingFee(500) // DB 제약조건(Not Null)을 통과하기 위해 추가
                .expectedCookingTime(15) // 조리시간 15분
                .menuStatus(MenuStatus.ACTIVE)
                .build();

        int insertResult = menuMapper.save(newMenu);
        assertEquals(1, insertResult, "메뉴가 1줄 정상적으로 INSERT 되어야 합니다.");

        // save 직후 XML의 useGeneratedKeys 덕분에 newMenu에 자동생성된 ID가 들어옵니다!
        Long generatedMenuId = newMenu.getId();
        assertNotNull(generatedMenuId, "DB가 자동 생성한 메뉴 ID가 존재해야 합니다.");

        // 2. [Read] 방금 넣은 메뉴가 진짜 DB에서 잘 찾아지는지 조회 (findByIdAndStoreId)
        Menu foundMenu = menuMapper.findByIdAndStoreId(generatedMenuId, storeId);
        assertNotNull(foundMenu);
        assertEquals("리얼 테스트 마라탕", foundMenu.getMenuName());

        // 3. [Update] 마라탕 가격을 인상해서 업데이트 (update)
        foundMenu.setMenuPrice(18000); // 물가 상승 ㅠㅠ
        int updateResult = menuMapper.update(foundMenu);
        assertEquals(1, updateResult, "메뉴가 1줄 정상적으로 UPDATE 되어야 합니다.");

        Menu updatedMenu = menuMapper.findByIdAndStoreId(generatedMenuId, storeId);
        assertEquals(18000, updatedMenu.getMenuPrice());

        // 4. [Delete] 마라탕 단종 처리 (softDelete)
        int deleteResult = menuMapper.softDelete(generatedMenuId, storeId);
        assertEquals(1, deleteResult, "메뉴가 1줄 정상적으로 DELETE 처리되어야 합니다.");

        // 삭제 처리(deleted_at 기록)되었기 때문에 이제 더이상 조회되면 안 됩니다!
        Menu deletedMenu = menuMapper.findByIdAndStoreId(generatedMenuId, storeId);

        // 테스트 정상 반환 코드
        assertNull(deletedMenu, "삭제된 메뉴는 조회 시 null이 반환되어야 합니다.");

        // 테스트 오류 반환 코드
        //assertNotNull(deletedMenu, "삭제된 메뉴는 조회 시 null이 반환되어야 합니다.");
    }

    @Test
    @DisplayName("진짜 DB 연동: LossDismissal (기각 ➔ 조회 ➔ 복구) 흐름 검증")
    void LossDismissal_Flow_IntegrationTest() {
        Long storeId = 1L;

        // [준비] 먼저 숨길 대상인 메뉴를 하나 만들어줍니다.
        Menu menu = Menu.builder()
                .storeId(storeId)
                .menuName("일시품절 탕후루")
                .menuPrice(3000)
                .menuCost(1000)
                .packagingFee(100)
                .expectedCookingTime(5)
                .menuStatus(MenuStatus.ACTIVE)
                .build();
        menuMapper.save(menu);
        Long menuId = menu.getId();

        // 1. [Dismiss] 메뉴 숨김 처리 (upsert)
        MenuLossDismissal dismissal = MenuLossDismissal.builder()
                .storeId(storeId)
                .menuId(menuId)
                .hideUntil(LocalDateTime.now().plusDays(7)) // 7일간 숨김
                .build();
        menuMapper.upsertLossDismissal(dismissal);

        // 숨긴 기록 조회 및 검증 (최초 숨김 시 restoredAt은 null이어야 함!)
        MenuLossDismissal savedDismissal = menuMapper.findLossDismissalByStoreIdAndMenuId(storeId, menuId);
        assertNotNull(savedDismissal);
        assertNull(savedDismissal.getRestoredAt(), "방금 숨긴 메뉴는 아직 복구된 적 없으므로 restoredAt이 null이어야 합니다.");

        // 숨긴 메뉴가 Active 목록에 잘 잡히는지 확인
        List<MenuLossDismissal> activeList = menuMapper.findActiveLossDismissalsByStoreId(storeId);
        boolean isHidden = activeList.stream().anyMatch(d -> d.getMenuId().equals(menuId));
        assertTrue(isHidden, "숨김(Active) 목록에 방금 숨긴 메뉴가 포함되어 있어야 합니다.");

        // 2. [Restore] 메뉴 복구 처리!
        menuMapper.restoreLossDismissal(storeId, menuId);

        // 3. 복구 검증! (지시서 8번의 핵심 요구사항!)
        MenuLossDismissal restoredDismissal = menuMapper.findLossDismissalByStoreIdAndMenuId(storeId, menuId);
        assertNotNull(restoredDismissal.getRestoredAt(), "복구를 실행했으므로 DB에 현재 시간(restoredAt)이 쾅 찍혀있어야 합니다!");
    }
}
