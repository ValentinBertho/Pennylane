; ==============================================================================
;
;     Projet    : Interface Pennylane
;     Copyright : (c) 2025 MISMO
;
;     Fichier   : InterfacePennylane.iss
;     Création  : 05/03/2025 par Valentin Bertho
;     Màj       : 02/02/2026 - Script idempotent install/MAJ
;
;     Objet     : Script d'installation et de mise à jour avec configuration
;                 guidée. Idempotent : préserve la configuration existante
;                 lors des mises à jour.
;
; ==============================================================================

#define MyAppName         "Interface Pennylane"
#define MyAppVersion      "2.0.2"
#define MyAppPublisher    "MISMO"
#define MyAppURL          "http://www.mismo.fr/"
#define Copyright         "Copyright (C) 2025 MISMO"
#define ServiceName       "ath-pennylane"

[Setup]
; --- Identifiant unique pour détecter install existante (ne jamais changer) ---
AppId={{E8A3F2B1-7C4D-4E5F-9A6B-1D2E3F4A5B6C}

; Configuration générale
AppName=                  {#MyAppName}
AppVersion=               {#MyAppVersion}
AppVerName=               {#MyAppName} {#MyAppVersion}
AppPublisher=             {#MyAppPublisher}
AppPublisherURL=          {#MyAppURL}
AppCopyright=             {#Copyright}
VersionInfoVersion=       {#MyAppVersion}.0

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
DisableDirPage=           yes
DisableProgramGroupPage=  yes

; Options
PrivilegesRequired=       admin
ArchitecturesAllowed=     x64compatible
ArchitecturesInstallIn64BitMode= x64compatible
DisableStartupPrompt=     yes

; MAJ : Fermer l'application/service si en cours d'exécution
CloseApplications=        force
RestartApplications=      yes

[Languages]
Name: "fr"; MessagesFile: "compiler:Languages\French.isl"

[CustomMessages]
fr.WelcomeLabel2=Cet assistant va vous guider dans l'installation de l'Interface Pennylane.%n%nL'Interface Pennylane est une application de synchronisation bidirectionnelle entre ATHENEO et Pennylane.%n%nSi une version précédente est détectée, seuls les binaires seront mis à jour et votre configuration sera préservée.%n%nEn cas de nouvelle installation, vous serez guidé pour configurer :%n  • La connexion à la base de données SQL Server%n  • Les paramètres de l'API Pennylane%n  • Les options de synchronisation%n%nIl est recommandé de fermer toutes les autres applications avant de continuer.

[Dirs]
Name: "{app}"; Permissions: users-modify
Name: "{app}\logs"; Permissions: users-modify
Name: "{app}\temp"; Permissions: users-modify
Name: "{app}\config"; Permissions: users-modify
Name: "{app}\backup"; Permissions: users-modify
Name: "{app}\SQL"; Permissions: users-modify

[Files]
; Application principale - toujours mise à jour
Source: "target\interface-pennylane.jar";                           DestDir: "{app}";   Flags: ignoreversion uninsrestartdelete
Source: "deploy\interface-pennylane.xml";                           DestDir: "{app}";   Flags: ignoreversion uninsrestartdelete
Source: "deploy\interface-pennylane.exe";                           DestDir: "{app}";   Flags: ignoreversion uninsrestartdelete

; Java Runtime
Source: "deploy\jdk-21.0.3_windows-x64_bin.exe";                   DestDir: "{tmp}";   Flags: deleteafterinstall

; Scripts de base de données
Source: "structure\*.sql";                                          DestDir: "{app}\SQL"; Flags: ignoreversion recursesubdirs createallsubdirs uninsrestartdelete

; Scripts d'installation/désinstallation
Source: "deploy\install.bat";                                       DestDir: "{app}";   Flags: ignoreversion uninsrestartdelete
Source: "deploy\uninstall.bat";                                     DestDir: "{app}";   Flags: ignoreversion uninsrestartdelete

; Icônes
Source: "complus_gray.bmp";                                         DestDir: "{app}";   Flags: ignoreversion

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\interface-pennylane.exe"; Comment: "Interface Pennylane"
Name: "{group}\Configuration"; Filename: "{app}\application.yml"; Comment: "Fichier de configuration"
Name: "{group}\Logs"; Filename: "{app}\logs"; Comment: "Dossier des logs"
Name: "{group}\Désinstaller {#MyAppName}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\interface-pennylane.exe"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Créer un raccourci sur le bureau"; GroupDescription: "Raccourcis :";
Name: "installjava"; Description: "Installer Java 21 (requis si non installé)"; GroupDescription: "Composants requis :"; Flags: checkedonce

[Run]
; Installation de Java si nécessaire
Filename: "{tmp}\jdk-21.0.3_windows-x64_bin.exe"; Parameters: "/s INSTALL_SILENT=1 STATIC=0 AUTO_UPDATE=0 WEB_JAVA=1 WEB_JAVA_SECURITY_LEVEL=H WEB_ANALYTICS=0 EULA=0 REBOOT=0 SPONSORS=0 REMOVEOUTOFDATEJRES=1"; StatusMsg: "Installation de Java 21..."; Flags: waituntilterminated skipifdoesntexist; Tasks: installjava; Check: not IsJavaInstalled

; Proposition d'ouvrir le dossier d'installation
Filename: "{win}\explorer.exe"; Parameters: """{app}"""; Description: "Ouvrir le dossier d'installation"; Flags: nowait postinstall skipifsilent shellexec unchecked

[UninstallRun]
; Arrêt et désinstallation du service
Filename: "{app}\interface-pennylane.exe"; Parameters: "stop"; WorkingDir: "{app}"; Flags: runhidden; RunOnceId: "StopService"
Filename: "{app}\interface-pennylane.exe"; Parameters: "uninstall"; WorkingDir: "{app}"; Flags: runhidden; RunOnceId: "UninstallService"

[UninstallDelete]
Type: filesandordirs; Name: "{app}\logs"
Type: filesandordirs; Name: "{app}\temp"
Type: filesandordirs; Name: "{app}\backup"

[Code]
// ==============================================================================
// Variables globales
// ==============================================================================
var
  // Pages de configuration (seulement en nouvelle installation)
  DatabaseConfigPage: TInputQueryWizardPage;
  ApiConfigPage: TInputQueryWizardPage;
  AdvancedConfigPage: TInputQueryWizardPage;
  CronConfigPage: TInputQueryWizardPage;

  // Flag : true si c'est une mise à jour (application.yml existe déjà)
  IsUpgrade: Boolean;

// ==============================================================================
// Détecte si c'est une mise à jour via le registre (AppId)
// Utilisable dès InitializeWizard car ne dépend pas de {app}
// ==============================================================================
function DetectUpgradeFromRegistry: Boolean;
var
  InstallPath: String;
begin
  Result := RegQueryStringValue(HKLM,
    'SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\{E8A3F2B1-7C4D-4E5F-9A6B-1D2E3F4A5B6C}_is1',
    'InstallLocation', InstallPath);
  if Result then
    Result := FileExists(InstallPath + '\application.yml');
end;

// ==============================================================================
// Détecte si c'est une mise à jour via le chemin d'install (après résolution {app})
// Appelée dans CurPageChanged quand {app} est disponible
// ==============================================================================
procedure RefreshUpgradeDetection;
var
  YmlPath: String;
begin
  YmlPath := ExpandConstant('{app}\application.yml');
  IsUpgrade := FileExists(YmlPath);
end;

// ==============================================================================
// Vérifie si Java 21 est installé
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
// Arrête le service Windows avant la mise à jour des fichiers
// ==============================================================================
procedure StopServiceBeforeInstall;
var
  ExePath: String;
  ResultCode: Integer;
begin
  ExePath := ExpandConstant('{app}\interface-pennylane.exe');
  if FileExists(ExePath) then
  begin
    Log('Arrêt du service avant mise à jour...');
    Exec(ExePath, 'stop', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ResultCode);
    // Attendre un peu pour que le service s'arrête proprement
    Sleep(2000);
  end;
end;

// ==============================================================================
// Sauvegarde la configuration existante avant mise à jour
// ==============================================================================
procedure BackupExistingConfig;
var
  SourcePath, BackupPath: String;
begin
  SourcePath := ExpandConstant('{app}\application.yml');
  BackupPath := ExpandConstant('{app}\backup\application.yml.' + GetDateTimeString('yyyymmdd_hhnn', #0, #0));

  if FileExists(SourcePath) then
  begin
    Log('Sauvegarde de la configuration existante : ' + BackupPath);
    FileCopy(SourcePath, BackupPath, False);
  end;
end;

// ==============================================================================
// Installe et démarre le service Windows
// ==============================================================================
procedure InstallAndStartService;
var
  ExePath: String;
  ResultCode: Integer;
begin
  ExePath := ExpandConstant('{app}\interface-pennylane.exe');

  // Installer le service (idempotent : si déjà installé, WinSW gère)
  Log('Installation du service Windows...');
  Exec(ExePath, 'install', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ResultCode);

  // Démarrer le service
  Log('Démarrage du service Windows...');
  Exec(ExePath, 'start', ExpandConstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ResultCode);
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

  DatabaseConfigPage.Add('Serveur SQL Server :', False);
  DatabaseConfigPage.Values[0] := 'localhost\SQLEXPRESS';

  DatabaseConfigPage.Add('Nom de la base de données :', False);
  DatabaseConfigPage.Values[1] := 'ATHENEO';

  DatabaseConfigPage.Add('Utilisateur SQL Server :', False);
  DatabaseConfigPage.Values[2] := 'sa';

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

  ApiConfigPage.Add('Clé API Pennylane :', False);
  ApiConfigPage.Values[0] := '';

  ApiConfigPage.Add('URL du service WS Document :', False);
  ApiConfigPage.Values[1] := 'http://localhost:8081/WSDocumentAth/WSDocumentAth.svc';

  ApiConfigPage.Add('Login WS Document :', False);
  ApiConfigPage.Values[2] := 'Admin';

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

  AdvancedConfigPage.Add('Port du serveur HTTP (défaut: 8088) :', False);
  AdvancedConfigPage.Values[0] := '8088';

  AdvancedConfigPage.Add('Niveau de log (INFO, DEBUG, TRACE) :', False);
  AdvancedConfigPage.Values[1] := 'INFO';
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

  CronConfigPage.Add('Sync écritures comptables (Entries) :', False);
  CronConfigPage.Values[0] := '*/10 * * * * *';

  CronConfigPage.Add('Import factures fournisseurs (Purchases) :', False);
  CronConfigPage.Values[1] := '-';

  CronConfigPage.Add('MAJ BAP factures (UpdateSale) :', False);
  CronConfigPage.Values[2] := '-';

  CronConfigPage.Add('MAJ règlements (UpdatePurchaseReglement) :', False);
  CronConfigPage.Values[3] := '-';
end;

// ==============================================================================
// Validation de la page base de données
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
// Validation de la page API
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
// Génération du fichier application.yml (SEULEMENT en nouvelle installation)
// ==============================================================================
procedure GenerateApplicationYml;
var
  ConfigFile: TStringList;
  ConfigPath: String;
  DbServer, DbName, DbUser, DbPassword: String;
  ApiKey, WsUrl, WsLogin, WsPassword: String;
  ServerPort, LogLevel: String;
  CronEntries, CronPurchases, CronUpdateSale, CronUpdateReglement: String;
begin
  ConfigPath := ExpandConstant('{app}\application.yml');
  ConfigFile := TStringList.Create;
  try
    // Récupération des valeurs saisies
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

    CronEntries := CronConfigPage.Values[0];
    CronPurchases := CronConfigPage.Values[1];
    CronUpdateSale := CronConfigPage.Values[2];
    CronUpdateReglement := CronConfigPage.Values[3];

    // ===== Construction du fichier YAML complet =====
    ConfigFile.Add('# ==============================================================================');
    ConfigFile.Add('# Configuration Interface Pennylane v{#MyAppVersion}');
    ConfigFile.Add('# Généré le ' + GetDateTimeString('dd/mm/yyyy à hh:nn', #0, #0));
    ConfigFile.Add('# ==============================================================================');
    ConfigFile.Add('');

    // --- Spring ---
    ConfigFile.Add('spring:');
    ConfigFile.Add('  application:');
    ConfigFile.Add('    name: Pennylane');
    ConfigFile.Add('  profiles:');
    ConfigFile.Add('    active: PROD');
    ConfigFile.Add('');
    ConfigFile.Add('  datasource:');
    ConfigFile.Add('    url: jdbc:sqlserver://' + DbServer + ';databaseName=' + DbName + ';encrypt=false');
    ConfigFile.Add('    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver');
    ConfigFile.Add('    username: ' + DbUser);
    ConfigFile.Add('    password: ' + DbPassword);
    ConfigFile.Add('');
    ConfigFile.Add('  jpa:');
    ConfigFile.Add('    show-sql: false');
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
    ConfigFile.Add('  data:');
    ConfigFile.Add('    web:');
    ConfigFile.Add('      pageable:');
    ConfigFile.Add('        default-page-size: 50');
    ConfigFile.Add('        max-page-size: 500');
    ConfigFile.Add('');
    ConfigFile.Add('  thymeleaf:');
    ConfigFile.Add('    prefix: classpath:/templates/');
    ConfigFile.Add('    suffix: .html');
    ConfigFile.Add('    mode: HTML');
    ConfigFile.Add('    encoding: UTF-8');
    ConfigFile.Add('    cache: true');
    ConfigFile.Add('');
    ConfigFile.Add('  task:');
    ConfigFile.Add('    scheduling:');
    ConfigFile.Add('      pool:');
    ConfigFile.Add('        size: 5');
    ConfigFile.Add('      thread-name-prefix: "pennylane-scheduler-"');
    ConfigFile.Add('');

    // --- Actuator ---
    ConfigFile.Add('management:');
    ConfigFile.Add('  endpoints:');
    ConfigFile.Add('    web:');
    ConfigFile.Add('      exposure:');
    ConfigFile.Add('        include: health,info,metrics,circuitbreakers,circuitbreakerevents,retries,retryevents,ratelimiters,bulkheads');
    ConfigFile.Add('  endpoint:');
    ConfigFile.Add('    health:');
    ConfigFile.Add('      show-details: always');
    ConfigFile.Add('  health:');
    ConfigFile.Add('    circuitbreakers:');
    ConfigFile.Add('      enabled: true');
    ConfigFile.Add('    ratelimiters:');
    ConfigFile.Add('      enabled: true');
    ConfigFile.Add('');

    // --- Resilience4j ---
    ConfigFile.Add('resilience4j:');
    ConfigFile.Add('  circuitbreaker:');
    ConfigFile.Add('    instances:');
    ConfigFile.Add('      pennylaneAPI:');
    ConfigFile.Add('        registerHealthIndicator: true');
    ConfigFile.Add('        slidingWindowSize: 10');
    ConfigFile.Add('        minimumNumberOfCalls: 5');
    ConfigFile.Add('        permittedNumberOfCallsInHalfOpenState: 3');
    ConfigFile.Add('        automaticTransitionFromOpenToHalfOpenEnabled: true');
    ConfigFile.Add('        waitDurationInOpenState: 30s');
    ConfigFile.Add('        failureRateThreshold: 50');
    ConfigFile.Add('        eventConsumerBufferSize: 10');
    ConfigFile.Add('        recordExceptions:');
    ConfigFile.Add('          - org.springframework.web.client.HttpServerErrorException');
    ConfigFile.Add('          - org.springframework.web.client.ResourceAccessException');
    ConfigFile.Add('          - java.io.IOException');
    ConfigFile.Add('          - java.util.concurrent.TimeoutException');
    ConfigFile.Add('      databaseOperations:');
    ConfigFile.Add('        registerHealthIndicator: true');
    ConfigFile.Add('        slidingWindowSize: 10');
    ConfigFile.Add('        minimumNumberOfCalls: 5');
    ConfigFile.Add('        waitDurationInOpenState: 20s');
    ConfigFile.Add('        failureRateThreshold: 60');
    ConfigFile.Add('        recordExceptions:');
    ConfigFile.Add('          - java.sql.SQLException');
    ConfigFile.Add('          - org.springframework.dao.DataAccessException');
    ConfigFile.Add('  retry:');
    ConfigFile.Add('    instances:');
    ConfigFile.Add('      pennylaneAPI:');
    ConfigFile.Add('        maxAttempts: 3');
    ConfigFile.Add('        waitDuration: 1s');
    ConfigFile.Add('        enableExponentialBackoff: true');
    ConfigFile.Add('        exponentialBackoffMultiplier: 2');
    ConfigFile.Add('        retryExceptions:');
    ConfigFile.Add('          - org.springframework.web.client.HttpServerErrorException');
    ConfigFile.Add('          - org.springframework.web.client.ResourceAccessException');
    ConfigFile.Add('          - java.net.SocketTimeoutException');
    ConfigFile.Add('        ignoreExceptions:');
    ConfigFile.Add('          - org.springframework.web.client.HttpClientErrorException.BadRequest');
    ConfigFile.Add('          - org.springframework.web.client.HttpClientErrorException.Unauthorized');
    ConfigFile.Add('          - org.springframework.web.client.HttpClientErrorException.NotFound');
    ConfigFile.Add('      databaseOperations:');
    ConfigFile.Add('        maxAttempts: 2');
    ConfigFile.Add('        waitDuration: 500ms');
    ConfigFile.Add('        retryExceptions:');
    ConfigFile.Add('          - org.springframework.dao.TransientDataAccessException');
    ConfigFile.Add('  ratelimiter:');
    ConfigFile.Add('    instances:');
    ConfigFile.Add('      pennylaneAPI:');
    ConfigFile.Add('        registerHealthIndicator: true');
    ConfigFile.Add('        limitForPeriod: 100');
    ConfigFile.Add('        limitRefreshPeriod: 60s');
    ConfigFile.Add('        timeoutDuration: 5s');
    ConfigFile.Add('        eventConsumerBufferSize: 100');
    ConfigFile.Add('  bulkhead:');
    ConfigFile.Add('    instances:');
    ConfigFile.Add('      schedulerThreadPool:');
    ConfigFile.Add('        maxConcurrentCalls: 5');
    ConfigFile.Add('        maxWaitDuration: 10ms');
    ConfigFile.Add('      pennylaneAPI:');
    ConfigFile.Add('        maxConcurrentCalls: 10');
    ConfigFile.Add('        maxWaitDuration: 100ms');
    ConfigFile.Add('  timelimiter:');
    ConfigFile.Add('    instances:');
    ConfigFile.Add('      pennylaneAPI:');
    ConfigFile.Add('        timeoutDuration: 30s');
    ConfigFile.Add('        cancelRunningFuture: true');
    ConfigFile.Add('      databaseOperations:');
    ConfigFile.Add('        timeoutDuration: 60s');
    ConfigFile.Add('        cancelRunningFuture: true');
    ConfigFile.Add('');

    // --- Logging ---
    ConfigFile.Add('logging:');
    ConfigFile.Add('  pattern:');
    ConfigFile.Add('    console: ''%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n''');
    ConfigFile.Add('  level:');
    ConfigFile.Add('    root: ' + LogLevel);
    ConfigFile.Add('    fr.mismo: INFO');
    ConfigFile.Add('    org.springframework: INFO');
    ConfigFile.Add('');

    // --- Crons (toutes les tâches planifiées de l'application) ---
    ConfigFile.Add('# Tâches planifiées - format cron Spring (sec min h jour mois jour-sem)');
    ConfigFile.Add('# Utiliser "-" pour désactiver une tâche');
    ConfigFile.Add('cron:');
    ConfigFile.Add('  Entries: "' + CronEntries + '"');
    ConfigFile.Add('  Purchases: "' + CronPurchases + '"');
    ConfigFile.Add('  PurchasesV2: "-"');
    ConfigFile.Add('  UpdateSale: "' + CronUpdateSale + '"');
    ConfigFile.Add('  UpdatePurchaseReglement: "' + CronUpdateReglement + '"');
    ConfigFile.Add('  UpdatePurchaseReglementV2: "-"');
    ConfigFile.Add('  PurgeLog: "0 0 2 * * *"');
    ConfigFile.Add('');

    // --- Server ---
    ConfigFile.Add('server:');
    ConfigFile.Add('  port: ' + ServerPort);
    ConfigFile.Add('  servlet:');
    ConfigFile.Add('    context-path: /');
    ConfigFile.Add('  error:');
    ConfigFile.Add('    include-message: always');
    ConfigFile.Add('    include-binding-errors: always');
    ConfigFile.Add('    include-stacktrace: on_param');
    ConfigFile.Add('    include-exception: false');
    ConfigFile.Add('');

    // --- API Pennylane ---
    ConfigFile.Add('api:');
    ConfigFile.Add('  url_v1: https://app.pennylane.com/api/external/v2/');
    ConfigFile.Add('  url_v2: https://app.pennylane.com/api/external/v2/');
    ConfigFile.Add('  key: ' + ApiKey);
    ConfigFile.Add('');

    // --- Logs applicatifs ---
    ConfigFile.Add('Log:');
    ConfigFile.Add('  niveau:');
    ConfigFile.Add('    ERROR: ''Erreur ''');
    ConfigFile.Add('    WARN: ''Avertissement ''');
    ConfigFile.Add('    INFO: ''Information ''');
    ConfigFile.Add('    DEBUG: ''Debug ''');
    ConfigFile.Add('    TRACE: ''Détail ''');
    ConfigFile.Add('  Actif: true');
    ConfigFile.Add('  Initiateur: ''INTERFACE_PENNYLANE''');
    ConfigFile.Add('');

    // --- WS Document ---
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
    ConfigFile.Add('');

    // --- Facture ---
    ConfigFile.Add('# Configuration du traitement des factures fournisseurs');
    ConfigFile.Add('facture:');
    ConfigFile.Add('  statusAFiltrer:');
    ConfigFile.Add('    - to_be_processed');
    ConfigFile.Add('    - to_be_paid');
    ConfigFile.Add('  daysBackward: 360');
    ConfigFile.Add('  categoriesAFiltrer:');
    ConfigFile.Add('    - ACH');
    ConfigFile.Add('  lastInsertPurchases: 2024-01-01T00:00:00');

    // Sauvegarde
    ConfigFile.SaveToFile(ConfigPath);
    Log('application.yml généré avec succès : ' + ConfigPath);
  finally
    ConfigFile.Free;
  end;
end;

// ==============================================================================
// Initialisation : détection install vs MAJ, création des pages si nécessaire
// ==============================================================================
procedure InitializeWizard;
begin
  // Détection initiale via le registre (ne dépend pas de {app})
  IsUpgrade := DetectUpgradeFromRegistry;

  if IsUpgrade then
    Log('Mode MISE À JOUR détecté (registre) - les pages de configuration seront masquées')
  else
    Log('Mode NOUVELLE INSTALLATION détecté - affichage des pages de configuration');

  // Toujours créer les pages (nécessaire pour InnoSetup)
  // mais elles seront masquées en mode MAJ via ShouldSkipPage
  CreateDatabaseConfigPage;
  CreateApiConfigPage;
  CreateAdvancedConfigPage;
  CreateCronConfigPage;
end;

// ==============================================================================
// Recalcul du mode upgrade quand le répertoire d'installation change
// ==============================================================================
procedure CurPageChanged(CurPageID: Integer);
begin
  // Après la page de sélection du répertoire, {app} est résolu
  if CurPageID = wpSelectDir then
    RefreshUpgradeDetection;
end;

// ==============================================================================
// Masquer les pages de configuration en mode MAJ
// ==============================================================================
function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;

  // En mode MAJ, masquer toutes les pages de configuration
  if IsUpgrade then
  begin
    if (PageID = DatabaseConfigPage.ID) or
       (PageID = ApiConfigPage.ID) or
       (PageID = AdvancedConfigPage.ID) or
       (PageID = CronConfigPage.ID) then
    begin
      Result := True;
    end;
  end;
end;

// ==============================================================================
// Validation lors du passage à la page suivante
// ==============================================================================
function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;

  if CurPageID = DatabaseConfigPage.ID then
    Result := ValidateDatabaseConfig;

  if CurPageID = ApiConfigPage.ID then
    Result := ValidateApiConfig;
end;

// ==============================================================================
// Actions à chaque étape de l'installation
// ==============================================================================
procedure CurStepChanged(CurStep: TSetupStep);
begin
  // Avant l'installation des fichiers : arrêter le service et sauvegarder
  if CurStep = ssInstall then
  begin
    if IsUpgrade then
    begin
      StopServiceBeforeInstall;
      BackupExistingConfig;
    end;
  end;

  // Après l'installation des fichiers
  if CurStep = ssPostInstall then
  begin
    // Générer application.yml SEULEMENT en nouvelle installation
    if not IsUpgrade then
    begin
      GenerateApplicationYml;
    end;

    // Toujours (re)installer et (re)démarrer le service
    InstallAndStartService;
  end;
end;
