<template>
  <div class="product-management-page">
    <div class="page-header">
      <h1>📦 Product Management</h1>
      <p class="subtitle">Update product information (name, price, stock)</p>
    </div>

    <div class="search-card">
      <div class="search-group">
        <input
            v-model="searchQuery"
            type="text"
            placeholder="Search by product name..."
            @keyup.enter="loadProducts"
        />
        <button class="btn-primary" @click="loadProducts" :disabled="loading">
          {{ loading ? 'Searching...' : 'Search' }}
        </button>
      </div>
    </div>

    <div v-if="error" class="error-message">{{ error }}</div>

    <div v-if="products" class="products-card">
      <div class="table-container">
        <table>
          <thead>
          <tr>
            <th>ID</th>
            <th>Name</th>
            <th>Price</th>
            <th>Stock</th>
            <th>Category ID</th>
            <th>Supplier ID</th>
            <th>Actions</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="product in products" :key="product.productId">
            <td>{{ product.productId }}</td>
            <td>{{ product.productName }}</td>
            <td>¥{{ product.price }}</td>
            <td>{{ product.stock }}</td>
            <td>{{ product.categoryId }}</td>
            <td>{{ product.supplierId }}</td>
            <td>
              <button class="btn-edit" @click="openEditModal(product)">Edit</button>
            </td>
          </tr>
          </tbody>
        </table>
        <div v-if="products.length === 0" class="no-data">No products found</div>
      </div>
    </div>

    <!-- Edit Modal -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Edit Product</h2>
          <button class="btn-close" @click="closeEditModal">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label for="writeDb">Target Database *</label>
            <select id="writeDb" v-model="editForm.writeDb">
              <option value="MYSQL">MySQL</option>
              <option value="POSTGRES">PostgreSQL</option>
            </select>
          </div>
          <div class="form-group">
            <label for="productName">Product Name *</label>
            <input
                id="productName"
                v-model="editForm.productName"
                type="text"
                required
            />
          </div>
          <div class="form-group">
            <label for="price">Price *</label>
            <input
                id="price"
                v-model.number="editForm.price"
                type="number"
                step="0.01"
                min="0"
                required
            />
          </div>
          <div class="form-group">
            <label for="stock">Stock *</label>
            <input
                id="stock"
                v-model.number="editForm.stock"
                type="number"
                min="0"
                required
            />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeEditModal">Cancel</button>
          <button class="btn-primary" @click="saveProduct" :disabled="saving">
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api'

const searchQuery = ref('')
const loading = ref(false)
const error = ref(null)
const products = ref(null)

const showEditModal = ref(false)
const saving = ref(false)
const editForm = ref({
  productId: null,
  writeDb: 'MYSQL',
  productName: '',
  price: 0,
  stock: 0
})

const loadProducts = async () => {
  loading.value = true
  error.value = null
  try {
    const params = searchQuery.value ? { search: searchQuery.value } : {}
    const response = await api.get('/api/admin/products', { params })
    if (response.data.code === 200) {
      products.value = response.data.data.products
    } else {
      error.value = response.data.message || 'Failed to load products'
    }
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to load products'
    console.error('Error loading products:', err)
  } finally {
    loading.value = false
  }
}

const openEditModal = (product) => {
  editForm.value = {
    productId: product.productId,
    writeDb: 'MYSQL',
    productName: product.productName,
    price: parseFloat(product.price),
    stock: product.stock
  }
  showEditModal.value = true
}

const closeEditModal = () => {
  showEditModal.value = false
  editForm.value = {
    productId: null,
    writeDb: 'MYSQL',
    productName: '',
    price: 0,
    stock: 0
  }
}

const saveProduct = async () => {
  if (!editForm.value.productName || editForm.value.price < 0 || editForm.value.stock < 0) {
    error.value = 'Please fill in all fields correctly'
    return
  }

  saving.value = true
  error.value = null
  try {
    const response = await api.put(`/api/admin/products/${editForm.value.productId}`, {
      writeDb: editForm.value.writeDb,
      productName: editForm.value.productName,
      price: editForm.value.price,
      stock: editForm.value.stock
    })
    if (response.data.code === 200) {
      closeEditModal()
      await loadProducts()
    } else {
      error.value = response.data.message || 'Failed to update product'
    }
  } catch (err) {
    error.value = err.response?.data?.message || 'Failed to update product'
    console.error('Error updating product:', err)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.product-management-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
}

.page-header h1 {
  font-size: 32px;
  color: #2c3e50;
  margin-bottom: 8px;
}

.subtitle {
  color: #7f8c8d;
  font-size: 16px;
}

.search-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.search-group {
  display: flex;
  gap: 12px;
}

.search-group input {
  flex: 1;
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.products-card {
  background: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.table-container {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #ecf0f1;
}

th {
  background: #f8f9fa;
  font-weight: 600;
  color: #2c3e50;
}

.btn-primary {
  padding: 10px 20px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover:not(:disabled) {
  background: #2980b9;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  padding: 10px 20px;
  background: #95a5a6;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-secondary:hover {
  background: #7f8c8d;
}

.btn-edit {
  padding: 6px 12px;
  background: #2ecc71;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-edit:hover {
  background: #27ae60;
}

.error-message {
  background: #ffe6e6;
  color: #c0392b;
  padding: 12px 16px;
  border-radius: 6px;
  margin-bottom: 16px;
  border: 1px solid #f5c6cb;
}

.no-data {
  text-align: center;
  padding: 40px;
  color: #95a5a6;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #ecf0f1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h2 {
  font-size: 20px;
  color: #2c3e50;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 28px;
  color: #95a5a6;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 28px;
  height: 28px;
}

.btn-close:hover {
  color: #2c3e50;
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #2c3e50;
  font-weight: 600;
  font-size: 14px;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #ecf0f1;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
