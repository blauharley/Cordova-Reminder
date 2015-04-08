using System;
using System.Globalization;
using System.Device.Location;
using Windows.Devices.Geolocation;
namespace Cordova.Extension.Commands
{
    public static class ExtensionMethods
    {
        public static string ToJson(this Cordova.Extension.Commands.Reminder.ReminderGeoCoordinate geocoordinate)
        {
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
                    "\"provider_enabled\": {6}," +
                    "\"out_of_service\": {7}" +
                "}}" +
                "\"timestamp\": {8}" +
            "}}"
            , geocoordinate.HorizontalAccuracy.ToString(numberFormatInfo)
            , geocoordinate.Latitude.ToString(numberFormatInfo)
            , geocoordinate.Longitude.ToString(numberFormatInfo)
            , geocoordinate.GetType().GetMethod("Altitude") != null ? geocoordinate.Altitude.ToString(numberFormatInfo) : "0"
            , geocoordinate.GetType().GetMethod("Course") != null ? geocoordinate.Course.ToString(numberFormatInfo) : "0"
            , geocoordinate.GetType().GetMethod("Speed") != null ? geocoordinate.Speed.ToString(numberFormatInfo) : "0"
            , geocoordinate.providerEnabled
            , geocoordinate.providerStatus == PositionStatus.Disabled || geocoordinate.providerStatus == PositionStatus.NotAvailable
            , ((DateTime.Now.Ticks - new DateTime(1970, 1, 1).Ticks) / TimeSpan.TicksPerSecond)*1000);
        }

    }
}