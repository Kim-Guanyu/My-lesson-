<template>
  <div class="page-card">
    <div class="page-toolbar">
      <el-input v-model="keyword" placeholder="关键词搜索" clearable style="max-width: 240px;" />
      <el-button type="primary" @click="loadData">刷新</el-button>
      <el-button type="success" @click="openCreate">新增</el-button>
      <el-button :disabled="!selection.length" @click="openEdit">编辑</el-button>
      <el-button type="danger" :disabled="!selection.length" @click="removeSelected">删除</el-button>
      <el-button v-if="deleteBatchPath" type="danger" plain :disabled="!selection.length" @click="removeBatch">
        批量删除
      </el-button>
      <el-button @click="clearFilter">清空</el-button>
    </div>

    <el-alert
      v-if="statsEndpoint"
      type="info"
      show-icon
      :title="statsMessage"
      class="table-meta"
    />

    <el-table
      :data="rows"
      v-loading="loading"
      style="width: 100%"
      table-layout="auto"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column
        v-for="col in columns"
        :key="col"
        :prop="col"
        :label="col"
        :min-width="resolveColumnMinWidth(col)"
        :show-overflow-tooltip="!isImageColumn(col)"
      >
        <template #default="scope">
          <div v-if="resolveImageUrl(col, scope.row)" class="image-cell">
            <img
              class="table-image"
              :src="resolveImageUrl(col, scope.row)"
              :alt="String(scope.row[col] || '')"
              @error="handleImageError($event, col, scope.row)"
              @click="openImage(resolveImageUrl(col, scope.row))"
            />
            <div class="image-name">{{ scope.row[col] }}</div>
          </div>
          <span v-else>{{ scope.row[col] }}</span>
        </template>
      </el-table-column>
      <el-table-column fixed="right" label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="openDetail(scope.row)">查看</el-button>
          <el-button link type="primary" size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="removeRow(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-meta">总数：{{ totalRows }}</div>

    <el-pagination
      v-if="totalRows"
      class="table-pagination"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="totalRows"
      layout="total, prev, pager, next"
    />

    <el-drawer v-model="drawerOpen" :title="drawerTitle" size="40%">
      <pre>{{ selectedRow }}</pre>
    </el-drawer>

    <el-dialog v-model="formOpen" :title="formTitle" width="520px">
      <el-form v-if="!useJsonEditor" label-width="110px">
        <el-form-item v-for="field in formFields" :key="field" :label="field">
          <el-input
            v-if="fieldTypes[field] === 'text'"
            v-model="formData[field]"
            clearable
          />
          <el-input-number
            v-else-if="fieldTypes[field] === 'number'"
            v-model="formData[field]"
            controls-position="right"
            style="width: 100%"
          />
          <el-switch v-else-if="fieldTypes[field] === 'boolean'" v-model="formData[field]" />
          <el-input v-else v-model="formData[field]" clearable />
        </el-form-item>
      </el-form>
      <el-input
        v-else
        v-model="jsonText"
        type="textarea"
        :rows="12"
        placeholder="请粘贴 JSON 数据"
      />
      <template #footer>
        <el-button @click="formOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch, onBeforeUnmount } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import http from "../api/http";

const route = useRoute();
const title = computed(() => route.meta?.title || "列表");
const apiBase = computed(() => route.meta?.apiBase || "");
const statsEndpoint = computed(() => route.meta?.statsEndpoint || "");
const listPath = computed(() => route.meta?.listPath || "page");
const createPath = computed(() => route.meta?.createPath || "insert");
const updatePath = computed(() => route.meta?.updatePath || "update");
const deletePath = computed(() => route.meta?.deletePath || "delete");
const deleteBatchPath = computed(() => route.meta?.deleteBatchPath ?? "");
const MINIO_BASE_URL = (import.meta.env.VITE_MINIO_BASE_URL || "http://192.168.211.132:9000/my-lesson").replace(/\/$/, "");
const MINIO_BASE_URL_LEGACY = MINIO_BASE_URL.replace(/\/my-lesson$/, "/mylesson");

const loading = ref(false);
const rows = ref([]);
const columns = ref([]);
const keyword = ref("");
const statsMessage = ref("统计信息已加载");

const currentPage = ref(1);
const pageSize = 10;
const totalRows = ref(0);

const selection = ref([]);

const drawerOpen = ref(false);
const selectedRow = ref(null);
const drawerTitle = computed(() => `${title.value}详情`);

const formOpen = ref(false);
const formMode = ref("create");
const formData = ref({});
const jsonText = ref("");
const formTitle = computed(() => `${formMode.value === "create" ? "新增" : "编辑"}${title.value}`);

