import { useRef, useState } from 'react';
import client from '../api/client';
import { apiError } from '../utils/apiError';

/**
 * Quản lý ảnh của một sản phẩm.
 *
 * basePath  : '/admin/motorbikes/<id>' hoặc '/admin/spare-parts/<id>'
 * photos    : mảng ảnh hiện có, dạng [{ photoId, imageUrl, caption }]
 * onChange  : hàm cha truyền xuống để cập nhật lại mảng ảnh sau khi thêm/xóa
 */
export default function PhotoManager({ basePath, photos, onChange }) {
    const [file, setFile] = useState(null);
    const [caption, setCaption] = useState('');
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState('');
    const fileInputRef = useRef(null);

    function handleFileChange(e) {
        const selected = e.target.files[0];
        setFile(selected === undefined ? null : selected);
        setError('');
    }

    function handleUpload(e) {
        e.preventDefault();
        if (file === null) {
            setError('Vui lòng chọn một file ảnh.');
            return;
        }

        const data = new FormData();
        data.append('file', file);
        if (caption.trim() !== '') data.append('caption', caption.trim());

        setUploading(true);
        setError('');

        client
            .post(`${basePath}/photos`, data)
            .then((res) => {
                onChange(res.data.photos);
                setFile(null);
                setCaption('');
                if (fileInputRef.current !== null) fileInputRef.current.value = '';
            })
            .catch((err) => setError(apiError(err, 'Tải ảnh lên không thành công.')))
            .finally(() => setUploading(false));
    }

    function handleDelete(photo) {
        const ok = window.confirm('Xóa ảnh này?');
        if (!ok) return;

        setError('');

        client
            .delete(`${basePath}/photos/${photo.photoId}`)
            .then((res) => {
                onChange(res.data.photos);
            })
            .catch((err) => setError(apiError(err, 'Không xóa được ảnh.')));
    }

    return (
        <section className="ap-section">
            <h2>Ảnh sản phẩm</h2>

            {error !== '' && <p className="ap-error">{error}</p>}

            <form className="ap-upload" onSubmit={handleUpload}>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    onChange={handleFileChange}
                />
                <input
                    type="text"
                    value={caption}
                    onChange={(e) => setCaption(e.target.value)}
                    placeholder="Chú thích (không bắt buộc)"
                />
                <button type="submit" className="ap-btn ap-btn-primary" disabled={uploading}>
                    {uploading ? 'Đang tải lên…' : 'Tải ảnh lên'}
                </button>
            </form>

            <p className="ap-hint">Chỉ nhận JPG, PNG hoặc WEBP, mỗi file tối đa 5MB.</p>

            {photos.length === 0 ? (
                <p className="ap-muted">Chưa có ảnh nào.</p>
            ) : (
                <ul className="ap-photo-list">
                    {photos.map((photo) => (
                        <li key={photo.photoId}>
                            <img src={photo.imageUrl} alt={photo.caption === null ? 'Ảnh sản phẩm' : photo.caption} />
                            <span>{photo.caption === null ? '' : photo.caption}</span>
                            <button
                                type="button"
                                className="ap-btn ap-btn-danger"
                                onClick={() => handleDelete(photo)}
                            >
                                Xóa
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </section>
    );
}
