using System;

namespace AtheneoSageSync.Models
{
    /// <summary>
    /// Représente une ligne de facture d'achat provenant de Sage
    /// </summary>
    public class LigneFactureAchatSage
    {
        public int NumeroLigne { get; set; }
        public string TypeLigne { get; set; }
        public string CodeArticle { get; set; }
        public string Designation { get; set; }
        public decimal Quantite { get; set; }
        public decimal PrixUnitaireHT { get; set; }
        public decimal MontantHT { get; set; }
        public decimal TauxTVA { get; set; }
        public decimal MontantTVA { get; set; }
        public decimal MontantTTC { get; set; }
        public string CompteComptable { get; set; }
        public string CentreAnalytique { get; set; }
    }
}
