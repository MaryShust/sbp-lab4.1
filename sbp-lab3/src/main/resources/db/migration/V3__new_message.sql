BEGIN;

-- Обрезаем длинные сообщения
UPDATE sbp_transactions
SET message = LEFT(message, 70)
WHERE LENGTH(message) > 70;

-- Меняем тип колонки
ALTER TABLE sbp_transactions
ALTER COLUMN message TYPE VARCHAR(70);

COMMIT;