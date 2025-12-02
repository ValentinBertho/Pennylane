using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using Serilog;

namespace AtheneoSageSync.Services
{
    public class ImportReglementsService
    {
        public void Run()
        {
            Log.Information("🚀 Import des règlements Sage vers Athénéo...");

            // TODO : Implémenter la lecture Sage et l'écriture BDD Athénéo

            Log.Information("✅ Import terminé.");
        }
    }
}
