import { createStore } from "vuex";
import { clearAuth, getStoredMenus, getStoredRoles, getStoredUser, getToken, setAuth } from "../utils/auth";

export default createStore({
  state() {
    return {
      sidebarCollapsed: false,
      token: getToken(),
      user: getStoredUser(),
      roles: getStoredRoles(),
      menus: getStoredMenus()
    };
  },
  mutations: {
    toggleSidebar(state) {
      state.sidebarCollapsed = !state.sidebarCollapsed;
    },
    setUser(state, user) {
      state.user = user;
    },
    setAuth(state, payload) {
      state.token = payload.token || "";
      state.user = payload.user || null;
      state.roles = payload.roles || state.roles || [];
      state.menus = payload.menus || [];
      setAuth(payload);
    },
    clearAuth(state) {
      state.token = "";
      state.user = null;
      state.roles = [];
      state.menus = [];
      clearAuth();
    },
    setRoles(state, roles) {
      state.roles = roles || [];
      setAuth({ roles: state.roles });
    }
  }
});
