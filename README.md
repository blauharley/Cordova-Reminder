<h3>Experimental-Project</h3>

This Cordova-Plugin enables users to get notified by Reminder-Notifications based on destination coordinates, distance, movement and time.

<h3>Start a Reminder-Notification</h3>

<blockquote>

  <p><b>Reminder.start( out success:Function, out error:Function, in options:Object ) : undefined</b></p>

  <p>@param <b><i>success</i></b> must be a Function to be called when all went right and/or a new Remind-Notification is set.</p>
  <p>@param <b><i>error</i></b> must be a Function to be called when there has been an error and/or a Remind-Notification could not be set.</p>
  <p>
	@param <b><i>options</i></b> must be an Object to be given to adjust some Preferences:
	<ul>
	  <li><b><i>mode</i></b> must be a String to determine whether to show a Reminder-Notification on "aim", "track" or "status" mode, </br><b>default: "aim"</b>
             <ul>

		<li>
		  "aim": show Reminder-Notifcation when an user reaches a certain aim/destination.</br>
                  required properties:
		  <ul>
		     <li>
                        <b><i>distanceTolerance</i></b> is considered to be the radius of the aim/destination in meter.
		     </li>
                     <li>
                        <b><i>aimCoord</i></b> must be an Object that holds lat/long properties to determine the location of aim/destination.
		     </li>
		  </ul>
		</li>

		<li>
                  "track": show Reminder-Notifcation when an user reaches a certain distance.</br>
                  required properties:
		  <ul>
		     <li>
                        <b><i>distance</i></b> is considered how far an user has gone in meter.
		     </li>
		  </ul>
		</li>

		<li>
                  "status": show Reminder-Notifcation when an user comes to a stop</br>
                  required properties:
		  <ul>
		     <li>
                        <b><i>speedMsTolerance</i></b> is considered how fast an user has to go in meter per seconds in order to be determined they come to a stop or they still move.
		     </li>
                     <li>
                        <b><i>interval</i></b> is considered how fast an user has to change stop/go movements in milliseconds.
		     </li>
		  </ul>
		</li>

	     </ul>
         </li>
         <li><b><i>distanceTolerance</i></b> must be a Number to omit possibly fluctuations, by default all little distance-alterations are taken into consideration. </br><b>default: 10</b></li>
	  <li><b><i>speedMsTolerance</i></b> must be a Number to determine the speed in m/s a user has to fall below. </br><b>default: 10</b></li>
          <li><b><i>aimCoord</i></b> must be an Object that holds latitude and longitude to determine aim/destination coordinates. </br><b>default: latitude 0, longitude 0</b></li>
          <li><b><i>title</i></b> must be a String to be shown as title within Reminder-Notification, </br><b>default: "Reminder-Notification"</b></li>
	  <li><b><i>content</i></b> must be a String to be shown as content-text within a Reminder-Notification. Within content there can be a #ML-Notation(linear-distance) that is replaced by the actual gone meters and #MR(radius-distance) for instance "You went #ML meters! within a radius of #MR" is shown within a Reminder-Notification where #ML/#MR are Numbers representing linear/radius-distance.</br> <b>default: "Reminder-Content"</b></li>
	  <li><b><i>interval</i></b> must be a Number in Milliseconds to be gone to show a Reminder-Notification, </br><b>default: 60000(1 minute)</b></li>
	  <li><b><i>distance</i></b> must be a Number in Meter to be reached to show a Reminder-Notification, </br><b>default: 100</b></li>
	  <li><b><i>whistle</i></b> must be a Boolean to enable/disable Whistle-Sound, </br><b>default: true</b></li>
	  <li><b><i>closeApp</i></b> must be a Boolean to-close/not-to-close App, </br><b>default: true</b></li>
	  <li><b><i>stopDate</i></b> must be a String to indicate Reminder should run forever or being stopped next Day, ("forever" | "tomorrow") </br><b>default: "forever"</b></li>
	</ul>
  </p>

  <p>@return undefined</p>
	
</blockquote>

<h4>A Reminder-Notification example of all modes:</h4>

