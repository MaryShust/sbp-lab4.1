# Подробный отчёт о тестировании лабораторной работы

## 1. Запуск инфраструктуры

### 1.1 Запуск WildFly Node 1
```bash
# Проверка какие порты заняты
lsof -i :8085 -i :9990 -i :8443 | grep LISTEN
# Принудительное завершение процесса 1030 заменить
kill -9 1030
# Проверка что порты освободились
lsof -i :8085 -i :9990 -i :8443 | grep LISTEN


C://Users/user/Desktop/wildfly-37.0.1.Final/bin/standalone.sh \
  -Djboss.home.dir=C://Users/user/Desktop/wildfly-37.0.1.Final \
  -Djboss.server.base.dir=C://Users/user/Desktop/wildfly-37.0.1.Final/standalone \
  -Djboss.socket.binding.port-offset=0 \
  -Djboss.node.name=node1 > /tmp/wildfly-node1.log 2>&1 &
```

/Users/admin_1/Desktop/wildfly-39.0.1.Final
C://Users/user/Desktop/wildfly-37.0.1.Final


C://Users/user/Downloads/sbp/target

### 1.2 Запуск WildFly Node 2
```bash
# Копировать standalone в standalone-node2
cp -r C://Users/user/Desktop/wildfly-37.0.1.Final/standalone \
      C://Users/user/Desktop/wildfly-node2

# Задеплоить WAR
cp C://Users/user/Downloads/sbp/target/sbp.war \
   C://Users/user/Desktop/wildfly-node2/deployments/sbp.war

# Запустить
C://Users/user/Desktop/wildfly-37.0.1.Final/bin/standalone.sh \
  -Djboss.home.dir=C://Users/user/Desktop/wildfly-37.0.1.Final \
  -Djboss.server.base.dir=C://Users/user/Desktop/wildfly-node2 \
  -Djboss.socket.binding.port-offset=1 \
  -Djboss.node.name=node2 > /tmp/wildfly-node2.log 2>&1 &
```

### 1.3 Запуск Kafka + ZooKeeper
```bash
cd C://Users/user/Downloads/sbp
docker-compose up -d
```
Kafka UI доступен по адресу: http://localhost:8091

---

## 2. Проверка компонентов

### 2.1 Базовый API (Node 1)
**Тест:**
```bash
curl http://localhost:8080/sbp/api/v1/payments/health
```

**Ожидаемый результат:**
```json
{"service":"SBP Payment Service","status":"UP"}
```

**Результат:** РАБОТАЕТ

### 2.2 Базовый API (Node 2)
**Тест:**
```bash
curl http://localhost:8081/sbp/api/v1/payments/health
```

**Ожидаемый результат:**
```json
{"service":"SBP Payment Service","status":"UP"}
```

**Результат:** РАБОТАЕТ

---

### 2.3 Kafka - Топики
**Проверка:**
```bash
docker exec sbp-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**Ожидаемый результат:**
```
__consumer_offsets
sbp-fraud-check
sbp-notifications
sbp-reports
sbp-transactions
```

**Результат:** Все топики созданы

---

### 2.4 Kafka Consumer Groups - Распределение по узлам
**Проверка:**
```bash
docker exec sbp-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group sbp-prefraud-consumer-group --describe
```

**Ожидаемый результат:**
```
GROUP                       TOPIC           PARTITION  CONSUMER-ID
sbp-prefraud-consumer-group sbp-fraud-check 0          consumer-sbp-prefraud-consumer-group-1-...  (Node 1)
sbp-prefraud-consumer-group sbp-fraud-check 1          consumer-sbp-prefraud-consumer-group-1-...  (Node 1)
sbp-prefraud-consumer-group sbp-fraud-check 2          consumer-sbp-prefraud-consumer-group-2-...  (Node 2)
```

**Результат:** Распределение по узлам работает - Node 1 обрабатывает partitions 0,1; Node 2 - partition 2

---

## 3. Проверка бизнес-логики

### 3.1 Аутентификация
**Получить токен:**
```bash
curl -s -X POST http://localhost:8080/sbp/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'

curl -i -X POST http://localhost:8080/sbp/api/v1/payments/sbp \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer " \
  -d '{
    "senderBillId": 17,
    "receiverIdentifier": 18,
    "amount": 500002.00,
    "message": "123455"
  }'

