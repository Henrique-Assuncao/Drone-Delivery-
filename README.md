# Drone Delivery Planner

## Objetivo

Simular o planejamento de entregas feitas por drones em uma cidade representada por coordenadas 2D.

O sistema recebe drones e pedidos no domínio, monta viagens respeitando peso, alcance, prioridade, base fixa e retorno para a base, e separa pedidos que nao podem ser alocados.

## Tecnologias

- Java, compilado com `release 17`.
- Maven.
- JUnit 5.
- Spring Boot 3.5.16.
- Spring Web.
- Spring Data JPA.
- PostgreSQL.
- Flyway.

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

O botao `Recriar demo` chama `POST /internal/demo/reset-and-seed?confirmation=RESET_DEMO_DATA`.
Essa acao exige confirmacao no dashboard e limpa os dados operacionais atuais antes de recriar o cenario demo.

Depois de instalar as dependencias do frontend uma vez, tambem e possivel subir banco, backend e dashboard com um unico comando:

```sh
./scripts/start-local.sh
```

O script inicia o PostgreSQL com Docker Compose, executa o backend Spring e abre o servidor Vite. A porta do dashboard pode mudar se a porta padrao ja estiver ocupada; use a URL `Local` impressa pelo Vite.

O dashboard atual possui:

- navegacao entre experiencia Admin e experiencia Cliente;
- visao geral com indicadores de drones, pedidos, viagens, recarga, tempo medio e avaliacoes;
- indicador de disponibilidade da API Spring, com bloqueio de acoes operacionais quando o backend estiver offline;
- botao para recriar um cenario demo com confirmacao, limpando dados operacionais e gerando drones, pedidos, obstaculo, avaliacao e planejamento otimizado;
- alertas operacionais para reatribuicao, nao alocacao, retorno antecipado e bateria baixa;
- jornada operacional guiada do ciclo completo, do cadastro ao encerramento da viagem;
- tabelas consultivas de drones, pedidos e viagens;
- abas, busca textual e filtro por status nas tabelas operacionais;
- status de drones, pedidos e viagens exibidos em portugues no dashboard;
- descricoes por tooltip nos botoes de acao da consulta operacional;
- visao detalhada de viagem com rota, progresso por entrega e historico de telemetria;
- mapa 2D com base, pedidos, obstaculos, marcador do drone em movimento, modo de viagem selecionada ou todas as viagens, cores por viagem, setas de direcao e pontos numerados pela ordem da rota;
- simulacao automatica de viagens planejadas ou em rota, com consumo de bateria, entrega automatica dos pontos alcancados, conclusao da rota ou retorno antecipado;
- visao dedicada de filas de entrega, reatribuicao e recarga;
- cadastro operacional de drones e pedidos;
- acionamento de planejamento persistido com opcao de rota otimizada;
- gestao de obstaculos circulares com cadastro, listagem e desativacao;
- cadastro e consulta de avaliacoes com estrelas, titulo e feedback;
- acoes operacionais de drones: marcar indisponivel, marcar disponivel, enviar para recarga e concluir recarga;
- acoes operacionais de viagens: iniciar, registrar proxima entrega da rota, enviar telemetria de bateria, concluir e cancelar.
- tela Cliente com solicitacao limitada de entrega por peso e coordenadas, acompanhamento por ID ou codigo, mapa da rota vinculada e avaliacoes publicas.

## Contrato da API

O contrato HTTP consolidado esta em [`API.md`](API.md).
O README mantem exemplos de uso manual; o `API.md` concentra endpoints, payloads, respostas e erros.

## Estrutura do codigo

O codigo principal fica organizado em pacotes por responsabilidade:

- `com.example.drone`: classe de bootstrap da aplicacao Spring.
- `com.example.drone.controller`: controllers REST e tratamento global de erros.
- `com.example.drone.domain`: modelos de dominio, status, calculos de rota e planejamento.
- `com.example.drone.service`: servicos de aplicacao e transicoes operacionais.
- `com.example.drone.persistence`: entidades JPA, repositories e adapters de persistencia.
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
- `Tests run: 190, Failures: 0, Errors: 0, Skipped: 0`

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
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Os campos de bateria sao opcionais no cadastro. Quando omitidos, a aplicacao usa os valores padrao mostrados no exemplo.

