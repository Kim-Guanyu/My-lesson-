package com.mdkj.gen;

import com.mdkj.util.GenUtil;


public class UMS {
    public static void main(String[] args) {
        // 根据数据库 ml_ums 生成代码并放置到 ml-user 子模块中
        GenUtil.getGenerator("ml_ums", "ml-user").generate();
    }
}
