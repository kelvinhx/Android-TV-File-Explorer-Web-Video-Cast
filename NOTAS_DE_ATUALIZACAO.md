# Notas de Atualização - Nexus Explorer Pro

Aqui você encontra o registro histórico de atualizações e aperfeiçoamentos do Nexus Explorer Pro.

## [1.2.9] - 13/06/2026 00:10
* **Versão do Aplicativo**: 1.2.9
* **Versão da Build**: 21
* **Data e Hora do Envio**: 13/06/2026 às 00:10 (Horário de Sincronização)
- **Remoção Total do Navegador Web Cast**: Conclusão da desativação e remoção integral de toda infraestrutura do navegador web no cliente e servidor, deixando a versão móvel focada única e exclusivamente como um explorador de arquivos de alta fidelidade e complexidade técnica intocada.
- **Aperfeiçoamento Crítico das Verificações de Atualização**: Correção definitiva do loop de instalação redundante ao comparar estritamente códigos de compilação maiores (`remoteCode > localCode`), garantindo que se o aplicativo estiver na versão mais atualizada, as caixas de avisos sejam silenciadas.
- **Integração do Sistema de Monitor de Atualizações**: Novo monitoramento em tempo real de logs de verificação (`UpdateMonitor`), possibilitando auditorias rápidas sobre a última resposta JSON do repositório no GitHub.

## [1.2.8] - 12/06/2026 21:30
* **Versão do Aplicativo**: 1.2.8
* **Versão da Build**: 20
* **Data e Hora do Envio**: 12/06/2026 às 21:30 (Horário de Sincronização)
- **Otimização Essencial de Armazenamento e Backups**: Aperfeiçoamento da inteligência de geração do instalador APK de segurança (`nexus-update-backup.apk`). Removida a cópia redundante sobressalente criada no diretório padrão `/Download` do sistema operacional para eliminar o consumo duplicado desnecessário de memória física e armazenamento em televisores e TV boxes de alta ressonância, mantendo-a disponível única e exclusivamente na pasta oficial de trabalho instalada do aplicativo (`/storage/emulated/0/Nexus Explorer`) para as mais estritas instalações manuais em caso de queda de rede ou instabilidade do Android.
- **Blindagem e Fidelidade do Higienizador de Cache**: Integração na rotina `cleanUpOldUpdates` do `Updater` de um detector de assinaturas que ignora todo e qualquer instalador remanescente que contenha a palavra de segurança `backup` em sua nomenclatura lógica. Isso assegura que futuras limpezas automáticas de buffers ou atualizações temporárias acumuladas pós-instalação preservem permanentemente intacto o arquivo de segurança manual para o usuário consultar ou reinstalar offline quando necessário.

## [1.2.7] - 12/06/2026 21:25
* **Versão do Aplicativo**: 1.2.7
* **Versão da Build**: 19
* **Data e Hora do Envio**: 12/06/2026 às 21:25 (Horário de Sincronização)
- **Backup de Segurança Automático do APK**: Implementação de um robusto pipeline de cópia de segurança para o instalador APK atualizado. Toda vez que uma atualização for baixada e descompactada com sucesso no diretório de cache isolado da aplicação, um backup de segurança (`nexus-update-backup.apk`) é automaticamente gerado e armazenado de forma permanente tanto na pasta principal do aplicativo (`/storage/emulated/0/Nexus Explorer`) quanto na pasta padrão de downloads do sistema (`/storage/emulated/0/Download`). Isso confere autonomia total ao usuário para instalar o aplicativo de forma 100% manual em caso de quaisquer instabilidades do sistema operacional ou falha de rede do instalador nativo.
- **Prevenção contra Exclusão Involuntária de Backups**: Atualização na lógica do método `cleanUpOldUpdates` do `Updater`. Ao realizar a higienização de compilações antigas ou arquivos corrompidos acumulados, o motor agora detecta, por meio de correspondência de strings, se o arquivo consiste em um APK de backup ativo. Esses arquivos são categoricamente mantidos fora do círculo de expurgação, garantindo sua visibilidade e fácil acesso a qualquer momento.