Saida JSON:

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
  "speed": 1.0,
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
    "speed": 1.0,
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

Filtro de status invalido retorna HTTP `400`:

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

Saida JSON:

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
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Endpoint:

```text
POST http://localhost:8080/api/drones/1/available
```

Esse endpoint marca um drone `UNAVAILABLE` como `AVAILABLE`.

Saida JSON:

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
  "speed": 1.0,
  "chargingRate": 10.0
}
```

Regras de erro:

- drone inexistente retorna HTTP `404` com `drone not found`;
- marcar como indisponivel exige status atual `AVAILABLE`;
- marcar como disponivel exige status atual `UNAVAILABLE`;
- drone `IN_ROUTE` nao pode ser alterado manualmente;
- transicao invalida retorna HTTP `400`.

### Consulta interna de bateria

Endpoint:

```text
GET http://localhost:8080/internal/drones/1/battery
```

Saida JSON:

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

Saida JSON:

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
  "speed": 1.0,
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
  "priority": "HIGH"
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
  "queuedAt": "2026-07-25T20:00:00Z"
}
```

Cadastro valido retorna HTTP `201`.
Pedidos cadastrados pela API iniciam com status `REQUESTED`.
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
    "queuedAt": "2026-07-25T20:00:00Z"
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

Filtro de status invalido retorna HTTP `400`:

```json
{
  "message": "status must be one of REQUESTED, ALLOCATED, IN_ROUTE, PENDING_REASSIGNMENT, DELIVERED, CANCELLED, UNALLOCATED"
}
```

### Fila operacional de pedidos

Endpoint:

```text
GET http://localhost:8080/api/delivery-queue
```

Esse endpoint lista pedidos `REQUESTED` e `PENDING_REASSIGNMENT` na ordem de entrada na fila operacional.
A fila e ordenada por `queuedAt` e, em caso de empate, por `id`.

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

### Avaliacoes do servico

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

Saida JSON:

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

Esse endpoint lista avaliacoes em ordem crescente de `id`.

Endpoint:

```text
GET http://localhost:8080/api/reviews/1
```

Esse endpoint retorna uma avaliacao pelo `id`.
Avaliacao inexistente retorna HTTP `404` com `review not found`.

Avaliacoes aceitam `stars` de 1 a 5, `title` obrigatorio e `feedback` obrigatorio.

### Obstaculos

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
Por padrao, optimizeRoute=true define automaticamente a ordem por prioridade, maior peso, menor distancia da base e identificador.
Com optimizeRoute=false, o planejamento respeita a ordem da fila operacional de pedidos.
Obstaculos ativos aumentam a distancia dos trechos que cruzariam a zona circular.
Drones so entram no plano se tiverem bateria suficiente para a rota completa e a reserva minima de retorno.
Drones disponiveis que teriam peso e alcance para pedidos solicitados, mas nao possuem bateria suficiente para nenhum deles, entram automaticamente na fila de recarga.
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

`estimatedDuration` e calculado por `totalDistance / speed` do drone associado.
`estimatedDeliveryTime` e o tempo acumulado ate cada posicao da rota, e `averageDeliveryTime` e a media desses tempos por pacote.
Quando ha obstaculos ativos, `totalDistance`, `estimatedDuration`, alcance e bateria usam a distancia ajustada pelo desvio.

Exemplo de pedido impossivel:

```json
{
  "trips": [],
  "unallocatedOrders": [
    {
      "orderId": 1,
      "orderIdentifier": "ORDER-1",
      "reason": "order exceeds max drone weight capacity"
    }
  ]
}
```

Motivos possíveis para pedidos não alocados:

- `order exceeds max drone weight capacity`;
- `order exceeds max drone range`;
- `order exceeds max drone weight capacity and max drone range`;
- `order exceeds drone battery for complete trip and safe return`;
- `order cannot be served by any drone`.

