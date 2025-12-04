using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SqlClient;
using System.Linq;

using AtheneoSageSync.Models;
using AtheneoSageSync.Utils;

using Dapper;
using Serilog;

namespace AtheneoSageSync.Services
{
    /// <summary>
    /// Service d'import des règlements depuis Sage vers Athénéo
    /// </summary>
    public class ImportReglementsService
    {
        private readonly string _connectionString;
        private readonly bool _dryRun;
        private readonly int _joursRetroactifs;

        public ImportReglementsService(bool dryRun = false)
        {
            _connectionString = ConfigurationManager.GetConnectionString();
            _dryRun = dryRun;

            // Chargement des paramètres depuis AppSettings
            var filters = ConfigurationManager.GetFilters();
            _joursRetroactifs = filters?.ImportReglements?.JoursRetroactifs ?? 7;
        }

        /// <summary>
        /// Lance l'import des règlements depuis Sage
        /// </summary>
        public void Run()
        {
            try
            {
                Log.Information("═══════════════════════════════════════════════════");
                Log.Information("🚀 IMPORT RÈGLEMENTS : Sage → Athénéo");
                Log.Information("═══════════════════════════════════════════════════");

                // Calcul de la période d'import
                DateTime dateFin = DateTime.Now;
                DateTime dateDebut = dateFin.AddDays(-_joursRetroactifs);

                Log.Information($"📅 Période: du {dateDebut:dd/MM/yyyy} au {dateFin:dd/MM/yyyy} ({_joursRetroactifs} jours)");

                // Connexion à Sage et récupération des règlements
                using (var sageConnector = new SageConnector(_dryRun))
                {
                    var reglementsSage = sageConnector.ImporterReglements(dateDebut, dateFin);

                    if (reglementsSage == null || reglementsSage.Count == 0)
                    {
                        Log.Information("ℹ️ Aucun règlement trouvé dans Sage pour cette période");
                        return;
                    }

                    Log.Information($"📥 {reglementsSage.Count} règlements récupérés depuis Sage");

                    // Statistiques par type
                    var statsParType = reglementsSage
                        .GroupBy(r => r.TypeReglement)
                        .Select(g => new { Type = g.Key, Count = g.Count() })
                        .ToList();

                    foreach (var stat in statsParType)
                    {
                        Log.Information($"  - {stat.Type}: {stat.Count} règlement(s)");
                    }

                    // Traitement de chaque règlement
                    int successCount = 0;
                    int errorCount = 0;

                    foreach (var reglementSage in reglementsSage)
                    {
                        try
                        {
                            if (ImporterReglement(reglementSage))
                            {
                                successCount++;
                            }
                            else
                            {
                                errorCount++;
                            }
                        }
                        catch (Exception ex)
                        {
                            Log.Error($"❌ Erreur import règlement {reglementSage.NumeroReglement}: {ex.Message}");
                            errorCount++;
                        }
                    }

                    // Rapport final
                    Log.Information("═══════════════════════════════════════════════════");
                    Log.Information($"✅ Import terminé : {successCount} réussis, {errorCount} erreurs");
                    Log.Information("═══════════════════════════════════════════════════");
                }
            }
            catch (Exception ex)
            {
                Log.Error($"❌ Erreur fatale lors de l'import : {ex.Message}");
                Log.Error(ex.StackTrace);
                throw;
            }
        }

        /// <summary>
        /// Importe un règlement dans la base Athénéo
        /// </summary>
        private bool ImporterReglement(ReglementSage reglementSage)
        {
            try
            {
                Log.Information($"🔄 Import règlement {reglementSage.NumeroReglement} - {reglementSage.NomTiers} - {reglementSage.MontantReglement:C}");

                if (_dryRun)
                {
                    Log.Information($"🧪 [DRY-RUN] Simulation import règlement {reglementSage.NumeroReglement}");
                    return true;
                }

                using (var connection = new SqlConnection(_connectionString))
                {
                    connection.Open();

                    var parameters = new DynamicParameters();
                    parameters.Add("@NUMERO_REGLEMENT", reglementSage.NumeroReglement);
                    parameters.Add("@NUMERO_FACTURE", reglementSage.NumeroFacture);
                    parameters.Add("@CODE_TIERS", reglementSage.CodeTiers);
                    parameters.Add("@NOM_TIERS", reglementSage.NomTiers);
                    parameters.Add("@DATE_REGLEMENT", reglementSage.DateReglement);
                    parameters.Add("@MONTANT_REGLEMENT", reglementSage.MontantReglement);
                    parameters.Add("@MODE_REGLEMENT", reglementSage.ModeReglement);
                    parameters.Add("@REFERENCE_REGLEMENT", reglementSage.ReferenceReglement);
                    parameters.Add("@COMPTE_COMPTABLE", reglementSage.CompteComptable);
                    parameters.Add("@JOURNAL", reglementSage.Journal);
                    parameters.Add("@TYPE_REGLEMENT", reglementSage.TypeReglement);
                    parameters.Add("@STATUT", reglementSage.Statut);

                    var result = connection.QueryFirstOrDefault<dynamic>(
                        "SP_IMPORT_REGLEMENT",
                        parameters,
                        commandType: CommandType.StoredProcedure
                    );

                    if (result == null)
                    {
                        throw new Exception("La procédure stockée n'a pas retourné de résultat");
                    }

                    int noReglement = result.NO_REGLEMENT;
                    string operation = result.OPERATION;

                    Log.Information($"  ✓ Règlement {operation} (ID: {noReglement})");
                    Log.Information($"✅ Règlement {reglementSage.NumeroReglement} importé avec succès");
                    return true;
                }
            }
            catch (SqlException ex)
            {
                Log.Error($"❌ Erreur SQL import règlement {reglementSage.NumeroReglement}: {ex.Message}");
                Log.Error($"   SQL Error Number: {ex.Number}");
                return false;
            }
            catch (Exception ex)
            {
                Log.Error($"❌ Erreur import règlement {reglementSage.NumeroReglement}: {ex.Message}");
                return false;
            }
        }
    }
}
