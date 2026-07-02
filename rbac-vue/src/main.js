import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import { hasAnyPermission } from './utils/permission'

const app = createApp(App)

const removeElement = (el) => {
  el.parentNode && el.parentNode.removeChild(el)
}

// 自定义权限指令 v-hasPerm
app.directive('hasPerm', {
  mounted(el, binding) {
    const { value } = binding
    if (!hasAnyPermission(value)) {
      removeElement(el)
    }
  },
  updated(el, binding) {
    if (binding.value !== binding.oldValue && !hasAnyPermission(binding.value)) {
      removeElement(el)
    }
  }
})

app.use(createPinia())
app.use(router)
app.use(Antd)
app.mount('#app')