```

**Результат:**
```json
{"token":"eyJhbGciOiJIUzM4NCJ9..."}
```

---

### 3.2 Пополнение счёта
**Тест:**
```bash
curl -s -X POST "http://localhost:8080/sbp/api/v1/bills/5/replenish?accountId=5" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d 10000
```

**Результат:**
```json
{"id":5,"accountId":5,"balance":10000.00,"isActive":true,...}
```

---

## 4. Fraud Analysis Scheduler

### 4.1 Проверка выполнения
**Настройка cron (для тестирования - каждые 2 минуты):**
```properties
sbp.scheduled.fraud-analysis.cron=0 0/2 * * * ?
```

**Проверка логов:**
```bash
grep -E "Fraud Analysis|Daily Fraud" /tmp/wildfly-node1.log
```

**Ожидаемые строки:**
```
2026-06-01T22:12:00.004 INFO  c.e.s.scheduler.FraudAnalysisScheduler : === Starting Daily Fraud Analysis ===
2026-06-01T22:12:00.023 INFO  c.e.s.scheduler.FraudAnalysisScheduler : === Fraud Analysis Completed: 0 duplicate groups found ===
```

**Результат:** Scheduler выполняется каждые 2 минуты

---

## 5. Сводная таблица проверок

| Компонент | Метод проверки | Статус |
|-----------|----------------|--------|
| WildFly Node 1 | http://localhost:8080/sbp | UP |
| WildFly Node 2 | http://localhost:8081/sbp | UP |
| Kafka топики | kafka-topics --list | 4 топика |
| Kafka распределение | consumer-groups describe | 2 узла |
| PreFraudCheckConsumer | Логи (partitions assigned) | Работает |
| FraudAnalysisScheduler | Логи (scheduling-1) | Выполняется |
| Таблица preSuspicion | Hibernate logs | Создана |
| Таблица suspicion | Hibernate logs | Создана |
| Exchange Rate API (JCA) | /api/exchange-rate/rate | Работает |

---

## 6. Архитектура реализации

### 6.1 Асинхронная обработка (Kafka)
```
PaymentService
    |
    └──> TransactionEventProducer
              |
              └──> sbp-fraud-check (топик)
                        |
                        └──> PreFraudCheckConsumer
                                  |
                                  └──> preSuspicion (таблица)
```

### 6.2 PreFraudCheckConsumer
- Слушает топик `sbp-fraud-check`
- На каждую транзакцию записывает событие в таблицу `preSuspicion`
- Определяет уровень риска (LOW/MEDIUM/HIGH/CRITICAL) на основе суммы
- Работает параллельно на 2 узлах

### 6.3 FraudAnalysisScheduler
- Выполняется раз в сутки (cron: `0 0 3 * * ?`)
- Для тестирования: каждые 2 минуты (`0 0/2 * * * ?`)
- Анализирует события за минувшие сутки из таблицы `preSuspicion`
- Находит дубли (разница во времени >= 1 час)
- Записывает результаты в таблицу `suspicion`

### 6.4 Таблицы базы данных

**preSuspicion:**
- transaction_id
- sender_bill_id
- sender_account_id
- sender_bank_bic
- receiver_bill_id
- receiver_account_id
- receiver_bank_bic
- amount
- event_time
- risk_level
- is_suspicious

**suspicion:**
- user_name
- account_id
- bank_bic
- duplicate_count
- analysis_date
- created_at

---

## 7. Распределённая 2-узловая архитектура

### 7.1 Запуск двух узлов
См. разделы 1.1 и 1.2

### 7.2 Проверка обоих узлов
```bash
# Node 1
curl http://localhost:8085/sbp/api/v1/payments/health

# Node 2
curl http://localhost:8086/sbp/api/v1/payments/health
```

### 7.3 Kafka распределение нагрузки
См. раздел 2.4 - каждый узел обрабатывает свои partitions

### 7.4 Отказоустойчивость
- Если один узел падает, Kafka перераспределяет partitions
- После перезапуска partition'ы перебалансируются автоматически

---

## 8. Файлы проекта

### Созданные файлы:

**Entity:**
- PreSuspicionEntity.java - таблица preSuspicion
- SuspicionEntity.java - таблица suspicion

**Repository:**
- PreSuspicionRepository.java
- SuspicionRepository.java

**Kafka:**
- PreFraudCheckConsumer.java - слушает fraud-check топик
- TransactionEventProducer.java - отправляет события после транзакции

**Scheduler:**
- FraudAnalysisScheduler.java - ежедневный анализ дубликатов

**Config:**
- KafkaConfig.java - конфигурация Kafka consumer/producer

**Removed:**
- ExchangeRateService.java, ExchangeRateController.java (EIS)
- NotificationConsumer.java, ReportGeneratorConsumer.java
- TransactionAuditScheduler.java

---

## 9. JCA Resource Adapter (Exchange Rate API)

### 9.1 Описание
Реализован JCA Resource Adapter для получения курсов валют с API open.er-api.com. Код JCA упакован внутри WAR файла (Inline Resource Adapter).

### 9.2 Структура файлов JCA

```
src/main/java/com/example/sbp/jca/
├── ExchangeRateConnection.java          # Интерфейс соединения
├── ExchangeRateConnectionFactory.java   # Интерфейс фабрики
└── exchangerate/
    ├── ExchangeRateService.java              # Spring сервис
    ├── ExchangeRateConnectionFactory.java    # Реализация фабрики
    ├── ExchangeRateConnectionImpl.java       # Реализация соединения
    ├── ExchangeRateManagedConnection.java    # ManagedConnection
    ├── ExchangeRateManagedConnectionFactory.java  # Фабрика ManagedConnection
    └── ExchangeRateManagedConnectionMetaData.java  # Метаданные

