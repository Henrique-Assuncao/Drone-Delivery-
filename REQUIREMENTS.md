# Requisitos do Desafio

## Objetivo principal

Simular entregas feitas por drones em uma cidade representada por coordenadas 2D, alocando pedidos aos drones de forma a buscar o menor número possível de viagens, respeitando as restrições de peso, distância e prioridade.

A evolução operacional aprovada deve transformar a aplicação em um sistema capaz de persistir dados e acompanhar o ciclo de vida de drones, pedidos e viagens.

## Unidades de medida

- Peso, capacidade e carga devem usar quilogramas (`kg`).
- Coordenadas X/Y, distância, alcance e raio de obstáculos devem usar quilômetros (`km`).
- Velocidade média dos drones deve usar quilômetros por hora (`km/h`).
- Bateria e reserva mínima devem usar percentual (`%`).
- Consumo de bateria deve usar percentual por quilômetro (`%/km`).
- Taxa de recarga deve usar percentual por minuto (`%/min`).
- Duração e estimativas de entrega devem usar minutos (`min`).

Com essas unidades, a duração estimada deve ser calculada por `(totalDistance / speed) * 60`, considerando `totalDistance` em km e `speed` em km/h.

## Requisitos obrigatórios

- A cidade deve ser representada por coordenadas 2D.
- Cada drone deve possuir capacidade máxima de peso.
- Cada drone deve possuir alcance máximo por carga ou por viagem.
- Cada pedido deve possuir:
  - localização X;
  - localização Y;
  - peso;
  - prioridade baixa, média ou alta.
- O sistema deve alocar pacotes buscando o menor número possível de viagens.
- A solução deve respeitar:
  - capacidade máxima de peso do drone;
  - alcance máximo do drone;
  - prioridade dos pedidos.
- Deve existir um `README.md` com instruções de execução.
- Deve haver testes unitários.
- A evolução operacional deve persistir drones, pedidos e viagens.
- A evolução operacional deve permitir controlar drones disponíveis.
- A evolução operacional deve permitir controlar pedidos solicitados, alocados, em rota, entregues, cancelados e não alocados.

## Funcionalidades opcionais

As funcionalidades abaixo foram mencionadas como possibilidades adicionais. No primeiro ciclo nenhuma delas foi implementada; depois disso, a API REST foi escolhida para permitir testes via Postman:

- bateria do drone;
- obstáculos ou zonas de exclusão;
- cálculo do tempo total;
- fila de entregas;
- simulação por estados;
- API REST implementada como interface mínima para planejamento de viagens;
- CLI não implementada;
- relatório ou dashboard;
- tratamento de erros;
- recarga automática;
- feedback do status da entrega.

O roteiro atual aprovou a evolução de bateria, recarga, cálculo de tempo, obstáculos, fila de entregas e replanejamento de pedidos não entregues. A bateria básica do drone, sua consulta interna, a validação de bateria mínima, a fila de recarga, o tempo estimado, a fila operacional de pedidos, obstáculos circulares, retorno antecipado e replanejamento por transferência já foram implementados.

## Regras de negócio já definidas

- Pedidos possuem prioridade baixa, média ou alta.
- A distância entre dois pontos deve ser calculada usando distância euclidiana.
- A base dos drones é fixa na coordenada `(0,0)`.
- Toda viagem deve sair da base e retornar para a base.
- O alcance máximo do drone deve considerar a distância total de ida e volta da viagem.
- Drones não podem transportar peso acima de sua capacidade máxima.
- Drones não podem executar viagens que excedam seu alcance máximo.
- Uma viagem pode transportar vários pedidos, desde que respeite a capacidade máxima de peso e o alcance máximo do drone.
- A sequência de pedidos dentro de cada viagem deve ser otimizada para reduzir a distância total da rota.
- A validação de alcance da viagem deve considerar a rota otimizada.
- A alocação deve tentar reduzir o número total de viagens dentro de cada grupo de prioridade.
- Os pedidos devem ser processados na ordem de prioridade alta, média e baixa.
- Pacotes impossíveis de transportar por qualquer drone devem ser marcados como não alocados com o respectivo motivo.
- Motivos de não alocação por restrição conhecida devem diferenciar:
  - peso acima da capacidade máxima dos drones;
  - distância de ida e volta acima do alcance máximo dos drones;
  - peso e distância acima dos limites dos drones.
- O motivo genérico deve ser mantido quando a causa não for atribuível exclusivamente a peso ou alcance, como ausência de drone disponível.
- O domínio deve aceitar uma lista de drones.
- A primeira versão deve conter somente domínio e testes unitários, sem CLI, API REST ou interface gráfica.
- A API REST operacional deve usar dados persistidos para cadastro, consulta e planejamento.
- A persistência da evolução operacional deve usar PostgreSQL via Docker.
- O PostgreSQL local deve usar banco `drone_delivery`, usuário `drone`, senha `drone` e porta `5432`.
- A evolução do esquema do banco deve ser controlada com Flyway.
- Pedidos devem possuir status operacional.
- Pedidos cadastrados pela API devem usar o identificador de rastreio como código de confirmação de entrega.
- A resposta de cadastro do pedido deve retornar o código de confirmação, com o mesmo valor do rastreio, para o cliente.
- Consultas de pedidos não devem expor o código de confirmação.
- Pedidos cadastrados pela API devem possuir horário confirmado de entrega.
- A experiência Cliente deve permitir cadastro, login e logout do usuário.
- Pedidos criados pela experiência Cliente devem ser vinculados à conta autenticada.
- A aba `Meus pedidos` deve listar apenas pedidos da conta autenticada e permitir alternar entre eles.
- A experiência Cliente não deve permitir solicitação de entrega sem autenticação.
- Pedidos não alocados devem manter uma mensagem de status para admin e cliente.
- Motivos de não alocação exibidos pela API operacional e pela experiência Cliente devem estar em português.
- O admin deve poder cancelar pedidos não alocados informando justificativa ou reenviar o pacote para planejamento.
- O cliente deve receber mensagem quando o pacote não puder ser alocado, quando não for entregue ou quando a entrega for cancelada.
- Os status iniciais de pedido serão:
  - `REQUESTED`;
  - `ALLOCATED`;
  - `IN_ROUTE`;
  - `PENDING_REASSIGNMENT`;
  - `DELIVERED`;
  - `NOT_DELIVERED`;
  - `CANCELLED`;
  - `UNALLOCATED`.
