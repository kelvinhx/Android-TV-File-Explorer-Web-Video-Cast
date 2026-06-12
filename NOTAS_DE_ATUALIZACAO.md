# Notas de Atualização - Nexus Explorer Pro

Aqui você encontra o registro histórico de atualizações e aperfeiçoamentos do Nexus Explorer Pro.

## [1.2.1] - 11/06/2026 19:15
- **Sistema Procedural de Ícones e Imagens**: Criação de um motor de renderização de sprites e desenhos de interface altamente personalizáveis (AestheticSprites) desenvolvidos em Canvas do Jetpack Compose para suportar temas claro e escuro de maneira dinâmica.
- **Cabeçalho Elaborado na TV (TvDashboardHeroBanner)**: Atualização do layout principal da TV com um luxuoso banner de boas-vindas com linhas de constelação fluidas, medidores de integridade de sinal e bordas de vidro (glassmorphism).
- **Decorações Técnicas de Biblioteca**: Implementação de uma fina grade conceitual de padrão cibernética atrás do texto das categorias de mídias, aperfeiçoando o preenchimento de espaço negativo.
- **Navegador & Mídias de Alta Fidelidade**: Base de dados de pastas e tipos de arquivos no navegador central atualizada para utilizar os novos sprites procedurais, e introdução de um ecrã de som ambiente com constelação ativa e visualizadores de áudio dedicados no reprodutor.
- **Identificadores das Builds**: Incremento sequencial da Build para número 13 e versão do aplicativo de mídias da TV unificada para a versão 1.2.1 em todos os manifestos de código, atualizadores e registros.

## [1.2.0] - 11/06/2026 18:45
- **Revisão Completa de Codificação**: Auditoria profunda em todos os arquivos de script e layouts Jetpack Compose, organizando formatações e polindo comentários redundantes ou typos em orientações de tela.
- **Correção de Typos e Textos**: Consertadas palavras com erros ortográficos no manual de instalação rápida do assistente de PWA do Safari para iPhones.
- **Homologação Estática**: Testes de compilação repetidos e bem-sucedidos em ritmo acelerado, eliminando possíveis avisos de compilação ou redundâncias de importação no SDK do Kotlin.
- **Identificadores das Builds**: Incremento sequencial da build para versão de número 12 e da compilação unificada do aplicativo de TV para a versão 1.2.0 em todos os manifestos de compilação, assets locais, checadores de atualização na nuvem e registros gerais de changelog.

## [1.1.9] - 11/06/2026 18:35
- **Suporte Avançado a Progressive Web App (PWA)**: Implementação de um banner de convite e sugestão de instalação elegante e não intrusivo para adicionar à tela inicial no iPhone (Safari) e Android (compatível com Chrome e Firefox).
- **Manual de Instrução Dinâmico para iOS**: Integração de uma gaveta de orientação visual passo-a-passo detalhando as ações rápidas necessárias para adicionar a aplicação às telas de início do iPhone através do botão de Compartilhar do Safari.
- **Modo Tela Cheia Autônomo**: Injeção estratégica de tags de controle específico para aparelhos Apple, habilitando a execução em tela cheia do "web app" autónomo, ocultando os controles tradicionais de navegação.
- **Identificadores das Builds**: Incremento unificado e controlado da Build para a versão de número 11 e da compilação do aplicativo de TV para a versão 1.1.9 em todos os manifestos, assets, atualizadores e registros.

## [1.1.8] - 11/06/2026 18:20
- **Conexão QR Code Premium**: Renovação total dos painéis de emparelhamento por QR Code, implementando bordas modernas com gradientes de alta fidelidade visual (azul e verde esmeralda para sinalizar o status ativo, ou laranja e vermelho para inativo).
- **QR Code Adaptado ao Tema**: Ajuste sistemático da cor de geração do QR Code via API para combinar de modo fluido com as nuances do tema selecionado, assegurando o melhor contraste e um acabamento deslumbrante.
- **Suporte a Light Mode no Host**: Adequacação visual completa do diálogo e painel Host Server ao Modo Claro e Modo Escuro, mantendo integridade da visualização em qualquer tema de uso na Android TV.
- **Identificadores das Builds**: Incremento coordenado da versão da Build (10) e versão do aplicativo (1.1.8) em todos os manifestos de compilação, assets e checadores estáticos locais e na nuvem.

