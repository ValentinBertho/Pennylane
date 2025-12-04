using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

using AtheneoSageSync.Models;
using AtheneoSageSync.Utils;

using Serilog;

namespace AtheneoSageSync.Services
{
    /// <summary>
    /// Connecteur COM Interop pour l'API Sage
    /// Gère l'export de factures de vente et l'import de factures d'achat et règlements
    /// </summary>
    public class SageConnector : IDisposable
    {
        private dynamic _session;
        private readonly bool _dryRun;
        private readonly string _progId;
        private readonly string _companyName;
        private readonly string _username;
        private readonly string _password;

        public SageConnector(bool dryRun = false)
        {
            _dryRun = dryRun;

            // Chargement de la configuration depuis AppSettings.json
            var config = ConfigurationManager.GetAppSettings();
            _progId = config.Sage?.ProgID ?? "Sage100c.SDO.Application";
            _companyName = config.Sage?.CompanyName ?? "NomSociete";
            _username = config.Sage?.Username ?? "Utilisateur";
            _password = config.Sage?.Password ?? "MotDePasse";

            if (!_dryRun)
            {
                OpenSageSession();
            }
            else
            {
                Log.Information("🧪 Mode DRY-RUN activé - Pas de connexion Sage réelle");
            }
        }

        private void OpenSageSession()
        {
            try
            {
                Log.Information($"🔌 Connexion à Sage via {_progId}...");

                Type t = Type.GetTypeFromProgID(_progId);
                if (t == null)
                {
                    throw new InvalidOperationException($"ProgID '{_progId}' non trouvé. Vérifiez l'installation de Sage.");
                }

                _session = Activator.CreateInstance(t);
                _session.Connect(_companyName, _username, _password);

                Log.Information($"✅ Connexion à Sage réussie (Société: {_companyName})");
            }
            catch (COMException ex)
            {
                Log.Error($"❌ Erreur connexion Sage (HRESULT: 0x{ex.ErrorCode:X}): {ex.Message}");
                throw;
            }
            catch (Exception ex)
            {
                Log.Error($"❌ Erreur connexion Sage : {ex.Message}");
                throw;
            }
        }

        #region Export Factures Vente vers Sage

        /// <summary>
        /// Exporte une facture de vente Athénéo vers Sage
        /// </summary>
        public bool ExporterFacture(FactureSage facture)
        {
            try
            {
                Log.Information($"🔄 Export facture vente {facture.ReferenceInterne} vers Sage");

                if (_dryRun)
                {
                    Log.Information($"🧪 [DRY-RUN] Facture {facture.ReferenceInterne} - {facture.TierCode} - {facture.Lignes.Count} lignes");
                    return true;
                }

                var doc = _session.CreateDocumentVente();
                doc.Type = "FV"; // Facture de Vente
                doc.Tier = facture.TierCode;
                doc.Date = facture.Date;
                doc.DateEcheance = facture.DateEcheance;
                doc.Objet = facture.Objet;
                doc.Reference = facture.ReferenceInterne;

                foreach (var ligne in facture.Lignes)
                {
                    var ligneDoc = doc.Lignes.Add();
                    ligneDoc.CodeArticle = ligne.CodeArticle;
                    ligneDoc.Description = ligne.Description;
                    ligneDoc.Quantite = ligne.Quantite;
                    ligneDoc.PrixUnitaire = ligne.PrixUnitaire;
                    ligneDoc.TauxTVA = ligne.TauxTva;
                    ligneDoc.TypeLigne = ligne.TypeLigne;
                }

                doc.Valider();
                Log.Information($"✅ Facture {facture.ReferenceInterne} exportée vers Sage avec succès");
                return true;
            }
            catch (COMException ex)
            {
                Log.Error($"❌ Erreur COM export facture {facture.ReferenceInterne} (HRESULT: 0x{ex.ErrorCode:X}): {ex.Message}");
                return false;
            }
            catch (Exception ex)
            {
                Log.Error($"❌ Erreur export facture {facture.ReferenceInterne}: {ex.Message}");
                return false;
            }
        }

        #endregion

        #region Import Factures Achat depuis Sage