Pedidos alocados passam para `ALLOCATED`.
Pedidos impossiveis passam para `UNALLOCATED`.
Viagens criadas iniciam como `PLANNED`.
Parametro `optimizeRoute` invalido retorna HTTP `400` com `optimizeRoute is invalid`.

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

### Entrega de posicao da rota

Endpoint:

```text
POST http://localhost:8080/api/trips/1/route/0/deliver
```

Esse endpoint registra que uma posicao da rota foi entregue durante uma viagem `IN_ROUTE`.
A entrega deve seguir a ordem da rota: a posicao `1` so pode ser marcada depois da posicao `0`.
Ao registrar a entrega, o item da rota recebe `deliveredAt` e o pedido associado passa para `DELIVERED`.

Saida JSON:

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
- posicao inexistente retorna HTTP `404` com `trip route position not found`;
- viagem que nao esteja `IN_ROUTE` retorna HTTP `400`;
- posicao negativa retorna HTTP `400`;
- posicao fora de ordem retorna HTTP `400`;
- posicao ja entregue retorna HTTP `400`.

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
- se a bateria atual ainda cobre a rota salva e a reserva minima de retorno, a viagem passa para `COMPLETED`;
- nesse fluxo completo, o drone associado volta para `AVAILABLE` e os pedidos associados passam para `DELIVERED`;
- se a bateria atual nao cobre a rota salva, o retorno antecipado usa o progresso persistido da rota;
- no retorno antecipado, a viagem passa para `RETURNED_EARLY`, posicoes ja reportadas como entregues permanecem `DELIVERED`, pedidos restantes passam para `PENDING_REASSIGNMENT` e o drone entra em `CHARGING`.

Erros esperados:

- viagem inexistente retorna HTTP `404` com `trip not found`;
- viagem que nao esteja `IN_ROUTE` retorna HTTP `400`.

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
- A consulta de pedido por `id` retorna `404` quando o pedido nao existe.
- Pedidos `REQUESTED` e `PENDING_REASSIGNMENT` compoem a fila operacional de entrega.
- A fila operacional de pedidos fica em `GET /api/delivery-queue`.
- A fila operacional e ordenada por `queuedAt` e `id`.
- Avaliacoes do servico ficam em `POST /api/reviews`, `GET /api/reviews` e `GET /api/reviews/{id}`.
- Avaliacoes aceitam estrelas de 1 a 5, titulo e feedback do cliente.
- Obstaculos circulares ativos ficam em `GET /api/obstacles`.
- Obstaculos podem ser cadastrados com `POST /api/obstacles`.
- Obstaculos podem ser desativados com `DELETE /api/obstacles/{id}`.
- Filtros de status invalidos retornam HTTP `400` com os valores aceitos.
- O planejamento operacional usa apenas drones `AVAILABLE` e pedidos `REQUESTED` ou `PENDING_REASSIGNMENT` salvos.
- O planejamento usa `optimizeRoute=true` por padrao.
- Com `optimizeRoute=true`, entregas sao ordenadas automaticamente por prioridade, maior peso, menor distancia da base e identificador.
- Com `optimizeRoute=false`, o planejamento respeita a ordem da fila operacional.
- Obstaculos ativos aumentam a distancia dos trechos de rota que cruzariam a zona circular.
- O planejamento move para a fila de recarga drones que teriam peso e alcance para pedidos solicitados, mas nao possuem bateria suficiente para atende-los.
- Viagens criadas pelo planejamento iniciam com status `PLANNED`.
- As consultas de viagens retornam resultados em ordem crescente de `id`.
- As consultas de viagens podem ser filtradas por status.
- A consulta de viagem por `id` retorna `404` quando a viagem nao existe.
- A duracao estimada da viagem e calculada por `totalDistance / speed` do drone associado.
- O tempo medio ate entrega fica em `averageDeliveryTime`, calculado pela media dos tempos acumulados por pacote.
- O inicio de uma viagem exige status `PLANNED` e drone `AVAILABLE`.
- Ao iniciar uma viagem, viagem, drone e pedidos passam para `IN_ROUTE`.
- A telemetria de viagem fica em `POST /api/trips/{id}/telemetry`.
- A telemetria de viagem atualiza a bateria atual do drone associado.
- A telemetria de viagem e persistida em historico consultavel por `GET /api/trips/{id}/telemetry`.
- Entregas durante uma viagem sao reportadas por posicao da rota com `POST /api/trips/{id}/route/{routePosition}/deliver`.
- A simulacao de viagem fica em `GET /api/trips/{id}/simulation` e `POST /api/trips/{id}/simulation/tick`.
- A simulacao automatica inicia viagens planejadas, move o drone, consome bateria, registra entregas alcancadas e conclui a viagem quando a rota termina.
- Se uma telemetria deixar a rota completa insegura, o retorno antecipado e acionado imediatamente.
- Se a simulacao deixar a rota restante insegura, o retorno antecipado e acionado, pedidos restantes passam para `PENDING_REASSIGNMENT` e o drone entra em `CHARGING`.
- A conclusao de uma viagem exige status `IN_ROUTE`.
- Ao concluir uma viagem com bateria suficiente, a viagem passa para `COMPLETED`, o drone volta para `AVAILABLE` e os pedidos passam para `DELIVERED`.
- Ao concluir uma viagem sem bateria suficiente para a rota completa, a viagem passa para `RETURNED_EARLY`, o drone entra em `CHARGING` e pedidos sem entrega reportada passam para `PENDING_REASSIGNMENT`.
- O cancelamento de uma viagem exige status `PLANNED` ou `IN_ROUTE`.
- Ao cancelar uma viagem, a viagem passa para `CANCELLED`, o drone volta para `AVAILABLE` e pedidos nao entregues voltam para `REQUESTED`.
- Pedidos alocados pelo planejamento passam para `ALLOCATED`.
- Pedidos impossiveis de alocar passam para `UNALLOCATED`.
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

