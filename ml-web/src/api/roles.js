import http from "./http";

const fetchRoleTitlesByUserId = async (userId) => {
  if (!userId) {
    return [];
  }
  const roleIds = await http.get(`/user-server/api/v1/role/listRoleIdsByUserId/${userId}`);
  const roleList = await http.get("/user-server/api/v1/role/list");
  const idSet = new Set(roleIds || []);
  return (roleList || []).filter((role) => idSet.has(role.id)).map((role) => role.title);
};

export { fetchRoleTitlesByUserId };
