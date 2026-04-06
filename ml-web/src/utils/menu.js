const normalizePath = (value) => {
  if (!value) {
    return "";
  }
  return value.startsWith("/") ? value : `/${value}`;
};

const resolveMenuPath = (menu) => normalizePath(menu.url || menu.menuUrl || menu.path || "");

const buildMenuTree = (menus = []) => {
  const map = new Map();
  menus.forEach((menu) => {
    map.set(menu.id, { ...menu, children: [] });
  });

  const roots = [];
  map.forEach((menu) => {
    const parentId = Number(menu.pid || 0);
    if (!parentId) {
      roots.push(menu);
      return;
    }
    const parent = map.get(parentId);
    if (parent) {
      parent.children.push(menu);
    } else {
      roots.push(menu);
    }
  });

  const sortMenus = (items) => {
    items.sort((a, b) => Number(a.idx || 0) - Number(b.idx || 0));
    items.forEach((item) => {
      if (item.children?.length) {
        sortMenus(item.children);
      }
    });
  };

  sortMenus(roots);
  return roots;
};

const buildMenuGroups = (menus = []) => {
  const tree = buildMenuTree(menus);
  return tree.map((menu) => ({
    label: menu.title,
    icon: menu.icon || "Menu",
    items: (menu.children || [])
      .map((child) => ({
        label: child.title,
        icon: child.icon || "Menu",
        path: resolveMenuPath(child)
      }))
      .filter((item) => item.path)
  }));
};

const collectMenuPaths = (menus = []) => {
  const paths = [];
  const visit = (items) => {
    items.forEach((item) => {
      const path = resolveMenuPath(item);
      if (path) {
        paths.push(path);
      }
      if (item.children?.length) {
        visit(item.children);
      }
    });
  };
  const tree = buildMenuTree(menus);
  visit(tree);
  return paths;
};

export { normalizePath, buildMenuTree, buildMenuGroups, collectMenuPaths };