<p><b><i>"aim"</i></b>: When a user has reached a certain aim/destination whithin 30 meters and 1 minute has passed(default) a Reminder-Notification is shown</p>

```javascript

 Reminder.start(function(){
    console.log("Reminder success")
 },
 function(e){
    console.log("Reminder error",e);
 },{
    title: "Reminder",
    content: "You have reached your destination!",
    mode: "aim",
    // required properties
    aimCoord:{
        lat:40.97989806,
        long:-95.2734375
    },
    distanceTolerance: 30
 });

```

<p><b><i>"track"</i></b>: When an user has gone 150 meter and 10 seconds have passed a Reminder-Notification is shown and do not take 10 meter movement fluctuation into consideration</p>

```javascript

 Reminder.start(function(){
    console.log("Reminder success")
 },
 function(e){
    console.log("Reminder error",e);
 },{
    title: "Reminder",
    content: "You have gone #ML within a radius of #MR in meter!",
    interval: 10000,
    mode: "track",
    // required properties
    distance: 150,
    distanceTolerance : 10
 });

```

<p><b><i>"status"</i></b>: All 45 seconds it is tested whether an user goes slower than 4 meter per second a Reminder-Notification is shown</p>

```javascript

 Reminder.start(function(){
    console.log("Reminder success")
 },
 function(e){
    console.log("Reminder error",e);
 },{
    title: "Reminder",
    content: "You have come to a stop while you have gone #ML in meter!",
    mode: "status",
    // required properties
    interval: 45000,
    speedMsTolerance: 4
 });

```


<h3>Clear/Cancel a Remind-Notification</h3>

<blockquote>

  <p><b>Reminder.clear( out success:Function, out error:Function ) : undefined</b></p>

  <p>@param <b><i>success</i></b> must be a Function to be called when all went right and/or a new Remind-Notification is cleared.</p>
  <p>@param <b><i>error</i></b> must be a Function to be called when there has been an error and/or a Remind-Notification could not be cleared.</p>

  <p>@return undefined</p>
	
</blockquote>

```javascript

 Reminder.clear(
    function(){
       console.log("Reminder cleared")
    },
    function(e){
       console.log("Reminder cleared error",e);
    }
 );

```


<h3>Check for Reminder is running</h3>

<blockquote>

  <p><b>Reminder.isRunning( out success:Function, out error:Function ) : undefined</b></p>

  <p>@param <b><i>success</i></b> must be a Function to be called when all went right and a result is returned as <i>Object</i>-parameter to check whether Reminder is running.</p>
  <p>@param <b><i>error</i></b> must be a Function to be called when there has been an error.</p>

  <p>@return undefined</p>
	
</blockquote>

```javascript

 Reminder.isRunning(function(result){
      console.log("Reminder isRunning",result.isRunning);
 },
 function(e){
      console.log("Reminder isRunning error",e);
 });

```

<h3>Make a request to GPS-Provider</h3>

<blockquote>

  <p><b>Reminder.requestProvider( out success:Function, out error:Function ) : undefined</b></p>

  <p>@param <b><i>success</i></b> must be a Function to be called when all went right and a result is returned as <i>Object</i>-parameters containing <i>accurancy</i>, <i>provider_enabled</i> and <i>out_of_service</i></p>
  <p>@param <b><i>error</i></b> must be a Function to be called when there has been an error.</p>

  <p>@return undefined</p>
	
</blockquote>

```javascript

 Reminder.requestProvider(function(info){
      console.log("Reminder requestProvider: ",info);
      console.log("Reminder requestProvider accurancy: ",info.accurancy);
      console.log("Reminder provider enabled: ",info.provider_enabled);
      console.log("Reminder out of service: ",info.out_of_service);	
 },
 function(e){
      console.log("Reminder requestProvider",e);
 });

```

<h3>Supported Platforms:</h3>

<ul>
  <li>Android(Jelly Bean, Version: >= 4.1, API: >= 16)</li>
</ul>

<h3>License:</h3>
GNU: Basically this software can be used and modified freely and without any restrictions but replace Icon, Images and other visual Resources in this Project when using it publicly. 