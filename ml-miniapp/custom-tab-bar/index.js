const util = require('../utils/util.js');

Component({
  data: {
    activeTab: 0,
    tabs: [
      {pagePath: '/pages/index/index', text: '首页', icon: 'home-o'},
      {pagePath: '/pages/course/course', text: '课程', icon: 'shop-o'},
      {pagePath: '/pages/cart/cart', text: '购物车', icon: 'cart-o'},
      {pagePath: '/pages/user/user', text: '我的', icon: 'user-o'}
    ]
  },
  methods: {
    changeTab(ev) {
      const tabIndex = ev.detail;
      const tab = this.data.tabs[tabIndex];
      util.tab(tab.pagePath);
    }
  }
});