- Drones devem possuir status operacional.
- Os status operacionais de drone serão:
  - `AVAILABLE`;
  - `IN_ROUTE`;
  - `UNAVAILABLE`;
  - `CHARGING`.
- Um drone deve deixar de estar disponível somente quando uma viagem for iniciada, não apenas quando o plano for criado.
- O admin deve poder excluir drones que não estejam em rota e que não possuam viagens vinculadas.
- Viagens devem possuir status operacional.
- Os status iniciais de viagem serão:
  - `PLANNED`;
  - `IN_ROUTE`;
  - `RETURNED_EARLY`;
  - `COMPLETED`;
  - `CANCELLED`.
- Uma viagem criada pelo planejamento deve iniciar com status `PLANNED`.
- Ao iniciar uma viagem:
  - a viagem deve passar de `PLANNED` para `IN_ROUTE`;
  - o drone associado deve estar `AVAILABLE`;
  - o drone associado deve passar para `IN_ROUTE`;
  - os pedidos associados devem passar para `IN_ROUTE`.
- Ao concluir uma viagem com bateria suficiente para a rota completa:
  - a viagem deve passar de `IN_ROUTE` para `COMPLETED`;
  - todas as posições da rota devem estar resolvidas, por confirmação do cliente ou falha de entrega por prazo expirado;
  - o drone associado deve voltar para `AVAILABLE`;
  - os pedidos confirmados devem permanecer `DELIVERED`.
- Durante uma viagem:
  - a API deve permitir registrar telemetria de bateria;
  - a telemetria deve atualizar a bateria atual do drone associado;
  - a telemetria deve ser persistida em histórico consultável por viagem;
  - a API deve permitir confirmar entrega por posição da rota com código informado pelo cliente;
  - a confirmação de entrega deve exigir disponibilidade confirmada pelo cliente após notificação de aproximação;
  - a confirmação de entrega deve exigir que o drone tenha alcançado a posição da rota;
  - a confirmação de entrega deve rejeitar código ausente ou inválido;
  - se o cliente não responder à notificação de disponibilidade dentro do prazo, o drone deve retornar à base com a encomenda;
  - nesse retorno por falta de disponibilidade, o pacote atual deve passar para `NOT_DELIVERED` com motivo em português;
  - depois que o drone alcançar o endereço e a disponibilidade estiver confirmada, o cliente deve ter 1 minuto para informar o código de recebimento;
  - se o cliente não informar o código nesse prazo, o pacote atual deve passar para `NOT_DELIVERED` com motivo em português e o drone deve seguir a rota levando a encomenda de volta para a base;
  - se a bateria informada não comportar a rota completa com reserva mínima, o retorno antecipado deve ser acionado imediatamente.
- Ao concluir uma viagem sem bateria suficiente para a rota completa:
  - o drone deve retornar à base preservando a reserva mínima possível;
  - a viagem deve passar para `RETURNED_EARLY`;
  - o drone associado deve entrar em `CHARGING`;
  - os pedidos com entrega reportada devem permanecer `DELIVERED`;
  - os pedidos restantes devem passar para `PENDING_REASSIGNMENT`.
- Ao cancelar uma viagem:
  - a viagem deve estar `PLANNED` ou `IN_ROUTE`;
  - a viagem deve passar para `CANCELLED`;
  - se o drone ainda estiver associado à viagem cancelada, ele deve voltar para `AVAILABLE`;
  - pedidos que ainda não foram entregues devem voltar para `REQUESTED`, salvo se uma regra futura definir outro status;
  - viagens `COMPLETED` ou já `CANCELLED` devem ser rejeitadas.
- A API operacional mínima deve expor cadastro e consulta de drones:
  - `POST /api/drones`;
  - `GET /api/drones`;
  - `GET /api/drones?status=AVAILABLE`;
  - `GET /api/drones/{id}`;
  - `GET /api/drones/available`;
  - `POST /api/drones/{id}/unavailable`;
  - `POST /api/drones/{id}/available`;
  - `POST /api/drones/{id}/recharge`;
  - `POST /api/drones/{id}/recharge/complete`;
  - `GET /api/recharge-queue`.
- Ao cadastrar um drone pela API operacional, o status inicial deve ser `AVAILABLE`.
- Ao cadastrar um drone pela API operacional, campos básicos de bateria podem ser informados.
- Quando os campos operacionais de bateria, velocidade e recarga não forem informados no cadastro, devem ser usados valores padrão.
- Campos de bateria inválidos devem retornar HTTP `400` com mensagem clara.
- O endpoint `POST /api/drones` deve retornar HTTP `201 Created` quando o cadastro for realizado.
- O identificador do drone deve ser único.
- Ao tentar cadastrar um drone com identificador já existente, a API deve retornar HTTP `409 Conflict`.
- As consultas de drones devem retornar os resultados em ordem crescente de `id`.
- Consulta de drone por `id` inexistente deve retornar HTTP `404 Not Found`.
- A API interna deve expor consulta de bateria de drones:
  - `GET /internal/drones/{id}/battery`.
- A consulta interna de bateria deve retornar HTTP `404 Not Found` quando o drone não existir.
- A API operacional deve permitir colocar drone elegível na fila de recarga com `POST /api/drones/{id}/recharge`.
- A API operacional deve permitir concluir recarga com `POST /api/drones/{id}/recharge/complete`.
- A API operacional deve permitir consultar a fila de recarga com `GET /api/recharge-queue`.
- Drones em fila de recarga devem possuir status `CHARGING`.
- A fila de recarga deve ser ordenada por entrada na fila.
- Drones `AVAILABLE` com bateria abaixo de `100.0` podem entrar manualmente na fila de recarga.
- Drones `CHARGING` podem concluir recarga, voltar para `AVAILABLE` e ter bateria restaurada para `100.0`.
- Ao marcar um drone como indisponível pela API:
  - drone inexistente deve retornar HTTP `404 Not Found`;
  - o drone deve estar `AVAILABLE`;
  - drone em status diferente de `AVAILABLE` deve retornar HTTP `400 Bad Request`;
  - o status deve mudar de `AVAILABLE` para `UNAVAILABLE`.
- Ao marcar um drone como disponível pela API:
  - drone inexistente deve retornar HTTP `404 Not Found`;
  - o drone deve estar `UNAVAILABLE`;
  - drone em status diferente de `UNAVAILABLE` deve retornar HTTP `400 Bad Request`;
  - o status deve mudar de `UNAVAILABLE` para `AVAILABLE`.
