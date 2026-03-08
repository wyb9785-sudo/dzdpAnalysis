import axios from 'axios'

const API_BASE_URL = 'http://localhost:8080/api'

export const etlApi = {
    // 运行ETL任务
    runETL: (hdfsPath, operator) => {
        return axios.post(`${API_BASE_URL}/etl/run`, null, {
            params: { hdfsPath, operator }
        })
    },

    // 获取任务状态
    getTaskStatus: (taskId) => {
        return axios.get(`${API_BASE_URL}/etl/stats/${taskId}`)
    },

    // 获取所有任务
    getAllTasks: () => {
        return axios.get(`${API_BASE_URL}/etl/tasks`)
    },

    // 获取质量报告
    getQualityReport: (taskId) => {
        return axios.get(`${API_BASE_URL}/etl/quality-report/${taskId}`)
    }
}

export default etlApi