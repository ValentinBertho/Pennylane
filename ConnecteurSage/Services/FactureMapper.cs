using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using AtheneoSageSync.Models;

namespace AtheneoSageSync.Services
{
    public static class FactureMapper
    {
        public static FactureSage MapToSage(FactureAtheneo source)
        {
            var target = new FactureSage
            {
                TierCode = GetTierCodeFromFacture(source), // TODO: mapping réel
                Date = source.DateFacture,
                DateEcheance = source.DateEcheance,
                Objet = source.Objet ?? "Facture Athénéo",
                ReferenceInterne = source.NoVFacture.ToString()
            };

            foreach (var ligne in source.Lignes)
            {
                target.Lignes.Add(new LigneFactureSage
                {
                    CodeArticle = ligne.CodProd ?? "ARTICLE_INCONNU",
                    Description = ligne.DesCom ?? "(Sans description)",
                    Quantite = ligne.QteFac,
                    PrixUnitaire = ligne.PuNet,
                    TauxTva = ligne.TauxTaxe,
                    TypeLigne = ligne.TypeLigne ?? "Produit"
                });
            }

            return target;
        }

        private static string GetTierCodeFromFacture(FactureAtheneo facture)
        {
            // 🛑 À adapter selon ta structure réelle (client lié à la facture)
            // Exemple temporaire :
            return "CLIENT_ATHENEO"; // stub
        }
    }
}