## [1.1.7] - 12/06/2026 00:45
- **Aperfeiçoamento de Navegação por Foco**: Ajuste no foco dinâmico da D-pad para todos os cartões e itens do gerenciador de arquivos da TV, configurando efeitos de animações com molas elásticas (spring dynamic transitions) de zoom 1.08x e bordas destacadas com alta visibilidade.
- **Modo Claro Independente**: Separação completa da paleta cromática do modo claro (Light Mode) para total legibilidade de textos, fundos, cabeçalhos, categorias ("Ações Rápidas", "Biblioteca" e "Armazenamento") e diálogos, sem herança indesejada de backgrounds escuros hardcoded.
- **Sincronização Integrada**: Automação sistemática da versão de build (9) e de versão do aplicativo de TV (1.1.7) ao longo de todas as dependências locais e serviços do atualizador (`version-check.json`, `build.gradle.kts` e assets).

## [1.1.6] - 11/06/2026 17:06
- **Refactoring de Integridade Técnica**: Cruzamento profundo e auditoria de arquivos de código para eliminar inconsistências. Validação semântica e correção de pequenos avisos do compilador Kotlin.
- **Automação de Release**: Padronização estrita de controle de versão, incrementando sincronizadamente as configurações de Build, os metadados do `version-check.json`, o changelog dinâmico do app, os fallbacks do atualizador e os logs históricos de modificações.

## [1.1.5] - 11/06/2026 16:35
- **Sistema de Atualizações Ultrarrobusto**: Adicionado o mecanismo de "Forçar Atualização (Bypass)" direto na janela de configurações e versão. Isso permite baixar o APK mais recente de forma direta no GitHub caso ocorra qualquer dificuldade técnica ou erro com checagem de metadados, garantindo atualizações garantidas para a Android TV.
- **Sincronização de Assinatura e Build**: Sincronia aprimorada dos identificadores estáticos internos de compilação com os arquivos manifest do projeto.

## [1.1.4] - 11/06/2026 15:20
- **Sistema de Atualizações Automáticas**: Implementação de um fluxo de checagem inteligente baseado em arquivos JSON dedicados (`version-check.json`), ignorando os caches de requisição do GitHub para garantir precisão instantânea na detecção de novas compilações.
- **Navegação Ultra-Fluida**: Fluxo de retorno de foco melhorado ao entrar e retornar por subdiretórios nas abas de arquivos para TV da TCL e eixos compatíveis.
- **Gerenciamento Unificado na Busca**: Diálogos contextuais de manipulação adicionados diretamente à aba de pesquisas globais, estendendo todas as operações de mover, copiar, renomear e excluir para lá.

## [1.1.3] - Junho de 2026
- **Reprodutor Integrado**: Novo player nativo estilizado para Android TV com feedback instantâneo de streaming.
- **Controle de Mídia**: Suporte completo ao D-Pad para controlar reproduções à distância.
- **Ocultação Inteligente**: Controles do player de vídeo que se recolhem após 5 segundos de inatividade do usuário.
- **Servidor Resistente**: Implementado WakeLock no Wi-Fi para transferências ininterruptas.
- **Português Localizado**: Todo o aplicativo foi traduzido e revisto de ponta a ponta para PT-BR.

## [1.1.2] - Maio de 2026
- **Servidor Web Incorporado**: Transferências de arquivos leves por compartilhamento sem fio.
- **Código QR**: Atalho elegante para leitura imediata de navegadores móveis/iPhone/iPad.
- **Suporte OTG**: Exploração de discos rígidos e pendrives conectados à entrada de armazenamento USB externo.

## [1.1.0] - Março de 2026
- **Interface Lateral**: Barra dedicada para acesso estruturado aos recursos do sistema do televisor.
- **Indicadores de Armazenamento**: Gráficos de disco responsivos na página principal.
