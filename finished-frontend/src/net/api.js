import axios from "axios";
import { ElMessage } from "element-plus";
import router from "@/router/index.js";

// ===== 统一 API 基础地址（从 Vite 环境变量读取） =====
const apiBaseURL = import.meta.env.VITE_API_BASE_URL
const baseURL = apiBaseURL === undefined ? "http://localhost:8080" : apiBaseURL;

const tokenAndExpire = "tokenAndExpire"

const defaultFailure = (message, code, url) => {
    ElMessage.warning({ message: message, plain: true });
}

const defaultError = (message) => {
    ElMessage.error({ message: message, plain: true })
}

const getToken = () => {
    const token = sessionStorage.getItem(tokenAndExpire) || localStorage.getItem(tokenAndExpire);
    if (!token) return null;
    const tokenObj = JSON.parse(token);
    if (new Date(tokenObj.expire) < new Date()) {
        deleteToken();
        ElMessage.warning({ message: "登录状态过期, 请重新登录", plain: true });
        return null;
    }
    return tokenObj.token;
}

const deleteToken = () => {
    localStorage.removeItem(tokenAndExpire);
    sessionStorage.removeItem(tokenAndExpire);
}

const storeToken = (token, remember, expire, role) => {
    const auth = { token: token, expire: expire, role: role };
    if (remember) {
        localStorage.setItem(tokenAndExpire, JSON.stringify(auth));
    } else {
        sessionStorage.setItem(tokenAndExpire, JSON.stringify(auth));
    }
}

const doGet = (url, header, success, failure = defaultFailure, error = defaultError) => {
    axios.get(baseURL + url, { headers: header }).then((res) => {
        if (res.data.code === 200) {
            success(res.data.data)
        } else {
            failure(res.data.message, res.data.code, url);
        }
    }).catch(err => {
        error(err)
    })
}

const get = (url, success, failure = defaultFailure) => {
    doGet(url, {
        "Authorization": `Bearer ${getToken()}`,
    }, success, failure)
}

const doPost = (url, data, header, success, failure = defaultFailure, error = defaultError) => {
    axios.post(baseURL + url, data, { headers: header }).then((res) => {
        if (res.data.code === 200) {
            success(res.data.data)
        } else {
            failure(res.data.message, res.data.code, url)
        }
    }).catch(err => {
        error(err)
    })
}

const post = (url, data, success, failure = defaultFailure, error = defaultError) => {
    doPost(url, data, {
        "Authorization": `Bearer ${getToken()}`,
    }, success, failure, error)
}

const put = (url, data, success, failure = defaultFailure, error = defaultError) => {
    axios.put(baseURL + url, data, {
        headers: { "Authorization": `Bearer ${getToken()}` }
    }).then((res) => {
        if (res.data.code === 200) {
            success(res.data.data)
        } else {
            failure(res.data.message, res.data.code, url)
        }
    }).catch(err => {
        error(err)
    })
}

const del = (url, success, failure = defaultFailure, error = defaultError) => {
    axios.delete(baseURL + url, {
        headers: { "Authorization": `Bearer ${getToken()}` }
    }).then((res) => {
        if (res.data.code === 200) {
            success(res.data.data)
        } else {
            failure(res.data.message, res.data.code, url)
        }
    }).catch(err => {
        error(err)
    })
}

const login = (username, password, remember) => {
    const formData = new URLSearchParams();
    formData.append("username", username);
    formData.append("password", password);
    doPost("/api/auth/login", formData, {
        "Content-Type": "application/x-www-form-urlencoded"
    }, (data) => {
        storeToken(data.token, remember, data.expireTime, data.role);
        ElMessage.success({ message: `欢迎用户${data.username}登录成功`, plain: true })
        const adminRoles = ['admin', 'content_admin', 'moderator']
        if (adminRoles.includes(data.role)) {
            router.push("/admin")
        } else {
            router.push("/home")
        }
    })
}

const logout = (success) => {
    doGet("/api/auth/logout", {
        "Authorization": `Bearer ${getToken()}`
    }, () => {
        deleteToken()
        if (success) {
            success()
        } else {
            router.push("/login")
        }
    })
}

const askCodeForType = (email, type, success) => {
    doGet(`/api/auth/ask-code?email=${email}&type=${type}`, {}, success)
}

export { baseURL, doGet, doPost, login, logout, askCodeForType, get, post, put, del, getToken }
