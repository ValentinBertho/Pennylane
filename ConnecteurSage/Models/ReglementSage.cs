using System;

namespace AtheneoSageSync.Models
{
    /// <summary>
    /// Représente un règlement provenant de Sage
    /// </summary>
    public class ReglementSage
    {
        public string NumeroReglement { get; set; }
        public string NumeroFacture { get; set; }
        public string CodeTiers { get; set; }
        public string NomTiers { get; set; }
        public DateTime DateReglement { get; set; }
        public decimal MontantReglement { get; set; }
        public string ModeReglement { get; set; }
        public string ReferenceReglement { get; set; }
        public string CompteComptable { get; set; }
        public string Journal { get; set; }
        public string NumeroEcriture { get; set; }
        public string TypeReglement { get; set; } // Client ou Fournisseur
        public string Statut { get; set; }
    }
}
