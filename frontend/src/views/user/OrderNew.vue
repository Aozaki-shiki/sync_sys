<template>
  <div class="order-page">
    <div class="header">
      <div>
        <h1>📦 Create New Order</h1>
        <p class="subtitle">Submit your order and it will be synced across databases</p>
      </div>
      <button @click="handleLogout" class="btn-logout-header">
        Logout
      </button>
    </div>

    <div class="user-info">
      <p><strong>User:</strong> {{ authStore.username }} (ID: {{ authStore.userId }})</p>
    </div>

    <div class="order-form-container">
      <form @submit.prevent="handleSubmit" class="order-form">
        <div class="form-grid">
          <div class="form-group">
            <label for="product">Product *</label>
            <select
                id="product"
                v-model="form.productId"
                required
                :disabled="loading || loadingProducts"
            >
              <option value="">-- Select a product --</option>
              <option
                  v-for="product in products"
                  :key="product.productId"
                  :value="product.productId"
              >
                {{ product.productName }} - ¥{{ product.price }}
              </option>
            </select>
          </div>

          <div class="form-group">
            <label for="quantity">Quantity *</label>
            <input
                id="quantity"
                v-model.number="form.quantity"
                type="number"
                min="1"
                placeholder="Enter quantity"
                required
                :disabled="loading"
            />
          </div>

          <div class="form-group full-width">
            <label for="address">Shipping Address *</label>
            <textarea
                id="address"
                v-model="form.shippingAddress"
                rows="3"
                placeholder="Enter shipping address"
                required
                :disabled="loading"
            ></textarea>
          </div>

          <div class="form-group">
            <label for="writeDb">Target Database *</label>
            <select
                id="writeDb"
                v-model="form.writeDb"
                required
                :disabled="loading"
            >
              <option value="">-- Select database --</option>
              <option value="MYSQL">MySQL</option>
              <option value="POSTGRES">PostgreSQL</option>
            </select>
            <small class="help-text">Select which database to write the order to</small>
          </div>
        </div>

        <div v-if="error" class="error-message">
          {{ error }}
        </div>

        <div v-if="success" class="success-message">
          {{ success }}
        </div>

        <button type="submit" class="btn-submit" :disabled="loading || loadingProducts">
          {{ loading ? 'Submitting...' : 'Submit Order' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
/**
 * Order creation page
 *
 * Note: productId and userId are stored as strings to avoid JavaScript Number precision loss.
 * JavaScript numbers are IEEE 754 double-precision floats with a safe integer range of
 * -(2^53 - 1) to (2^53 - 1). Snowflake IDs are 64-bit integers (18-19 digits) that exceed
 * this limit. Using Number() would truncate the last digits (e.g., 1000000000000000001 becomes
 * 1000000000000000000), causing backend lookup failures.
 */
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import api from '../../api'
const router = useRouter()
const authStore = useAuthStore()
const products = ref([])
const loadingProducts = ref(false)
const form = ref({
  productId: '',
  quantity: 1,
  shippingAddress: '',
  writeDb: 'MYSQL'
})
const loading = ref(false)
const error = ref(null)
const success = ref(null)
onMounted(async () => {
  await loadProducts()
})
async function loadProducts() {
  loadingProducts.value = true
  try {
    const response = await api.get('/api/products')
    products.value = response.data.data || []
  } catch (err) {
    console.error('Error loading products:', err)
    error.value = 'Failed to load products'
  } finally {
    loadingProducts.value = false
  }
}
async function handleSubmit() {
  loading.value = true
  error.value = null
  success.value = null
  try {
    // Keep userId and productId as strings to preserve full precision of 64-bit Snowflake IDs
    const payload = {
      userId: authStore.userId,
      productId: form.value.productId,
      quantity: form.value.quantity,
      shippingAddress: form.value.shippingAddress,
      writeDb: form.value.writeDb
    }
    const response = await api.post('/api/orders/place', payload)

    if (response.data.code === 0) {
      success.value = `Order created successfully! Order ID: ${response.data.data.orderId}, Database: ${response.data.data.writeDb}`

      // Reset form
      form.value = {
        productId: '',
        quantity: 1,
        shippingAddress: '',
        writeDb: 'MYSQL'
      }
    } else {
      error.value = response.data.message || 'Failed to create order'
    }
  } catch (err) {
    console.error('Submit error:', err)
    error.value = err.response?.data?.message || 'Failed to submit order'
  } finally {
    loading.value = false
  }
}
function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
<style scoped>
.order-page {
  max-width: 900px;
  margin: 0 auto;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}
.header h1 {
  color: #1976d2;
  font-size: 28px;
  margin-bottom: 4px;
}
.subtitle {
  color: #666;
  font-size: 14px;
}
.btn-logout-header {
  padding: 10px 20px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-logout-header:hover {
  background: #c0392b;
}
.user-info {
  background: #e3f2fd;
  padding: 12px 20px;
  border-radius: 6px;
  margin-bottom: 24px;
  color: #1976d2;
}
.order-form-container {
  background: white;
  padding: 32px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.order-form {
  max-width: 100%;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}
.form-group {
  display: flex;
  flex-direction: column;
}
.form-group.full-width {
  grid-column: 1 / -1;
}
label {
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  font-size: 14px;
}
input,
select,
textarea {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
  font-family: inherit;
}
input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #1976d2;
  box-shadow: 0 0 0 3px rgba(25, 118, 210, 0.1);
}
input:disabled,
select:disabled,
textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}
.help-text {
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}
.error-message {
  background: #ffebee;
  color: #c62828;
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-size: 14px;
}
.success-message {
  background: #e8f5e9;
  color: #2e7d32;
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-size: 14px;
}
.btn-submit {
  width: 100%;
  padding: 14px;
  background: #1976d2;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-submit:hover:not(:disabled) {
  background: #1565c0;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.3);
}
.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
@media (max-width: 768px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .order-form-container {
    padding: 20px;
  }
}
</style>