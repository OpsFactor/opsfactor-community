# OpsFactor Community Backend

Backend Community da plataforma OpsFactor.

Este workspace e o futuro repositorio aberto/source-available da edicao Community. Ele deve compilar sozinho, sem depender de artefatos ou fontes do `opsfactor-enterprise`.

## Status

- Projeto em migracao a partir do monolito legado.
- O repositorio Git local acompanha o repositorio Community remoto.
- A licenca Sustainable Use License 1.0 esta em `LICENSE.md`.
- Front-end Community sera mantido em repositorio separado (`opsfactor-community-front`). O backend nao versiona o `dist` do front; o pipeline de release compilara o front por fora e copiara o artefato para o instalador/imagem final com servidor front incluido.
- Publicacao Git sera por snapshot inicial limpo apos terminar e validar este workspace.

## Estrutura

O Community usa um unico build Maven raiz. A separacao interna fica em packages
sob `src/main/java`, nao em submodulos Maven.

- `com.opsfactor.community.web`: aplicacao web/API Community e seus contratos.
- `com.opsfactor.community.capability.<dominio>`: entidades, repositories, projections, services, DTOs e rotinas de cada capacidade.
- `com.opsfactor.community.platform`: infraestrutura compartilhada, calendario, BI em memoria, integracao, runtime e utilitarios.
- `com.opsfactor.community.rinstance`: acesso R restrito aos modelos estatisticos Community.
- `com.opsfactor.community.scheduler`: historico tecnico de execucoes sincronas.
- `com.opsfactor.community.security`: login simples Community.

## Build

Usando o repositorio Maven local compartilhado do workspace:

```powershell
mvn -q -f C:\Users\erick\IdeaProjects\opsfactor-community\pom.xml process-resources compile -DskipTests "-Dmaven.repo.local=C:\Users\erick\IdeaProjects\.m2repo"
```

Testes focados de contrato Community:

```powershell
mvn -q -f C:\Users\erick\IdeaProjects\opsfactor-community\pom.xml "-Dtest=CommunityRuntimeInfoServiceTest,ClusterEParametrosProjectionFactoryCommunityContractTest,SupplyNetworkProjectionCommunityContractTest,BIProjectionCapacidadeProdutivaCommunityContractTest,PoliticaEstoquesProjectionFactoryCommunityContractTest,EstoqueProjectionFactoryCommunityTest,SalesProjectionFactoryCommunityContractTest,SupplyPlanServiceCommunityContractTest,DemandPlanningServiceCommunityContractTest" test "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.repo.local=C:\Users\erick\IdeaProjects\.m2repo"
```

Contratos Community de master data, data upload e bordas transacionais:

```powershell
mvn -q -f C:\Users\erick\IdeaProjects\opsfactor-community\pom.xml "-Dtest=IntegrationServiceComConfiguracoesInterfaceTest,ProductionIntegrationServicesCommunityContractTest,TransportationLaneIntegrationServicesCommunityContractTest,LinhaTransporteProdutoIntegrationServiceTest,HistoricalDataIntegrationServicesCommunityContractTest,ParametrosMaterialLocationIntegrationServiceCommunityContractTest,MasterdataFrontServicesCommunityContractTest,ProductionFrontDtoCommunityContractTest,IntegrationMapperHeadersCommunityBoundaryTest,IntegrationControllerAbstractCommunityTest,DataUploadControllersCommunityContractTest,MasterdataControllersCommunityContractTest,IntegrationOpenApiConfigurationCommunityTest,CommunityArchitectureBoundaryTest" test "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.repo.local=C:\Users\erick\IdeaProjects\.m2repo"
```

Guardrails arquiteturais e de bootstrap:

```powershell
mvn -q -f C:\Users\erick\IdeaProjects\opsfactor-community\pom.xml "-Dtest=CommunityArchitectureBoundaryTest,RequiresEnterpriseVersionExceptionTest,CommunityRuntimeInfoServiceTest,RuntimeInfoControllerTest,AdminRestControllerCommunityContractTest,UserFrontServiceTest,CustomUserDetailsServiceTest,CustomHttpSecurityConfigTest" test "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.repo.local=C:\Users\erick\IdeaProjects\.m2repo"
```

Teste completo do workspace Community:

```powershell
mvn -q -f C:\Users\erick\IdeaProjects\opsfactor-community\pom.xml test "-Dmaven.repo.local=C:\Users\erick\IdeaProjects\.m2repo"
```

Empacotamento sem testes:

```powershell
mvn -q -f C:\Users\erick\IdeaProjects\opsfactor-community\pom.xml package -DskipTests "-Dmaven.repo.local=C:\Users\erick\IdeaProjects\.m2repo"
```

## Configuracao Local

