# Prompts técnicos do desenvolvimento

Este documento consolida prompts técnicos que representam o desenvolvimento da aplicação Drone Delivery. Os textos abaixo foram reestruturados em formato imperativo, orientado à execução e aderente às decisões registradas em `DECISIONS.md`, aos requisitos definidos em `REQUIREMENTS.md`, ao contrato em `API.md` e à visão operacional descrita em `README.md`.

Os prompts não são transcrições literais das solicitações originais. Eles são versões técnicas consolidadas, redigidas para descrever como cada etapa deveria ser solicitada a um agente de desenvolvimento ou a uma equipe técnica.

## 1. Formalização do escopo inicial do desafio

**Base documental:** `REQUIREMENTS.md` objetivo principal, requisitos obrigatórios e regras de negócio; `DECISIONS.md` D001-D012.

**Prompt técnico**

Formalize o escopo inicial da aplicação de planejamento de entregas por drones em uma cidade representada por coordenadas 2D. Restrinja o primeiro ciclo ao domínio e aos testes unitários, sem interface gráfica, CLI, persistência, bateria, obstáculos, cálculo de tempo, fila operacional ou relatórios.

Execute as seguintes etapas:

- Modele a cidade como um plano cartesiano 2D.
- Defina a base fixa dos drones na coordenada `(0,0)`.
- Calcule distâncias por métrica euclidiana.
- Exija que toda viagem saia da base e retorne à base.
- Defina pedidos com localização X/Y, peso e prioridade baixa, média ou alta.
- Defina drones com capacidade máxima de peso e alcance máximo de viagem.
- Permita que uma viagem transporte múltiplos pedidos quando peso e alcance forem respeitados.
- Processe pedidos por prioridade, mantendo pedidos de maior prioridade antes de pedidos de menor prioridade.
- Busque reduzir o número de viagens dentro de cada grupo de prioridade.
- Marque pedidos impossíveis como não alocados, preservando o motivo técnico da restrição.
- Documente ambiguidades, decisões e critérios de aceite antes de codificar regras de negócio.

## 2. Estruturação técnica do repositório Java

**Base documental:** `DECISIONS.md` T001-T008; `README.md` tecnologias e estrutura do código.

**Prompt técnico**

Crie a fundação técnica do projeto em `java-technical-challenge`, usando Maven, Java 17 e JUnit 5. Inicialize o repositório Git local, configure a estrutura Maven padrão e isole arquivos gerados ou locais por meio de `.gitignore`.

Execute as seguintes etapas:

- Crie a estrutura `src/main/java` e `src/test/java`.
- Configure `pom.xml` com `maven.compiler.release=17`.
- Adicione JUnit 5 como dependência de teste.
- Crie um teste mínimo de validação do ambiente antes de implementar o domínio.
- Configure `.gitignore` para ignorar `target/`, arquivos de IDE e arquivos temporários.
- Registre no `README.md` os comandos básicos de compilação e teste.
- Registre no `DECISIONS.md` as decisões técnicas de build, linguagem, testes e organização inicial.

## 3. Implementação do núcleo de domínio

**Base documental:** `REQUIREMENTS.md` regras de negócio já definidas; `DECISIONS.md` D001-D010.

**Prompt técnico**

Implemente o domínio puro de planejamento de entregas, sem dependência de framework, banco de dados ou camada HTTP. Garanta que as regras de peso, alcance, prioridade, retorno à base e pedidos não alocados sejam expressas por objetos de domínio testáveis.

Execute as seguintes etapas:

- Crie o modelo de coordenada com cálculo de distância euclidiana.
- Crie o modelo de pedido com identificador, localização, peso e prioridade.
- Crie o modelo de drone com identificador, capacidade máxima e alcance máximo.
- Crie o modelo de viagem com drone associado, lista de pedidos, peso total e distância total.
- Implemente validação de peso máximo por viagem.
- Implemente validação de alcance considerando ida, entregas intermediárias e retorno à base.
- Implemente rejeição de entradas inválidas com exceções de domínio explícitas.
- Implemente saída de planejamento contendo viagens alocadas e pedidos não alocados.
- Mantenha o domínio independente de Spring, JPA, DTOs e serialização JSON.

