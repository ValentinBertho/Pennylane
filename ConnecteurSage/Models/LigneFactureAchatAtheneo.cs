namespace AtheneoSageSync.Models
{
    /// <summary>
    /// Représente une ligne de facture d'achat pour Athénéo
    /// </summary>
    public class LigneFactureAchatAtheneo
    {
        public int NoLigne { get; set; }
        public string TypeLigne { get; set; }
        public string CodProd { get; set; }
        public string DesCom { get; set; }
        public decimal TauxTaxe { get; set; }
        public decimal QteFac { get; set; }
        public decimal Puvb { get; set; }
        public decimal PuNet { get; set; }
        public decimal TotalHT { get; set; }
        public string CompteComptable { get; set; }
    }
}
