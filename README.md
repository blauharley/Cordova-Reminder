This Cordova-Plugin enables users to get notified by Reminder-Notifications when they reach a certain <b>distance</b> and time-<b>interval</b>. Therefore showing Reminder-Notifications bases on how far user have gone in meters and whether a certain time has gone.

<h3>Examples:</h3>

<h3>Start a Reminder-Notification</h3>

<blockquote>

  <p>@param <i>success</i> must be a Function to be called when all went right and/or a new Remind-Notification is set.</p>
  <p>@param <i>error</i> must be a Function to be called when there was an error and/or a Remind-Notification could not be set.</p>
  <p>
	@param <i>options</i> must be an Object to be given to adjust some Preferences:
	<ul>
	  <li><i>title</i>must be a String to be shown as title within Reminder-Notification, default: "Reminder-Notification"</li>
	  <li><i>content</i>must be a String to be shown as content-text within Reminder-Notification. Within content there can be a #METER-Notation that is replaced by the actual meters for instance "You went #METER meters!" is shown within a Reminder-Notification where #METER is a Number. default: "Reminder-Content"</li>
	  <li><i>interval</i>must be a Number in Milliseconds to be gone to show a Reminder-Notification, default: 60000</li>
	  <li><i>distance</i>must be a Number in Meter to be reached to show a Reminder-Notification, default: 100</li>
	  <li><i>whistle</i>must be a Boolean to enable/disable Whistle-Sound, default: true</li>
	  <li><i>closeApp</i>must be a Boolean to-close/not-to-close App, default: true</li>
	</ul>
  </p>

  <p>@return undefined</p>
	
  <p><b>Reminder.start( out success:Function, out error:Function, in options:Object ) : undefined</b></p>

</blockquote>

```javascript

 Reminder.start(
    function(){
       console.log("Reminder success")
    },
    function(e){
       console.log("Reminder error",e);
    },
    {
       title: String,
       content: String,
       interval: Number
       distance: Number,
       whistle: Boolean,
       closeApp: Boolean
    }
 );

```

<h3>Clear/Cancel a Remind-Notification</h3>

<blockquote>

  <p>@param <i>success</i> must be a Function to be called when all went right and/or a new Remind-Notification is set.</p>
  <p>@param <i>error</i> must be a Function to be called when there was an error and/or a Remind-Notification could not be set.</p>

  <p>@return undefined</p>
	
  <p><b>Reminder.clear( out success:Function, out error:Function ) : undefined</b></p>

</blockquote>

```javascript

 Reminder.clear(
    function(){
       console.log("Reminder clear success")
    },
    function(e){
       console.log("Reminder clear error",e);
    }
 );

```

<h3>Supported Platforms:</h3>

<ul>
	<li>Android(Jelly Bean, Version: >= 4.1, API: >= 16)</li>
</ul>

<h3>License:</h3>
GNU: Basically this software can be used and modified freely and without any restrictions but replace Icon, Images and other visual Resources in this Project when using it publicly. 