using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AtheneoSageSync.Models
{
    public class FactureSage
    {
        public string TierCode { get; set; }             // Code client (Sage)
        public DateTime Date { get; set; }               // Date de facture
        public DateTime DateEcheance { get; set; }       // Date échéance
        public string Objet { get; set; }                // Libellé / objet
        public string ReferenceInterne { get; set; }     // Réf. Athénéo
        public List<LigneFactureSage> Lignes { get; set; } = new List<LigneFactureSage>{};
    }
}
