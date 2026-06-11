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
