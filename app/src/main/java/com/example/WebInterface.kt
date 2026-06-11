package com.example

object WebInterface {
    fun getHtml(): String {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
    <title>Nexus Explorer Pro</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --ios-bg: #000000;
            --ios-card: #1C1C1E;
            --ios-card-hover: #2C2C2E;
            --ios-blue: #0A84FF;
            --ios-gray: #8E8E93;
            --ios-border: rgba(255, 255, 255, 0.1);
            --ios-tint: rgba(255, 255, 255, 0.05);
        }
        body {
            background: var(--ios-bg);
            font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
            color: #FFFFFF;
            user-select: none;
            -webkit-user-select: none;
            overflow-x: hidden;
            padding-bottom: env(safe-area-inset-bottom);
            margin: 0;
            display: flex;
            flex-direction: column;
            height: 100vh;
        }
        .ios-header {
            font-weight: 700;
            font-size: 34px;
            letter-spacing: 0.3px;
            padding: 16px 20px 8px;
            background: rgba(0, 0, 0, 0.85);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            position: sticky;
            top: 0;
            z-index: 50;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .ios-list {
            background: var(--ios-card);
            border-radius: 12px;
            margin: 0 16px 16px;
            overflow: hidden;
        }
        .ios-list-item {
            display: flex;
            align-items: center;
            padding: 12px 16px;
            border-bottom: 0.5px solid var(--ios-border);
            transition: background 0.15s;
        }
        .ios-list-item:last-child {
            border-bottom: none;
        }
        .ios-list-item:active {
            background: var(--ios-card-hover);
        }
        
        .ios-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(85px, 1fr));
            gap: 12px;
            padding: 16px;
        }
        .ios-grid-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
            background: transparent;
            border-radius: 12px;
            padding: 4px;
            transition: transform 0.1s, background 0.15s;
        }
        .ios-grid-item:active {
            transform: scale(0.92);
            background: rgba(255, 255, 255, 0.05);
        }
        
        .ios-icon-box {
            width: 65px;
            height: 65px;
            background: var(--ios-bg);
            border-radius: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 6px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            font-size: 32px;
            overflow: hidden;
        }
        .ios-folder { color: #81D4FA; } /* Light blue for iOS folder */
        .ios-file-title {
            font-size: 13px;
            font-weight: 500;
            line-height: 1.2;
            word-break: break-word;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            width: 100%;
            max-width: 90px;
        }
        .ios-file-subtitle {
            font-size: 11px;
            color: var(--ios-gray);
            margin-top: 2px;
        }

        .tab-bar {
            background: rgba(28, 28, 30, 0.9);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border-top: 0.5px solid var(--ios-border);
            display: flex;
            justify-content: space-around;
            padding: 8px 0 calc(8px + env(safe-area-inset-bottom));
            position: fixed;
            bottom: 0;
            width: 100%;
            z-index: 100;
        }
        .tab-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            color: var(--ios-gray);
            font-size: 10px;
            font-weight: 500;
            transition: color 0.2s;
            width: 33%;
        }
        .tab-item i {
            font-size: 24px;
            margin-bottom: 4px;
        }
        .tab-item.active {
            color: var(--ios-blue);
        }

        .toolbar {
            display: flex;
            align-items: center;
            padding: 8px 16px;
            background: var(--ios-bg);
        }
        .toolbar-btn {
            color: var(--ios-blue);
            font-size: 22px;
            padding: 4px 12px;
        }

        /* Context Menu */
        .context-menu {
            display: none;
            position: fixed;
            z-index: 1000;
            background: rgba(30, 30, 30, 0.85);
            backdrop-filter: blur(25px);
            -webkit-backdrop-filter: blur(25px);
            border: 0.5px solid rgba(255, 255, 255, 0.15);
            border-radius: 14px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
            width: 250px;
            animation: contextReveal 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }
        @keyframes contextReveal {
            from { transform: scale(0.9) translateY(10px); opacity: 0; }
            to { transform: scale(1) translateY(0); opacity: 1; }
        }
        .context-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            width: 100%;
            padding: 14px 16px;
            font-size: 16px;
            border-bottom: 0.5px solid rgba(255,255,255,0.1);
        }
        .context-item:last-child {
            border-bottom: none;
        }
        .context-item:active {
            background: rgba(255, 255, 255, 0.1);
        }
        .context-item.danger {
            color: var(--ios-red);
        }

        .main-content {
            flex: 1;
            overflow-y: auto;
            padding-bottom: 80px;
        }

        .search-bar {
            background: var(--ios-card);
            border-radius: 10px;
            margin: 0 16px 16px;
            padding: 8px 12px;
            display: flex;
            align-items: center;
            gap: 8px;
            color: var(--ios-gray);
        }
        .search-bar input {
            background: transparent;
            border: none;
            color: white;
            flex: 1;
            font-size: 16px;
            outline: none;
        }

        /* iOS Modal setup */
        .modal-overlay {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0,0,0,0.4);
            z-index: 200;
            display: none;
            align-items: flex-end;
        }
        .modal-sheet {
            background: var(--ios-card);
            border-top-left-radius: 20px;
            border-top-right-radius: 20px;
            width: 100%;
            padding: 20px;
            padding-bottom: calc(20px + env(safe-area-inset-bottom));
            transform: translateY(100%);
            transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        }
        .modal-overlay.active .modal-sheet {
            transform: translateY(0);
        }

        /* Remote D-Pad */
        .dpad-container {
            width: 260px;
            height: 260px;
            background: var(--ios-card);
            border-radius: 50%;
            position: relative;
            margin: 40px auto;
            box-shadow: inset 0 0 0 1px rgba(255,255,255,0.05), 0 10px 30px rgba(0,0,0,0.5);
        }
        .dpad-btn {
            position: absolute;
            width: 33%;
            height: 33%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            color: var(--ios-gray);
        }
        .dpad-btn:active {
            color: white;
            background: rgba(255,255,255,0.1);
        }
        .dpad-up { top: 0; left: 33.3%; border-radius: 50% 50% 0 0; }
        .dpad-down { bottom: 0; left: 33.3%; border-radius: 0 0 50% 50%; }
        .dpad-left { top: 33.3%; left: 0; border-radius: 50% 0 0 50%; }
        .dpad-right { top: 33.3%; right: 0; border-radius: 0 50% 50% 0; }
        .dpad-ok {
            top: 33.3%; left: 33.3%;
            border-radius: 50%;
            background: var(--ios-bg);
            box-shadow: inset 0 0 0 1px rgba(255,255,255,0.1);
            color: white;
            font-weight: 600;
            font-size: 16px;
        }

        ::-webkit-scrollbar { display: none; }
    </style>
