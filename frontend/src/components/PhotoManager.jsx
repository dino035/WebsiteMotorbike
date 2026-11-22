import { useRef, useState } from 'react';
import client from '../api/client';
import { apiError } from '../utils/apiError';

/**
 * Quản lý ảnh của một sản phẩm.
 *
 * basePath  : '/admin/motorbikes/<id>' hoặc '/admin/spare-parts/<id>'
 * photos    : mảng ảnh hiện có, dạng [{ photoId, imageUrl, caption }]
 * onChange  : hàm cha truyền xuống để cập nhật lại mảng ảnh sau khi thêm/xóa
 *
 * API chỉ nhận MỘT file mỗi request, nên khi chọn nhiều ảnh thì gửi lần lượt từng cái.
 * Phải tuần tự chứ không song song: mỗi response trả về toàn bộ danh sách ảnh của sản phẩm,
 * bắn cùng lúc thì response về sau cùng có thể là ảnh chụp trạng thái cũ hơn, làm mất ảnh trên màn hình.
 */
export default function PhotoManager({ basePath, photos, onChange }) {
    const [files, setFiles] = useState([]);
    const [caption, setCaption] = useState('');
    const [uploading, setUploading] = useState(false);
    const [done, setDone] = useState(0);        // đã gửi xong bao nhiêu ảnh, để hiện tiến trình
    const [error, setError] = useState('');
    const fileInputRef = useRef(null);

    function handleFileChange(e) {
        // e.target.files là FileList, không phải mảng, nên không có .map/.filter
        setFiles(Array.from(e.target.files));
        setError('');
    }

    function resetForm() {
        setFiles([]);
        setCaption('');
        if (fileInputRef.current !== null) fileInputRef.current.value = '';
    }

    async function handleUpload(e) {
        e.preventDefault();
        if (files.length === 0) {
            setError('Vui lòng chọn ít nhất một file ảnh.');
            return;
        }

        setUploading(true);
        setDone(0);
        setError('');

        const failed = [];
        let newest = null;

        for (let i = 0; i < files.length; i += 1) {
            const data = new FormData();
            data.append('file', files[i]);
            if (caption.trim() !== '') data.append('caption', caption.trim());

            try {
                const res = await client.post(`${basePath}/photos`, data);
                newest = res.data.photos;
            } catch (err) {
                // Một ảnh hỏng không làm dừng cả lượt: ghi tên lại, gửi tiếp những ảnh còn lại
                failed.push(`${files[i].name} (${apiError(err, 'lỗi không rõ')})`);
            }
            setDone(i + 1);
        }

        // Chỉ cập nhật khi có ít nhất một ảnh lên được; nếu hỏng hết thì giữ nguyên danh sách cũ
        if (newest !== null) onChange(newest);

        if (failed.length === 0) {
            resetForm();
        } else {
            setError(`Không tải lên được ${failed.length}/${files.length} ảnh: ${failed.join('; ')}`);
        }

        setUploading(false);
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

    let buttonLabel = 'Tải ảnh lên';
    if (uploading) buttonLabel = `Đang tải ${done}/${files.length}…`;
    else if (files.length > 1) buttonLabel = `Tải ${files.length} ảnh lên`;

    return (
        <section className="ap-section">
            <h2>Ảnh sản phẩm</h2>

            {error !== '' && <p className="ap-error">{error}</p>}

            <form className="ap-upload" onSubmit={handleUpload}>
                <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept="image/jpeg,image/png,image/webp"
                    onChange={handleFileChange}
                />
                <input
                    type="text"
                    value={caption}
                    onChange={(e) => setCaption(e.target.value)}
                    placeholder={files.length > 1 ? 'Chú thích chung cho cả lượt (không bắt buộc)' : 'Chú thích (không bắt buộc)'}
                />
                <button type="submit" className="ap-btn ap-btn-primary" disabled={uploading}>
                    {buttonLabel}
                </button>
            </form>

            <p className="ap-hint">
                Chọn được nhiều ảnh cùng lúc (giữ Ctrl hoặc Shift khi chọn). Chỉ nhận JPG, PNG hoặc WEBP, mỗi file tối đa 5MB.
                {files.length > 1 && ` Đang chọn ${files.length} ảnh, chú thích sẽ áp cho tất cả.`}
            </p>

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