src/main/java/com/example/sbp/controller/
└── ExchangeRateController.java          # REST контроллер
```

### 9.3 REST API эндпоинты

**Получить курс валюты:**
```bash
curl "http://localhost:8080/sbp-0.0.1-SNAPSHOT/api/exchange-rate/rate?base=USD&target=EUR"
```

**Получить все курсы базовой валюты:**
```bash
curl "http://localhost:8080/sbp-0.0.1-SNAPSHOT/api/exchange-rate/rates?base=USD"
```

### 9.4 Деплой в WildFly

**1. Собрать проект:**
```bash
cd sbp_lab2
./mvnw clean package -DskipTests
```

**2. Запустить WildFly:**
```bash
cd wildfly-39.0.1.Final/bin
./standalone.sh
```

**3. Дождаться запуска (10-15 секунд)**

**4. Задеплоить WAR:**
```bash
./jboss-cli.sh --connect
deploy /path/to/sbp-0.0.1-SNAPSHOT.war --force
```

**5. Проверить работу:**
```bash
curl "http://localhost:8080/sbp/api/exchange-rate/rate?base=USD&target=EUR"
0.865865
```

**Ожидаемый ответ:**
```json
{
  "result": "success",
  "base_code": "USD",
  "rates": {
    "EUR": 0.867245,
    "RUB": 73.12195,
    ...
  }
}
```

### 9.5 Перенос на другой компьютер

На новом компьютере достаточно:
1. Скопировать проект
2. Собрать: `./mvnw clean package -DskipTests`
3. Запустить WildFly и задеплоить WAR

Никаких дополнительных настроек не требуется.

---

## 10. Как запустить полный тест

```bash
# 1. Запустить Kafka
cd /Users/admin_1/Downloads/sbp_lab2
docker-compose up -d

# 2. Запустить Node 1
pkill -f wildfly
sleep 2
/Users/admin_1/Desktop/wildfly-39.0.1.Final/bin/standalone.sh \
  -Djboss.home.dir=/Users/admin_1/Desktop/wildfly-39.0.1.Final \
  -Djboss.server.base.dir=/Users/admin_1/Desktop/wildfly-39.0.1.Final/standalone \
  -Djboss.socket.binding.port-offset=0 \
  -Djboss.node.name=node1 > /tmp/wildfly-node1.log 2>&1 &

# 3. Скопировать standalone для Node 2
cp -r /Users/admin_1/Desktop/wildfly-39.0.1.Final/standalone \
      /Users/admin_1/Desktop/wildfly-node2
rm -rf /Users/admin_1/Desktop/wildfly-node2/deployments/*
cp /Users/admin_1/Downloads/sbp_lab2/target/sbp-0.0.1-SNAPSHOT.war \
   /Users/admin_1/Desktop/wildfly-node2/deployments/sbp.war

# 4. Запустить Node 2
/Users/admin_1/Desktop/wildfly-39.0.1.Final/bin/standalone.sh \
  -Djboss.home.dir=/Users/admin_1/Desktop/wildfly-39.0.1.Final \
  -Djboss.server.base.dir=/Users/admin_1/Desktop/wildfly-node2 \
  -Djboss.socket.binding.port-offset=1 \
  -Djboss.node.name=node2 > /tmp/wildfly-node2.log 2>&1 &

# 5. Подождать запуска (45 секунд)
sleep 45

# 6. Проверить все компоненты
echo "=== Node 1 ===" && curl -s http://localhost:8080/sbp/api/v1/payments/health
echo "=== Node 2 ===" && curl -s http://localhost:8081/sbp/api/v1/payments/health
echo "=== Kafka Topics ===" && docker exec sbp-kafka kafka-topics --bootstrap-server localhost:9092 --list
echo "=== Consumer Groups ===" && docker exec sbp-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --group sbp-prefraud-consumer-group --describe
echo "=== Exchange Rate API ===" && curl -s "http://localhost:8080/sbp-0.0.1-SNAPSHOT/api/exchange-rate/rate?base=USD&target=EUR"
```
