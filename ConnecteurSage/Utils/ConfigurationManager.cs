using System.IO;
using Newtonsoft.Json.Linq;

namespace AtheneoSageSync.Utils
{
    public static class ConfigurationManager
    {
        public static string GetConnectionString()
        {
            var json = JObject.Parse(File.ReadAllText("Config/AppSettings.json"));
            return json["ConnectionStrings"]["AtheneoDb"].ToString();
        }
    }
}