</head>
<body>

    <!-- Header -->
    <header class="ios-header" id="main-header">
        <div class="flex items-center gap-2">
            <span>Navegar</span>
            <span class="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-[pulse_1.5s_infinite] shadow-[0_0_8px_#10b981]" title="Conectado à TV"></span>
        </div>
        <button class="text-[var(--ios-blue)] text-lg" onclick="openSettingsModal()"><i class="fa-solid fa-ellipsis-circle"></i></button>
    </header>

    <!-- Toolbar for Files -->
    <div class="toolbar" id="files-toolbar">
        <button class="toolbar-btn" onclick="navigateBack()" id="back-btn"><i class="fa-solid fa-chevron-left"></i></button>
        <div class="flex-1 text-center font-semibold text-[15px] truncate px-4" id="path-title">Na minha TV</div>
        <input type="file" id="file-uploader" class="hidden" multiple onchange="handleUpload(this.files)">
        <button class="toolbar-btn text-sm" onclick="document.getElementById('file-uploader').click()"><i class="fa-solid fa-plus text-xl"></i></button>
        <button class="toolbar-btn text-sm" onclick="createNewFolder()"><i class="fa-solid fa-folder-plus text-xl"></i></button>
    </div>

    <!-- Main Content Area -->
    <div class="main-content" id="content-area">
        
        <!-- Tab 1: Files -->
        <div id="view-files">
            <div class="search-bar">
                <i class="fa-solid fa-magnifying-glass"></i>
                <input type="text" id="search-input" placeholder="Buscar arquivos...">
                <i class="fa-solid fa-microphone" id="mic-btn" style="cursor: pointer;" onclick="startSpeechRecognition()"></i>
            </div>

            <!-- TV Health status bar -->
            <div id="tv-status-bar" class="grid grid-cols-2 gap-3 mx-4 mb-4">
                <div class="bg-[var(--ios-card)] px-3 py-2.5 rounded-xl border border-[rgba(255,255,255,0.05)] flex items-center justify-between">
                    <div>
                        <div class="text-[10px] text-[var(--ios-gray)] font-semibold uppercase tracking-wider">Memória da TV</div>
                        <div class="text-[12px] font-bold mt-0.5 text-white" id="tv-ram-badge">RAM Carregada</div>
                    </div>
                    <div class="w-8 h-8 rounded-full border-2 border-stone-800 flex items-center justify-center text-[9px] font-extrabold text-[var(--ios-blue)]" id="tv-ram-ring">
                        --
                    </div>
                </div>
                <div class="bg-[var(--ios-card)] px-3 py-2.5 rounded-xl border border-[rgba(255,255,255,0.05)] flex items-center justify-between">
                    <div>
                        <div class="text-[10px] text-[var(--ios-gray)] font-semibold uppercase tracking-wider">Armazenamento</div>
                        <div class="text-[11px] font-bold mt-0.5 text-emerald-400 truncate max-w-[80px]" id="tv-storage-badge">Status do Disco</div>
                    </div>
                    <div class="w-8 h-8 rounded-full border-2 border-stone-800 flex items-center justify-center text-[9px] font-extrabold text-[#34C759]" id="tv-storage-ring">
                        --
                    </div>
                </div>
            </div>
            
            <div id="upload-progress-container" class="hidden ios-list mb-4 p-4 text-sm font-medium">
                <div class="flex justify-between mb-2"><span>Enviando...</span><span id="upload-percent">0%</span></div>
                <div class="h-1.5 bg-stone-800 rounded-full overflow-hidden">
                    <div id="upload-bar" class="h-full bg-[var(--ios-blue)] w-0 transition-all"></div>
                </div>
            </div>

            <!-- iOS Grid View -->
            <div class="ios-grid" id="files-list">
                <!-- Dynamically populated files -->
            </div>
        </div>

        <!-- Tab 3: Browser Cast -->
        <div id="view-browser" class="hidden flex-col fixed inset-0 z-[150] bg-[#1E1E1E]">
            
            <!-- Safari Top Address Bar -->
            <div class="bg-[#1E1E1E] pt-[max(env(safe-area-inset-top),32px)] pb-2 px-3 w-full shrink-0 flex items-center justify-between z-10 shadow-sm border-b border-[rgba(255,255,255,0.05)]">
                <div class="flex items-center bg-[#2C2C2E] rounded-xl h-10 w-full px-3">
                    <button onclick="toggleStartPage()" class="text-white text-sm active:opacity-50 flex items-center justify-center w-8 h-full font-serif tracking-normal">
                        <span class="text-[10px]">a</span><span class="text-[14px]">A</span>
                    </button>
                    <div class="flex-1 h-full flex items-center justify-center relative">
                        <i class="fa-solid fa-lock text-[10px] text-gray-400 absolute left-2 top-1/2 -translate-y-1/2" id="browser-lock-icon"></i>
                        <input type="text" id="browser-url" class="bg-transparent text-white w-full h-full outline-none text-center focus:text-left text-[15px] font-medium pl-6 pr-2" value="" placeholder="Pesquisar ou digitar site">
                    </div>
                    <button onclick="loadInternalBrowser()" class="text-gray-400 text-lg active:opacity-50 flex items-center justify-center w-8 h-full">
                        <i class="fa-solid fa-rotate-right text-sm"></i>
                    </button>
                </div>
            </div>

            <div id="video-sniffer-bar" class="hidden bg-[#ff3b30] text-white px-4 py-3 text-sm flex justify-between items-center shadow-lg shrink-0 w-full border-b border-[#ff3b30] z-20 relative">
                <span class="font-bold flex items-center"><i class="fa-solid fa-play-circle mr-2 animate-pulse text-lg"></i> Vídeo Encontrado!</span>
                <button onclick="castSniffedVideo()" class="bg-white text-[#ff3b30] px-4 py-2 rounded-full font-bold shadow-sm active:opacity-70 flex items-center gap-2"><i class="fa-solid fa-tv"></i> Cast TV</button>
            </div>

            <!-- Start Page (Bookmarks/History) -->
            <div id="browser-start-page" class="flex-1 w-full bg-[#1E1E1E] p-6 overflow-y-auto hidden relative z-10">
                <div class="max-w-md mx-auto">
                    <h2 class="text-3xl font-bold mb-6 text-white text-center mt-4">Navegador</h2>
                    
                    <div class="grid grid-cols-4 gap-4 mb-8" id="favorite-icons">
                        <!-- Example quick icons -->
                        <div class="flex flex-col items-center gap-2 cursor-pointer" onclick="openUrl('https://google.com')">
                            <div class="w-14 h-14 bg-white rounded-xl flex items-center justify-center shadow-sm">
                                <i class="fa-brands fa-google text-2xl text-black"></i>
                            </div>
                            <span class="text-xs text-white">Google</span>
                        </div>
                        <div class="flex flex-col items-center gap-2 cursor-pointer" onclick="openUrl('https://youtube.com')">
                            <div class="w-14 h-14 bg-white rounded-xl flex items-center justify-center shadow-sm">
                                <i class="fa-brands fa-youtube text-2xl text-red-600"></i>
                            </div>
                            <span class="text-xs text-white">YouTube</span>
                        </div>
                    </div>

                    <h3 class="font-bold text-gray-400 mb-3 text-sm uppercase px-2">Favoritos</h3>
                    <div class="ios-list mb-8 bg-[#2C2C2E] rounded-xl overflow-hidden" id="bookmarks-list">
                        <!-- Populated by JS -->
                    </div>

                    <h3 class="font-bold text-gray-400 mb-3 text-sm uppercase px-2">Histórico Recente</h3>
                    <div class="ios-list bg-[#2C2C2E] rounded-xl overflow-hidden" id="history-list">
                        <!-- Populated by JS -->
                    </div>
                </div>
            </div>

            <!-- Web Frame -->
            <div class="flex-1 relative bg-white w-full h-full overflow-hidden" id="browser-frame-container">
                <iframe id="internal-browser" class="w-full h-full border-none absolute inset-0 bg-[#1E1E1E]" sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-downloads allow-presentation"></iframe>
                <div id="browser-loading" class="absolute inset-0 bg-[rgba(30,30,30,0.8)] flex flex-col items-center justify-center hidden z-20 backdrop-blur-md">
                    <i class="fa-solid fa-circle-notch fa-spin text-5xl text-blue-500 mb-4"></i>
                    <span class="text-[10px] font-bold tracking-widest text-blue-500 mt-2">CARREGANDO</span>
                </div>
            </div>
            
            <!-- Safari iOS Bottom Bar -->
            <div class="bg-[#1C1C1E] border-t border-[rgba(255,255,255,0.05)] h-[83px] w-full shrink-0 flex items-start justify-between px-6 pt-2 pb-[max(env(safe-area-inset-bottom),20px)] z-10 transition-transform duration-300">
                <button onclick="document.getElementById('internal-browser').contentWindow.history.back()" class="text-blue-500 text-3xl active:opacity-50 disabled:opacity-30"><i class="fa-solid fa-angle-left"></i></button>
                <button onclick="document.getElementById('internal-browser').contentWindow.history.forward()" class="text-blue-500 text-3xl active:opacity-50 disabled:opacity-30"><i class="fa-solid fa-angle-right"></i></button>
                <button onclick="castCurrentPage()" class="text-blue-500 text-2xl active:opacity-50"><i class="fa-regular fa-share-from-square"></i></button>
                <button onclick="addBookmark()" class="text-blue-500 text-2xl active:opacity-50"><i class="fa-solid fa-book-open" id="bookmark-icon"></i></button>
                <button onclick="toggleView('files')" class="text-blue-500 text-[22px] active:opacity-50 mt-0.5"><i class="fa-regular fa-clone"></i></button>
            </div>
        </div>

    </div>

    <!-- Tab Bar -->
    <div class="tab-bar">
        <button id="tab-files" onclick="toggleView('files')" class="tab-item active" style="width: 50%;">
            <i class="fa-solid fa-folder"></i>
            <span>Navegar</span>
        </button>
        <button id="tab-browser" onclick="toggleView('browser')" class="tab-item" style="width: 50%;">
            <i class="fa-solid fa-compass"></i>
            <span>Browser Cast</span>
        </button>
    </div>

    <!-- Context Menu -->
    <div id="context-menu" class="context-menu">
        <input type="hidden" id="context-file-path" value="">
        <input type="hidden" id="context-file-type" value="">
        
        <button onclick="execContextAction('OPEN')" class="context-item">
            <span>Abrir na TV</span>
            <i class="fa-solid fa-play text-[var(--ios-blue)]"></i>
        </button>
        <div class="h-[1px] bg-[rgba(255,255,255,0.1)] w-full"></div>
        <button onclick="execContextAction('STREAM')" class="context-item">
            <span>Ver no Celular</span>
            <i class="fa-solid fa-mobile-screen-button text-purple-400"></i>
        </button>
        <div class="h-[1px] bg-[rgba(255,255,255,0.1)] w-full"></div>
        <button id="context-download-btn" onclick="execContextAction('DOWNLOAD')" class="context-item">
            <span>Baixar</span>
            <i class="fa-solid fa-cloud-arrow-down text-emerald-400"></i>
        </button>
        <div class="h-[1px] bg-[rgba(255,255,255,0.1)] w-full"></div>
        <button onclick="execContextAction('CUT')" class="context-item">
            <span>Recortar</span>
            <i class="fa-solid fa-scissors text-amber-500"></i>
        </button>
        <div class="h-[1px] bg-[rgba(255,255,255,0.1)] w-full"></div>
        <button onclick="execContextAction('COPY')" class="context-item">
            <span>Copiar</span>
            <i class="fa-solid fa-copy text-teal-400"></i>
        </button>
        <div class="h-[1px] bg-[rgba(255,255,255,0.1)] w-full"></div>
        <button onclick="execContextAction('RENAME')" class="context-item">
            <span>Renomear</span>
            <i class="fa-solid fa-pen text-sky-400"></i>
        </button>
        <div class="h-[1px] bg-[rgba(255,255,255,0.1)] w-full"></div>
        <button onclick="execContextAction('DELETE')" class="context-item danger">
            <span>Excluir</span>
            <i class="fa-solid fa-trash-can text-red-500"></i>
        </button>
    </div>

    <!-- Floating Clipboard Bar -->
    <div id="clipboard-bar" class="hidden fixed bottom-24 left-4 right-4 bg-[#1C1C1E] border border-[var(--ios-blue)] rounded-xl p-3 flex justify-between items-center shadow-2xl z-50">
        <div class="flex items-center gap-3">
            <i class="fa-solid fa-scissors text-[var(--ios-blue)] text-lg animate-pulse" id="clipboard-icon"></i>
            <div class="text-xs">
                <div class="font-bold text-white text-ellipsis overflow-hidden max-w-[150px]" id="clipboard-item-name">Item</div>
                <div class="text-gray-400" id="clipboard-mode-text">Pronto para mover</div>
            </div>
        </div>
        <div class="flex gap-2">
            <button onclick="clearClipboard()" class="bg-stone-800 text-stone-300 font-semibold px-3 py-1.5 rounded-lg text-xs">Cancelar</button>
            <button onclick="pasteClipboard()" class="bg-[var(--ios-blue)] text-white font-semibold px-4 py-1.5 rounded-lg text-xs flex items-center gap-1">
                <i class="fa-solid fa-paste"></i>
                <span>Colar</span>
            </button>
        </div>
    </div>

    <!-- Streaming Video/Audio Player & ImageViewer Modal Sheet -->
    <div id="media-modal" class="modal-overlay" onclick="closeMediaModal(event)">
        <div class="modal-sheet" onclick="event.stopPropagation()">
            <div class="w-10 h-1.5 bg-[var(--ios-gray)] rounded-full mx-auto mb-4"></div>
            <h3 class="text-lg font-bold mb-3 text-center truncate" id="media-title">Transmitindo</h3>
            <div class="bg-black rounded-xl overflow-hidden w-full flex items-center justify-center min-h-[220px]" id="media-container">
                <!-- Dynamic target player goes here -->
            </div>
            <button onclick="closeMediaModal('close')" class="w-full bg-[var(--ios-card-hover)] py-3 rounded-xl text-white font-bold text-lg mt-6">Fechar Reprodutor</button>
        </div>
    </div>

    <!-- Settings Modal Sheet -->
    <div id="settings-modal" class="modal-overlay" onclick="closeSettingsModal(event)">
        <div class="modal-sheet" onclick="event.stopPropagation()">
            <div class="w-10 h-1.5 bg-[var(--ios-gray)] rounded-full mx-auto mb-6"></div>
            <h2 class="text-xl font-bold mb-4">Configurações</h2>
            <div class="ios-list bg-[var(--ios-bg)] border border-[rgba(255,255,255,0.1)]">
                <div class="ios-list-item justify-between">
                    <span>Mostrar Itens Ocultos</span>
                    <input type="checkbox" id="toggle-hidden" class="accent-[var(--ios-blue)] scale-125">
                </div>
                <div class="ios-list-item justify-between">
                    <span>Visualização em Grade</span>
                    <input type="checkbox" id="toggle-grid" checked class="accent-[var(--ios-blue)] scale-125">
                </div>
            </div>
            
            <h2 class="text-sm font-bold text-gray-400 mt-6 mb-2">Sistema da Android TV</h2>
            <div class="ios-list bg-[var(--ios-bg)] border border-[rgba(255,255,255,0.1)]">
                <div onclick="sendRemoteCmd('SETTINGS'); closeSettingsModal('close');" class="ios-list-item justify-between cursor-pointer active:bg-[rgba(255,255,255,0.1)]">
                    <span>Abrir Configurações da TV</span>
                    <i class="fa-solid fa-chevron-right text-gray-500"></i>
                </div>
                <div onclick="sendRemoteCmd('HOME'); closeSettingsModal('close');" class="ios-list-item justify-between cursor-pointer active:bg-[rgba(255,255,255,0.1)]">
                    <span>Ir para Tela Inicial da TV</span>
                    <i class="fa-solid fa-chevron-right text-gray-500"></i>
                </div>
            </div>
            <button onclick="closeSettingsModal('close')" class="w-full bg-[var(--ios-card)] py-3 rounded-xl text-[var(--ios-blue)] font-bold text-lg mt-4">Concluído</button>
        </div>
    </div>

    <div id="toast-container" class="fixed top-4 left-1/2 -translate-x-1/2 z-[100] flex flex-col items-center gap-2 pointer-events-none"></div>

    <script>
        function showNotification(msg, isError = false) {
            const container = document.getElementById('toast-container');
            const toast = document.createElement('div');
            const bgClass = isError ? 'bg-red-600' : 'bg-green-600';
            const iconClass = isError ? 'fa-circle-xmark' : 'fa-circle-check';
            toast.className = 'px-4 py-2 rounded-full shadow-lg text-white text-sm font-bold flex items-center gap-2 transform transition-all duration-300 opacity-0 translate-y-[-20px] ' + bgClass;
            toast.innerHTML = '<i class="fa-solid ' + iconClass + '"></i> ' + msg;
            container.appendChild(toast);
            
            // Animate in
            setTimeout(() => {
                toast.classList.remove('opacity-0', 'translate-y-[-20px]');
                toast.classList.add('opacity-100', 'translate-y-0');
            }, 10);
            
            // Animate out
            setTimeout(() => {
                toast.classList.add('opacity-0', 'translate-y-[-20px]');
                setTimeout(() => toast.remove(), 300);
            }, 3000);
            
            if (!isError) doVibrate(30);
        }
        let currentPath = "/storage/emulated/0";
        let activeView = "files";
        let longPressTimer = null;
        let isLongPressActive = false;
        let clipboard = null; // Stores { action: 'COPY'|'MOVE', path: '..', name: '..' }

        let searchTimeout = null;

        document.addEventListener('DOMContentLoaded', () => {
            fetchFiles();
            
            // Server-side search filter
            document.getElementById('search-input').addEventListener('input', (e) => {
                const query = e.target.value.trim();
                clearTimeout(searchTimeout);
                if (query === '') {
                    document.getElementById('path-title').style.display = 'block';
                    fetchFiles();
                    return;
                }
                
                searchTimeout = setTimeout(() => {
                    doSearch(query);
                }, 800);
            });

            document.getElementById('search-input').addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    clearTimeout(searchTimeout);
                    const query = e.target.value.trim();
                    if (query !== '') doSearch(query);
                    e.target.blur();
                }
            });

            // Close context menu on anywhere click
            document.addEventListener('click', (e) => {
                const menu = document.getElementById('context-menu');
                if (menu && !menu.contains(e.target)) {
                    menu.style.display = 'none';
                }
            });
        });

        function doSearch(query) {
            document.getElementById('path-title').textContent = "Buscando...";
            document.getElementById('path-title').style.display = 'block';
            document.getElementById('files-list').innerHTML = `
                <div class="col-span-full text-center py-20">
                    <i class="fa-solid fa-circle-notch fa-spin text-3xl text-[var(--ios-blue)] mb-4"></i>
                    <div class="text-[var(--ios-gray)] text-sm">Procurando no sistema...</div>
                </div>
            `;
            
            fetch('/api/search?q=' + encodeURIComponent(query))
                .then(res => res.json())
                .then(data => {
                    document.getElementById('path-title').textContent = "Resultados da Busca";
                    const backBtn = document.getElementById('back-btn');
                    backBtn.style.visibility = 'visible';
                    // clear currentPath so back button resets
                    currentPath = "/storage/emulated/0"; 
                    
                    renderFileList(data.status, data.message, data.files);
                }).catch(err => {
                    document.getElementById('path-title').textContent = "Erro na Busca";
                    renderFileList("error", "Erro de conexão", []);
                });
        }
        
        function startSpeechRecognition() {
            if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
                showNotification("Busca por voz não suportada neste navegador.", true);
                return;
            }
            
            const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
            const recognition = new SpeechRecognition();
            recognition.lang = 'pt-BR';
            recognition.interimResults = false;
            recognition.maxAlternatives = 1;
            
            const micBtn = document.getElementById('mic-btn');
            micBtn.style.color = '#ff3b30';
            
            recognition.onresult = (event) => {
                const query = event.results[0][0].transcript;
                document.getElementById('search-input').value = query;
                doSearch(query);
            };
            
            recognition.onerror = (event) => {
                showNotification("Erro na captura de voz.", true);
            };
            
            recognition.onend = () => {
                micBtn.style.color = '';
            };
            
            recognition.start();
        }

        function doVibrate(duration = 60) {
            if ("vibrate" in navigator) {
                if (typeof duration === 'number') {
                    if (duration > 50) {
                        navigator.vibrate([15, 20, 15]); // Simulated haptic pop
                    } else {
                        navigator.vibrate([10, 15, 10]); // Simulated light tap
                    }
                } else {
                    navigator.vibrate(duration);
                }
            }
        }

        function openSettingsModal() {
            document.getElementById('settings-modal').style.display = 'flex';
            setTimeout(() => {
                document.getElementById('settings-modal').classList.add('active');
            }, 10);
        }

        function closeSettingsModal(e) {
            if (e === 'close' || e.target.id === 'settings-modal') {
                document.getElementById('settings-modal').classList.remove('active');
                setTimeout(() => {
                    document.getElementById('settings-modal').style.display = 'none';
                }, 300);
            }
        }

        function toggleView(view) {
            doVibrate(30);
            activeView = view;
            
            document.getElementById('view-files').classList.add('hidden');
            document.getElementById('view-browser').classList.add('hidden');
            document.getElementById('view-browser').classList.remove('flex');
            
            document.getElementById('tab-files').classList.remove('active');
            document.getElementById('tab-browser').classList.remove('active');

            if (view === 'files') {
                document.getElementById('view-files').classList.remove('hidden');
                document.getElementById('tab-files').classList.add('active');
                document.getElementById('main-header').firstElementChild.textContent = "Navegar";
                document.getElementById('main-header').style.display = "flex";
                document.getElementById('files-toolbar').style.display = "flex";
                fetchFiles();
            } else if (view === 'browser') {
                document.getElementById('view-browser').classList.remove('hidden');
                document.getElementById('view-browser').classList.add('flex');
                document.getElementById('tab-browser').classList.add('active');
                document.getElementById('main-header').style.display = "none";
                document.getElementById('files-toolbar').style.display = "none";
                
                const iframe = document.getElementById('internal-browser');
                if (!iframe.src || iframe.src === window.location.href) {
                    toggleStartPage(true);
                }
            }
        }

        let sniffedVideoUrl = null;
        let isStartPage = false;
        
        function toggleStartPage(forceShow = null) {
            if (forceShow === true) {
                isStartPage = true;
            } else if (forceShow === false) {
                isStartPage = false;
            } else {
                isStartPage = !isStartPage;
            }
            if (isStartPage) {
                document.getElementById('browser-frame-container').classList.add('hidden');
                document.getElementById('browser-start-page').classList.remove('hidden');
                loadStartPage();
            } else {
                document.getElementById('browser-frame-container').classList.remove('hidden');
                document.getElementById('browser-start-page').classList.add('hidden');
            }
        }

        function loadInternalBrowser() {
            let url = document.getElementById('browser-url').value.trim();
            if(!url) return;
            
            // Allow searching with google
            if (!url.startsWith('http://') && !url.startsWith('https://')) {
                if(url.includes('.') && !url.includes(' ')) {
                    url = 'https://' + url;
                } else {
                    url = 'https://www.google.com/search?q=' + encodeURIComponent(url);
                }
            }
            
            document.getElementById('browser-url').value = url;
            
            toggleStartPage(false);
            
            document.getElementById('browser-loading').classList.remove('hidden');
            document.getElementById('internal-browser').src = '/api/proxy?url=' + encodeURIComponent(url);
            
            saveToHistory(url);
        }

        function saveToHistory(url) {
            let history = JSON.parse(localStorage.getItem('browser_history') || '[]');
            history = history.filter(item => item !== url); // Remove duplicate
            history.unshift(url);
            if(history.length > 20) history.pop();
            localStorage.setItem('browser_history', JSON.stringify(history));
        }

        function addBookmark() {
            let url = document.getElementById('browser-url').value.trim();
            if(!url || !url.startsWith('http')) return;
            let bookmarks = JSON.parse(localStorage.getItem('browser_bookmarks') || '[]');
            
            const idx = bookmarks.indexOf(url);
            if(idx > -1) {
                bookmarks.splice(idx, 1);
                showNotification("Removido dos favoritos");
            } else {
                bookmarks.unshift(url);
                showNotification("Adicionado aos favoritos!");
            }
            localStorage.setItem('browser_bookmarks', JSON.stringify(bookmarks));
            loadStartPage();
        }

        function loadStartPage() {
            let bookmarks = JSON.parse(localStorage.getItem('browser_bookmarks') || '[]');
            let history = JSON.parse(localStorage.getItem('browser_history') || '[]');
            
            const bList = document.getElementById('bookmarks-list');
            bList.innerHTML = bookmarks.length === 0 ? '<div class="p-4 text-center text-gray-500 text-sm">Nenhum favorito</div>' : '';
            bookmarks.forEach(url => {
                bList.innerHTML += '<div onclick="openUrl(\'' + url + '\')" class="ios-list-item justify-between cursor-pointer"><span class="truncate pr-4">' + url + '</span><i class="fa-solid fa-chevron-right text-gray-500"></i></div>';
            });

            const hList = document.getElementById('history-list');
            hList.innerHTML = history.length === 0 ? '<div class="p-4 text-center text-gray-500 text-sm">Nenhum histórico</div>' : '';
            history.forEach(url => {
                hList.innerHTML += '<div onclick="openUrl(\'' + url + '\')" class="ios-list-item justify-between cursor-pointer"><span class="truncate pr-4">' + url + '</span><i class="fa-solid fa-chevron-right text-gray-500"></i></div>';
            });
        }
        
        function openUrl(url) {
            document.getElementById('browser-url').value = url;
            loadInternalBrowser();
        }

        document.getElementById('internal-browser').addEventListener('load', function() {
            document.getElementById('browser-loading').classList.add('hidden');
        });

        document.getElementById('browser-url').addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                e.target.blur();
                loadInternalBrowser();
            }
        });

        window.addEventListener('message', function(e) {
            if (e.data) {
                if (e.data.type === 'video_found') {
                    sniffedVideoUrl = e.data.url;
                    document.getElementById('video-sniffer-bar').classList.remove('hidden');
                    doVibrate(60);
                } else if (e.data.type === 'navigate') {
                    document.getElementById('browser-url').value = e.data.url;
                    loadInternalBrowser();
                }
            }
        });

        function castSniffedVideo() {
            if (sniffedVideoUrl) {
                doVibrate(50);
                fetch('/api/cast', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ url: sniffedVideoUrl })
                })
                .then(res => res.json())
                .then(data => {
                    if (data.status === 'success') {
                        document.getElementById('video-sniffer-bar').classList.add('hidden');
                        showNotification("O vídeo foi enviado à sua TV!");
                    } else {
                        showNotification("Falha na transmissão: " + data.message, true);
                    }
                }).catch(err => showNotification("Erro de conexão", true));
            }
        }
        
        function castCurrentPage() {
            let url = document.getElementById('browser-url').value;
            if (!url) return;
            if (!url.startsWith('http')) url = 'https://' + url;
            
            fetch('/api/cast', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ url: url })
            })
            .then(res => res.json())
            .then(data => {
                if (data.status === 'success') {
                    showNotification("Página aberta na TV!");
                } else {
                    showNotification('Falha: ' + data.message, true);
                }
            }).catch(err => showNotification('Erro na conexão', true));
        }


        function fetchFiles() {
            fetch('/api/files?path=' + encodeURIComponent(currentPath))
                .then(res => res.json())
                .then(data => {
                    currentPath = data.currentPath;
                    let pathParts = currentPath.split('/');
                    let folderName = pathParts[pathParts.length - 1];
                    if (currentPath === "/storage/emulated/0") folderName = "Na minha TV";
                    
                    document.getElementById('path-title').textContent = folderName;
                    
                    const backBtn = document.getElementById('back-btn');
                    if (data.isRoot) backBtn.style.visibility = 'hidden';
                    else backBtn.style.visib                    // Update hardware metrics overlay
                    if (data.ramPercent !== undefined) {
                        document.getElementById('tv-ram-badge').textContent = data.ramPercent + "% Carregada";
                        document.getElementById('tv-ram-ring').textContent = data.ramPercent + "%";
                        document.getElementById('tv-storage-badge').textContent = data.storageFreeFormatted + " Livre";
                        document.getElementById('tv-storage-ring').textContent = data.storagePercent + "%";
                    }

                    renderFileList(data.status, data.message, data.files);
                }).catch(console.error);
        }

        function renderFileList(status, message, filesArray) {
            const list = document.getElementById('files-list');
            list.innerHTML = "";

            if (status === 'error') {
                document.getElementById('files-list').innerHTML = `<div class="col-span-full text-center py-20 text-red-500 font-bold">${"$"}{message || 'Erro'}</div>`;
                return;
            }

            if (!filesArray || filesArray.length === 0) {
                list.innerHTML = `
                    <div class="col-span-full text-center py-20 text-[var(--ios-gray)]">
                        <span class="text-sm">A pasta ou busca está vazia.</span>
                    </div>
                `;
                return;
            }

            filesArray.forEach(file => {
                const card = document.createElement('div');
                card.className = "ios-grid-item";
                
                // Set up haptic touch event logic for Long Press menu
                card.addEventListener('touchstart', (e) => {
                    isLongPressActive = false;
                    longPressTimer = setTimeout(() => {
                        isLongPressActive = true;
                        doVibrate(65);
                        showContextMenu(e, file);
                    }, 500);
                }, { passive: true });

                card.addEventListener('touchend', () => clearTimeout(longPressTimer), { passive: true });
                card.addEventListener('touchmove', () => clearTimeout(longPressTimer), { passive: true });

                card.addEventListener('click', (e) => {
                    if (isLongPressActive) {
                        isLongPressActive = false;
                        return;
                    }
                    doVibrate(30);
                    if (file.isDirectory) {
                        const searchInput = document.getElementById('search-input');
                        if (searchInput) searchInput.value = "";
                        currentPath = file.path || file.absolutePath;
                        fetchFiles();
                    } else {
                        requestOpenFile(file.path || file.absolutePath);
                    }
                });

                let iconClass = "fa-solid fa-file";
                let iconColor = "text-[#8E8E93]"; // iOS Gray
                let isImg = false;

                const nameLower = file.name.toLowerCase();
                const absolutePath = file.path || file.absolutePath;
                if (file.isDirectory) {
                    iconClass = "fa-solid fa-folder";
                    iconColor = "ios-folder";
                } else if (nameLower.endsWith('.apk')) {
                    iconClass = "fa-brands fa-android";
                    iconColor = "text-[#34C759]"; // iOS Green
                } else if (['.mp4', '.mkv', '.webm', '.mov', '.avi', '.ts', '.m3u8'].some(el => nameLower.endsWith(el))) {
                    iconClass = "fa-solid fa-circle-play";
                    iconColor = "text-[#AF52DE]"; // iOS Purple
                } else if (['.mp3', '.wav', '.aac', '.flac', '.ogg', '.m4a'].some(el => nameLower.endsWith(el))) {
                    iconClass = "fa-solid fa-music";
                    iconColor = "text-[#FF2D55]"; // iOS Pink
                } else if (['.zip', '.rar', '.7z', '.tar', '.gz'].some(el => nameLower.endsWith(el))) {
                    iconClass = "fa-solid fa-file-zipper";
                    iconColor = "text-[#FF9500]"; // iOS Amber
                } else if (nameLower.endsWith('.pdf')) {
                    iconClass = "fa-solid fa-file-pdf";
                    iconColor = "text-[#FF3B30]"; // iOS Red
                } else if (['.txt', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.json', '.xml', '.html', '.css', '.js', '.kt'].some(el => nameLower.endsWith(el))) {
                    iconClass = "fa-solid fa-file-lines";
                    iconColor = "text-[#0A84FF]"; // iOS Blue
                } else if (['.jpg', '.jpeg', '.png', '.webp', '.gif'].some(el => nameLower.endsWith(el))) {
                    isImg = true;
                }

                let iconHtml = "";
                if (isImg) {
                    iconHtml = '<img src="/api/download?path=' + encodeURIComponent(absolutePath) + '" class="w-full h-full object-cover rounded-lg">';
                } else {
                    iconHtml = '<i class="' + iconClass + ' ' + iconColor + '"></i>';
                }

                const displaySize = file.isDirectory ? "Pasta" : (file.lengthFormatted || file.sizeFormatted);
                card.innerHTML = '\n' +
'                            <div class="ios-icon-box">\n' +
'                                ' + iconHtml + '\n' +
'                            </div>\n' +
'                            <div class="ios-file-title">' + file.name + '</div>\n' +
'                            <div class="ios-file-subtitle">' + displaySize + '</div>\n' +
'                        ';
                list.appendChild(card);
            });
        }

        function showContextMenu(e, file) {
            const menu = document.getElementById('context-menu');
            document.getElementById('context-file-path').value = file.absolutePath || file.path;
            document.getElementById('context-file-type').value = file.isDirectory ? 'dir' : 'file';

            const downloadBtn = document.getElementById('context-download-btn');
            if (file.isDirectory) {
                downloadBtn.style.display = 'none';
                downloadBtn.nextElementSibling.style.display = 'none';
            } else {
                downloadBtn.style.display = 'flex';
                downloadBtn.nextElementSibling.style.display = 'block';
            }

            let x = e.clientX || (e.touches && e.touches[0].clientX);
            let y = e.clientY || (e.touches && e.touches[0].clientY);
            if (!x || !y) {
                const rect = e.currentTarget.getBoundingClientRect();
                x = rect.left + (rect.width/2);
                y = rect.top + (rect.height/2);
            }

            const menuWidth = 250;
            if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 20;

            menu.style.left = x + 'px';
            menu.style.top = y + window.scrollY + 'px';
            menu.style.display = 'block';
            
            e.stopPropagation();
            e.preventDefault();
        }

        function execContextAction(action) {
            const absolutePath = document.getElementById('context-file-path').value;
            const menu = document.getElementById('context-menu');
            menu.style.display = 'none';
            doVibrate(50);

            const parts = absolutePath.split('/');
            const fileName = parts[parts.length - 1];

            if (action === 'OPEN') requestOpenFile(absolutePath);
            else if (action === 'STREAM') playOnPhone(absolutePath, fileName);
            else if (action === 'DOWNLOAD') window.open('/api/download?path=' + encodeURIComponent(absolutePath) + '&download=1', '_blank');
            else if (action === 'CUT' || action === 'COPY') {
                clipboard = { action: action, path: absolutePath, name: fileName };
                document.getElementById('clipboard-item-name').textContent = fileName;
                document.getElementById('clipboard-mode-text').textContent = action === 'CUT' ? 'Pronto para mover' : 'Pronto para copiar';
                document.getElementById('clipboard-icon').className = action === 'CUT' ? 'fa-solid fa-scissors text-[var(--ios-blue)] text-lg animate-pulse' : 'fa-solid fa-copy text-[var(--ios-blue)] text-lg animate-pulse';
                document.getElementById('clipboard-bar').classList.remove('hidden');
            } else if (action === 'RENAME') {
                const newName = prompt("Renomear para:");
                if (newName) processFileAction('RENAME', absolutePath, newName.trim());
            } else if (action === 'DELETE') {
                if (confirm("Deseja realmente excluir este item?")) processFileAction('DELETE', absolutePath);
            }
        }

        function clearClipboard() {
            doVibrate(30);
            clipboard = null;
            document.getElementById('clipboard-bar').classList.add('hidden');
        }

        function pasteClipboard() {
            if (!clipboard) return;
            doVibrate(65);
            const dstDir = currentPath;
            const actionType = clipboard.action;
            const apiAction = actionType === 'CUT' ? 'MOVE' : 'COPY';
            
            processFileAction(apiAction, dstDir, clipboard.path);
            clearClipboard();
        }

        function playOnPhone(path, name) {
            document.getElementById('media-title').textContent = name;
            const container = document.getElementById('media-container');
            container.innerHTML = "";
            const ext = name.split('.').pop().toLowerCase();
            const streamUrl = '/api/download?path=' + encodeURIComponent(path);
            
            if (['mp4', 'mkv', 'webm', 'mov', 'avi'].includes(ext)) {
                container.innerHTML = '<video src="' + streamUrl + '" controls autoplay class="w-full h-auto max-h-[400px] rounded-lg"></video>';
            } else if (['mp3', 'wav', 'ogg', 'aac', 'flac'].includes(ext)) {
                container.innerHTML = '\n' +
'                    <div class="flex flex-col items-center justify-center py-6 w-full">\n' +
'                        <i class="fa-solid fa-music text-5xl text-[var(--ios-blue)] mb-4 animate-bounce"></i>\n' +
'                        <audio src="' + streamUrl + '" controls autoplay class="w-full px-4"></audio>\n' +
'                    </div>\n' +
'                ';
            } else if (['jpg', 'jpeg', 'png', 'webp', 'gif'].includes(ext)) {
                container.innerHTML = '<img src="' + streamUrl + '" class="max-w-full max-h-[400px] object-contain rounded-lg shadow-xl">';
            } else {
                container.innerHTML = '<div class="text-gray-400 p-6 text-center"><i class="fa-solid fa-file text-5xl mb-3"></i><br>Formato não transmissível no celular. Baixe ou abra na TV!</div>';
            }
            
            document.getElementById('media-modal').style.display = 'flex';
            setTimeout(() => {
                document.getElementById('media-modal').classList.add('active');
            }, 10);
        }

        function closeMediaModal(e) {
            if (e === 'close' || e.target.id === 'media-modal') {
                const container = document.getElementById('media-container');
                container.innerHTML = ""; // Stop audio/video
                document.getElementById('media-modal').classList.remove('active');
                setTimeout(() => {
                    document.getElementById('media-modal').style.display = 'none';
                }, 300);
            }
        }

        function navigateBack() {
            doVibrate(30);
            const parent = currentPath.substring(0, currentPath.lastIndexOf('/'));
            if (parent.trim().length > 0 && parent !== "/storage") {
                currentPath = parent;
                fetchFiles();
            }
        }

        function requestOpenFile(absolutePath) {
            fetch('/api/open?path=' + encodeURIComponent(absolutePath), { method: 'POST' })
                .then(res => res.json())
                .then(data => {
                     if (data.status === 'success') { showNotification("Arquivo aberto na TV!"); }
                     else showNotification("Erro ao abrir o arquivo: " + data.message, true);
                }).catch(err => showNotification("Erro de conexão", true));
        }

        function createNewFolder() {
            doVibrate(30);
            const name = prompt("Nome da Nova Pasta:");
            if (name) processFileAction('MKDIR', currentPath + '/' + name.trim());
        }

        function processFileAction(action, path, argument = "") {
            fetch('/api/action', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: action, path: path, argument: argument })
            }).then(res => res.json()).then(data => {
                if (data.status === 'success') {
                    showNotification("Ação concluída!");
                    fetchFiles();
                } else {
                    showNotification("Falha: " + data.message, true);
                }
            }).catch(err => showNotification("Erro de conexão", true));
        }

        function handleUpload(files) {
            if (files.length === 0) return;
            doVibrate(40);

            const container = document.getElementById('upload-progress-container');
            const bar = document.getElementById('upload-bar');
            const percent = document.getElementById('upload-percent');

            container.classList.remove('hidden');
            bar.style.width = '0%';
            percent.textContent = '0%';

            const formData = new FormData();
            for (let i = 0; i < files.length; i++) formData.append('files', files[i]);

            const xhr = new XMLHttpRequest();
            xhr.open('POST', '/api/upload?path=' + encodeURIComponent(currentPath), true);

            xhr.upload.onprogress = function(e) {
                if (e.lengthComputable) {
                    const doneP = Math.round((e.loaded / e.total) * 100);
                    bar.style.width = doneP + '%';
                    percent.textContent = doneP + '%';
                }
            };

            xhr.onload = function() {
                doVibrate(90);
                setTimeout(() => container.classList.add('hidden'), 1000);
                if (xhr.status === 200) { fetchFiles(); showNotification("Upload concluído!"); }
                else showNotification("Falha no upload.", true);
            };
            xhr.onerror = function() { container.classList.add('hidden'); showNotification("Erro de conexão.", true); };
            xhr.send(formData);
        }

        function sendRemoteCmd(cmd) {
            doVibrate(cmd === 'OK' ? 70 : 45);
            fetch('/api/remote', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ command: cmd })
            }).catch(console.error);
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