## 4. Algoritmo inicial de alocação e otimização

**Base documental:** `DECISIONS.md` D006-D010; `REQUIREMENTS.md` requisitos obrigatórios e regras de negócio.

**Prompt técnico**

Implemente um planejador de viagens capaz de alocar pedidos a uma lista de drones, buscando reduzir o número de viagens dentro de cada grupo de prioridade sem violar capacidade, alcance ou retorno à base.

Execute as seguintes etapas:

- Agrupe pedidos por prioridade em ordem alta, média e baixa.
- Avalie drones disponíveis como candidatos para cada grupo de pedidos.
- Monte viagens agregando múltiplos pedidos sempre que a capacidade e o alcance permitirem.
- Preserve a prioridade como restrição de ordenação entre grupos.
- Evite misturar pedidos de prioridade inferior antes de resolver os de prioridade superior.
- Mantenha resultado determinístico para facilitar validação por testes.
- Separe pedidos inviáveis sem interromper o planejamento dos demais pedidos.
- Retorne motivo de não alocação quando o pacote exceder peso, alcance ou ambos.

## 5. Cobertura de testes do domínio inicial

**Base documental:** `REQUIREMENTS.md` critérios de aceite; `DECISIONS.md` T005-T007.

**Prompt técnico**

Crie uma suíte de testes unitários para validar o comportamento do domínio e do planejador de viagens. Cubra cenários de sucesso, rejeição, priorização e pedidos não alocados.

Execute as seguintes etapas:

- Teste o cálculo de distância entre coordenadas.
- Teste a criação de drones e pedidos válidos.
- Teste rejeições para peso, alcance ou entrada inválida.
- Teste viagem com um pedido e retorno à base.
- Teste viagem com múltiplos pedidos dentro dos limites operacionais.
- Teste separação de pedidos não alocados sem falhar o planejamento inteiro.
- Teste ordenação por prioridade.
- Teste uso de múltiplos drones.
- Execute `mvn test` e mantenha a suíte determinística.

## 6. Documentação inicial de requisitos, decisões e execução

**Base documental:** `README.md`, `REQUIREMENTS.md`, `DECISIONS.md`.

**Prompt técnico**

Documente a primeira versão do projeto para que o escopo, as regras de negócio, as decisões técnicas e a execução local fiquem rastreáveis. Separe documentação de uso, documentação de requisitos e registro de decisões.

Execute as seguintes etapas:

- Descreva o objetivo do projeto no `README.md`.
- Liste tecnologias, pré-requisitos e comandos de build/teste.
- Registre regras de negócio e critérios de aceite em `REQUIREMENTS.md`.
- Registre decisões de produto e decisões técnicas em `DECISIONS.md`.
- Declare explicitamente que o primeiro ciclo não inclui funcionalidades opcionais.
- Registre entregáveis esperados e pendências conhecidas.
- Atualize a documentação sempre que uma decisão alterar o comportamento esperado da aplicação.

## 7. Primeira API REST sem persistência

**Base documental:** `DECISIONS.md` D013-D015 e T009-T011; `API.md` contrato de planejamento.

**Prompt técnico**

Evolua a aplicação para expor uma API REST mínima com Spring Boot, mantendo o domínio independente da camada HTTP. Disponibilize um endpoint inicial de planejamento para permitir testes manuais via Postman ou cliente HTTP equivalente.

Execute as seguintes etapas:

- Adicione Spring Boot 3.5.16 e Spring Web ao projeto Maven.
- Crie a classe de bootstrap da aplicação.
- Exponha `POST /api/trip-plans` para receber drones e pedidos em JSON.
- Converta requests HTTP em objetos de domínio sem vazar DTOs para o domínio.
- Retorne viagens planejadas e pedidos não alocados no corpo da resposta.
- Trate corpo inválido, enums inválidos e violações de domínio com HTTP `400`.
- Mantenha pedidos impossíveis dentro do resultado de planejamento, e não como erro HTTP.
- Documente o contrato mínimo no `API.md`.

