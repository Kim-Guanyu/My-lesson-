package com.mdkj.util;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EasyExcelUtil {

    // 定义每个sheet的最大数据量（行）
    private static final int BATCH_SIZE = 100;

    /**
     * 生成Excel报表并自动下载
     *
     * @param resp      HTTP响应对象
     * @param fileName  Excel文件名，可以省略 .xlsx 后缀
     * @param data      数据
     */
    @SneakyThrows
    public static void download(HttpServletResponse resp,
                                String fileName,
                                Collection<?> data) {
        // 空数据处理：空数据时也需要返回响应，避免前端无反馈
        if (CollectionUtil.isEmpty(data)) {
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // 1. 核心修复：清空响应缓冲区（避免残留数据干扰）
        resp.reset();

        // 处理文件名：若未包含.xlsx则添加，否则保持原文件名
        fileName = fileName.endsWith(".xlsx") ? fileName : fileName + ".xlsx";
        // 对文件名重新编码，以避免文件名中文乱码问题
        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        // 2. 核心修复：补充完整的响应头配置（关键！）
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setCharacterEncoding("UTF-8");
        // 修复Content-Disposition格式：添加filename*参数兼容不同浏览器
        resp.setHeader("Content-Disposition",
                "attachment;filename=" + fileName + ";filename*=UTF-8''" + fileName);
        // 禁止缓存（避免浏览器复用旧响应）
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Expires", "0");

        // 获取数据类型
        Class<?> type = data.iterator().next().getClass();
        // 准备数据分批
        List<Object> allData = new ArrayList<>(data);
        int totalSize = allData.size();
        // sheet索引
        int sheetIndex = 1;

        OutputStream outputStream = resp.getOutputStream();
        // 创建写入器
        try (ExcelWriter excelWriter = EasyExcel.write(outputStream, type)
                .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                .build()) {
            // 循环分片处理
            for (int i = 0; i < totalSize; i += BATCH_SIZE) {
                // 计算当前分片的结束索引：确保结束索引不会超过数据总量，避免数组越界。
                int end = Math.min(i + BATCH_SIZE, totalSize);
                // 截取当前分片数据：从索引 i 到 end（不包含）
                List<Object> batchData = allData.subList(i, end);
                // 创建sheet，指定索引和名称
                WriteSheet writeSheet = EasyExcel.writerSheet(sheetIndex, "sheet" + String.format("%02d", sheetIndex)).build();
                // 将数据写入当前sheet数据
                excelWriter.write(batchData, writeSheet);
                // sheet索引自增
                sheetIndex++;
            }
        }

        // 3. 核心修复：强制刷新并关闭输出流，确保数据完整返回前端
        outputStream.flush();
        outputStream.close();
    }
}