        /// <summary>
        /// Récupère les factures d'achat depuis Sage pour une période donnée
        /// </summary>
        public List<FactureAchatSage> ImporterFacturesAchats(DateTime dateDebut, DateTime dateFin)
        {
            var factures = new List<FactureAchatSage>();

            try
            {
                Log.Information($"📥 Import des factures d'achat Sage du {dateDebut:dd/MM/yyyy} au {dateFin:dd/MM/yyyy}");

                if (_dryRun)
                {
                    Log.Information("🧪 [DRY-RUN] Simulation import - retour de données factices");
                    return GenererFacturesAchatsFictives(dateDebut, dateFin);
                }

                // Accès au module de gestion des achats
                var achatsModule = _session.GetModule("Achats");
                var facturesAchats = achatsModule.GetDocuments("FA"); // FA = Facture Achat

                foreach (dynamic doc in facturesAchats)
                {
                    // Filtre par date
                    DateTime dateDoc = doc.Date;
                    if (dateDoc < dateDebut || dateDoc > dateFin)
                        continue;

                    var facture = new FactureAchatSage
                    {
                        NumeroFacture = doc.Numero,
                        ReferenceFournisseur = doc.ReferenceFournisseur ?? "",
                        CodeFournisseur = doc.CodeTiers,
                        NomFournisseur = doc.NomTiers,
                        DateFacture = dateDoc,
                        DateEcheance = doc.DateEcheance,
                        LibelleFacture = doc.Libelle ?? "",
                        MontantHT = (decimal)doc.MontantHT,
                        MontantTVA = (decimal)doc.MontantTVA,
                        MontantTTC = (decimal)doc.MontantTTC,
                        CodeDevise = doc.Devise ?? "EUR",
                        StatutFacture = doc.Statut,
                        ReferenceInterne = doc.ReferenceInterne ?? ""
                    };

                    // Récupération des lignes
                    foreach (dynamic ligne in doc.Lignes)
                    {
                        facture.Lignes.Add(new LigneFactureAchatSage
                        {
                            NumeroLigne = ligne.NumeroLigne,
                            TypeLigne = ligne.Type,
                            CodeArticle = ligne.CodeArticle ?? "",
                            Designation = ligne.Designation ?? "",
                            Quantite = (decimal)ligne.Quantite,
                            PrixUnitaireHT = (decimal)ligne.PrixUnitaire,
                            MontantHT = (decimal)ligne.MontantHT,
                            TauxTVA = (decimal)ligne.TauxTVA,
                            MontantTVA = (decimal)ligne.MontantTVA,
                            MontantTTC = (decimal)ligne.MontantTTC,
                            CompteComptable = ligne.CompteComptable ?? "",
                            CentreAnalytique = ligne.CentreAnalytique ?? ""
                        });
                    }

                    factures.Add(facture);
                }

                Log.Information($"✅ {factures.Count} factures d'achat importées depuis Sage");
            }
            catch (COMException ex)
            {
                Log.Error($"❌ Erreur COM import factures achats (HRESULT: 0x{ex.ErrorCode:X}): {ex.Message}");
            }
            catch (Exception ex)
            {
                Log.Error($"❌ Erreur import factures achats: {ex.Message}");
            }

            return factures;
        }

        #endregion

        #region Import Règlements depuis Sage

