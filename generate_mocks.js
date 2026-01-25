const fs = require('fs');
const path = require('path');

const OUTPUT_DIR = 'docs/assets/mocks';
const SCREENS = [
    'Home', 'Library', 'Player', 'Settings', 'Search',
    'Artist', 'Album', 'Lyrics', 'Playlists'
];

const THEMES = {
    dark: {
        bg: '#121212',
        surface: '#1E1E1E',
        text: '#FFFFFF',
        textSec: '#B3B3B3',
        accent: '#BB86FC'
    },
    light: {
        bg: '#FFFFFF',
        surface: '#F5F5F5',
        text: '#000000',
        textSec: '#666666',
        accent: '#6200EE'
    }
};

const WIDTH = 360;
const HEIGHT = 800;

function createSvg(screenName, themeName) {
    const theme = THEMES[themeName];
    // Simple wireframe representation based on screen name
    let content = '';

    // Status Bar
    content += `<rect x="0" y="0" width="${WIDTH}" height="24" fill="${theme.surface}" />
                <text x="16" y="16" font-family="Arial" font-size="12" fill="${theme.text}">12:30</text>
                <circle cx="${WIDTH - 20}" cy="12" r="6" fill="${theme.text}" />`;

    // Navigation Bar (Bottom)
    content += `<rect x="0" y="${HEIGHT - 56}" width="${WIDTH}" height="56" fill="${theme.surface}" />
                <rect x="${WIDTH / 2 - 24}" y="${HEIGHT - 48}" width="48" height="40" rx="20" fill="${theme.accent}33" />`;

    // Screen specific content
    switch (screenName) {
        case 'Home':
            content += renderHome(theme);
            break;
        case 'Library':
            content += renderLibrary(theme);
            break;
        case 'Player':
            content += renderPlayer(theme);
            break;
        case 'Settings':
            content += renderSettings(theme);
            break;
        case 'Search':
            content += renderSearch(theme);
            break;
        case 'Artist':
            content += renderArtist(theme);
            break;
        case 'Album':
            content += renderAlbum(theme);
            break;
        case 'Lyrics':
            content += renderLyrics(theme);
            break;
        case 'Playlists':
            content += renderPlaylists(theme);
            break;
    }

    // Modal/Dialog Title overlay if relevant? No, keep it simple.

    return `<svg width="${WIDTH}" height="${HEIGHT}" xmlns="http://www.w3.org/2000/svg">
        <rect width="100%" height="100%" fill="${theme.bg}" />
        ${content}
        <text x="10" y="${HEIGHT - 10}" font-family="Arial" font-size="10" fill="${theme.textSec}">Mock: ${screenName} (${themeName})</text>
    </svg>`;
}

function renderHome(t) {
    return `<text x="20" y="80" font-family="Arial" font-size="24" font-weight="bold" fill="${t.text}">Good Morning</text>
            <rect x="20" y="110" width="150" height="50" rx="8" fill="${t.surface}" />
            <rect x="190" y="110" width="150" height="50" rx="8" fill="${t.surface}" />

            <text x="20" y="200" font-family="Arial" font-size="18" fill="${t.text}">Made For You</text>
            <rect x="20" y="220" width="120" height="120" rx="8" fill="${t.surface}" />
            <rect x="150" y="220" width="120" height="120" rx="8" fill="${t.surface}" />
            <rect x="280" y="220" width="120" height="120" rx="8" fill="${t.surface}" />`;
}

function renderLibrary(t) {
     return `<text x="20" y="80" font-family="Arial" font-size="24" font-weight="bold" fill="${t.text}">Library</text>
             <rect x="20" y="100" width="80" height="32" rx="16" fill="${t.accent}" />
             <text x="35" y="120" font-family="Arial" font-size="14" fill="${t.bg}">Songs</text>
             <text x="120" y="120" font-family="Arial" font-size="14" fill="${t.textSec}">Albums</text>

             ${renderListItem(t, 160, "Liked Songs", "Auto Playlist")}
             ${renderListItem(t, 230, "Recent Hits", "Playlist")}
             ${renderListItem(t, 300, "Jazz Vibes", "Playlist")}`;
}

function renderPlayer(t) {
    return `<rect x="40" y="80" width="280" height="280" rx="16" fill="${t.surface}" />
            <text x="40" y="400" font-family="Arial" font-size="24" font-weight="bold" fill="${t.text}">Song Title</text>
            <text x="40" y="430" font-family="Arial" font-size="18" fill="${t.textSec}">Artist Name</text>

            <rect x="40" y="480" width="280" height="4" rx="2" fill="${t.surface}" />
            <rect x="40" y="480" width="100" height="4" rx="2" fill="${t.accent}" />

            <circle cx="180" cy="550" r="32" fill="${t.accent}" />
            <path d="M175,540 L175,560 L190,550 Z" fill="${t.bg}" />`;
}

