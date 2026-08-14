; VIVI Music DE — Windows installer (Inno Setup).
;
; The self-contained app image is produced by jpackage (Gradle
; `createDistributable`) and passed in through /DSourceDir. Version and icon
; paths are also supplied from the CI workflow so this file stays the single
; source of the installer layout.

#ifndef AppVersion
#define AppVersion "0.0.0-dev"
#endif
#ifndef InstallerVersion
#define InstallerVersion "0.0.0"
#endif
#ifndef SourceDir
#define SourceDir "."
#endif
#ifndef OutputDir
#define OutputDir "dist"
#endif
#ifndef IconFile
#define IconFile "desktop/icons/logo_vmde.ico"
#endif

#define AppName "VIVI Music DE"
#define AppExe "VIVIMusic.exe"
#define AppPublisher "VIVI Music"
#define AppId "com.vivi.vivimusic.desktop"

[Setup]
AppId={#AppId}
AppName={#AppName}
; Inno Setup requires a numeric application version. The full `-DE` SemVer stays
; visible in AppVerName and in the output filename.
AppVersion={#InstallerVersion}
AppVerName={#AppName} {#AppVersion}
AppPublisher={#AppPublisher}
AppPublisherURL=https://github.com/PiBOH/vivi-music
AppSupportURL=https://github.com/PiBOH/vivi-music
AppUpdatesURL=https://github.com/PiBOH/vivi-music/releases
DefaultDirName={autopf}\VIVIMusic
DefaultGroupName={#AppName}
DisableProgramGroupPage=yes
OutputDir={#OutputDir}
OutputBaseFilename=VIVIMusic-{#AppVersion}-setup
SetupIconFile={#IconFile}
WizardStyle=modern
; The jpackage image is 200+ MB; avoid solid compression so CI stays fast.
Compression=lzma
SolidCompression=no
PrivilegesRequired=admin
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
CloseApplications=yes
RestartApplications=no
Uninstallable=yes
UninstallDisplayIcon={app}\{#AppExe}
VersionInfoCompany={#AppPublisher}
VersionInfoDescription={#AppName} desktop client
VersionInfoProductName={#AppName}
VersionInfoVersion={#InstallerVersion}
VersionInfoProductVersion={#InstallerVersion}
VersionInfoCopyright=Copyright (c) 2026 VIVI Music

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "startmenu"; Description: "Create a Start Menu shortcut"; GroupDescription: "Additional shortcuts:"; Flags: checkedonce
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional shortcuts:"; Flags: unchecked
Name: "launchafterinstall"; Description: "Start {#AppName} when setup is complete"; GroupDescription: "Installation options:"; Flags: unchecked

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: startmenu
Name: "{autodesktop}\{#AppName}"; Filename: "{app}\{#AppExe}"; Tasks: desktopicon

[Run]
Filename: "{app}\{#AppExe}"; Description: "Start {#AppName}"; Flags: nowait postinstall skipifsilent; Tasks: launchafterinstall

[Code]
// Show a confirmation once the uninstaller has finished removing the app.
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
begin
  if CurUninstallStep = usPostUninstall then
    MsgBox('{#AppName} was successfully uninstalled.', mbInformation, MB_OK);
end;