- Drone em rota (`IN_ROUTE`) não pode ter disponibilidade alterada manualmente.
- A API operacional mínima deve expor cadastro e consulta de pedidos:
  - `POST /api/orders`;
  - `GET /api/orders`;
  - `GET /api/orders/{id}`;
  - `GET /api/orders?status=REQUESTED`.
- Ao cadastrar um pedido pela API operacional, o status inicial deve ser `REQUESTED`.
- O endpoint `POST /api/orders` deve retornar HTTP `201 Created` quando o cadastro for realizado.
- O identificador do pedido deve ser único.
- Ao tentar cadastrar um pedido com identificador já existente, a API deve retornar HTTP `409 Conflict`.
- As consultas de pedidos devem retornar os resultados em ordem crescente de `id`.
- Consulta de pedido por `id` inexistente deve retornar HTTP `404 Not Found`.
- Filtros de status com valor inválido devem retornar HTTP `400` com mensagem clara.
- A API operacional mínima deve expor planejamento e consulta de viagens:
  - `POST /api/trip-plans`;
  - `GET /api/trips`;
  - `GET /api/trips?status=PLANNED`;
  - `GET /api/trips/{id}`.
- Na evolução operacional, `POST /api/trip-plans` deve usar drones e pedidos salvos, em vez de exigir todos os dados no corpo da requisição.
- Ao planejar viagens com dados salvos:
  - somente drones `AVAILABLE` devem ser considerados;
  - somente pedidos `REQUESTED` ou `PENDING_REASSIGNMENT` devem ser considerados;
  - a rota planejada deve exigir bateria suficiente para a distância total e a reserva mínima de retorno do drone;
  - drones disponíveis que teriam peso e alcance para pedidos solicitados, mas não possuem bateria suficiente para nenhum deles, devem entrar automaticamente na fila de recarga;
  - viagens criadas devem iniciar como `PLANNED`;
  - viagens planejadas devem ser persistidas;
  - a sequência de pedidos da rota deve ser persistida;
  - pedidos alocados devem passar para `ALLOCATED`;
  - pedidos impossíveis devem passar para `UNALLOCATED`.
- As consultas de viagens devem retornar os resultados em ordem crescente de `id`.
- Consulta de viagem por `id` inexistente deve retornar HTTP `404 Not Found`.
- A API operacional mínima deve expor transições de viagem:
  - `POST /api/trips/{id}/start`;
  - `POST /api/trips/{id}/complete`;
  - `POST /api/trips/{id}/cancel`.
- Ao iniciar uma viagem pela API:
  - viagem inexistente deve retornar HTTP `404 Not Found`;
  - viagem que não esteja `PLANNED` deve retornar HTTP `400 Bad Request`;
  - drone que não esteja `AVAILABLE` deve retornar HTTP `400 Bad Request`;
  - drone sem bateria suficiente para a rota completa e reserva mínima de retorno deve retornar HTTP `400 Bad Request`.
- Ao concluir uma viagem pela API:
  - viagem inexistente deve retornar HTTP `404 Not Found`;
  - viagem que não esteja `IN_ROUTE` deve retornar HTTP `400 Bad Request`.
- Ao cancelar uma viagem pela API:
  - viagem inexistente deve retornar HTTP `404 Not Found`;
  - viagem que não esteja `PLANNED` nem `IN_ROUTE` deve retornar HTTP `400 Bad Request`.
- Nenhuma funcionalidade opcional deve ser implementada no primeiro ciclo.
- A localização dos pedidos é definida por coordenadas X e Y em um plano 2D.

## Evolução aprovada: bateria, recarga, tempo, obstáculos e filas

Esta seção registra o roteiro aprovado e o estado implementado.

### Regras planejadas

- Cada drone possui nível de bateria operacional.
- Cada drone possui consumo estimado de bateria em percentual por quilômetro.
- Cada drone possui reserva mínima de segurança para retorno à base.
- Cada drone possui velocidade média para estimativa de tempo.
- Uma viagem só pode ser planejada ou iniciada quando a bateria prevista for suficiente para cumprir a rota completa e retornar com a reserva mínima de segurança.
- A bateria necessária considera distância ajustada por obstáculos, e não apenas a distância euclidiana direta.
- Drones sem bateria suficiente para iniciar uma viagem entram em fila de recarga.
- Durante uma viagem, se a bateria atual não comportar a rota completa, o drone retorna à base mesmo que ainda existam pedidos pendentes na rota.
- Pedidos sem entrega reportada por retorno antecipado mudam para `PENDING_REASSIGNMENT`.
- Pedidos `PENDING_REASSIGNMENT` voltam para a fila operacional e podem ser transferidos para outro drone `AVAILABLE`.
- A estimativa de tempo de entrega deve considerar distância ajustada e velocidade do drone.
- Obstáculos impactam distância, tempo e viabilidade da rota.
- Obstáculos podem tornar uma rota inviável quando não houver desvio seguro dentro das regras do modelo.
- A fila de entrega deverá ordenar pedidos por prioridade e data de entrada na fila.
- O planejamento deverá poder usar rota otimizada ou respeitar a sequência da fila, conforme opção informada na criação do plano.
- A rota automática deve ordenar entregas por prioridade, maior peso, menor distância da base e identificador.
- A otimização de rota considera peso, prioridade, distância, fila, bateria e obstáculos antes de persistir uma viagem planejada.

### Campos de drone implementados

Drone:

- `batteryLevel`: percentual atual de bateria.
- `batteryConsumptionPerDistanceUnit`: consumo estimado em percentual por quilômetro (`%/km`).
- `minimumReturnBattery`: percentual mínimo reservado para retorno seguro.
- `speed`: velocidade média em quilômetros por hora (`km/h`) usada no cálculo de tempo.
- `chargingRate`: taxa estimada de recarga em percentual por minuto (`%/min`).

### Campos de viagem implementados

Viagem:

- `estimatedDuration`: duração estimada calculada por `(totalDistance / speed) * 60` do drone associado.
- `averageDeliveryTime`: média dos tempos acumulados até cada entrega da rota.

Item da rota:

- `estimatedDeliveryTime`: tempo acumulado estimado desde o início da viagem até a entrega do pedido naquela posição.

### Campos de pedido implementados

Pedido:

- `queuedAt`: data e hora de entrada na fila operacional.
- `confirmedDeliveryTime`: data e hora confirmada para a entrega.

