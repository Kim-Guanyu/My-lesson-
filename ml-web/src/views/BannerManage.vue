<template>
  <div class="page-card">
    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="小程序首页轮播只展示「排序」值最小的前 5 张。换图后小程序刷新即可看到（后端已自动清除缓存）。"
      class="page-hint"
    />

    <div class="page-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增轮播图</el-button>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" style="width: 100%">
      <el-table-column prop="idx" label="排序" width="90" sortable />
      <el-table-column label="图片" width="220">
        <template #default="scope">
          <img
            v-if="scope.row.url"
            class="banner-thumb"
            :src="imageUrl(scope.row.url)"
            :alt="scope.row.url"
            @click="preview(scope.row.url)"
          />
          <span v-else class="muted">未设置</span>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="文件名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="info" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="小程序展示" width="120">
        <template #default="scope">
          <el-tag v-if="displayedIds.includes(scope.row.id)" type="success" effect="dark">展示中</el-tag>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" size="small" @click="openPicker(scope.row)">换图</el-button>
          <el-button link type="primary" size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="removeRow(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="totalRows"
      class="table-pagination"
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="totalRows"
      layout="total, prev, pager, next"
    />

    <!-- 新增 / 编辑基本信息 -->
    <el-dialog v-model="formOpen" :title="formTitle" width="520px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="80px">
        <el-form-item label="描述" prop="info">
          <el-input v-model="formData.info" placeholder="用于后台辨识，不显示在小程序上" />
        </el-form-item>
        <el-form-item label="排序" prop="idx">
          <el-input-number v-model="formData.idx" :min="0" controls-position="right" />
          <span class="form-tip">数字越小越靠前，前 5 张会显示在小程序上</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ editingId ? "保存" : "下一步：选图" }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 从 MinIO 已有图片中选图 -->
    <el-dialog v-model="pickerOpen" title="选择图片" width="760px">
      <div class="picker-toolbar">
        <span class="muted">共 {{ minioImages.length }} 张，点击选中后保存</span>
        <el-button link type="primary" :icon="Refresh" @click="loadMinioImages">刷新图库</el-button>
      </div>

      <div v-loading="pickerLoading" class="picker-grid">
        <div
          v-for="name in minioImages"
          :key="name"
          class="picker-item"
          :class="{ active: pickedUrl === name }"
          @click="pickedUrl = name"
        >
          <img :src="imageUrl(name)" :alt="name" />
          <div class="picker-name" :title="name">{{ name }}</div>
        </div>
        <el-empty v-if="!pickerLoading && !minioImages.length" description="MinIO banner 目录下暂无图片" />
      </div>

      <template #footer>
        <el-button @click="pickerOpen = false">取消</el-button>
        <el-button type="primary" :disabled="!pickedUrl" :loading="submitting" @click="confirmPick">
          确定使用这张
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Refresh } from "@element-plus/icons-vue";
import http from "../api/http";

const API = "/sale-server/api/v1/banner";
const MINIO_BASE_URL = (
  import.meta.env.VITE_MINIO_BASE_URL || "http://192.168.211.132:9000/my-lesson"
).replace(/\/$/, "");

// 小程序取 banner/top/5
const TOP_N = 5;

const loading = ref(false);
const submitting = ref(false);
const rows = ref([]);
const totalRows = ref(0);
const currentPage = ref(1);
const pageSize = 10;

const formOpen = ref(false);
const formRef = ref(null);
const editingId = ref(null);
const formData = ref({ info: "", idx: 0 });

const pickerOpen = ref(false);
const pickerLoading = ref(false);
const minioImages = ref([]);
const pickedUrl = ref("");
// 存整行而不是 id：新建的记录可能不在当前分页里，用 id 反查会找不到
const pickingRow = ref(null);

const formTitle = computed(() => (editingId.value ? "编辑轮播图" : "新增轮播图"));

const displayedIds = computed(() =>
  [...rows.value]
    .sort((a, b) => Number(a.idx || 0) - Number(b.idx || 0) || Number(b.id) - Number(a.id))
    .slice(0, TOP_N)
    .map((row) => row.id)
);

const rules = {
  idx: [{ required: true, message: "排序不能为空", trigger: "blur" }]
};

const imageUrl = (name) => `${MINIO_BASE_URL}/banner/${encodeURIComponent(name)}`;

