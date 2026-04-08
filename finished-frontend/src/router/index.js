import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from "@/net/api.js";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      redirect: "/login"
    },
    {
      path: "/index/:pathMatch(.*)*",
      redirect: to => {
        const pathMatch = to.params.pathMatch
        const suffix = Array.isArray(pathMatch)
          ? pathMatch.join("/")
          : (pathMatch || "")
        return suffix ? `/home/${suffix}` : "/home"
      }
    },
    {
      path: '/',
      name: 'auth',
      component: () => import("@/views/AuthView.vue"),
      children: [
        {
          path: "login",
          name: "auth-login",
          component: () => import("@/views/welcome/LoginView.vue")
        }, {
          path: "register",
          name: "auth-register",
          component: () => import("@/views/welcome/RegisterView.vue")
        }, {
          path: "reset",
          name: "auth-reset",
          component: () => import("@/views/welcome/ResetView.vue")
        }
      ]
    }, {
      path: "/home",
      name: "home",
      component: () => import("@/views/HomeView.vue"),
      children: [
        {
          path: "",
          name: "home-topics",
          component: () => import("@/views/forum/ForumView.vue"),
          children: [
            {
              path: "",
              name: "home-topic-list",
              component: () => import("@/views/forum/TopicListView.vue")
            },
            {
              path: "topic/:tid",
              name: "home-topic-detail",
              component: () => import("@/views/forum/TopicDetailView.vue")
            },
          ]
        },
        {
          path: "user",
          name: "home-user",
          component: () => import("@/views/forum/UserIndexView.vue")
        }, {
          path: "collections",
          name: "home-collections",
          component: () => import("@/views/forum/CollectView.vue")
        },
        {
          path: "user-setting",
          name: "home-user-setting",
          component: () => import("@/views/setting/UserSettingView.vue")
        }, {
          path: "privacy-setting",
          name: "home-privacy-setting",
          component: () => import("@/views/setting/PrivacySettingView.vue")
        }, {
          path: "resource",
          name: "home-resource",
          component: () => import("@/views/forum/ResourceView.vue")
        }, {
          path: "stat",
          name: "home-stat",
          component: () => import("@/views/forum/StatView.vue")
        }, {
          path: "activity",
          name: "home-activity",
          component: () => import("@/views/forum/ActivityView.vue")
        }, {
          path: "quick-access",
          name: "home-quick-access",
          component: () => import("@/views/forum/QuickAccessView.vue")
        }, {
          path: "feedback",
          name: "home-feedback",
          component: () => import("@/views/forum/FeedbackView.vue")
        }, {
          path: "admin",
          name: "home-admin",
          component: () => import("@/views/admin/AdminView.vue")
        }

      ]
    }

  ]
})

router.beforeEach((to, from, next) => {
  const loginStatus = getToken()
  if (to.name && to.name.startsWith("auth-") && loginStatus) {
    next("/home")
  } else if (to.fullPath.startsWith("/home") && !loginStatus) {
    next("/login")
  } else {
    next();
  }
})

export default router
