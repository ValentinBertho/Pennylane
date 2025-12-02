using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SqlClient;
using System.Linq;
using System.Threading.Tasks;
using Dapper;
using AtheneoSageSync.Models;
using AtheneoSageSync.Utils;
using Serilog;

namespace AtheneoSageSync.Services
{
    public class AtheneoReader
    {
        private readonly string _connectionString;

        public AtheneoReader()
        {
            _connectionString = ConfigurationManager.GetConnectionString();
        }

        /// <summary>
        /// Récupère les factures non exportées avec leurs lignes en utilisant les procédures stockées
        /// </summary>
        /// <returns>Liste des factures avec leurs lignes</returns>
        public List<FactureAtheneo> GetFacturesNonExportees()
        {
            var factures = new List<FactureAtheneo>();

            using (var connection = new SqlConnection(_connectionString))
            {
                connection.Open();

                // Étape 1 : Récupérer les ID des factures à exporter
                var ids = connection.Query<int>(
                    "SP_EXPORT_FACTURE",
                    commandType: CommandType.StoredProcedure
                ).ToList();

                if (!ids.Any())
                    return factures;

                // Étape 2 : Récupérer chaque facture individuellement
                foreach (var id in ids)
                {
                    try
                    {
                        var facture = GetFactureById(connection, id);
                        if (facture != null)
                        {
                            var lignes = GetLignesFactureById(connection, id);
                            facture.Lignes = lignes ?? new List<LigneFactureAtheneo>();
                            factures.Add(facture);
                        }
                    }
                    catch (Exception ex)
                    {
                        // Log l'erreur et continue avec la facture suivante
                        // Vous pouvez remplacer par votre système de logging
                        Log.Error($"Erreur lors du traitement de la facture {id}: {ex.Message}");
                        continue;
                    }
                }
            }

            return factures;
        }

        /// <summary>
        /// Version asynchrone pour de meilleures performances
        /// </summary>
        /// <returns>Liste des factures avec leurs lignes</returns>
        public async Task<List<FactureAtheneo>> GetFacturesNonExporteesAsync()
        {
            var factures = new List<FactureAtheneo>();

            using (var connection = new SqlConnection(_connectionString))
            {
                await connection.OpenAsync();

                // Étape 1 : Récupérer les ID des factures à exporter
                var ids = (await connection.QueryAsync<int>(
                    "SP_EXPORT_FACTURE",
                    commandType: CommandType.StoredProcedure
                )).ToList();

                if (!ids.Any())
                    return factures;

                // Étape 2 : Récupérer chaque facture individuellement
                foreach (var id in ids)
                {
                    try
                    {
                        var facture = await GetFactureByIdAsync(connection, id);
                        if (facture != null)
                        {
                            var lignes = await GetLignesFactureByIdAsync(connection, id);
                            facture.Lignes = lignes ?? new List<LigneFactureAtheneo>();
                            factures.Add(facture);
                        }
                    }
                    catch (Exception ex)
                    {
                        // Log l'erreur et continue avec la facture suivante
                        Log.Error($"Erreur lors du traitement de la facture {id}: {ex.Message}");
                        continue;
                    }
                }
            }

            return factures;
        }

        #region Méthodes privées synchrones

        /// <summary>
        /// Récupère une facture par son ID en utilisant la procédure stockée SP_GET_FACTURE
        /// </summary>
        /// <param name="connection">Connexion à la base de données</param>
        /// <param name="factureId">ID de la facture</param>
        /// <returns>Facture ou null si non trouvée</returns>
        private FactureAtheneo GetFactureById(IDbConnection connection, int factureId)
        {
            return connection.QueryFirstOrDefault<FactureAtheneo>(
                "SP_GET_FACTURE",
                new { NO_V_FACTURE = factureId },
                commandType: CommandType.StoredProcedure
            );
        }

