<template>
  <div class="page-card">
    <el-alert
      type="info"
      show-icon
      :closable="false"
      title="小程序首页顶部的通知栏，只显示「排序」值最小的那一条公告。"
      class="page-hint"
    />

    <div class="page-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">发布公告</el-button>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" style="width: 100%">
      <el-table-column prop="idx" label="排序" width="90" sortable />
      <el-table-column prop="content" label="公告内容" min-width="320" show-overflow-tooltip />
      <el-table-column label="小程序展示" width="120">
        <template #default="scope">
          <el-tag v-if="scope.row.id === topNoticeId" type="success" effect="dark">当前展示</el-tag>
          <span v-else class="muted">—</span>
        </template>
      </el-table-column>
      <el-table-column prop="updated" label="更新时间" width="180" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="scope">
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

    <el-dialog v-model="formOpen" :title="formTitle" width="560px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="公告内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请输入公告内容，会显示在小程序首页顶部滚动通知栏"
          />
        </el-form-item>
        <el-form-item label="排序" prop="idx">
          <el-input-number v-model="formData.idx" :min="0" controls-position="right" />
          <span class="form-tip">数字越小越靠前，最小的那条会显示在小程序上</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Refresh } from "@element-plus/icons-vue";
import http from "../api/http";

const API = "/sale-server/api/v1/notice";

const loading = ref(false);
const submitting = ref(false);
const rows = ref([]);
const totalRows = ref(0);
const currentPage = ref(1);
const pageSize = 10;

const formOpen = ref(false);
const formRef = ref(null);
const editingId = ref(null);
const formData = ref({ content: "", idx: 0 });

const formTitle = computed(() => (editingId.value ? "编辑公告" : "发布公告"));

// 小程序取的是 notice/top/1，即 idx 升序、id 降序的第一条
const topNoticeId = computed(() => {
  if (!rows.value.length) {
    return null;
  }
  const sorted = [...rows.value].sort(
    (a, b) => Number(a.idx || 0) - Number(b.idx || 0) || Number(b.id) - Number(a.id)
  );
  return sorted[0].id;
});

const rules = {
  content: [{ required: true, message: "公告内容不能为空", trigger: "blur" }],
  idx: [{ required: true, message: "排序不能为空", trigger: "blur" }]
};

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

const openCreate = () => {
  editingId.value = null;
  formData.value = { content: "", idx: 0 };
  formOpen.value = true;
};

const openEdit = (row) => {
  editingId.value = row.id;
  formData.value = { content: row.content, idx: Number(row.idx || 0) };
  formOpen.value = true;
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
      ElMessage.success("公告已更新，小程序刷新即可看到");
    } else {
      await http.post(`${API}/insert`, formData.value);
      ElMessage.success("公告已发布，小程序刷新即可看到");
    }
    formOpen.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
};

const removeRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除这条公告吗？`, "删除确认", {
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

.form-tip {
  margin-left: 12px;
  color: #9ca3af;
  font-size: 12px;
}

.muted {
  color: #d1d5db;
}
</style>
