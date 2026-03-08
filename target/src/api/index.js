import axios from 'axios'

// axios.defaults.baseURL = 'http://localhost:8080'; // 后端端口
const API_BASE_URL = 'http://localhost:8080'
// 创建axios实例
const api = axios.create({
    baseURL: API_BASE_URL,
    timeout: 30000,
    // headers: {
    //     'Content-Type': 'application/json;charset=UTF-8'
    // }
})

// 请求拦截器
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        // 确保使用UTF-8编码
      //  config.headers['Content-Type'] = 'application/json;charset=UTF-8'
        // 调试信息
        if (config.url === '/api/data/upload') {
            console.log('上传请求配置:', config)
            console.log('请求数据:', config.data)
            // 删除可能存在的Content-Type头，让浏览器自动设置
            delete config.headers['Content-Type']
            return config
        }

        // 确保使用UTF-8编码（仅对非文件上传请求）
        if (!config.headers['Content-Type']) {
            config.headers['Content-Type'] = 'application/json;charset=UTF-8'
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)
// api/index.js
// 响应拦截器 - 修复分页数据处理
api.interceptors.response.use(
    (response) => {
        console.log('✅ API成功响应:', response.config.url, response.status, response.data)

        // 处理分页响应（Spring Data Page对象）
        if (response.data &&
            typeof response.data === 'object' &&
            response.data.content !== undefined &&
            response.data.totalElements !== undefined) {
            console.log('📊 检测到分页数据，返回完整分页对象')
            return response.data // 返回完整的分页对象
        }

        // 如果响应数据已经是数组，直接返回
        if (Array.isArray(response.data)) {
            return response.data
        }

        // 否则返回整个响应数据
        return response.data || response
    },
    (error) => {
        console.log('❌ API错误响应:', {
            url: error.config?.url,
            status: error.response?.status,
            data: error.response?.data
        })

        if (error.response?.status === 401) {
            console.log('🔐 401未授权错误，清除本地存储')
            localStorage.removeItem('token')
            localStorage.removeItem('user')

            if (window.location.pathname !== '/login') {
                window.location.href = '/login'
            }
        }

        return Promise.reject(error)
    }
)
// 添加 token 检查函数
export const checkAuth = () => {
    const token = localStorage.getItem('token')
    const user = localStorage.getItem('user')

    if (!token || !user) {
        console.warn('未找到token或用户信息')
        return false
    }

    // 简单的存在性检查，不解析JWT
    console.log('✅ Token和用户信息存在')
    return true
}

// 数据相关API
export const dataApi = {
    upload: (formData) => {
        return api.post('/api/data/upload', formData)
    },
    // 获取上传记录
    getUploadLogs: (params = {}) => {
        return api.get('/api/data/upload-logs', { params })
    },
    // 删除上传记录
    deleteUploadLog: (id) => {
        return api.delete(`/api/data/upload-log/${id}`)
    },
    // 批量删除上传记录
    deleteBatchUploadLogs: (ids) => {
        return api.delete('/api/data/upload-logs/batch', {
            data: ids
        })
    }
}

// ETL相关API
export const etlApi = {
    // 执行ETL
    run: (hdfsPath, operator) => {
        return api.post('/api/etl/run', null, {
            params: { hdfsPath, operator }
        })
    },
    // 获取ETL任务
    getTasks: (params = {}) => {
        return api.get('/api/etl/tasks', { params })
    },
    // 获取ETL统计
    getStats: () => {
        return api.get('/api/etl/stats')
    },
    // 获取任务状态
    getTaskStatus: (taskId) => {
        return api.get(`/api/etl/stats/${taskId}`)
    }
}

// 集群相关API
export const clusterApi = {
    health: () => {
        return api.get('/api/cluster/health')
    }
}
// 质量报告相关API
export const qualityApi = {
    // 获取质量报告列表
    getQualityReportsList(params) {
        return api.get('api/quality-reports/list', { params })
    },

    // 根据任务ID获取详情
    getQualityReportByTaskId(taskId) {
        return api.get(`api/quality-reports/task/${taskId}`)
    },

    // 获取最近的质量报告
    getRecentQualityReports(limit = 5) {
        return api.get('api/quality-reports/recent', { params: { limit } })
    },

    // 获取质量统计
    getQualityStats() {
        return api.get('api/quality-reports/stats/summary')
    }
}
// 添加仪表盘API
// 修改 dashboardApi 配置，使用正确的 api 实例
export const dashboardApi = {
    getStats: () => api.get('/api/dashboard/stats'),
    getRecentUploads: () => api.get('/api/dashboard/recent-uploads'),
    getRecentTasks: () => api.get('/api/etl/tasks', {
        params: {
            page: 0,
            size: 5,
            sort: 'startTime,desc'
        }
    })
}