### Campos de obstáculo implementados

Obstáculo:

- `id`: identificador persistido.
- `center`: coordenada central em X e Y.
- `radius`: raio da zona de restrição circular.
- `active`: indica se o obstáculo deve ser considerado no planejamento.

### Campos planejados

Pedido:

- `reassignmentReason`: motivo do retorno para replanejamento, quando aplicável.

Viagem:

- `estimatedCompletionTime`: data e hora estimadas para conclusão.
- `batteryRequired`: bateria estimada para executar a rota planejada.

### Status planejados

Novos status de drone planejados:

- `RETURNING_TO_BASE`.

Status de pedido implementado:

- `PENDING_REASSIGNMENT`.

Status de viagem implementado:

- `RETURNED_EARLY`.

Novos status de viagem ainda planejados:

- `RETURNING`;
- `INTERRUPTED`.

### Ordem sugerida de implementação

1. Bateria básica do drone e endpoint interno de consulta. Implementado.
2. Validação de bateria mínima para planejar e iniciar viagem. Implementado.
3. Fila de recarga para drones sem bateria suficiente. Implementado.
4. Estimativa de tempo de entrega. Implementado.
5. Fila operacional de entrega e opção `optimizeRoute`. Implementado.
6. Obstáculos circulares e distância ajustada. Implementado.
7. Retorno antecipado por bateria mínima. Implementado.
8. Status `PENDING_REASSIGNMENT` e transferência de pedidos para outro drone. Implementado.
9. Jornada guiada no dashboard para o ciclo operacional completo. Implementado.
10. Reset e carregamento controlado de cenário demo pelo dashboard. Implementado.
11. Status de disponibilidade da API e bloqueio de ações quando o backend estiver offline. Implementado.
12. Script de execução local integrada para banco, backend e dashboard. Implementado.
13. Status operacionais em português no dashboard. Implementado.
14. Descrições de hover nos botões de ação da consulta operacional. Implementado.
15. Modos de visualização e diferenciação de viagens no mapa 2D. Implementado.
16. Separação visual entre experiências Admin e Cliente no frontend. Implementado.
17. Tela Cliente para solicitação limitada, acompanhamento por ID ou código e avaliações. Implementado.

## Contratos mínimos da API operacional

### Drones

`POST /api/drones`

Entrada:

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

Saída:

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

Os campos operacionais de bateria, velocidade e recarga são opcionais no cadastro.

`GET /api/drones` deve listar todos os drones em ordem crescente de `id`.

`GET /api/drones?status=UNAVAILABLE` deve listar drones filtrados por status, em ordem crescente de `id`.

Status inválido no filtro deve retornar HTTP `400`.

`GET /api/drones/{id}` deve retornar um drone pelo `id`.

Saída:

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

Drone inexistente deve retornar HTTP `404`.

`GET /internal/drones/{id}/battery` deve retornar a bateria operacional do drone pelo `id`.

Saída:

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

Drone inexistente deve retornar HTTP `404`.

`GET /api/drones/available` deve listar apenas drones com status `AVAILABLE`, em ordem crescente de `id`.

`POST /api/drones/{id}/unavailable` deve marcar um drone disponível como indisponível.

Saída:

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

Erros esperados:

- HTTP `404` quando o drone não existir.
- HTTP `400` quando o drone não estiver `AVAILABLE`.

`POST /api/drones/{id}/available` deve marcar um drone indisponível como disponível.

Saída:

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

Erros esperados:

- HTTP `404` quando o drone não existir.
- HTTP `400` quando o drone não estiver `UNAVAILABLE`.

### Pedidos

`POST /api/orders`

Entrada:

```json
{
  "identifier": "ORDER-1",
  "location": { "x": 3.0, "y": 4.0 },
  "weight": 4.0,
  "priority": "HIGH",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
}
```

Saída:

```json
{
  "id": 1,
  "identifier": "ORDER-1",
  "location": { "x": 3.0, "y": 4.0 },
  "weight": 4.0,
  "priority": "HIGH",
  "status": "REQUESTED",
  "queuedAt": "2026-07-25T20:00:00Z",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
}
```

`GET /api/orders` deve listar todos os pedidos em ordem crescente de `id`.

`GET /api/orders/{id}` deve retornar um pedido pelo `id`.

Saída:

```json
{
  "id": 1,
  "identifier": "ORDER-1",
  "location": { "x": 3.0, "y": 4.0 },
  "weight": 4.0,
  "priority": "HIGH",
  "status": "REQUESTED",
  "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
}
```

Pedido inexistente deve retornar HTTP `404`.

`GET /api/orders?status=REQUESTED` deve listar pedidos filtrados por status, em ordem crescente de `id`.

Status inválido no filtro deve retornar HTTP `400`.

`GET /api/delivery-queue` deve listar pedidos `REQUESTED` e `PENDING_REASSIGNMENT` em ordem de fila operacional.

Saída:

```json
[
  {
    "orderId": 1,
    "orderIdentifier": "ORDER-1",
    "location": { "x": 3.0, "y": 4.0 },
    "weight": 4.0,
    "priority": "HIGH",
    "status": "REQUESTED",
    "queuedAt": "2026-07-25T20:00:00Z",
    "confirmedDeliveryTime": "2026-07-26T18:30:00Z"
  }
]
```

Pedidos na fila operacional devem ser ordenados por `queuedAt` e `id`.

### Obstáculos

`POST /api/obstacles` deve cadastrar um obstáculo ativo.

Entrada:

```json
{
  "center": { "x": 5.0, "y": 0.0 },
  "radius": 1.0
}
```

Saída:

```json
{
  "id": 1,
  "center": { "x": 5.0, "y": 0.0 },
  "radius": 1.0,
  "active": true
}
```

`GET /api/obstacles` deve listar obstáculos cadastrados em ordem crescente de `id`.

`DELETE /api/obstacles/{id}` deve desativar um obstáculo.

Saída:

```json
{
  "id": 1,
  "center": { "x": 5.0, "y": 0.0 },
  "radius": 1.0,
  "active": false
}
```

Obstáculo inexistente deve retornar HTTP `404`.

### Planejamento

`POST /api/trip-plans`
`POST /api/trip-plans?optimizeRoute=false`

Esse endpoint deve planejar viagens usando drones e pedidos salvos.