const preview = (name) => window.open(imageUrl(name), "_blank", "noopener,noreferrer");

const loadData = async () => {
  loading.value = true;
  try {
    const data = await http.get(`${API}/page`, {
      params: { pageNum: currentPage.value, pageSize }
    });
    rows.value = data?.records || [];
    totalRows.value = Number(data?.totalRow || 0);
  } catch (error) {
    rows.value = [];
    totalRows.value = 0;
  } finally {
    loading.value = false;
  }
};

const loadMinioImages = async () => {
  pickerLoading.value = true;
  try {
    minioImages.value = (await http.get(`${API}/minioImages`)) || [];
  } catch (error) {
    minioImages.value = [];
  } finally {
    pickerLoading.value = false;
  }
};

const openCreate = () => {
  editingId.value = null;
  formData.value = { info: "", idx: 0 };
  formOpen.value = true;
};

const openEdit = (row) => {
  editingId.value = row.id;
  formData.value = { info: row.info || "", idx: Number(row.idx || 0) };
  formOpen.value = true;
};

const openPicker = (row) => {
  pickingRow.value = row;
  pickedUrl.value = row.url || "";
  pickerOpen.value = true;
  loadMinioImages();
};

/** 取全部轮播图中 id 最大的一条，即最近新建的那条 */
const findLatestBanner = async () => {
  try {
    const all = (await http.get(`${API}/list`)) || [];
    return all.reduce((max, row) => (!max || Number(row.id) > Number(max.id) ? row : max), null);
  } catch (error) {
    return null;
  }
};

const submitForm = async () => {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) {
      return;
    }
  }
  submitting.value = true;
  try {
    if (editingId.value) {
      await http.put(`${API}/update`, { id: editingId.value, ...formData.value });
      ElMessage.success("已保存");
      formOpen.value = false;
      await loadData();
    } else {
      // 新增接口只收 idx / info，图片要再走一次选图
      await http.post(`${API}/insert`, formData.value);
      formOpen.value = false;
      await loadData();
      // insert 只返回 boolean，拿不到新记录的 id。
      // 走 list 取全量（不受分页影响），id 自增，最大的那条就是刚建的。
      const created = await findLatestBanner();
      if (created) {
        ElMessage.success("已创建，请为它选一张图片");
        openPicker(created);
      } else {
        ElMessage.success("已创建，请在列表中点「换图」为它选择图片");
      }
    }
  } finally {
    submitting.value = false;
  }
};

const confirmPick = async () => {
  const row = pickingRow.value;
  if (!row) {
    return;
  }
  submitting.value = true;
  try {
    // update 接口要求 idx 非空，这里把原值一并带上，只改 url
    await http.put(`${API}/update`, {
      id: row.id,
      idx: Number(row.idx || 0),
      info: row.info || "",
      url: pickedUrl.value
    });
    ElMessage.success("图片已更换，小程序刷新即可看到");
    pickerOpen.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
};

const removeRow = async (row) => {
  try {
    await ElMessageBox.confirm("确定删除这张轮播图吗？", "删除确认", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
  } catch (error) {
    return;
  }
  await http.delete(`${API}/delete/${row.id}`);
  ElMessage.success("已删除");
  await loadData();
};

watch(currentPage, loadData);
onMounted(loadData);
</script>

<style scoped>
.page-hint {
  margin-bottom: 12px;
}

.table-pagination {
  margin-top: 12px;
  justify-content: flex-end;
}

.banner-thumb {
  width: 180px;
  height: 72px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #e5e7eb;
}

.picker-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.picker-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  max-height: 420px;
  overflow-y: auto;
}

.picker-item {
  border: 2px solid transparent;
  border-radius: 6px;
  padding: 4px;
  cursor: pointer;
  transition: border-color 0.15s;
}

.picker-item:hover {
  border-color: #c7d2fe;
}

.picker-item.active {
  border-color: #3b82f6;
  background: rgba(59, 130, 246, 0.06);
}

.picker-item img {
  width: 100%;
  height: 90px;
  object-fit: cover;
  border-radius: 4px;
  display: block;
}

.picker-name {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.form-tip {
  margin-left: 12px;
  color: #9ca3af;
  font-size: 12px;
}

.muted {
  color: #9ca3af;
  font-size: 12px;
}
</style>
