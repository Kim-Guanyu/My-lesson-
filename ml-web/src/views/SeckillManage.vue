<template>
  <div class="page-card">
    <el-alert
      type="warning"
      show-icon
      :closable="false"
      class="page-hint"
    >
      <template #title>
        小程序秒杀专区只展示<b>开始时间是今天</b>的活动。新建活动后必须先添加秒杀课程，
        再点「开启活动」—— 开启动作会把 Redis 库存预热好，否则小程序上点了抢不到。
      </template>
    </el-alert>

    <div class="page-toolbar">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增活动</el-button>
      <el-button :icon="Refresh" @click="loadData">刷新</el-button>
    </div>

    <el-table
      :data="rows"
      v-loading="loading"
      style="width: 100%"
      row-key="id"
      @expand-change="onExpand"
    >
      <el-table-column type="expand">
        <template #default="scope">
          <div class="detail-panel">
            <div class="detail-head">
              <span class="detail-title">秒杀课程</span>
              <el-button size="small" type="primary" :icon="Plus" @click="openAddDetail(scope.row)">
                添加课程
              </el-button>
            </div>
            <el-table :data="detailMap[scope.row.id] || []" size="small" v-loading="detailLoading === scope.row.id">
              <el-table-column prop="courseTitle" label="课程" min-width="200" />
              <el-table-column prop="coursePrice" label="原价" width="100" />
              <el-table-column prop="skPrice" label="秒杀价" width="100" />
              <el-table-column prop="skCount" label="库存" width="90" />
              <el-table-column label="操作" width="90">
                <template #default="d">
                  <el-button link type="danger" size="small" @click="removeDetail(scope.row, d.row)">
                    移除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty
              v-if="!detailLoading && !(detailMap[scope.row.id] || []).length"
              description="还没有秒杀课程，先添加一个才能开启活动"
              :image-size="60"
            />
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="title" label="活动标题" min-width="160" />
      <el-table-column prop="startTime" label="开始时间" width="180" />
      <el-table-column prop="endTime" label="结束时间" width="180" />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="STATUS_TAG[scope.row.status] || 'info'">
            {{ STATUS_TEXT[scope.row.status] || "未知" }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="小程序" width="100">
        <template #default="scope">
          <el-tag v-if="isToday(scope.row.startTime)" type="success" size="small">今日可见</el-tag>
          <el-tooltip v-else content="开始时间不是今天，小程序 seckill/today 查不到" placement="top">
            <el-tag type="info" size="small">不可见</el-tag>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="scope">
          <el-button
            v-if="scope.row.status !== STATUS.STARTED"
            link
            type="success"
            size="small"
            @click="startActivity(scope.row)"
          >
            开启活动
          </el-button>
          <el-button
            v-else
            link
            type="warning"
            size="small"
            @click="stopActivity(scope.row)"
          >
            结束活动
          </el-button>
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

    <!-- 新增 / 编辑活动 -->
    <el-dialog v-model="formOpen" :title="formTitle" width="560px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="formData.title" placeholder="如：午间秒杀场" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="formData.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="formData.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="活动描述" prop="info">
          <el-input v-model="formData.info" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加秒杀课程 -->
    <el-dialog v-model="detailFormOpen" title="添加秒杀课程" width="520px">
      <el-form ref="detailFormRef" :model="detailForm" :rules="detailRules" label-width="90px">
        <el-form-item label="课程" prop="fkCourseId">
          <el-select
            v-model="detailForm.fkCourseId"
            filterable
            placeholder="搜索并选择课程"
            style="width: 100%"
            @change="onCourseChange"
          >
            <el-option
              v-for="course in courses"
              :key="course.id"
              :label="`${course.title}（原价 ${course.price}）`"
              :value="course.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价" prop="skPrice">
          <el-input-number v-model="detailForm.skPrice" :min="0" :precision="2" controls-position="right" />
          <span v-if="selectedCoursePrice !== null" class="form-tip">
            原价 {{ selectedCoursePrice }}
          </span>
        </el-form-item>
        <el-form-item label="库存" prop="skCount">
          <el-input-number v-model="detailForm.skCount" :min="1" controls-position="right" />
          <span class="form-tip">开启活动时会按这个数量预热 Redis</span>
        </el-form-item>
        <el-form-item label="描述" prop="info">
          <el-input v-model="detailForm.info" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailFormOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDetail">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Refresh } from "@element-plus/icons-vue";
import http from "../api/http";

const API = "/sale-server/api/v1/seckill";
const DETAIL_API = "/sale-server/api/v1/seckillDetail";
const COURSE_API = "/course-server/api/v1/course";

// 与后端 ML.Seckill 保持一致：0未开始，1已开始，2已结束
const STATUS = { NOT_START: 0, STARTED: 1, ENDED: 2 };
const STATUS_TEXT = { 0: "未开始", 1: "已开始", 2: "已结束" };
const STATUS_TAG = { 0: "info", 1: "success", 2: "danger" };

const loading = ref(false);
const submitting = ref(false);
const rows = ref([]);
const totalRows = ref(0);
const currentPage = ref(1);
const pageSize = 10;

const detailMap = ref({});
const detailLoading = ref(null);

const formOpen = ref(false);
const formRef = ref(null);
const editingId = ref(null);
const formData = ref({ title: "", startTime: "", endTime: "", info: "", status: STATUS.NOT_START });

const detailFormOpen = ref(false);
const detailFormRef = ref(null);
const detailForm = ref({ fkSeckillId: null, fkCourseId: null, skPrice: 0, skCount: 1, info: "" });
const courses = ref([]);

const formTitle = computed(() => (editingId.value ? "编辑活动" : "新增活动"));

const selectedCoursePrice = computed(() => {
  const course = courses.value.find((item) => item.id === detailForm.value.fkCourseId);
  return course ? course.price : null;
});

const rules = {
  title: [{ required: true, message: "活动标题不能为空", trigger: "blur" }],
  startTime: [{ required: true, message: "开始时间不能为空", trigger: "change" }],
  endTime: [{ required: true, message: "结束时间不能为空", trigger: "change" }]
};

const detailRules = {
  fkCourseId: [{ required: true, message: "请选择课程", trigger: "change" }],
  skPrice: [{ required: true, message: "秒杀价不能为空", trigger: "blur" }],
  skCount: [{ required: true, message: "库存不能为空", trigger: "blur" }]
};

const isToday = (value) => {
  if (!value) {
    return false;
  }
  const today = new Date();
  const pad = (n) => String(n).padStart(2, "0");
  const prefix = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
  return String(value).startsWith(prefix);
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

const loadDetails = async (seckillId) => {
  detailLoading.value = seckillId;
  try {
    const data = await http.get(`${DETAIL_API}/page`, {
      params: { pageNum: 1, pageSize: 100, seckillId }
    });
    detailMap.value = { ...detailMap.value, [seckillId]: data?.records || [] };
  } catch (error) {
    detailMap.value = { ...detailMap.value, [seckillId]: [] };
  } finally {
    detailLoading.value = null;
  }
};

const onExpand = (row, expanded) => {
  if (expanded) {
    loadDetails(row.id);
  }
};

const loadCourses = async () => {
  if (courses.value.length) {
    return;
  }
  courses.value = (await http.get(`${COURSE_API}/simpleList`)) || [];
};

const onCourseChange = () => {
  // 默认把秒杀价填成原价，让运营在此基础上下调
  if (selectedCoursePrice.value !== null && !detailForm.value.skPrice) {
    detailForm.value.skPrice = selectedCoursePrice.value;
  }
};

const openCreate = () => {
  editingId.value = null;
  formData.value = { title: "", startTime: "", endTime: "", info: "", status: STATUS.NOT_START };
  formOpen.value = true;
};

const openEdit = (row) => {
  editingId.value = row.id;
  formData.value = {
    title: row.title,
    startTime: row.startTime,
    endTime: row.endTime,
    info: row.info || "",
    status: row.status
  };
  formOpen.value = true;
};

const submitForm = async () => {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false);
    if (!valid) {
      return;
    }
  }
  if (formData.value.endTime <= formData.value.startTime) {
    ElMessage.error("结束时间必须晚于开始时间");
    return;
  }
  submitting.value = true;
  try {
    if (editingId.value) {
      await http.put(`${API}/update`, { id: editingId.value, ...formData.value });
      ElMessage.success("已保存");
    } else {
      await http.post(`${API}/insert`, formData.value);
      ElMessage.success("活动已创建，展开这一行添加秒杀课程后再开启");
    }
    formOpen.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
};

const openAddDetail = async (row) => {
  detailForm.value = { fkSeckillId: row.id, fkCourseId: null, skPrice: 0, skCount: 1, info: "" };
  detailFormOpen.value = true;
  await loadCourses();
};

const submitDetail = async () => {
  if (detailFormRef.value) {
    const valid = await detailFormRef.value.validate().catch(() => false);
    if (!valid) {
      return;
    }
  }
  submitting.value = true;
  try {
    await http.post(`${DETAIL_API}/insert`, detailForm.value);
    ElMessage.success("已添加");
    detailFormOpen.value = false;
    await loadDetails(detailForm.value.fkSeckillId);
  } finally {
    submitting.value = false;
  }
};

const removeDetail = async (row, detail) => {
  try {
    await ElMessageBox.confirm(`确定移除课程「${detail.courseTitle}」吗？`, "移除确认", {
      type: "warning",
      confirmButtonText: "移除",
      cancelButtonText: "取消"
    });
  } catch (error) {
    return;
  }
  await http.delete(`${DETAIL_API}/delete/${detail.id}`);
  ElMessage.success("已移除");
  await loadDetails(row.id);
};

const startActivity = async (row) => {
  // 后端也会校验，这里先给出更明确的提示
  const details = detailMap.value[row.id];
  if (details && !details.length) {
    ElMessage.error("请先添加至少一个秒杀课程");
    return;
  }
  if (!isToday(row.startTime)) {
    try {
      await ElMessageBox.confirm(
        "这个活动的开始时间不是今天，小程序首页查不到它（seckill/today 只查今天的）。仍要开启吗？",
        "提示",
        { type: "warning", confirmButtonText: "仍要开启", cancelButtonText: "取消" }
      );
    } catch (error) {
      return;
    }
  }
  await http.post(`${API}/start/${row.id}`);
  ElMessage.success("活动已开启，Redis 库存已预热，小程序刷新即可抢购");
  await loadData();
};

const stopActivity = async (row) => {
  try {
    await ElMessageBox.confirm("确定结束这个活动吗？结束后小程序不再显示抢购按钮。", "结束确认", {
      type: "warning",
      confirmButtonText: "结束",
      cancelButtonText: "取消"
    });
  } catch (error) {
    return;
  }
  await http.post(`${API}/stop/${row.id}`);
  ElMessage.success("活动已结束");
  await loadData();
};

const removeRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除活动「${row.title}」吗？`, "删除确认", {
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

.detail-panel {
  padding: 8px 24px 16px;
  background: #f9fafb;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.detail-title {
  font-weight: 600;
  color: #374151;
}

.form-tip {
  margin-left: 12px;
  color: #9ca3af;
  font-size: 12px;
}
</style>