const formFields = computed(() => Object.keys(formData.value || {}));
const useJsonEditor = computed(() => formFields.value.length === 0);

const fieldTypes = computed(() => {
  const map = {};
  formFields.value.forEach((field) => {
    const value = formData.value[field];
    if (typeof value === "number") {
      map[field] = "number";
    } else if (typeof value === "boolean") {
      map[field] = "boolean";
    } else {
      map[field] = "text";
    }
  });
  return map;
});

const resolveRowId = (row) => row?.id ?? row?.ID;

const routeImageDirMap = {
  "/users": { avatar: "avatar" },
  "/comments": { avatar: "avatar" },
  "/banners": { url: "banner" },
  "/courses": { cover: "course-cover", summary: "course-summary" },
  "/episodes": { cover: "episode-video-cover", video: "episode-video" },
  "/seckill-details": { courseCover: "course-cover", course_cover: "course-cover" },
  "/order-details": { courseCover: "course-cover", course_cover: "course-cover" }
};

const isImageColumn = (column) => {
  const imageColumns = ["avatar", "url", "cover", "summary", "video", "courseCover", "course_cover"];
  return imageColumns.includes(column);
};

const resolveColumnMinWidth = (column) => {
  if (isImageColumn(column)) {
    return 180;
  }
  if (["id", "idx", "gender", "age", "status", "payType", "price", "skPrice", "cpPrice"].includes(column)) {
    return 90;
  }
  if (["title", "courseTitle", "nickname", "username", "phone", "email", "realname"].includes(column)) {
    return 140;
  }
  if (["info", "content"].includes(column)) {
    return 220;
  }
  return 120;
};

const filterColumns = (cols) => {
  if (route.path === "/courses") {
    return cols.filter((col) => col !== "summary");
  }
  return cols;
};

const resolveImageUrl = (column, row) => {
  const value = row?.[column];
  if (!value || typeof value !== "string") {
    return "";
  }
  if (/^https?:\/\//i.test(value)) {
    return value;
  }
  const dir = routeImageDirMap[route.path]?.[column];
  if (!dir) {
    return "";
  }
  return `${MINIO_BASE_URL}/${dir}/${encodeURIComponent(value)}`;
};

const openImage = (url) => {
  if (!url) {
    return;
  }
  window.open(url, "_blank", "noopener,noreferrer");
};

const resolveLegacyImageUrl = (column, row) => {
  const value = row?.[column];
  if (!value || typeof value !== "string") {
    return "";
  }
  if (/^https?:\/\//i.test(value)) {
    return value;
  }
  const dir = routeImageDirMap[route.path]?.[column];
  if (!dir) {
    return "";
  }
  return `${MINIO_BASE_URL_LEGACY}/${dir}/${encodeURIComponent(value)}`;
};

const handleImageError = (event, column, row) => {
  const img = event?.target;
  if (!img || img.dataset.fallbackTried === "1") {
    return;
  }
  const legacyUrl = resolveLegacyImageUrl(column, row);
  if (!legacyUrl || legacyUrl === img.src) {
    return;
  }
  img.dataset.fallbackTried = "1";
  img.src = legacyUrl;
};

const extractRows = (payload) => {
  if (Array.isArray(payload)) {
    return payload;
  }
  if (payload?.records && Array.isArray(payload.records)) {
    return payload.records;
  }
  if (payload?.data && Array.isArray(payload.data)) {
    return payload.data;
  }
  if (payload?.data?.records && Array.isArray(payload.data.records)) {
    return payload.data.records;
  }
  return [];
};

const extractTotal = (payload) => {
  if (!payload || typeof payload !== "object") {
    return 0;
  }
  if (typeof payload.totalRow === "number") {
    return payload.totalRow;
  }
  if (typeof payload.totalRow === "string") {
    return Number(payload.totalRow) || 0;
  }
  if (typeof payload.total === "number") {
    return payload.total;
  }
  if (typeof payload.total === "string") {
    return Number(payload.total) || 0;
  }
  if (typeof payload.data?.totalRow === "number") {
    return payload.data.totalRow;
  }
  if (typeof payload.data?.totalRow === "string") {
    return Number(payload.data.totalRow) || 0;
  }
  if (typeof payload.data?.total === "number") {
    return payload.data.total;
  }
  if (typeof payload.data?.total === "string") {
    return Number(payload.data.total) || 0;
  }
  return 0;
};