O profile MariaDB Community usa variaveis externas para conexao:

- `OPSFACTOR_DATASOURCE_HOST` (default `localhost`)
- `OPSFACTOR_DATASOURCE_PORT` (default `3306`)
- `OPSFACTOR_DATASOURCE_DATABASE` (default `opsfactor`)
- `OPSFACTOR_DATASOURCE_USERNAME` (default `opsfactor`)
- `OPSFACTOR_DATASOURCE_PASSWORD` (default vazio)

Nao versionar nomes de cliente, hosts privados ou credenciais reais/fake em properties ou README.

## Recorte Funcional

Community deve permanecer limitado a:

- Demand Planning com modelos estatisticos basicos, split por Historical Sales e ajustes via Planning Book material/location.
- Supply Planning heuristico, fair share operacional, plano restrito heuristico e Planning Book material/location.
- Sell-out quantitativo e estoque inicial como dados transacionais.
- Login simples com role unica `ROLE_ADMIN`.
- MariaDB/H2/SQLite no backend aberto.

`ClusterEParametrosProjectionFactory` valida o snapshot base de materiais e
locations antes de montar mapas por id. Colecao nula, item nulo, id ausente ou
id funcional duplicado falham antes de consultar clusters/parametros e antes de
materializar a projection central usada por Demand/Supply. Os repositories
internos de clusters, regras de alocacao e parametros material/location tambem
falham para colecao ou item nulo antes dos streams/groupings.

No Supply Planning Community, `SafetyStockMultiplasLocationsProjection`
materializa no maximo uma projection de safety stock por location. Duplicidade
falha antes de overwrite para que o plano heuristico e overlays Enterprise nao
dependam da ordem da colecao recebida.

`SupplyPlanningMultiplasLocationsProjection` segue a mesma regra: a escrita de
projections locais passa por metodo proprio, com uma unica
`SupplyPlanningProjection` por location planejada.

`BIProjectionCapacidadeProdutiva` valida disponibilidade de master data e
snapshots persistidos de capacidade efetiva antes de indexar por recurso/data
ou recurso/periodo. Chave incompleta, valor nulo, `NaN`, infinito, capacidade
negativa ou duplicidade funcional falham antes de alimentar o plano restrito
Community.

`PoliticaEstoquesProjectionFactory` aceita lista vazia de politicas como
ausencia operacional de override, mas valida vinculos, cabecalho de politica,
vigencia, regras material/location e valores fisicos antes de popular safety
stock vigente.

`EstoqueProjectionFactory` valida agregados de estoque inicial antes de popular
as projections por location/material, location/material/data ou material. UOM
nula segue usando fallback operacional; quantidade nula, `NaN`, infinito ou
chave material/location/data quebrada falham antes de alimentar Supply
Planning. As proprias projections de estoque repetem esse contrato nos
`addEstoque(...)` publicos, preservando mapas internos vazios em builders e
impedindo que testes, factories alternativas ou overlays Enterprise criem
indices mutaveis com chave funcional quebrada.

`SupplyNetworkProjectionFactory` valida colecoes retornadas por repositories de
malha e producao antes de montar mapas de transporte, recursos produtivos, BOM,
roteiros, versoes simples e versoes de malha. Lista vazia segue sendo snapshot
operacional valido; colecao nula ou item nulo falham como contrato quebrado do
repository/stub. Versoes de malha tambem precisam ter id obrigatorio e unico
antes do mapa por id usado pelo Supply Planning; recursos produtivos precisam
ter id obrigatorio e unico antes de `mapaRecursosProdutivos`; listas tecnicas
e roteiros seguem a mesma regra antes de `mapaListasTecnicas` e
`mapaRoteiros`.

No Demand Planning Community, `DemandPlanningProjection` materializa uma unica
linha de plano e uma unica linha historica por `periodo/location/material`; uma
segunda instancia para a mesma chave falha antes de sobrescrever a fotografia do
Planning Book ou da ponte Demand -> Supply.

`DemandAnalysisMapper` publica a simulacao de forecast apenas como series
material/location. Antes de montar o DTO, ele valida configuracao, calendario,
projection de sales/UOM, lista de projections, lista material/location,
identidade location/material e series numericas. Historicos podem ser menores
que o horizonte total, mas nao maiores; baseline, trend/seasonal e bounds
opcionais precisam acompanhar o calendario completo e conter valores finitos.

Na ponte Demand -> Supply, `DemandaDiretaConsideradaProjection` exige
`SupplyPlan`, calendario e linhas com chave funcional completa antes de indexar
o BI em memoria por material/location/periodo. A projection preserva a regra de
deduplicacao mensal de snapshots antigos, mantendo a primeira linha encontrada
para nao dobrar demanda considerada.

