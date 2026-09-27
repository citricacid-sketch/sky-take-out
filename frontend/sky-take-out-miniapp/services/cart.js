// services/cart.js
// Single source of truth for the shopping cart across the whole mini program.
// - In-memory cache of the cart list, refreshed from the backend.
// - Tiny subscribe/publish so pages can re-render on change.
// - Keeps the TabBar cart badge (index 2) in sync.
//
// Backend contract: a cart line is de-duplicated by (userId, dishId, dishFlavor),
// so dishFlavor MUST be a stable string. We send it as JSON.stringify(selectedValues),
// e.g. '["热饮","少糖"]'. add() increments by one, so call it `number` times for qty.

const http = require('./request.js');

let cartList = [];
let listeners = [];

function emit() {
  listeners.forEach((fn) => {
    try {
      fn(cartList);
    } catch (e) {
      // a bad listener must not break the others
    }
  });
  updateTabBadge();
}

function getCount() {
  return cartList.reduce((sum, item) => sum + (item.number || 0), 0);
}

function getTotal() {
  return cartList.reduce((sum, item) => {
    const price = Number(item.amount) || 0;
    const qty = item.number || 0;
    return sum + price * qty;
  }, 0);
}

// TabBar cart item is the 3rd entry (index 2): home, menu, cart, profile.
function updateTabBadge() {
  const count = getCount();
  try {
    if (count > 0) {
      wx.setTabBarBadge({ index: 2, text: String(count) }).catch(() => {});
    } else {
      wx.removeTabBarBadge({ index: 2 }).catch(() => {});
    }
  } catch (e) {
    // setTabBarBadge needs the TabBar to exist; safe to ignore early calls
  }
}

async function refresh() {
  const list = (await http.get('/user/shoppingCart/list', { silent: true })) || [];
  cartList = list;
  emit();
  return cartList;
}

async function add(dishId, dishFlavor = '') {
  await http.post('/user/shoppingCart/add', { dishId, dishFlavor });
  await refresh();
}

async function sub(dishId, dishFlavor = '') {
  await http.post('/user/shoppingCart/sub', { dishId, dishFlavor });
  await refresh();
}

async function clean() {
  await http.delete('/user/shoppingCart/clean');
  await refresh();
}

// Subscribe to cart changes. Returns an unsubscribe function.
function subscribe(fn) {
  if (typeof fn !== 'function') return () => {};
  listeners.push(fn);
  return () => {
    listeners = listeners.filter((l) => l !== fn);
  };
}

module.exports = {
  refresh,
  add,
  sub,
  clean,
  subscribe,
  getCount,
  getTotal,
  updateTabBadge,
  // raw list read — pages normally use subscribe, this is for one-off reads
  getList: () => cartList,
};