1. Ordena drones por maior capacidade, maior alcance e identificador.
2. Processa pedidos por prioridade: `HIGH`, depois `MEDIUM`, depois `LOW`.
3. Dentro de cada prioridade, ordena pedidos por maior peso, maior distancia de ida e volta a partir da base, e identificador.
4. Para cada pedido, verifica se algum drone consegue atende-lo sozinho.
5. Se nenhum drone conseguir, o pedido entra em `unallocatedOrders` com motivo detalhado quando a falha for peso, alcance, bateria ou uma combinacao conhecida.
6. Se for possivel atender, tenta inserir o pedido na primeira viagem existente da mesma prioridade.
7. Se nao couber em viagem existente, cria uma nova viagem com o primeiro drone capaz.

O algoritmo busca reduzir o numero de viagens dentro de cada grupo de prioridade, mas nao prova otimalidade global.
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
- No planejamento padrao, prioridades nao sao misturadas na mesma fase de planejamento.
- No planejamento padrao, a rota dentro de cada viagem e otimizada antes da validacao de alcance e da persistencia.
- O planejamento valida bateria minima depois de calcular a distancia ajustada da rota.
- Com `optimizeRoute=true`, a rota usa ordenacao deterministica por prioridade, maior peso, menor distancia da base e identificador.
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
- A simulacao automatica ja decrementa bateria durante a rota; a acao manual de conclusao permanece como transicao operacional simplificada.
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
9. Movimento automatico dos drones com entregas registradas pela simulacao. Implementado.

## Possiveis evolucoes

- Criar uma CLI, se necessario.
- Criar novas migrations Flyway quando novas regras exigirem alteracoes de schema.
- Melhorar os motivos de pedidos nao alocados.
- Adicionar testes de aceitacao com multiplos drones e cenarios maiores.
- Avaliar estrategias de rota mais sofisticadas para viagens com muitos pedidos.
- Avaliar autenticacao e autorizacao para endpoints internos.
- Expandir o dashboard operacional com acoes de criacao, planejamento e transicao de viagens.
- Gerar uma especificacao OpenAPI a partir do contrato documentado em `API.md`.
- Atualizar a documentacao de decisoes conforme novas escolhas forem feitas.
