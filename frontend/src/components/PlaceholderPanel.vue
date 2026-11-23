<script setup>
import { Notebook } from '@element-plus/icons-vue'

defineProps({
  title: { type: String, required: true },
  description: { type: String, required: true },
  owner: { type: String, required: true },
  nextStep: { type: String, required: true },
})
</script>

<template>
  <section class="placeholder-page">
    <div class="page-heading">
      <div>
        <p class="page-eyebrow">MODULE ENTRY / 模块入口</p>
        <h1>{{ title }}</h1>
      </div>
      <el-tag type="info" effect="plain">待后端联调</el-tag>
    </div>

    <div class="placeholder-card">
      <div class="placeholder-icon" aria-hidden="true">
        <el-icon :size="30"><Notebook /></el-icon>
      </div>
      <div class="placeholder-copy">
        <h2>页面入口已建立</h2>
        <p>{{ description }}</p>
      </div>
      <dl class="placeholder-meta">
        <div>
          <dt>协作模块</dt>
          <dd>{{ owner }}</dd>
        </div>
        <div>
          <dt>下一步</dt>
          <dd>{{ nextStep }}</dd>
        </div>
      </dl>
    </div>
  </section>
</template>