const loadData = async () => {
  if (!apiBase.value) {
    rows.value = [];
    totalRows.value = 0;
    return;
  }
  loading.value = true;
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize
    };
    if (keyword.value) {
      params.keyword = keyword.value;
    }
    const data = await http.get(`${apiBase.value}/${listPath.value}`, { params });
    rows.value = extractRows(data);
    totalRows.value = extractTotal(data) || rows.value.length;
    const rawColumns = rows.value.length ? Object.keys(rows.value[0]) : [];
    columns.value = filterColumns(rawColumns);
  } finally {
    loading.value = false;
  }
};

const loadStats = async () => {
  if (!statsEndpoint.value) {
    return;
  }
  try {
    const data = await http.get(statsEndpoint.value);
    if (data && typeof data === "object") {
      const first = Object.entries(data)[0];
      if (first) {
        statsMessage.value = `${first[0]}：${first[1]}`;
      }
    }
  } catch (error) {
    statsMessage.value = "统计信息不可用";
  }
};

const clearFilter = () => {
  keyword.value = "";
  currentPage.value = 1;
  loadData();
};

const openDetail = (row) => {
  selectedRow.value = row;
  drawerOpen.value = true;
};

const handleSelectionChange = (selectionRows) => {
  selection.value = selectionRows;
};

const prepareFormData = (row) => {
  if (row) {
    formData.value = { ...row };
    jsonText.value = JSON.stringify(row, null, 2);
    return;
  }
  if (columns.value.length) {
    const payload = {};
    columns.value.forEach((col) => {
      payload[col] = "";
    });
    formData.value = payload;
    jsonText.value = JSON.stringify(payload, null, 2);
    return;
  }
  formData.value = {};
  jsonText.value = "{}";
};

const openCreate = () => {
  formMode.value = "create";
  prepareFormData();
  formOpen.value = true;
};

const openEdit = (row) => {
  const target = row || selection.value[0];
  if (!target) {
    ElMessage.warning("请选择要编辑的记录");
    return;
  }
  formMode.value = "edit";
  prepareFormData(target);
  formOpen.value = true;
};

const submitForm = async () => {
  let payload = {};
  if (useJsonEditor.value) {
    try {
      payload = JSON.parse(jsonText.value || "{}");
    } catch (error) {
      ElMessage.error("JSON 格式错误");
      return;
    }
  } else {
    payload = { ...formData.value };
  }

  if (!apiBase.value) {
    return;
  }

  if (formMode.value === "create") {
    await http.post(`${apiBase.value}/${createPath.value}`, payload);
    ElMessage.success("新增成功");
  } else {
    await http.put(`${apiBase.value}/${updatePath.value}`, payload);
    ElMessage.success("更新成功");
  }
  formOpen.value = false;
  loadData();
};

const removeRow = async (row) => {
  const id = resolveRowId(row);
  if (!id) {
    ElMessage.warning("未找到记录 ID");
    return;
  }
  await ElMessageBox.confirm("确认删除该记录？", "提示", { type: "warning" });
  await http.delete(`${apiBase.value}/${deletePath.value}/${id}`);
  ElMessage.success("删除成功");
  loadData();
};

const removeSelected = async () => {
  const row = selection.value[0];
  if (!row) {
    return;
  }
  await removeRow(row);
};

const removeBatch = async () => {
  if (!deleteBatchPath.value) {
    return;
  }
  const ids = selection.value.map(resolveRowId).filter(Boolean);
  if (!ids.length) {
    ElMessage.warning("请选择有效的记录");
    return;
  }
  await ElMessageBox.confirm(`确认删除 ${ids.length} 条记录？`, "提示", { type: "warning" });
  await http.delete(`${apiBase.value}/${deleteBatchPath.value}`, { params: { ids } });
  ElMessage.success("删除成功");
  loadData();
};

let keywordTimer = null;
watch(
  () => keyword.value,
  () => {
    currentPage.value = 1;
    if (keywordTimer) {
      clearTimeout(keywordTimer);
    }
    keywordTimer = setTimeout(() => {
      loadData();
    }, 400);
  }
);

onBeforeUnmount(() => {
  if (keywordTimer) {
    clearTimeout(keywordTimer);
  }
});

watch(
  () => route.path,
  () => {
    selection.value = [];
    currentPage.value = 1;
    loadData();
    loadStats();
  }
);

onMounted(() => {
  loadData();
  loadStats();
});

watch(
  () => currentPage.value,
  (page) => {
    if (!page) {
      return;
    }
    loadData();
  }
);
</script>

<style scoped>
.page-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.page-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.table-meta {
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.table-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.image-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.table-image {
  width: 120px;
  height: 76px;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
  object-fit: cover;
  background: #f8fafc;
  cursor: pointer;
}

.image-name {
  color: #64748b;
  font-size: 12px;
  word-break: break-all;
}
</style>
