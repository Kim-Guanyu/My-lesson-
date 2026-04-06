package com.mdkj.gen;

import com.mdkj.util.GenUtil;


public class SMS {
    public static void main(String[] args) {
        // 根据数据库 ml_sms 生成代码并放置到 ml-sale 子模块中
        GenUtil.getGenerator("ml_sms", "ml-sale").generate();
    }
}
