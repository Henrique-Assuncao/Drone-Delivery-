# Registro de Decisões

Este documento mantém o histórico das decisões tomadas durante o desenvolvimento do desafio técnico.

## Convenções

- **Decidida**: decisão já adotada para o escopo atual.
- **Pendente**: decisão ainda não tomada.
- **Técnica**: decisão relacionada à estrutura do projeto, ferramenta ou ambiente.
- **Produto**: decisão relacionada às regras do desafio.

## Decisões de produto

| ID | Decisão | Status | Impacto |
| --- | --- | --- | --- |
| D001 | A cidade será representada por coordenadas 2D. | Decidida | Todos os pedidos terão localização baseada em valores X e Y. |
| D002 | A distância usada será euclidiana. | Decidida | O cálculo de distância será direto entre dois pontos do plano 2D. |
| D003 | A base dos drones será fixa em `(0,0)`. | Decidida | Todas as viagens terão um ponto inicial comum. |
| D004 | Toda viagem sairá da base e retornará para a base. | Decidida | O drone não manterá posição final em outro ponto após uma entrega. |
| D005 | O alcance da viagem considerará ida e volta. | Decidida | Uma viagem só será válida se a distância total couber no alcance máximo do drone. |
| D006 | Uma viagem poderá transportar vários pedidos. | Decidida | O agrupamento de pedidos será permitido quando peso e distância forem respeitados. |
| D007 | O domínio aceitará uma lista de drones. | Decidida | A solução não ficará limitada a um único drone. |
| D008 | Os pedidos serão processados por prioridade: alta, média e baixa. | Decidida | Pedidos de prioridade maior serão considerados antes dos demais. |
| D009 | A redução do número de viagens será buscada dentro de cada grupo de prioridade. | Decidida | A otimização de viagens não deverá passar pedidos de prioridade menor na frente dos de prioridade maior. |
| D010 | Pacotes impossíveis de transportar serão marcados como não alocados com motivo. | Decidida | A simulação poderá continuar mesmo quando algum pedido não puder ser entregue. |
| D011 | A primeira versão terá somente domínio e testes unitários. | Decidida | Não haverá CLI, API REST ou interface gráfica no primeiro ciclo. |
| D012 | Funcionalidades opcionais não serão implementadas no primeiro ciclo. | Decidida | Bateria, obstáculos, cálculo de tempo, fila, estados, relatório, recarga e status ficam fora do escopo inicial. |
| D013 | A API REST será implementada após o ciclo inicial para permitir testes via Postman. | Decidida | O domínio continua independente de HTTP, e a API atua como adaptador de entrada e saída. |
| D014 | A primeira API terá apenas planejamento de viagens, sem CRUD. | Decidida | A aplicação demonstra o desafio sem exigir banco de dados ou persistência. |
| D015 | Entrada inválida na API retornará HTTP `400`; pedido impossível continuará no resultado como não alocado. | Decidida | Erros de formato ou validação são separados de restrições de alocação do domínio. |
| D016 | A aplicação evoluirá de calculadora sem estado para controle operacional persistido. | Decidida | Drones, pedidos e viagens passarão a ter ciclo de vida acompanhado pelo sistema. |
| D017 | Pedidos terão status `REQUESTED`, `ALLOCATED`, `IN_ROUTE`, `PENDING_REASSIGNMENT`, `DELIVERED`, `CANCELLED` e `UNALLOCATED`. | Decidida | Será possível acompanhar pedidos solicitados, em planejamento, em rota, aguardando reatribuição, concluídos, cancelados e não alocados. |
| D018 | Drones terão status `AVAILABLE`, `IN_ROUTE` e `UNAVAILABLE`. | Decidida | Será possível diferenciar drones disponíveis, em rota e indisponíveis por outro motivo. |
| D019 | Um drone só deixará de estar disponível quando a viagem for iniciada. | Decidida | Criar um plano não bloqueia o drone; o bloqueio operacional ocorre no início efetivo da rota. |
| D020 | Viagens terão status `PLANNED`, `IN_ROUTE`, `RETURNED_EARLY`, `COMPLETED` e `CANCELLED`. | Decidida | Será possível acompanhar viagens planejadas, em rota, retornadas antecipadamente, concluídas e canceladas. |
| D021 | Uma viagem criada pelo planejamento começará como `PLANNED`. | Decidida | O planejamento registra intenção operacional sem iniciar a rota imediatamente. |
| D022 | Ao iniciar uma viagem, viagem, drone e pedidos passam para `IN_ROUTE`. | Decidida | O início da rota bloqueia o drone e move os pedidos para acompanhamento de entrega. |
| D023 | Ao concluir uma viagem, a viagem passa para `COMPLETED`, o drone volta para `AVAILABLE` e as posições da rota precisam estar resolvidas. | Decidida | A conclusão libera o drone sem marcar pedidos como entregues sem confirmação do cliente. |
| D024 | Ao cancelar uma viagem não concluída, a viagem passa para `CANCELLED`, o drone volta para `AVAILABLE` e os pedidos não entregues voltam para `REQUESTED`. | Decidida | O cancelamento libera recursos e permite replanejar pedidos ainda não entregues. |
| D025 | A API operacional terá endpoints mínimos para cadastro e consulta de drones. | Decidida | A frota poderá ser registrada e consultada antes do planejamento. |
| D026 | A API operacional terá endpoints mínimos para cadastro e consulta de pedidos. | Decidida | A demanda poderá ser registrada e filtrada por status. |
| D027 | Na evolução operacional, `POST /api/trip-plans` usará dados salvos. | Decidida | O planejamento deixa de depender de drones e pedidos enviados no corpo da requisição. |
| D028 | A API operacional terá endpoints para listar, iniciar, concluir e cancelar viagens. | Decidida | O ciclo de vida das viagens poderá ser acompanhado e atualizado. |
| D029 | O endpoint `POST /api/drones` criará drones com status inicial `AVAILABLE`. | Decidida | Novos drones entram disponíveis para planejamento. |
| D030 | O endpoint `POST /api/orders` criará pedidos com status inicial `REQUESTED`. | Decidida | Novos pedidos entram como demanda pendente. |
| D031 | O planejamento operacional considerará apenas drones `AVAILABLE` e pedidos `REQUESTED` ou `PENDING_REASSIGNMENT`. | Decidida | O sistema evita alocar drones ocupados e permite transferir pedidos pendentes após retorno antecipado. |
| D032 | `POST /api/drones` retornará HTTP `201 Created` em cadastro válido. | Decidida | A API comunica criação de recurso de forma explícita. |
| D033 | O identificador de drone será único. | Decidida | A frota poderá ser referenciada de forma não ambígua. |
| D034 | Cadastro de drone com identificador duplicado retornará HTTP `409 Conflict`. | Decidida | Conflitos de recurso ficam separados de erros simples de validação. |
| D035 | As consultas de drones retornarão resultados ordenados por `id` crescente. | Decidida | As respostas ficam determinísticas e acompanham a ordem de cadastro. |
| D036 | `POST /api/orders` criará pedidos persistidos com status inicial `REQUESTED`. | Decidida | A demanda passa a ser registrada no banco antes do planejamento operacional. |
| D037 | As consultas de pedidos retornarão resultados ordenados por `id` crescente. | Decidida | As respostas ficam determinísticas e acompanham a ordem de cadastro. |
| D038 | Filtros públicos baseados em enum retornarão HTTP `400` quando receberem valor inválido. | Decidida | Entradas inválidas em query string ficam consistentes com o tratamento de erros da API. |
| D039 | `POST /api/trip-plans` usará drones e pedidos persistidos, sem exigir corpo na requisição. | Decidida | O planejamento operacional passa a ser acionado a partir do estado salvo no banco. |
| D040 | O planejamento persistido altera pedidos alocados para `ALLOCATED` e pedidos impossíveis para `UNALLOCATED`. | Decidida | O status dos pedidos passa a refletir o resultado do planejamento. |
| D041 | A resposta operacional do planejamento usa IDs persistidos de viagens, drones e pedidos. | Decidida | A API passa a referenciar recursos salvos de forma mais estável que identificadores textuais. |
| D042 | `GET /api/trips` listará viagens persistidas em ordem crescente de `id`. | Decidida | As viagens planejadas podem ser consultadas de forma determinística antes das transições operacionais. |
| D043 | `POST /api/trips/{id}/start` iniciará somente viagens `PLANNED` com drone `AVAILABLE`. | Decidida | Evita iniciar viagens inválidas ou duas rotas simultâneas para drone já em rota. |
| D044 | Viagem inexistente em transição operacional retornará HTTP `404`. | Decidida | Recursos ausentes ficam separados de erros de validação. |
| D045 | `POST /api/trips/{id}/complete` concluirá somente viagens `IN_ROUTE`. | Decidida | A conclusão passa a liberar o drone e finalizar os pedidos da rota. |
| D046 | `POST /api/trips/{id}/cancel` cancelará somente viagens `PLANNED` ou `IN_ROUTE`. | Decidida | O cancelamento libera o drone e devolve pedidos não entregues para replanejamento. |
| D047 | A API operacional terá consultas por `id` para drones, pedidos e viagens. | Decidida | Operadores podem consultar rapidamente o estado atual de um recurso específico. |
| D048 | O identificador de pedido será único. | Decidida | Evita ambiguidade operacional ao consultar e acompanhar pedidos cadastrados. |
| D049 | Motivos de não alocação serão detalhados para peso, alcance ou peso e alcance. | Decidida | O operador passa a entender melhor por que um pedido não entrou no planejamento. |
| D050 | A disponibilidade manual do drone será controlada por endpoints explícitos de ativação e inativação. | Decidida | Operadores podem retirar drones disponíveis do planejamento e devolvê-los quando estiverem novamente operacionais, sem alterar drones em rota. |
| D051 | `GET /api/drones` aceitará filtro opcional por status. | Decidida | Operadores podem consultar drones por `AVAILABLE`, `IN_ROUTE` ou `UNAVAILABLE` sem usar endpoints adicionais para cada status. |
| D052 | `GET /api/trips` aceitará filtro opcional por status. | Decidida | Operadores podem consultar viagens por `PLANNED`, `IN_ROUTE`, `RETURNED_EARLY`, `COMPLETED` ou `CANCELLED` sem carregar todo o histórico operacional. |
| D053 | A rota dentro de cada viagem será otimizada. | Decidida | A sequência de entrega passa a buscar menor distância antes de validar alcance e persistir a rota. |
| D054 | Drones terão controle de bateria operacional. | Decidida | A elegibilidade de uma viagem passará a considerar bateria, além de peso, alcance e status. |
| D055 | O sistema terá endpoint interno para consulta de bateria. | Decidida | Quem acompanha a entrega poderá verificar a bateria do drone sem alterar manualmente o estado operacional. |
| D056 | Viagens terão estimativa de tempo de entrega. | Decidida | Respostas futuras poderão informar duração e previsão de conclusão calculadas por distância ajustada, velocidade e paradas. |
| D057 | Pedidos pendentes serão organizados em fila operacional. | Decidida | O planejamento e o replanejamento passam a usar uma fonte ordenada de demanda. |
| D058 | Drones sem bateria suficiente para iniciar viagem entrarão em fila de recarga. | Decidida | O planejamento evita viagens inseguras e separa drones aguardando recarga dos drones indisponíveis por outros motivos. |
| D059 | Drone em viagem deverá retornar quando atingir bateria mínima de retorno seguro. | Decidida | Pedidos restantes deixam a viagem atual e voltam a ser replanejáveis. |
| D060 | Pedidos não entregues por retorno antecipado receberão novo status `PENDING_REASSIGNMENT`. | Decidida | O sistema diferencia pedido novo de pedido aguardando transferência após interrupção operacional. |
| D061 | Obstáculos serão modelados como restrições circulares de rota no plano 2D. | Decidida | O cálculo de distância, tempo e viabilidade passa a considerar zonas que exigem desvio. |
| D062 | A otimização de rota deverá considerar fila, bateria e obstáculos. | Decidida | A rota automática precisa respeitar critérios operacionais e continuar validando segurança antes da persistência. |
| D063 | Planejamento e início de viagem validarão bateria mínima para rota completa e retorno seguro. | Decidida | Viagens inseguras deixam de ser planejadas ou iniciadas, mesmo quando peso, alcance e status permitiriam a rota. |
| D064 | Drones sem bateria suficiente entrarão em fila de recarga com status `CHARGING`. | Decidida | Drones em recarga deixam de ser considerados em novos planejamentos até a conclusão da recarga. |
| D065 | Viagens expõem duração estimada calculada pela rota e velocidade do drone. | Decidida | Operadores passam a enxergar a duração prevista sem depender de cálculo manual fora da API. |
| D066 | Pedidos `REQUESTED` e `PENDING_REASSIGNMENT` formam a fila operacional de entrega. | Decidida | O planejamento pode respeitar a ordem de entrada dos pedidos novos e dos pedidos aguardando transferência. |
| D067 | Obstáculos ativos ajustam distância, alcance, bateria e tempo estimado. | Decidida | O planejamento passa a rejeitar ou alongar rotas quando um trecho cruzaria uma zona circular ativa. |
| D068 | Retorno antecipado marca a viagem como `RETURNED_EARLY`, pedidos restantes como `PENDING_REASSIGNMENT` e o drone como `CHARGING`. | Decidida | O sistema preserva entregas já realizadas, separa pedidos transferíveis e tira o drone de novos planejamentos até recarga. |
| D069 | Viagens em rota aceitam telemetria de bateria pelo endpoint `POST /api/trips/{id}/telemetry`. | Decidida | O retorno antecipado pode ser acionado por evento operacional antes da conclusão manual da viagem. |
| D070 | O histórico de telemetria de uma viagem será consultável por `GET /api/trips/{id}/telemetry`. | Decidida | Operadores podem auditar as leituras recebidas durante a viagem. |
| D071 | Viagens em rota confirmam entregas por posição com `POST /api/trips/{id}/route/{routePosition}/deliver` e código do cliente. | Decidida | O retorno antecipado passa a preservar somente entregas confirmadas pelo destinatário, sem inferir pedidos entregues pela bateria. |
| D072 | A ordem automática de entregas usará prioridade, maior peso e menor distância da base. | Decidida | A análise deixa de depender apenas de fila ou menor rota geométrica e passa a refletir critérios operacionais dos pacotes. |
| D073 | As respostas de viagem retornarão tempo estimado por pacote e tempo médio até entrega. | Decidida | O cliente passa a receber uma previsão por posição da rota e uma média da viagem. |
| D074 | Clientes poderão avaliar o serviço com estrelas, título e feedback. | Decidida | A aplicação passa a registrar percepção simples do serviço sem depender de pedidos ou viagens neste ciclo. |
| D075 | Viagens poderão ser avançadas por simulação automática de tempo. | Decidida | O sistema passa a mover drones, consumir bateria, parar em entregas alcançadas aguardando confirmação e encerrar ou retornar viagens conforme a rota. |
| D076 | Pedidos não alocados terão mensagem de status persistida e visível para admin e cliente. | Decidida | O cliente entende por que o pacote ficou parado e o admin recebe orientação para cancelar ou reenviar ao planejamento. |
| D077 | O admin poderá cancelar pedido não alocado com justificativa obrigatória ou reenviá-lo para planejamento. | Decidida | A operação passa a ter uma decisão explícita para pacotes que não puderam ser alocados automaticamente. |
| D078 | A área Cliente exibirá aviso interativo com som quando o drone estiver chegando ao destino. | Decidida | O destinatário recebe um alerta mais difícil de ignorar antes da confirmação de recebimento. |
| D079 | O admin poderá excluir drones sem viagem vinculada e fora de rota. | Decidida | A frota pode ser corrigida no cadastro sem remover histórico operacional nem drone em operação. |
| D080 | Motivos de não alocação exibidos na API operacional e no cliente serão em português. | Decidida | O cliente recebe uma explicação apresentável, sem mensagens técnicas internas em inglês. |

