# Drone Delivery Planner

## Objetivo

Simular o planejamento de entregas feitas por drones em uma cidade representada por coordenadas 2D.

O sistema recebe drones e pedidos no domínio, monta viagens respeitando peso, alcance, prioridade, base fixa e retorno para a base, e separa pedidos que nao podem ser alocados.

## Unidades de medida

A aplicação usa o padrão métrico adotado no Brasil:

- Peso, capacidade e carga: quilogramas (`kg`).
- Coordenadas X/Y, distância, alcance e raio de obstáculos: quilômetros (`km`).
- Velocidade média do drone: quilômetros por hora (`km/h`).
- Bateria e reserva mínima: percentual (`%`).
- Consumo de bateria: percentual por quilômetro (`%/km`).
- Taxa de recarga: percentual por minuto (`%/min`).
- Duração e estimativas de entrega: minutos (`min`).

Com essas unidades, `estimatedDuration` é calculado por `(totalDistance / speed) * 60`, considerando `totalDistance` em km e `speed` em km/h.

## Tecnologias

- Java, compilado com `release 17`.
- Maven.
- JUnit 5.
- Spring Boot 3.5.16.
- Spring Web.
- Spring Data JPA.
- PostgreSQL.
- Flyway.
- Springdoc OpenAPI e Swagger UI.

Versoes verificadas neste ambiente:

- `java -version`: OpenJDK `24.0.1`.
- `mvn -version`: Apache Maven `3.9.12`, executando com Java `25.0.2`.

## Pre-requisitos

- JDK compativel com Java 17 ou superior.
- Maven instalado e disponivel no terminal.
- Docker e Docker Compose para subir o PostgreSQL local.
- Postman ou outro cliente HTTP para testar a API.

## Banco de dados local

O ambiente local usa PostgreSQL via Docker Compose:

```text
Banco: drone_delivery
Usuario: drone
Senha: drone
Porta: 5432
```

Para iniciar o banco:

```sh
docker compose up -d
```

Para parar o banco:

```sh
docker compose down
```

O volume `drone_delivery_postgres_data` preserva os dados entre reinicios do container.
As alteracoes de schema sao versionadas com Flyway em `src/main/resources/db/migration`.

Nesta etapa, drones, pedidos e viagens planejadas usam persistencia. As transicoes de inicio, conclusao e cancelamento de viagem ja foram implementadas.

## Como compilar

```sh
mvn compile
```

## Como executar

Suba o banco antes de iniciar a aplicacao:

```sh
docker compose up -d
```

```sh
mvn spring-boot:run
```

A aplicacao sobe por padrao em:

```text
http://localhost:8080
```

## Documentação Swagger/OpenAPI

Com o backend em execução, a documentação interativa fica disponível em:

```text
http://localhost:8080/swagger-ui.html
```

A especificação OpenAPI também pode ser consumida diretamente:

```text
http://localhost:8080/v3/api-docs
http://localhost:8080/v3/api-docs.yaml
```

O Swagger UI abre o grupo `backend-completo` por padrão e também possui grupos para visualizar separadamente a API pública ou a API interna. Endpoints da área do cliente usam o esquema `clientBearerAuth`; endpoints `/internal` usam o esquema `internalApiKey` com o header `X-Internal-Api-Key`.

## Como executar o dashboard

Em outro terminal, instale as dependencias do frontend uma vez:

```sh
cd frontend
npm install
```

Depois inicie o dashboard:

```sh
npm run dev
```

O dashboard sobe por padrao em:

```text
http://127.0.0.1:5173
```

Durante o desenvolvimento, o Vite encaminha chamadas `/api` e `/internal` para a API Spring em `http://localhost:8080`.
Endpoints internos sob `/internal` exigem o header `X-Internal-Api-Key`. O valor local padrao e `dev-internal-key`, configurado em `drone.internal.api-key`; no backend ele pode ser alterado com `DRONE_INTERNAL_API_KEY` e no dashboard com `VITE_INTERNAL_API_KEY`.

O botao `Recriar demo` chama `POST /internal/demo/reset-and-seed?confirmation=RESET_DEMO_DATA`.
Essa acao exige confirmacao no dashboard e limpa os dados operacionais atuais antes de recriar o cenario demo.

Depois de instalar as dependencias do frontend uma vez, tambem e possivel subir banco, backend e dashboard com um unico comando:

```sh
./scripts/start-local.sh
```

O script inicia o PostgreSQL com Docker Compose, executa o backend Spring e abre o servidor Vite. A porta do dashboard pode mudar se a porta padrao ja estiver ocupada; use a URL `Local` impressa pelo Vite.

O dashboard atual possui:

- navegação entre experiência Admin e experiência Cliente;
- visão geral com indicadores de drones, pedidos, viagens, recarga, tempo médio e avaliações;
- relatório mensal de produtividade com alternância do mês exibido;
- indicador de disponibilidade da API Spring, com bloqueio de ações operacionais quando o backend estiver offline;
- botão para recriar um cenário demo com confirmação, limpando dados operacionais e gerando drones, pedidos, obstáculo, avaliação e planejamento otimizado;
- alertas operacionais para reatribuição, não alocação, retorno antecipado e bateria baixa;
- jornada operacional guiada do ciclo completo, do cadastro ao encerramento da viagem;
- tabelas consultivas de drones, pedidos e viagens;
- abas, busca textual e filtro por status nas tabelas operacionais;
- status de drones, pedidos e viagens exibidos em português no dashboard;
- descrições por tooltip nos botões de ação da consulta operacional;
- visão detalhada de viagem com rota, progresso por entrega e histórico de telemetria;
- mapa 2D com base, pedidos, obstáculos, marcador do drone em movimento, modo de viagem selecionada ou todas as viagens, cores por viagem, setas de direção e pontos numerados pela ordem da rota;
- simulação automática de viagens planejadas ou em rota, respeitando janela ideal de saída, com consumo de bateria, solicitação de disponibilidade do cliente na aproximação, parada em pontos alcançados para confirmação do cliente, prazo de 1 minuto para informar o código, conclusão da rota ou retorno antecipado;
- visão dedicada de filas de entrega, reatribuição e recarga;
- cadastro operacional de drones e pedidos;
- acionamento de planejamento persistido com opção de rota otimizada;
- gestão de obstáculos circulares com cadastro, listagem e desativação;
- cadastro e consulta de avaliações com estrelas, título e feedback;
- ações operacionais de drones: marcar indisponível, marcar disponível, enviar para recarga, concluir recarga e excluir drone;
- painel de tratamento de pedidos não alocados para cancelar com justificativa para o cliente ou reenviar para planejamento;
- ações operacionais de viagens: iniciar, aguardar confirmação de entrega pelo cliente, enviar telemetria de bateria, concluir e cancelar.
- tela Cliente com cadastro, login, pedidos vinculados à conta, solicitação limitada de entrega por peso, coordenadas e horário confirmado, aba `Meus pedidos` para alternar pedidos da conta, acompanhamento por ID ou código, confirmação de disponibilidade na aproximação, confirmação de recebimento com o próprio código de rastreio, aviso central com som quando o drone se aproxima, mensagens de pacote não alocado/não entregue/cancelado, mapa da rota vinculada e avaliações públicas.

## Contrato da API

O contrato HTTP consolidado está em [`API.md`](API.md).
O README mantém exemplos de uso manual; o `API.md` concentra endpoints, payloads, respostas e erros.

## Estrutura do código

O código principal fica organizado em pacotes por responsabilidade:

- `com.example.drone`: classe de bootstrap da aplicação Spring.
- `com.example.drone.controller`: controllers REST e tratamento global de erros.
- `com.example.drone.domain`: modelos de domínio, status, cálculos de rota e planejamento.
- `com.example.drone.service`: serviços de aplicação e transições operacionais.
- `com.example.drone.persistence`: entidades JPA, repositories e adapters de persistência.
- `com.example.drone.exception`: excecoes usadas pelas regras e pela API.

Para executar apenas os cenarios do planejador de viagens:

```sh
mvn -Dtest=TripPlannerTest test
```

## Como rodar os testes

Os testes incluem uma jornada de integracao com Spring Boot, JPA, Flyway e PostgreSQL.
Suba o banco local antes de executar a suite completa:

```sh
docker compose up -d
```

```sh
mvn test
```

Resultado verificado:

- `BUILD SUCCESS`
- `Tests run: 218, Failures: 0, Errors: 0, Skipped: 0`

## Exemplo de entrada e saida

### Cadastro de drone

Endpoint:

```text
POST http://localhost:8080/api/drones
```

Entrada JSON:

```json
{
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 60.0,
  "chargingRate": 10.0
}
```

Os campos operacionais de bateria, velocidade e recarga são opcionais no cadastro. Quando omitidos, a aplicação usa os valores padrão mostrados no exemplo.

Saída JSON:

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 60.0,
  "chargingRate": 10.0
}
```

Cadastro valido retorna HTTP `201`.
Identificador duplicado retorna HTTP `409`.

### Consulta de drones

Endpoint:

```text
GET http://localhost:8080/api/drones
```

Esse endpoint lista todos os drones cadastrados.

Saida JSON:

```json
[
  {
    "id": 1,
    "identifier": "DRONE-1",
    "maxWeightCapacity": 10.0,
    "maxRange": 20.0,
    "status": "AVAILABLE",
    "batteryLevel": 100.0,
    "batteryConsumptionPerDistanceUnit": 1.0,
    "minimumReturnBattery": 20.0,
    "speed": 60.0,
    "chargingRate": 10.0
  }
]
```

Endpoint:

```text
GET http://localhost:8080/api/drones?status=UNAVAILABLE
```

Esse endpoint retorna somente drones com o status informado.

Status aceitos:

- `AVAILABLE`;
- `IN_ROUTE`;
- `UNAVAILABLE`;
- `CHARGING`.

Filtro de status inválido retorna HTTP `400`:

```json
{
  "message": "status must be one of AVAILABLE, IN_ROUTE, UNAVAILABLE, CHARGING"
}
```

Endpoint:

```text
GET http://localhost:8080/api/drones/available
```

Esse endpoint retorna somente drones com status `AVAILABLE`.

Endpoint:

```text
GET http://localhost:8080/api/drones/1
```

Esse endpoint retorna um drone pelo `id`.
Drone inexistente retorna HTTP `404` com `drone not found`.

### Disponibilidade de drone

Endpoint:

```text
POST http://localhost:8080/api/drones/1/unavailable
```

Esse endpoint marca um drone `AVAILABLE` como `UNAVAILABLE`.

Saída JSON:

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "UNAVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 60.0,
  "chargingRate": 10.0
}
```

Endpoint:

```text
POST http://localhost:8080/api/drones/1/available
```

Esse endpoint marca um drone `UNAVAILABLE` como `AVAILABLE`.

Saída JSON:

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 60.0,
  "chargingRate": 10.0
}
```

Regras de erro:

- drone inexistente retorna HTTP `404` com `drone not found`;
- marcar como indisponível exige status atual `AVAILABLE`;
- marcar como disponível exige status atual `UNAVAILABLE`;
- drone `IN_ROUTE` não pode ser alterado manualmente;
- transição inválida retorna HTTP `400`.

### Exclusão de drone

Endpoint:

```text
DELETE http://localhost:8080/api/drones/1
```

Esse endpoint exclui um drone que não esteja em rota e ainda não possua viagens vinculadas.

Saída JSON:

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 60.0,
  "chargingRate": 10.0
}
```

Regras de erro:

- drone inexistente retorna HTTP `404` com `drone not found`;
- drone em rota retorna HTTP `400` com `drone must not be IN_ROUTE to delete`;
- drone com viagens vinculadas retorna HTTP `400` com `drone with trips cannot be deleted`.

