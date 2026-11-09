import { useParams } from 'react-router-dom'

export default function ProductDetailPage() {
    const { id } = useParams()
    return <h1>Chi tiết sản phẩm {id}</h1>
}