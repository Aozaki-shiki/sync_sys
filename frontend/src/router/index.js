import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
    {
        path: '/',
        redirect: (to) => {
            const authStore = useAuthStore()
            if (!authStore.isAuthenticated) {
                return '/login'
            }
            return authStore.isAdmin ? '/admin' : '/orders/new'
        }
    },
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { requiresGuest: true }
    },
    {
        path: '/orders/new',
        name: 'OrderNew',
        component: () => import('../views/user/OrderNew.vue'),
        meta: { requiresAuth: true, roles: ['USER', 'ADMIN'] }
    },
    {
        path: '/admin',
        name: 'AdminLayout',
        component: () => import('../components/layout/AdminLayout.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] },
        redirect: '/admin/queries/complex',
        children: [
            {
                path: 'queries/complex',
                name: 'ComplexQuery',
                component: () => import('../views/admin/ComplexQuery.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'] }
            },
            {
                path: 'reports/daily-sync',
                name: 'DailySyncReport',
                component: () => import('../views/admin/DailySyncReport.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'] }
            },
            {
                path: 'conflicts',
                name: 'ConflictManagement',
                component: () => import('../views/admin/ConflictManagement.vue'),
                meta: { requiresAuth: true, roles: ['ADMIN'] }
            }
        ]
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/'
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// Navigation guard
router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()

    // Check if route requires authentication
    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
        next('/login')
        return
    }

    // Check if route requires guest (not logged in)
    if (to.meta.requiresGuest && authStore.isAuthenticated) {
        next(authStore.isAdmin ? '/admin' : '/orders/new')
        return
    }

    // Check role-based access
    if (to.meta.roles && !to.meta.roles.includes(authStore.role)) {
        next(authStore.isAdmin ? '/admin' : '/orders/new')
        return
    }

    next()
})

export default router