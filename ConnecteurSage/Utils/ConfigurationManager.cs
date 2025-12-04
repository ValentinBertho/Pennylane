using System;
using System.IO;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace AtheneoSageSync.Utils
{
    /// <summary>
    /// Gestionnaire de configuration pour AppSettings.json
    /// </summary>
    public static class ConfigurationManager
    {
        private static dynamic _cachedSettings;
        private static readonly string ConfigPath = "Config/AppSettings.json";

        /// <summary>
        /// Charge les paramètres depuis AppSettings.json
        /// </summary>
        public static dynamic GetAppSettings()
        {
            if (_cachedSettings == null)
            {
                if (!File.Exists(ConfigPath))
                {
                    throw new FileNotFoundException($"Fichier de configuration non trouvé: {ConfigPath}");
                }

                string json = File.ReadAllText(ConfigPath);
                _cachedSettings = JsonConvert.DeserializeObject<dynamic>(json);
            }

            return _cachedSettings;
        }

        /// <summary>
        /// Récupère la chaîne de connexion Athénéo
        /// </summary>
        public static string GetConnectionString()
        {
            var settings = GetAppSettings();
            return settings.ConnectionStrings.AtheneoDb.ToString();
        }

        /// <summary>
        /// Récupère la configuration Sage
        /// </summary>
        public static dynamic GetSageConfig()
        {
            var settings = GetAppSettings();
            return settings.Sage;
        }

        /// <summary>
        /// Récupère la configuration de synchronisation
        /// </summary>
        public static dynamic GetSyncConfig()
        {
            var settings = GetAppSettings();
            return settings.Sync;
        }

        /// <summary>
        /// Récupère les filtres de synchronisation
        /// </summary>
        public static dynamic GetFilters()
        {
            var settings = GetAppSettings();
            return settings.Filters;
        }

        /// <summary>
        /// Récupère un paramètre spécifique avec une valeur par défaut
        /// </summary>
        public static T GetValue<T>(string path, T defaultValue)
        {
            try
            {
                var settings = GetAppSettings();
                var tokens = path.Split('.');
                dynamic current = settings;

                foreach (var token in tokens)
                {
                    current = current[token];
                    if (current == null)
                        return defaultValue;
                }

                return (T)Convert.ChangeType(current.ToString(), typeof(T));
            }
            catch
            {
                return defaultValue;
            }
        }

        /// <summary>
        /// Recharge le fichier de configuration (utile pour les tests)
        /// </summary>
        public static void ReloadSettings()
        {
            _cachedSettings = null;
        }
    }
}
