import { useEffect, useState } from 'react';
import client from '../api/client';
import './VehicleModelPicker.css';

/**
 * Chọn dòng xe theo 3 bước: hãng → dòng xe → năm.
 * Chọn đủ 3 ô thì gọi onChange({ modelId, brand, model, year }); đổi lại ô phía trên thì gọi onChange(null).
 */
export default function VehicleModelPicker({ onChange }) {
    const [brands, setBrands] = useState([]);
    const [models, setModels] = useState([]);
    const [years, setYears] = useState([]);
    const [brand, setBrand] = useState('');
    const [model, setModel] = useState('');
    const [modelId, setModelId] = useState('');
    const [error, setError] = useState('');

    // 1. Tải danh sách hãng một lần khi component xuất hiện
    useEffect(() => {
        let ignore = false;
        client
            .get('/vehicle-models/brands')
            .then((res) => {
                if (!ignore) setBrands(res.data);
            })
            .catch(() => {
                if (!ignore) setError('Không tải được danh sách hãng xe.');
            });
        return () => {
            ignore = true;
        };
    }, []);

    // 2. Chọn hãng xong thì tải các dòng xe của hãng đó
    useEffect(() => {
        if (!brand) return undefined;
        let ignore = false;
        client
            .get('/vehicle-models/models', { params: { brand } })
            .then((res) => {
                if (!ignore) setModels(res.data);
            })
            .catch(() => {
                if (!ignore) setError('Không tải được danh sách dòng xe.');
            });
        return () => {
            ignore = true;
        };
    }, [brand]);

    // 3. Chọn dòng xe xong thì tải các năm
    useEffect(() => {
        if (!brand || !model) return undefined;
        let ignore = false;
        client
            .get('/vehicle-models/years', { params: { brand, model } })
            .then((res) => {
                if (!ignore) setYears(res.data);
            })
            .catch(() => {
                if (!ignore) setError('Không tải được danh sách năm.');
            });
        return () => {
            ignore = true;
        };
    }, [brand, model]);

    function handleBrandChange(e) {
        setBrand(e.target.value);
        setModel('');
        setModelId('');
        setModels([]);
        setYears([]);
        setError('');
        onChange?.(null);
    }

    function handleModelChange(e) {
        setModel(e.target.value);
        setModelId('');
        setYears([]);
        setError('');
        onChange?.(null);
    }

    function handleYearChange(e) {
        const id = e.target.value;
        setModelId(id);
        const found = years.find((y) => y.modelId === id);
        onChange?.(found ? { modelId: id, brand, model, year: found.year } : null);
    }

    return (
        <div className="vehicle-picker">
            <select value={brand} onChange={handleBrandChange} aria-label="Hãng xe">
                <option value="">Hãng xe</option>
                {brands.map((b) => (
                    <option key={b} value={b}>{b}</option>
                ))}
            </select>

            <select value={model} onChange={handleModelChange} disabled={!brand} aria-label="Dòng xe">
                <option value="">Dòng xe</option>
                {models.map((m) => (
                    <option key={m} value={m}>{m}</option>
                ))}
            </select>

            <select value={modelId} onChange={handleYearChange} disabled={!model} aria-label="Năm">
                <option value="">Năm</option>
                {years.map((y) => (
                    <option key={y.modelId} value={y.modelId}>{y.year}</option>
                ))}
            </select>

            {error && <p className="vehicle-picker__error">{error}</p>}
        </div>
    );
}