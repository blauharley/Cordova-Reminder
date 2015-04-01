/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

var exec = require("cordova/exec");
module.exports = {

    /*
     @info
     start reminder

     @params

     successCallback: Function
     errorCallback: Function
     options:{
     title : "Reminder-Notification"
     content: "Reminder-Content"
     interval: 60000 (in milliseconds)
     distance: 100 (in meters)
     whistle: true
     closeApp: true
     stopDate: "forever" ("forever" | "tomorrow")
     distanceTolerance: 10
     speedMsTolerance: 10
     }
     */
    start : function (successCallback, errorCallback, options) {

        options = options || {};

        var title = options.title ? options.title : "Reminder-Notification";
        var content = options.content ? options.content : "Reminder-Content";
        var interval = options.interval ? options.interval : 60000;
        var distance = options.distance ? options.distance : 100;
        var whistle = options.whistle != undefined ? options.whistle : true;
        var closeApp = options.closeApp != undefined ? options.closeApp : true;
        var stopDate = options.stopDate != undefined ? (options.stopDate == "tomorrow" ? "tomorrow" : "forever") : "forever";

        var mode = options.mode != undefined ? (options.mode == "aim" ? options.mode : (options.mode == "track" ? "track" : "status")) : "aim";
        var distanceTolerance = options.distanceTolerance != undefined ? options.distanceTolerance : 10;
        var aimCoordLat = mode == "aim" ? (options.aimCoord != undefined ? options.aimCoord.lat : 0) : 0;
        var aimCoordLong = mode == "aim" ? (options.aimCoord != undefined ? options.aimCoord.long : 0) : 0;

        var args = [title,content,interval,distance,whistle,closeApp,stopDate,distanceTolerance,mode,aimCoordLat,aimCoordLong];

        exec(successCallback, errorCallback, "Reminder", "start", args);
    },

    /*
     @info
     stop reminder

     @params

     successCallback: Function
     errorCallback: Function
     */
    clear : function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, "Reminder", "clear", []);
    },

    /*
     @info
     request provider

     @params

     successCallback: Function
     @param accurancy:integer
     @param provider_enabled:boolean
     @param out_of_service:boolean

     errorCallback: Function
     */
    requestProvider : function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, "Reminder", "request", []);
    },

    /*
     @info
     check reminder runs

     @params

     successCallback: Function
     errorCallback: Function
     */
    isRunning : function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, "Reminder", "isrunning", []);
    }

};