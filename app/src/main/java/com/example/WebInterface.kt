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
            --ios-blue: #0A84FF;
            --ios-green: #30D158;
            --ios-red: #FF453A;
            --ios-orange: #FF9F0A;
        }
        body {
            background: #000000;
            font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "Segoe UI", Roboto, sans-serif;
            color: #F2F2F7;
            user-select: none;
            -webkit-user-select: none;
            overflow-x: hidden;
            padding-bottom: env(safe-area-inset-bottom);
        }
        .liquid-panel {
            background: #1C1C1E;
            border: 1px solid rgba(255, 255, 255, 0.08);
            border-radius: 20px;
        }
        .liquid-card {
            background: #1C1C1E;
            border: 1px solid rgba(255, 255, 255, 0.05);
            border-radius: 16px;
            transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
        }
        .liquid-card:active {
            transform: scale(0.96);
            background: rgba(10, 132, 255, 0.15);
            border-color: rgba(10, 132, 255, 0.3);
        }
        /* Custom Context Menu */
        .context-menu {
            display: none;
            position: absolute;
            z-index: 1000;
            background: rgba(28, 28, 35, 0.94);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 14px;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.6);
            width: 220px;
            animation: contextReveal 0.18s cubic-bezier(0.16, 1, 0.3, 1);
        }
        @keyframes contextReveal {
            from { transform: scale(0.92); opacity: 0; }
            to { transform: scale(1); opacity: 1; }
        }
        .context-item {
            display: flex;
            align-items: center;
            width: 100%;
            padding: 12px 16px;
            font-size: 15px;
            font-weight: 500;
            border-bottom: 0.5px solid rgba(255, 255, 255, 0.06);
            transition: background 0.15s;
        }
        .context-item:last-child {
            border-bottom: none;
        }
        .context-item:active {
            background: rgba(255, 255, 255, 0.1);
        }
        /* Haptic active state */
        .haptic-btn:active {
            transform: scale(0.93);
            filter: brightness(1.15);
            transition: all 0.08s ease;
        }
        /* Hide scrollbar */
        ::-webkit-scrollbar {
            display: none;
        }
    </style>