## 8. Evolução para operação persistida

**Base documental:** `DECISIONS.md` D016-D028 e T012-T019; `REQUIREMENTS.md` evolução operacional aprovada.

**Prompt técnico**

Transforme a calculadora sem estado em uma aplicação operacional persistida. Use PostgreSQL via Docker, Flyway para versionamento de schema, Spring Data JPA para acesso a dados e endpoints REST para cadastro, consulta, planejamento e transições de ciclo de vida.

Execute as seguintes etapas:

- Configure PostgreSQL local com banco `drone_delivery`, usuário `drone`, senha `drone` e porta `5432`.
- Configure Flyway e impeça criação automática de schema pelo Hibernate usando validação explícita.
- Crie migrations para tabelas de drones, pedidos, viagens e itens de rota.
- Crie entidades JPA e repositories para drones, pedidos, viagens e associação entre viagem e pedido.
- Implemente serviços de aplicação para cadastro, consulta e planejamento persistido.
- Faça `POST /api/trip-plans` consumir drones e pedidos salvos, sem exigir corpo obrigatório.
- Persista viagens planejadas com status inicial `PLANNED`.
- Persista a sequência de pedidos da rota.
- Atualize pedidos alocados para `ALLOCATED`.
- Atualize pedidos impossíveis para `UNALLOCATED`.
- Escreva testes de integração com schema temporário de PostgreSQL, JPA e Flyway.

## 9. Ciclo operacional de drones, pedidos e viagens

**Base documental:** `DECISIONS.md` D017-D052; `API.md` seções Drones, Orders, Trip Plans e Trips.

**Prompt técnico**

Implemente o ciclo operacional persistido de drones, pedidos e viagens. Controle status, filtros, consultas por identificador, unicidade, transições válidas e respostas HTTP coerentes com o contrato documentado.

Execute as seguintes etapas:

- Defina status de pedido: `REQUESTED`, `ALLOCATED`, `IN_ROUTE`, `PENDING_REASSIGNMENT`, `DELIVERED`, `NOT_DELIVERED`, `CANCELLED` e `UNALLOCATED`.
- Defina status de drone: `AVAILABLE`, `IN_ROUTE`, `UNAVAILABLE` e `CHARGING`.
- Defina status de viagem: `PLANNED`, `IN_ROUTE`, `RETURNED_EARLY`, `COMPLETED` e `CANCELLED`.
- Implemente `POST /api/drones`, `GET /api/drones`, `GET /api/drones/{id}` e filtros por status.
- Implemente `POST /api/orders`, `GET /api/orders`, `GET /api/orders/{id}` e filtros por status.
- Implemente `GET /api/trips`, `GET /api/trips/{id}` e filtros por status.
- Implemente `POST /api/trips/{id}/start`, validando viagem planejada e drone disponível.
- Implemente `POST /api/trips/{id}/complete`, validando viagem em rota e posições resolvidas.
- Implemente `POST /api/trips/{id}/cancel`, liberando drone e devolvendo pedidos não entregues para replanejamento.
- Garanta HTTP `201` para criação, `400` para entrada inválida, `404` para recurso inexistente e `409` para identificador duplicado.
- Mantenha consultas ordenadas por `id` crescente.

## 10. Rota otimizada, bateria, recarga e estimativas de tempo

**Base documental:** `REQUIREMENTS.md` evolução aprovada; `DECISIONS.md` D053-D065 e T020-T029.

**Prompt técnico**

Amplie o planejamento para considerar rota otimizada, bateria operacional, reserva mínima de retorno, fila de recarga, velocidade média e estimativas de duração. Garanta que o planejamento rejeite rotas inseguras e envie drones sem bateria suficiente para recarga.

Execute as seguintes etapas:

- Ordene a rota automaticamente por horário confirmado, prioridade, maior peso, menor distância da base e identificador.
- Adicione ao drone os campos `batteryLevel`, `batteryConsumptionPerDistanceUnit`, `minimumReturnBattery`, `speed` e `chargingRate`.
- Use a fórmula `distância total * consumo + reserva mínima <= bateria atual` para validar segurança.
- Valide bateria no planejamento e no início da viagem.
- Enfileire drones sem bateria suficiente com status `CHARGING`, data de entrada e motivo.
- Exponha `POST /api/drones/{id}/recharge`, `POST /api/drones/{id}/recharge/complete` e `GET /api/recharge-queue`.
- Exponha consulta interna de bateria em `GET /internal/drones/{id}/battery`.
- Calcule `estimatedDuration` com base na distância total e velocidade do drone.
- Calcule `estimatedDeliveryTime` por posição da rota.
- Calcule `averageDeliveryTime` para cada viagem.
- Atualize testes de domínio, serviço e integração para cobrir bateria, recarga e tempo.

## 11. Filas, obstáculos circulares e retorno antecipado

**Base documental:** `REQUIREMENTS.md` filas, obstáculos e retorno antecipado; `DECISIONS.md` D061-D068 e T030-T035.

**Prompt técnico**

Implemente fila operacional de pedidos, obstáculos circulares no plano 2D e retorno antecipado por segurança de bateria. Faça obstáculos ativos alterarem distância, alcance, bateria e tempo estimado.

Execute as seguintes etapas:

- Adicione `queuedAt` aos pedidos para representar entrada na fila operacional.
- Exponha `GET /api/delivery-queue`.
- Permita `POST /api/trip-plans?optimizeRoute=false` para respeitar a sequência da fila.
- Modele obstáculos como zonas circulares com centro, raio e flag `active`.
- Exponha `POST /api/obstacles`, `GET /api/obstacles` e `DELETE /api/obstacles/{id}`.
- Ajuste cálculo de distância para aplicar desvio em trechos que cruzam obstáculo ativo.
- Recalcule alcance, bateria e tempo usando distância ajustada.
- Implemente retorno antecipado quando a bateria restante não comportar rota completa e reserva mínima.
- Marque a viagem como `RETURNED_EARLY`.
- Mantenha entregas já confirmadas como `DELIVERED`.
- Marque pedidos restantes como `PENDING_REASSIGNMENT`.
- Envie o drone para `CHARGING`.

## 12. Telemetria e confirmação de entrega por posição

**Base documental:** `DECISIONS.md` D069-D073; `API.md` seções de telemetria e entrega por rota.

**Prompt técnico**

Implemente telemetria de bateria e confirmação de entrega por posição da rota. Use a confirmação do cliente como fonte de verdade para marcar entregas, evitando inferir sucesso apenas pelo avanço da viagem.

Execute as seguintes etapas:

- Exponha `POST /api/trips/{id}/telemetry` para receber leitura de bateria.
- Atualize a bateria atual do drone associado à viagem.
- Persista cada leitura na tabela de histórico de telemetria.
- Exponha `GET /api/trips/{id}/telemetry`.
- Acione retorno antecipado quando uma telemetria tornar a rota insegura.
- Exponha `POST /api/trips/{id}/route/{routePosition}/deliver`.
- Exija `confirmationCode` no payload de entrega.
- Valide a posição da rota, o status da viagem e o código informado.
- Persista `deliveredAt` na posição correta da rota.
- Marque o pedido confirmado como `DELIVERED`.
- Mantenha a conclusão da viagem dependente de todas as posições estarem resolvidas.

## 13. Avaliações, relatório e documentação de contrato

**Base documental:** `DECISIONS.md` D074, T019 e T036; `API.md` Reviews e Productivity Reports.

**Prompt técnico**

Adicione recursos de avaliação e relatório operacional, mantendo o contrato HTTP documentado em artefato próprio. Garanta que o feedback do cliente e os indicadores mensais fiquem persistidos e consultáveis.

Execute as seguintes etapas:

- Implemente avaliações com estrelas, título, feedback e data de registro.
- Exponha `POST /api/reviews`, `GET /api/reviews` e `GET /api/reviews/{id}`.
- Calcule média de avaliações para uso no dashboard.
- Implemente relatório mensal de produtividade com agregações de viagens, entregas, cancelamentos, retornos e drones.
- Exponha endpoint mensal com parâmetro `month=YYYY-MM`.
- Exponha histórico de relatórios quando aplicável.
- Atualize `API.md` com endpoints, payloads, respostas e erros.
- Atualize `README.md` com resumo operacional do recurso.

## 14. Reorganização arquitetural por responsabilidades

**Base documental:** `README.md` estrutura do código; `DECISIONS.md` T037.

**Prompt técnico**

Reorganize o código principal em pacotes por responsabilidade, preservando isolamento entre domínio, serviços de aplicação, persistência, controllers e tratamento de exceções.

Execute as seguintes etapas:

- Mantenha entidades e repositories em `persistence`.
- Mantenha regras puras e cálculos em `domain`.
- Mantenha orquestrações de casos de uso em `service`.
- Mantenha endpoints HTTP em `controller`.
- Mantenha exceções específicas em `exception`.
- Evite dependências de framework dentro do domínio.
- Garanta que controllers façam tradução de DTOs e serviços executem regras de aplicação.
- Rode a suíte de testes após a reorganização.

## 15. Dashboard administrativo em React, TypeScript e Vite

**Base documental:** `README.md` dashboard atual; `DECISIONS.md` T038-T054.

**Prompt técnico**

Crie um dashboard administrativo em React, TypeScript e Vite para operar a API Spring. A interface deve consumir endpoints reais, exibir indicadores operacionais e permitir conduzir o ciclo completo de cadastro, planejamento, execução, telemetria e encerramento.

Execute as seguintes etapas:

- Crie o frontend no diretório `frontend`.
- Configure Vite com proxy local para `/api` e `/internal`.
- Exiba visão geral com indicadores de drones, pedidos, viagens, recarga, tempo médio e avaliações.
- Implemente tabelas de drones, pedidos e viagens com busca e filtro por status.
- Implemente cadastro de drones e pedidos.
- Implemente acionamento de planejamento persistido, com opção de rota otimizada.
- Implemente ações de drone: marcar indisponível, marcar disponível, enviar para recarga e concluir recarga.
- Implemente ações de viagem: iniciar, registrar telemetria, concluir e cancelar.
- Implemente gestão de obstáculos.
- Implemente visão dedicada de filas de entrega, reatribuição e recarga.
- Implemente visão detalhada de viagem com rota, progresso por entrega e histórico de telemetria.
- Exiba disponibilidade da API Spring e bloqueie ações quando o backend estiver offline.
- Adicione script integrado para subir banco, backend e frontend.

## 16. Mapa 2D operacional

**Base documental:** `DECISIONS.md` T046, T053 e T054; `README.md` descrição do dashboard.

**Prompt técnico**

Implemente um mapa 2D operacional para visualizar base, pedidos, obstáculos, rotas e posição do drone em movimento. Permita foco na viagem selecionada ou comparação entre todas as viagens planejadas.

Execute as seguintes etapas:

- Renderize a base na coordenada `(0,0)`.
- Renderize pedidos conforme suas coordenadas persistidas.
- Renderize obstáculos circulares ativos e inativos.
- Renderize segmentos de rota com direção visual.
- Numere pontos de entrega conforme a posição na rota.
- Atribua cor por viagem.
- Permita alternar entre viagem selecionada e todas as viagens.
- Exiba marcador do drone a partir do estado de simulação.
- Atualize a posição conforme a simulação avançar.
- Preserve legibilidade visual em rotas com múltiplos pedidos.

## 17. Experiência Cliente e autenticação

**Base documental:** `REQUIREMENTS.md` regras de cliente; `DECISIONS.md` T055-T060 e T067; `API.md` Client Authentication e Client Orders.

**Prompt técnico**