Entrada: sem corpo obrigatório.
O parâmetro opcional `optimizeRoute` deve aceitar `true` ou `false` e usar `true` por padrão.
Obstáculos ativos devem ajustar a distância dos trechos que cruzariam a zona circular.

Saída:

```json
{
  "trips": [
    {
      "id": 1,
      "droneId": 1,
      "status": "PLANNED",
      "orders": [1],
      "route": [1],
      "totalWeight": 4.0,
      "totalDistance": 10.0,
      "estimatedDuration": 10.0
    }
  ],
  "unallocatedOrders": []
}
```

Pedidos impossíveis devem aparecer em `unallocatedOrders`:

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

### Viagens

`GET /api/trips` deve listar viagens salvas em ordem crescente de `id`.

`GET /api/trips?status=IN_ROUTE` deve listar viagens filtradas por status, em ordem crescente de `id`.

Status inválido no filtro deve retornar HTTP `400`.

Saída:

```json
[
  {
    "id": 1,
    "droneId": 1,
    "status": "PLANNED",
    "orders": [1],
    "route": [1],
    "totalWeight": 4.0,
    "totalDistance": 10.0,
    "estimatedDuration": 10.0
  }
]
```

`GET /api/trips/{id}` deve retornar uma viagem pelo `id`.

Saída:

```json
{
  "id": 1,
  "droneId": 1,
  "status": "PLANNED",
  "orders": [1],
  "route": [1],
  "totalWeight": 4.0,
  "totalDistance": 10.0,
  "estimatedDuration": 10.0
}
```

Viagem inexistente deve retornar HTTP `404`.

`POST /api/trips/{id}/start` deve iniciar uma viagem planejada.

Saída:

```json
{
  "id": 1,
  "droneId": 1,
  "status": "IN_ROUTE",
  "orders": [1],
  "route": [1],
  "totalWeight": 4.0,
  "totalDistance": 10.0,
  "estimatedDuration": 10.0
}
```

Erros esperados:

- HTTP `404` quando a viagem não existir;
- HTTP `400` quando a viagem não estiver `PLANNED`;
- HTTP `400` quando o drone associado não estiver `AVAILABLE`.

`POST /api/trips/{id}/complete` deve concluir uma viagem em rota.

Saída:

```json
{
  "id": 1,
  "droneId": 1,
  "status": "COMPLETED",
  "orders": [1],
  "route": [1],
  "totalWeight": 4.0,
  "totalDistance": 10.0,
  "estimatedDuration": 10.0
}
```

Erros esperados:

- HTTP `404` quando a viagem não existir;
- HTTP `400` quando a viagem não estiver `IN_ROUTE`.

`POST /api/trips/{id}/cancel` deve cancelar uma viagem planejada ou em rota.

Saída:

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

Erros esperados:

- HTTP `404` quando a viagem não existir;
- HTTP `400` quando a viagem não estiver `PLANNED` nem `IN_ROUTE`.

## Decisões ainda pendentes

- Definir se haverá CLI além da API REST.

## Critérios de aceite

