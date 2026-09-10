import test from 'node:test';
import assert from 'node:assert/strict';
import {
  calculateOrderTotal,
  createOrderPayload,
  resolveAllowedActions,
} from '../src/features/order/orderPayload.js';

test('선택한 외부 메뉴만 주문 payload에 포함한다', () => {
  const menus = [
    { externalMenuId: 'BAE-MENU-004', price: 18000 },
    { externalMenuId: 'BAE-MENU-007', price: 2000 },
  ];

  const payload = createOrderPayload({
    externalStoreId: 'BAE-STORE-003',
    menus,
    quantities: {
      'BAE-MENU-004': 2,
      'BAE-MENU-007': 0,
    },
    deliveryAddress: ' 대구광역시 동구 ',
    customerRequest: ' 문 앞에 놓아주세요 ',
  });

  assert.equal(payload.storeId, 'BAE-STORE-003');
  assert.deepEqual(payload.items, [
    { menuId: 'BAE-MENU-004', quantity: 2, unitPrice: 18000 },
  ]);
  assert.equal(payload.deliveryAddress, '대구광역시 동구');
  assert.equal(payload.customerRequest, '문 앞에 놓아주세요');
  assert.equal(payload.financials, null);
});

test('주문 합계는 외부 플랫폼 판매가와 수량으로 계산한다', () => {
  const total = calculateOrderTotal(
    [
      { externalMenuId: 'A', price: 18000 },
      { externalMenuId: 'B', price: 2000 },
    ],
    { A: 1, B: 2 },
  );

  assert.equal(total, 22000);
});

test('외부 플랫폼은 매장 픽업 준비 이후에만 배송 상태를 진행한다', () => {
  assert.deepEqual(resolveAllowedActions('CREATED'), ['CANCELED']);
  assert.deepEqual(resolveAllowedActions('READY_FOR_PICKUP'), ['PICKED_UP']);
  assert.deepEqual(resolveAllowedActions('PICKED_UP'), ['DELIVERED']);
  assert.deepEqual(resolveAllowedActions('DELIVERED'), []);
  assert.deepEqual(resolveAllowedActions('CANCELED'), []);
});
