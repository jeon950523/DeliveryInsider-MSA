export const createOrderPayload = ({
  externalStoreId,
  menus,
  quantities,
  couponIds,
  deliveryAddress,
  customerRequest,
}) => {
  const items = menus
    .map((menu) => ({
      menuId: menu.externalMenuId,
      quantity: Number(quantities[menu.externalMenuId] || 0),
      unitPrice: Number(menu.price || 0),
    }))
    .filter((item) => item.quantity > 0);

  return {
    storeId: externalStoreId,
    deliveryAddress: deliveryAddress?.trim() || null,
    customerRequest: customerRequest?.trim() || null,
    items,
    couponIds: Array.isArray(couponIds) ? couponIds : [],
    financials: null,
  };
};

export const calculateOrderTotal = (menus, quantities) => {
  return menus.reduce((total, menu) => {
    const quantity = Number(quantities[menu.externalMenuId] || 0);
    return total + Number(menu.price || 0) * quantity;
  }, 0);
};

export const resolveAllowedActions = (order) => {
  if (order?.status === 'CREATED' && order?.operationStatus === 'WAITING') {
    return ['COOKING', 'CANCELED'];
  }

  if (order?.status === 'CREATED' && order?.operationStatus === 'COOKING') {
    return ['READY_FOR_PICKUP', 'CANCELED'];
  }

  if (order?.status === 'READY_FOR_PICKUP') {
    return ['PICKED_UP'];
  }

  if (order?.status === 'PICKED_UP') {
    return ['DELIVERED'];
  }

  return [];
};