### Consulta interna de bateria

Endpoint:

```text
GET http://localhost:8080/internal/drones/1/battery
```

Saída JSON:

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "status": "AVAILABLE",
  "batteryLevel": 100.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "chargingRate": 10.0
}
```

Drone inexistente retorna HTTP `404` com `drone not found`.

### Fila de recarga

Endpoint:

```text
POST http://localhost:8080/api/drones/1/recharge
```

Esse endpoint coloca um drone `AVAILABLE` com bateria abaixo de `100.0` na fila de recarga.

Saída JSON:

```json
{
  "id": 1,
  "identifier": "DRONE-1",
  "maxWeightCapacity": 10.0,
  "maxRange": 20.0,
  "status": "CHARGING",
  "batteryLevel": 75.0,
  "batteryConsumptionPerDistanceUnit": 1.0,
  "minimumReturnBattery": 20.0,
  "speed": 60.0,
  "chargingRate": 10.0,
  "rechargeQueuedAt": "2026-07-25T20:00:00Z",
  "rechargeReason": "manual recharge requested"
}
```

Endpoint:

```text
GET http://localhost:8080/api/recharge-queue
```

Esse endpoint lista drones em recarga na ordem de entrada na fila.

Saida JSON:

```json
[
  {
    "droneId": 1,
    "droneIdentifier": "DRONE-1",
    "status": "CHARGING",
    "batteryLevel": 75.0,
    "queuedAt": "2026-07-25T20:00:00Z",
    "reason": "manual recharge requested"
  }
]
```

Endpoint:

```text
POST http://localhost:8080/api/drones/1/recharge/complete
```

Esse endpoint conclui a recarga de um drone `CHARGING`, define `batteryLevel` como `100.0` e retorna o drone para `AVAILABLE`.

Erros esperados:

- drone inexistente retorna HTTP `404` com `drone not found`;
- entrar na fila de recarga exige status atual `AVAILABLE`;
- entrar na fila de recarga exige bateria abaixo de `100.0`;
- concluir recarga exige status atual `CHARGING`.

### Cadastro de pedido

Endpoint:

```text
POST http://localhost:8080/api/orders
```

Entrada JSON:

```json
{
  "identifier": "ORDER-1",
  "location": {
    "x": 3.0,
    "y": 4.0
  },
  "weight": 4.0,
  "priority": "HIGH",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
}
```

Saida JSON:

```json
{
  "id": 1,
  "identifier": "ORDER-1",
  "location": {
    "x": 3.0,
    "y": 4.0
  },
  "weight": 4.0,
  "priority": "HIGH",
  "status": "REQUESTED",
  "queuedAt": "2026-07-25T20:00:00Z",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z",
  "deliveryConfirmationCode": "ORDER-1"
}
```

Cadastro válido retorna HTTP `201`.
Pedidos cadastrados pela API iniciam com status `REQUESTED` e precisam informar `confirmedDeliveryTime`.
O cadastro retorna `deliveryConfirmationCode` com o mesmo valor do rastreio para o cliente acompanhar e confirmar o recebimento depois; consultas de pedido não exibem esse campo.
Identificador duplicado retorna HTTP `409`.

### Consulta de pedidos

Endpoint:

```text
GET http://localhost:8080/api/orders
```

Saida JSON:

```json
[
  {
    "id": 1,
    "identifier": "ORDER-1",
    "location": {
      "x": 3.0,
      "y": 4.0
    },
    "weight": 4.0,
    "priority": "HIGH",
    "status": "REQUESTED",
    "queuedAt": "2026-07-25T20:00:00Z",
    "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
  }
]
```

Endpoint:

```text
GET http://localhost:8080/api/orders?status=REQUESTED
```

Esse endpoint retorna somente pedidos com o status informado.

Endpoint:

```text
GET http://localhost:8080/api/orders/1
```

Esse endpoint retorna um pedido pelo `id`.
Pedido inexistente retorna HTTP `404` com `order not found`.

Filtro de status inválido retorna HTTP `400`:

```json
{
  "message": "status must be one of REQUESTED, ALLOCATED, IN_ROUTE, PENDING_REASSIGNMENT, DELIVERED, NOT_DELIVERED, CANCELLED, UNALLOCATED"
}
```

### Cancelamento e realocação de pedido não alocado

Endpoint:

```text
POST http://localhost:8080/api/orders/1/cancel
```

Entrada JSON:

```json
{
  "reason": "Endereço fora da área atendida pela frota disponível."
}
```

Esse endpoint cancela apenas pedidos `UNALLOCATED` e grava a justificativa em `statusReason`, exibida para admin e cliente.

Endpoint:

```text
POST http://localhost:8080/api/orders/1/requeue
```

Esse endpoint retorna apenas pedidos `UNALLOCATED` para `REQUESTED`, permitindo nova tentativa de planejamento.

Regras de erro:

- pedido inexistente retorna HTTP `404` com `order not found`;
- cancelamento exige justificativa não vazia;
- pedidos que não estejam `UNALLOCATED` retornam HTTP `400`.

### Fila operacional de pedidos

Endpoint:

```text
GET http://localhost:8080/api/delivery-queue
```

Esse endpoint lista pedidos `REQUESTED` e `PENDING_REASSIGNMENT` na ordem da fila operacional.
A fila é ordenada por `confirmedDeliveryTime`, prioridade, `queuedAt` e, em caso de empate, por `id`.

Saida JSON:

```json
[
  {
    "orderId": 1,
    "orderIdentifier": "ORDER-1",
    "location": {
      "x": 3.0,
      "y": 4.0
    },
    "weight": 4.0,
    "priority": "HIGH",
    "status": "REQUESTED",
    "queuedAt": "2026-07-25T20:00:00Z"
  }
]
```

### Avaliações do serviço

Endpoint:

```text
POST http://localhost:8080/api/reviews
```

Entrada JSON:

```json
{
  "stars": 5,
  "title": "Entrega excelente",
  "feedback": "O pedido chegou antes do previsto."
}
```

Saída JSON:

```json
{
  "id": 1,
  "stars": 5,
  "title": "Entrega excelente",
  "feedback": "O pedido chegou antes do previsto.",
  "reviewedAt": "2026-07-25T20:00:00Z"
}
```

Endpoint:

```text
GET http://localhost:8080/api/reviews
```

Esse endpoint lista avaliações em ordem crescente de `id`.

Endpoint:

```text
GET http://localhost:8080/api/reviews/1
```

Esse endpoint retorna uma avaliação pelo `id`.
Avaliação inexistente retorna HTTP `404` com `review not found`.

Avaliações aceitam `stars` de 1 a 5, `title` obrigatório e `feedback` obrigatório.

### Obstáculos

Endpoint:

```text
POST http://localhost:8080/api/obstacles
```

Entrada JSON:

```json
{
  "center": {
    "x": 5.0,
    "y": 0.0
  },
  "radius": 1.0
}
```

Saida JSON:

```json
{
  "id": 1,
  "center": {
    "x": 5.0,
    "y": 0.0
  },
  "radius": 1.0,
  "active": true
}
```

Endpoint:

```text
GET http://localhost:8080/api/obstacles
```

Esse endpoint lista todos os obstaculos em ordem crescente de `id`.

Endpoint:

```text
DELETE http://localhost:8080/api/obstacles/1
```

Esse endpoint desativa o obstaculo e retorna `active: false`.
Obstaculo inexistente retorna HTTP `404` com `obstacle not found`.

### Planejamento operacional

Endpoint:

```text
POST http://localhost:8080/api/trip-plans
POST http://localhost:8080/api/trip-plans?optimizeRoute=false
```

Entrada:

```text
Sem corpo obrigatorio. O endpoint usa drones `AVAILABLE` e pedidos `REQUESTED` ou `PENDING_REASSIGNMENT` salvos no banco.
Por padrao, optimizeRoute=true define automaticamente a ordem por horario confirmado de entrega, prioridade, maior peso, menor distancia da base e identificador.
Com optimizeRoute=false, o planejamento respeita a ordem da fila operacional de pedidos.
Obstaculos ativos aumentam a distancia dos trechos que cruzariam a zona circular.
Drones so entram no plano se tiverem bateria suficiente para a rota completa e a reserva minima de retorno.
Drones disponiveis que teriam peso e alcance para pedidos solicitados, mas nao possuem bateria suficiente para nenhum deles, entram automaticamente na fila de recarga.
O horario ideal de saida e calculado pelo menor valor entre horario confirmado de entrega menos tempo estimado ate cada pacote da rota.
Viagens `PLANNED` antes desse horario ainda podem receber carga quando a rota recalculada respeita peso, alcance, bateria e obstaculos.
Viagens `PLANNED` com janela de saida aberta e viagens `IN_ROUTE` sao tratadas como reservadas para novas rodadas de planejamento.
Em uma mesma rodada de planejamento, cada drone disponivel recebe no maximo uma viagem planejada. Se uma encomenda nao couber na viagem do drone ja reservado, o planejamento tenta aloca-la imediatamente em outro drone disponivel e capaz; se nao houver outro drone imediato, o pedido fica como nao alocado com motivo especifico.
```

Saida JSON:

```json
{
  "trips": [
    {
      "id": 1,
      "droneId": 1,
	      "status": "PLANNED",
	      "orders": [1],
	      "route": [1],
	      "routeProgress": [
	        {
	          "orderId": 1,
	          "routePosition": 0,
	          "delivered": false,
	          "deliveredAt": null,
	          "estimatedDeliveryTime": 5.0
	        }
	      ],
	      "totalWeight": 4.0,
	      "totalDistance": 10.0,
	      "estimatedDuration": 10.0,
	      "averageDeliveryTime": 5.0
	    }
  ],
  "unallocatedOrders": []
}
```

`estimatedDuration` é calculado por `(totalDistance / speed) * 60` do drone associado.
`estimatedDeliveryTime` é o tempo acumulado até cada posição da rota, e `averageDeliveryTime` é a média desses tempos por pacote.
Quando há obstáculos ativos, `totalDistance`, `estimatedDuration`, alcance e bateria usam a distância ajustada pelo desvio.

Exemplo de pedido impossivel:

```json
{
  "trips": [],
  "unallocatedOrders": [
    {
      "orderId": 1,
      "orderIdentifier": "ORDER-1",
      "reason": "Pedido excede a capacidade máxima de peso dos drones disponíveis."
    }
  ]
}
```

Motivos possíveis para pedidos não alocados:

- `Pedido excede a capacidade máxima de peso dos drones disponíveis.`
- `Pedido excede o alcance máximo dos drones disponíveis.`
- `Pedido excede a capacidade máxima de peso e o alcance máximo dos drones disponíveis.`
- `Pedido exige mais bateria do que a frota disponível possui para concluir a rota e retornar em segurança.`
- `Pedido exige outro drone imediato, mas não há drone disponível nesta rodada de planejamento.`
- `Pedido não pode ser atendido por nenhum drone no planejamento atual.`

Pedidos alocados passam para `ALLOCATED`.
Pedidos impossíveis passam para `UNALLOCATED`.
Viagens criadas iniciam como `PLANNED`.
Parâmetro `optimizeRoute` inválido retorna HTTP `400` com `optimizeRoute is invalid`.

### Consulta de viagens

Endpoint:

```text
GET http://localhost:8080/api/trips
```

Saida JSON:

```json
[
  {
    "id": 1,
    "droneId": 1,
    "status": "PLANNED",
    "orders": [1],
    "route": [1],
    "routeProgress": [
      {
        "orderId": 1,
        "routePosition": 0,
        "delivered": false,
        "deliveredAt": null
      }
    ],
    "totalWeight": 4.0,
    "totalDistance": 10.0,
    "estimatedDuration": 10.0
  }
]
```

Esse endpoint retorna viagens salvas em ordem crescente de `id`.

Endpoint:

```text
GET http://localhost:8080/api/trips?status=PLANNED
```

Esse endpoint retorna somente viagens com o status informado.

Status aceitos:

- `PLANNED`;
- `IN_ROUTE`;
- `RETURNED_EARLY`;
- `COMPLETED`;
- `CANCELLED`.

Filtro de status invalido retorna HTTP `400`:

```json
{
  "message": "status must be one of PLANNED, IN_ROUTE, RETURNED_EARLY, COMPLETED, CANCELLED"
}
```

Endpoint:

```text
GET http://localhost:8080/api/trips/1
```

Esse endpoint retorna uma viagem pelo `id`.
Viagem inexistente retorna HTTP `404` com `trip not found`.

### Inicio de viagem

Endpoint:

```text
POST http://localhost:8080/api/trips/1/start
```

Saida JSON:

```json
{
  "id": 1,
  "droneId": 1,
  "status": "IN_ROUTE",
  "orders": [1],
  "route": [1],
  "routeProgress": [
    {
      "orderId": 1,
      "routePosition": 0,
      "delivered": false,
      "deliveredAt": null
    }
  ],
  "totalWeight": 4.0,
  "totalDistance": 10.0,
  "estimatedDuration": 10.0
}
```

Ao iniciar uma viagem:

- a viagem passa de `PLANNED` para `IN_ROUTE`;
- o drone associado deve estar `AVAILABLE` e passa para `IN_ROUTE`;
- o drone associado deve ter bateria suficiente para a rota completa e a reserva minima de retorno;
- os pedidos associados passam para `IN_ROUTE`.

Erros esperados:

- viagem inexistente retorna HTTP `404` com `trip not found`;
- viagem que nao esteja `PLANNED` retorna HTTP `400`;
- drone que nao esteja `AVAILABLE` retorna HTTP `400`;
- drone sem bateria suficiente retorna HTTP `400`.

### Entrega de posição da rota

Endpoint:

```text
POST http://localhost:8080/api/trips/1/route/0/availability
```

Entrada JSON:

```json
{
  "available": true
}
```

Esse endpoint registra se o cliente está disponível para receber o pacote quando o drone está chegando. Com `available: true`, a confirmação final por código fica liberada quando o drone parar no endereço. Com `available: false`, ou se o cliente não responder dentro do prazo da notificação, a viagem passa para `RETURNED_EARLY` e o pacote atual recebe a tag `NOT_DELIVERED` com motivo em português.

Endpoint:

```text
POST http://localhost:8080/api/trips/1/route/0/deliver
```

Entrada JSON:

```json
{
  "confirmationCode": "ORDER-1"
}
```

Esse endpoint registra que uma posição da rota foi entregue durante uma viagem `IN_ROUTE`, usando o código de rastreio digitado pelo cliente na aba Cliente.
A entrega deve seguir a ordem da rota: a posição `1` só pode ser marcada depois da posição `0`.
O cliente precisa ter confirmado disponibilidade para receber o pacote.
O drone precisa ter alcançado a posição da rota antes da confirmação.
Depois que o drone chega ao endereço e a disponibilidade está confirmada, o cliente tem 1 minuto para informar o código. Se o prazo expirar, o pacote é marcado como `NOT_DELIVERED`, a posição fica resolvida como falha e o drone segue a rota levando a encomenda de volta para a base.
Ao registrar a entrega, o item da rota recebe `deliveredAt` e o pedido associado passa para `DELIVERED`.

Saída JSON:

```json
{
  "id": 1,
  "droneId": 1,
  "status": "IN_ROUTE",
  "orders": [1, 2],
  "route": [1, 2],
  "routeProgress": [
    {
      "orderId": 1,
      "routePosition": 0,
      "delivered": true,
      "deliveredAt": "2026-07-25T20:00:00Z"
    },
    {
      "orderId": 2,
      "routePosition": 1,
      "delivered": false,
      "deliveredAt": null
    }
  ],
  "totalWeight": 8.0,
  "totalDistance": 20.0,
  "estimatedDuration": 20.0
}
```

Erros esperados:

- viagem inexistente retorna HTTP `404` com `trip not found`;
- posição inexistente retorna HTTP `404` com `trip route position not found`;
- viagem que não esteja `IN_ROUTE` retorna HTTP `400`;
- corpo ausente retorna HTTP `400`;
- código de confirmação ausente ou inválido retorna HTTP `400`;
- drone que ainda não alcançou a posição retorna HTTP `400`;
- posição negativa retorna HTTP `400`;
- posição fora de ordem retorna HTTP `400`;
- posição já entregue retorna HTTP `400`.

### Telemetria de viagem

Endpoint:

```text
POST http://localhost:8080/api/trips/1/telemetry
```

Entrada JSON:

```json
{
  "batteryLevel": 35.0
}
```

Esse endpoint registra a bateria atual reportada durante uma viagem `IN_ROUTE`.
Se a bateria informada ainda permite cumprir a rota salva com a reserva minima de retorno, a viagem continua `IN_ROUTE`.
Se a bateria informada nao permite cumprir a rota completa com seguranca, o retorno antecipado e acionado imediatamente.
No retorno antecipado, apenas posicoes ja reportadas como entregues permanecem `DELIVERED`; posicoes nao reportadas passam para `PENDING_REASSIGNMENT`.
Cada leitura valida fica persistida no historico da viagem com `reportedAt`.

Endpoint:

```text
GET http://localhost:8080/api/trips/1/telemetry
```

Esse endpoint lista o historico de telemetria da viagem em ordem de `reportedAt` e `id`.

Saida JSON:

```json
[
  {
    "id": 1,
    "tripId": 1,
    "batteryLevel": 90.0,
    "reportedAt": "2026-07-25T20:00:00Z"
  }
]
```

Erros esperados:

- viagem inexistente retorna HTTP `404` com `trip not found`;
- viagem que nao esteja `IN_ROUTE` retorna HTTP `400`;
- bateria ausente ou fora de `0` a `100` retorna HTTP `400`.

### Conclusao de viagem

Endpoint:

```text
POST http://localhost:8080/api/trips/1/complete
```

Saida JSON:

```json
{
  "id": 1,
  "droneId": 1,
  "status": "COMPLETED",
  "orders": [1],
  "route": [1],
  "routeProgress": [
    {
      "orderId": 1,
      "routePosition": 0,
      "delivered": true,
      "deliveredAt": "2026-07-25T20:00:00Z"
    }
  ],
  "totalWeight": 4.0,
  "totalDistance": 10.0,
  "estimatedDuration": 10.0
}
```

Ao concluir uma viagem:

- a viagem deve estar `IN_ROUTE`;
- se a bateria atual ainda cobre a rota salva e a reserva mínima de retorno, a viagem passa para `COMPLETED` somente depois que todas as posições da rota estiverem resolvidas;
- nesse fluxo completo, o drone associado volta para `AVAILABLE` e os pedidos já foram marcados como `DELIVERED` pelas confirmações de recebimento;
- se a bateria atual nao cobre a rota salva, o retorno antecipado usa o progresso persistido da rota;
- no retorno antecipado, a viagem passa para `RETURNED_EARLY`, posicoes ja reportadas como entregues permanecem `DELIVERED`, pedidos restantes passam para `PENDING_REASSIGNMENT` e o drone entra em `CHARGING`.

Erros esperados:

- viagem inexistente retorna HTTP `404` com `trip not found`;
- viagem que não esteja `IN_ROUTE` retorna HTTP `400`;
- viagem com posições de rota ainda sem confirmação retorna HTTP `400`.

### Cancelamento de viagem

Endpoint:

```text
POST http://localhost:8080/api/trips/1/cancel
```

Saida JSON:

```json
{
  "id": 1,
  "droneId": 1,
  "status": "CANCELLED",
  "orders": [1],
  "route": [1],
  "totalWeight": 4.0,
  "totalDistance": 10.0,
  "estimatedDuration": 10.0
}
```

Ao cancelar uma viagem:

- a viagem deve estar `PLANNED` ou `IN_ROUTE` e passa para `CANCELLED`;
- o drone associado volta para `AVAILABLE`;
- pedidos ainda nao entregues voltam para `REQUESTED`.

Erros esperados:

- viagem inexistente retorna HTTP `404` com `trip not found`;
- viagem `COMPLETED` ou ja `CANCELLED` retorna HTTP `400`.

## Regras de negocio adotadas

- A cidade e representada por coordenadas 2D.
- A distancia entre coordenadas usa distancia euclidiana.
- A base dos drones e fixa em `(0,0)`.
- Toda viagem sai da base e retorna para a base.
- O alcance maximo considera a distancia total da viagem, incluindo retorno.
- Cada drone possui identificador, capacidade maxima de peso, alcance maximo e dados basicos de bateria.
- Os dados basicos de bateria do drone sao nivel atual, consumo por distancia, bateria minima de retorno, velocidade e taxa de recarga.
- O identificador do drone deve ser unico.
- Drones cadastrados pela API iniciam com status `AVAILABLE`.
- Drones cadastrados sem dados de bateria usam valores padrao.
- A consulta interna de bateria fica em `GET /internal/drones/{id}/battery`.
- Endpoints internos sob `/internal` exigem o header `X-Internal-Api-Key`.
- Drones em fila de recarga usam status `CHARGING`.
- A fila de recarga fica em `GET /api/recharge-queue`.
- Drones podem entrar manualmente na fila de recarga com `POST /api/drones/{id}/recharge`.
- Drones em recarga podem voltar para `AVAILABLE` com `POST /api/drones/{id}/recharge/complete`.
- As consultas de drones retornam resultados em ordem crescente de `id`.
- As consultas de drones podem ser filtradas por status.
- A consulta de drone por `id` retorna `404` quando o drone nao existe.
- Drones `AVAILABLE` podem ser marcados manualmente como `UNAVAILABLE`.
- Drones `UNAVAILABLE` podem ser marcados manualmente como `AVAILABLE`.
- Drones `IN_ROUTE` nao podem ter disponibilidade alterada manualmente.
- Cada pedido possui identificador, localizacao, peso e prioridade.
- O identificador do pedido deve ser unico.
- Pedidos cadastrados pela API iniciam com status `REQUESTED`.
- As consultas de pedidos retornam resultados em ordem crescente de `id`.
- A consulta de pedido por `id` retorna `404` quando o pedido não existe.
- Pedidos `REQUESTED` e `PENDING_REASSIGNMENT` compõem a fila operacional de entrega.
- A fila operacional de pedidos fica em `GET /api/delivery-queue`.
- A fila operacional é ordenada por `confirmedDeliveryTime`, prioridade, `queuedAt` e `id`.
- Avaliações do serviço ficam em `POST /api/reviews`, `GET /api/reviews` e `GET /api/reviews/{id}`.
- Avaliações aceitam estrelas de 1 a 5, título e feedback do cliente.
- Obstáculos circulares ativos ficam em `GET /api/obstacles`.
- Obstáculos podem ser cadastrados com `POST /api/obstacles`.
- Obstáculos podem ser desativados com `DELETE /api/obstacles/{id}`.
- Filtros de status invalidos retornam HTTP `400` com os valores aceitos.
- O planejamento operacional usa apenas drones `AVAILABLE` e pedidos `REQUESTED` ou `PENDING_REASSIGNMENT` salvos.
- O planejamento usa `optimizeRoute=true` por padrao.
- Com `optimizeRoute=true`, entregas sao ordenadas automaticamente por horario confirmado, prioridade, maior peso, menor distancia da base e identificador.
- Com `optimizeRoute=false`, o planejamento respeita a ordem da fila operacional.
- Cada drone disponivel recebe no maximo uma viagem planejada por rodada de planejamento.
- Quando uma encomenda nao cabe na viagem ja planejada para um drone, o planejamento tenta aloca-la imediatamente em outro drone disponivel e capaz.
- Obstaculos ativos aumentam a distancia dos trechos de rota que cruzariam a zona circular.
- O planejamento move para a fila de recarga drones que teriam peso e alcance para pedidos solicitados, mas nao possuem bateria suficiente para atende-los.
- Viagens criadas pelo planejamento iniciam com status `PLANNED`.
- As consultas de viagens retornam resultados em ordem crescente de `id`.
- As consultas de viagens podem ser filtradas por status.
- A consulta de viagem por `id` retorna `404` quando a viagem não existe.
- A duração estimada da viagem é calculada por `(totalDistance / speed) * 60` do drone associado.
- O tempo médio até entrega fica em `averageDeliveryTime`, calculado pela média dos tempos acumulados por pacote.
- O início de uma viagem exige status `PLANNED` e drone `AVAILABLE`.
- O início de uma viagem `PLANNED` antes da janela ideal de saída retorna HTTP `400`.
- Ao iniciar uma viagem, viagem, drone e pedidos passam para `IN_ROUTE`.
- A telemetria de viagem fica em `POST /api/trips/{id}/telemetry`.
- A telemetria de viagem atualiza a bateria atual do drone associado.
- A telemetria de viagem é persistida em histórico consultável por `GET /api/trips/{id}/telemetry`.
- Entregas durante uma viagem exigem confirmação de disponibilidade com `POST /api/trips/{id}/route/{routePosition}/availability` antes da confirmação por código em `POST /api/trips/{id}/route/{routePosition}/deliver`.
- A simulação de viagem fica em `GET /api/trips/{id}/simulation` e `POST /api/trips/{id}/simulation/tick`.
- A simulação automática mantém viagens planejadas paradas antes da janela ideal de saída; quando a janela abre, inicia a viagem, move o drone, consome bateria, solicita disponibilidade na aproximação, para em entregas alcançadas aguardando confirmação do cliente e conclui a viagem quando a rota termina.
- Se o cliente não responder à solicitação de disponibilidade, o drone retorna à base, a viagem passa para `RETURNED_EARLY` e o pacote atual passa para `NOT_DELIVERED`.
- Se o cliente confirmar disponibilidade, mas não informar o código de recebimento em 1 minuto após a chegada do drone, o pacote atual passa para `NOT_DELIVERED` e a viagem segue para os próximos pontos da rota.
- Se uma telemetria deixar a rota completa insegura, o retorno antecipado é acionado imediatamente.
- Se a simulação deixar a rota restante insegura, o retorno antecipado é acionado, pedidos restantes passam para `PENDING_REASSIGNMENT` e o drone entra em `CHARGING`.
- A conclusão de uma viagem exige status `IN_ROUTE`.
- Ao concluir uma viagem com bateria suficiente, a viagem passa para `COMPLETED` somente depois das confirmações de entrega, e o drone volta para `AVAILABLE`.
- Ao concluir uma viagem sem bateria suficiente para a rota completa, a viagem passa para `RETURNED_EARLY`, o drone entra em `CHARGING` e pedidos sem entrega reportada passam para `PENDING_REASSIGNMENT`.
- O cancelamento de uma viagem exige status `PLANNED` ou `IN_ROUTE`.
- Ao cancelar uma viagem, a viagem passa para `CANCELLED`, o drone volta para `AVAILABLE` e pedidos não entregues voltam para `REQUESTED`.
- Pedidos alocados pelo planejamento passam para `ALLOCATED`.
- Pedidos impossíveis de alocar passam para `UNALLOCATED`.
- As prioridades existentes sao `HIGH`, `MEDIUM` e `LOW`.
- Uma viagem pode transportar varios pedidos.
- Uma viagem invalida nao pode ultrapassar a capacidade do drone.
- Uma viagem invalida nao pode ultrapassar o alcance do drone.
- Uma viagem invalida nao pode ultrapassar o alcance do drone apos ajuste por obstaculos ativos.
- Pedidos impossiveis de atender por qualquer drone sao retornados como nao alocados.
- Motivos de nao alocacao diferenciam restricoes de peso, alcance ou peso e alcance quando a causa for identificavel.

## Algoritmo de alocacao

O planejador usa uma estrategia deterministica simples, baseada em first-fit.

Fluxo:

1. Ordena drones por menor capacidade capaz, menor alcance capaz e identificador, preservando drones maiores para pacotes que realmente dependem deles.
2. Processa pedidos por horario confirmado de entrega mais proximo.
3. Em caso de empate no horario confirmado, ordena por prioridade, maior peso, menor distancia a partir da base e identificador.
4. Para cada pedido, verifica se algum drone consegue atende-lo sozinho.
5. Se nenhum drone conseguir, o pedido entra em `unallocatedOrders` com motivo detalhado quando a falha for peso, alcance, bateria ou uma combinacao conhecida.
6. Antes da janela ideal de saida, viagens `PLANNED` existentes entram como viagens abertas para receber mais carga.
7. Se for possivel atender, tenta inserir o pedido em uma viagem existente que ainda respeite peso, alcance, bateria e obstaculos.
8. Se nao couber em viagem existente, cria uma nova viagem com o primeiro drone capaz que ainda nao tenha viagem planejada na rodada.
9. Se o pedido exigir outro drone imediato e todos os drones capazes ja tiverem viagem planejada, o pedido entra em `unallocatedOrders` com motivo especifico.

O algoritmo busca reduzir o numero de viagens sem sacrificar o atendimento em tempo habil, mas nao prova otimalidade global.
Quando ha obstaculos ativos, as comparacoes de distancia usam a distancia ajustada por desvio circular.

## Decisoes e trade-offs

- O dominio foi implementado antes de qualquer interface externa.
- A infraestrutura de banco foi configurada antes de conectar as entidades persistidas.
- O cadastro de drones foi o primeiro fluxo conectado ao banco.
- O cadastro de pedidos foi conectado ao banco antes das consultas e antes do planejamento operacional.
- As consultas de pedidos usam ordenacao por `id` crescente para manter respostas deterministicas.
- Filtros publicos baseados em enum sao tratados como entrada invalida quando recebem valores fora da lista aceita.
- O planejamento operacional foi conectado ao banco depois que drones e pedidos ja podiam ser cadastrados e consultados.
- A resposta do planejamento operacional usa IDs persistidos de viagens, drones e pedidos.
- A consulta de viagens carrega drone e pedidos da rota junto com a viagem para funcionar com `spring.jpa.open-in-view=false`.
- A transicao de inicio rejeita viagem inexistente com HTTP `404`.
- A transicao de inicio rejeita viagem fora de `PLANNED` ou drone fora de `AVAILABLE` com HTTP `400`.
- A transicao de inicio rejeita viagem `PLANNED` antes da janela ideal de saida com HTTP `400`.
- A transicao de conclusao rejeita viagem inexistente com HTTP `404`.
- A transicao de conclusao rejeita viagem fora de `IN_ROUTE` com HTTP `400`.
- A transicao de cancelamento aceita viagens `PLANNED` ou `IN_ROUTE`.
- A transicao de cancelamento rejeita viagem inexistente com HTTP `404`.
- A transicao de cancelamento rejeita viagem `COMPLETED` ou ja `CANCELLED` com HTTP `400`.
- As consultas por `id` foram adicionadas para drones, pedidos e viagens sem mudar o formato das respostas de listagem.
- A unicidade do identificador de pedido foi adicionada na aplicacao e no banco por migration Flyway.
- Motivos de nao alocacao foram detalhados sem alterar o formato da resposta.
- A disponibilidade manual de drones usa endpoints explicitos de acao em vez de um `PATCH` generico de status.
- A alteracao manual de disponibilidade nao foi permitida para drones `IN_ROUTE`, porque esse status e controlado pelas transicoes de viagem.
- A interface atual para Postman e uma API REST minima com cadastro, consulta e planejamento operacional persistidos.
- O algoritmo de dominio continua isolado e tambem e exercitado diretamente por testes unitarios.
- As consultas de drones usam ordenacao por `id` crescente para manter respostas deterministicas.
- No planejamento padrao, o horario confirmado de entrega tem precedencia sobre prioridade para reduzir atraso operacional.
- No planejamento padrao, a rota dentro de cada viagem e otimizada antes da validacao de alcance e da persistencia.
- O planejamento valida bateria minima depois de calcular a distancia ajustada da rota.
- Com `optimizeRoute=true`, a rota usa ordenacao deterministica por horario confirmado, prioridade, maior peso, menor distancia da base e identificador.
- Cada drone disponivel e reservado para no maximo uma viagem planejada por rodada, evitando sequenciar excedentes no mesmo drone quando outro drone pode sair imediatamente.
- Viagens planejadas antes da janela ideal de saida podem receber novos pedidos quando a rota recalculada continua dentro dos limites do drone.
- Viagens planejadas com janela ideal de saida aberta e viagens em rota nao recebem novos pedidos no planejamento seguinte.
- A transicao de inicio rejeita viagem quando a bateria atual do drone nao cobre a rota salva e a reserva minima de retorno.
- O tratamento de entrada invalida usa excecoes de dominio, sem dependencia de HTTP.
- Pedido impossivel de alocar nao e erro de entrada; ele aparece no resultado como nao alocado.
- Flyway foi escolhido para versionar alteracoes de schema.
- O historico de telemetria de viagem fica na tabela `trip_telemetry`.
- O Hibernate esta configurado para validar o schema, nao para criar tabelas automaticamente.

## Limitacoes

- Nao existe CLI.
- O algoritmo e heuristico e pode nao produzir o menor numero global de viagens.
- A otimizacao exata de rota e limitada a viagens com ate 8 pedidos.
- Bateria basica, consulta interna, validacao de bateria minima, fila de recarga, tempo estimado, fila operacional de pedidos, obstaculos e retorno antecipado ja existem.
- A simulação automática já decrementa bateria durante a rota; a ação manual de conclusão permanece como transição operacional, mas exige que as posições da rota estejam resolvidas.
- Recarga automatica por tempo ja foi aprovada como roteiro, mas ainda nao esta implementada.

## Roteiro aprovado

Proximas etapas aprovadas para evolucao da aplicacao:

1. Bateria basica do drone e endpoint interno de consulta. Implementado.
2. Validacao de bateria minima para planejar e iniciar viagem completa com retorno seguro. Implementado.
3. Fila de recarga para drones sem bateria suficiente. Implementado.
4. Calculo de tempo estimado de entrega. Implementado.
5. Fila operacional de pedidos e opcao de planejamento com ou sem otimizacao de rota. Implementado.
6. Obstaculos circulares afetando rota, distancia, tempo e bateria. Implementado.
7. Retorno antecipado quando a bateria atingir o limite minimo de retorno seguro. Implementado.
8. Replanejamento de pedidos nao entregues com status `PENDING_REASSIGNMENT`. Implementado.
9. Movimento automático dos drones com parada para confirmação de entregas pelo cliente. Implementado.

## Possiveis evolucoes

- Criar uma CLI, se necessario.
- Criar novas migrations Flyway quando novas regras exigirem alteracoes de schema.
- Melhorar os motivos de pedidos nao alocados.
- Adicionar testes de aceitacao com multiplos drones e cenarios maiores.
- Avaliar estrategias de rota mais sofisticadas para viagens com muitos pedidos.
- Avaliar autenticacao e autorizacao para endpoints internos.
- Expandir o dashboard operacional com acoes de criacao, planejamento e transicao de viagens.
- Atualizar a documentacao de decisoes conforme novas escolhas forem feitas.