## [1.2.6] - 12/06/2026 21:15
- **Motor do Navegador Web Caster Interno Totalmente Reformulado na Web**: Evasão de segurança inteligente de bloqueadores de iframe (anti-frame-busting) adicionada por meio da redefinição estrutural das propriedades de referenciamento global (`window.top` e `window.parent` mockados para apontar diretamente para `window.self` via `Object.defineProperty`). Isso impede que scripts maliciosos de páginas web de terceiros ocultem o documento, limpem a tela ou deixem a tela preta ao detectarem sua execução interna no navegador do Caster. Adicionada injeção de estilo forçado mantendo visíveis elementos vitais do corpo da página (`display: block` e `opacity: 1`).
- **Intercepção Inteligente de Cabeçalhos e CORS Sem Limites**: Implementado no servidor Netty do Ktor um pipeline de interceptação de requisições de rede para anexar cabeçalhos de permissão de compartilhamento de recursos entre origens distintas (CORS - Cross-Origin Resource Sharing) em todas as rotas de ativos rápidos do servidor, anulando qualquer restrição do navegador de dispositivos emissores na carga de folhas de estilo, scripts JavaScript ou requisições AJAX.
- **Identificação de Vídeos Através de Transmissão por Wi-Fi**: Aprimorado o mecanismo de recepção inteligente do endpoint `/api/cast`, que agora analisa minuciosos parâmetros e gatilhos de mídia na URL de transmissão (como extensões `.mp4`, `.m3u8`, `.mpd`, termos específicos de stream, mídias do Google, etc.). Se corresponder a um fluxo de vídeo, o app abre de forma 100% nativa o player de mídias de alto desempenho com aceleração de hardware e controles na tela da TV, em vez de carregá-lo desnecessariamente no navegador web. Também extrai e mapeia dados adicionais opcionais (como o título do vídeo de forma dinâmica do corpo JSON para a TV).
- **Notificação Não Invasiva de Ativação Única no Primeiro Uso**: Adicionado um elegante card de aviso com transição animada de deslizar posicionado no topo superior direito da tela de início da TV (DPAD 100% amigável com suporte a focagem e clique de ativação rápida), que notifica o usuário sobre a configuração de Fontes Desconhecidas na primeira abertura do app. O card pode ser dispensado permanentemente pelo usuário através de salvamento local nos dados do app (`SharedPreferences`) e some instantaneamente assim que outorgado, só reaparecendo no sistema caso o utilizador retire os direitos.
- **Controle de Versões e Atualizadores Unificados**: Correção estratégica no algoritmo comparador de atualização remota do FileServer para versão unificada `1.2.6`, impedindo que o Nexus ofereça atualizações obsoletas ao usuário e permitindo a leitura de datas de build em tempo real nas notas de lançamento.

## [1.2.5] - 12/06/2026 20:31
- **Suporte Avançado e Instalação de Updates sem Reinstalação**: Migração definitiva da pasta de download de atualizações APK para o diretório de cache interno seguro e privativo do aplicativo (`context.cacheDir/updates`). Isso anula qualquer necessidade de concessão prévia de permissões profundas de armazenamento (Scoped Storage / `MANAGE_EXTERNAL_STORAGE`) para baixar e assinar a atualização do Nexus Explorer. O FileProvider compartilha as atualizações com o PackageInstaller perfeitamente, tornando as atualizações 100% integradas e sem exigir a deleção/desinstalação de versões anteriores para realizar o upgrade.
- **Solicitação Não-Invasiva de Fontes Desconhecidas**: Desenvolvimento de uma notificação extremamente polida na forma de uma mensagem/card de aviso contextual no Painel Principal da TV e de um ponto de ativação nas Configurações do app. O card só é exibido se a permissão para instalar APKs (`REQUEST_INSTALL_PACKAGES`) não estiver atribuída nas permissões de segurança da TV. Uma vez habilitada no sistema, ela some de modo 100% orgânico e reativo.
- **Aprimoramento Visual e de Área Segura (Notch do iPhone)**: Expansão do espaçamento superior no cabeçalho unificado `.liquid-header` por meio de `padding-top: calc(env(safe-area-inset-top, 38px) + 16px)`. Isso afasta de forma premium e elegante o título "Navegar" e os botões de controle de qualquer colisão física com os alto-falantes de tela, Dynamic Island e barras de status de dispositivos móveis iOS e Android com displays modernos de bordas infinitas.
- **Identificadores das Builds**: Incremento sequencial da Build para número de compilação 17 do aplicativo de mídias e atribuição da numeração geral de versão 1.2.5 unificada no Nexus.

