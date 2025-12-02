using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using Serilog;

namespace AtheneoSageSync.Services
{
    public class ImportFacturesAchatsService
    {
        public void Run()
        {
            Log.Information("🚀 Import des factures d'achats Sage vers Athénéo...");

            // TODO : Implémenter la lecture Sage et l'écriture BDD Athénéo

            Log.Information("✅ Import terminé.");
        }
    }
}
