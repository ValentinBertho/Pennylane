using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AtheneoSageSync.Models
{
    public class LigneFactureSage
    {
        public string CodeArticle { get; set; }
        public string Description { get; set; }
        public double Quantite { get; set; }
        public double PrixUnitaire { get; set; }
        public double TauxTva { get; set; }
        public string TypeLigne { get; set; } // Produit / Service
    }
}