Implemente experiência separada para cliente, com cadastro, login, sessão autenticada, pedidos vinculados à conta, acompanhamento de pedidos e confirmação segura de recebimento.

Execute as seguintes etapas:

- Crie entidade de usuário cliente.
- Armazene senha com hash seguro.
- Implemente cadastro, login e consulta do cliente autenticado.
- Emita token assinado para a sessão do cliente.
- Exija autenticação para criar pedidos pela área Cliente.
- Vincule pedidos criados ao usuário autenticado.
- Implemente aba `Meus pedidos` listando apenas pedidos da conta.
- Permita alternar entre múltiplos pedidos.
- Permita acompanhar pedido por ID ou código quando aplicável.
- Use o identificador de rastreio como código único de rastreio e confirmação.
- Exiba o código uma única vez, indicando sua dupla finalidade.

## 18. Notificação de aproximação, disponibilidade e confirmação segura

**Base documental:** `REQUIREMENTS.md` regras durante viagem; `DECISIONS.md` D071, D075, D078 e T064-T066.

**Prompt técnico**

Implemente o fluxo de entrega com notificação de aproximação, confirmação de disponibilidade, parada no endereço e janela limitada para digitação do código de entrega. Garanta que o pacote não seja marcado como entregue sem interação explícita do cliente.

Execute as seguintes etapas:

- Avance automaticamente viagens planejadas ou em rota por simulação de tempo.
- Persista posição atual, distância percorrida, progresso e próximo pedido.
- Detecte aproximação do drone ao destino.
- Exiba aviso central ao cliente quando o drone estiver chegando.
- Emita som de notificação para chamar atenção do cliente.
- Exija resposta de disponibilidade do cliente.
- Registre horário da notificação, confirmação e prazo de resposta.
- Retorne o drone à base se o cliente não confirmar disponibilidade.
- Marque o pacote como `NOT_DELIVERED` quando a disponibilidade expirar.
- Libere a digitação do código somente quando o drone alcançar o endereço.
- Defina janela de 1 minuto para informar o código.
- Rejeite código ausente, inválido ou expirado.
- Marque a entrega como confirmada apenas quando o código correto for informado dentro do prazo.
- Registre motivo em português quando a entrega não for concluída.

## 19. Tratamento administrativo de pedidos não alocados

**Base documental:** `REQUIREMENTS.md` pedidos não alocados; `DECISIONS.md` D076-D077, D080 e T061.

**Prompt técnico**

Implemente tratamento administrativo para pedidos que não puderam ser alocados. Exiba mensagens em português para administrador e cliente, registre justificativas e permita cancelar ou reenviar o pacote ao planejamento.

Execute as seguintes etapas:

- Persista `statusReason` no pedido.
- Traduza motivos técnicos de não alocação para português.
- Diferencie restrições por peso, alcance, combinação de peso/alcance, bateria ou ausência de drone disponível.
- Exiba alerta administrativo para pedidos `UNALLOCATED`.
- Exiba mensagem para o cliente explicando a situação do pedido.
- Implemente cancelamento administrativo com justificativa obrigatória.
- Implemente reenvio de pedido não alocado para planejamento.
- Atualize o status conforme a ação executada.
- Mantenha rastreabilidade do motivo exibido ao cliente.

## 20. Exclusão segura de drones

**Base documental:** `REQUIREMENTS.md` regras de drones; `DECISIONS.md` D079.

**Prompt técnico**

Adicione exclusão administrativa de drones, impedindo remoção de drones em rota ou com vínculo histórico incompatível. Mantenha integridade operacional da frota e mensagens claras para o administrador.

Execute as seguintes etapas:

- Exponha endpoint de exclusão de drone.
- Bloqueie exclusão de drone em status `IN_ROUTE`.
- Bloqueie exclusão quando houver viagem vinculada que impeça remoção segura.
- Adicione ação de exclusão na tabela administrativa de drones.
- Atualize a lista e os indicadores após exclusão.
- Exiba sucesso ou erro de forma objetiva.
- Preserve histórico operacional de viagens existentes.

