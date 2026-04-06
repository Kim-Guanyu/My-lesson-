package com.mdkj.gen;

import com.mdkj.util.GenUtil;


public class OMS {
    public static void main(String[] args) {
        // 根据数据库 ml_oms 生成代码并放置到 ml-order 子模块中
        GenUtil.getGenerator("ml_oms", "ml-order").generate();
    }
}