`FiltroDFUProjection` permanece como escopo tecnico em memoria para Planning
Book, Demand Planning e Supply Planning. Ele nao representa filtros/agregadores
cadastraveis Enterprise, mas exige `Location` e `Produto` com id funcional em
construtores, colecoes de DFU, filtros opcionais e operacoes diretas antes de
alterar o escopo.

`ConfiguredViewProjection` materializa a view Community do Planning Book no
menor nivel material/location. Quando houver selecao de celulas para atualizar,
cada `AjusteCelulaPlanningBook` precisa carregar escopo DFU, periodo, key
figure, unidade de medida e novo valor finito antes de Demand Planning, Supply
Planning ou Planning Book consumirem a projection. O mapa de erros por celula
usa o mesmo contrato para garantir que tooltip e log sejam renderizados com
dados completos. `AjusteCelulaPlanningBook` e apenas o envelope tecnico da
celula ja resolvida pela view Community; a decisao de quais key figures sao
editaveis continua nos services de Demand/Supply.

`ConfiguredViewService` valida o resultado da factory antes de mutar selecoes
ou percorrer DFUs em cobertura de views. Na escrita de Planning Book, a celula
material/location tambem precisa pertencer ao `FiltroDFUProjection` da view; DFU
fora da view e erro funcional de payload/escopo, enquanto projection nula ou sem
DFU filtrada e quebra estrutural de snapshot/factory.

`ConfiguredViewProjectionFactory` parte da `ClusterEParametrosProjection` ja
materializada e valida a fotografia de materiais/locations antes de montar
`FiltroDFUProjection` e `PlanningBookDfuScope`. Colecoes vazias seguem
representando view sem DFUs; item nulo, id ausente ou duplicidade funcional por
id falham antes de aplicar filtros de ativo ou filtros ad-hoc de DFU.

`KeyFigureProjectionFactory` publica apenas key figures padrao Community nos
Planning Books. Em Demand e Supply, view sem lista explicita de KFs (`null` ou
vazia) usa o catalogo padrao Community; lista preenchida e tratada como
configuracao explicita e validada contra a allowlist antes de chegar ao front.
No Supply Planning Book, a location de entrada tambem precisa ter id funcional
antes de qualquer calendario, projection ou BI material/location.

`KeyFigureProjection` e o indice material/location/periodo das KFs exibidas no
Planning Book. Escritas publicas exigem BI inicializado, calendario quando a
chamada usa posicao de periodo, location/material com id, data de referencia,
key figure e valor finito. Leituras continuam podendo usar material/location
nulos como filtro agregado tecnico, mas filtros informados precisam ter id.

`PlanningBookService` recebe a `KeyFigureProjection` ja materializada e valida
calendario, view, projection de parametros, parametros globais e lista de KFs
antes de montar `PlanningBookDTO`. Assim snapshots incompletos falham na borda
do service, antes de colunas, grupos ou preenchimento de periodos.

`PlanningBookExcelExportService` e somente leitura e reusa a mesma montagem de
DTO do `DemandPlanningFrontService`; portanto nao bypassa bloqueios de
reference plan, agrupamentos, upload/importacao ou key figures Enterprise.
Antes de criar o workbook, o export valida a estrutura minima do
`PlanningBookDTO`: colunas com `field`, periodos, grupos/subgrupos, key figures
e valores numericos finitos. Metadados visuais opcionais continuam opcionais.

`SalesProjectionFactory` valida a fotografia first/last de sell-out antes de
popular `FirstLastSalesProjection`. Datas nulas ou janela invertida
(`lastDateTime` anterior a `firstDateTime`) falham como snapshot quebrado do
repository, nao como ausencia operacional de historico. A propria
`FirstLastSalesProjection` tambem exige calendario, valida entradas diretas,
valida chaves de lookup e rejeita segunda escrita para a mesma chave
material/location, material ou location antes de alimentar seus mapas mutaveis.
As projections quantitativas de sales exigem material/location com id funcional
antes de indexar agregados, preservando a mesma regra para sell-out Community e
para overlays Enterprise que reutilizam essas estruturas.

As rotinas estatisticas Community diferenciam configuracao quebrada de feature
Enterprise: modelo estatistico ou split nulo falham com erro de contrato; modelos
e splits Enterprise falham com `RequiresEnterpriseVersionException`.

O caller R Community (`com.opsfactor.community.rinstance`) valida calendario, array de venda
historica tratada, tamanho minimo da janela historica e valores finitos antes
de montar a serie enviada ao R para ARIMA, Holt-Winters e Exponential Smoothing.
Isso evita que historico curto seja completado com zeros antes do runtime
estatistico.

