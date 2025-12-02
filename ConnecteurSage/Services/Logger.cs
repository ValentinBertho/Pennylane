using System;
using System.Data;
using System.Data.SqlClient;
using System.Threading.Tasks;
using Dapper;
using AtheneoSageSync.Models;
using AtheneoSageSync.Utils;

namespace AtheneoSageSync.Services
{
    public static class Logger
    {
        /// <summary>
        /// Log le résultat d'export d'une facture
        /// </summary>
        /// <param name="facture">Facture concernée</param>
        /// <param name="success">Succès ou échec de l'export</param>
        /// <param name="message">Message optionnel</param>
        public static void LogFacture(FactureAtheneo facture, bool success, string message = null)
        {
            using (var connection = new SqlConnection(ConfigurationManager.GetConnectionString()))
            {
                connection.Open();
                connection.Execute(
                    "SP_TRAITER_FACTURE",
                    new
                    {
                        NO_V_FACTURE = facture.NoVFacture,
                        STATUS = success ? "OK" : "KO",
                        MESSAGE = message ?? (success ? "Export réussi" : "Erreur inconnue"),
                        EXPORT_DATE = DateTime.Now
                    },
                    commandType: CommandType.StoredProcedure
                );
            }
        }

        /// <summary>
        /// Version asynchrone du logging
        /// </summary>
        /// <param name="facture">Facture concernée</param>
        /// <param name="success">Succès ou échec de l'export</param>
        /// <param name="message">Message optionnel</param>
        public static async Task LogFactureAsync(FactureAtheneo facture, bool success, string message = null)
        {
            using (var connection = new SqlConnection(ConfigurationManager.GetConnectionString()))
            {
                await connection.OpenAsync();
                await connection.ExecuteAsync(
                    "SP_LOG_EXPORT_FACTURE",
                    new
                    {
                        NO_V_FACTURE = facture.NoVFacture,
                        STATUS = success ? "OK" : "KO",
                        MESSAGE = message ?? (success ? "Export réussi" : "Erreur inconnue"),
                        EXPORT_DATE = DateTime.Now
                    },
                    commandType: CommandType.StoredProcedure
                );
            }
        }

        /// <summary>
        /// Log avec gestion d'erreur intégrée
        /// </summary>
        /// <param name="facture">Facture concernée</param>
        /// <param name="success">Succès ou échec de l'export</param>
        /// <param name="message">Message optionnel</param>
        public static void LogFactureSafe(FactureAtheneo facture, bool success, string message = null)
        {
            try
            {
                LogFacture(facture, success, message);
            }
            catch (Exception ex)
            {
                // Log dans le journal des événements Windows ou fichier
                // En cas d'échec du logging en base
                System.Diagnostics.EventLog.WriteEntry(
                    "AtheneoSageSync",
                    $"Erreur lors du logging de la facture {facture.NoVFacture}: {ex.Message}",
                    System.Diagnostics.EventLogEntryType.Error
                );
            }
        }

        /// <summary>
        /// Log par ID de facture (si la facture complète n'est pas disponible)
        /// </summary>
        /// <param name="factureId">ID de la facture</param>
        /// <param name="success">Succès ou échec de l'export</param>
        /// <param name="message">Message optionnel</param>
        public static void LogFactureById(int factureId, bool success, string message = null)
        {
            using (var connection = new SqlConnection(ConfigurationManager.GetConnectionString()))
            {
                connection.Open();
                connection.Execute(
                    "SP_LOG_EXPORT_FACTURE",
                    new
                    {
                        NO_V_FACTURE = factureId,
                        STATUS = success ? "OK" : "KO",
                        MESSAGE = message ?? (success ? "Export réussi" : "Erreur inconnue"),
                        EXPORT_DATE = DateTime.Now
                    },
                    commandType: CommandType.StoredProcedure
                );
            }
        }

        /// <summary>
        /// Version asynchrone du log par ID
        /// </summary>
        /// <param name="factureId">ID de la facture</param>
        /// <param name="success">Succès ou échec de l'export</param>
        /// <param name="message">Message optionnel</param>
        public static async Task LogFactureByIdAsync(int factureId, bool success, string message = null)
        {
            using (var connection = new SqlConnection(ConfigurationManager.GetConnectionString()))
            {
                await connection.OpenAsync();
                await connection.ExecuteAsync(
                    "SP_LOG_EXPORT_FACTURE",
                    new
                    {
                        NO_V_FACTURE = factureId,
                        STATUS = success ? "OK" : "KO",
                        MESSAGE = message ?? (success ? "Export réussi" : "Erreur inconnue"),
                        EXPORT_DATE = DateTime.Now
                    },
                    commandType: CommandType.StoredProcedure
                );
            }
        }
    }
}