<template>
  <div class="page-container" style="padding: 24px; text-align: center;">
    <h2 style="margin-bottom: 12px;">代码生成器</h2>
    <a-button type="primary" @click="openCodegen">打开代码生成器</a-button>
  </div>
</template>

<script setup>
const openCodegen = () => {
  window.open('http://localhost:18081', '_blank')
}
</script>
