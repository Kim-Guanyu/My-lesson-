export const ROLE_SYSTEM_ADMIN = "系统管理员";
export const ROLE_USER_ADMIN = "用户管理员";
export const ROLE_COURSE_ADMIN = "课程管理员";
export const ROLE_MARKETING_ADMIN = "营销管理员";
export const ROLE_ORDER_ADMIN = "订单管理员";
export const ROLE_NORMAL_USER = "普通用户";

export const ROLE_GROUPS = {
  user: [ROLE_SYSTEM_ADMIN, ROLE_USER_ADMIN],
  course: [ROLE_SYSTEM_ADMIN, ROLE_COURSE_ADMIN],
  marketing: [ROLE_SYSTEM_ADMIN, ROLE_MARKETING_ADMIN],
  order: [ROLE_SYSTEM_ADMIN, ROLE_ORDER_ADMIN]
};

export const hasAnyRole = (userRoles = [], requiredRoles = []) => {
  if (!requiredRoles.length) {
    return true;
  }
  return requiredRoles.some((role) => userRoles.includes(role));
};
