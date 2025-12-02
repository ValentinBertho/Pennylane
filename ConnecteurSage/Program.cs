using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

using AtheneoSageSync.Enums;
using AtheneoSageSync.Services;

using Serilog;

namespace AtheneoSageSync
{
    internal class Program
    {
        static void Main(string[] args)
        {
            Log.Logger = new LoggerConfiguration()
                .MinimumLevel.Debug()
                .WriteTo.Console()
                .WriteTo.File("logs/export-sage.log", rollingInterval: RollingInterval.Day)
                .CreateLogger();

            Log.Information("▶️ Démarrage de l'application");
            Log.Information("🔄 Lancement Atheneo ↔ Sage Sync");

            ModeExecution mode = ModeExecution.None;

            if (args.Length > 0)
            {
                Enum.TryParse(args[0], true, out mode);
                bool dryRun = args.Contains("--dry-run");
            }

            switch (mode)
            {
                case ModeExecution.ExportFactures:
                    new ExportFacturesService().Run();
                    break;
                case ModeExecution.ImportReglements:
                    new ImportReglementsService().Run();
                    break;
                case ModeExecution.ImportFacturesAchat:
                    new ImportFacturesAchatsService().Run();
                    break;
                default:
                    Log.Warning("❌ Mode non reconnu. Utilisez : ExportFactures | ImportReglements | ImportFacturesAchat");
                    break;
            }
        }
    }
}