        /// <summary>
        /// Récupère les règlements depuis Sage pour une période donnée
        /// </summary>
        public List<ReglementSage> ImporterReglements(DateTime dateDebut, DateTime dateFin)
        {
            var reglements = new List<ReglementSage>();

            try
            {
                Log.Information($"📥 Import des règlements Sage du {dateDebut:dd/MM/yyyy} au {dateFin:dd/MM/yyyy}");

                if (_dryRun)
                {
                    Log.Information("🧪 [DRY-RUN] Simulation import - retour de données factices");
                    return GenererReglementsFictifs(dateDebut, dateFin);
                }

                // Accès au module comptabilité / trésorerie
                var comptaModule = _session.GetModule("Comptabilite");
                var ecritures = comptaModule.GetEcritures();

                foreach (dynamic ecriture in ecritures)
                {
                    // Filtre par date et par type (règlements uniquement)
                    DateTime dateEcriture = ecriture.Date;
                    if (dateEcriture < dateDebut || dateEcriture > dateFin)
                        continue;

                    if (ecriture.TypeEcriture != "REGLEMENT")
                        continue;

                    var reglement = new ReglementSage
                    {
                        NumeroReglement = ecriture.NumeroEcriture,
                        NumeroFacture = ecriture.NumeroDocument ?? "",
                        CodeTiers = ecriture.CodeTiers ?? "",
                        NomTiers = ecriture.NomTiers ?? "",
                        DateReglement = dateEcriture,
                        MontantReglement = (decimal)ecriture.Montant,
                        ModeReglement = ecriture.ModeReglement ?? "",
                        ReferenceReglement = ecriture.Reference ?? "",
                        CompteComptable = ecriture.CompteComptable ?? "",
                        Journal = ecriture.Journal ?? "",
                        NumeroEcriture = ecriture.NumeroEcriture,
                        TypeReglement = ecriture.Sens == "D" ? "Client" : "Fournisseur",
                        Statut = ecriture.Statut ?? "Validé"
                    };

                    reglements.Add(reglement);
                }

                Log.Information($"✅ {reglements.Count} règlements importés depuis Sage");
            }
            catch (COMException ex)
            {
                Log.Error($"❌ Erreur COM import règlements (HRESULT: 0x{ex.ErrorCode:X}): {ex.Message}");
            }
            catch (Exception ex)
            {
                Log.Error($"❌ Erreur import règlements: {ex.Message}");
            }

            return reglements;
        }

        #endregion

        #region Méthodes Utilitaires

        /// <summary>
        /// Génère des factures d'achat fictives pour le mode DRY-RUN
        /// </summary>
        private List<FactureAchatSage> GenererFacturesAchatsFictives(DateTime dateDebut, DateTime dateFin)
        {
            return new List<FactureAchatSage>
            {
                new FactureAchatSage
                {
                    NumeroFacture = "FA-2024-001",
                    ReferenceFournisseur = "FACT-FICT-001",
                    CodeFournisseur = "FOURN001",
                    NomFournisseur = "Fournisseur Fictif 1",
                    DateFacture = dateDebut.AddDays(1),
                    DateEcheance = dateDebut.AddDays(31),
                    LibelleFacture = "Facture fictive pour test",
                    MontantHT = 1000.00m,
                    MontantTVA = 200.00m,
                    MontantTTC = 1200.00m,
                    CodeDevise = "EUR",
                    StatutFacture = "Validé",
                    Lignes = new List<LigneFactureAchatSage>
                    {
                        new LigneFactureAchatSage
                        {
                            NumeroLigne = 1,
                            TypeLigne = "Produit",
                            CodeArticle = "ART001",
                            Designation = "Article fictif 1",
                            Quantite = 10,
                            PrixUnitaireHT = 100.00m,
                            MontantHT = 1000.00m,
                            TauxTVA = 20.00m,
                            MontantTVA = 200.00m,
                            MontantTTC = 1200.00m
                        }
                    }
                }
            };
        }

        /// <summary>
        /// Génère des règlements fictifs pour le mode DRY-RUN
        /// </summary>
        private List<ReglementSage> GenererReglementsFictifs(DateTime dateDebut, DateTime dateFin)
        {
            return new List<ReglementSage>
            {
                new ReglementSage
                {
                    NumeroReglement = "REG-2024-001",
                    NumeroFacture = "FA-2024-001",
                    CodeTiers = "FOURN001",
                    NomTiers = "Fournisseur Fictif 1",
                    DateReglement = dateDebut.AddDays(2),
                    MontantReglement = 1200.00m,
                    ModeReglement = "Virement",
                    ReferenceReglement = "VIR-001",
                    CompteComptable = "512000",
                    Journal = "BQ",
                    TypeReglement = "Fournisseur",
                    Statut = "Validé"
                }
            };
        }

        #endregion

        #region IDisposable

        /// <summary>
        /// Ferme la connexion Sage proprement
        /// </summary>
        public void Dispose()
        {
            try
            {
                if (_session != null && !_dryRun)
                {
                    _session.Disconnect();
                    Marshal.ReleaseComObject(_session);
                    _session = null;
                    Log.Information("🔌 Déconnexion Sage effectuée");
                }
            }
            catch (Exception ex)
            {
                Log.Warning($"⚠️ Erreur lors de la déconnexion Sage: {ex.Message}");
            }
        }

        #endregion
    }
}
