// app.js
// Application entry. Non-blocking launch: login is on-demand, not forced here.

const auth = require('./services/auth.js');
const http = require('./services/request.js');
const cart = require('./services/cart.js');

App({
  globalData: {
    userInfo: null,
    shopStatus: null, // 1 = open, 0 = closed
    pendingCategoryId: null, // homepage → menu category jump bridge
    currentDish: null, // list → dish-detail bridge
  },

  onLaunch() {
    // Best-effort: read cached token + fetch shop status for the homepage.
    this.globalData.userInfo = auth.isLoggedIn() ? { token: auth.getToken() } : null;
    this.fetchShopStatus();
    // Restore the cart TabBar badge from the backend on cold start.
    // Guard: only if logged in — otherwise the 401 would trigger a redirectTo(login)
    // before any webview exists, which throws "routeDone webviewId not found".
    if (auth.isLoggedIn()) {
      cart.refresh().then(() => cart.updateTabBadge()).catch(() => {});
    } else {
      // Not logged in: clear any stale badge so the TabBar renders cleanly.
      cart.updateTabBadge();
    }
  },

  fetchShopStatus() {
    http
      .get('/user/shop/status', { silent: true })
      .then((status) => {
        this.globalData.shopStatus = status;
      })
      .catch(() => {
        this.globalData.shopStatus = 1; // default to open on failure
      });
  },
});