        /// <summary>
        /// Récupère les lignes d'une facture par son ID en utilisant la procédure stockée SP_GET_FACTURE_LINES
        /// </summary>
        /// <param name="connection">Connexion à la base de données</param>
        /// <param name="factureId">ID de la facture</param>
        /// <returns>Liste des lignes de la facture</returns>
        private List<LigneFactureAtheneo> GetLignesFactureById(IDbConnection connection, int factureId)
        {
            var lignes = connection.Query<LigneFactureAtheneo>(
                "SP_GET_FACTURE_LINES",
                new { NO_V_FACTURE = factureId },
                commandType: CommandType.StoredProcedure
            );

            return lignes?.ToList() ?? new List<LigneFactureAtheneo>();
        }

        #endregion

        #region Méthodes privées asynchrones

        /// <summary>
        /// Version asynchrone - Récupère une facture par son ID
        /// </summary>
        /// <param name="connection">Connexion à la base de données</param>
        /// <param name="factureId">ID de la facture</param>
        /// <returns>Facture ou null si non trouvée</returns>
        private async Task<FactureAtheneo> GetFactureByIdAsync(IDbConnection connection, int factureId)
        {
            return await connection.QueryFirstOrDefaultAsync<FactureAtheneo>(
                "SP_GET_FACTURE",
                new { NO_V_FACTURE = factureId },
                commandType: CommandType.StoredProcedure
            );
        }

        /// <summary>
        /// Version asynchrone - Récupère les lignes d'une facture par son ID
        /// </summary>
        /// <param name="connection">Connexion à la base de données</param>
        /// <param name="factureId">ID de la facture</param>
        /// <returns>Liste des lignes de la facture</returns>
        private async Task<List<LigneFactureAtheneo>> GetLignesFactureByIdAsync(IDbConnection connection, int factureId)
        {
            var lignes = await connection.QueryAsync<LigneFactureAtheneo>(
                "SP_GET_FACTURE_LINES",
                new { NO_V_FACTURE = factureId },
                commandType: CommandType.StoredProcedure
            );

            return lignes?.ToList() ?? new List<LigneFactureAtheneo>();
        }

        #endregion

        #region Méthodes utilitaires optionnelles

        /// <summary>
        /// Récupère uniquement les IDs des factures à exporter
        /// </summary>
        /// <returns>Liste des IDs de facture</returns>
        public List<int> GetFactureIdsToExport()
        {
            using (var connection = new SqlConnection(_connectionString))
            {
                connection.Open();
                return connection.Query<int>(
                    "SP_EXPORT_FACTURE",
                    commandType: CommandType.StoredProcedure
                ).ToList();
            }
        }

        /// <summary>
        /// Version asynchrone - Récupère uniquement les IDs des factures à exporter
        /// </summary>
        /// <returns>Liste des IDs de facture</returns>
        public async Task<List<int>> GetFactureIdsToExportAsync()
        {
            using (var connection = new SqlConnection(_connectionString))
            {
                await connection.OpenAsync();
                var result = await connection.QueryAsync<int>(
                    "SP_EXPORT_FACTURE",
                    commandType: CommandType.StoredProcedure
                );
                return result.ToList();
            }
        }

        /// <summary>
        /// Récupère une facture complète (avec lignes) par son ID
        /// </summary>
        /// <param name="factureId">ID de la facture</param>
        /// <returns>Facture complète ou null</returns>
        public FactureAtheneo GetFactureComplete(int factureId)
        {
            using (var connection = new SqlConnection(_connectionString))
            {
                connection.Open();

                var facture = GetFactureById(connection, factureId);
                if (facture != null)
                {
                    facture.Lignes = GetLignesFactureById(connection, factureId);
                }

                return facture;
            }
        }

        /// <summary>
        /// Version asynchrone - Récupère une facture complète par son ID
        /// </summary>
        /// <param name="factureId">ID de la facture</param>
        /// <returns>Facture complète ou null</returns>
        public async Task<FactureAtheneo> GetFactureCompleteAsync(int factureId)
        {
            using (var connection = new SqlConnection(_connectionString))
            {
                await connection.OpenAsync();

                var facture = await GetFactureByIdAsync(connection, factureId);
                if (facture != null)
                {
                    facture.Lignes = await GetLignesFactureByIdAsync(connection, factureId);
                }

                return facture;
            }
        }

        #endregion
    }
}