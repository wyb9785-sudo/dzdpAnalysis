import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

// 导入所有组件
const LoginView = () => import('../views/LoginView.vue')
const RegisterView = () => import('../views/RegisterView.vue')
const MainLayout = () => import('../components/layout/MainLayout.vue')
const MerchantLayout = () => import('../components/layout/MerchantLayout.vue')
const MerchantComparison = ()=> import('../components/layout/MerchantComparison.vue')

const DashboardView = () => import('../views/DashboardView.vue')
const DataUploadView = () => import('../views/DataUploadView.vue')
const ETLMonitorView = () => import('../views/ETLMonitorView.vue')
const QualityReportView = () => import('../views/QualityReportView.vue')
const UserManagementView = () => import('../views/UserManagementView.vue')
const MerchantAnalysisView =()=> import('../views/MerchantAnalysisView.vue')
const DecisionAssistantView =()=> import('../views/DecisionAssistantView.vue')

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: LoginView
    },
    {
        path: '/register',
        name: 'Register',
        component: RegisterView,
        meta: { requiresGuest: true }
    },
    //管理员路由
    {
        path: '/',
        component: MainLayout,
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                redirect: '/dashboard'
            },
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: DashboardView
            },
            {
                path: 'data-upload',
                name: 'DataUpload',
                component: DataUploadView,
                meta: { requiresAdmin: true }
            },
            {
                path: 'etl-monitor',
                name: 'ETLMonitor',
                component: ETLMonitorView,
                meta: { requiresAdmin: true }
            },
            {
                path: 'quality-report/:taskId?',
                name: 'QualityReport',
                component: QualityReportView,
                meta: { requiresAdmin: true }
            },
            {
                path: 'user-management',
                name: 'UserManagement',
                component: UserManagementView,
                meta: {   requiresAdmin: true }
            }
        ]
    },
    // 商户路由 - 简化配置
    {
        path: '/merchant/analysis',
        name: 'MerchantAnalysis',
        component: MerchantAnalysisView,
        meta: { requiresAuth: true, requiresMerchant: true }
    },
    //顾客路由
    {
        path: '/decision-assistant',
        name: 'DecisionAssistant',
        component: DecisionAssistantView,
        meta: { requiresAuth: true, requiresCustomer: true }
    },

    // 添加404页面
    {
        path: '/:pathMatch(.*)*',
        redirect: '/dashboard'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    const userStr = localStorage.getItem('user')
    let user = {}
    try {
        user = userStr ? JSON.parse(userStr) : {}
    } catch (e) {
        console.error('解析用户信息失败:', e)
    }

    console.log('路由检查:', {
        to: to.path,
        hasToken: !!token,
        userRole: user.role
    })

    // 检查是否需要认证
    if (to.meta.requiresAuth && !token) {
        ElMessage.warning('请先登录系统')
        next('/login')
        return
    }
    // 检查管理员权限
    if (to.meta.requiresAdmin && user.role !== 'ADMIN') {
        ElMessage.warning('权限不足，需要管理员权限')
        next('/dashboard')
        return
    }

    // 检查商户权限
    if (to.meta.requiresMerchant && user.role !== 'MERCHANT') {
        ElMessage.warning('权限不足，需要商户权限')
        next('/dashboard')
        return
    }
    // 检查顾客权限
    if (to.meta.requiresCustomer && user.role !== 'CUSTOMER') {
        ElMessage.warning('权限不足，需要顾客权限')
        next('/dashboard')
        return
    }

    // 如果已登录但访问登录页，重定向到首页
    if ((to.path === '/login' || to.path === '/register') && token) {
        if (user.role === 'MERCHANT') {
            next('/merchant/analysis')
        } else {
            next('/dashboard')
        }
        return
    }


    next()
})

export default router