- O sistema permite cadastrar ou informar uma lista de drones com capacidade máxima de peso e alcance máximo.
- O sistema permite cadastrar ou informar pedidos com coordenadas X e Y, peso e prioridade.
- O sistema calcula a distância entre coordenadas usando distância euclidiana.
- O sistema considera a base dos drones como a coordenada fixa `(0,0)`.
- O sistema considera que toda viagem sai da base e retorna para a base.
- O sistema valida o alcance máximo usando a distância total de ida e volta da viagem.
- O sistema gera uma alocação de entregas respeitando capacidade máxima de peso.
- O sistema gera uma alocação de entregas respeitando alcance máximo.
- O sistema permite alocar vários pedidos em uma mesma viagem quando capacidade e alcance forem respeitados.
- O sistema processa pedidos de prioridade alta antes de prioridade média, e prioridade média antes de prioridade baixa.
- O sistema busca reduzir o número de viagens necessárias dentro de cada grupo de prioridade.
- O sistema marca pacotes impossíveis de transportar como não alocados e informa o motivo.
- O sistema diferencia motivo de não alocação por peso, alcance ou peso e alcance quando essas restrições forem identificáveis.
- A primeira versão é exercitada por testes unitários sobre o domínio, sem exigir CLI, API REST ou interface gráfica.
- A API REST permite cadastrar drones e pedidos em JSON antes do planejamento.
- A API REST permite acionar `POST /api/trip-plans` usando os dados previamente salvos.
- A API REST retorna viagens alocadas e pedidos não alocados em JSON.
- Entradas inválidas na API retornam HTTP `400` com mensagem clara.
- Pedidos impossíveis de transportar não retornam erro HTTP; eles aparecem como não alocados com motivo.
- A evolução operacional persiste drones, pedidos e viagens em PostgreSQL.
- O ambiente local possui configuração de PostgreSQL via Docker Compose.
- O projeto usa Flyway para versionar alterações no esquema do banco.
- O cadastro de drones persiste dados na tabela `drones`.
- O endpoint `POST /api/drones` retorna HTTP `201 Created` para cadastro válido.
- O endpoint `POST /api/drones` retorna HTTP `409 Conflict` para identificador duplicado.
- O endpoint `POST /api/drones` aceita campos opcionais de bateria.
- O endpoint `POST /api/drones` aplica valores padrão quando campos operacionais de bateria, velocidade e recarga não são informados.
- O endpoint `POST /api/drones` rejeita campos operacionais inválidos de bateria, velocidade e recarga com HTTP `400`.
- O endpoint `GET /api/drones` retorna todos os drones cadastrados em ordem crescente de `id`.
- O endpoint `GET /api/drones?status=UNAVAILABLE` retorna somente drones com status `UNAVAILABLE` em ordem crescente de `id`.
- Um filtro de status de drone inválido retorna HTTP `400` com os valores aceitos.
- O endpoint `GET /api/drones/{id}` retorna o drone correspondente quando ele existe.
- O endpoint `GET /api/drones/{id}` rejeita drone inexistente com HTTP `404`.
- O endpoint `GET /internal/drones/{id}/battery` retorna a bateria operacional do drone correspondente.
- O endpoint `GET /internal/drones/{id}/battery` rejeita drone inexistente com HTTP `404`.
- O endpoint `GET /api/drones/available` retorna somente drones com status `AVAILABLE` em ordem crescente de `id`.
- O endpoint `POST /api/drones/{id}/unavailable` altera um drone `AVAILABLE` para `UNAVAILABLE`.
- O endpoint `POST /api/drones/{id}/available` altera um drone `UNAVAILABLE` para `AVAILABLE`.
- O endpoint `POST /api/drones/{id}/recharge` altera um drone `AVAILABLE` com bateria abaixo de `100.0` para `CHARGING`.
- O endpoint `POST /api/drones/{id}/recharge/complete` altera um drone `CHARGING` para `AVAILABLE` e restaura sua bateria para `100.0`.
- O endpoint `GET /api/recharge-queue` retorna drones em `CHARGING` na ordem de entrada na fila.
- Os endpoints de disponibilidade de drones rejeitam drone inexistente com HTTP `404`.
- Os endpoints de disponibilidade de drones rejeitam transição inválida com HTTP `400`.
- Drone `IN_ROUTE` não pode ser alterado manualmente para `AVAILABLE` ou `UNAVAILABLE`.
- Drone `CHARGING` não pode ser alterado manualmente para `AVAILABLE` pelo endpoint de disponibilidade.
- A evolução operacional permite identificar drones com status `AVAILABLE`, `IN_ROUTE`, `UNAVAILABLE` e `CHARGING`.
- A evolução operacional permite identificar pedidos com status `REQUESTED`, `ALLOCATED`, `IN_ROUTE`, `PENDING_REASSIGNMENT`, `DELIVERED`, `NOT_DELIVERED`, `CANCELLED` e `UNALLOCATED`.
- O endpoint `POST /api/orders` persiste pedidos válidos com status inicial `REQUESTED`.
- O endpoint `POST /api/orders` exige e persiste `confirmedDeliveryTime`.
- O endpoint `POST /api/orders` retorna HTTP `409 Conflict` para identificador duplicado.
- O endpoint `POST /api/orders` atribui `queuedAt` aos pedidos criados.
- O endpoint `GET /api/orders` retorna todos os pedidos cadastrados em ordem crescente de `id`.
- O endpoint `GET /api/orders/{id}` retorna o pedido correspondente quando ele existe.
- O endpoint `GET /api/orders/{id}` rejeita pedido inexistente com HTTP `404`.
- O endpoint `GET /api/orders?status=REQUESTED` retorna somente pedidos com status `REQUESTED` em ordem crescente de `id`.
- O endpoint `GET /api/delivery-queue` retorna pedidos `REQUESTED` e `PENDING_REASSIGNMENT` ordenados por `queuedAt` e `id`.
- O endpoint `POST /api/reviews` cadastra avaliações do serviço com estrelas de 1 a 5, título e feedback.
- O endpoint `POST /api/reviews` rejeita estrelas fora de 1 a 5 com HTTP `400`.
- O endpoint `POST /api/reviews` rejeita título ou feedback em branco com HTTP `400`.
- O endpoint `GET /api/reviews` retorna avaliações cadastradas em ordem crescente de `id`.
- O endpoint `GET /api/reviews/{id}` retorna a avaliação correspondente quando ela existe.
- O endpoint `GET /api/reviews/{id}` rejeita avaliação inexistente com HTTP `404`.
- O endpoint `POST /api/obstacles` cadastra obstáculo ativo com centro e raio.
- O endpoint `POST /api/obstacles` rejeita centro ausente ou raio inválido com HTTP `400`.
- O endpoint `GET /api/obstacles` retorna obstáculos cadastrados em ordem crescente de `id`.
- O endpoint `DELETE /api/obstacles/{id}` desativa o obstáculo e retorna `active: false`.
- O endpoint `DELETE /api/obstacles/{id}` rejeita obstáculo inexistente com HTTP `404`.
- Um filtro de status inválido retorna HTTP `400` com os valores aceitos.
- Criar um plano de viagem não torna o drone indisponível por si só.
- O endpoint `POST /api/trip-plans` considera somente drones `AVAILABLE`.
- O endpoint `POST /api/trip-plans` considera somente pedidos `REQUESTED` e `PENDING_REASSIGNMENT`.
- O endpoint `POST /api/trip-plans` só cria viagens quando a bateria do drone cobre a rota completa e a reserva mínima de retorno.
- O endpoint `POST /api/trip-plans` move para `CHARGING` drones disponíveis sem bateria suficiente para pedidos que eles poderiam atender por peso e alcance.
- O endpoint `POST /api/trip-plans` persiste viagens planejadas com status `PLANNED`.
- O endpoint `POST /api/trip-plans` retorna `estimatedDuration` calculado por `(totalDistance / speed) * 60` do drone.
- O endpoint `POST /api/trip-plans` retorna `averageDeliveryTime` calculado pela média dos tempos acumulados de entrega.
- O endpoint `POST /api/trip-plans` retorna `estimatedDeliveryTime` por posição da rota.
- O endpoint `POST /api/trip-plans` usa `optimizeRoute=true` por padrão.
- O endpoint `POST /api/trip-plans` com `optimizeRoute=true` ordena entregas por prioridade, maior peso, menor distância da base e identificador.
- O endpoint `POST /api/trip-plans?optimizeRoute=false` persiste a rota respeitando a fila operacional de pedidos.
- Um parâmetro `optimizeRoute` inválido retorna HTTP `400`.
- Obstáculos ativos ajustam `totalDistance` das viagens planejadas quando algum trecho cruza a zona circular.
- Obstáculos ativos afetam validação de alcance, validação de bateria e `estimatedDuration`.
- As respostas de consulta e transição de viagens retornam `estimatedDuration`.
- O endpoint `GET /api/trips?status=IN_ROUTE` retorna somente viagens com status `IN_ROUTE` em ordem crescente de `id`.
- Um filtro de status de viagem inválido retorna HTTP `400` com os valores aceitos.
- O endpoint `POST /api/trip-plans` persiste a sequência otimizada de pedidos da rota quando `optimizeRoute=true`.
- O planejamento valida o alcance da viagem usando a sequência de rota selecionada pelo parâmetro `optimizeRoute`.
- O endpoint `POST /api/trip-plans` altera pedidos alocados para `ALLOCATED`.
- O endpoint `POST /api/trip-plans` altera pedidos impossíveis para `UNALLOCATED`.
- O endpoint `POST /api/trip-plans` retorna motivo detalhado para pedido não alocado por peso, alcance ou peso e alcance.
- O endpoint `POST /api/trip-plans` retorna motivo detalhado para pedido não alocado por bateria insuficiente.
- O endpoint `GET /api/trips` retorna viagens salvas em ordem crescente de `id`.
- O endpoint `GET /api/trips/{id}` retorna a viagem correspondente quando ela existe.
- O endpoint `GET /api/trips/{id}` rejeita viagem inexistente com HTTP `404`.
- O endpoint `POST /api/trips/{id}/start` altera a viagem para `IN_ROUTE`.
- O endpoint `POST /api/trips/{id}/start` altera o drone associado para `IN_ROUTE`.
- O endpoint `POST /api/trips/{id}/start` altera os pedidos associados para `IN_ROUTE`.
- O endpoint `POST /api/trips/{id}/start` rejeita viagem inexistente com HTTP `404`.
- O endpoint `POST /api/trips/{id}/start` rejeita viagem que não esteja `PLANNED` com HTTP `400`.
- O endpoint `POST /api/trips/{id}/start` rejeita drone que não esteja `AVAILABLE` com HTTP `400`.
- O endpoint `POST /api/trips/{id}/start` rejeita drone sem bateria suficiente para a rota completa e reserva mínima de retorno com HTTP `400`.
- O endpoint `POST /api/trips/{id}/telemetry` atualiza a bateria atual do drone associado à viagem.
- O endpoint `POST /api/trips/{id}/telemetry` persiste a leitura de bateria no histórico da viagem.
- O endpoint `POST /api/trips/{id}/telemetry` mantém a viagem em `IN_ROUTE` quando a bateria informada ainda comporta a rota completa.
- O endpoint `POST /api/trips/{id}/telemetry` altera a viagem para `RETURNED_EARLY` quando a bateria informada não comporta a rota completa.
- O endpoint `POST /api/trips/{id}/telemetry` rejeita viagem inexistente com HTTP `404`.
- O endpoint `POST /api/trips/{id}/telemetry` rejeita viagem que não esteja `IN_ROUTE` com HTTP `400`.
- O endpoint `POST /api/trips/{id}/telemetry` rejeita bateria ausente ou inválida com HTTP `400`.
- O endpoint `GET /api/trips/{id}/telemetry` lista o histórico de telemetria da viagem por `reportedAt` e `id`.
- O endpoint `GET /api/trips/{id}/telemetry` rejeita viagem inexistente com HTTP `404`.
- As respostas de viagem incluem `routeProgress` com `orderId`, `routePosition`, `delivered` e `deliveredAt`.
- O endpoint `POST /api/orders` retorna `deliveryConfirmationCode` igual ao identificador do pedido no cadastro válido.
- O endpoint `GET /api/orders` e a consulta por `id` não retornam `deliveryConfirmationCode`.
- As respostas de pedido retornam `statusReason` quando houver mensagem de não alocação ou cancelamento.
- O endpoint `POST /api/orders/{id}/cancel` cancela pedido `UNALLOCATED` com justificativa obrigatória.
- O endpoint `POST /api/orders/{id}/requeue` retorna pedido `UNALLOCATED` para `REQUESTED`.
- O endpoint `DELETE /api/drones/{id}` exclui drones sem viagens vinculadas e rejeita drones `IN_ROUTE`.
- O endpoint `POST /api/trips/{id}/route/{routePosition}/deliver` confirma entrega de uma posição da rota com `confirmationCode`.
- O endpoint `POST /api/trips/{id}/route/{routePosition}/deliver` altera o pedido associado para `DELIVERED` somente quando o código informado é válido.
- O endpoint `POST /api/trips/{id}/route/{routePosition}/deliver` rejeita viagem inexistente com HTTP `404`.
- O endpoint `POST /api/trips/{id}/route/{routePosition}/deliver` rejeita posição inexistente com HTTP `404`.
- O endpoint `POST /api/trips/{id}/route/{routePosition}/deliver` rejeita viagem que não esteja `IN_ROUTE` com HTTP `400`.
- O endpoint `POST /api/trips/{id}/route/{routePosition}/deliver` rejeita corpo ausente, código ausente, código inválido ou drone que ainda não alcançou a posição com HTTP `400`.
- O endpoint `POST /api/trips/{id}/route/{routePosition}/deliver` rejeita posição negativa, fora de ordem ou já entregue com HTTP `400`.
- O endpoint `POST /api/trips/{id}/complete` altera a viagem para `COMPLETED`.
- O endpoint `POST /api/trips/{id}/complete` altera o drone associado para `AVAILABLE`.
- O endpoint `POST /api/trips/{id}/complete` exige que todas as posições da rota estejam resolvidas antes de concluir.
- O endpoint `POST /api/trips/{id}/complete` altera a viagem para `RETURNED_EARLY` quando a bateria atual não comporta a rota completa.
- O retorno antecipado preserva como `DELIVERED` somente posições da rota já reportadas como entregues.
- O endpoint `POST /api/trips/{id}/complete` altera pedidos restantes para `PENDING_REASSIGNMENT` no retorno antecipado.
- O endpoint `POST /api/trips/{id}/complete` altera o drone associado para `CHARGING` no retorno antecipado.
- O endpoint `POST /api/trips/{id}/complete` rejeita viagem inexistente com HTTP `404`.
- O endpoint `POST /api/trips/{id}/complete` rejeita viagem que não esteja `IN_ROUTE` com HTTP `400`.
- O endpoint `POST /api/trips/{id}/cancel` altera viagem `PLANNED` ou `IN_ROUTE` para `CANCELLED`.
- O endpoint `POST /api/trips/{id}/cancel` altera o drone associado para `AVAILABLE`.
- O endpoint `POST /api/trips/{id}/cancel` altera pedidos não entregues para `REQUESTED`.
- O endpoint `POST /api/trips/{id}/cancel` rejeita viagem inexistente com HTTP `404`.
- O endpoint `POST /api/trips/{id}/cancel` rejeita viagem `COMPLETED` ou já `CANCELLED` com HTTP `400`.
- A evolução operacional permite identificar viagens com status `PLANNED`, `IN_ROUTE`, `RETURNED_EARLY`, `COMPLETED` e `CANCELLED`.
- A suite de testes deve validar pelo menos uma jornada operacional persistida com Spring Boot, JPA, Flyway e PostgreSQL.
- O contrato HTTP da API operacional deve estar consolidado em `API.md`.
- Viagens recém-planejadas iniciam como `PLANNED`.
- Uma viagem `PLANNED` pode ser iniciada e passar para `IN_ROUTE`.
- Uma viagem `IN_ROUTE` pode ser concluída e passar para `COMPLETED`.
- Uma viagem `IN_ROUTE` pode retornar antecipadamente e passar para `RETURNED_EARLY`.
- Uma viagem que ainda não foi concluída pode ser cancelada e passar para `CANCELLED`.
- A conclusão de uma viagem exige posições de rota já resolvidas e libera o drone como `AVAILABLE`.
- A API operacional permite cadastrar drones com status inicial `AVAILABLE`.
- A API operacional permite listar todos os drones e listar apenas drones disponíveis.
- A API operacional permite cadastrar pedidos com status inicial `REQUESTED`.
- A API operacional permite listar pedidos e filtrar pedidos por status.
- A API operacional permite planejar viagens usando drones e pedidos previamente salvos.
- A API operacional permite listar viagens salvas.
- A API operacional permite iniciar, concluir e cancelar viagens por identificador.
- O dashboard frontend permite consultar indicadores operacionais e tabelas de drones, pedidos e viagens.
- O dashboard frontend permite consultar detalhes de uma viagem, incluindo rota, progresso por entrega e histórico de telemetria.
- O dashboard frontend permite visualizar mapa 2D com base, pedidos, obstáculos e rota da viagem selecionada.
- O dashboard frontend permite alternar o mapa 2D entre a viagem selecionada e todas as viagens.
- O dashboard frontend diferencia viagens no mapa 2D com cores, chips de seleção, setas de direção e pontos numerados pela ordem da rota.
- O dashboard frontend exibe o marcador do drone em movimento conforme o estado de simulação da viagem.
- O backend permite avançar uma viagem por tempo simulado com `POST /api/trips/{id}/simulation/tick`.
- A simulação automática inicia viagens planejadas, move o drone, consome bateria, solicita disponibilidade do cliente na aproximação, para em entregas alcançadas aguardando confirmação do cliente e conclui a viagem quando a rota termina.
- A simulação automática retorna o drone à base e marca o pacote atual como `NOT_DELIVERED` se o cliente não responder à solicitação de disponibilidade.
- A simulação automática aciona retorno antecipado quando a rota restante deixa de ser segura para a bateria atual.
- O dashboard frontend permite consultar filas operacionais de entrega, reatribuição e recarga.
- O dashboard frontend permite cadastrar drones e pedidos usando os endpoints existentes da API.
- O dashboard frontend permite informar horário confirmado ao cadastrar pedidos.
- O dashboard frontend permite acionar o planejamento persistido com opção de rota otimizada.
- O dashboard frontend permite cadastrar, listar e desativar obstáculos usando os endpoints existentes da API.
- O dashboard frontend permite cadastrar e consultar avaliações usando os endpoints existentes da API.
- O dashboard frontend permite executar ações operacionais de drones usando os endpoints existentes da API.
- O dashboard frontend permite excluir drones no painel Admin.
- O dashboard frontend permite cancelar ou reenviar para planejamento pedidos não alocados.
- O dashboard frontend apresenta uma área dedicada para tratar pedidos não alocados.
- O dashboard frontend permite executar ações operacionais de viagens usando os endpoints existentes da API.
- O dashboard frontend permite alternar entre experiência Admin e experiência Cliente.
- A experiência Admin mantém acesso ao painel operacional completo.
- A experiência Cliente permite solicitar entrega informando peso, coordenadas e horário confirmado.
- A experiência Cliente deve manter uma aba `Meus pedidos` com os pedidos vinculados à conta autenticada para alternar rapidamente o pedido acompanhado.
- A experiência Cliente permite acompanhar pedido por ID ou código.
- A experiência Cliente permite confirmar disponibilidade quando o drone está chegando ao destino.
- A experiência Cliente permite confirmar recebimento digitando o próprio código de rastreio do pedido acompanhado.
- A experiência Cliente exibe um aviso interativo com som quando o drone está chegando ao destino.
- A experiência Cliente exibe mensagens para pedidos não alocados, não entregues e cancelados.
- O dashboard frontend permite alternar o mês do relatório mensal de produtividade.
- A experiência Cliente exibe mapa da viagem associada ao pedido acompanhado quando houver planejamento.
- A experiência Cliente permite cadastrar avaliação e consultar avaliações públicas.
- O dashboard frontend apresenta uma jornada guiada para acompanhar cadastro, obstáculo opcional, planejamento, início, entregas, telemetria e encerramento.
- O dashboard frontend indica se a API Spring está online, verificando ou offline.
- O dashboard frontend bloqueia ações operacionais quando a API Spring está offline.
- O dashboard frontend exibe os status de drones, pedidos e viagens em português.
- O dashboard frontend apresenta descrições ao passar o cursor sobre os botões de ação da consulta operacional.
- O endpoint `POST /internal/demo/reset-and-seed?confirmation=RESET_DEMO_DATA` limpa os dados operacionais atuais e recria um cenário demo determinístico.
- Endpoints internos sob `/internal` exigem autenticação por header `X-Internal-Api-Key`.
- Chamadas internas sem chave ou com chave inválida retornam HTTP `401`.
- O endpoint interno de demo rejeita chamadas sem a confirmação `RESET_DEMO_DATA` com HTTP `400`.
- O dashboard frontend permite recriar um cenário demo com drones, pedidos, obstáculo, avaliação e planejamento otimizado usando o endpoint interno controlado.
- O dashboard frontend exige confirmação antes de recriar o cenário demo e limpar dados operacionais.
- As funcionalidades opcionais listadas não são necessárias para aceitar a primeira versão.
- O projeto possui `README.md` com instruções de execução.
- O projeto possui script local para iniciar banco, backend e dashboard no mesmo fluxo.
- O projeto possui testes unitários cobrindo as regras principais.
- A solução pode ser executada conforme instruções documentadas.
- A evolução operacional calcula tempo estimado de entrega.
- A evolução operacional considera obstáculos no cálculo de rota, distância, bateria e tempo.
- A evolução futura deverá manter fila de entrega e simular tempo de recarga.
- A evolução operacional replaneja pedidos não entregues quando uma viagem é interrompida por retorno antecipado.

## Entregáveis

- Código-fonte da solução.
- `README.md` com instruções de execução.
- Testes unitários.
- API REST mínima para planejamento de viagens.
- Configuração de PostgreSQL via Docker para a evolução operacional.
- Documentação dos requisitos no arquivo `REQUIREMENTS.md`.