## 21. Relatório mensal com alternância de competência

**Base documental:** `DECISIONS.md` T062; `API.md` Productivity Reports.

**Prompt técnico**

Permita alternar a competência do relatório mensal diretamente no dashboard. Use o parâmetro `month=YYYY-MM` para consultar meses específicos sem criar endpoints redundantes.

Execute as seguintes etapas:

- Adicione controle visual para avançar e retroceder mês.
- Gere o valor `YYYY-MM` a partir da competência selecionada.
- Consulte o relatório mensal com o parâmetro `month`.
- Atualize indicadores, funil e produtividade por drone conforme o mês selecionado.
- Mantenha estado visual consistente quando não houver dados na competência.
- Documente o parâmetro no contrato HTTP.

## 22. Swagger/OpenAPI completo do backend

**Base documental:** `DECISIONS.md` T068; `README.md` documentação Swagger/OpenAPI; `API.md`.

**Prompt técnico**

Implemente documentação Swagger/OpenAPI completa para o backend, expondo visualização interativa de todos os endpoints públicos, administrativos, internos e de cliente.

Execute as seguintes etapas:

- Adicione Springdoc OpenAPI compatível com Spring Boot.
- Configure metadados globais de título, versão e descrição.
- Configure grupos para backend completo, API pública e API interna.
- Configure esquema `clientBearerAuth` para endpoints da área Cliente.
- Configure esquema `internalApiKey` para endpoints sob `/internal`.
- Anote controllers com tags e descrições coerentes.
- Anote requests e responses com schemas e exemplos.
- Garanta que todos os endpoints apareçam em `/swagger-ui.html`.
- Garanta que `/v3/api-docs` e `/v3/api-docs.yaml` respondam corretamente.
- Valide a documentação executando a aplicação localmente.

## 23. Padronização de unidades de medida no padrão brasileiro

**Base documental:** `REQUIREMENTS.md` unidades de medida; `DECISIONS.md` T022 e T069; `README.md` unidades; `API.md` Measurement Units.

**Prompt técnico**

Padronize explicitamente todas as unidades de medida da aplicação conforme o padrão métrico utilizado no Brasil. Aplique as unidades no backend, frontend, documentação, OpenAPI, testes e migrações de dados.

Execute as seguintes etapas:

- Use quilogramas (`kg`) para peso, capacidade e carga.
- Use quilômetros (`km`) para coordenadas, distância, alcance e raio de obstáculos.
- Use quilômetros por hora (`km/h`) para velocidade média dos drones.
- Use percentual (`%`) para bateria e reserva mínima.
- Use percentual por quilômetro (`%/km`) para consumo de bateria.
- Use percentual por minuto (`%/min`) para taxa de recarga.
- Use minutos (`min`) para duração e estimativas.
- Ajuste a fórmula de tempo para `(totalDistance / speed) * 60`.
- Crie utilitário de domínio para conversão entre distância, velocidade e tempo.
- Atualize defaults de velocidade para `60.0 km/h`.
- Crie migration para converter velocidades antigas armazenadas.
- Atualize labels, tabelas, formulários e cards do frontend.
- Atualize Swagger, `README.md`, `REQUIREMENTS.md`, `API.md` e `DECISIONS.md`.
- Atualize testes impactados por mudança de unidade.

## 24. Escala adaptativa do mapa 2D

**Base documental:** `README.md` mapa 2D; `DECISIONS.md` T046, T053 e T054.

**Prompt técnico**

Faça a escala do mapa 2D adaptar-se automaticamente à distância das entregas visíveis, especialmente em rotas curtas. Garanta que o movimento e o progresso do drone permaneçam perceptíveis mesmo quando a entrega ocorrer em uma distância pequena.

Execute as seguintes etapas:

- Calcule o viewport do mapa com base nas viagens exibidas.
- Inclua base, destinos da rota, posição atual do drone e obstáculos próximos no enquadramento.
- Ignore pedidos e obstáculos distantes que não participem da visualização atual.
- Defina margem proporcional ao tamanho real da rota.
- Use tamanho mínimo reduzido para permitir zoom em entregas curtas.
- Filtre marcadores fora da área visível quando eles comprometerem a escala.
- Exiba indicador de escala em quilômetros.
- Formate distâncias pequenas com precisão suficiente para evitar exibição como `0 km`.
- Execute build do frontend para validar TypeScript e empacotamento.

## 25. Integração GitHub por SSH e publicação

**Base documental:** histórico operacional do projeto; comandos de publicação executados.

**Prompt técnico**

Verifique a integração SSH com GitHub, configure o repositório remoto e publique as alterações consolidadas na branch principal. Preserve rastreabilidade por commits técnicos e valide o estado local após o envio.

Execute as seguintes etapas:

- Verifique a existência de chave SSH local para GitHub.
- Configure `origin` com URL SSH do repositório remoto.
- Normalize a branch principal como `main`.
- Execute testes e builds relevantes antes do commit.
- Execute `git diff --check`.
- Crie commit com mensagem técnica e abrangente.
- Execute `git push -u origin main`.
- Confirme que `main` rastreia `origin/main`.
- Confirme que o diretório de trabalho ficou limpo.

## 26. Otimização de remessas por prazo e drone imediato

**Base documental:** `REQUIREMENTS.md` regras de planejamento operacional; `DECISIONS.md` D081-D085 e T070-T073.

**Prompt técnico**

Otimize o planejamento de remessas para priorizar entrega em tempo hábil e distribuir excedentes imediatamente entre drones disponíveis. Garanta que pedidos de um mesmo cliente possam ser separados em drones distintos quando não couberem na mesma viagem.

Execute as seguintes etapas:

- Use o horário confirmado de entrega como primeiro critério de ordenação dos pedidos.
- Use prioridade, peso, distância e identificador como critérios determinísticos de desempate.
- Ordene a fila operacional por horário confirmado, prioridade, entrada na fila e identificador persistido.
- Reserve cada drone disponível para no máximo uma viagem planejada por rodada.
- Calcule a janela ideal de saída pela menor diferença entre horário confirmado de entrega e tempo estimado até cada pacote da rota.
- Permita adicionar pedidos a viagens planejadas antes da janela ideal de saída quando peso, alcance, bateria e obstáculos continuarem válidos.
- Exclua de novas rodadas os drones em rota ou com viagem planejada cuja janela ideal de saída já esteja aberta.
- Ao criar nova viagem, selecione o menor drone capaz para preservar drones maiores para pacotes mais pesados.
- Tente inserir cada pedido em uma viagem existente somente se peso, alcance, bateria e obstáculos continuarem válidos.
- Aloque imediatamente em outro drone disponível e capaz quando o pedido não couber na viagem existente.
- Marque o pedido como não alocado quando ele exigir outro drone imediato e não houver drone livre na rodada.
- Traduza o novo motivo de não alocação para português na resposta operacional e na interface.
- Atualize testes de domínio, planejamento persistido e fila operacional.
- Atualize documentação de requisitos, decisões, contrato HTTP e README.

## 27. Validação final integrada

**Base documental:** `README.md` como executar, dashboard atual e documentação Swagger/OpenAPI.

**Prompt técnico**

Execute validação final integrada da aplicação antes da entrega. Garanta que backend, banco, frontend, Swagger e testes automatizados estejam consistentes com o comportamento documentado.

Execute as seguintes etapas:

- Suba PostgreSQL com Docker Compose.
- Execute testes automatizados do backend com Maven.
- Execute build do frontend.
- Inicie backend e frontend com script integrado quando aplicável.
- Verifique o backend em `http://localhost:8080`.
- Verifique o dashboard em `http://127.0.0.1:5173`.
- Verifique Swagger UI em `http://localhost:8080/swagger-ui.html`.
- Verifique especificação OpenAPI em `/v3/api-docs`.
- Confirme que as principais jornadas funcionais estão acessíveis pela interface.
- Registre limitações conhecidas ou validações não executadas.
