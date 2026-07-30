# Guia Da Licença Da OpsFactor Community

Status: licença oficial da release `v0.1.0`.

Este documento explica as escolhas e os cenários de uso do
[`LICENSE.md`](LICENSE.md). Em caso de divergência, prevalece o texto inglês
do `LICENSE.md` incluído na release correspondente.

## Modelo Adotado

A OpsFactor Community adota a própria **Sustainable Use License 1.0** criada e
publicada pela n8n, reproduzindo seu corpo sem mudanças substantivas.

Essa escolha elimina um instrumento contratual próprio da OpsFactor e permite
explicar a fronteira comercial por uma FAQ, seguindo o modelo da n8n:

- uso e modificação para finalidades internas, pessoais ou não comerciais;
- distribuição gratuita e não comercial;
- preservação dos avisos jurídicos;
- consultoria e suporte remunerados;
- contratação comercial separada quando a funcionalidade é oferecida a
  terceiros como produto, hosting ou serviço.

A atribuição à n8n deve permanecer explícita. Ela também deve informar que a
n8n não é afiliada à OpsFactor e não endossa a empresa ou o produto.

O preâmbulo específico da OpsFactor delimita branches, conteúdo
Community e componentes de terceiros. Ele declara expressamente que não
modifica os termos da Sustainable Use License.

## Matriz Consolidada

| Situação | Tratamento sob a licença |
| --- | --- |
| Empresa usa a Community para planejar a própria operação | Permitido |
| Empresa modifica código ou telas para o próprio uso | Permitido |
| Portal interno substitui ou remove o logo visual | Permitido; avisos jurídicos continuam obrigatórios |
| Empresa usa relatórios e planos em sua atividade comercial | Permitido |
| Distribuição gratuita para finalidade não comercial | Permitido com licença e avisos |
| Consultor cobra implantação, configuração, integração, treinamento, suporte ou manutenção | Permitido |
| Consultor reutiliza uma customização em vários clientes e cobra o trabalho | Permitido |
| Consultor cobra licença ou distribuição da Community ou de código OpsFactor derivado | Proibido |
| Empresa paga AWS, Azure, VPS, banco ou infraestrutura genérica para sua instância interna | Permitido |
| Consultor instala ou mantém a instância interna do cliente | Permitido |
| Provedor oferece acesso hospedado, SaaS ou planejamento automatizado para seus clientes | Exige acordo comercial |
| Produto white-label ou embedded deixa clientes operarem seus próprios planos | Exige acordo comercial |
| ERP ou conector independente usa APIs documentadas sem copiar código | Pode ter licença comercial própria |
| Plugin, fork ou produto pago copia código OpsFactor | A parte copiada continua sujeita à licença e não pode ser distribuída comercialmente |
| Imagem ou oferta de marketplace inclui OpsFactor em uma listagem comercial | Consultar a OpsFactor antes da publicação |

## O Que É A Própria Licença Da n8n

### Licença Curta E FAQ

O corpo contratual contém a mesma concessão, limitações, patentes, avisos,
encerramento, ausência de responsabilidade e definições da n8n Sustainable Use
License 1.0. Casos concretos ficam na FAQ pública, que interpreta a distinção
entre uso interno, integração independente, consultoria e oferta comercial da
funcionalidade.

### Código Parcial

A definição de software inclui “qualquer parte” do código. Portanto, uma
classe, componente ou fragmento copiado continua sujeito à licença. Não é
necessário criar uma cláusula viral que relicencie automaticamente todo o
produto independente ao redor.

### Hosting E Nuvem

Pagar infraestrutura de propósito geral para operar uma instalação interna
não equivale a comprar uma licença OpsFactor. A restrição surge quando um
provedor oferece a funcionalidade OpsFactor aos próprios clientes como
serviço ou produto.

A licença não exige que o cliente pague diretamente a conta de nuvem. Uma
consultoria pode contratar, instalar e administrar infraestrutura como parte
de seus serviços, desde que não esteja vendendo acesso à OpsFactor como sua
própria ferramenta.

### Consultoria

Consultores podem cobrar pelo valor profissional de implantação,
customização, integração, análise, treinamento e suporte. Também podem
reutilizar customizações. A restrição é cobrar licença ou distribuição da
Community ou do código OpsFactor copiado ou derivado.

### Outputs

Planos, forecasts, relatórios e decisões podem ser usados na operação
comercial interna do licenciado. Isso não autoriza transformar a plataforma
em um serviço automatizado de planejamento para terceiros.

## Onde A FAQ Foi Contextualizada Para A OpsFactor

A licença principal não adiciona proibições próprias de logo, embedding,
marketplace ou infraestrutura. A FAQ traduz os testes da n8n para situações de
planejamento:

- empresa cadastrando sua malha, materiais, demanda, estoque e parâmetros;
- execução interna por interface, API, script ou wrapper;
- entrega de resultado externo limitado, sem transferir ao terceiro a
  operação substancial da plataforma;
- planejamento multiempresa em que clientes independentes mantêm dados,
  modelos, workspaces e execuções próprios;
- conectores independentes em contraste com plugins que copiam código;
- consultoria genuína em contraste com SaaS automatizado.

Essa contextualização evita regras novas no instrumento e reduz pontos cegos do
domínio de Supply Chain Planning.

## Marca E Interface

A licença não exige preservar o logo OpsFactor nem o layout original em um
portal interno. Ela exige preservar avisos de licença, copyright e demais
avisos jurídicos presentes no software.

O uso de marcas continua sujeito à legislação aplicável. Essa solução segue a
lógica da n8n e evita transformar uma política visual de produto em obrigação
autoral da licença.

## Documentos Públicos

- O arquivo inglês do repositório reproduz a n8n Sustainable Use License 1.0 e
  é o instrumento aplicável à release.
- A documentação inglesa pode espelhar o texto para referência.
- A versão portuguesa é tradução informativa e declara a prevalência do
  `LICENSE.md` inglês da release correspondente.
- A FAQ bilíngue explica os casos de uso; o instrumento aplicável permanece o
  `LICENSE.md`.

O código-fonte, o `LICENSE.md` e a tag `v0.1.0` estão publicados no repositório
oficial da OpsFactor Community.

## Evoluções Do Projeto

1. manter a auditoria de dependências, assets, licenças e avisos de terceiros;
2. evoluir `NOTICE.md` e `THIRD_PARTY_NOTICES.md` quando novos componentes
   exigirem avisos adicionais;
3. adotar um Contributor License Agreement antes de aceitar contribuições
   externas;
4. publicar canais de contato para dúvidas de licenciamento e acordos
   comerciais.

Não se deve inventar um identificador SPDX oficial. Enquanto a Sustainable Use
License 1.0 não tiver identificador reconhecido no catálogo adotado pelo
projeto, um eventual `LicenseRef-...` deve ser documentado como referência
local e não como licença distinta da OpsFactor.

## Referências Primárias

- [n8n Sustainable Use License 1.0](https://github.com/n8n-io/n8n/blob/master/LICENSE.md)
- [n8n Sustainable Use License documentation](https://docs.n8n.io/privacy-and-security/sustainable-use-license/)
- [n8n license use-case guidance](https://support.n8n.io/article/can-i-use-your-license-for-my-use-case)
