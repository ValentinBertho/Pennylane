using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading.Tasks;

using AtheneoSageSync.Models;

using Serilog;

namespace AtheneoSageSync.Services
{
    public class SageConnector
    {
        private dynamic _session;
        private readonly bool _dryRun;

        public SageConnector(bool dryRun = false)
        {
            _dryRun = dryRun;
            OpenSageSession();
        }

        private void OpenSageSession()
        {
            try
            {
                Type t = Type.GetTypeFromProgID("Sage100c.SDO.Application"); // Adapter au ProgID réel
                _session = Activator.CreateInstance(t);

                _session.Connect(
                    "NomSociete",    // à adapter
                    "Utilisateur",   // à adapter
                    "MotDePasse"     // à adapter
                );

                Log.Information("✅ Connexion à Sage réussie.");
            }
            catch (COMException ex)
            {
                Log.Error("❌ Erreur connexion Sage : " + ex.Message);
                throw;
            }
        }

        public bool ExporterFacture(FactureSage facture)
        {
            try
            {
                Log.Information($"🔄 Traitement facture {facture.ReferenceInterne} (dry-run: {_dryRun})");

                var doc = _session.CreateDocumentVente();
                doc.Type = "FV";
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

                if (_dryRun)
                {
                    Log.Information($"🧪 Simulation terminée pour {facture.ReferenceInterne}");
                    return true;
                }

                doc.Valider();

                Log.Information($"✅ Facture {facture.ReferenceInterne} exportée.");
                return true;
            }
            catch (COMException ex)
            {
                Log.Error($"❌ Erreur export {facture.ReferenceInterne} : {ex.Message}");
                return false;
            }
        }
    }
}
