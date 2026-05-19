import {reactive} from 'vue'
import { defineStore } from 'pinia'
import axios from "axios";

export const useAppStore = defineStore('app', () => {
  const user = reactive({
    id: -1,
    username: "",
    email: "",
    role: "",
    avatar: "",
    registerTime: null,
  })
  const forum = reactive({
    types: [],
    _typesLoaded: false,
    topicListRefreshVersion: 0
  })
  function requestTopicListRefresh() {
    forum.topicListRefreshVersion += 1
  }
  function findTypeById(id) {
    for (let type of forum.types) {
      if (type.id === id)  {
        return type
      }
    }
  }
  function findTypeByName(name) {
    return forum.types.find(t => t.name === name)
  }
  const geo = reactive({
    loading: true,
    label: "位置识别中...",
    ip: "",
    city: "",
    region: "",
  })

  async function loadGeo() {
    if (!geo.loading && geo.city) return
    geo.loading = true
    try {
      const response = await fetch("https://ipapi.co/json/", {
        headers: { Accept: "application/json" }
      })
      if (!response.ok) throw new Error("ipapi request failed")
      const data = await response.json()
      geo.ip = data.ip || ""
      geo.city = data.city || ""
      geo.region = data.region || ""
      geo.label = [geo.region, geo.city].filter(Boolean).join(" · ") || "暂未识别到位置"
    } catch (e) {
      geo.label = "位置识别失败"
    } finally {
      geo.loading = false
    }
  }

  const getAvatar = (avatar, username = 'U') => {
    if (avatar) {
      return `${axios.defaults.baseURL}/images${avatar}`;
    }
    return `https://ui-avatars.com/api/?name=${encodeURIComponent(username)}&background=random&color=fff`;
  };

  return { user, forum, geo, findTypeById, findTypeByName, requestTopicListRefresh, getAvatar, loadGeo}
})