## Decisões técnicas

| ID | Decisão | Status | Impacto |
| --- | --- | --- | --- |
| T001 | O projeto ficará na subpasta `java-technical-challenge`. | Decidida | O repositório do desafio fica isolado dentro do diretório de projetos. |
| T002 | O repositório será local e inicializado com Git. | Decidida | O histórico poderá ser controlado localmente, sem commits automáticos. |
| T003 | O build será configurado com Maven. | Decidida | Os comandos de build e teste usarão o fluxo padrão do Maven. |
| T004 | O projeto será compilado com Java `17`. | Decidida | O Maven usa `maven.compiler.release` definido como `17`. |
| T005 | Os testes unitários usarão JUnit 5. | Decidida | A base inicial de testes usa `junit-jupiter`. |
| T006 | A estrutura inicial seguirá o padrão Maven. | Decidida | Código e testes ficarão em `src/main/java` e `src/test/java`. |
| T007 | O primeiro teste será apenas de validação do ambiente. | Decidida | Nenhuma regra de negócio será implementada ou testada antes do próximo passo. |
| T008 | Arquivos gerados e configurações locais serão ignorados pelo Git. | Decidida | `target/`, arquivos de IDE e arquivos temporários ficam fora do controle de versão. |
| T009 | A API REST usará Spring Boot 3.5.16 com Spring Web. | Decidida | O projeto passa a ter aplicação executável por Maven e servidor HTTP embarcado. |
| T010 | A execução local usará `mvn spring-boot:run`. | Decidida | A aplicação poderá ser testada no Postman em `http://localhost:8080`. |
| T011 | O endpoint inicial será `POST /api/trip-plans`. | Decidida | A API recebe drones e pedidos em JSON e retorna o plano calculado. |
| T012 | A persistência da evolução operacional usará PostgreSQL via Docker. | Decidida | Os dados deverão sobreviver ao ciclo da aplicação e o ambiente ficará mais próximo de uso real. |
| T013 | O PostgreSQL local usará banco `drone_delivery`, usuário `drone`, senha `drone` e porta `5432`. | Decidida | O ambiente local fica simples e reproduzível por Docker Compose. |
| T014 | Migrações de banco serão controladas com Flyway. | Decidida | A evolução do esquema do banco será versionada em arquivos SQL. |
| T015 | O Hibernate não criará tabelas automaticamente. | Decidida | `spring.jpa.hibernate.ddl-auto=validate` evita alterações implícitas no schema. |
| T016 | Viagens planejadas serão persistidas nas tabelas `trips` e `trip_orders`. | Decidida | O cabeçalho da viagem e a sequência da rota ficam salvos separadamente. |
| T017 | A consulta JPA de viagens carregará drone e pedidos da rota junto com a viagem. | Decidida | Evita acesso lazy fora da transação com `spring.jpa.open-in-view=false`. |
| T018 | Testes de integração usarão PostgreSQL local com schema temporário. | Decidida | A suite valida Spring Boot, JPA e Flyway sem reutilizar nem limpar tabelas do schema público de desenvolvimento. |
| T019 | O contrato HTTP consolidado ficará em `API.md`. | Decidida | Endpoints, payloads, filtros, respostas e erros ficam documentados em um artefato dedicado, enquanto o README permanece focado em execucao e exemplos. |
| T020 | A otimização de rota usará ordenação determinística por prioridade, peso, distância da base e identificador. | Decidida | A rota passa a refletir critérios operacionais dos pacotes e mantém resultado previsível para o cliente. |
| T021 | Bateria será modelada inicialmente em percentual e consumo por quilômetro. | Decidida | O modelo fica simples para consulta e validação de segurança, usando `%` e `%/km` sem exigir unidade física real de energia neste ciclo. |
| T022 | Tempo estimado usará `(distância total / velocidade) * 60`. | Decidida | A estimativa combina distância em km e velocidade em km/h para retornar duração em minutos, sem coluna nova no banco. |
| T023 | Obstáculos começarão como zonas circulares em 2D. | Decidida | O primeiro modelo de desvio fica simples de validar e pode evoluir depois para polígonos ou zonas mais complexas. |
| T024 | Endpoints internos planejados ficarão sob `/internal`. | Decidida | Recursos operacionais que não fazem parte da API pública ficam separados por caminho desde a primeira implementação. |
| T025 | Fila de recarga e fila de entrega serão implementadas primeiro como estado persistido simples. | Decidida | O projeto não adotará broker externo antes de existir necessidade real de processamento assíncrono. |
| T026 | Campos operacionais de bateria, velocidade e recarga foram adicionados à tabela `drones` com defaults. | Decidida | Drones existentes continuam válidos após a migration e novos cadastros podem omitir esses campos operacionais. |
| T027 | A bateria mínima usa a fórmula `distância total * consumo + reserva mínima <= bateria atual`. | Decidida | A validação fica determinística e reaproveita a distância total da rota já otimizada. |
| T028 | A fila de recarga será persistida nos próprios drones com `status`, `rechargeQueuedAt` e `rechargeReason`. | Decidida | A primeira versão da fila fica simples, ordenável e sem tabela ou broker adicional. |
| T029 | A fila operacional de pedidos será persistida com `queuedAt` na tabela `orders`. | Decidida | O endpoint de fila e o modo `optimizeRoute=false` usam a mesma ordenação por `queuedAt` e `id`. |
| T030 | Obstáculos serão persistidos na tabela `obstacles` e aplicados por desvio tangencial simples por trecho. | Decidida | A primeira versão evita pathfinding complexo, mas já torna distância, bateria e tempo sensíveis a zonas circulares. |
| T031 | O retorno antecipado usa o progresso persistido em `trip_orders.delivered_at` para separar pedidos entregues dos pedidos pendentes. | Decidida | A regra evita inferir entregas pela bateria e mantém os pedidos restantes disponíveis para replanejamento. |
| T032 | A telemetria de bateria atualiza o campo `batteryLevel` do drone associado à viagem. | Decidida | O drone mantém a última leitura operacional disponível para consultas e validações posteriores. |
| T033 | O histórico de telemetria será persistido na tabela `trip_telemetry`. | Decidida | Cada leitura mantém `trip_id`, `battery_level` e `reported_at` sem misturar histórico com o estado atual do drone. |
| T034 | O progresso por item da rota será persistido em `trip_orders.delivered_at`. | Decidida | A sequência da rota e o estado de entrega ficam juntos sem criar uma tabela nova para este ciclo. |
| T035 | O tempo estimado por pacote será persistido em `trip_orders.estimated_delivery_time`. | Decidida | Consultas futuras da viagem retornam a mesma previsão calculada no planejamento. |
| T036 | Avaliações serão persistidas em tabela própria `reviews`. | Decidida | O recurso de feedback fica isolado do ciclo operacional de pedidos e viagens. |
| T037 | O código principal será organizado por pacotes de responsabilidade. | Decidida | Controllers, serviços, domínio, persistência e exceções ficam separados para facilitar navegação e evolução no IntelliJ IDEA. |
| T038 | O dashboard frontend ficará no diretório `frontend` com React, TypeScript e Vite. | Decidida | A interface evolui separada do backend Java, usando proxy local para consumir a API Spring. |
| T039 | Ações operacionais do dashboard reutilizarão endpoints REST existentes. | Decidida | O frontend evolui sem duplicar regra de negócio, e a API continua sendo a fonte das validações de estado. |
| T040 | O dashboard acionará transições de viagens e telemetria pela tabela de viagens. | Decidida | Operadores podem conduzir o ciclo planejado, em rota, concluído, cancelado ou retornado antecipadamente pela interface, usando endpoints existentes. |
| T041 | O dashboard permitirá criar drones, criar pedidos e acionar planejamento persistido. | Decidida | O ciclo operacional pode começar pela interface sem Postman para cadastro básico e geração de viagens planejadas. |
| T042 | O dashboard permitirá gerenciar obstáculos circulares. | Decidida | Operadores podem cadastrar e desativar restrições de rota antes de acionar novo planejamento. |
| T043 | O dashboard permitirá cadastrar e consultar avaliações do serviço. | Decidida | Feedbacks de cliente ficam visíveis na interface junto com a média de estrelas já calculada nos indicadores. |
| T044 | O dashboard terá uma visão dedicada de filas operacionais. | Decidida | Operadores podem acompanhar pedidos aguardando planejamento, pedidos em reatribuição e drones aguardando recarga no mesmo painel. |
| T045 | O dashboard terá uma visão detalhada de viagem. | Decidida | Operadores podem acompanhar rota por posição, entrega estimada por pacote, estado de cada entrega e histórico de telemetria da viagem. |
| T046 | O dashboard terá um mapa 2D operacional. | Decidida | Operadores podem visualizar base, pedidos, obstáculos e a rota da viagem selecionada a partir das coordenadas persistidas. |
| T047 | O dashboard terá uma jornada guiada do ciclo operacional completo. | Decidida | Operadores podem acompanhar a sequência de cadastro, obstáculo opcional, planejamento, início, entregas, telemetria e encerramento usando as áreas já existentes. |
| T048 | O dashboard terá um carregador controlado de cenário demo. | Decidida | Operadores podem limpar os dados operacionais e recriar dados de teste por endpoint interno com confirmação explícita. |
| T049 | O dashboard indicará a disponibilidade da API Spring. | Decidida | Operadores enxergam quando o backend está offline e as ações operacionais ficam bloqueadas até a API voltar. |
| T050 | A execução local integrada terá script dedicado. | Decidida | Banco, backend e frontend podem ser iniciados por um fluxo único depois da instalação das dependências do frontend. |
| T051 | O dashboard exibirá status operacionais em português. | Decidida | Operadores não precisam interpretar enums técnicos para entender drones, pedidos e viagens. |
| T052 | Botões de ação por ícone terão descrições de hover. | Decidida | A consulta operacional fica menos ambígua, principalmente em ações críticas de drones e viagens. |
| T053 | O mapa 2D permitirá alternar entre viagem selecionada e todas as viagens. | Decidida | Operadores podem focar uma rota ou comparar visualmente o conjunto de viagens planejadas. |
| T054 | Rotas no mapa terão cor por viagem, direção e numeração de pontos. | Decidida | A leitura operacional da sequência de entregas fica menos ambígua sem exigir alteração no backend. |
| T055 | O frontend terá experiências separadas para Admin e Cliente. | Decidida | A interface administrativa permanece completa e a interface do cliente expõe somente pedido, acompanhamento e avaliações. |
| T056 | A primeira tela Cliente reutilizará endpoints existentes. | Decidida | O cliente poderá solicitar entrega, acompanhar por ID ou código e avaliar o serviço sem alterar o backend neste ciclo. |
| T057 | O estado de simulação será persistido em `trips`. | Decidida | O dashboard pode recarregar e continuar exibindo posição, distância percorrida e progresso sem manter estado apenas no navegador. |
| T058 | O dashboard avançará automaticamente viagens planejadas ou em rota. | Decidida | Admin e Cliente observam o mesmo ciclo operacional em tempo quase real usando o endpoint de tick da API. |
| T059 | Endpoints internos sob `/internal` exigirão header `X-Internal-Api-Key`. | Decidida | A separação por caminho passa a ter uma barreira de autenticação configurável sem afetar a API pública do desafio. |
| T060 | Pedidos terão `delivery_confirmation_code` persistido com o mesmo valor do identificador de rastreio. | Decidida | A aba Cliente pode acompanhar e confirmar recebimento com um único código, sem depender de ação administrativa para registrar a entrega. |
| T061 | Pedidos terão `status_reason` persistido para mensagens operacionais. | Decidida | Motivos de não alocação e justificativas de cancelamento podem ser exibidos de forma consistente para admin e cliente. |
| T062 | O dashboard usará o parâmetro `month=YYYY-MM` para alternar o relatório mensal. | Decidida | A interface pode recalcular e exibir competências anteriores sem criar novos endpoints. |
| T063 | Pedidos terão `confirmed_delivery_time` persistido. | Decidida | O cadastro passa a registrar o horário confirmado de entrega antes do planejamento e esse horário fica visível para admin e cliente. |
| T064 | A entrega por código exigirá disponibilidade confirmada pelo cliente. | Decidida | A notificação de aproximação deixa de ser apenas informativa e passa a liberar a confirmação de recebimento somente após resposta explícita do cliente. |
| T065 | Falta de resposta de disponibilidade marcará o pacote como `NOT_DELIVERED`. | Decidida | O drone retorna à base com a encomenda, a viagem fica `RETURNED_EARLY` e o pacote mantém uma tag rastreável de não entrega com motivo em português. |
| T066 | O cliente terá 1 minuto para informar o código após a chegada do drone. | Decidida | Se o prazo expirar, o pacote fica `NOT_DELIVERED`, a posição é resolvida como falha e o drone segue a rota levando a encomenda de volta para a base. |
| T067 | A aba Cliente terá `Meus pedidos` vinculada à conta autenticada. | Decidida | Com autenticação de cliente, a interface lista pedidos persistidos da conta e permite alternar o pedido acompanhado sem depender de códigos salvos no navegador. |
| T068 | A documentação interativa do backend será gerada com Springdoc OpenAPI. | Decidida | O Swagger UI expõe a visão completa do backend, a API pública e a API interna, mantendo os esquemas de autenticação visíveis para teste manual. |
| T069 | A aplicação usará unidades métricas explícitas no padrão brasileiro. | Decidida | Peso/capacidade ficam em kg; coordenadas, distância, alcance e raio em km; velocidade em km/h; bateria em %, consumo em %/km, recarga em %/min e tempos em min. |

