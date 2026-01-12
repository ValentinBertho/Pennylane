; ==============================================================================
;
;     Projet    : Interface Pennylane - Version 2.0.2
;     Copyright : (c) 2025 MISMO
;
;     Fichier   : InterfacePennylane.iss
;     Création  : 05/03/2025 par Valentin Bertho
;     Màj       : 31/12/2025 - Installation guidée améliorée
;
;     Objet     : Script d'installation avec configuration guidée
;
; ==============================================================================

#define MyAppName         "Interface Pennylane"
#define MyAppVersion      "2.0.2"
#define MyAppPublisher    "MISMO"
#define MyAppURL          "http://www.mismo.fr/"
#define Copyright         "Copyright (C) 2025 MISMO"
#define SourceDir=        "C:\GIT\interface-pennylane"
#define CheminPhysique=   "{pf}\InterfacePennylane"

[Setup]
; Configuration générale
AppName=                  {#MyAppName}
AppVersion=               {#MyAppVersion}
AppVerName=               {#MyAppName} {#MyAppVersion}
AppPublisher=             {#MyAppPublisher}
AppPublisherURL=          {#MyAppURL}
AppCopyright=             {#Copyright}
VersionInfoVersion=       {#MyAppVersion}

; Chemins d'installation
DefaultDirName=           {autopf}\InterfacePennylane
DefaultGroupName=         {#MyAppPublisher}\{#MyAppName}
UninstallDisplayIcon=     {app}\complus_gray.ico

; Sortie
OutputDir=                Bin
OutputBaseFilename=       InterfacePennylane_{#MyAppVersion}_Setup
Compression=              lzma2/ultra64
SolidCompression=         yes

; Interface
WizardStyle=              modern
WizardImageFile=          complus_gray.bmp
WizardSmallImageFile=     complus_gray.bmp
DisableWelcomePage=       no
DisableDirPage=           no
DisableProgramGroupPage=  no

; Options
PrivilegesRequired=       admin
ArchitecturesAllowed=     x64compatible
ArchitecturesInstallIn64BitMode= x64compatible
DisableStartupPrompt=     yes

[Languages]
Name: "fr"; MessagesFile: "compiler:Languages\French.isl"

[CustomMessages]
fr.WelcomeLabel2=Cet assistant va vous guider dans l'installation et la configuration de l'Interface Pennylane.%n%nL'Interface Pennylane est une application de synchronisation bidirectionnelle entre ATHENEO et Pennylane.%n%nVous serez guidé à travers plusieurs étapes pour configurer :%n  • La connexion à la base de données SQL Server%n  • Les paramètres de l'API Pennylane%n  • Les options de synchronisation%n%nL'installation prendra environ 5 minutes.%n%nIl est recommandé de fermer toutes les autres applications avant de continuer.

[Dirs]
Name: "{app}"; Permissions: users-modify
Name: "{app}\logs"; Permissions: users-modify
Name: "{app}\temp"; Permissions: users-modify
Name: "{app}\config"; Permissions: users-modify

[Files]
; Application principale
Source: "target\interface-pennylane.jar";                           DestDir: "{app}";                     Flags: ignoreversion uninsrestartdelete;
Source: "deploy\interface-pennylane.xml";                           DestDir: "{app}";                     Flags: ignoreversion uninsrestartdelete
Source: "deploy\interface-pennylane.exe";                           DestDir: "{app}";                     Flags: ignoreversion uninsrestartdelete;

; Java Runtime
Source: "deploy\jdk-21.0.3_windows-x64_bin.exe";                    DestDir: "{tmp}";                     Flags: deleteafterinstall

; Scripts de base de données
Source: "structure\*.sql";                                          DestDir: "{app}\SQL";                 Flags: ignoreversion recursesubdirs createallsubdirs uninsrestartdelete

; Scripts d'installation/désinstallation
Source: "deploy\install.bat";                                       DestDir: "{app}";                     Flags: ignoreversion uninsrestartdelete;
Source: "deploy\uninstall.bat";                                     DestDir: "{app}";                     Flags: ignoreversion uninsrestartdelete;

; Icônes
Source: "complus_gray.bmp";                                         DestDir: "{app}";                     Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\interface-pennylane.exe"; Comment: "Interface Pennylane"
Name: "{group}\Configuration"; Filename: "{app}\application.yml"; Comment: "Fichier de configuration"
Name: "{group}\Logs"; Filename: "{app}\logs"; Comment: "Dossier des logs"
Name: "{group}\Désinstaller {#MyAppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\interface-pennylane.exe"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Créer un raccourci sur le bureau"; GroupDescription: "Raccourcis :";
Name: "installjava"; Description: "Installer Java 21 (requis si non installé)"; GroupDescription: "Composants requis :"; Flags: checkedonce
Name: "installservice"; Description: "Installer et démarrer le service Windows automatiquement"; GroupDescription: "Service Windows :"; Flags: checkedonce
Name: "startservice"; Description: "Démarrer le service après l'installation"; GroupDescription: "Service Windows :"; Flags: checkedonce

[Run]
; Installation de Java si nécessaire
Filename: "{tmp}\jdk-21.0.3_windows-x64_bin.exe"; Parameters: "/s INSTALL_SILENT=1 STATIC=0 AUTO_UPDATE=0 WEB_JAVA=1 WEB_JAVA_SECURITY_LEVEL=H WEB_ANALYTICS=0 EULA=0 REBOOT=0 SPONSORS=0 REMOVEOUTOFDATEJRES=1"; StatusMsg: "Installation de Java 21..."; Flags: waituntilterminated skipifdoesntexist; Tasks: installjava; Check: not IsJavaInstalled

; Installation du service Windows
Filename: "{app}\interface-pennylane.exe"; Parameters: "install"; WorkingDir: "{app}"; StatusMsg: "Installation du service Windows..."; Flags: runhidden waituntilterminated; Tasks: installservice

; Démarrage du service
Filename: "{app}\interface-pennylane.exe"; Parameters: "start"; WorkingDir: "{app}"; StatusMsg: "Démarrage du service Interface Pennylane..."; Flags: runhidden waituntilterminated; Tasks: startservice

; Proposition d'ouvrir le dossier d'installation
Filename: "{win}\explorer.exe";     Parameters: """{app}""";                                Description: "📁 Ouvrir le dossier d'installation";          Flags: nowait postinstall skipifsilent shellexec unchecked

[UninstallRun]
; Arrêt et désinstallation du service
Filename: "{app}\interface-pennylane.exe"; Parameters: "stop"; WorkingDir: "{app}"; Flags: runhidden; RunOnceId: "StopService"
Filename: "{app}\interface-pennylane.exe"; Parameters: "uninstall"; WorkingDir: "{app}"; Flags: runhidden; RunOnceId: "UninstallService"

[Code]
// ==============================================================================
// Variables globales pour stocker la configuration
// ==============================================================================
var
  // Page de configuration de la base de données
  DatabaseConfigPage: TInputQueryWizardPage;

  // Page de configuration de l'API Pennylane
  ApiConfigPage: TInputQueryWizardPage;

  // Page de configuration des paramètres avancés
  AdvancedConfigPage: TInputQueryWizardPage;

  // Page de configuration des tâches planifiées
  CronConfigPage: TInputQueryWizardPage;

// ==============================================================================
// Fonction pour vérifier si Java est installé
// ==============================================================================
function IsJavaInstalled: Boolean;
var
  JavaVersion: String;
begin
  Result := RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\Java Development Kit\21', 'JavaHome', JavaVersion) or
            RegQueryStringValue(HKLM, 'SOFTWARE\JavaSoft\JDK\21', 'JavaHome', JavaVersion) or
            RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\Java Development Kit\21', 'JavaHome', JavaVersion) or
            RegQueryStringValue(HKLM64, 'SOFTWARE\JavaSoft\JDK\21', 'JavaHome', JavaVersion);
end;

// ==============================================================================
// Création de la page de configuration de la base de données
// ==============================================================================
procedure CreateDatabaseConfigPage;
begin
  DatabaseConfigPage := CreateInputQueryPage(wpSelectDir,
    'Configuration de la base de données',
    'Paramètres de connexion à SQL Server',
    'Veuillez saisir les informations de connexion à votre base de données SQL Server.' + #13#10 +
    'Ces informations seront utilisées pour synchroniser les données avec Pennylane.');

  // Serveur SQL Server
  DatabaseConfigPage.Add('Serveur SQL Server :', False);
  DatabaseConfigPage.Values[0] := 'localhost\SQLEXPRESS';

  // Nom de la base de données
  DatabaseConfigPage.Add('Nom de la base de données :', False);
  DatabaseConfigPage.Values[1] := 'ATHENEO';

  // Nom d'utilisateur
  DatabaseConfigPage.Add('Utilisateur SQL Server :', False);
  DatabaseConfigPage.Values[2] := 'sa';

  // Mot de passe
  DatabaseConfigPage.Add('Mot de passe :', True);
  DatabaseConfigPage.Values[3] := '';
end;

// ==============================================================================
// Création de la page de configuration de l'API Pennylane
// ==============================================================================
procedure CreateApiConfigPage;
begin
  ApiConfigPage := CreateInputQueryPage(DatabaseConfigPage.ID,
    'Configuration de l''API Pennylane',
    'Paramètres de connexion à Pennylane',
    'Veuillez saisir vos informations de connexion à l''API Pennylane.' + #13#10 +
    'Vous pouvez obtenir votre clé API depuis votre compte Pennylane dans Paramètres > Intégrations.');

  // Clé API Pennylane
  ApiConfigPage.Add('Clé API Pennylane :', False);
  ApiConfigPage.Values[0] := '';

  // URL du service WSDL Document
  ApiConfigPage.Add('URL du service WS Document :', False);
  ApiConfigPage.Values[1] := 'http://athsql2.mismo.local:8081/WSDocumentAth/WSDocumentAth.svc';

  // Login WS Document
  ApiConfigPage.Add('Login WS Document :', False);
  ApiConfigPage.Values[2] := 'Admin';

  // Password WS Document
  ApiConfigPage.Add('Mot de passe WS Document :', True);
  ApiConfigPage.Values[3] := 'ADMIN';
end;

// ==============================================================================
// Création de la page de configuration avancée
// ==============================================================================
procedure CreateAdvancedConfigPage;
begin
  AdvancedConfigPage := CreateInputQueryPage(ApiConfigPage.ID,
    'Configuration avancée',
    'Paramètres de l''application',
    'Configurez les paramètres avancés de l''interface.' + #13#10 +
    'Les valeurs par défaut conviennent dans la plupart des cas.');

  // Port du serveur
  AdvancedConfigPage.Add('Port du serveur HTTP (défaut: 8093) :', False);
  AdvancedConfigPage.Values[0] := '8093';

  // Niveau de log
  AdvancedConfigPage.Add('Niveau de log (INFO, DEBUG, TRACE) :', False);
  AdvancedConfigPage.Values[1] := 'INFO';

  // Activer les logs en base
  AdvancedConfigPage.Add('Activer les logs en base (true/false) :', False);
  AdvancedConfigPage.Values[2] := 'true';

  // Initiateur des logs
  AdvancedConfigPage.Add('Initiateur des logs :', False);
  AdvancedConfigPage.Values[3] := 'INTERFACE_PENNYLANE';
end;

// ==============================================================================
// Création de la page de configuration des tâches planifiées
// ==============================================================================
procedure CreateCronConfigPage;
begin
  CronConfigPage := CreateInputQueryPage(AdvancedConfigPage.ID,
    'Configuration des synchronisations',
    'Planification des tâches de synchronisation',
    'Configurez la fréquence de synchronisation des données.' + #13#10 +
    'Format CRON : "*/10 * * * * *" = toutes les 10 secondes' + #13#10 +
    'Utilisez "-" pour désactiver une synchronisation.');

  // Synchronisation des écritures comptables
  CronConfigPage.Add('Synchronisation des écritures (défaut: */10 * * * * *) :', False);
  CronConfigPage.Values[0] := '*/10 * * * * *';

  // Synchronisation des clients
  CronConfigPage.Add('Synchronisation des clients (défaut: -) :', False);
  CronConfigPage.Values[1] := '-';

  // Synchronisation des produits
  CronConfigPage.Add('Synchronisation des produits (défaut: -) :', False);
  CronConfigPage.Values[2] := '-';
end;

// ==============================================================================
// Validation des champs de la page base de données
// ==============================================================================
function ValidateDatabaseConfig: Boolean;
begin
  Result := True;

  if Trim(DatabaseConfigPage.Values[0]) = '' then
  begin
    MsgBox('Veuillez saisir le serveur SQL Server.', mbError, MB_OK);
    Result := False;
    Exit;
  end;

  if Trim(DatabaseConfigPage.Values[1]) = '' then
  begin
    MsgBox('Veuillez saisir le nom de la base de données.', mbError, MB_OK);
    Result := False;
    Exit;
  end;

  if Trim(DatabaseConfigPage.Values[2]) = '' then
  begin
    MsgBox('Veuillez saisir le nom d''utilisateur SQL Server.', mbError, MB_OK);
    Result := False;
    Exit;
  end;
end;

// ==============================================================================
// Validation des champs de la page API
// ==============================================================================
function ValidateApiConfig: Boolean;
begin
  Result := True;

  if Trim(ApiConfigPage.Values[0]) = '' then
  begin
    MsgBox('Veuillez saisir votre clé API Pennylane.' + #13#10 +
           'Vous pouvez l''obtenir depuis votre compte Pennylane dans Paramètres > Intégrations.',
           mbError, MB_OK);
    Result := False;
    Exit;
  end;
end;

// ==============================================================================
// Génération du fichier application.yml
// ==============================================================================
procedure GenerateApplicationYml;
var
  ConfigFile: TStringList;
  ConfigPath: String;
  DbServer, DbName, DbUser, DbPassword: String;
  ApiKey, WsUrl, WsLogin, WsPassword: String;
  ServerPort, LogLevel, LogActive, LogInitiator: String;
  CronEntries, CronCustomer, CronProduct: String;
begin
  ConfigPath := ExpandConstant('{app}\application.yml');
  ConfigFile := TStringList.Create;
  try
    // Récupération des valeurs
    DbServer := DatabaseConfigPage.Values[0];
    DbName := DatabaseConfigPage.Values[1];
    DbUser := DatabaseConfigPage.Values[2];
    DbPassword := DatabaseConfigPage.Values[3];

    ApiKey := ApiConfigPage.Values[0];
    WsUrl := ApiConfigPage.Values[1];
    WsLogin := ApiConfigPage.Values[2];
    WsPassword := ApiConfigPage.Values[3];

    ServerPort := AdvancedConfigPage.Values[0];
    LogLevel := AdvancedConfigPage.Values[1];
    LogActive := AdvancedConfigPage.Values[2];
    LogInitiator := AdvancedConfigPage.Values[3];

    CronEntries := CronConfigPage.Values[0];
    CronCustomer := CronConfigPage.Values[1];
    CronProduct := CronConfigPage.Values[2];

    // Construction du fichier YAML
    ConfigFile.Add('# ==============================================================================');
    ConfigFile.Add('# Configuration Interface Pennylane');
    ConfigFile.Add('# Généré automatiquement le ' + GetDateTimeString('dd/mm/yyyy à hh:nn', #0, #0));
    ConfigFile.Add('# ==============================================================================');
    ConfigFile.Add('');
    ConfigFile.Add('spring:');
    ConfigFile.Add('  datasource:');
    ConfigFile.Add('    url: jdbc:sqlserver://' + DbServer + ';databaseName=' + DbName + ';encrypt=false');
    ConfigFile.Add('    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver');
    ConfigFile.Add('    username: ' + DbUser);
    ConfigFile.Add('    password: ' + DbPassword);
    ConfigFile.Add('');
    ConfigFile.Add('  jpa:');
    ConfigFile.Add('    show-sql: true');
    ConfigFile.Add('    hibernate:');
    ConfigFile.Add('      naming:');
    ConfigFile.Add('        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl');
    ConfigFile.Add('');
    ConfigFile.Add('  servlet:');
    ConfigFile.Add('    multipart:');
    ConfigFile.Add('      enabled: true');
    ConfigFile.Add('      max-file-size: 10MB');
    ConfigFile.Add('      max-request-size: 10MB');
    ConfigFile.Add('');
    ConfigFile.Add('logging:');
    ConfigFile.Add('  pattern:');
    ConfigFile.Add('    console: ''%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n''');
    ConfigFile.Add('  level:');
    ConfigFile.Add('    root: ' + LogLevel);
    ConfigFile.Add('    fr.mismo: TRACE');
    ConfigFile.Add('    org.springframework: INFO');
    ConfigFile.Add('');
    ConfigFile.Add('cron:');
    ConfigFile.Add('  Logging: "-"');
    ConfigFile.Add('  Customer: "' + CronCustomer + '"');
    ConfigFile.Add('  Entries: "' + CronEntries + '"');
    ConfigFile.Add('  Deadlines: "-"');
    ConfigFile.Add('  Account: "-"');
    ConfigFile.Add('  Journals: "-"');
    ConfigFile.Add('  Analytic: "-"');
    ConfigFile.Add('  Payment: "-"');
    ConfigFile.Add('  Product: "' + CronProduct + '"');
    ConfigFile.Add('');
    ConfigFile.Add('server:');
    ConfigFile.Add('  port: ' + ServerPort);
    ConfigFile.Add('  servlet:');
    ConfigFile.Add('    context-path: /api/v1');
    ConfigFile.Add('    session:');
    ConfigFile.Add('      timeout: 60s');
    ConfigFile.Add('');
    ConfigFile.Add('api:');
    ConfigFile.Add('  url_v1: https://app.pennylane.com/api/external/v1/');
    ConfigFile.Add('  url_v2: https://app.pennylane.com/api/external/v2/');
    ConfigFile.Add('  key: ' + ApiKey);
    ConfigFile.Add('');
    ConfigFile.Add('Log:');
    ConfigFile.Add('  niveau:');
    ConfigFile.Add('    ERROR: ''Erreur ''');
    ConfigFile.Add('    WARN: ''Avertissement ''');
    ConfigFile.Add('    INFO: ''Information ''');
    ConfigFile.Add('    DEBUG: ''Debug ''');
    ConfigFile.Add('    TRACE: ''Détail ''');
    ConfigFile.Add('  Actif: ' + LogActive);
    ConfigFile.Add('  Initiateur: ''' + LogInitiator + '''');
    ConfigFile.Add('');
    ConfigFile.Add('wsdocument:');
    ConfigFile.Add('  defaultUri: ' + WsUrl);
    ConfigFile.Add('  proprieteDocument:');
    ConfigFile.Add('    typeDocument: PENNYLANE');
    ConfigFile.Add('    auteurDocument: ADMIN');
    ConfigFile.Add('  login: ' + WsLogin);
    ConfigFile.Add('  password: ' + WsPassword);
    ConfigFile.Add('  timeouts:');
    ConfigFile.Add('    readTimeout: 60000');
    ConfigFile.Add('    connectionTimeout: 60000');

    // Sauvegarde du fichier
    ConfigFile.SaveToFile(ConfigPath);
  finally
    ConfigFile.Free;
  end;
end;

// ==============================================================================
// Génération du fichier README.txt
// ==============================================================================
procedure GenerateReadmeFile;
var
  ReadmeFile: TStringList;
  ReadmePath: String;
begin
  ReadmePath := ExpandConstant('{app}\README.txt');
  ReadmeFile := TStringList.Create;
  try
    ReadmeFile.Add('================================================================================');
    ReadmeFile.Add('  INTERFACE PENNYLANE - Guide de démarrage rapide');
    ReadmeFile.Add('  Version 2.0.0');
    ReadmeFile.Add('  Copyright (C) 2025 MISMO');
    ReadmeFile.Add('================================================================================');
    ReadmeFile.Add('');
    ReadmeFile.Add('DESCRIPTION');
    ReadmeFile.Add('------------');
    ReadmeFile.Add('Interface de synchronisation bidirectionnelle entre ATHENEO et Pennylane.');
    ReadmeFile.Add('');
    ReadmeFile.Add('FICHIERS IMPORTANTS');
    ReadmeFile.Add('-------------------');
    ReadmeFile.Add('  application.yml          Configuration de l''application');
    ReadmeFile.Add('  interface-pennylane.jar  Application Java Spring Boot');
    ReadmeFile.Add('  interface-pennylane.exe  Gestionnaire de service Windows (WinSW)');
    ReadmeFile.Add('  interface-pennylane.xml  Configuration du service Windows');
    ReadmeFile.Add('  logs\                    Dossier contenant les fichiers de log');
    ReadmeFile.Add('  SQL\                     Scripts SQL de la base de données');
    ReadmeFile.Add('');
    ReadmeFile.Add('GESTION DU SERVICE WINDOWS');
    ReadmeFile.Add('--------------------------');
    ReadmeFile.Add('  Démarrer le service :    interface-pennylane.exe start');
    ReadmeFile.Add('  Arrêter le service :     interface-pennylane.exe stop');
    ReadmeFile.Add('  Redémarrer le service :  interface-pennylane.exe restart');
    ReadmeFile.Add('  État du service :        interface-pennylane.exe status');
    ReadmeFile.Add('');
    ReadmeFile.Add('ACCÈS À L''APPLICATION');
    ReadmeFile.Add('----------------------');
    ReadmeFile.Add('  URL locale : http://localhost:' + AdvancedConfigPage.Values[0] + '/api/v1');
    ReadmeFile.Add('');
    ReadmeFile.Add('LOGS');
    ReadmeFile.Add('-----');
    ReadmeFile.Add('  Les logs sont disponibles dans le dossier : logs\');
    ReadmeFile.Add('  Niveau de log configuré : ' + AdvancedConfigPage.Values[1]);
    ReadmeFile.Add('');
    ReadmeFile.Add('CONFIGURATION');
    ReadmeFile.Add('-------------');
    ReadmeFile.Add('  Pour modifier la configuration, éditez le fichier application.yml');
    ReadmeFile.Add('  puis redémarrez le service.');
    ReadmeFile.Add('');
    ReadmeFile.Add('SUPPORT');
    ReadmeFile.Add('--------');
    ReadmeFile.Add('  Pour toute question ou assistance, contactez le support MISMO.');
    ReadmeFile.Add('  Site web : http://www.mismo.fr/');
    ReadmeFile.Add('');
    ReadmeFile.Add('================================================================================');

    ReadmeFile.SaveToFile(ReadmePath);
  finally
    ReadmeFile.Free;
  end;
end;

// ==============================================================================
// Initialisation des pages personnalisées
// ==============================================================================
procedure InitializeWizard;
begin
  // Création des pages de configuration
  CreateDatabaseConfigPage;
  CreateApiConfigPage;
  CreateAdvancedConfigPage;
  CreateCronConfigPage;
end;

// ==============================================================================
// Validation lors du passage à la page suivante
// ==============================================================================
function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;

  // Validation de la page de configuration de la base de données
  if CurPageID = DatabaseConfigPage.ID then
  begin
    Result := ValidateDatabaseConfig;
  end;

  // Validation de la page de configuration de l'API
  if CurPageID = ApiConfigPage.ID then
  begin
    Result := ValidateApiConfig;
  end;
end;

// ==============================================================================
// Actions après l'installation des fichiers
// ==============================================================================
procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
  begin
    // Génération du fichier de configuration
    GenerateApplicationYml;

    // Génération du fichier README
    GenerateReadmeFile;
  end;
end;





















