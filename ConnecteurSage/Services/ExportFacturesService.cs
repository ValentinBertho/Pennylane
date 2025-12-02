using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using Serilog;

namespace AtheneoSageSync.Services
{
    public class ExportFacturesService
    {
        public void Run(bool dryRun = false)
        {
            Log.Information("🚀 Export des factures Athénéo vers Sage...");

            var factures = new AtheneoReader().GetFacturesNonExportees();
            var connector = new SageConnector(dryRun);

            foreach (var facture in factures)
            {
                var factureSage = FactureMapper.MapToSage(facture);
                bool success = connector.ExporterFacture(factureSage);
                Logger.LogFacture(facture, success);
            }


            Log.Information("✅ Export terminé.");
        }
    }
}
