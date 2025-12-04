using System;
using System.Collections.Generic;

namespace AtheneoSageSync.Models
{
    /// <summary>
    /// Représente une facture d'achat provenant de Sage
    /// </summary>
    public class FactureAchatSage
    {
        public string NumeroFacture { get; set; }
        public string ReferenceFournisseur { get; set; }
        public string CodeFournisseur { get; set; }
        public string NomFournisseur { get; set; }
        public DateTime DateFacture { get; set; }
        public DateTime DateEcheance { get; set; }
        public string LibelleFacture { get; set; }
        public decimal MontantHT { get; set; }
        public decimal MontantTVA { get; set; }
        public decimal MontantTTC { get; set; }
        public string CodeDevise { get; set; }
        public string StatutFacture { get; set; }
        public string ReferenceInterne { get; set; }

        // Lignes de la facture
        public List<LigneFactureAchatSage> Lignes { get; set; } = new List<LigneFactureAchatSage>();
    }
}
