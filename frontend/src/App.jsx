import { Routes, Route } from 'react-router-dom'
import MainLayout from './layouts/MainLayout.jsx'
import RequireAuth from './components/RequireAuth.jsx'
import RequireAdmin from './components/RequireAdmin.jsx'
import AdminOrdersPage from './pages/AdminOrdersPage.jsx'
import AdminOrderDetailPage from './pages/AdminOrderDetailPage.jsx'
import AdminInstallationsPage from './pages/AdminInstallationsPage.jsx'
import AdminWarrantyPage from './pages/AdminWarrantyPage.jsx'
import AdminWarrantyDetailPage from './pages/AdminWarrantyDetailPage.jsx'
import WarrantyPage from './pages/WarrantyPage.jsx'
import HomePage from './pages/HomePage.jsx'
import ProductListPage from './pages/ProductListPage.jsx'
import ProductDetailPage from './pages/ProductDetailPage.jsx'
import CartPage from './pages/CartPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'
import ForgotPasswordPage from './pages/ForgotPasswordPage.jsx'
import ResetPasswordPage from './pages/ResetPasswordPage.jsx'
import AddressesPage from './pages/AddressesPage.jsx'
import CheckoutPage from './pages/CheckoutPage.jsx'
import OrdersPage from './pages/OrdersPage.jsx'
import OrderDetailPage from './pages/OrderDetailPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

export default function App() {
  return (
      <Routes>
        <Route element={<MainLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/products" element={<ProductListPage />} />
          <Route path="/products/:id" element={<ProductDetailPage />} />
          <Route path="/cart" element={<RequireAuth><CartPage /></RequireAuth>} />
          <Route path="/checkout" element={<RequireAuth><CheckoutPage /></RequireAuth>} />
          <Route path="/orders" element={<RequireAuth><OrdersPage /></RequireAuth>} />
          <Route path="/orders/:id" element={<RequireAuth><OrderDetailPage /></RequireAuth>} />
          <Route path="/addresses" element={<RequireAuth><AddressesPage /></RequireAuth>} />
          <Route path="/admin/orders" element={<RequireAdmin><AdminOrdersPage /></RequireAdmin>} />
          <Route path="/admin/orders/:id" element={<RequireAdmin><AdminOrderDetailPage /></RequireAdmin>} />
          <Route path="/admin/installations" element={<RequireAdmin><AdminInstallationsPage /></RequireAdmin>} />
          <Route path="/admin/warranty" element={<RequireAdmin><AdminWarrantyPage /></RequireAdmin>} />
          <Route path="/admin/warranty/:id" element={<RequireAdmin><AdminWarrantyDetailPage /></RequireAdmin>} />
          <Route path="/warranty" element={<RequireAuth><WarrantyPage /></RequireAuth>} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
  )
}
