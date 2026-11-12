import { useState } from 'react';
import VehicleModelPicker from './VehicleModelPicker';

/** Khung "Chọn xe của bạn" ở trang chủ. Sau này nối với API phụ tùng tương thích. */
export default function FindPartsByVehicle() {
    const [vehicle, setVehicle] = useState(null);

    return (
        <section style={{ marginTop: 32 }}>
            <h2>Chọn xe của bạn</h2>
            <VehicleModelPicker onChange={setVehicle} />
            <p>
                {vehicle
                    ? `Đã chọn: ${vehicle.brand} ${vehicle.model} ${vehicle.year}`
                    : 'Chọn đủ hãng, dòng xe và năm.'}
            </p>
        </section>
    );
}