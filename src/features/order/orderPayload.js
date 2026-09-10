export const createOrderPayload = ({
  externalStoreId,
  menus,
  quantities,
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
    financials: null,
  };
};

export const calculateOrderTotal = (menus, quantities) => {
  return menus.reduce((total, menu) => {
    const quantity = Number(quantities[menu.externalMenuId] || 0);
    return total + Number(menu.price || 0) * quantity;
  }, 0);
};

export const resolveAllowedActions = (status) => {
  if (status === 'CREATED') {
    return ['CANCELED'];
  }

  if (status === 'PICKED_UP') {
    return ['DELIVERED'];
  }

  return [];
};
