INSERT INTO vehicle_model (model_id, brand, model, year_from, year_to)
SELECT gen_random_uuid()::text, d.brand, d.model, y, y
FROM (VALUES
          ('Honda',   'Wave Alpha',   2015, 2025),
          ('Honda',   'Wave RSX',     2015, 2025),
          ('Honda',   'Future',       2015, 2025),
          ('Honda',   'Blade',        2015, 2025),
          ('Honda',   'Vision',       2015, 2025),
          ('Honda',   'Lead',         2015, 2025),
          ('Honda',   'Air Blade',    2015, 2025),
          ('Honda',   'SH Mode',      2015, 2025),
          ('Honda',   'SH',           2015, 2025),
          ('Honda',   'Vario',        2018, 2025),
          ('Honda',   'Winner X',     2019, 2025),
          ('Yamaha',  'Sirius',       2015, 2025),
          ('Yamaha',  'Jupiter',      2015, 2025),
          ('Yamaha',  'Exciter',      2015, 2025),
          ('Yamaha',  'Grande',       2015, 2025),
          ('Yamaha',  'Janus',        2016, 2025),
          ('Yamaha',  'NVX',          2017, 2025),
          ('Yamaha',  'Latte',        2019, 2025),
          ('Suzuki',  'Raider',       2015, 2025),
          ('Suzuki',  'Satria',       2018, 2025),
          ('Piaggio', 'Liberty',      2015, 2025),
          ('Piaggio', 'Vespa Sprint', 2015, 2025),
          ('SYM',     'Attila',       2015, 2025),
          ('SYM',     'Elegant',      2015, 2025)
     ) AS d(brand, model, start_year, end_year)
         CROSS JOIN LATERAL generate_series(d.start_year, d.end_year) AS y
ON CONFLICT (brand, model, year_from, year_to) DO NOTHING;

-- Kiểm tra: mỗi hãng bao nhiêu dòng xe, bao nhiêu dòng dữ liệu
-- SELECT brand, COUNT(DISTINCT model) AS so_dong_xe, COUNT(*) AS so_dong_du_lieu
-- FROM vehicle_model GROUP BY brand ORDER BY brand;