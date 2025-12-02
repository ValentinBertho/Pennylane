using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AtheneoSageSync.Models
{
    public class FactureAtheneo
    {
        // Facture principale
        public string NoVFacture { get; set; }        // Numéro de la facture
        public string ChronoVFacture { get; set; }    // Référence unique
        public string CodEtat { get; set; }           // État de la facture
        public DateTime DateFacture { get; set; }     // Date de la facture
        public double MttHt { get; set; }             // Montant HT
        public double MttTtc { get; set; }            // Montant TTC
        public double NetAPayer { get; set; }         // Net à payer
        public string Objet { get; set; }             // Objet de la facture
        public DateTime DateEcheance { get; set; }    // Date d'échéance
        public string DocumentId { get; set; }
        public string ModuleId { get; set; }
        public string StatutPdp { get; set; }
        public string EtatEnvoi { get; set; }
        public string EtatRejet { get; set; }
        public string DateSync { get; set; }          // Format string si non parsé
        public string Comment { get; set; }
        public string SyncStatus { get; set; }
        public string SyncComment { get; set; }

        // Lignes associées
        public List<LigneFactureAtheneo> Lignes { get; set; } = new List<LigneFactureAtheneo>();
    }
}
