using Microsoft.Phone.Shell;
using System;
using System.Device.Location;
using System.Globalization;
using System.Runtime.Serialization;
using System.Windows;
using Windows.Devices.Geolocation;
using WPCordovaClassLib.Cordova;
using WPCordovaClassLib.Cordova.Commands;
using WPCordovaClassLib.Cordova.JSON;

namespace Cordova.Extension.Commands
{
    public class Reminder : BaseCommand
    {

        private static string AIM_MODE = "aim";
        private static string TRACK_MODE = "track";
        private static string STATUS_MODE = "status";
        private static string STOP_SERVICE_DATE_FOREVER = "forever";
        private static string STOP_SERVICE_DATE_TOMORROW = "tomorrow";


        public string mode;

        public string title;

        public string content;

        public double distance;

        public UInt32 interval;

        public bool whistle;

        public string stopDate;

        public double distanceTolerance;

        public double aimCoordLat;

        public double aimCoordLong;

        public bool aggressive;



        /*
         * request vars
         */
        private UInt32 locationRequestTimeout = 60 * 1000;
        private bool isProviderRequest = false;
        private long requestStartTime;
        private Int32 requestwarmUpTime = 5000;

        /*
         * mode vars
         */
        private Int32 warmUpTime = 5000;
        private long startTime;
        private double radiusDistance;
        private double linearDistance;
        private long currentMsTime;
        private Int32 stopServiceDate = -1;
        
        private bool goToHold = false;

        private GeoCoordinate startLoc = null;
        private GeoCoordinate lastLoc = null;
        private GeoCoordinate currLoc = null;
        private GeoCoordinate aimLoc = null;

        private string ConfigureCallbackToken { get; set; }

        public static Geolocator Geolocator { get; set; }

        public static bool RunningInBackground { get; set; }

        private static Version TargetVersion = new Version(8, 0, 10492);

        public static bool IsTargetedVersion { get { return Environment.OSVersion.Version >= TargetVersion; } }

        public class ReminderGeoCoordinate : GeoCoordinate
        {

            public bool providerEnabled { get; set; }
            public bool isGPSAvailable { get; set; }
            public PositionStatus providerStatus { get; set; }

            public ReminderGeoCoordinate()
            {
                this.providerEnabled = true;
            }

            public ReminderGeoCoordinate(GeoCoordinate obj)
            {
                this.HorizontalAccuracy = obj.HorizontalAccuracy;
                this.Altitude = obj.Altitude;
                this.Course = obj.Course;
                this.Latitude = obj.Latitude;
                this.Longitude = obj.Longitude;
                this.Speed = obj.Speed;
                this.providerEnabled = false;
                this.isGPSAvailable = false;
            }

            public void copyValues(GeoCoordinate obj)
            {
                this.HorizontalAccuracy = obj.HorizontalAccuracy;
                this.Altitude = obj.Altitude;
                this.Course = obj.Course;
                this.Latitude = obj.Latitude;
                this.Longitude = obj.Longitude;
                this.Speed = obj.Speed;
            }

        }

        private ReminderGeoCoordinate requestedCoordInfo;


        public Reminder()
        {
            
        }

        public void start(string args)
        {
            try
            {

                // [title,content,interval,distance,whistle,closeApp,stopDate,distanceTolerance,mode,aimCoordLat,aimCoordLong,aggressive]
                string[] opts = JsonHelper.Deserialize<string[]>(args);
                mode = opts[8];
                title = opts[0];
                content = opts[1];
                double.TryParse(opts[3], out distance);
                uint.TryParse(opts[2], out interval);
                bool.TryParse(opts[4], out whistle);
                stopDate = opts[6];
                double.TryParse(opts[7], out distanceTolerance);
                double.TryParse(opts[9], System.Globalization.NumberStyles.Any, CultureInfo.InvariantCulture, out aimCoordLat);
                double.TryParse(opts[10], System.Globalization.NumberStyles.Any, CultureInfo.InvariantCulture, out aimCoordLong);
                bool.TryParse(opts[11], out aggressive);

                DateTime now = new DateTime();

                if (stopDate.Equals(STOP_SERVICE_DATE_TOMORROW, StringComparison.InvariantCultureIgnoreCase))
                {
                    stopServiceDate = now.Day;
                }

                if (mode.Equals(AIM_MODE, StringComparison.InvariantCultureIgnoreCase))
                {
                    aimLoc = new GeoCoordinate(aimCoordLat, aimCoordLong);
                }

                if (!aggressive)
                {
                    warmUpTime = 0;
                }

                radiusDistance = 0;
                linearDistance = 0;
                currentMsTime = (new DateTime()).Millisecond;

                isProviderRequest = false;

                ConfigureCallbackToken = CurrentCommandCallbackId;

                stopGeolocatorIfActive();

                startTime = now.Millisecond;

                startGeolocator();

            }
            catch (Exception e)
            {
                dispatchMessage(PluginResult.Status.ERROR, string.Format("ACTION-Start-Error: {0}!", e.ToString()), true, ConfigureCallbackToken);
            }

        }

