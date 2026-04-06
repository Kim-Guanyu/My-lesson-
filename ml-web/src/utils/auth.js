const TOKEN_KEY = "ml_token";
const USER_KEY = "ml_user";
const ROLE_KEY = "ml_roles";
const MENU_KEY = "ml_menus";

export const getToken = () => localStorage.getItem(TOKEN_KEY) || "";

export const setAuth = ({ token, user, roles, menus }) => {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  }
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
  if (roles) {
    localStorage.setItem(ROLE_KEY, JSON.stringify(roles));
  }
  if (menus) {
    localStorage.setItem(MENU_KEY, JSON.stringify(menus));
  }
};

export const getStoredUser = () => {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    return null;
  }
};

export const getStoredRoles = () => {
  const raw = localStorage.getItem(ROLE_KEY);
  if (!raw) {
    return [];
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    return [];
  }
};

export const getStoredMenus = () => {
  const raw = localStorage.getItem(MENU_KEY);
  if (!raw) {
    return [];
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    return [];
  }
};

export const clearAuth = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  localStorage.removeItem(ROLE_KEY);
  localStorage.removeItem(MENU_KEY);
};