## Decisões pendentes

| ID | Decisão pendente | Observação |
| --- | --- | --- |
| P002 | Definir se haverá CLI além da API REST. | A interface escolhida para teste manual no momento é HTTP via Postman. |

## Histórico resumido

- O repositório local foi criado dentro da pasta `java-technical-challenge`.
- O arquivo `REQUIREMENTS.md` foi criado para registrar objetivo, requisitos, regras, critérios de aceite e entregáveis.
- As principais ambiguidades de negócio foram analisadas antes da implementação.
- As decisões de negócio já tomadas foram incorporadas ao `REQUIREMENTS.md`.
- O projeto mínimo Java foi configurado com Maven, Java 17 e JUnit 5.
- O domínio foi implementado incrementalmente com prioridades, coordenadas, pedidos, drones, viagens e planejador de viagens.
- Uma API REST mínima foi adicionada para permitir testes manuais via Postman.
- Foi aprovado o roteiro de evolução para controle operacional persistido usando PostgreSQL via Docker.
- Foram aprovados os status iniciais de pedido e drone.
- Foi decidido que o drone só ficará indisponível quando uma viagem for iniciada.
- O conceito `OrderStatus` foi implementado com os status operacionais aprovados para pedidos.
- O conceito `DroneStatus` foi implementado com os status operacionais aprovados para drones.
- Foram definidos os status de viagem e as transições iniciais entre planejamento, início, conclusão e cancelamento.
- O conceito `TripStatus` foi implementado com os status operacionais aprovados para viagens.
- A infraestrutura local de PostgreSQL via Docker Compose foi definida.
- Flyway foi escolhido e configurado como ferramenta de migração de banco.
- Foram definidos os endpoints mínimos e contratos iniciais da API operacional.
- O cadastro persistido de drones foi implementado com `POST /api/drones`.
- A consulta persistida de drones foi implementada com `GET /api/drones` e `GET /api/drones/available`.
- Foi criada a migration da tabela `drones`.
- O cadastro persistido de pedidos foi implementado com `POST /api/orders`.
- A consulta persistida de pedidos foi implementada com `GET /api/orders` e filtro opcional por status.
- O filtro inválido de status de pedido foi padronizado como HTTP `400`.
- Foi criada a migration da tabela `orders`.
- O planejamento operacional persistido foi implementado em `POST /api/trip-plans`.
- Foram criadas as tabelas `trips` e `trip_orders`.
- A consulta de viagens persistidas foi implementada com `GET /api/trips`.
- A transição de início de viagem foi implementada com `POST /api/trips/{id}/start`.
- A transição de conclusão de viagem foi implementada com `POST /api/trips/{id}/complete`.
- A transição de cancelamento de viagem foi implementada com `POST /api/trips/{id}/cancel`.
- As consultas por identificador foram implementadas com `GET /api/drones/{id}`, `GET /api/orders/{id}` e `GET /api/trips/{id}`.
- A unicidade do identificador de pedido foi implementada e protegida pela migration `V5__add_unique_order_identifier.sql`.
- Os motivos de não alocação foram detalhados para restrições de peso, alcance ou peso e alcance.
- A alteração manual de disponibilidade de drones foi implementada com `POST /api/drones/{id}/unavailable` e `POST /api/drones/{id}/available`.
- A consulta de drones foi estendida com filtro opcional por status em `GET /api/drones?status=...`.
- A consulta de viagens foi estendida com filtro opcional por status em `GET /api/trips?status=...`.
- Foi adicionada uma suite de integracao para validar o fluxo operacional persistido com Spring Boot, JPA, Flyway e PostgreSQL.
- O contrato HTTP consolidado foi documentado em `API.md`.
- A rota dentro de cada viagem passou a ser otimizada antes da validação de alcance e da persistência.
- Foi aprovado o roteiro de evolução para bateria, recarga, tempo estimado, obstáculos, filas e replanejamento por retorno antecipado.
- A bateria básica do drone foi persistida e exposta para consulta interna em `GET /internal/drones/{id}/battery`.
- O planejamento e o início de viagem passaram a validar bateria suficiente para rota completa e reserva mínima de retorno.
- A fila de recarga foi implementada com status `CHARGING`, endpoints de entrada/conclusão e consulta em `GET /api/recharge-queue`.
- As respostas de planejamento, consulta e transição de viagens passaram a incluir `estimatedDuration`.
- A fila operacional de pedidos foi implementada com `queuedAt`, `GET /api/delivery-queue` e `POST /api/trip-plans?optimizeRoute=false`.
- Obstáculos circulares foram implementados com `POST /api/obstacles`, `GET /api/obstacles`, `DELETE /api/obstacles/{id}` e distância ajustada no planejamento.
- O retorno antecipado foi implementado em `POST /api/trips/{id}/complete` com status `RETURNED_EARLY`, pedidos restantes em `PENDING_REASSIGNMENT` e drone em `CHARGING`.
- A telemetria de bateria foi implementada com `POST /api/trips/{id}/telemetry`, atualizando `batteryLevel`, persistindo histórico e acionando retorno antecipado quando a rota deixa de ser segura.
- A consulta de histórico de telemetria foi implementada com `GET /api/trips/{id}/telemetry`.
- O progresso de entrega por posição da rota foi implementado com `POST /api/trips/{id}/route/{routePosition}/deliver`, `confirmationCode` e `trip_orders.delivered_at`.
- A ordem automática de entrega por prioridade, peso e distância foi implementada no planejamento e na rota persistida.
- O tempo estimado por pacote e o tempo médio até entrega foram implementados nas respostas de planejamento e viagem.
- Avaliações do serviço foram implementadas com `POST /api/reviews`, `GET /api/reviews` e `GET /api/reviews/{id}`.
- O código principal foi reorganizado em pacotes `controller`, `domain`, `service`, `persistence` e `exception`.
- A base do dashboard frontend foi criada em `frontend`, com visão geral operacional consumindo endpoints reais da API.
- O dashboard frontend recebeu tabelas consultivas de drones, pedidos e viagens com busca e filtro por status.
- O dashboard frontend recebeu ações operacionais de drones com atualização automática dos indicadores após cada ação.
- O dashboard frontend recebeu ações operacionais de viagens para iniciar, aguardar confirmação do cliente, enviar telemetria de bateria, concluir e cancelar.
- O dashboard frontend recebeu cadastro de drones, cadastro de pedidos e acionamento de planejamento persistido com opção de rota otimizada.
- O dashboard frontend recebeu gestão de obstáculos circulares com cadastro, listagem de estado e desativação.
- O dashboard frontend recebeu cadastro e consulta de avaliações com estrelas, título e feedback.
- O dashboard frontend recebeu visão dedicada de filas de entrega, reatribuição e recarga.
- O dashboard frontend recebeu visão detalhada de viagem com rota, progresso por entrega e histórico de telemetria.
- O dashboard frontend recebeu mapa 2D com base, pedidos, obstáculos e rota da viagem selecionada.
- O dashboard frontend recebeu jornada guiada do ciclo operacional completo com atalhos para as áreas relacionadas.
- O dashboard frontend recebeu carregamento controlado de cenário demo com reset interno, drones, pedidos, obstáculo, avaliação e planejamento otimizado.
- O dashboard frontend passou a indicar disponibilidade da API Spring e bloquear ações operacionais quando o backend está offline.
- O reset do cenário demo no dashboard passou a exigir confirmação visual antes de limpar dados operacionais.
- Foi adicionado um script de execução local integrada para subir banco, backend e frontend no mesmo fluxo.
- O dashboard frontend passou a exibir status de drones, pedidos e viagens em português.
- Os botões de ação por ícone da consulta operacional receberam descrições de hover mais explicativas.
- O mapa 2D do dashboard passou a alternar entre rota da viagem selecionada e rotas de todas as viagens.
- As rotas do mapa receberam cores por viagem, setas de direção, chips de seleção e pontos numerados pela ordem de entrega.
- O frontend passou a ter navegação entre experiência Admin e experiência Cliente.
- A tela Cliente passou a permitir solicitação limitada de entrega, acompanhamento por ID ou código, confirmação de recebimento, mapa da viagem associada e avaliações públicas.
- Viagens passaram a expor estado de simulação com posição atual, distância percorrida, progresso e próximo pedido.
- O dashboard passou a avançar automaticamente viagens planejadas ou em rota, exibindo o marcador do drone no mapa e aguardando confirmação do cliente nos pontos de entrega.
- Endpoints internos sob `/internal` passaram a exigir o header `X-Internal-Api-Key`.
- Pedidos passaram a usar o próprio código de rastreio como código de confirmação de entrega, exibido no cadastro e exigido na confirmação feita pela aba Cliente.
- A experiência Cliente passou a exigir cadastro/login, com senha armazenada por hash PBKDF2 no backend, token assinado para sessão e pedidos vinculados à conta autenticada.
- A aba `Meus pedidos` passou a listar pedidos persistidos da conta autenticada, substituindo a lista local de códigos salvos no navegador.
- O backend passou a publicar documentação Swagger/OpenAPI em `/swagger-ui.html`, `/v3/api-docs` e `/v3/api-docs.yaml`, com grupos para API pública, API interna e backend completo.
