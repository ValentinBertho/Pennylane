using System;
using System.Collections.Generic;

namespace AtheneoSageSync.Models
{
    /// <summary>
    /// Représente une facture d'achat pour insertion dans Athénéo
    /// </summary>
    public class FactureAchatAtheneo
    {
        public string ChronoAFacture { get; set; }
        public string CodEtat { get; set; }
        public DateTime DateFacture { get; set; }
        public decimal MttHt { get; set; }
        public decimal MttTtc { get; set; }
        public decimal NetAPayer { get; set; }
        public string Objet { get; set; }
        public DateTime DateEcheance { get; set; }
        public string CodeFournisseur { get; set; }
        public string NomFournisseur { get; set; }
        public string ReferenceExterne { get; set; }
        public string ReferenceFournisseur { get; set; }

        // Lignes associées
        public List<LigneFactureAchatAtheneo> Lignes { get; set; } = new List<LigneFactureAchatAtheneo>();
    }
}
