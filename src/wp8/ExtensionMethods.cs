using System;
using System.Globalization;
using System.Device.Location;
using Windows.Devices.Geolocation;
namespace Cordova.Extension.Commands
{
    public static class ExtensionMethods
    {
        public static string ToJson(this GeoCoordinate geocoordinate)
        {
            DateTime now = new DateTime();
            var numberFormatInfo = (NumberFormatInfo)NumberFormatInfo.CurrentInfo.Clone();
            numberFormatInfo.NaNSymbol = "0";
            numberFormatInfo.NumberDecimalSeparator = ".";
            return string.Format("{{ " +
                "\"coord\": {{ " +
                    "\"accuracy\": {0}," +
                    "\"latitude\": {1}," +
                    "\"longitude\": {2}," +
                    "\"altitude\": {3}," +
                    "\"heading\": {4}," +
                    "\"speed\": {5}," +
                    "\"provider_enabled\": true," +
                    "\"out_of_service\": false" +
                "}}" +
                "\"timestamp\": {6}" +
            "}}"
            , geocoordinate.HorizontalAccuracy.ToString(numberFormatInfo)
            , geocoordinate.Latitude.ToString(numberFormatInfo)
            , geocoordinate.Longitude.ToString(numberFormatInfo)
            , geocoordinate.GetType().GetMethod("Altitude") != null ? geocoordinate.Altitude.ToString(numberFormatInfo) : "0"
            , geocoordinate.GetType().GetMethod("Course") != null ? geocoordinate.Course.ToString(numberFormatInfo) : "0"
            , geocoordinate.GetType().GetMethod("Speed") != null ? geocoordinate.Speed.ToString(numberFormatInfo) : "0"
            , -now.ToJavaScriptMilliseconds());
        }
        public static long ToJavaScriptMilliseconds(this DateTime dt)
        {
            return (long)dt
            .ToUniversalTime()
            .Subtract(new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc))
            .TotalMilliseconds;
        }
    }
}