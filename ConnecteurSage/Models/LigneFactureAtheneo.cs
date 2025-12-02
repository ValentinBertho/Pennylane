using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AtheneoSageSync.Models
{
    public class LigneFactureAtheneo
    {
        public string NoVFacture { get; set; }      // Numéro de la facture
        public string NoVLFacture { get; set; }     // Numéro de la ligne
        public int NoLigne { get; set; }            // Numéro de ligne
        public string TypeLigne { get; set; }       // Produit / Service
        public int NoProduit { get; set; }          // ID produit
        public string CodProd { get; set; }         // Code produit/service
        public string DesCom { get; set; }          // Description
        public double TauxTaxe { get; set; }        // Taux TVA
        public double QteFac { get; set; }          // Quantité
        public double Puvb { get; set; }            // Prix unitaire brut
        public double PuNet { get; set; }           // Prix unitaire net
        public double TotalNet { get; set; }        // Total net
        public double TotalHT { get; set; }         // Total HT
    }
}
