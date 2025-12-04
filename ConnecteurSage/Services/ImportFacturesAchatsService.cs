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
    /// Service d'import des factures d'achat depuis Sage vers Athénéo
    /// </summary>
    public class ImportFacturesAchatsService
    {
        private readonly string _connectionString;
        private readonly bool _dryRun;
        private readonly int _joursRetroactifs;

        public ImportFacturesAchatsService(bool dryRun = false)
        {
            _connectionString = ConfigurationManager.GetConnectionString();
            _dryRun = dryRun;

            // Chargement des paramètres depuis AppSettings
            var filters = ConfigurationManager.GetFilters();
            _joursRetroactifs = filters?.ImportFacturesAchats?.JoursRetroactifs ?? 30;
        }

        /// <summary>
        /// Lance l'import des factures d'achat depuis Sage
        /// </summary>
        public void Run()
        {
            try
            {
                Log.Information("═══════════════════════════════════════════════════");
                Log.Information("🚀 IMPORT FACTURES ACHATS : Sage → Athénéo");
                Log.Information("═══════════════════════════════════════════════════");

                // Calcul de la période d'import
                DateTime dateFin = DateTime.Now;
                DateTime dateDebut = dateFin.AddDays(-_joursRetroactifs);

                Log.Information($"📅 Période: du {dateDebut:dd/MM/yyyy} au {dateFin:dd/MM/yyyy} ({_joursRetroactifs} jours)");

                // Connexion à Sage et récupération des factures
                using (var sageConnector = new SageConnector(_dryRun))
                {
                    var facturesSage = sageConnector.ImporterFacturesAchats(dateDebut, dateFin);

                    if (facturesSage == null || facturesSage.Count == 0)
                    {
                        Log.Information("ℹ️ Aucune facture d'achat trouvée dans Sage pour cette période");
                        return;
                    }

                    Log.Information($"📥 {facturesSage.Count} factures d'achat récupérées depuis Sage");

                    // Traitement de chaque facture
                    int successCount = 0;
                    int errorCount = 0;

                    foreach (var factureSage in facturesSage)
                    {
                        try
                        {
                            if (ImporterFacture(factureSage))
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
                            Log.Error($"❌ Erreur import facture {factureSage.NumeroFacture}: {ex.Message}");
                            errorCount++;
                        }
                    }

                    // Rapport final
                    Log.Information("═══════════════════════════════════════════════════");
                    Log.Information($"✅ Import terminé : {successCount} réussies, {errorCount} erreurs");
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
        /// Importe une facture d'achat dans la base Athénéo
        /// </summary>
        private bool ImporterFacture(FactureAchatSage factureSage)
        {
            try
            {
                Log.Information($"🔄 Import facture {factureSage.NumeroFacture} - {factureSage.NomFournisseur}");

                if (_dryRun)
                {
                    Log.Information($"🧪 [DRY-RUN] Simulation import facture {factureSage.NumeroFacture}");
                    return true;
                }

                using (var connection = new SqlConnection(_connectionString))
                {
                    connection.Open();

                    // Début de transaction
                    using (var transaction = connection.BeginTransaction())
                    {
                        try
                        {
                            // 1. Import de l'en-tête de la facture
                            var parameters = new DynamicParameters();
                            parameters.Add("@CHRONO_A_FACTURE", GenerateChronoFacture(factureSage));
                            parameters.Add("@COD_ETAT", "2"); // État validé
                            parameters.Add("@DATE_FACTURE", factureSage.DateFacture);
                            parameters.Add("@MTT_HT", factureSage.MontantHT);
                            parameters.Add("@MTT_TTC", factureSage.MontantTTC);
                            parameters.Add("@NET_A_PAYER", factureSage.MontantTTC);
                            parameters.Add("@OBJET", factureSage.LibelleFacture);
                            parameters.Add("@DATE_ECHEANCE", factureSage.DateEcheance);
                            parameters.Add("@CODE_FOURNISSEUR", factureSage.CodeFournisseur);
                            parameters.Add("@NOM_FOURNISSEUR", factureSage.NomFournisseur);
                            parameters.Add("@REFERENCE_EXTERNE", factureSage.NumeroFacture);
                            parameters.Add("@REFERENCE_FOURNISSEUR", factureSage.ReferenceFournisseur);

                            var result = connection.QueryFirstOrDefault<dynamic>(
                                "SP_IMPORT_FACTURE_ACHAT",
                                parameters,
                                transaction: transaction,
                                commandType: CommandType.StoredProcedure
                            );

                            if (result == null)
                            {
                                throw new Exception("La procédure stockée n'a pas retourné de résultat");
                            }

                            int noAFacture = result.NO_A_FACTURE;
                            string operation = result.OPERATION;

                            Log.Information($"  ✓ Facture {operation} (ID: {noAFacture})");

                            // 2. Import des lignes de facture
                            int numeroLigne = 0;
                            foreach (var ligne in factureSage.Lignes)
                            {
                                numeroLigne++;

                                var ligneParams = new DynamicParameters();
                                ligneParams.Add("@NO_A_FACTURE", noAFacture);
                                ligneParams.Add("@NO_LIGNE", numeroLigne);
                                ligneParams.Add("@TYPE_LIGNE", ligne.TypeLigne ?? "Produit");
                                ligneParams.Add("@COD_PROD", ligne.CodeArticle);
                                ligneParams.Add("@DES_COM", ligne.Designation);
                                ligneParams.Add("@TAUX_TAXE", ligne.TauxTVA);
                                ligneParams.Add("@QTE_FAC", ligne.Quantite);
                                ligneParams.Add("@PUVB", ligne.PrixUnitaireHT);
                                ligneParams.Add("@PU_NET", ligne.PrixUnitaireHT);
                                ligneParams.Add("@TOTAL_HT", ligne.MontantHT);
                                ligneParams.Add("@COMPTE_COMPTABLE", ligne.CompteComptable);

                                connection.Execute(
                                    "SP_IMPORT_LIGNE_FACTURE_ACHAT",
                                    ligneParams,
                                    transaction: transaction,
                                    commandType: CommandType.StoredProcedure
                                );
                            }

                            Log.Information($"  ✓ {factureSage.Lignes.Count} lignes importées");

                            // Commit de la transaction
                            transaction.Commit();

                            Log.Information($"✅ Facture {factureSage.NumeroFacture} importée avec succès");
                            return true;
                        }
                        catch (Exception)
                        {
                            transaction.Rollback();
                            throw;
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Log.Error($"❌ Erreur import facture {factureSage.NumeroFacture}: {ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// Génère un numéro chrono unique pour la facture
        /// </summary>
        private string GenerateChronoFacture(FactureAchatSage facture)
        {
            // Format: SAGE-FA-YYYYMMDD-XXXXX
            return $"SAGE-FA-{facture.DateFacture:yyyyMMdd}-{facture.NumeroFacture}";
        }
    }
}