function renderSettings(t) {
    return `<text x="20" y="80" font-family="Arial" font-size="24" font-weight="bold" fill="${t.text}">Settings</text>
            ${renderListItem(t, 120, "Appearance", "Dark Mode, Accents")}
            ${renderListItem(t, 190, "Playback", "Crossfade, Quality")}
            ${renderListItem(t, 260, "Data & Storage", "Cache, Proxy")}`;
}

function renderSearch(t) {
    return `<rect x="20" y="60" width="320" height="48" rx="24" fill="${t.surface}" />
            <text x="60" y="90" font-family="Arial" font-size="16" fill="${t.textSec}">Search songs, artists...</text>
            <text x="20" y="150" font-family="Arial" font-size="18" fill="${t.text}">Recent Searches</text>
            ${renderListItem(t, 180, "Vivi Music Theme", "Song")}
            ${renderListItem(t, 250, "Cyberpunk 2077", "Album")}`;
}

function renderArtist(t) {
    return `<rect x="0" y="0" width="${WIDTH}" height="250" fill="${t.surface}" />
            <text x="20" y="220" font-family="Arial" font-size="32" font-weight="bold" fill="${t.text}">Artist Name</text>
            <text x="20" y="240" font-family="Arial" font-size="14" fill="${t.textSec}">1.2M Subscribers</text>

            <text x="20" y="290" font-family="Arial" font-size="20" fill="${t.text}">Popular</text>
            ${renderListItem(t, 310, "Top Hit 1", "3:45")}
            ${renderListItem(t, 380, "Top Hit 2", "2:30")}`;
}

function renderAlbum(t) {
     return `<rect x="20" y="80" width="140" height="140" rx="8" fill="${t.surface}" />
             <text x="180" y="100" font-family="Arial" font-size="20" font-weight="bold" fill="${t.text}">Album Name</text>
             <text x="180" y="130" font-family="Arial" font-size="14" fill="${t.textSec}">Artist</text>
             <text x="180" y="150" font-family="Arial" font-size="12" fill="${t.textSec}">2024 • 12 Songs</text>

             ${renderListItem(t, 240, "1. Intro", "1:20")}
             ${renderListItem(t, 310, "2. Main Track", "4:12")}`;
}

function renderLyrics(t) {
    return `<text x="20" y="100" font-family="Arial" font-size="24" fill="${t.textSec}" opacity="0.5">Previous line...</text>
            <text x="20" y="150" font-family="Arial" font-size="28" font-weight="bold" fill="${t.text}">This is the active</text>
            <text x="20" y="190" font-family="Arial" font-size="28" font-weight="bold" fill="${t.text}">Lyrics line singing</text>
            <text x="20" y="240" font-family="Arial" font-size="24" fill="${t.textSec}" opacity="0.5">Next line coming up...</text>`;
}

function renderPlaylists(t) {
    return `<text x="20" y="80" font-family="Arial" font-size="24" font-weight="bold" fill="${t.text}">Playlists</text>
            <rect x="20" y="120" width="150" height="150" rx="8" fill="${t.surface}" />
            <text x="20" y="290" font-family="Arial" font-size="16" fill="${t.text}">My Favs</text>

            <rect x="190" y="120" width="150" height="150" rx="8" fill="${t.surface}" />
            <text x="190" y="290" font-family="Arial" font-size="16" fill="${t.text}">Gym Mix</text>

            <rect x="20" y="330" width="150" height="150" rx="8" fill="${t.surface}" />
            <text x="20" y="500" font-family="Arial" font-size="16" fill="${t.text}">Sleep</text>`;
}


function renderListItem(t, y, title, sub) {
    return `<rect x="20" y="${y}" width="48" height="48" rx="4" fill="${t.surface}" />
            <text x="80" y="${y + 20}" font-family="Arial" font-size="16" fill="${t.text}">${title}</text>
            <text x="80" y="${y + 40}" font-family="Arial" font-size="14" fill="${t.textSec}">${sub}</text>`;
}


// --- Main Execution ---
if (!fs.existsSync(OUTPUT_DIR)){
    fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

SCREENS.forEach(screen => {
    Object.keys(THEMES).forEach(themeName => {
        const svg = createSvg(screen, themeName);
        const fileName = \`mock_\${themeName}_\${screen.toLowerCase()}.svg\`;
        fs.writeFileSync(path.join(OUTPUT_DIR, fileName), svg);
        console.log(\`Generated \${fileName}\`);
    });
});