// 认证相关API
export const authApi = {

    login: (credentials) => {
        return api.post('/api/auth/login', credentials)
    },

    register: (userData) => {
        return api.post('/api/auth/register', userData)
    },
    checkUsername: (username) => {
        return api.get(`/api/auth/check-username/${username}`)
    },

    checkEmail: (email) => {
        return api.get(`/api/auth/check-email/${email}`)
    },

    // 用户管理API
    getUsers: (params = {}) => {
        const queryParams = new URLSearchParams();

        // 恢复正确的参数格式
        if (params.page !== undefined) queryParams.append('page', params.page);
        if (params.size !== undefined) queryParams.append('size', params.size);
        if (params.sortField) queryParams.append('sortField', params.sortField);
        if (params.sortDirection) queryParams.append('sortDirection', params.sortDirection);
        if (params.search) queryParams.append('search', params.search);
        if (params.role) queryParams.append('role', params.role);
        if (params.status) queryParams.append('status', params.status);

        return api.get(`/api/admin/users?${queryParams.toString()}`);
    },

    updateUserStatus: (id, status) => {
        // 尝试使用 PUT 方法而不是 PATCH
        return api.put(`/api/admin/users/${id}/status`, null, {
            params: { status }
        });
    },

    createUser: (userData) => {
        return api.post('/api/admin/users', userData)
    },

    updateUser: (id, userData) => {
        return api.put(`/api/admin/users/${id}`, userData)
    },


    deleteUser: (id) => {
        return api.delete(`/api/admin/users/${id}`)
    }
}

// 商户分析相关API
export const merchantAnalysisApi = {
    // 获取当前商户的仪表板数据（雷达图用）
    getDashboardData: () => api.get('/api/merchant-analysis/dashboard-data'),

    // 获取所有商户的气泡图数据
    getBubbleData: () => api.get('/api/merchant-analysis/bubble-data'),

    // 获取情感趋势数据（堆叠条形图）
    getSentimentTrend: () => api.get('/api/merchant-analysis/sentiment-trend'),

    // 获取玫瑰图数据
    getRoseChart: () => api.get('/api/merchant-analysis/rose-chart'),
    getAllSentimentData: () => api.get('/api/merchant-analysis/all-sentiment-data'),
    // 原有方法保持不变
   // 获取仪表板数据
    getDashboard: () => api.get('/api/merchant-analysis/dashboard'),

    // 获取趋势数据
    getTrend: (months) => api.get(`/api/merchant-analysis/trend?months=${months}`),

    // 获取热门菜品
    getFoods: (limit) => api.get(`/api/merchant-analysis/foods?limit=${limit}`),

    // 获取异常检测
    getAnomalies: () => api.get('/api/merchant-analysis/anomalies'),

    // 获取商户口碑分析
    getOverview: () => api.get('/api/merchant-analysis/overview')
}

// api/index.js - 添加决策辅助API
export const decisionApi = {
    // 获取商户名称列表
    getMerchantNames: () => {
        return api.get('/api/decision/merchants')
    },

    // 获取排行榜类型
    getRankTypes: () => {
        return api.get('/api/decision/rank-types')
    },

    // api/index.js - 修改searchRestaurants方法
    searchRestaurants: (params) => {
        // 确保好评率参数正确传递
        const processedParams = { ...params };
        if (processedParams.minPositiveRate !== undefined) {
            processedParams.minPositiveRate = processedParams.minPositiveRate;
        }

        return api.get('/api/decision/search', {
            params: processedParams
        })
    },

    // 获取排行榜
    getRankings: (params) => {
        return api.get('/api/decision/rankings', { params })
    },

    // 商户对比
    compareMerchants: (merchantNames) => {
        return api.post('/api/decision/compare', merchantNames)
    },

    // 获取热门关键词
    getKeywords: (merchantName, limit = 10) => {
        return api.get(`/api/decision/keywords/${merchantName}`, {
            params: { limit }
        })
    }
}

// 添加这行代码 - 导出 api 实例
export { api }
export default api