</head>
<body class="p-4 safe-top select-none">

    <!-- Header / Brand -->
    <header class="flex justify-between items-center mb-6">
        <div>
            <h1 class="text-2xl font-bold tracking-tight text-white mb-0.5">Nexus TV Controller</h1>
            <p class="text-xs text-stone-400"><i class="fa-solid ref-icon fa-link text-blue-500 mr-1"></i> Connected to Android TV</p>
        </div>
        <button onclick="toggleView('remote')" class="haptic-btn w-10 h-10 rounded-full bg-blue-600/20 text-blue-400 flex items-center justify-center border border-blue-500/25">
            <i class="fa-solid fa-gamepad text-lg"></i>
        </button>
    </header>

    <!-- Navigation Tabs -->
    <div class="flex gap-2 mb-6">
        <button id="tab-files" onclick="toggleView('files')" class="flex-1 py-3 text-sm font-semibold rounded-xl text-center transition bg-white/10 text-white border border-white/5 shadow">
            <i class="fa-solid fa-folder-open mr-1.5"></i> Storage
        </button>
        <button id="tab-remote" onclick="toggleView('remote')" class="flex-1 py-3 text-sm font-semibold rounded-xl text-center transition bg-stone-900/40 text-stone-400 border border-transparent">
            <i class="fa-solid fa-gamepad mr-1.5"></i> D-Pad RC
        </button>
        <button id="tab-browser" onclick="toggleView('browser')" class="flex-1 py-3 text-sm font-semibold rounded-xl text-center transition bg-stone-900/40 text-stone-400 border border-transparent">
            <i class="fa-solid fa-globe mr-1.5"></i> Cast Web
        </button>
    </div>

    <!-- MAIN VIEW: FILES & STORAGE -->
    <div id="view-files">
        <!-- Path Bar -->
        <div class="liquid-panel px-4 py-3 mb-4 flex items-center gap-2 overflow-x-auto text-sm text-stone-300 font-mono">
            <i class="fa-solid fa-hard-drive text-blue-500"></i>
            <span id="current-path" class="whitespace-nowrap overflow-ellipsis">Loading storage...</span>
        </div>

        <!-- Toolbar / Direct Actions -->
        <div class="grid grid-cols-2 gap-3 mb-5">
            <label class="liquid-card px-4 py-3 flex flex-col items-center justify-center cursor-pointer">
                <i class="fa-solid fa-cloud-arrow-up text-xl text-blue-400 mb-1"></i>
                <span class="text-xs font-semibold text-stone-200">Upload Files</span>
                <input type="file" id="file-uploader" class="hidden" multiple onchange="handleUpload(this.files)">
            </label>
            <button onclick="createNewFolder()" class="liquid-card px-4 py-3 flex flex-col items-center justify-center">
                <i class="fa-solid fa-folder-plus text-xl text-green-400 mb-1"></i>
                <span class="text-xs font-semibold text-stone-200">New Directory</span>
            </button>
        </div>

        <!-- Upload Status Dashboard -->
        <div id="upload-progress-container" class="hidden liquid-panel p-4 mb-4">
            <div class="flex justify-between items-center text-xs text-stone-300 mb-1.5 font-semibold">
                <span>Uploading...</span>
                <span id="upload-percent">0%</span>
            </div>
            <div class="w-full bg-stone-800 h-2 rounded-full overflow-hidden">
                <div id="upload-bar" class="bg-blue-500 h-full w-0 transition-all duration-100"></div>
            </div>
        </div>

        <!-- Files List Container -->
        <div class="mb-2">
            <div id="back-btn-container" class="hidden mb-2">
                <button onclick="navigateBack()" class="w-full py-2.5 px-4 rounded-xl bg-stone-900/45 text-amber-400 hover:text-amber-300 text-sm font-semibold flex items-center gap-2 border border-amber-500/15">
                    <i class="fa-solid fa-backward"></i> Go Up / Back
                </button>
            </div>
            <div id="files-list" class="space-y-2">
                <!-- Dynamically populated files -->
            </div>
        </div>
    </div>

    <!-- REMOTE CONTROLLER VIEW -->
    <div id="view-remote" class="hidden">
        <!-- Device Quick Control Center -->
        <div class="liquid-panel p-5 text-center mb-6">
            <h2 class="text-md font-bold text-stone-300 mb-4 tracking-wide"><i class="fa-solid fa-network-wired mr-1.5 text-blue-500"></i> ANDROID TV REMOTE CONTROL</h2>
            
            <!-- Context navigation commands -->
            <div class="grid grid-cols-3 gap-3 mb-5">
                <button onclick="sendRemoteCmd('BACK')" class="haptic-btn py-3 liquid-panel flex flex-col items-center justify-center text-stone-300">
                    <i class="fa-solid fa-arrow-left text-lg text-amber-500 mb-1"></i>
                    <span class="text-[11px] font-medium text-stone-400">Back</span>
                </button>
                <button onclick="sendRemoteCmd('SETTINGS')" class="haptic-btn py-3 liquid-panel flex flex-col items-center justify-center text-stone-300">
                    <i class="fa-solid fa-gears text-lg text-stone-400 mb-1"></i>
                    <span class="text-[11px] font-medium text-stone-400">Settings</span>
                </button>
                <button onclick="sendRemoteCmd('HOME')" class="haptic-btn py-3 liquid-panel flex flex-col items-center justify-center text-stone-300">
                    <i class="fa-solid fa-house-chimney text-lg text-blue-500 mb-1"></i>
                    <span class="text-[11px] font-medium text-stone-400">Home</span>
                </button>
            </div>

            <!-- Authentic Apple D-Pad Circle layout -->
            <div class="flex justify-center my-6">
                <div class="relative w-56 h-56 bg-stone-900/30 rounded-full border border-stone-800 flex items-center justify-center p-2 shadow-2xl">
                    <!-- UP Button -->
                    <button onclick="sendRemoteCmd('UP')" class="haptic-btn absolute top-1.5 w-16 h-14 flex items-center justify-center text-stone-300 rounded-t-full">
                        <i class="fa-solid fa-chevron-up text-2xl text-blue-500"></i>
                    </button>
                    <!-- DOWN Button -->
                    <button onclick="sendRemoteCmd('DOWN')" class="haptic-btn absolute bottom-1.5 w-16 h-14 flex items-center justify-center text-stone-300 rounded-b-full">
                        <i class="fa-solid fa-chevron-down text-2xl text-blue-500"></i>
                    </button>
                    <!-- LEFT Button -->
                    <button onclick="sendRemoteCmd('LEFT')" class="haptic-btn absolute left-1.5 w-14 h-16 flex items-center justify-center text-stone-300 rounded-l-full">
                        <i class="fa-solid fa-chevron-left text-2xl text-blue-500"></i>
                    </button>
                    <!-- RIGHT Button -->
                    <button onclick="sendRemoteCmd('RIGHT')" class="haptic-btn absolute right-1.5 w-14 h-16 flex items-center justify-center text-stone-300 rounded-r-full">
                        <i class="fa-solid fa-chevron-right text-2xl text-blue-500"></i>
                    </button>
                    
                    <!-- OK / CENTER SELECT -->
                    <button onclick="sendRemoteCmd('OK')" class="haptic-btn w-24 h-24 rounded-full bg-blue-600 shadow-lg border border-blue-400 flex items-center justify-center text-white active:scale-90 font-bold text-lg">
                        SELECT
                    </button>
                </div>
            </div>

            <!-- Quick TV System Actions -->
            <div class="grid grid-cols-2 gap-4 mt-6">
                <div class="p-3 bg-stone-950/40 rounded-xl border border-stone-900 flex items-center justify-between">
                    <span class="text-xs font-semibold text-stone-300"><i class="fa-solid fa-volume-low text-blue-400 mr-2"></i> TV Audio</span>
                    <div class="flex gap-1.5">
                        <button onclick="sendRemoteCmd('VOL_DOWN')" class="haptic-btn w-8 h-8 rounded-lg bg-stone-800 text-stone-300 flex items-center justify-center"><i class="fa-solid fa-minus text-xs"></i></button>
                        <button onclick="sendRemoteCmd('VOL_UP')" class="haptic-btn w-8 h-8 rounded-lg bg-stone-800 text-stone-300 flex items-center justify-center"><i class="fa-solid fa-plus text-xs"></i></button>
                    </div>
                </div>
                <div class="p-3 bg-stone-950/40 rounded-xl border border-stone-900 flex items-center justify-between">
                    <span class="text-xs font-semibold text-stone-300"><i class="fa-solid fa-play-pause text-blue-400 mr-2"></i> Media</span>
                    <div class="flex gap-1.5">
                        <button onclick="sendRemoteCmd('MEDIA_PREV')" class="haptic-btn w-8 h-8 rounded-lg bg-stone-800 text-stone-300 flex items-center justify-center"><i class="fa-solid fa-backward text-[10px]"></i></button>
                        <button onclick="sendRemoteCmd('MEDIA_PLAY')" class="haptic-btn w-8 h-8 rounded-lg bg-stone-800 text-stone-300 flex items-center justify-center"><i class="fa-solid fa-play text-[10px]"></i></button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- BROWSER / CAST VIEW -->
    <div id="view-browser" class="hidden">
        <div class="liquid-panel p-5 text-center mb-6">
            <h2 class="text-md font-bold text-stone-300 mb-4 tracking-wide"><i class="fa-solid fa-earth-americas mr-1.5 text-blue-500"></i> WEB & VIDEO CASTING</h2>
            <p class="text-xs text-stone-400 mb-4">Paste a URL here (YouTube, web page, video link) to instantly cast it to your Android TV's built-in browser. Ad-blocking is enabled on the TV.</p>
            
            <input type="text" id="cast-url" placeholder="https://youtube.com/..." class="w-full bg-stone-900 border border-stone-700 text-white p-3 rounded-lg text-sm mb-4 outline-none focus:border-blue-500">
            
            <button onclick="castUrl()" class="w-full bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 px-4 rounded-xl shadow-lg border border-blue-400 active:scale-95 transition-all">
                <i class="fa-solid fa-tv mr-2"></i> Cast to TV
            </button>

            <div class="mt-6 text-left">
                <p class="text-xs font-semibold text-stone-500 mb-2 uppercase">Quick Cast Shortcuts</p>
                <div class="grid grid-cols-2 gap-2">
                    <button onclick="document.getElementById('cast-url').value='https://youtube.com'; castUrl();" class="p-2 bg-stone-800 rounded text-xs text-stone-300 border border-stone-700"><i class="fa-brands fa-youtube text-red-500"></i> YouTube</button>
                    <button onclick="document.getElementById('cast-url').value='https://twitch.tv'; castUrl();" class="p-2 bg-stone-800 rounded text-xs text-stone-300 border border-stone-700"><i class="fa-brands fa-twitch text-purple-500"></i> Twitch</button>
                    <button onclick="document.getElementById('cast-url').value='https://google.com'; castUrl();" class="p-2 bg-stone-800 rounded text-xs text-stone-300 border border-stone-700"><i class="fa-brands fa-google text-blue-400"></i> Google</button>
                    <button onclick="document.getElementById('cast-url').value='https://netflix.com'; castUrl();" class="p-2 bg-stone-800 rounded text-xs text-stone-300 border border-stone-700"><i class="fa-solid fa-n text-red-600"></i> Netflix</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Long Press Haptic Context Menu -->
    <div id="context-menu" class="context-menu">
        <!-- Set context target dynamically -->
        <input type="hidden" id="context-file-path" value="">
        <input type="hidden" id="context-file-type" value="">
        <div id="context-title" class="p-4 text-xs font-semibold uppercase tracking-wider text-stone-400 border-b border-stone-800 truncate">File Actions</div>
        <button onclick="execContextAction('OPEN')" class="context-item text-blue-400">
            <i class="fa-solid fa-play mr-3"></i> Open / Use on TV
        </button>
        <button id="context-download-btn" onclick="execContextAction('DOWNLOAD')" class="context-item text-green-400">
            <i class="fa-solid fa-download mr-3"></i> Download to Client
        </button>
        <button onclick="execContextAction('RENAME')" class="context-item text-stone-300">
            <i class="fa-solid fa-pen mr-3"></i> Rename File
        </button>
        <button onclick="execContextAction('DELETE')" class="context-item text-red-500">
            <i class="fa-solid fa-trash-can mr-3"></i> Delete
        </button>
    </div>

    <!-- JavaScript logic -->
    <script>
        let currentPath = "/storage/emulated/0";
        let activeView = "files";
        let longPressTimer = null;
        let isLongPressActive = false;

        document.addEventListener('DOMContentLoaded', () => {
            fetchFiles();
            
            // Close context menu on anywhere click
            document.addEventListener('click', (e) => {
                const menu = document.getElementById('context-menu');
                if (!menu.contains(e.target)) {
                    menu.style.display = 'none';
                }
            });
        });

        function doVibrate(duration = 60) {
            if ("vibrate" in navigator) {
                navigator.vibrate(duration);
            }
        }

        function toggleView(view) {
            doVibrate(30);
            activeView = view;
            
            // Hide all
            document.getElementById('view-files').classList.add('hidden');
            document.getElementById('view-remote').classList.add('hidden');
            document.getElementById('view-browser').classList.add('hidden');
            
            // Reset tabs
            const inactiveClass = "flex-1 py-3 text-sm font-semibold rounded-xl text-center transition bg-stone-900/40 text-stone-400 border border-transparent";
            const activeClass = "flex-1 py-3 text-sm font-semibold rounded-xl text-center transition bg-white/10 text-white border border-white/5 shadow";
            
            document.getElementById('tab-files').className = inactiveClass;
            document.getElementById('tab-remote').className = inactiveClass;
            document.getElementById('tab-browser').className = inactiveClass;

            if (view === 'files') {
                document.getElementById('view-files').classList.remove('hidden');
                document.getElementById('tab-files').className = activeClass;
                fetchFiles();
            } else if (view === 'remote') {
                document.getElementById('view-remote').classList.remove('hidden');
                document.getElementById('tab-remote').className = activeClass;
            } else if (view === 'browser') {
                document.getElementById('view-browser').classList.remove('hidden');
                document.getElementById('tab-browser').className = activeClass;
            }
        }

        function castUrl() {
            doVibrate(50);
            const urlInput = document.getElementById('cast-url');
            let url = urlInput.value.trim();
            if (!url) {
                alert("Please enter a URL first.");
                return;
            }
            if (!url.startsWith('http://') && !url.startsWith('https://')) {
                url = 'https://' + url;
            }
            
            fetch('/api/cast', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ url: url })
            })
            .then(res => res.json())
            .then(data => {
                if (data.status === 'success') {
                    alert("Casting to TV! Look at the Android screen.");
                    urlInput.value = "";
                } else {
                    alert("Casting failed: " + data.message);
                }
            })
            .catch(err => {
                console.error("Cast API error", err);
                alert("Error communicating with TV.");
            });
        }

        function fetchFiles() {
            fetch('/api/files?path=' + encodeURIComponent(currentPath))
                .then(res => res.json())
                .then(data => {
                    currentPath = data.currentPath;
                    document.getElementById('current-path').textContent = currentPath;
                    
                    const backContainer = document.getElementById('back-btn-container');
                    if (data.isRoot) {
                        backContainer.classList.add('hidden');
                    } else {
                        backContainer.classList.remove('hidden');
                    }

                    const list = document.getElementById('files-list');
                    list.innerHTML = "";

                    if (data.files.length === 0) {
                        list.innerHTML = `
                            <div class="text-center py-12 text-stone-500">
                                <i class="fa-regular fa-folder-open text-4xl mb-3 block"></i>
                                <span class="text-sm">Empty Directory</span>
                            </div>
                        `;
                        return;
                    }

                    data.files.forEach(file => {
                        const card = document.createElement('div');
                        card.className = "liquid-card p-3.5 flex items-center justify-between";
                        
                        // Set up haptic touch event logic for Long Press menu
                        card.addEventListener('touchstart', (e) => {
                            isLongPressActive = false;
                            longPressTimer = setTimeout(() => {
                                isLongPressActive = true;
                                doVibrate(65);
                                showContextMenu(e, file);
                            }, 600); // 600ms required for Long Press
                        }, { passive: true });

                        card.addEventListener('touchend', () => {
                            clearTimeout(longPressTimer);
                        }, { passive: true });

                        card.addEventListener('touchmove', () => {
                            clearTimeout(longPressTimer);
                        }, { passive: true });

                        // Normal simple tap
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
                                // Default open action on single click
                                requestOpenFile(file.absolutePath);
                            }
                        });

                        // Dynamic Icon or Thumbnail Generation
                        let iconHtml = "";
                        let iconClass = "fa-solid fa-file text-blue-400";
                        let isImage = false;
                        if (file.isDirectory) {
                            iconClass = "fa-solid fa-folder text-amber-500";
                        } else if (file.name.endsWith('.apk')) {
                            iconClass = "fa-brands fa-android text-emerald-400";
                        } else if (file.name.endsWith('.mp4') || file.name.endsWith('.mkv') || file.name.endsWith('.avi') || file.name.endsWith('.mov')) {
                            iconClass = "fa-solid fa-video text-purple-400";
                        } else if (file.name.endsWith('.mp3') || file.name.endsWith('.wav') || file.name.endsWith('.ogg') || file.name.endsWith('.flac')) {
                            iconClass = "fa-solid fa-music text-pink-400";
                        } else {
                            const ext = file.name.toLowerCase();
                            if (ext.endsWith('.jpg') || ext.endsWith('.jpeg') || ext.endsWith('.png') || ext.endsWith('.webp') || ext.endsWith('.gif')) {
                                isImage = true;
                            } else {
                                iconClass = "fa-solid fa-file text-teal-400";
                            }
                        }

                        if (isImage) {
                            iconHtml = `<img src="/api/download?path=${'$'}{encodeURIComponent(file.absolutePath)}" class="w-full h-full object-cover rounded-xl" />`;
                        } else {
                            iconHtml = `<i class="${'$'}{iconClass} text-lg"></i>`;
                        }

                        card.innerHTML = `
                            <div class="flex items-center gap-3.5 overflow-hidden">
                                <div class="w-10 h-10 rounded-xl bg-white/5 flex items-center justify-center border border-white/5 overflow-hidden">
                                    ${'$'}{iconHtml}
                                </div>
                                <div class="overflow-hidden">
                                    <div class="text-[14px] font-semibold text-white truncate max-w-[200px]">${'$'}{file.name}</div>
                                    <div class="text-[11px] text-stone-400 mt-0.5">${'$'}{file.sizeFormatted}</div>
                                </div>
                            </div>
                            <div class="flex items-center gap-2">
                                <button onclick="event.stopPropagation(); triggerNativeContextMenu(event, '${'$'}{file.name}', '${'$'}{file.absolutePath}', ${'$'}{file.isDirectory})" class="haptic-btn w-8 h-8 rounded-full hover:bg-white/5 text-stone-400 flex items-center justify-center border border-transparent hover:border-white/5">
                                    <i class="fa-solid fa-ellipsis-vertical text-sm"></i>
                                </button>
                            </div>
                        `;
                        list.appendChild(card);
                    });
                })
                .catch(err => {
                    console.error("Error loading files", err);
                });
        }

        function triggerNativeContextMenu(e, name, absolutePath, isDirectory) {
            doVibrate(40);
            const fileObj = {
                name: name,
                absolutePath: absolutePath,
                isDirectory: isDirectory
            };
            showContextMenu(e, fileObj);
        }

        function showContextMenu(e, file) {
            const menu = document.getElementById('context-menu');
            document.getElementById('context-file-path').value = file.absolutePath;
            document.getElementById('context-file-type').value = file.isDirectory ? 'dir' : 'file';
            document.getElementById('context-title').textContent = file.name;

            // Only show download for files, not directories
            const downloadBtn = document.getElementById('context-download-btn');
            if (file.isDirectory) {
                downloadBtn.style.display = 'none';
            } else {
                downloadBtn.style.display = 'flex';
            }

            // UI coordinates
            let x = e.clientX || (e.touches && e.touches[0].clientX);
            let y = e.clientY || (e.touches && e.touches[0].clientY);
            
            if (!x || !y) {
                // Fallback inside element bounds
                const rect = e.currentTarget.getBoundingClientRect();
                x = rect.left + 50;
                y = rect.top + 20;
            }

            // Offsite clipping check
            const menuWidth = 220;
            if (x + menuWidth > window.innerWidth) {
                x = window.innerWidth - menuWidth - 10;
            }

            menu.style.left = x + 'px';
            menu.style.top = y + window.scrollY + 'px';
            menu.style.display = 'block';
            
            // Block other clicks
            e.stopPropagation();
            e.preventDefault();
        }

        function execContextAction(action) {
            const absolutePath = document.getElementById('context-file-path').value;
            const type = document.getElementById('context-file-type').value;
            const menu = document.getElementById('context-menu');
            menu.style.display = 'none';

            doVibrate(50);

            if (action === 'OPEN') {
                requestOpenFile(absolutePath);
            } else if (action === 'DOWNLOAD') {
                window.open('/api/download?path=' + encodeURIComponent(absolutePath), '_blank');
            } else if (action === 'RENAME') {
                const newName = prompt("Rename this entry to:");
                if (newName && newName.trim().length > 0) {
                    processFileAction('RENAME', absolutePath, newName.trim());
                }
            } else if (action === 'DELETE') {
                if (confirm("Delete this entry permanently?")) {
                    processFileAction('DELETE', absolutePath);
                }
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
                    if (data.status === 'success') {
                        alert("File launched on Android TV!");
                    } else {
                        alert("Error opening file: " + data.message);
                    }
                })
                .catch(err => {
                    console.error("Open API error", err);
                });
        }

        function createNewFolder() {
            doVibrate(30);
            const name = prompt("Enter directory name:");
            if (name && name.trim().length > 0) {
                processFileAction('MKDIR', currentPath + '/' + name.trim());
            }
        }

        function processFileAction(action, path, argument = "") {
            fetch('/api/action', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action: action, path: path, argument: argument })
            })
            .then(res => res.json())
            .then(data => {
                if (data.status === 'success') {
                    fetchFiles();
                } else {
                    alert("Failure: " + data.message);
                }
            })
            .catch(err => {
                console.error("Action API error", err);
            });
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
            for (let i = 0; i < files.length; i++) {
                formData.append('files', files[i]);
            }

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
                container.classList.add('hidden');
                if (xhr.status === 200) {
                    fetchFiles();
                    alert("Files uploaded successfully!");
                } else {
                    alert("Upload failed. Android Server status: " + xhr.status);
                }
            };

            xhr.onerror = function() {
                container.classList.add('hidden');
                alert("Upload failed due to connection error.");
            };

            xhr.send(formData);
        }

        function sendRemoteCmd(cmd) {
            doVibrate(cmd === 'OK' ? 70 : 45);
            fetch('/api/remote', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ command: cmd })
            })
            .catch(err => {
                console.error("Remote controller API offline", err);
            });
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}
