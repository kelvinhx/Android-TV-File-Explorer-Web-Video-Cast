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
            grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
            gap: 16px;
            padding: 16px;
        }
        .ios-grid-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            text-align: center;
            background: transparent;
            border-radius: 12px;
            padding: 8px;
            transition: transform 0.1s, background 0.15s;
        }
        .ios-grid-item:active {
            transform: scale(0.95);
            background: var(--ios-tint);
        }
        
        .ios-icon-box {
            width: 60px;
            height: 60px;
            background: var(--ios-bg);
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 8px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
            font-size: 28px;
            overflow: hidden;
        }
        .ios-folder { color: #81D4FA; } /* Light blue for iOS folder */
        .ios-file-title {
            font-size: 13px;
            font-weight: 500;
            word-break: break-word;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
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
                <i class="fa-solid fa-microphone"></i>
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

        <!-- Tab 2: Remote -->
        <div id="view-remote" class="hidden">
            <div class="px-6 text-center">
                <h2 class="text-xl font-bold mb-1">Controle Remoto TV</h2>
                <p class="text-xs text-[var(--ios-gray)] mb-6">Toque para controlar a Android TV</p>

                <!-- D-Pad -->
                <div class="dpad-container">
                    <button onclick="sendRemoteCmd('UP')" class="dpad-btn dpad-up"><i class="fa-solid fa-chevron-up"></i></button>
                    <button onclick="sendRemoteCmd('DOWN')" class="dpad-btn dpad-down"><i class="fa-solid fa-chevron-down"></i></button>
                    <button onclick="sendRemoteCmd('LEFT')" class="dpad-btn dpad-left"><i class="fa-solid fa-chevron-left"></i></button>
                    <button onclick="sendRemoteCmd('RIGHT')" class="dpad-btn dpad-right"><i class="fa-solid fa-chevron-right"></i></button>
                    <button onclick="sendRemoteCmd('OK')" class="dpad-btn dpad-ok">OK</button>
                </div>

                <!-- Auxiliary Buttons -->
                <div class="flex justify-center gap-6 mt-8">
                    <button onclick="sendRemoteCmd('BACK')" class="w-14 h-14 rounded-full bg-[var(--ios-card)] flex items-center justify-center text-[var(--ios-gray)]"><i class="fa-solid fa-arrow-left text-xl"></i></button>
                    <button onclick="sendRemoteCmd('HOME')" class="w-14 h-14 rounded-full bg-[var(--ios-card)] flex items-center justify-center text-[var(--ios-gray)]"><i class="fa-solid fa-tv text-xl"></i></button>
                    <button onclick="sendRemoteCmd('SETTINGS')" class="w-14 h-14 rounded-full bg-[var(--ios-card)] flex items-center justify-center text-[var(--ios-gray)]"><i class="fa-solid fa-gear text-xl"></i></button>
                </div>
                
                <!-- Volume & Media -->
                <div class="flex justify-center gap-4 mt-8">
                    <div class="bg-[var(--ios-card)] rounded-xl flex items-center p-1">
                        <button onclick="sendRemoteCmd('VOL_DOWN')" class="px-4 py-2 text-xl text-[var(--ios-gray)]"><i class="fa-solid fa-volume-low"></i></button>
                        <div class="w-[1px] h-6 bg-[rgba(255,255,255,0.1)]"></div>
                        <button onclick="sendRemoteCmd('VOL_UP')" class="px-4 py-2 text-xl text-[var(--ios-gray)]"><i class="fa-solid fa-volume-high"></i></button>
                    </div>
                </div>
            </div>
        </div>

        <!-- Tab 3: Browser Cast -->
        <div id="view-browser" class="hidden flex-col absolute top-[60px] bottom-[84px] left-0 right-0">
            <div class="flex items-center gap-1 p-2 bg-[#1C1C1E] border-b border-[rgba(255,255,255,0.1)] z-10 w-full shrink-0">
                <button onclick="document.getElementById('internal-browser').contentWindow.history.back()" class="p-2 text-[var(--ios-blue)]"><i class="fa-solid fa-chevron-left"></i></button>
                <div class="flex-1 bg-black rounded-lg flex items-center px-3 py-1.5 border border-stone-800">
                    <i class="fa-solid fa-lock text-xs text-[#34C759] mr-2"></i>
                    <input type="text" id="browser-url" class="bg-transparent text-sm text-white w-full outline-none" value="https://www.google.com.br">
                </div>
                <button onclick="loadInternalBrowser()" class="p-2 text-[var(--ios-blue)]"><i class="fa-solid fa-arrow-rotate-right"></i></button>
            </div>
            
            <div id="video-sniffer-bar" class="hidden bg-emerald-600 text-white px-3 py-2 text-xs flex justify-between items-center shadow-lg shrink-0">
                <span class="font-bold flex items-center"><i class="fa-solid fa-video mr-1.5 animate-pulse"></i> Vídeo Detectado!</span>
                <button onclick="castSniffedVideo()" class="bg-white text-emerald-800 px-3 py-1.5 rounded-full font-bold shadow-sm">Transmitir à TV</button>
            </div>

            <div class="flex-1 relative bg-white w-full h-full overflow-hidden">
                <iframe id="internal-browser" class="w-full h-full border-none absolute inset-0" sandbox="allow-scripts allow-same-origin allow-forms"></iframe>
                <div id="browser-loading" class="absolute inset-0 bg-[rgba(0,0,0,0.7)] flex flex-col items-center justify-center hidden z-20">
                    <i class="fa-solid fa-spinner fa-spin text-4xl text-[var(--ios-blue)] mb-4"></i>
                    <span class="text-sm font-semibold text-white tracking-wider">CARREGANDO...</span>
                </div>
            </div>
        </div>

    </div>

    <!-- Tab Bar -->
    <div class="tab-bar">
        <button id="tab-files" onclick="toggleView('files')" class="tab-item active">
            <i class="fa-solid fa-folder"></i>
            <span>Navegar</span>
        </button>
        <button id="tab-remote" onclick="toggleView('remote')" class="tab-item">
            <i class="fa-solid fa-gamepad"></i>
            <span>Controle</span>
        </button>
        <button id="tab-browser" onclick="toggleView('browser')" class="tab-item">
            <i class="fa-solid fa-display"></i>
            <span>Transmitir</span>
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
            <button onclick="closeSettingsModal('close')" class="w-full bg-[var(--ios-card)] py-3 rounded-xl text-[var(--ios-blue)] font-bold text-lg mt-4">Concluído</button>
        </div>
    </div>

    <script>
        let currentPath = "/storage/emulated/0";
        let activeView = "files";
        let longPressTimer = null;
        let isLongPressActive = false;
        let clipboard = null; // Stores { action: 'COPY'|'MOVE', path: '..', name: '..' }

        document.addEventListener('DOMContentLoaded', () => {
            fetchFiles();
            
            // Real-time search filter
            document.getElementById('search-input').addEventListener('input', (e) => {
                const query = e.target.value.toLowerCase().trim();
                const items = document.querySelectorAll('.ios-grid-item');
                items.forEach(item => {
                    const titleText = item.querySelector('.ios-file-title').textContent.toLowerCase();
                    if (titleText.includes(query)) {
                        item.style.display = 'flex';
                    } else {
                        item.style.display = 'none';
                    }
                });
            });

            // Close context menu on anywhere click
            document.addEventListener('click', (e) => {
                const menu = document.getElementById('context-menu');
                if (menu && !menu.contains(e.target)) {
                    menu.style.display = 'none';
                }
            });
        });

        function doVibrate(duration = 60) {
            if ("vibrate" in navigator) {
                navigator.vibrate(duration);
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
            document.getElementById('view-remote').classList.add('hidden');
            document.getElementById('view-browser').classList.add('hidden');
            
            document.getElementById('tab-files').classList.remove('active');
            document.getElementById('tab-remote').classList.remove('active');
            document.getElementById('tab-browser').classList.remove('active');

            if (view === 'files') {
                document.getElementById('view-files').classList.remove('hidden');
                document.getElementById('tab-files').classList.add('active');
                document.getElementById('main-header').firstElementChild.textContent = "Navegar";
                document.getElementById('files-toolbar').style.display = "flex";
                fetchFiles();
            } else if (view === 'remote') {
                document.getElementById('view-remote').classList.remove('hidden');
                document.getElementById('tab-remote').classList.add('active');
                document.getElementById('main-header').firstElementChild.textContent = "Controle";
                document.getElementById('files-toolbar').style.display = "none";
            } else if (view === 'browser') {
                document.getElementById('view-browser').classList.remove('hidden');
                document.getElementById('tab-browser').classList.add('active');
                document.getElementById('main-header').firstElementChild.textContent = "Transmitir";
                document.getElementById('files-toolbar').style.display = "none";
                
                const iframe = document.getElementById('internal-browser');
                if (!iframe.src || iframe.src === window.location.href) {
                    loadInternalBrowser();
                }
            }
        }

        let sniffedVideoUrl = null;

        function loadInternalBrowser() {
            let url = document.getElementById('browser-url').value.trim();
            if(!url) return;
            if (!url.startsWith('http://') && !url.startsWith('https://')) url = 'https://' + url;
            document.getElementById('browser-url').value = url;
            document.getElementById('browser-loading').classList.remove('hidden');
            document.getElementById('internal-browser').src = '/api/proxy?url=' + encodeURIComponent(url);
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
                        alert("O vídeo foi enviado à sua TV!");
                    } else {
                        alert("Falha na transmissão: " + data.message);
                    }
                }).catch(console.error);
            }
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
                    else backBtn.style.visibility = 'visible';

                    // Update hardware metrics overlay
                    if (data.ramPercent !== undefined) {
                        document.getElementById('tv-ram-badge').textContent = data.ramPercent + "% Carregada";
                        document.getElementById('tv-ram-ring').textContent = data.ramPercent + "%";
                        document.getElementById('tv-storage-badge').textContent = data.storageFreeFormatted + " Livre";
                        document.getElementById('tv-storage-ring').textContent = data.storagePercent + "%";
                    }

                    const list = document.getElementById('files-list');
                    list.innerHTML = "";

                    if (data.files.length === 0) {
                        list.innerHTML = `
                            <div class="col-span-full text-center py-20 text-[var(--ios-gray)]">
                                <span class="text-sm">A pasta está vazia.</span>
                            </div>
                        `;
                        return;
                    }

                    data.files.forEach(file => {
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
                                currentPath = file.absolutePath;
                                fetchFiles();
                            } else {
                                requestOpenFile(file.absolutePath);
                            }
                        });

                        let iconClass = "fa-solid fa-file";
                        let iconColor = "text-[#8E8E93]"; // iOS Gray
                        let isImg = false;

                        if (file.isDirectory) {
                            iconClass = "fa-solid fa-folder";
                            iconColor = "ios-folder";
                        } else if (file.name.endsWith('.apk')) {
                            iconClass = "fa-brands fa-android";
                            iconColor = "text-[#34C759]"; // iOS Green
                        } else if (file.name.endsWith('.mp4') || file.name.endsWith('.mkv') || file.name.endsWith('.webm') || file.name.endsWith('.mov')) {
                            iconClass = "fa-solid fa-circle-play";
                            iconColor = "text-[#AF52DE]"; // iOS Purple
                        } else if (file.name.endsWith('.mp3') || file.name.endsWith('.wav') || file.name.endsWith('.aac')) {
                            iconClass = "fa-solid fa-music";
                            iconColor = "text-[#FF2D55]"; // iOS Pink
                        } else if (file.name.endsWith('.jpg') || file.name.endsWith('.jpeg') || file.name.endsWith('.png') || file.name.endsWith('.webp') || file.name.endsWith('.gif')) {
                            isImg = true;
                        }

                        let iconHtml = "";
                        if (isImg) {
                            iconHtml = '<img src="/api/download?path=' + encodeURIComponent(file.absolutePath) + '" class="w-full h-full object-cover rounded-lg">';
                        } else {
                            iconHtml = '<i class="' + iconClass + ' ' + iconColor + '"></i>';
                        }

                        card.innerHTML = '\n' +
'                            <div class="ios-icon-box">\n' +
'                                ' + iconHtml + '\n' +
'                            </div>\n' +
'                            <div class="ios-file-title">' + file.name + '</div>\n' +
'                            <div class="ios-file-subtitle">' + file.sizeFormatted + '</div>\n' +
'                        ';
                        list.appendChild(card);
                    });
                }).catch(console.error);
        }

        function showContextMenu(e, file) {
            const menu = document.getElementById('context-menu');
            document.getElementById('context-file-path').value = file.absolutePath;
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
            else if (action === 'DOWNLOAD') window.open('/api/download?path=' + encodeURIComponent(absolutePath), '_blank');
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
                     if (data.status === 'success') { /* sucesso */ }
                     else alert("Erro ao abrir o arquivo: " + data.message);
                }).catch(console.error);
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
                if (data.status === 'success') fetchFiles();
                else alert("Falha: " + data.message);
            }).catch(console.error);
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
                if (xhr.status === 200) fetchFiles();
                else alert("Falha no upload.");
            };
            xhr.onerror = function() { container.classList.add('hidden'); alert("Erro de conexão."); };
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
