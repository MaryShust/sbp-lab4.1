BEGIN;

-- Обрезаем длинные сообщения
UPDATE sbp_transactions
SET message = LEFT(message, 100)
WHERE LENGTH(message) > 100;

-- Меняем тип колонки
ALTER TABLE sbp_transactions
ALTER COLUMN message TYPE VARCHAR(100);

COMMIT;