        public void clear(string nothing)
        {
            stopGeolocatorIfActive();
        }

        public void isrunning(string nothing)
        {
            
            ConfigureCallbackToken = CurrentCommandCallbackId;
            bool isRunning = Geolocator != null;
            
            dispatchMessage(PluginResult.Status.OK, isRunning.toRunningJson(), false, ConfigureCallbackToken);

        }

        public void request(string nothing)
        {

            ConfigureCallbackToken = CurrentCommandCallbackId;
            
            requestedCoordInfo = new ReminderGeoCoordinate();
            isProviderRequest = true;

            DateTime now = new DateTime();
            requestStartTime = now.Millisecond;

            if (Geolocator == null)
            {
                startGeolocator();
            }
            else
            {
                updateCoordinates();
            }

        }

        /*
         *  private members
         * 
         */

        private void startGeolocator()
        {
            Geolocator = new Geolocator
            {
                MovementThreshold = 0,
                ReportInterval = aggressive ? 0 : interval,
                DesiredAccuracyInMeters = 100,
            };

            Geolocator.PositionChanged += OnGeolocatorOnPositionChanged;
            Geolocator.StatusChanged += OnGeolocatorStatusChanged;

            updateCoordinates();

            RunningInBackground = true;
        }

        private void stopGeolocatorIfActive()
        {
            if (Geolocator == null) return;
            Geolocator.PositionChanged -= OnGeolocatorOnPositionChanged;
            Geolocator.StatusChanged -= OnGeolocatorStatusChanged;
            Geolocator = null;
        }

        private void OnGeolocatorOnPositionChanged(Geolocator sender, PositionChangedEventArgs configureCallbackTokenargs)
        {
            if (Geolocator.LocationStatus == PositionStatus.Disabled || Geolocator.LocationStatus == PositionStatus.NotAvailable)
            {
                dispatchMessage(PluginResult.Status.ERROR, string.Format("OnGeolocatorOnPositionChanged-Error: {0}!", Geolocator.LocationStatus), true, ConfigureCallbackToken);
                return;
            }

            if (handleServiceStop())
            {
                stopGeolocatorIfActive();
                return;
            }

            Geocoordinate currLoc = configureCallbackTokenargs.Position.Coordinate;
            double _lat = currLoc.Latitude;
            double _long = currLoc.Longitude;

            GeoCoordinate location = new GeoCoordinate(_lat, _long);

            if (!warmUpTimeOut() || warmUpTime == 0)
            {
                startLoc = location;
                lastLoc = location;

                if (warmUpTime == 0)
                {
                    warmUpTime = -1;
                }
                else
                {
                    return;
                }
                
            }

            if (mode.Equals(AIM_MODE, StringComparison.InvariantCultureIgnoreCase))
            {
                handleAimModeByLocation(location);
            }
            else if (mode.Equals(TRACK_MODE, StringComparison.InvariantCultureIgnoreCase))
            {
                handleTrackModeByLocation(location);
            }
            else if (mode.Equals(STATUS_MODE, StringComparison.InvariantCultureIgnoreCase))
            {
                handleStatusModeByLocation(location);
            }

        }

        private void OnGeolocatorStatusChanged(Geolocator sender, StatusChangedEventArgs args)
        {
            switch (args.Status)
            {
                case PositionStatus.Disabled:
                    // the application does not have the right capability or the location master switch is off
                    requestedCoordInfo.providerEnabled = false;
                    requestedCoordInfo.isGPSAvailable = false;
                    break;
                case PositionStatus.Initializing:
                    // the geolocator started the tracking operation
                    requestedCoordInfo.providerEnabled = true;
                    requestedCoordInfo.isGPSAvailable = true;
                    break;
                case PositionStatus.NoData:
                    // the location service was not able to acquire the location
                    requestedCoordInfo.isGPSAvailable = false;
                    break;
                case PositionStatus.Ready:
                    // the location service is generating geopositions as specified by the tracking parameters
                    requestedCoordInfo.providerEnabled = true;
                    requestedCoordInfo.isGPSAvailable = true;
                    break;
                case PositionStatus.NotAvailable:
                    // not used in WindowsPhone, Windows desktop uses this value to signal that there is no hardware capable to acquire location information
                    requestedCoordInfo.isGPSAvailable = false;
                    break;
                case PositionStatus.NotInitialized:
                    // the initial state of the geolocator, once the tracking operation is stopped by the user the geolocator moves back to this state
                    break;
            }

            requestedCoordInfo.providerStatus = args.Status;

        }