## [1.2.4] - 12/06/2026 20:15
- **Correção Crítica no Carregamento de Arquivos via Web**: Tratamento e resolução de uma falha de sintaxe JavaScript essencial (parênteses e chaves de fechamento ausentes na definição do ouvinte de cliques do cartão de arquivos) no script do navegador. Essa falha impedia o interpretador web do telefone de processar o restante do arquivo JS, travando a inicialização de chamadas à API, ocultando pastas/mídias e deixando os relatórios de integridade (RAM e armazenamento) com valores marcadores vazios (`--`).
- **Segurança de Safe Areas para iPhone e Outros Dispositivos com Notch**: Atualização cuidadosa do seletor CSS do cabeçalho (.liquid-header) empregando preenchimento superior calculado dinamicamente com base nas margens físicas declaradas do SO (`env(safe-area-inset-top)`). Isso rebaixa o título "Navegar" e os botões de ação para fora da barra de status ocupada do telefone e do entalhe de tela (Notch/Dynamic Island), devolvendo excelente respiro de layout e impedindo sobreposição e compressão visual.
- **Resolução Estável de Contagens de Subpastas e Streams**: Ajuste nos fluxos de downloads de mídias e cálculo de tamanhos de diretórios para usar o local de armazenamento virtual mapeado (`context.filesDir/virtual_storage`) quando o sistema de TV não possui o privilégio `MANAGE_EXTERNAL_STORAGE` plenamente assinado.
- **Identificadores das Builds**: Incremento sequencial de Build para versão de compilação 16 do aplicativo de mídias e numeração geral para a versão 1.2.4 de interface unificada do Nexus.

## [1.2.3] - 12/06/2026 19:55
- **Acesso Total aos Arquivos da TV via Web**: Unificação avançada de rotas que mapeia de forma transparente caminhos físicos internos de armazenamento (/data/user/0/.../virtual_storage) de volta para o prefixo lógico '/storage/emulated/0'. Isso corrige o botão de retorno da navegação web (evitando que retorne até a raiz do Linux ou pastas restritas), resolve listagens vazias em celulares e permite download, upload, renomeação e criação de pastas com consistência absoluta de segurança.
- **Correção Completa no Clique do Browser Cast**: Ocultação inteligente e dinâmica do menu e da barra principal de abas (main tab-bar) quando o modo navegador 'Browser Cast' é selecionado. Isso elimina a sobreposição de elementos visuais no rodapé da página que impedia o clique do usuário e travava os controles do navegador (back/forward/bookmark/cast), restabelecendo o fluxo funcional total.
- **Identificadores das Builds**: Incremento sequencial da Build para número 15 e versão do aplicativo de mídias da TV unificada para a versão 1.2.3 em todos os manifestos de compilação, atualizadores, sincronizadores do GitHub, notas de lançamento e registros históricos.

## [1.2.2] - 12/06/2026 18:45
- **Aprimoramento End-to-End da Versão Web**: Correção estrutural na responsividade, cliques (click-handling) e posicionamento da barra de abas inferior e botões do Browser Cast para garantir máxima interatividade sem interrupções.
- **Sistema de Armazenamento Virtual Unificado**: Implementação de um robusto fallback automático no FileUtils para mapear '/storage/emulated/0' para 'context.filesDir/virtual_storage' em caso de sandbox ou restrição física do Android 11+ da TV, garantindo que arquivos estejam disponíveis sem barreiras na web.
- **Mídias de Demonstração Geradas Automaticamente**: Geração nativa e criação de pastas virtuais com arquivos simulados de alta fidelidade para testes imediatos de reprodução de vídeo, som, logotipo de fotos e introdução de texto.
- **Identificadores das Builds**: Incremento sequencial da Build para número 14 e versão do aplicativo de mídias da TV unificada para a versão 1.2.2 em todos os manifestos de compilação, atualizadores, logs internos e registros históricos.

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
