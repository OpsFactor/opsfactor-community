# OpsFactor Community

[English](README.md)

OpsFactor Community é o backend source-available para planejamento de demanda
e supply chain. É uma aplicação Java 21 / Spring Boot que pode ser compilada
independentemente, sem depender de fontes ou artefatos do OpsFactor Enterprise.

O repositório é distribuído sob a [Sustainable Use License 1.0](LICENSE.md).
Consulte exemplos práticos na [FAQ de licenciamento Community](https://docs.opsfactor.com/pt/documentation/community/licensing-faq/).

## Versão

Use a tag Git e o manifesto da release para identificar a fonte exata de uma
distribuição. Backend e frontend têm versionamento independente; a versão Maven
sozinha não identifica um instalador. Use instalador, documentação e planilhas
de importação da mesma release.

## Comece aqui

- **Usar a aplicação:** siga o [guia de instalação Community](https://docs.opsfactor.com/pt/documentation/community/tutorial/01-installation/).
- **Carregar um cenário reproduzível:** siga o [tutorial Community](https://docs.opsfactor.com/pt/documentation/community/tutorial/), incluindo os arquivos para download e a ordem de importação.
- **Desenvolver o backend:** use as etapas de build e configuração abaixo. O backend não serve o frontend separado.

## Planejamento e dados de produção

- O planejamento estatístico de demanda e o Planning Book por material/location permitem colaboração por **Direct Demand = Baseline + Demand Adjustment**.
- O planejamento heurístico de supply usa a malha, estoques, recursos produtivos, disponibilidades, roteiros, listas técnicas e versões de produção.
- O roteiro contém a **quantidade base e sua unidade**; suas operações informam **duração e unidade de tempo**. A versão de produção vincula roteiro e lista técnica. Baixe os templates da versão em execução antes de preparar a importação: a ordem das colunas importa.
- **Production Plan Volume** e **Production Plan Occupation** expõem a produção planejada e o uso dos recursos. A unidade padrão SNP deve ser compatível com as unidades do cenário.

Os dados mestres de produção incluem modelos de output único e múltiplos outputs.
O tutorial valida o fluxo de output único. A persistência de Production Plan com
múltiplos outputs ainda tem uma limitação conhecida de chave/conflito; não
presuma que o suporte no cadastro significa que o fluxo completo de planejamento
está validado. Consulte o [tutorial de dados de produção](https://docs.opsfactor.com/pt/documentation/community/tutorial/11-production-data/).

## Arquitetura

- `com.opsfactor.community.bootstrap`: ponto de entrada da aplicação.
- `com.opsfactor.community.web`: fronteira de APIs Community.
- `com.opsfactor.community.capability.<domain>`: serviços, entidades, repositórios, projections, DTOs e rotinas de domínio.
- `com.opsfactor.community.platform`: runtime compartilhado, calendário, BI em memória, integrações e utilitários.
- `com.opsfactor.community.platform.rinstance`: acesso a modelos estatísticos por R.
- `com.opsfactor.community.platform.scheduler`: execução e histórico de tasks.
- `com.opsfactor.community.platform.security`: autenticação Community.

A edição Community inclui planejamento estatístico de demanda, planejamento
heurístico de supply, Planning Book por material/location e contratos de carga
de dados operacionais. Recursos exclusivos do Enterprise ficam fora deste repositório.

## Build

Use Java 21 e Maven na raiz deste repositório. Compile e execute a suíte padrão
de testes para produzir o backend executável:

```powershell
mvn clean package
```

Para executar somente a suíte padrão:

```powershell
mvn test
```

O gate de integração da migração PostgreSQL é separado: `*IT` não entra
automaticamente na suíte padrão. Informe o diretório de binários PostgreSQL
que contém `initdb` e `pg_ctl`; o teste cria e encerra seu próprio cluster
descartável e não deve apontar para um banco existente:

```powershell
mvn test "-Dtest=ProductionSchemaCompatibilityPostgreSqlIT" "-Dopsfactor.test.postgresql.bin=C:/path/to/postgresql/bin"
```

## Configuração local

Crie primeiro um banco PostgreSQL e um usuário dedicados. Os profiles padrão
são `prd,database-postgresql`; a conexão usa estas variáveis de ambiente:

- `OPSFACTOR_DATASOURCE_HOST` (padrão: `localhost`)
- `OPSFACTOR_DATASOURCE_PORT` (padrão: `5432`)
- `OPSFACTOR_DATASOURCE_DATABASE` (padrão: `opsfactor`)
- `OPSFACTOR_DATASOURCE_USERNAME` (padrão: `opsfactor`)
- `OPSFACTOR_DATASOURCE_PASSWORD` (padrão: vazio)

Não versione credenciais, hosts privados ou configurações específicas de clientes.
Depois de definir essas variáveis no shell, inicie o backend em primeiro plano:

```powershell
java -jar target/opsfactor-community-exec.jar
```

A porta padrão do código-fonte é `5000`; a prontidão pode ser consultada em
`http://localhost:5000/health-status`. As portas gerenciadas pelo instalador podem
ser diferentes. O forecast estatístico também precisa do R com o pacote
`forecast` disponível para o RCaller. A distribuição Windows empacota suas
dependências de runtime; compilar o código-fonte não as instala. Faça backup
de um banco existente antes de atualizar a versão.

## Frontend

O frontend Community fica no repositório separado
[`opsfactor-community-front`](https://github.com/OpsFactor/opsfactor-community-front).
O backend não versiona o `dist` do frontend; o processo de release o compila
separadamente e empacota o artefato resultante na distribuição final.