Capacidades como otimizador, process chain, pricing, finance, AI, mapas/GIS, frotas, warehouses, sell-in, sales orders, campanhas/eventos, SSO e filas pertencem ao Enterprise.

## Regras De Fronteira

- O Community nao deve importar nem depender de `com.opsfactor.enterprise`.
- Beans Enterprise devem ser opcionais e tratados por SPI quando o Community precisar conhecer o ponto de extensao.
- Sem implementacoes falsas de features Enterprise. Se a implementacao real nao existir no classpath, o fluxo deve falhar com `RequiresEnterpriseVersionException`.
- Execucoes Community sao sincronas; mensageria, batch workers e filas voltam apenas no Enterprise.
- A edicao nao deve ser escolhida por `opsfactor.edition`; Community e o default quando apenas os artefatos abertos estao no classpath.
- `api/open/runtime-info` deve expor `edition` e listas estaticas de
  opcoes/capacidades por runtime; nao deve depender de dados cadastrados no
  banco nem carregar booleano redundante de Enterprise. Campos `available...`
  representam somente valores selecionaveis no backend Community atual; campos
  `...Options` representam catalogos visuais completos para a SPA mostrar itens
  Enterprise bloqueados, como Chronos, HTS, Sell-in, Sales Orders, Optimizer e
  Process Chain, com `requiredEdition`, `availableInCurrentRuntime`,
  `disabled` e `disabledReason`. Essas listas incluem modelos/documentos
  selecionaveis e as KFs padrao dos Planning Books de Demand/Supply; em Supply
  os ids seguem o formato tecnico da grade, como `Stock-Working Plan`.
- O backend Community nao empacota front legado (`templates`, `public`, `static`, `.html`, `.js`, `.css`). O front Community roda em repositorio/servidor separado.
- Naming operacional deve ser Community/Enterprise. Nomes antigos de edicao nao devem voltar para codigo, Maven ou properties.
- POMs Community nao devem declarar repositórios Spring snapshot/milestone; o build publicado deve depender de releases estaveis.
- Seguranca Community permanece pequena: HTTP Basic, BCrypt, `/api/open/**` e health publicos, `ROLE_ADMIN` como unica role funcional.
- `UserFrontService` e a borda administrativa de usuarios validam tambem a
  fotografia JPA de roles antes de listar, mutar ou aceitar save como sucesso:
  lista nula, item nulo, chave composta ausente ou role sem tipo sao snapshot
  quebrado de seguranca, nao role funcional Enterprise ignoravel.
- `CustomUserDetailsService` aplica a mesma regra no login runtime antes de
  montar `UserDetails`: `AuthenticationService` deve devolver `Optional` real,
  usuario com id/senha e roles estruturalmente integras; somente depois disso
  roles diferentes de `ROLE_ADMIN` podem ser ignoradas como legado/Enterprise.
- `AuthenticationService` valida o `SecurityContext`, ids de usuario, authorities
  e `Optional` retornado pelo repository antes de comparar permissoes ou expor
  roles serializadas para contratos legados.
- `IntegrationControllerAbstract` valida as roles declaradas por subclasses GET
  e POST antes de chamar `AuthenticationService`; lista vazia significa acesso
  negado, mas lista nula ou item nulo e erro de contrato do controller/overlay.
- Controllers Community que precisam do usuario logado ou da authority atual
  devem preferir `AuthenticationService` a acesso direto ao `SecurityContext`;
  `UserConfigurationController`, `DemandPlanningRestController` e
  `SupplyPlanningController` ja seguem esse contrato para views, preferencias e
  Planning Books.
- `WebControllerTaskSchedulingService` tambem usa `AuthenticationService` para
  registrar o usuario das tasks imediatas/sincronas, preservando o gate
  Community de bloquear ASYNC/BATCH antes de consultar beans de runtime.
- Campos Community que representam beans Spring devem declarar `@Autowired` explicitamente; `Task` e a excecao documentada porque cada instancia recebe o scheduler/service no construtor.
- Codigo produtivo Community nao deve usar `TODO`, `FIXME`, marcadores genericos de implementacao pendente nem `IllegalArgumentException`/`UnsupportedOperationException` sem mensagem. Bordas abertas precisam falhar com contrato claro: payload invalido, dado operacional ausente ou capability Enterprise bloqueada.
- `ParametrosChronos` permanece no modelo compartilhado apenas como metadata para o overlay Enterprise e para catalogo visual bloqueado; mesmo assim valida quantis e timeout tecnico para evitar que configuracao quebrada viaje ate a chamada Python privada.
- `CommunityFrontendArtifactBoundaryTest` protege que o backend Community nao
  volte a versionar `dist`, `static`, `public`, `templates` ou arquivos
  legados `.html`, `.js`, `.css` e `.map`; o front e empacotado em etapa de
  release separada.