        private void handleAimModeByLocation(GeoCoordinate location)
        {

            double distanceToAim = location.GetDistanceTo(aimLoc);

            /*
            * show notification when user has entered aim area
            */
            if (distanceToAim < distanceTolerance && (!aggressive || (aggressive && timeOut())))
            {
                startLoc = location;
                showNotification();
                linearDistance = 0;
                currentMsTime = (new DateTime()).Millisecond;
            }

        }

        private void handleTrackModeByLocation(GeoCoordinate location)
        {

            double distanceStep = lastLoc.GetDistanceTo(location);

            if (distanceStep < distanceTolerance)
            {
                return;
            }

            updateStatisticsByStepAndLocation(ref distanceStep, ref location);

            /*
            * show notification when time and distance is reached
            */
            if (linearDistance >= distance && (!aggressive || (aggressive && timeOut())))
            {
                startLoc = location;
                showNotification();
                linearDistance = 0;
                currentMsTime = (new DateTime()).Millisecond;
            }

        }

        private void handleStatusModeByLocation(GeoCoordinate location)
        {

            bool isStanding = goToHold;

            double distanceStep = lastLoc.GetDistanceTo(location);

            if (distanceStep < distance)
            {
                goToHold = true;
            }
            else
            {
                goToHold = false;
            }

            updateStatisticsByStepAndLocation(ref distanceStep, ref location);

            if (isStanding != goToHold && (!aggressive || (aggressive && timeOut())))
            {
                startLoc = location;

                showNotification();
                linearDistance = 0;

                currentMsTime = (new DateTime()).Millisecond;

            }

        }

        private bool handleServiceStop()
        {
            DateTime now = new DateTime();
            int currDay = now.Day;

            return stopServiceDate != -1 && stopServiceDate != currDay;
        }

        private void showNotification()
        {
            var toast = new ShellToast
            {
                Title = title,
                Content = content.Replace("#ML", linearDistance.ToString()).Replace("#MR", radiusDistance.ToString())
            };

            // does phone support reflection
            if (IsTargetedVersion)
            {
                if (whistle)
                {
                    string audioPath = BaseCommand.GetBaseURL() + "Plugins/com.phonegap.reminder/whistle.wav";
                    SetProperty(toast, "Sound", new Uri(audioPath, UriKind.RelativeOrAbsolute));
                }
                else
                {
                    SetProperty(toast, "Sound", new Uri("", UriKind.RelativeOrAbsolute));
                }
            }

            toast.Show();

        }

        // Function for setting a property value using reflection.
        private static void SetProperty(object instance, string name, object value)
        {
            var setMethod = instance.GetType().GetProperty(name).GetSetMethod();
            setMethod.Invoke(instance, new object[] { value });
        }


        private async void updateCoordinates()
        {
            try
            {
                Geoposition geoposition = await Geolocator.GetGeopositionAsync();

                Geocoordinate coord = geoposition.Coordinate;
                double _lat = coord.Latitude;
                double _long = coord.Longitude;

                currLoc = new GeoCoordinate(_lat, _long);

                if (isProviderRequest)
                {

                    DateTime now = new DateTime();
                    requestedCoordInfo.isGPSAvailable = (now.Millisecond - requestStartTime) < requestwarmUpTime;
                    requestedCoordInfo.providerEnabled = true;
                    requestedCoordInfo.copyValues(currLoc);

                    var callbackJsonResult = requestedCoordInfo.ToJson();

                    dispatchMessage(PluginResult.Status.OK, callbackJsonResult, true, ConfigureCallbackToken);

                }

                if (lastLoc == null)
                {
                    lastLoc = new GeoCoordinate(_lat, _long);
                }

                if (startLoc == null)
                {
                    startLoc = new GeoCoordinate(_lat, _long);
                }

            }
            catch (Exception ex)
            {
                dispatchMessage(PluginResult.Status.ERROR, string.Format("Error while quering location: {0}!", ex.ToString()), true, ConfigureCallbackToken);
            }

        }

        private bool warmUpTimeOut()
        {
            DateTime now = new DateTime();
            return (now.Millisecond - startTime) >= warmUpTime;
        }

        private bool timeOut()
        {
            DateTime now = new DateTime();
            return now.Millisecond >= (currentMsTime + interval);
        }

        private void updateStatisticsByStepAndLocation(ref double distanceStep, ref GeoCoordinate location)
        {
            linearDistance += distanceStep;
            radiusDistance = startLoc.GetDistanceTo(location);
            lastLoc = location;
        }

        private void dispatchMessage(PluginResult.Status status, string message, bool keepCallback, string callBackId)
        {
            var pluginResult = new PluginResult(status, message) { KeepCallback = keepCallback };
            DispatchCommandResult(pluginResult, callBackId);
        }

    }
}