package com.mdkj.gen;

import com.mdkj.util.GenUtil;


public class CMS {
    public static void main(String[] args) {
        // 根据数据库 ml_cms 生成代码并放置到 ml-course 子模块中
        GenUtil.getGenerator("ml_cms", "ml-course").generate();
    }
}
