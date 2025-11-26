; --------------------------------------------------------------
;
;     Projet    : Interface Pennylane - Version 1.8.0
;     Copyright : (c) 2025 MISMO
;
;     Fichier   : InterfacePennylane.iss
;     Création  : 05/03/2025 par Valentin Bertho
;     Màj       : 23/07/2025 par Valentin Bertho
;
;     Objet     : Script d'installation
;
;----------------------------------------------------------------*)

#define MyAppName         "Interface Pennylane"
#define MyAppVersion      "1.8.0"
#define MyAppPublisher    "MISMO"
#define MyAppURL          "http://www.mismo.fr/"
#define Copyright         "Copyright (C) 2025 MISMO"
#define SourceDir=        "C:\GIT\interface-pennylane"
#define CheminPhysique=   "{pf}\InterfacePennylane"

[Setup]
DisableStartupPrompt=     True
DisableProgramGroupPage=  True
Compression=              lzma/ultra
OutputDir=                Bin
AppPublisher=             {#MyAppPublisher}
AppPublisherURL=          {#MyAppURL}
DefaultGroupName=         {#MyAppPublisher}

OutputBaseFilename=       Interface Pennylane {#MyAppVersion} - Install

AppName=                  InterfacePennylane
AppVerName=               InterfacePennylane
AppVersion=               {#MyAppVersion}                                                      
VersionInfoVersion=       {#MyAppVersion}
DefaultDirName=           {pf}\InterfacePennylane
AppCopyright=             {#MyAppPublisher}
UninstallDisplayIcon=     {app}\complus_gray.ico
WizardImageFile=          complus_gray.bmp
WizardSmallImageFile=     complus_gray.bmp
DisableWelcomePage=no
DisableDirPage=no

[Languages]
Name: "fr"; MessagesFile: "compiler:Languages\French.isl"

[Dirs]
Name: "{app}"; Permissions: users-modify

[Files]
Source: "target\interface-pennylane.jar";                           DestDir: "{app}";                                   Flags: ignoreversion uninsrestartdelete;
Source: "deploy\interface-pennylane.xml";                           DestDir: "{app}";                                   Flags: ignoreversion uninsrestartdelete
Source: "deploy\interface-pennylane.exe";                           DestDir: "{app}";                                   Flags: ignoreversion uninsrestartdelete;
Source: "deploy\jdk-21.0.3_windows-x64_bin.exe";                    DestDir: "{app}\JRE";                               Flags: ignoreversion uninsrestartdelete
Source: "deploy\application.yml";                                   DestDir: "{app}";                                   Flags: ignoreversion uninsrestartdelete onlyifdoesntexist;
Source: "structure\*.sql";                                          DestDir: "{app}\SQL";                               Flags: ignoreversion recursesubdirs createallsubdirs uninsrestartdelete
Source: "deploy\install.bat";                                       DestDir: "{app}";                                   Flags: ignoreversion uninsrestartdelete; 
Source: "deploy\uninstall.bat";                                     DestDir: "{app}";                                   Flags: ignoreversion uninsrestartdelete;





















