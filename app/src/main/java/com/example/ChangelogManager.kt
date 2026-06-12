package com.example

import android.content.Context
import android.content.SharedPreferences

data class VersionChangelog(
    val versionName: String,
    val versionCode: Int,
    val releaseDate: String,
    val highlights: List<String>
)

object ChangelogManager {
    private const val PREFS_NAME = "nexus_changelog_prefs"
    private const val KEY_LAST_SHOWN_VERSION = "last_shown_version_code"

    val creatorName = "Kelvin Douglas"
    val creatorGithub = "https://github.com/kelvinhx"
    val projectRepository = "https://github.com/kelvinhx/Android-TV-File-Explorer-Web-Video-Cast"

    val changelogs = listOf(
        VersionChangelog(
            versionName = "1.2.6",
            versionCode = 18,
            releaseDate = "12 de Junho de 2026 (21:10)",
            highlights = listOf(
                "Nova Engine de Navegação e Web Casting: Reformulação completa do navegador web caster interno, com renderização otimizada de páginas complexas para evitar telas pretas, garantindo uma navegação 100% fluida e estável.",
                "Sincronização Avançada de Compartilhamento Wi-Fi: Ajuste na sinalização e correspondência do identificador de vídeo transmitido pelo celular, sincronizando perfeitamente as ações de play/pause/seek com a TV.",
                "Ecrã de Recepção da TV Inteligente: Aprimoramento do layout e lógica de recepção na TV para detectar gatilhos e metadados dinamicamente, iniciando streams de mídia sem atrasos.",
                "Sistema de Atualização de Software de Alta Precisão: Refinamento na detecção de versão que impede ofertas fantasmas ou redundantes caso a TV já esteja rodando a build mais recente."
            )
        ),
        VersionChangelog(
            versionName = "1.2.5",
            versionCode = 17,
            releaseDate = "12 de Junho de 2026 (20:31)",
            highlights = listOf(
                "Instalação de Updates sem Reinstalação: Migração inteligente do download de atualizações APK para o diretório privado do aplicativo (context.cacheDir/updates), liberando a instalação rápida via FileProvider sem necessidade de apagar o app.",
                "Solicitação Não-Invasiva de Fontes Desconhecidas: Exibição inteligente de um banner discreto de aviso no Painel da TV e atalho de ativação direta nas Configurações quando a permissão estiver em falta."
            )
        ),
        VersionChangelog(
            versionName = "1.2.4",
            versionCode = 16,
            releaseDate = "12 de Junho de 2026 (20:15)",
            highlights = listOf(
                "Correção Crítica no Carregamento Web: Correção de um erro de sintaxe JavaScript essencial na visualização de pastas na web, liberando a navegação de mídias e relatórios de integridade.",
                "Adaptação para iPhone e Notch (Safe Areas): Inclusão das variáveis de safe area 'env(safe-area-inset-top)' no cabeçalho, evitando colisão visual com o notch e a barra de status."
            )
        ),
        VersionChangelog(
            versionName = "1.2.3",
            versionCode = 15,
            releaseDate = "12 de Junho de 2026 (19:55)",
            highlights = listOf(
                "Acesso Total aos Arquivos da TV via Web: Unificação de caminhos virtuais e mapeabilidade estável de volta ao prefixo lógico unificado '/storage/emulated/0' para evitar quebras de navegação ou diretórios vazios.",
                "Correção Física de Cliques no Browser Cast: Esconder dinâmico da menu-bar do app quando o modo navegador 'Browser Cast' estivesse habilitado para reestabelecer o toque livre de elementos da base."
            )
        ),
        VersionChangelog(
            versionName = "1.2.2",
            versionCode = 14,
            releaseDate = "12 de Junho de 2026 (18:45)",
            highlights = listOf(
                "Aprimoramento End-to-End da Versão Web: Correção estrutural na responsividade e click-handling da barra de abas inferior e ícones do Browser Cast para garantir máxima interatividade.",
                "Sistema de Armazenamento Virtual Unificado: Implementação de um robusto fallback automático no FileUtils para mapear '/storage/emulated/0' para 'context.filesDir/virtual_storage' em caso de sandbox ou restrição, garantindo que arquivos da TV estejam acessíveis sem obstáculos na web.",
                "Geração e Criação Automatizada de Mídias de Demonstração: População nativa e criação de pastas virtuais com arquivos de testes de som, vídeo, fotos e documentos de introdução para testes imediatos.",
                "Homologação e Alinhamento Estático: Incremento de identificadores de Build para Code 14 e versão 1.2.2 de forma harmônica em todos os arquivos de configuração, manifestos virtuais, atualizadores locais e notas de atualizações."
            )
        ),
        VersionChangelog(
            versionName = "1.2.1",
            versionCode = 13,
            releaseDate = "11 de Junho de 2026 (19:15)",
            highlights = listOf(
                "Sistema de Ícones e Imagens Dinâmico: Lançamento de um motor de renderização de sprites e ícones altamente detalhados desenhados de forma procedural com Canvas nativo.",
                "Ecrã de TV Elaborado & Banners: Criação de um Hero Banner de cabeçalho na TV super refinado contendo linhas de constelação animadas, status em tempo real e polimento de bordas de vidro.",
                "Visualizador de Áudio e Som Integrado: Embutimento de um visualizador responsivo baseado no fluxo de sinal no player de mídia interna que reage elegantemente a mídias de som ambiente."
            )
        ),
        VersionChangelog(
            versionName = "1.2.0",
            versionCode = 12,
            releaseDate = "11 de Junho de 2026 (18:45)",
            highlights = listOf(
                "Integridade do Código Reforçada: Verificação geral de encoding, indentação e padrões de construção do projeto para conformidade de alto nível.",
                "Correção de Inconsistências de Texto: Ajustado o texto descritivo de instruções para iOS/Android no assistente de instalação de PWA.",
                "Compilação estática homologada: Otimização de compilação sem riscos de estouro de memória ou avisos obsoletos no Kotlin SDK."
            )
        ),
        VersionChangelog(
            versionName = "1.1.9",
            versionCode = 11,
            releaseDate = "11 de Junho de 2026 (18:35)",
            highlights = listOf(
                "Suporte Avançado a Progressive Web App (PWA): Banner e notificações não invasivas para adicionar à tela inicial no iPhone (Safari) e dispositivos Android (Chrome, Firefox).",
                "Instrução Passo-a-Passo para iOS: Integração de guias dinâmicos com símbolos nítidos para facilitar a ação de Compartilhar e Adicionar no Safari.",
                "Tag de Configuração Apple Mobile: Suporte completo para execução nativa em tela cheia sem barras de controle do navegador uma vez instalado."
            )
        ),
        VersionChangelog(
            versionName = "1.1.8",
            versionCode = 10,
            releaseDate = "11 de Junho de 2026 (18:20)",
            highlights = listOf(
                "Interface de Conexão QR-code Reformulada: Visual premium deslumbrante com bordas em gradientes dinâmicos indicando instantaneamente o status do servidor (azul/verde para ativo, laranja/vermelho para inativo).",
                "QR Code Colorido Personalizado: Geração inteligente de QR Codes que combinam com as nuances azul-esmeralda e nuances quentes da identidade do projeto, otimizando o contraste e a escaneabilidade.",
                "Modo Claro no Servidor Host: Adaptação perfeita de ambos os diálogos de compartilhamento e da dashboard principal do Host para o tema ativo (Modo Claro/Escuro)."
            )
        ),
        VersionChangelog(
            versionName = "1.1.7",
            versionCode = 9,
            releaseDate = "12 de Junho de 2026 (00:45)",
            highlights = listOf(
                "Aprimoramento completo do foco por D-pad em TVs: transições com amortecimento físico (spring animation) de 1.08x e bordas de destaque de alto contraste.",
                "Visual Premium Multi-Tema: Total independência cromática para o Modo Claro, garantindo alto contraste de legibilidade sem sobreposição de backgrounds escuros.",
                "Ajuste estrutural de backgrounds raiz e cards informativos no painel inicial para adaptação contínua."
            )
        ),
        VersionChangelog(
            versionName = "1.1.6",
            versionCode = 8,
            releaseDate = "11 de Junho de 2026 (17:06)",
            highlights = listOf(
                "Varredura de integridade de arquivos do projeto para evitar inconsistências semânticas e falhas estruturais.",
                "Consolidação da automação de Notas de Atualização e sincronismo contínuo de metadados.",
                "Compilação otimizada para melhor velocidade de execução na Android TV e uso seguro de fluxos de eventos."
            )
        ),
        VersionChangelog(
            versionName = "1.1.5",
            versionCode = 7,
            releaseDate = "11 de Junho de 2026 (16:35)",
            highlights = listOf(
                "Inclusão do mecanismo 'Forçar Atualização (Bypass)', garantindo a instalação direta mesmo em caso de erro na detecção automática das versões.",
                "Estatutos de atualização robustos para evitar lentidão ou erros no download direto do repositório GitHub.",
                "Sincronização estrita de builds e integridade dos manifestos de release para a Android TV."
            )
        ),
        VersionChangelog(
            versionName = "1.1.4",
            versionCode = 6,
            releaseDate = "11 de Junho de 2026",
            highlights = listOf(
                "Aprimoramento completo e otimização do sistema de atualizações automáticas integradas com arquivo JSON dedicado.",
                "Sincronização impecável com o histórico do GitHub sem problemas de cache.",
                "Interface de navegação por pastas com fluxo contínuo e retorno de foco rápido para o botão superior na TV.",
                "Aba de pesquisa de arquivos atualizada com menu contextual de manipulação completo (Mover, Copiar, Renomear e Excluir)."
            )
        ),
        VersionChangelog(
            versionName = "1.1.3",
            versionCode = 5,
            releaseDate = "Junho de 2026",
            highlights = listOf(
                "Novo reprodutor de mídia nativo aprimorado com interface elegante adaptada para telas de Android TV.",
                "Suporte completo ao controle remoto da sua TV: pause, retome, avance e retroceda vídeos usando o botão direcional (D-Pad).",
                "Controles de mídia flutuantes com ocultação automática inteligente após 5 segundos sem interação.",
                "Recurso de Otimização Rápida no painel de Ações Rápidas para limpar caches ociosos e liberar memória RAM na TV.",
                "Implementação de WakeLock no servidor Wi-Fi para impedir que a TV suspenda ou desligue a rede durante transferências longas.",
                "Comportamento inteligente do botão 'Voltar' do controle remoto para fechar diálogos, reprodutores e menus sem sair do app por engano.",
                "Tradução e revisão completa de toda a interface do sistema para o idioma português do Brasil."
            )
        ),
        VersionChangelog(
            versionName = "1.1.2",
            versionCode = 4,
            releaseDate = "Maio de 2026",
            highlights = listOf(
                "Inclusão do servidor web de alta performance embarcado diretamente na sua TV.",
                "Compartilhamento web inteligente para transferir qualquer arquivo do celular ou do computador diretamente do navegador.",
                "Geração local de QR Code para escaneamento rápido no celular ou tablet.",
                "Suporte integrado a dispositivos de armazenamento USB externos e HDs portáteis na dashboard."
            )
        ),
        VersionChangelog(
            versionName = "1.1.0",
            versionCode = 3,
            releaseDate = "Março de 2026",
            highlights = listOf(
                "Nova interface inicial dedicada para Android TVs TCL, Semp e compatíveis, focando em legibilidade.",
                "Barra lateral de navegação rápida para facilitar o acesso de categorias por pastas, buscas em tempo real e configurações do aplicativo.",
                "Indicadores visuais de armazenamento interno com dados detalhados e percentual de uso em tempo real."
            )
        )
    )

    /**
     * Verifica se deve exibir o pop-up de novidades para a versão atual.
     */
    fun shouldShowChangelogForCurrentVersion(context: Context, currentVersionCode: Int): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShown = prefs.getInt(KEY_LAST_SHOWN_VERSION, -1)
        return currentVersionCode > lastShown
    }

    /**
     * Registra que as novidades da versão atual foram exibidas para o usuário.
     */
    fun markChangelogAsShown(context: Context, currentVersionCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_SHOWN_VERSION, currentVersionCode).apply()
    }
}
